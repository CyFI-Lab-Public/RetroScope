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

static void handle_g(struct io *g_iop)
{
	struct io *q_iop;

	iostat_getrq(g_iop);

	q_iop = dip_find_sec(g_iop->dip, IOP_Q, g_iop->t.sector);
	if (q_iop) {
		q_iop->g_time = g_iop->t.time;
		update_q2g(q_iop, tdelta(q_iop->t.time, g_iop->t.time));
		if (q_iop->s_time != 0)
			update_s2g(q_iop, tdelta(q_iop->s_time, g_iop->t.time));
	}
}

static void handle_s(struct io *s_iop)
{
	struct io *q_iop = dip_find_sec(s_iop->dip, IOP_Q, s_iop->t.sector);

	if (q_iop)
		q_iop->s_time = s_iop->t.time;
}

static void handle_i(struct io *i_iop)
{
	struct io *q_iop = dip_find_sec(i_iop->dip, IOP_Q, i_iop->t.sector);

	if (q_iop) {
		q_iop->i_time = i_iop->t.time;
		if (q_iop->g_time != (__u64)-1)
			update_g2i(q_iop, tdelta(q_iop->g_time, i_iop->t.time));
	}
}

static void handle_m(struct io *m_iop)
{
	struct io *q_iop;

	iostat_merge(m_iop);

	q_iop = dip_find_sec(m_iop->dip, IOP_Q, m_iop->t.sector);
	if (q_iop) {
		q_iop->m_time = m_iop->t.time;
		update_q2m(q_iop, tdelta(q_iop->t.time, m_iop->t.time));
	}

	if (m_iop->dip->n_act_q != 0)
		m_iop->dip->n_act_q--;
}

void trace_sleeprq(struct io *s_iop)
{
	if (s_iop->t.bytes == 0)
		return;
	if (io_setup(s_iop, IOP_S))
		handle_s(s_iop);
	io_release(s_iop);
}

void trace_getrq(struct io *g_iop)
{
	if (g_iop->t.bytes == 0)
		return;
	if (io_setup(g_iop, IOP_G))
		handle_g(g_iop);
	io_release(g_iop);
}

void trace_insert(struct io *i_iop)
{
	if (i_iop->t.bytes == 0)
		return;
	if (io_setup(i_iop, IOP_I))
		handle_i(i_iop);
	io_release(i_iop);
}

void trace_merge(struct io *m_iop)
{
	if (m_iop->t.bytes == 0)
		return;
	if (io_setup(m_iop, IOP_M))
		handle_m(m_iop);
	io_release(m_iop);
}
