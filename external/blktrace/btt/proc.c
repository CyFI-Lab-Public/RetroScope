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
#include <string.h>

#include "globals.h"

struct pn_info {
	struct rb_node rb_node;
	struct p_info *pip;
	union {
		char *name;
		__u32 pid;
	}  u;
};

struct rb_root root_pid, root_name;

static void __foreach(struct rb_node *n, void (*f)(struct p_info *, void *),
			void *arg)
{
	if (n) {
		__foreach(n->rb_left, f, arg);
		f(rb_entry(n, struct pn_info, rb_node)->pip, arg);
		__foreach(n->rb_right, f, arg);
	}
}

static void __destroy(struct rb_node *n, int free_name, int free_pip)
{
	if (n) {
		struct pn_info *pnp = rb_entry(n, struct pn_info, rb_node);

		__destroy(n->rb_left, free_name, free_pip);
		__destroy(n->rb_right, free_name, free_pip);

		if (free_name)
			free(pnp->u.name);
		if (free_pip) {
			free(pnp->pip->name);
			region_exit(&pnp->pip->regions);
			free(pnp->pip);
		}
		free(pnp);
	}
}

struct p_info * __find_process_pid(__u32 pid)
{
	struct pn_info *this;
	struct rb_node *n = root_pid.rb_node;

	while (n) {
		this = rb_entry(n, struct pn_info, rb_node);
		if (pid < this->u.pid)
			n = n->rb_left;
		else if (pid > this->u.pid)
			n = n->rb_right;
		else
			return this->pip;
	}

	return NULL;
}

struct p_info *__find_process_name(char *name)
{
	int cmp;
	struct pn_info *this;
	struct rb_node *n = root_name.rb_node;

	while (n) {
		this = rb_entry(n, struct pn_info, rb_node);
		cmp = strcmp(name, this->u.name);
		if (cmp < 0)
			n = n->rb_left;
		else if (cmp > 0)
			n = n->rb_right;
		else
			return this->pip;
	}

	return NULL;
}

static void insert_pid(struct p_info *that, __u32 pid)
{
	struct pn_info *this;
	struct rb_node *parent = NULL;
	struct rb_node **p = &root_pid.rb_node;

	while (*p) {
		parent = *p;
		this = rb_entry(parent, struct pn_info, rb_node);

		if (pid < this->u.pid)
			p = &(*p)->rb_left;
		else if (pid > this->u.pid)
			p = &(*p)->rb_right;
		else
			return;	// Already there
	}

	this = malloc(sizeof(struct pn_info));
	this->u.pid = pid;
	this->pip = that;

	rb_link_node(&this->rb_node, parent, p);
	rb_insert_color(&this->rb_node, &root_pid);
}

static void insert_name(struct p_info *that)
{
	int cmp;
	struct pn_info *this;
	struct rb_node *parent = NULL;
	struct rb_node **p = &root_name.rb_node;

	while (*p) {
		parent = *p;
		this = rb_entry(parent, struct pn_info, rb_node);
		cmp = strcmp(that->name, this->u.name);

		if (cmp < 0)
			p = &(*p)->rb_left;
		else if (cmp > 0)
			p = &(*p)->rb_right;
		else
			return;	// Already there...
	}

	this = malloc(sizeof(struct pn_info));
	this->u.name = strdup(that->name);
	this->pip = that;

	rb_link_node(&this->rb_node, parent, p);
	rb_insert_color(&this->rb_node, &root_name);
}

static void insert(struct p_info *pip)
{
	insert_pid(pip, pip->pid);
	insert_name(pip);
}

static inline struct p_info *pip_alloc(void)
{
	return memset(malloc(sizeof(struct p_info)), 0, sizeof(struct p_info));
}

struct p_info *find_process(__u32 pid, char *name)
{
	struct p_info *pip;

	if (pid != ((__u32)-1)) {
		if ((pip = __find_process_pid(pid)) != NULL)
			return pip;
		else if (name) {
			pip = __find_process_name(name);

			if (pip && pid != pip->pid) {
				/*
				 * This is a process with the same name
				 * as another, but a different PID.
				 *
				 * We'll store a reference in the PID
				 * tree...
				 */
				insert_pid(pip, pid);
			}
			return pip;
		}

		/*
		 * We're here because we have a pid, and no name, but
		 * we didn't find a process ...
		 *
		 * We'll craft one using the pid...
		 */

		name = alloca(256);
		sprintf(name, "pid%09u", pid);
		process_alloc(pid, name);
		return __find_process_pid(pid);
	}

	return __find_process_name(name);
}

void process_alloc(__u32 pid, char *name)
{
	struct p_info *pip = find_process(pid, name);

	if (pip == NULL) {
		pip = pip_alloc();
		pip->pid = pid;
		region_init(&pip->regions);
		pip->last_q = (__u64)-1;
		pip->name = strdup(name);

		insert(pip);
	}
}

void pip_update_q(struct io *iop)
{
	if (iop->pip) {
		if (remapper_dev(iop->dip->device))
			update_lq(&iop->pip->last_q, &iop->pip->avgs.q2q_dm,
								iop->t.time);
		else
			update_lq(&iop->pip->last_q, &iop->pip->avgs.q2q,
								iop->t.time);
		update_qregion(&iop->pip->regions, iop->t.time);
	}
}

void pip_foreach_out(void (*f)(struct p_info *, void *), void *arg)
{
	if (exes == NULL)
		__foreach(root_name.rb_node, f, arg);
	else {
		struct p_info *pip;
		char *exe, *p, *next, *exes_save = strdup(exes);

		p = exes_save;
		while (exes_save != NULL) {
			exe = exes_save;
			if ((next = strchr(exes_save, ',')) != NULL) {
				*next = '\0';
				exes_save = next+1;
			} else
				exes_save = NULL;

			pip = __find_process_name(exe);
			if (pip)
				f(pip, arg);
		}
	}
}

void pip_exit(void)
{
	__destroy(root_pid.rb_node, 0, 0);
	__destroy(root_name.rb_node, 1, 1);
}
