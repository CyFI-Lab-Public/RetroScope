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

#ifndef UBLOCK_UBLOCK_H
#define UBLOCK_UBLOCK_H

#include <stdint.h>
#include <sys/types.h>

#define UBLOCK_VERSION 0

struct ublock_ctx;

struct ublock_ops {
  int (*read)(char *buf, uint64_t length, uint64_t offset);
  int (*write)(const char *buf, uint64_t length, uint64_t offset);
};

/*
 * Initializes a ublock block device.
 *
 * May call your read and write functions as the kernel scans partition
 * tables.  Will return once the kernel has finished adding the block device.
 *
 * Returns 0 on success,
 *   -ENOMEM if we ran out of memory,
 *   -ENOENT if the kernel appears to lack ublock support, and
 *   -EPROTO if the kernel did something unexpected.
 */
int ublock_init(struct ublock_ctx **ub, struct ublock_ops *ops, uint64_t size);

/*
 * Returns the index of a ublock block device.
 *
 * Returns -EFAULT if the context is NULL,
 *         -EINVAL if the context is invalid.
 */
int ublock_index(struct ublock_ctx *ub);

/*
 * Runs a loop waiting for ublock requests and calling the ops callbacks.
 *
 * Returns 0 if the loop was stopped by ublock_stop,
 *   -EPROTO if the loop was stopped by a protocol error,
 */
int ublock_run(struct ublock_ctx *ub);

/*
 * Stops ublock_run, if it is running.  Should be called from one of the ops
 * callbacks.
 */
void ublock_stop(struct ublock_ctx *ub);

/*
 * Destroys a ublock block device.
 */
void ublock_destroy(struct ublock_ctx *ub);

#endif
