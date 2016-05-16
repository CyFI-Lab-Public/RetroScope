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
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "blktrace.h"
#include "rbtree.h"
#include "list.h"

/*
 * 0 == 1 blk
 * 1 == 2 blks
 * ...
 * 1022 == 1023 blks
 * 1023 == 1024 blks
 * 1024 == > 1024 blks
 */
#define N_HIST_BKTS	1025

#define BIT_TIME(t)	((double)SECONDS(t) + ((double)NANO_SECONDS(t) / 1.0e9))

#define BIT_START(iop)	((iop)->t.sector)
#define BIT_END(iop)	((iop)->t.sector + ((iop)->t.bytes >> 9))
#define IOP_READ(iop)	((iop)->t.action & BLK_TC_ACT(BLK_TC_READ))
#define IOP_RW(iop)	(IOP_READ(iop) ? 1 : 0)

#define TO_SEC(nanosec)	((double)(nanosec) / 1.0e9)
#define TO_MSEC(nanosec) (1000.0 * TO_SEC(nanosec))

enum iop_type {
	IOP_Q = 0,
	IOP_X = 1,
	IOP_A = 2,
	IOP_G = 3,
	IOP_M = 4,
	IOP_D = 5,
	IOP_C = 6,
	IOP_R = 7,
	IOP_I = 8,
	IOP_S = 9
};
#define N_IOP_TYPES	(IOP_S + 1)

struct mode {
	int most_seeks, nmds;
	long long *modes;
};

struct io;
struct io_list {
	struct list_head head;
	struct io *iop;
	int cy_users;
};

struct avg_info {
	__u64 min, max, total;
	double avg;
	int n;
};

struct avgs_info {
        struct avg_info q2q_dm;
        struct avg_info q2a_dm;
        struct avg_info q2c_dm;

        struct avg_info q2q;
	struct avg_info q2a;
	struct avg_info q2g;
	struct avg_info s2g;
	struct avg_info g2i;
	struct avg_info q2m;
	struct avg_info i2d;
	struct avg_info m2d;
	struct avg_info d2c;
	struct avg_info q2c;

	struct avg_info blks;		/* Blocks transferred */
};

struct range_info {
	struct list_head head;		/* on: qranges OR cranges */
	__u64 start, end;
};

struct region_info {
	struct list_head qranges;
	struct list_head cranges;
};

struct p_info {
	struct region_info regions;
	struct avgs_info avgs;
	__u64 last_q;
	__u32 pid;
	char *name;
};

struct stats {
	__u64 rqm[2], ios[2], sec[2], wait, svctm;
	double last_qu_change, last_dev_change, tot_qusz, idle_time;
	int cur_qusz, cur_dev;
};

struct stats_t {
	double n;
	double rqm_s[2], ios_s[2], sec_s[2];
	double avgrq_sz, avgqu_sz, await, svctm, p_util;
};

struct d_info {
	struct list_head all_head, hash_head;
	void *heads;
	struct region_info regions;
	char *devmap;
	void *q2q_handle, *seek_handle, *bno_dump_handle, *up_hist_handle;
	void *q2d_priv, *aqd_handle;
	void *q2d_plat_handle, *q2c_plat_handle, *d2c_plat_handle;
	FILE *q2d_ofp, *d2c_ofp, *q2c_ofp, *pit_fp;
	struct avgs_info avgs;
	struct stats stats, all_stats;
	__u64 last_q, n_qs, n_ds;
	__u64 n_act_q, t_act_q;	/* # currently active when Q comes in */
	__u32 device;

	int pre_culling;
	int is_plugged, nplugs, nplugs_t;
	__u64 nios_up, nios_upt;
	double start_time, last_plug, plugged_time, end_time;
};

struct io {
	struct rb_node rb_node;
	struct list_head f_head, a_head;
	struct d_info *dip;
	struct p_info *pip;
	void *pdu;
	__u64 bytes_left, g_time, i_time, m_time, d_time, c_time, d_sec, c_sec;
	__u64 s_time;
	__u32 d_nsec, c_nsec;

	struct blk_io_trace t;

	int linked;
	enum iop_type type;
};

/* bt_timeline.c */

extern char bt_timeline_version[], *devices, *exes, *input_name, *output_name;
extern char *seek_name, *iostat_name, *d2c_name, *q2c_name, *per_io_name;
extern char *bno_dump_name, *unplug_hist_name, *sps_name, *aqd_name, *q2d_name;
extern char *per_io_trees;
extern double range_delta, plat_freq;
extern FILE *rngs_ofp, *avgs_ofp, *xavgs_ofp, *iostat_ofp, *per_io_ofp;
extern FILE *msgs_ofp;
extern int verbose, done, time_bounded, output_all_data, seek_absolute;
extern int easy_parse_avgs, ignore_remaps;
extern unsigned int n_devs;
extern unsigned long n_traces;
extern struct list_head all_devs, all_procs;
extern struct avgs_info all_avgs;
extern __u64 last_q;
extern struct region_info all_regions;
extern struct list_head all_ios, free_ios;
extern __u64 iostat_interval, iostat_last_stamp;
extern time_t genesis, last_vtrace;
extern double t_astart, t_aend;
extern __u64 q_histo[N_HIST_BKTS], d_histo[N_HIST_BKTS];

/* args.c */
void handle_args(int argc, char *argv[]);
void clean_args();

/* aqd.c */
void *aqd_alloc(char *str);
void aqd_free(void *info);
void aqd_clean(void);
void aqd_issue(void *info, double ts);
void aqd_complete(void *info, double ts);

/* devmap.c */
int dev_map_read(char *fname);
char *dev_map_find(__u32 device);
void dev_map_exit(void);

/* devs.c */
void init_dev_heads(void);
struct d_info *dip_alloc(__u32 device, struct io *iop);
void iop_rem_dip(struct io *iop);
struct d_info *__dip_find(__u32 device);
void dip_foreach_list(struct io *iop, enum iop_type type, struct list_head *hd);
void dip_foreach(struct io *iop, enum iop_type type,
		 void (*fnc)(struct io *iop, struct io *this), int rm_after);
struct io *dip_find_sec(struct d_info *dip, enum iop_type type, __u64 sec);
void dip_foreach_out(void (*func)(struct d_info *, void *), void *arg);
void dip_plug(__u32 dev, double cur_time);
void dip_unplug(__u32 dev, double cur_time, __u64 nio_ups);
void dip_unplug_tm(__u32 dev, double cur_time, __u64 nio_ups);
void dip_exit(void);
void dip_cleanup(void);

/* dip_rb.c */
int rb_insert(struct rb_root *root, struct io *iop);
struct io *rb_find_sec(struct rb_root *root, __u64 sec);
void rb_foreach(struct rb_node *n, struct io *iop,
		      void (*fnc)(struct io *iop, struct io *this),
		      struct list_head *head);

/* iostat.c */
void iostat_init(void);
void iostat_getrq(struct io *iop);
void iostat_merge(struct io *iop);
void iostat_issue(struct io *iop);
void iostat_complete(struct io *d_iop, struct io *c_iop);
void iostat_check_time(__u64 stamp);
void iostat_dump_stats(__u64 stamp, int all);

/* latency.c */
void latency_alloc(struct d_info *dip);
void latency_clean(void);
void latency_q2d(struct d_info *dip, __u64 tstamp, __u64 latency);
void latency_d2c(struct d_info *dip, __u64 tstamp, __u64 latency);
void latency_q2c(struct d_info *dip, __u64 tstamp, __u64 latency);

/* misc.c */
void add_file(FILE *fp, char *oname);
void add_buf(void *buf);
char *make_dev_hdr(char *pad, size_t len, struct d_info *dip, int add_parens);
FILE *my_fopen(const char *path, const char *mode);
int my_open(const char *path, int flags);
void dbg_ping(void);
void clean_allocs(void);

/* mmap.c */
void setup_ifile(char *fname);
void cleanup_ifile(void);
int next_trace(struct blk_io_trace *t, void **pdu);
double pct_done(void);

/* output.c */
int output_avgs(FILE *ofp);
int output_ranges(FILE *ofp);

/* proc.c */
void process_alloc(__u32 pid, char *name);
struct p_info *find_process(__u32 pid, char *name);
void pip_update_q(struct io *iop);
void pip_foreach_out(void (*f)(struct p_info *, void *), void *arg);
void pip_exit(void);

/* bno_dump.c */
void *bno_dump_alloc(__u32 device);
void bno_dump_free(void *param);
void bno_dump_add(void *handle, struct io *iop);
void bno_dump_clean(void);

/* plat.c */
void *plat_alloc(char *str);
void plat_free(void *info);
void plat_clean(void);
void plat_x2c(void *info, __u64 ts, __u64 latency);

/* q2d.c */
void q2d_histo_add(void *priv, __u64 q2d);
void *q2d_alloc(void);
void q2d_free(void *priv);
void q2d_display_header(FILE *fp);
void q2d_display_dashes(FILE *fp);
void q2d_display(FILE *fp, void *priv);
int q2d_ok(void *priv);
void q2d_acc(void *a1, void *a2);

/* seek.c */
void *seeki_alloc(char *str);
void seeki_free(void *param);
void seek_clean(void);
void seeki_add(void *handle, struct io *iop);
double seeki_mean(void *handle);
long long seeki_nseeks(void *handle);
long long seeki_median(void *handle);
int seeki_mode(void *handle, struct mode *mp);

/* trace.c */
void add_trace(struct io *iop);

/* trace_complete.c */
void trace_complete(struct io *c_iop);

/* trace_im.c */
void run_im(struct io *im_iop, struct io *d_iop, struct io *c_iop);
void run_unim(struct io *im_iop, struct io *d_iop, struct io *c_iop);
int ready_im(struct io *im_iop, struct io *c_iop);
void trace_insert(struct io *i_iop);
void trace_merge(struct io *m_iop);
void trace_getrq(struct io *g_iop);
void trace_sleeprq(struct io *s_iop);

/* trace_issue.c */
void run_issue(struct io *d_iop, struct io *u_iop, struct io *c_iop);
void run_unissue(struct io *d_iop, struct io *u_iop, struct io *c_iop);
int ready_issue(struct io *d_iop, struct io *c_iop);
void trace_issue(struct io *d_iop);

/* trace_plug.c */
void trace_plug(struct io *p_iop);
void trace_unplug_io(struct io *u_iop);
void trace_unplug_timer(struct io *u_iop);

/* trace_queue.c */
void run_queue(struct io *q_iop, struct io *u_iop, struct io *c_iop);
int ready_queue(struct io *q_iop, struct io *c_iop);
void trace_queue(struct io *q_iop);

/* trace_remap.c */
void run_remap(struct io *a_iop, struct io *u_iop, struct io *c_iop);
int ready_remap(struct io *a_iop, struct io *c_iop);
void trace_remap(struct io *a_iop);

/* trace_requeue.c */
void trace_requeue(struct io *r_iop);

/* unplug_hist.c */
void *unplug_hist_alloc(__u32 device);
void unplug_hist_free(void *arg);
void unplug_hist_add(struct io *u_iop);

#include "inlines.h"
