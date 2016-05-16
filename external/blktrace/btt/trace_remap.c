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

static inline void cvt_pdu_remap(struct blk_io_trace_remap *rp)
{
	rp->device_from = be32_to_cpu(rp->device_from);
	rp->device_to   = be32_to_cpu(rp->device_to);
	rp->sector_from = be64_to_cpu(rp->sector_from);
}

/*
 * q_iop == volume device
 * a_iop == underlying device
 */
void trace_remap(struct io *a_iop)
{
	struct io *q_iop;
	struct d_info *q_dip;
	struct blk_io_trace_remap *rp;

	if (ignore_remaps)
		goto out;

	rp = a_iop->pdu;
	cvt_pdu_remap(rp);

	if (!io_setup(a_iop, IOP_A))
		goto out;

	q_dip = __dip_find(rp->device_from);
	if (!q_dip)
		goto out;

	q_iop = dip_find_sec(q_dip, IOP_Q, rp->sector_from);
	if (q_iop)
		update_q2a(q_iop, tdelta(q_iop->t.time, a_iop->t.time));

out:
	io_release(a_iop);
}
