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
#include <linux/ublock.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <ublock/ublock.h>

#define CONTROL_FILE "/dev/ublockctl"

struct ublock_ctx {
	struct ublock_ops *ops;

	uint32_t index;
	uint64_t size;
	uint32_t max_buf;

	char *in_buf;
	char *out_buf;

	int flags;
	int fd;
	int fails;
};

#define CTX_INITED  0x1
#define CTX_READY   0x2
#define CTX_RUNNING 0x4

#define MAX_BUF 65536

#define MAX_FAILURES 10

static inline void ublock_succeed(struct ublock_ctx *ub_ctx)
{
	assert(ub_ctx);

	ub_ctx->fails = 0;
}

static void ublock_fail(struct ublock_ctx *ub_ctx)
{
	assert(ub_ctx);

	ub_ctx->fails++;
	if (ub_ctx->fails > MAX_FAILURES)
		ublock_stop(ub_ctx);
}

static int ublock_handle_init(struct ublock_ctx *ub_ctx,
                              const void *in, size_t in_len,
                              void *out, size_t *out_len)
{
	const struct ublock_init_in *in_h;
	struct ublock_init_out *out_h;

	assert(ub_ctx);
	assert(in);
	assert(out);

	if (in_len != sizeof(*in_h))
		return -EPROTO;

	in_h = (const struct ublock_init_in *)in;

	if (in_h->version != UBLOCK_VERSION)
		return -EPROTO;

	out_h = (struct ublock_init_out *)out;
	out_h->version = UBLOCK_VERSION;
	out_h->size = ub_ctx->size;
	if (in_h->max_buf < MAX_BUF)
		ub_ctx->max_buf = in_h->max_buf;
	else
		ub_ctx->max_buf = MAX_BUF;
	out_h->max_buf = ub_ctx->max_buf;

	*out_len = sizeof(*out_h);

	ub_ctx->index = in_h->index;
	ub_ctx->flags |= CTX_INITED;

	return 0;
}

static int ublock_handle_ready(struct ublock_ctx *ub_ctx,
                               const void *in, size_t in_len,
                               void *out, size_t *out_len)
{
	struct ublock_ready_out *out_h;

	assert(ub_ctx);
	assert(in);
	assert(out);

	if (in_len != sizeof(struct ublock_ready_in))
		return -EPROTO;

	*out_len = sizeof(struct ublock_ready_out);

	ub_ctx->flags |= CTX_READY;

	return 0;
}

static int ublock_handle_read(struct ublock_ctx *ub_ctx,
                              const void *in, size_t in_len,
                              void *out, size_t *out_len)
{
	const struct ublock_read_in *in_h;
	struct ublock_read_out *out_h;
	char *out_buf;

	assert(ub_ctx);
	assert(in);
	assert(out);

	if (in_len != sizeof(*in_h))
		return -EPROTO;

	in_h = (const struct ublock_read_in *)in;

	out_h = (struct ublock_read_out *)out;
	out_buf = (char *)(out_h + 1);

	out_h->status = (ub_ctx->ops->read)(out_buf, in_h->length, in_h->offset);

	if (out_h->status >= 0)
		*out_len = sizeof(*out_h) + in_h->length;
	else
		*out_len = sizeof(*out_h);

	return 0;
}

static int ublock_handle_write(struct ublock_ctx *ub_ctx,
                               const void *in, size_t in_len,
                               void *out, size_t *out_len)
{
	const struct ublock_write_in *in_h;
	const char *in_buf;
	struct ublock_write_out *out_h;

	assert(ub_ctx);
	assert(in);
	assert(out);

	if (in_len < sizeof(*in_h))
		return -EPROTO;

	in_h = (const struct ublock_write_in *)in;
	in_buf = (const char*)(in_h + 1);

	out_h = (struct ublock_write_out *)out;
	*out_len = sizeof(*out_h);

	out_h->status = (ub_ctx->ops->write)(in_buf, in_h->length, in_h->offset);

	return 0;
}

static int ublock_handle_request(struct ublock_ctx *ub_ctx,
                                 const void *in, size_t in_len,
                                 void *out, size_t *out_len)
{
	const struct ublock_in_header *in_h;
	const void *in_buf;
	size_t in_buf_len;

	struct ublock_out_header *out_h;
	void *out_buf;
	size_t out_buf_len;

	int result;
	int (*handle_fn)(struct ublock_ctx *, const void *, size_t, void *, size_t *);

	assert(ub_ctx);
	assert(in);
	assert(out);

	if (in_len < sizeof(*in_h))
		return -EPROTO;

	in_h = (const struct ublock_in_header *)in;
	in_buf = in_h + 1;
	in_buf_len = in_len - sizeof(*in_h);

	out_h = (struct ublock_out_header *)out;
	out_buf = out_h + 1;

	switch (in_h->opcode) {
	case UBLOCK_INIT_IN:
		out_h->opcode = UBLOCK_INIT_OUT;
		handle_fn = &ublock_handle_init;
		break;
	case UBLOCK_READY_IN:
		out_h->opcode = UBLOCK_READY_OUT;
		handle_fn = &ublock_handle_ready;
		break;
	case UBLOCK_READ_IN:
		out_h->opcode = UBLOCK_READ_OUT;
		handle_fn = &ublock_handle_read;
		break;
	case UBLOCK_WRITE_IN:
		out_h->opcode = UBLOCK_WRITE_OUT;
		handle_fn = &ublock_handle_write;
		break;
	default:
		return -EPROTO;
	}

	out_h->seq = in_h->seq;
	result = (handle_fn)(ub_ctx, in_buf, in_buf_len, out_buf, &out_buf_len);
	*out_len = sizeof(*out_h) + out_buf_len;

	return result;
}

static int ublock_do_request(struct ublock_ctx *ub_ctx,
			     void *in_buf, size_t in_size,
			     void *out_buf, size_t out_size)
{
	size_t out_len;
	ssize_t in_len, out_wrote;
	int result;

	assert(ub_ctx);
	assert(in_buf);
	assert(out_buf);

	in_len = read(ub_ctx->fd, in_buf, in_size);
	if (in_len < 0)
		return -EPROTO;

	result = ublock_handle_request(ub_ctx, in_buf, in_len, out_buf, &out_len);

	assert(out_len <= out_size);

	out_wrote = write(ub_ctx->fd, out_buf, out_len);

	if (out_wrote < out_len)
		return -EPROTO;

	if (result)
		ublock_fail(ub_ctx);
	else
		ublock_succeed(ub_ctx);

	return result;
}

#define EXIT_READY 1
#define EXIT_STOPPED 2
#define EXIT_FAIL 4

static int ublock_loop(struct ublock_ctx *ub_ctx, int exit_cond)
{
	size_t in_len, out_len;
	int result;

	result = 0;
	while (((exit_cond & EXIT_READY) && (!(ub_ctx->flags & CTX_READY))) ||
	       ((exit_cond & EXIT_STOPPED) && (ub_ctx->flags & CTX_RUNNING)) ||
	       ((exit_cond & EXIT_FAIL) && (result))) {
		result = ublock_do_request(ub_ctx,
		                           ub_ctx->in_buf, ub_ctx->max_buf,
		                           ub_ctx->out_buf, ub_ctx->max_buf);
		if (result)
			return result;
	}

	return 0;
}

int ublock_run(struct ublock_ctx *ub_ctx)
{
	if (!ub_ctx)
		return -EFAULT;

	if (!(ub_ctx->flags & CTX_INITED))
		return -EINVAL;

	ub_ctx->flags |= CTX_RUNNING;
	ublock_loop(ub_ctx, EXIT_STOPPED);

	return 0;
}

void ublock_stop(struct ublock_ctx *ub_ctx)
{
	if (!ub_ctx)
		return;

	if (!(ub_ctx->flags & CTX_INITED))
		return;

	ub_ctx->flags &= ~CTX_RUNNING;
}

int ublock_index(struct ublock_ctx *ub_ctx)
{
	if (!ub_ctx)
		return -EFAULT;

	if (!(ub_ctx->flags & CTX_INITED))
		return -EINVAL;

	return ub_ctx->index;
}

static inline size_t ublock_init_buf_size(void)
{
	size_t in_size = sizeof(struct ublock_in_header) +
			 sizeof(struct ublock_init_in);
	size_t out_size = sizeof(struct ublock_out_header) +
			  sizeof(struct ublock_init_out);

	return (in_size > out_size) ? in_size : out_size;
}

int ublock_init(struct ublock_ctx **ub_ctx_out, struct ublock_ops *ops,
		uint64_t dev_size)
{
	struct ublock_ctx *ub_ctx;
	char *in_buf, *out_buf;
	size_t size;
	int result;

	if (!ub_ctx_out || !ops)
		return -EFAULT;

	in_buf = out_buf = NULL;

	ub_ctx = malloc(sizeof(struct ublock_ctx));
	if (!ub_ctx) {
		result = -ENOMEM;
		goto error;
	}

	size = ublock_init_buf_size();
	in_buf = malloc(size);
	out_buf = malloc(size);
	if (!(in_buf && out_buf)) {
		result = -ENOMEM;
		goto error;
	}

	ub_ctx->ops = ops;
	ub_ctx->size = dev_size;
	ub_ctx->max_buf = 0;
	ub_ctx->flags = 0;

	ub_ctx->fd = open(CONTROL_FILE, O_RDWR);
	if (ub_ctx->fd < 0) {
		result = -ENOENT;
		goto error;
	}

	result = ublock_do_request(ub_ctx, in_buf, size, out_buf, size);
	if (result) {
		result = -EPROTO;
		goto error;
	}
	if (!ub_ctx->flags & CTX_INITED) {
		result = -EPROTO;
		goto error;
	}

	free(in_buf);
	in_buf = NULL;
	free(out_buf);
	out_buf = NULL;

	ub_ctx->in_buf = malloc(ub_ctx->max_buf);
	ub_ctx->out_buf = malloc(ub_ctx->max_buf);
	if (!(ub_ctx->in_buf && ub_ctx->out_buf)) {
		result = -ENOMEM;
		goto error;
	}

	ublock_loop(ub_ctx, EXIT_READY);

	*ub_ctx_out = ub_ctx;

	return 0;

error:
	if (ub_ctx) {
		if (ub_ctx->in_buf)
			free(ub_ctx->in_buf);
		if (ub_ctx->out_buf)
			free(ub_ctx->out_buf);
		if (ub_ctx->fd)
			close(ub_ctx->fd);
		free(ub_ctx);
	}
	if (in_buf)
		free(in_buf);
	if (out_buf)
		free(out_buf);

	return result;
}

void ublock_destroy(struct ublock_ctx *ub_ctx)
{
	if (!ub_ctx)
		return;
	
	close(ub_ctx->fd);
	free(ub_ctx);
}
