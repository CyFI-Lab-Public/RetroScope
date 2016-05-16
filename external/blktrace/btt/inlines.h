/*
 * blktrace output analysis: generate a timeline & gather statistics
 *
 * Copyright (C) 2006 Alan D. Brunelle <Alan.Brunelle@hp.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

static inline int remapper_dev(__u32 dev)
{
	int mjr = MAJOR(dev);
	return mjr == 9 || mjr == 253 || mjr == 254;
}

static inline void region_init(struct region_info *reg)
{
	INIT_LIST_HEAD(&reg->qranges);
	INIT_LIST_HEAD(&reg->cranges);
}

static inline void __region_exit(struct list_head *range_head)
{
	struct list_head *p, *q;
	struct range_info *rip;

	list_for_each_safe(p, q, range_head) {
		rip = list_entry(p, struct range_info, head);
		free(rip);
	}
}

static inline void region_exit(struct region_info *reg)
{
	__region_exit(&reg->qranges);
	__region_exit(&reg->cranges);
}

static inline void update_range(struct list_head *head_p, __u64 time)
{
	struct range_info *rip;

	if (!list_empty(head_p)) {
		rip = list_entry(head_p->prev, struct range_info, head);

		if (time < rip->end)
			return;

		if (BIT_TIME(time - rip->end) < range_delta) {
			rip->end = time;
			return;
		}
	}

	rip = malloc(sizeof(*rip));
	rip->start = rip->end = time;
	list_add_tail(&rip->head, head_p);
}

static inline void update_qregion(struct region_info *reg, __u64 time)
{
	update_range(&reg->qranges, time);
}

static inline void update_cregion(struct region_info *reg, __u64 time)
{
	update_range(&reg->cranges, time);
}

static inline void avg_update(struct avg_info *ap, __u64 t)
{
        if (ap->n++ == 0)
                ap->min = ap->total = ap->max = t;
        else {
                if (t < ap->min)
                        ap->min = t;
                else if (t > ap->max)
                        ap->max = t;
                ap->total += t;
        }
}

static inline void avg_update_n(struct avg_info *ap, __u64 t, int n)
{
        if (ap->n == 0) {
                ap->min = ap->max = t;
		ap->total = (n * t);
	} else {
                if (t < ap->min)
                        ap->min = t;
                else if (t > ap->max)
                        ap->max = t;
                ap->total += (n * t);
        }

	ap->n += n;
}

static inline void avg_unupdate(struct avg_info *ap, __u64 t)
{
	ap->n--;
	ap->total -= t;
}

static inline void update_lq(__u64 *last_q, struct avg_info *avg, __u64 time)
{
	if (*last_q != ((__u64)-1))
		avg_update(avg, (time > *last_q) ? time - *last_q : 1);
	*last_q = time;
}

static inline void dip_update_q(struct d_info *dip, struct io *iop)
{
	if (remapper_dev(dip->device))
		update_lq(&dip->last_q, &dip->avgs.q2q_dm, iop->t.time);
	else
		update_lq(&dip->last_q, &dip->avgs.q2q, iop->t.time);
	update_qregion(&dip->regions, iop->t.time);
}

static inline struct io *io_alloc(void)
{
	struct io *iop = malloc(sizeof(*iop));

	memset(iop, 0, sizeof(struct io));
	list_add_tail(&iop->a_head, &all_ios);

	return iop;
}

static inline void io_free(struct io *iop)
{
	list_del(&iop->a_head);
	free(iop);
}

static inline void io_free_all(void)
{
	struct io *iop;
	struct list_head *p, *q;

	list_for_each_safe(p, q, &all_ios) {
		iop = list_entry(p, struct io, a_head);
		free(iop);
	}
}

static inline int io_setup(struct io *iop, enum iop_type type)
{
	iop->type = type;
	iop->dip = dip_alloc(iop->t.device, iop);
	if (iop->linked) {
		iop->pip = find_process(iop->t.pid, NULL);
		iop->bytes_left = iop->t.bytes;
	}

	return iop->linked;
}

static inline void io_release(struct io *iop)
{
	if (iop->linked)
		iop_rem_dip(iop);
	if (iop->pdu)
		free(iop->pdu);

	io_free(iop);
}

#define UPDATE_AVGS(_avg, _iop, _pip, _time) do {			\
		avg_update(&all_avgs. _avg , _time);			\
		avg_update(&_iop->dip->avgs. _avg , _time);		\
		if (_pip) avg_update(&_pip->avgs. _avg , _time);	\
	} while (0)

#define UPDATE_AVGS_N(_avg, _iop, _pip, _time, _n) do {			\
		avg_update_n(&all_avgs. _avg , _time, _n);		\
		avg_update_n(&_iop->dip->avgs. _avg , _time, _n);	\
		if (_pip) avg_update_n(&_pip->avgs. _avg , _time,_n);	\
	} while (0)

#define UNUPDATE_AVGS(_avg, _iop, _pip, _time) do {			\
		avg_unupdate(&all_avgs. _avg , _time);			\
		avg_unupdate(&_iop->dip->avgs. _avg , _time);		\
		if (_pip) avg_unupdate(&_pip->avgs. _avg , _time);	\
	} while (0)

static inline void update_q2c(struct io *iop, __u64 c_time)
{
	if (remapper_dev(iop->dip->device))
		UPDATE_AVGS(q2c_dm, iop, iop->pip, c_time);
	else
		UPDATE_AVGS(q2c, iop, iop->pip, c_time);
}

static inline void update_q2a(struct io *iop, __u64 a_time)
{
	if (remapper_dev(iop->dip->device))
		UPDATE_AVGS(q2a_dm, iop, iop->pip, a_time);
	else
		UPDATE_AVGS(q2a, iop, iop->pip, a_time);
}

static inline void update_q2g(struct io *iop, __u64 g_time)
{
	UPDATE_AVGS(q2g, iop, iop->pip, g_time);
}

static inline void update_s2g(struct io *iop, __u64 g_time)
{
	UPDATE_AVGS(s2g, iop, iop->pip, g_time);
}

static inline void unupdate_q2g(struct io *iop, __u64 g_time)
{
	UNUPDATE_AVGS(q2g, iop, iop->pip, g_time);
}

static inline void update_g2i(struct io *iop, __u64 i_time)
{
	UPDATE_AVGS(g2i, iop, iop->pip, i_time);
}

static inline void unupdate_g2i(struct io *iop, __u64 i_time)
{
	UNUPDATE_AVGS(g2i, iop, iop->pip, i_time);
}

static inline void update_q2m(struct io *iop, __u64 m_time)
{
	UPDATE_AVGS(q2m, iop, iop->pip, m_time);
}

static inline void unupdate_q2m(struct io *iop, __u64 m_time)
{
	UNUPDATE_AVGS(q2m, iop, iop->pip, m_time);
}

static inline void update_i2d(struct io *iop, __u64 d_time)
{
	UPDATE_AVGS(i2d, iop, iop->pip, d_time);
}

static inline void unupdate_i2d(struct io *iop, __u64 d_time)
{
	UNUPDATE_AVGS(i2d, iop, iop->pip, d_time);
}

static inline void update_m2d(struct io *iop, __u64 d_time)
{
	UPDATE_AVGS(m2d, iop, iop->pip, d_time);
}

static inline void unupdate_m2d(struct io *iop, __u64 d_time)
{
	UNUPDATE_AVGS(m2d, iop, iop->pip, d_time);
}

static inline void update_d2c(struct io *iop, __u64 c_time)
{
	UPDATE_AVGS(d2c, iop, iop->pip, c_time);
}

static inline void update_blks(struct io *iop)
{
	__u64 nblks = iop->t.bytes >> 9;
	avg_update(&all_avgs.blks, nblks);
	avg_update(&iop->dip->avgs.blks, nblks);
	if (iop->pip)
		avg_update(&iop->pip->avgs.blks, nblks);
}

static inline struct rb_root *__get_root(struct d_info *dip, enum iop_type type)
{
	struct rb_root *roots = dip->heads;
	return &roots[type];
}

static inline int dip_rb_ins(struct d_info *dip, struct io *iop)
{
	return rb_insert(__get_root(dip, iop->type), iop);
}

static inline void dip_rb_rem(struct io *iop)
{
	rb_erase(&iop->rb_node, __get_root(iop->dip, iop->type));
}

static inline void dip_rb_fe(struct d_info *dip, enum iop_type type,
		             struct io *iop,
			     void (*fnc)(struct io *iop, struct io *this),
			     struct list_head *head)
{
	rb_foreach(__get_root(dip, type)->rb_node, iop, fnc, head);
}

static inline struct io *dip_rb_find_sec(struct d_info *dip,
		                         enum iop_type type, __u64 sec)
{
	return rb_find_sec(__get_root(dip, type), sec);
}

static inline __u64 tdelta(__u64 from, __u64 to)
{
	return (from < to) ? (to - from) : 1;
}

static inline int type2c(enum iop_type type)
{
	int c;

	switch (type) {
	case IOP_Q: c = 'Q'; break;
	case IOP_X: c = 'X'; break;
	case IOP_A: c = 'A'; break;
	case IOP_I: c = 'I'; break;
	case IOP_M: c = 'M'; break;
	case IOP_D: c = 'D'; break;
	case IOP_C: c = 'C'; break;
	case IOP_R: c = 'R'; break;
	case IOP_G: c = 'G'; break;
	default   : c = '?'; break;
	}

	return c;
}

static inline int histo_idx(__u64 nbytes)
{
	int idx = (nbytes >> 9) - 1;
	return min(idx, N_HIST_BKTS-1);
}

static inline void update_q_histo(__u64 nbytes)
{
	q_histo[histo_idx(nbytes)]++;
}

static inline void update_d_histo(__u64 nbytes)
{
	d_histo[histo_idx(nbytes)]++;
}

static inline struct io *io_first_list(struct list_head *head)
{
	if (list_empty(head))
		return NULL;

	return list_entry(head->next, struct io, f_head);
}

static inline void __dump_iop(FILE *ofp, struct io *iop, int extra_nl)
{
	fprintf(ofp, "%5d.%09lu %3d,%-3d %c %10llu+%-4u\n",
		(int)SECONDS(iop->t.time),
		(unsigned long)NANO_SECONDS(iop->t.time),
		MAJOR(iop->t.device), MINOR(iop->t.device), type2c(iop->type),
		(unsigned long long)iop->t.sector, t_sec(&iop->t));
	if (extra_nl) fprintf(ofp, "\n");
}

static inline void __dump_iop2(FILE *ofp, struct io *a_iop, struct io *l_iop)
{
	fprintf(ofp, "%5d.%09lu %3d,%-3d %c %10llu+%-4u <- (%3d,%-3d) %10llu\n",
		(int)SECONDS(a_iop->t.time),
		(unsigned long)NANO_SECONDS(a_iop->t.time),
		MAJOR(a_iop->t.device), MINOR(a_iop->t.device),
		type2c(a_iop->type), (unsigned long long)a_iop->t.sector,
		t_sec(&a_iop->t), MAJOR(l_iop->t.device),
		MINOR(l_iop->t.device), (unsigned long long)l_iop->t.sector);
}
