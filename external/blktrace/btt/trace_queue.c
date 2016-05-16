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

static void handle_queue(struct io *q_iop)
{
	seeki_add(q_iop->dip->q2q_handle, q_iop);
	update_qregion(&all_regions, q_iop->t.time);
	dip_update_q(q_iop->dip, q_iop);
	pip_update_q(q_iop);
	if (remapper_dev(q_iop->t.device))
		update_lq(&last_q, &all_avgs.q2q_dm, q_iop->t.time);
	else {
		update_q_histo(q_iop->t.bytes);
		update_lq(&last_q, &all_avgs.q2q, q_iop->t.time);
	}

	q_iop->i_time = q_iop->g_time = q_iop->i_time = q_iop->m_time =
						q_iop->d_time = (__u64)-1;
	q_iop->dip->n_qs++;

	q_iop->dip->t_act_q += q_iop->dip->n_act_q;
	q_iop->dip->n_act_q++;
}

void trace_queue(struct io *q_iop)
{
	if (q_iop->t.bytes == 0)
		return;

	if (io_setup(q_iop, IOP_Q))
		handle_queue(q_iop);
	else
		io_release(q_iop);
}
