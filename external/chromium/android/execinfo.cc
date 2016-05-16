// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "execinfo.h"

int backtrace(void **array, int size) { return 0; }

char **backtrace_symbols(void *const *array, int size) { return 0; }

void backtrace_symbols_fd (void *const *array, int size, int fd) {}


