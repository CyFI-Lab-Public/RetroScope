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

static void handle_issue(struct io *d_iop)
{
	LIST_HEAD(head);
	struct list_head *p, *q;

	if (d_iop->dip->n_act_q != 0)
		d_iop->dip->n_act_q--;

	seeki_add(d_iop->dip->seek_handle, d_iop);
	bno_dump_add(d_iop->dip->bno_dump_handle, d_iop);
	iostat_issue(d_iop);
	d_iop->dip->n_ds++;
	if (!remapper_dev(d_iop->t.device))
		update_d_histo(d_iop->t.bytes);
	aqd_issue(d_iop->dip->aqd_handle, BIT_TIME(d_iop->t.time));

	dip_foreach_list(d_iop, IOP_Q, &head);
	list_for_each_safe(p, q, &head) {
		struct io *q_iop = list_entry(p, struct io, f_head);

		if (q_iop->i_time != (__u64)-1)
			update_i2d(q_iop, tdelta(q_iop->i_time, d_iop->t.time));
		else if (q_iop->m_time != (__u64)-1)
			update_m2d(q_iop, tdelta(q_iop->m_time, d_iop->t.time));

		d_iop->bytes_left -= q_iop->t.bytes;
		list_del(&q_iop->f_head);

		q_iop->d_time = d_iop->t.time;
		q_iop->d_sec = d_iop->t.sector;
		q_iop->d_nsec = t_sec(&d_iop->t);

		if (output_all_data)
			q2d_histo_add(q_iop->dip->q2d_priv,
						d_iop->t.time - q_iop->t.time);
		latency_q2d(q_iop->dip, d_iop->t.time, 
						d_iop->t.time - q_iop->t.time);
	}
}

void trace_issue(struct io *d_iop)
{
	if (d_iop->t.bytes == 0)
		return;

	if (io_setup(d_iop, IOP_D))
		handle_issue(d_iop);

	io_release(d_iop);

}
