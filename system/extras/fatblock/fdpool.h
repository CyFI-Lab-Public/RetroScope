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

#ifndef FDPOOL_H
#define FDPOOL_H

struct pooled_fd {
  struct pooled_fd *prev;
  struct pooled_fd *next;
  int fd;
};

void fdpool_init(struct pooled_fd *pfd);
int fdpool_open(struct pooled_fd *pfd, const char *pathname, int flags);
void fdpool_close(struct pooled_fd *pfd);

#endif
