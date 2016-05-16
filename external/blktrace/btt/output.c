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
#include "globals.h"

typedef struct avg_info *ai_dip_t;
ai_dip_t dip_q2q_dm_avg(struct d_info *dip) { return &dip->avgs.q2q_dm; }
ai_dip_t dip_q2a_dm_avg(struct d_info *dip) { return &dip->avgs.q2a_dm; }
ai_dip_t dip_q2c_dm_avg(struct d_info *dip) { return &dip->avgs.q2c_dm; }

ai_dip_t dip_q2q_avg(struct d_info *dip) { return &dip->avgs.q2q; }
ai_dip_t dip_q2c_avg(struct d_info *dip) { return &dip->avgs.q2c; }
ai_dip_t dip_q2a_avg(struct d_info *dip) { return &dip->avgs.q2a; }
ai_dip_t dip_q2g_avg(struct d_info *dip) { return &dip->avgs.q2g; }
ai_dip_t dip_s2g_avg(struct d_info *dip) { return &dip->avgs.s2g; }
ai_dip_t dip_g2i_avg(struct d_info *dip) { return &dip->avgs.g2i; }
ai_dip_t dip_q2m_avg(struct d_info *dip) { return &dip->avgs.q2m; }
ai_dip_t dip_i2d_avg(struct d_info *dip) { return &dip->avgs.i2d; }
ai_dip_t dip_d2c_avg(struct d_info *dip) { return &dip->avgs.d2c; }

typedef struct avg_info *ai_pip_t;
ai_pip_t pip_q2q_dm_avg(struct p_info *pip) { return &pip->avgs.q2q_dm; }
ai_pip_t pip_q2a_dm_avg(struct p_info *pip) { return &pip->avgs.q2a_dm; }
ai_pip_t pip_q2c_dm_avg(struct p_info *pip) { return &pip->avgs.q2c_dm; }

ai_pip_t pip_q2q_avg(struct p_info *pip) { return &pip->avgs.q2q; }
ai_pip_t pip_q2c_avg(struct p_info *pip) { return &pip->avgs.q2c; }
ai_pip_t pip_q2a_avg(struct p_info *pip) { return &pip->avgs.q2a; }
ai_pip_t pip_q2g_avg(struct p_info *pip) { return &pip->avgs.q2g; }
ai_pip_t pip_s2g_avg(struct p_info *pip) { return &pip->avgs.s2g; }
ai_pip_t pip_g2i_avg(struct p_info *pip) { return &pip->avgs.g2i; }
ai_pip_t pip_q2m_avg(struct p_info *pip) { return &pip->avgs.q2m; }
ai_pip_t pip_i2d_avg(struct p_info *pip) { return &pip->avgs.i2d; }
ai_pip_t pip_d2c_avg(struct p_info *pip) { return &pip->avgs.d2c; }

void output_section_hdr(FILE *ofp, char *hdr)
{
	fprintf(ofp, "==================== ");
	fprintf(ofp, "%s", hdr);
	fprintf(ofp, " ====================\n\n");
}

void output_hdr(FILE *ofp, char *hdr)
{
	fprintf(ofp, "%15s %13s %13s %13s %11s\n",
	        hdr, "MIN", "AVG", "MAX", "N" );
	fprintf(ofp, "--------------- ------------- ------------- ------------- -----------\n");
}

void __output_avg(FILE *ofp, char *hdr, struct avg_info *ap, int do_easy)
{
	if (ap->n > 0) {
		ap->avg = BIT_TIME(ap->total) / (double)ap->n;
		fprintf(ofp, "%-15s %13.9f %13.9f %13.9f %11d\n", hdr,
			BIT_TIME(ap->min), ap->avg, BIT_TIME(ap->max), ap->n);

		if (do_easy && easy_parse_avgs) {
			fprintf(xavgs_ofp,
				"%s %.9lf %.9lf %.9lf %d\n",
				hdr, BIT_TIME(ap->min), ap->avg,
						BIT_TIME(ap->max), ap->n);
		}
	}
}

static inline char *avg2string(struct avg_info *ap, char *string)
{
	if (ap->n > 0)
		sprintf(string, "%13.9f", ap->avg);
	else
		sprintf(string, " ");
	return string;
}

struct __oda {
	FILE *ofp;
	ai_dip_t (*func)(struct d_info *);
};
void __output_dip_avg(struct d_info *dip, void *arg)
{
	struct __oda *odap = arg;
	ai_dip_t ap = odap->func(dip);
	if (ap->n > 0) {
		char dev_info[15];
		ap->avg = BIT_TIME(ap->total) / (double)ap->n;
		__output_avg(odap->ofp, make_dev_hdr(dev_info, 15, dip, 1),
				ap, 0);
	}
}

void output_dip_avg(FILE *ofp, char *hdr, ai_dip_t (*func)(struct d_info *))
{
	struct __oda oda = { .ofp = ofp, .func = func};
	output_hdr(ofp, hdr);
	dip_foreach_out(__output_dip_avg, &oda);
	fprintf(ofp, "\n");
}

struct __q2d {
	FILE *ofp;
	void *q2d_all;
	int n;
};
void __output_q2d_histo(struct d_info *dip, void *arg)
{
	struct __q2d *q2dp = arg;

	if (q2d_ok(dip->q2d_priv)) {
		char dev_info[15];
		FILE *ofp = q2dp->ofp;

		fprintf(q2dp->ofp, "%10s | ",
					make_dev_hdr(dev_info, 15, dip, 1));
		q2d_display(ofp, dip->q2d_priv);
		q2d_acc(q2dp->q2d_all, dip->q2d_priv);
		q2dp->n++;
	}
}

void output_q2d_histo(FILE *ofp)
{
	struct __q2d __q2d = {
		.ofp = ofp,
		.q2d_all = q2d_alloc(),
		.n = 0
	};

	fprintf(ofp, "%10s | ", "DEV");
	q2d_display_header(ofp);
	fprintf(ofp, "--------- | ");
	q2d_display_dashes(ofp);
	dip_foreach_out(__output_q2d_histo, &__q2d);

	if (__q2d.n) {
		fprintf(ofp, "========== | ");
		q2d_display_dashes(ofp);
		fprintf(ofp, "%10s | ", "AVG");
		q2d_display(ofp, __q2d.q2d_all);
		fprintf(ofp, "\n");
	}

	q2d_free(__q2d.q2d_all);
}

int n_merges = 0;
struct {
	unsigned long long nq, nd, blkmin, blkmax, total;
} merge_data;
void __output_dip_merge_ratio(struct d_info *dip, void *arg)
{
	double blks_avg;
	char dev_info[15];
	double ratio, q2c_n, d2c_n;

	if (dip->n_qs == 0 || dip->n_ds == 0)
		return;
	else if (dip->n_qs < dip->n_ds)
		dip->n_qs = dip->n_ds;

	q2c_n = dip->n_qs;
	d2c_n = dip->n_ds;
	if (q2c_n > 0.0 && d2c_n > 0.0) {
		if (q2c_n < d2c_n)
			ratio = 1.0;
		else
			ratio = q2c_n / d2c_n;
		blks_avg = (double)dip->avgs.blks.total / d2c_n;
		fprintf((FILE *)arg,
			"%10s | %8llu %8llu %7.1lf | %8llu %8llu %8llu %8llu\n",
			make_dev_hdr(dev_info, 15, dip, 1),
			(unsigned long long)dip->n_qs,
			(unsigned long long)dip->n_ds,
			ratio,
			(unsigned long long)dip->avgs.blks.min,
			(unsigned long long)blks_avg,
			(unsigned long long)dip->avgs.blks.max,
			(unsigned long long)dip->avgs.blks.total);

		if (easy_parse_avgs) {
			fprintf(xavgs_ofp,
				"DMI %s %llu %llu %.9lf %llu %llu %llu %llu\n",
				make_dev_hdr(dev_info, 15, dip, 0),
				(unsigned long long)dip->n_qs,
				(unsigned long long)dip->n_ds,
				ratio,
				(unsigned long long)dip->avgs.blks.min,
				(unsigned long long)blks_avg,
				(unsigned long long)dip->avgs.blks.max,
				(unsigned long long)dip->avgs.blks.total);
		}

		if (n_merges++ == 0) {
			merge_data.blkmin = dip->avgs.blks.min;
			merge_data.blkmax = dip->avgs.blks.max;
		}

		merge_data.nq += dip->n_qs;
		merge_data.nd += dip->n_ds;
		merge_data.total += dip->avgs.blks.total;
		if (dip->avgs.blks.min < merge_data.blkmin)
			merge_data.blkmin = dip->avgs.blks.min;
		if (dip->avgs.blks.max > merge_data.blkmax)
			merge_data.blkmax = dip->avgs.blks.max;
	}
}

void output_dip_merge_ratio(FILE *ofp)
{
	fprintf(ofp, "%10s | %8s %8s %7s | %8s %8s %8s %8s\n", "DEV", "#Q", "#D", "Ratio", "BLKmin", "BLKavg", "BLKmax", "Total");
	fprintf(ofp, "---------- | -------- -------- ------- | -------- -------- -------- --------\n");
	dip_foreach_out(__output_dip_merge_ratio, ofp);
	if (n_merges > 1) {
		fprintf(ofp, "---------- | -------- -------- ------- | -------- -------- -------- --------\n");
		fprintf(ofp, "%10s | %8s %8s %7s | %8s %8s %8s %8s\n", "DEV", "#Q", "#D", "Ratio", "BLKmin", "BLKavg", "BLKmax", "Total");
		fprintf((FILE *)ofp,
			"%10s | %8llu %8llu %7.1lf | %8llu %8llu %8llu %8llu\n",
			"TOTAL", merge_data.nq, merge_data.nd,
			(float)merge_data.nq / (float)merge_data.nd,
			merge_data.blkmin,
			merge_data.total / merge_data.nd,
			merge_data.blkmax, merge_data.total);
	}
	fprintf(ofp, "\n");
}

struct __ohead_data {
	__u64 total;
	int n;
};

struct ohead_data {
	FILE *ofp;
	struct __ohead_data q2g, g2i, q2m, i2d, d2c, q2c;
};

#define __update_odp(odp, dip, fld)					\
	do {								\
		(odp)-> fld .total += dip->avgs. fld . total;		\
		(odp)-> fld .n     += dip->avgs. fld . n;		\
	} while (0)

void __output_dip_prep_ohead(struct d_info *dip, void *arg)
{
	if (dip->avgs.q2c.n > 0 && dip->avgs.q2c.total > 0) {
		char dev_info[15];
		struct ohead_data *odp = arg;
		double q2c_total = (double)(dip->avgs.q2c.total);

		fprintf(odp->ofp,
			"%10s | %8.4lf%% %8.4lf%% %8.4lf%% %8.4lf%% %8.4lf%%\n",
			make_dev_hdr(dev_info, 15, dip, 1),
			100.0 * (double)(dip->avgs.q2g.total) / q2c_total,
			100.0 * (double)(dip->avgs.g2i.total) / q2c_total,
			100.0 * (double)(dip->avgs.q2m.total) / q2c_total,
			100.0 * (double)(dip->avgs.i2d.total) / q2c_total,
			100.0 * (double)(dip->avgs.d2c.total) / q2c_total);

		__update_odp(odp, dip, q2g);
		__update_odp(odp, dip, g2i);
		__update_odp(odp, dip, q2m);
		__update_odp(odp, dip, i2d);
		__update_odp(odp, dip, d2c);
		__update_odp(odp, dip, q2c);
	}
}

#define OD_AVG(od, fld, q2c)						\
	(od. fld .n == 0) ? (double)0.0 :				\
		(100.0 * ((double)((od). fld . total) / q2c))

void output_dip_prep_ohead(FILE *ofp)
{
	double q2c;
	struct ohead_data od;

	memset(&od, 0, sizeof(od));
	od.ofp = ofp;

	fprintf(ofp, "%10s | %9s %9s %9s %9s %9s\n",
				"DEV", "Q2G", "G2I", "Q2M", "I2D", "D2C");
	fprintf(ofp, "---------- | --------- --------- --------- --------- ---------\n");
	dip_foreach_out(__output_dip_prep_ohead, &od);

	if (od.q2g.n == 0 && od.g2i.n == 0 && od.q2m.n == 0 &&
						od.i2d.n == 0 && od.d2c.n == 0)
		goto out;

	q2c = od.q2c.total;
	fprintf(ofp, "---------- | --------- --------- --------- --------- ---------\n");
	fprintf(ofp, "%10s | %8.4lf%% %8.4lf%% %8.4lf%% %8.4lf%% %8.4lf%%\n", "Overall",
			OD_AVG(od, q2g, q2c), OD_AVG(od, g2i, q2c),
			OD_AVG(od, q2m, q2c), OD_AVG(od, i2d, q2c),
			OD_AVG(od, d2c, q2c));

out:
	fprintf(ofp, "\n");
}

struct seek_mode_info {
	struct seek_mode_info *next;
	long long mode;
	int nseeks;
};
struct o_seek_info {
	long long nseeks, median;
	double mean;
	struct seek_mode_info *head;
} seek_info;
int n_seeks;

void output_seek_mode_info(FILE *ofp, struct o_seek_info *sip)
{
	struct seek_mode_info *p, *this, *new_list = NULL;

	while ((this = sip->head) != NULL) {
		sip->head = this->next;
		this->next = NULL;

		if (new_list == NULL || this->nseeks > new_list->nseeks)
			new_list = this;
		else if (this->nseeks == new_list->nseeks) {
			for (p = new_list; p != NULL; p = p->next)
				if (p->mode == this->mode)
					break;

			if (p)
				this->nseeks += p->nseeks;
			else
				this->next = new_list;
			new_list = this;
		}
	}

	fprintf(ofp, "%10s | %15lld %15.1lf %15lld | %lld(%d)",
	        "Average", sip->nseeks, sip->mean / sip->nseeks,
		sip->median / sip->nseeks, new_list->mode, new_list->nseeks);

	if (new_list->next) {
		int i = 0;
		for (p = new_list->next; p != NULL; p = p->next)
			i++;
		fprintf(ofp, "\n%10s   %15s %15s %15s   ...(%d more)\n", "", "", "", "", i);
	}
}

void add_seek_mode_info(struct o_seek_info *sip, struct mode *mp)
{
	int i;
	long long *lp = mp->modes;
	struct seek_mode_info *smip;

	n_seeks++;
	for (i = 0; i < mp->nmds; i++, lp++) {
		for (smip = sip->head; smip; smip = smip->next) {
			if (smip->mode == *lp) {
				smip->nseeks += mp->most_seeks;
				break;
			}
		}
		if (!smip) {
			struct seek_mode_info *new = malloc(sizeof(*new));

			new->next = sip->head;
			sip->head = new;
			new->mode = *lp;
			new->nseeks = mp->most_seeks;

			add_buf(new);
		}
	}
}

static void do_output_dip_seek_info(struct d_info *dip, FILE *ofp, int is_q2q)
{
	double mean;
	int i, nmodes;
	long long nseeks;
	char dev_info[15];
	long long median;
	struct mode m;
	void *handle = is_q2q ? dip->q2q_handle : dip->seek_handle;

	nseeks = seeki_nseeks(handle);
	if (nseeks > 0) {
		mean = seeki_mean(handle);
		median = seeki_median(handle);
		nmodes = seeki_mode(handle, &m);

		fprintf(ofp, "%10s | %15lld %15.1lf %15lld | %lld(%d)",
			make_dev_hdr(dev_info, 15, dip, 1), nseeks, mean,
			median, nmodes > 0 ? m.modes[0] : 0, m.most_seeks);
		if (nmodes > 2)
			fprintf(ofp, "\n%10s   %15s %15s %15s   ...(%d more)\n", "", "", "", "", nmodes-1);
		else  {
			for (i = 1; i < nmodes; i++)
				fprintf(ofp, " %lld", m.modes[i]);
			fprintf(ofp, "\n");
		}

		if (easy_parse_avgs) {
			char *rec = is_q2q ? "QSK" : "DSK";
			fprintf(xavgs_ofp,
				"%s %s %lld %.9lf %lld %lld %d",
				rec, make_dev_hdr(dev_info, 15, dip, 0),
				nseeks, mean, median,
				nmodes > 0 ? m.modes[0] : 0, m.most_seeks);
				for (i = 1; i < nmodes; i++)
					fprintf(xavgs_ofp, " %lld", m.modes[i]);
				fprintf(xavgs_ofp, "\n");
		}

		seek_info.nseeks += nseeks;
		seek_info.mean += (nseeks * mean);
		seek_info.median += (nseeks * median);
		add_seek_mode_info(&seek_info, &m);
		free(m.modes);
	}
}

void __output_dip_seek_info(struct d_info *dip, void *arg)
{
	do_output_dip_seek_info(dip, (FILE *)arg, 0);
}

void __output_dip_q2q_seek_info(struct d_info *dip, void *arg)
{
	do_output_dip_seek_info(dip, (FILE *)arg, 1);
}

void output_dip_seek_info(FILE *ofp)
{
	n_seeks = 1;
	memset(&seek_info, 0, sizeof(seek_info));

	fprintf(ofp, "%10s | %15s %15s %15s | %-15s\n", "DEV", "NSEEKS",
			"MEAN", "MEDIAN", "MODE");
	fprintf(ofp, "---------- | --------------- --------------- --------------- | ---------------\n");
	dip_foreach_out(__output_dip_seek_info, ofp);
	if (n_seeks > 1) {
		fprintf(ofp, "---------- | --------------- --------------- --------------- | ---------------\n");
		fprintf(ofp, "%10s | %15s %15s %15s | %-15s\n",
		        "Overall", "NSEEKS", "MEAN", "MEDIAN", "MODE");
		output_seek_mode_info(ofp, &seek_info);
		fprintf(ofp, "\n");
	}
	fprintf(ofp, "\n");
}

void output_dip_q2q_seek_info(FILE *ofp)
{
	n_seeks = 1;
	memset(&seek_info, 0, sizeof(seek_info));

	fprintf(ofp, "%10s | %15s %15s %15s | %-15s\n", "DEV", "NSEEKS",
			"MEAN", "MEDIAN", "MODE");
	fprintf(ofp, "---------- | --------------- --------------- --------------- | ---------------\n");
	dip_foreach_out(__output_dip_q2q_seek_info, ofp);
	if (n_seeks > 1) {
		fprintf(ofp, "---------- | --------------- --------------- --------------- | ---------------\n");
		fprintf(ofp, "%10s | %15s %15s %15s | %-15s\n",
		        "Overall", "NSEEKS", "MEAN", "MEDIAN", "MODE");
		output_seek_mode_info(ofp, &seek_info);
		fprintf(ofp, "\n");
	}
	fprintf(ofp, "\n");
}

struct __opa {
	FILE *ofp;
	ai_pip_t (*func)(struct p_info *);
};

void __output_pip_avg(struct p_info *pip, void *arg)
{
	struct __opa *opap = arg;
	ai_pip_t ap = opap->func(pip);

	if (ap->n > 0) {
		char proc_name[15];
		snprintf(proc_name, 15, "%s", pip->name);

		ap->avg = BIT_TIME(ap->total) / (double)ap->n;
		__output_avg(opap->ofp, proc_name, ap, 0);
	}
}

void output_pip_avg(FILE *ofp, char *hdr, ai_pip_t (*func)(struct p_info *))
{
	struct __opa opa = { .ofp = ofp, .func = func };

	output_hdr(ofp, hdr);
	pip_foreach_out(__output_pip_avg, &opa);
	fprintf(ofp, "\n");
}

int n_plugs;
struct plug_info {
	long n_plugs, n_unplugs_t;
	double t_percent;
} plug_info;

void __dip_output_plug(struct d_info *dip, void *arg)
{
	char dev_info[15];
	FILE *ofp = arg;
	double delta, pct;

	if (dip->is_plugged)
		dip_unplug(dip->device, dip->end_time, 0);
	if ((dip->nplugs + dip->nplugs_t) > 0) {
		delta = dip->end_time - dip->start_time;
		pct = 100.0 * (dip->plugged_time / delta);

		fprintf(ofp, "%10s | %10d(%10d) | %13.9lf%%\n",
			make_dev_hdr(dev_info, 15, dip, 1),
			dip->nplugs, dip->nplugs_t, pct);

		if (easy_parse_avgs) {
			fprintf(xavgs_ofp,
				"PLG %s %d %d %.9lf\n",
				make_dev_hdr(dev_info, 15, dip, 0),
				dip->nplugs, dip->nplugs_t, pct);
		}

		n_plugs++;
		plug_info.n_plugs += dip->nplugs;
		plug_info.n_unplugs_t += dip->nplugs_t;
		plug_info.t_percent += pct;
	}
}

void __dip_output_plug_all(FILE *ofp, struct plug_info *p)
{
	fprintf(ofp, "---------- | ---------- ----------  | ----------------\n");
	fprintf(ofp, "%10s | %10s %10s  | %s\n",
	        "Overall", "# Plugs", "# Timer Us", "% Time Q Plugged");
	fprintf(ofp, "%10s | %10ld(%10ld) | %13.9lf%%\n", "Average",
	        p->n_plugs / n_plugs, p->n_unplugs_t / n_plugs,
		p->t_percent / n_plugs);

}

__u64 n_nios_uplugs, n_nios_uplugs_t;
struct nios_plug_info {
	__u64 tot_nios_up, tot_nios_up_t;
} nios_plug_info;

void __dip_output_plug_nios(struct d_info *dip, void *arg)
{
	char dev_info[15];
	FILE *ofp = arg;
	double a_nios_uplug = 0.0, a_nios_uplug_t = 0.0;

	if (dip->nios_up && dip->nplugs) {
		a_nios_uplug = (double)dip->nios_up / (double)dip->nplugs;
		n_nios_uplugs += dip->nplugs;
		nios_plug_info.tot_nios_up += dip->nios_up;
	}
	if (dip->nios_upt && dip->nplugs_t) {
		a_nios_uplug_t = (double)dip->nios_upt / (double)dip->nplugs_t;
		n_nios_uplugs_t += dip->nplugs_t;
		nios_plug_info.tot_nios_up_t += dip->nios_upt;
	}

	fprintf(ofp, "%10s | %10.1lf   %10.1lf\n",
		make_dev_hdr(dev_info, 15, dip, 1),
		a_nios_uplug, a_nios_uplug_t);

	if (easy_parse_avgs) {
		fprintf(xavgs_ofp,
			"UPG %s %.9lf %.9lf\n",
			make_dev_hdr(dev_info, 15, dip, 0),
			a_nios_uplug, a_nios_uplug_t);
	}
}

void __dip_output_uplug_all(FILE *ofp, struct nios_plug_info *p)
{
	double ios_unp = 0.0, ios_unp_to = 0.0;

	if (n_nios_uplugs)
		ios_unp = (double)p->tot_nios_up / (double)n_nios_uplugs;
	if (n_nios_uplugs_t)
		ios_unp_to = (double)p->tot_nios_up_t / (double)n_nios_uplugs_t;

	fprintf(ofp, "---------- | ----------   ----------\n");
	fprintf(ofp, "%10s | %10s   %10s\n",
		"Overall", "IOs/Unp", "IOs/Unp(to)");
	fprintf(ofp, "%10s | %10.1lf   %10.1lf\n",
		"Average", ios_unp, ios_unp_to);
}

void output_plug_info(FILE *ofp)
{
	fprintf(ofp, "%10s | %10s %10s  | %s\n",
	        "DEV", "# Plugs", "# Timer Us", "% Time Q Plugged");
	fprintf(ofp, "---------- | ---------- ----------  | ----------------\n");
	dip_foreach_out(__dip_output_plug, ofp);
	if (n_plugs > 1)
		__dip_output_plug_all(ofp, &plug_info);
	fprintf(ofp, "\n");

	fprintf(ofp, "%10s | %10s   %10s\n",
		"DEV", "IOs/Unp", "IOs/Unp(to)");
	fprintf(ofp, "---------- | ----------   ----------\n");
	dip_foreach_out(__dip_output_plug_nios, ofp);
	if (n_nios_uplugs || n_nios_uplugs_t)
		__dip_output_uplug_all(ofp, &nios_plug_info);
	fprintf(ofp, "\n");
}

int n_actQs;
struct actQ_info {
	__u64 t_qs;
	__u64 t_act_qs;
} actQ_info;

void __dip_output_actQ(struct d_info *dip, void *arg)
{
	if (dip->n_qs > 0 && !remapper_dev(dip->device)) {
		char dev_info[15];
		double a_actQs = (double)dip->t_act_q / (double)dip->n_qs;

		fprintf((FILE *)arg, "%10s | %13.1lf\n",
			make_dev_hdr(dev_info, 15, dip, 1), a_actQs);

		if (easy_parse_avgs) {
			fprintf(xavgs_ofp,
				"ARQ %s %.9lf\n",
				make_dev_hdr(dev_info, 15, dip, 0), a_actQs);
		}

		n_actQs++;
		actQ_info.t_qs += dip->n_qs;
		actQ_info.t_act_qs += dip->t_act_q;
	}
}

void __dip_output_actQ_all(FILE *ofp, struct actQ_info *p)
{
	fprintf(ofp, "---------- | -------------\n");
	fprintf(ofp, "%10s | %13s\n", "Overall", "Avgs Reqs @ Q");
	fprintf(ofp, "%10s | %13.1lf\n", "Average",
		(double)p->t_act_qs / (double)p->t_qs);
}

void output_actQ_info(FILE *ofp)
{
	fprintf(ofp, "%10s | %13s\n", "DEV", "Avg Reqs @ Q");
	fprintf(ofp, "---------- | -------------\n");
	dip_foreach_out(__dip_output_actQ, ofp);
	if (n_actQs > 1)
		__dip_output_actQ_all(ofp, &actQ_info);
	fprintf(ofp, "\n");
}

void output_histos(void)
{
	int i;
	FILE *ofp;
	char fname[256];

	if (output_name == NULL) return;

	sprintf(fname, "%s_qhist.dat", output_name);
	ofp = my_fopen(fname, "w");
	if (!ofp) {
		perror(fname);
		return;
	}

	fprintf(ofp, "# BTT histogram data\n");
	fprintf(ofp, "# Q buckets\n");
	for (i = 0; i < (N_HIST_BKTS-1); i++)
		fprintf(ofp, "%4d %lld\n", (i+1), (long long)q_histo[i]);
	fprintf(ofp, "\n# Q bucket for > %d\n%4d %lld\n", (int)N_HIST_BKTS-1,
		N_HIST_BKTS-1, (long long)q_histo[N_HIST_BKTS-1]);
	fclose(ofp);

	sprintf(fname, "%s_dhist.dat", output_name);
	ofp = my_fopen(fname, "w");
	if (!ofp) {
		perror(fname);
		return;
	}
	fprintf(ofp, "# D buckets\n");
	for (i = 0; i < (N_HIST_BKTS-1); i++)
		fprintf(ofp, "%4d %lld\n", (i+1), (long long)d_histo[i]);
	fprintf(ofp, "\n# D bucket for > %d\n%4d %lld\n", (int)N_HIST_BKTS-1,
		N_HIST_BKTS-1, (long long)d_histo[N_HIST_BKTS-1]);
	fclose(ofp);
}

int output_avgs(FILE *ofp)
{
	if (output_all_data) {
		if (exes == NULL || *exes != '\0') {
			output_section_hdr(ofp, "Per Process");
			output_pip_avg(ofp, "Q2Qdm", pip_q2q_dm_avg);
			output_pip_avg(ofp, "Q2Adm", pip_q2a_dm_avg);
			output_pip_avg(ofp, "Q2Cdm", pip_q2c_dm_avg);
			fprintf(ofp, "\n");

			output_pip_avg(ofp, "Q2Q", pip_q2q_avg);
			output_pip_avg(ofp, "Q2A", pip_q2a_avg);
			output_pip_avg(ofp, "Q2G", pip_q2g_avg);
			output_pip_avg(ofp, "S2G", pip_s2g_avg);
			output_pip_avg(ofp, "G2I", pip_g2i_avg);
			output_pip_avg(ofp, "Q2M", pip_q2m_avg);
			output_pip_avg(ofp, "I2D", pip_i2d_avg);
			output_pip_avg(ofp, "D2C", pip_d2c_avg);
			output_pip_avg(ofp, "Q2C", pip_q2c_avg);
		}

		output_section_hdr(ofp, "Per Device");
		output_dip_avg(ofp, "Q2Qdm", dip_q2q_dm_avg);
		output_dip_avg(ofp, "Q2Adm", dip_q2a_dm_avg);
		output_dip_avg(ofp, "Q2Cdm", dip_q2c_dm_avg);
		fprintf(ofp, "\n");

		output_dip_avg(ofp, "Q2Q", dip_q2q_avg);
		output_dip_avg(ofp, "Q2A", dip_q2a_avg);
		output_dip_avg(ofp, "Q2G", dip_q2g_avg);
		output_dip_avg(ofp, "S2G", dip_s2g_avg);
		output_dip_avg(ofp, "G2I", dip_g2i_avg);
		output_dip_avg(ofp, "Q2M", dip_q2m_avg);
		output_dip_avg(ofp, "I2D", dip_i2d_avg);
		output_dip_avg(ofp, "D2C", dip_d2c_avg);
		output_dip_avg(ofp, "Q2C", dip_q2c_avg);
	}

	output_section_hdr(ofp, "All Devices");
	output_hdr(ofp, "ALL");
	__output_avg(ofp, "Q2Qdm", &all_avgs.q2q_dm, 0);
	__output_avg(ofp, "Q2Adm", &all_avgs.q2a_dm, 0);
	__output_avg(ofp, "Q2Cdm", &all_avgs.q2c_dm, 0);
	fprintf(ofp, "\n");

	__output_avg(ofp, "Q2Q", &all_avgs.q2q, 1);
	__output_avg(ofp, "Q2A", &all_avgs.q2a, 1);
	__output_avg(ofp, "Q2G", &all_avgs.q2g, 1);
	__output_avg(ofp, "S2G", &all_avgs.s2g, 1);
	__output_avg(ofp, "G2I", &all_avgs.g2i, 1);
	__output_avg(ofp, "Q2M", &all_avgs.q2m, 1);
	__output_avg(ofp, "I2D", &all_avgs.i2d, 1);
	__output_avg(ofp, "M2D", &all_avgs.m2d, 1);
	__output_avg(ofp, "D2C", &all_avgs.d2c, 1);
	__output_avg(ofp, "Q2C", &all_avgs.q2c, 1);
	fprintf(ofp, "\n");

	output_section_hdr(ofp, "Device Overhead");
	output_dip_prep_ohead(ofp);

	output_section_hdr(ofp, "Device Merge Information");
	output_dip_merge_ratio(ofp);

	output_section_hdr(ofp, "Device Q2Q Seek Information");
	output_dip_q2q_seek_info(ofp);

	output_section_hdr(ofp, "Device D2D Seek Information");
	output_dip_seek_info(ofp);

	output_section_hdr(ofp, "Plug Information");
	output_plug_info(ofp);

	output_section_hdr(ofp, "Active Requests At Q Information");
	output_actQ_info(ofp);

	output_histos();

	if (output_all_data) {
		output_section_hdr(ofp, "Q2D Histogram");
		output_q2d_histo(ofp);
	}

	return 0;
}

void __output_ranges(FILE *ofp, struct list_head *head_p, float base)
{
	struct range_info *rip;
	struct list_head *p;
	float limit = base + 0.4;

	__list_for_each(p, head_p) {
		rip = list_entry(p, struct range_info, head);
		fprintf(ofp, "%13.9lf %5.1f\n", BIT_TIME(rip->start), base);
		fprintf(ofp, "%13.9lf %5.1f\n", BIT_TIME(rip->start), limit);
		fprintf(ofp, "%13.9lf %5.1f\n", BIT_TIME(rip->end), limit);
		fprintf(ofp, "%13.9lf %5.1f\n", BIT_TIME(rip->end), base);
	}
}

int output_regions(FILE *ofp, char *header, struct region_info *reg,
			  float base)
{
	if (list_len(&reg->qranges) == 0 && list_len(&reg->cranges) == 0)
		return 0;

	fprintf(ofp, "# %16s : q activity\n", header);
	__output_ranges(ofp, &reg->qranges, base);
	fprintf(ofp, "\n");

	fprintf(ofp, "# %16s : c activity\n", header);
	__output_ranges(ofp, &reg->cranges, base + 0.5);
	fprintf(ofp, "\n");

	return 1;
}

struct __od {
	FILE *ofp;
	float base;
};
void __output_dev(struct d_info *dip, void *arg)
{
	char header[128];
	struct __od *odp = arg;

	sprintf(header, "%d,%d", MAJOR(dip->device), MINOR(dip->device));
	if (output_regions(odp->ofp, header, &dip->regions, odp->base))
		odp->base += 1.0;
}

float output_devs(FILE *ofp, float base)
{
	struct __od od = { .ofp = ofp, .base = base };

	fprintf(ofp, "# Per device\n" );
	dip_foreach_out(__output_dev, &od);
	return od.base;
}

static inline int exe_match(char *exe, char *name)
{
	return (exe == NULL) || (strstr(name, exe) != NULL);
}

struct __op {
	FILE *ofp;
	float base;
};
void __output_procs(struct p_info *pip, void *arg)
{
	struct __op *opp = arg;
	output_regions(opp->ofp, pip->name, &pip->regions, opp->base);
	opp->base += 1.0;
}

float output_procs(FILE *ofp, float base)
{
	struct __op op = { .ofp = ofp, .base = base };

	fprintf(ofp, "# Per process\n" );
	pip_foreach_out(__output_procs, &op);
	return op.base;
}

int output_ranges(FILE *ofp)
{
	float base = 0.0;

	fprintf(ofp, "# %s\n", "Total System");
	if (output_regions(ofp, "Total System", &all_regions, base))
		base += 1.0;

	if (n_devs > 1)
		base = output_devs(ofp, base);

	base = output_procs(ofp, base);

	return 0;
}
