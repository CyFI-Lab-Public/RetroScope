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

#ifndef UTILS_H
#define UTILS_H

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#define EPRINT(...) fprintf(stderr, __VA_ARGS__)
#define DIE(...) do { EPRINT(__VA_ARGS__); exit(EXIT_FAILURE); } while (0)
#define WARN(...) EPRINT(__VA_ARGS__)
#define INFO(...) EPRINT(__VA_ARGS__)
#define DEBUG(...) EPRINT(__VA_ARGS__)

void strpadcpy(char *d, const char *s, char p, size_t l);
void warn(char *msg);
void die(char *msg);

#endif
