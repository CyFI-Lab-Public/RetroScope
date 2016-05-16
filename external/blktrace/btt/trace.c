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

static void __add_trace(struct io *iop)
{
	time_t now = time(NULL);

	n_traces++;
	iostat_check_time(iop->t.time);

	if (verbose && ((now - last_vtrace) > 0)) {
		printf("%10lu t (%6.2lf%%)\r", n_traces, pct_done());
		if ((n_traces % 1000000) == 0) printf("\n");
		fflush(stdout);
		last_vtrace = now;
	}

	switch (iop->t.action & 0xffff) {
	case __BLK_TA_QUEUE:		trace_queue(iop); break;
	case __BLK_TA_REMAP:		trace_remap(iop); break;
	case __BLK_TA_INSERT:		trace_insert(iop); break;
	case __BLK_TA_GETRQ:		trace_getrq(iop); break;
	case __BLK_TA_BACKMERGE:	trace_merge(iop); break;
	case __BLK_TA_FRONTMERGE:	trace_merge(iop); break;
	case __BLK_TA_REQUEUE:		trace_requeue(iop); break;
	case __BLK_TA_ISSUE:		trace_issue(iop); break;
	case __BLK_TA_COMPLETE:		trace_complete(iop); break;
	case __BLK_TA_PLUG:		trace_plug(iop); break;
	case __BLK_TA_UNPLUG_IO:	trace_unplug_io(iop); break;
	case __BLK_TA_UNPLUG_TIMER:	trace_unplug_timer(iop); break;
	case __BLK_TA_SLEEPRQ:		trace_sleeprq(iop); break;
	default:
		io_release(iop);
		return;
	}
}

static void trace_message(struct io *iop)
{
	char scratch[15];
	char msg[iop->t.pdu_len + 1];

	if (!io_setup(iop, IOP_M))
		return;

	memcpy(msg, iop->pdu, iop->t.pdu_len);
	msg[iop->t.pdu_len] = '\0';

	fprintf(msgs_ofp, "%s %5d.%09lu %s\n",
		make_dev_hdr(scratch, 15, iop->dip, 1),
		(int)SECONDS(iop->t.time),
		(unsigned long)NANO_SECONDS(iop->t.time), msg);
}

void add_trace(struct io *iop)
{
	if (iop->t.action & BLK_TC_ACT(BLK_TC_NOTIFY)) {
		if (iop->t.action == BLK_TN_PROCESS) {
			if (iop->t.pid == 0)
				process_alloc(0, "kernel");
			else {
				char *slash = strchr(iop->pdu, '/');
				if (slash)
					*slash = '\0';

				process_alloc(iop->t.pid, iop->pdu);
			}
		} else if (iop->t.action == BLK_TN_MESSAGE)
			trace_message(iop);
		io_release(iop);
	} else if (iop->t.action & BLK_TC_ACT(BLK_TC_PC)) {
		io_release(iop);
	} else {
		if (time_bounded) {
			if (BIT_TIME(iop->t.time) < t_astart) {
				io_release(iop);
				return;
			} else if (BIT_TIME(iop->t.time) > t_aend) {
				io_release(iop);
				done = 1;
				return;
			}
		}
		__add_trace(iop);
	}
}
