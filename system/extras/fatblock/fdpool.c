/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <utils.h>

#include "fdpool.h"

#define INVALID_FD (-1)
#define FDPOOL_SIZE 4

static struct pooled_fd fdpool_head = {
	.fd = INVALID_FD,
	.prev = &fdpool_head,
	.next = &fdpool_head
};
static int fdpool_count = 0;

static void fdpool_insert_head(struct pooled_fd *node)
{
	struct pooled_fd *prev = &fdpool_head;
	struct pooled_fd *next = prev->next;

	assert(node);

	prev->next = node;
	node->prev = prev;
	node->next = next;
	next->prev = node;

	fdpool_count++;
}

static void fdpool_remove(struct pooled_fd *node)
{
	struct pooled_fd *prev = node->prev;
	struct pooled_fd *next = node->next;

	assert(prev);
	assert(next);

	prev->next = next;
	next->prev = prev;

	fdpool_count--;
}

static struct pooled_fd *fdpool_remove_tail(void)
{
	struct pooled_fd *tail = fdpool_head.prev;

	assert(tail != &fdpool_head);

	fdpool_remove(tail);

	return tail;
}

static void fdpool_clear(struct pooled_fd *pfd)
{
	assert(pfd);

	pfd->fd = INVALID_FD;
	pfd->prev = pfd->next = NULL;
}

static void fdpool_unpool(struct pooled_fd *pfd)
{
	close(pfd->fd);
	fdpool_clear(pfd);
}

static void fdpool_evict(void)
{
	struct pooled_fd *tail;

	tail = fdpool_remove_tail();
	fdpool_unpool(tail);
}

static void fdpool_pool(struct pooled_fd *pfd, int fd)
{
	if (fdpool_count >= FDPOOL_SIZE)
		fdpool_evict();

	fdpool_insert_head(pfd);
	pfd->fd = fd;
}

static void fdpool_touch(struct pooled_fd *pfd)
{
	fdpool_remove(pfd);
	fdpool_insert_head(pfd);
}



void fdpool_init(struct pooled_fd *pfd)
{
	fdpool_clear(pfd);
}

int fdpool_open(struct pooled_fd *pfd, const char *pathname, int flags)
{
	int open_errno;
	int fd;

	if (pfd->fd != INVALID_FD) {
		fdpool_touch(pfd);
		return pfd->fd;
	}

	fd = open(pathname, flags);
	open_errno = errno;

	if (fd >= 0) {
		fdpool_pool(pfd, fd);
	}

	errno = open_errno;
	return fd;
}

void fdpool_close(struct pooled_fd *pfd)
{
	assert(pfd);

	fdpool_unpool(pfd);
}
