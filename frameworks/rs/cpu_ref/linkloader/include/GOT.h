/*
 * Copyright 2011, The Android Open Source Project
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

#ifndef GOT_H
#define GOT_H

#include "utils/rsl_assert.h"
#include "ELF.h"

#define GP_OFFSET	((int)0x8000)
#define GOT_SIZE	(1 << 16)	// bytes
#define GOT_ENTRY_SIZE	4	// bytes
#define NUM_OF_GOT_ENTRY	(GOT_SIZE/GOT_ENTRY_SIZE)

void *got_address();
int search_got(int symbol_index, void *addr, uint8_t bind_type);

#endif // GOT_H
