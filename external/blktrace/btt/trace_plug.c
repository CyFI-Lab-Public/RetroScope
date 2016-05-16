/*
 * blktrace output analysis: generate a timeline & gather statistics
 *
 * Copyright (C) 2007 Alan D. Brunelle <Alan.Brunelle@hp.com>
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

static __u64 get_nio_up(struct io *u_iop)
{
	__u64 *val = u_iop->pdu;
	return be64_to_cpu(*val);
}

void trace_unplug_io(struct io *u_iop)
{
	unplug_hist_add(u_iop);
	dip_unplug(u_iop->t.device, BIT_TIME(u_iop->t.time), get_nio_up(u_iop));
	io_release(u_iop);
}

void trace_unplug_timer(struct io *ut_iop)
{
	dip_unplug_tm(ut_iop->t.device, BIT_TIME(ut_iop->t.time),
			get_nio_up(ut_iop));
	io_release(ut_iop);
}

void trace_plug(struct io *p_iop)
{
	dip_plug(p_iop->t.device, BIT_TIME(p_iop->t.time));
	io_release(p_iop);
}
