/*
 * Blktrace record utility - Convert binary trace data into bunches of IOs
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
 */

#if !defined(__BTRECORD_H__)
#define __BTRECORD_H__

#include <asm/types.h>

#define BT_MAX_PKTS	512

/*
 * Header for each bunch
 *
 * @nkts: 	Number of IO packets to process
 * @time_stamp:	Time stamp for this bunch of IOs
 */
struct io_bunch_hdr {
	__u64 npkts;
	__u64 time_stamp;
};

/*
 * IO specifer
 *
 * @sector:	Sector number of IO
 * @nbytes:	Number of bytes to process
 * @rw:		IO direction: 0 = write, 1 = read
 */
struct io_pkt {
	__u64 sector;
	__u64 nbytes;
	__u32 rw;
};

/*
 * Shorthand notion of a bunch of IOs
 *
 * @hdr: 	Header describing stall and how many IO packets follow
 * @pkts: 	Individual IOs are described here
 */
struct io_bunch {
	struct io_bunch_hdr hdr;
	struct io_pkt pkts[BT_MAX_PKTS];
};

/*
 * Header for each recorded file
 *
 * @version:	Version information
 * @genesis:	Time stamp for earliest bunch
 * @nbunches:	Number of bunches put into the file
 * @total_pkts:	Number of packets to be processed
 */
struct io_file_hdr {
	__u64 version;
	__u64 genesis;
	__u64 nbunches;
	__u64 total_pkts;
};

static inline __u64 mk_btversion(int mjr, int mnr, int sub)
{
	return ((mjr & 0xff) << 16) | ((mnr & 0xff) << 8) | (sub & 0xff);
}

static inline void get_btversion(__u64 version, int *mjr, int *mnr, int *sub)
{
	*mjr = (int)((version >> 16) & 0xff);
	*mnr = (int)((version >>  8) & 0xff);
	*sub = (int)((version >>  0) & 0xff);
}

static char my_btversion[] = "1.0.0";
static int btver_mjr = 1;
static int btver_mnr = 0;
static int btver_sub = 0;

#endif
