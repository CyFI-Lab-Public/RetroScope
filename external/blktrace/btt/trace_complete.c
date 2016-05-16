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
#include "globals.h"

static inline void __out(FILE *ofp, __u64 tm, enum iop_type type,
					__u64 sec, __u32 nsec, int indent)
{
	if (tm != (__u64)-1) {
		if (indent)
			fprintf(ofp, "         ");
		fprintf(ofp, "%5d.%09lu %c %10llu+%-4u\n",
			(int)SECONDS(tm), (unsigned long)NANO_SECONDS(tm),
			type2c(type), (unsigned long long)sec, nsec);
	}
}

static void display_io_track(FILE *ofp, struct io *iop)
{
	fprintf(ofp, "%3d,%-3d: ", MAJOR(iop->t.device), MINOR(iop->t.device));
	__out(ofp, iop->t.time, IOP_Q, iop->t.sector, t_sec(&iop->t), 0);

	if (iop->g_time != (__u64)-1)
		__out(ofp, iop->g_time, IOP_G, iop->t.sector, t_sec(&iop->t),1);
	if (iop->i_time != (__u64)-1)
		__out(ofp, iop->i_time, IOP_I, iop->t.sector, t_sec(&iop->t),1);
	if (iop->m_time != (__u64)-1)
		__out(ofp, iop->m_time, IOP_M, iop->t.sector, t_sec(&iop->t),1);

	__out(ofp, iop->d_time, IOP_D, iop->d_sec, iop->d_nsec, 1);
	__out(ofp, iop->c_time, IOP_C, iop->c_sec, iop->c_nsec, 1);
	fprintf(ofp, "\n");
}

static void handle_complete(struct io *c_iop)
{
	LIST_HEAD(head);
	struct list_head *p, *q;
	__u64 d_time = (__u64)-1;
	FILE *pit_fp = c_iop->dip->pit_fp;

	update_blks(c_iop);
	update_cregion(&all_regions, c_iop->t.time);
	update_cregion(&c_iop->dip->regions, c_iop->t.time);
	if (c_iop->pip)
		update_cregion(&c_iop->pip->regions, c_iop->t.time);
	aqd_complete(c_iop->dip->aqd_handle, BIT_TIME(c_iop->t.time));

	dip_foreach_list(c_iop, IOP_Q, &head);
	list_for_each_safe(p, q, &head) {
		struct io *q_iop = list_entry(p, struct io, f_head);
		__u64 q2c = tdelta(q_iop->t.time, c_iop->t.time);

		c_iop->bytes_left -= q_iop->t.bytes;

		update_q2c(q_iop, q2c);
		latency_q2c(q_iop->dip, q_iop->t.time, q2c);

		if (q_iop->d_time != (__u64)-1) {
			__u64 d2c = tdelta(q_iop->d_time, c_iop->t.time);

			update_d2c(q_iop, d2c);
			latency_d2c(q_iop->dip, c_iop->t.time, d2c);
			iostat_complete(q_iop, c_iop);

			d_time = q_iop->d_time;
		}

		if (per_io_ofp) {
			q_iop->c_time = c_iop->t.time;
			q_iop->c_sec = c_iop->t.sector;
			q_iop->c_nsec = t_sec(&c_iop->t);
			display_io_track(per_io_ofp, q_iop);
		}

		if (q_iop->dip->pit_fp) {
			fprintf(pit_fp, "%d.%09lu ",
				(int)SECONDS(q_iop->t.time),
				(unsigned long)NANO_SECONDS(q_iop->t.time));
		}

		list_del(&q_iop->f_head);
		io_release(q_iop);
	}

	if (per_io_ofp)
		fprintf(per_io_ofp,
			"-----------------------------------------\n");

	if (c_iop->dip->pit_fp) {
		fprintf(pit_fp, "| %d.%09lu | %d.%09lu\n",
			(int)SECONDS(d_time),
			(unsigned long)NANO_SECONDS(d_time),
			(int)SECONDS(c_iop->t.time),
			(unsigned long)NANO_SECONDS(c_iop->t.time));
	}
}

void trace_complete(struct io *c_iop)
{
	if (c_iop->t.bytes == 0)
		return;

	if (io_setup(c_iop, IOP_C))
		handle_complete(c_iop);

	io_release(c_iop);
}
