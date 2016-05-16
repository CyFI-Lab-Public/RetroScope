/*
 * Copyright 2011-2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef LIBRSLOADER_H
#define LIBRSLOADER_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

struct RSExecOpaque;
typedef struct RSExecOpaque *RSExecRef;
typedef void *(*RSFindSymbolFn)(void *, char const *);

RSExecRef rsloaderCreateExec(unsigned char const *buf,
                             size_t buf_size,
                             RSFindSymbolFn find_symbol,
                             void *find_symbol_context);

RSExecRef rsloaderLoadExecutable(unsigned char const *buf,
                                 size_t buf_size);

int rsloaderRelocateExecutable(RSExecRef object,
                               RSFindSymbolFn find_symbol,
                               void *find_symbol_context);

void rsloaderUpdateSectionHeaders(RSExecRef object, unsigned char *buf);

void rsloaderDisposeExec(RSExecRef object);

void *rsloaderGetSymbolAddress(RSExecRef object, char const *name);

size_t rsloaderGetSymbolSize(RSExecRef object, char const *name);

size_t rsloaderGetFuncCount(RSExecRef object);

void rsloaderGetFuncNameList(RSExecRef object, size_t size, char const **list);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* LIBRSLOADER_H */
