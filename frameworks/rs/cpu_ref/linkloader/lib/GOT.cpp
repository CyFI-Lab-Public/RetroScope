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

#include <stdio.h>
#include "GOT.h"

void *got_symbol_addresses[NUM_OF_GOT_ENTRY];
int got_symbol_indexes[NUM_OF_GOT_ENTRY];
size_t got_symbol_count = 0;

void *got_address()
{
  return &got_symbol_addresses[0];
}

int search_got(int symbol_index, void *addr, uint8_t bind_type)
{
  size_t i;

  // For local symbols (R_MIPS_GOT16), we only store the high 16-bit value
  // after adding 0x8000.
  if (bind_type == STB_LOCAL)
    addr = (void *)(((intptr_t)addr + 0x8000) & 0xFFFF0000);

  for (i = 0; i < got_symbol_count; i++) {
    if (got_symbol_indexes[i] == symbol_index) {
      if (bind_type == STB_LOCAL) {
        // Check if the value is the same for local symbols.
        // If yes, we can reuse this entry.
        // If not, we continue searching.
        if (got_symbol_addresses[i] == addr) {
          return i;
        }
      }
      else {
        // The value must be the same for global symbols .
        rsl_assert (got_symbol_addresses[i] == addr
                    && "MIPS GOT address error.");
        return i;
      }
    }
  }

  // Cannot find this symbol with correct value, so we need to create one
  rsl_assert (got_symbol_count < NUM_OF_GOT_ENTRY && "MIPS GOT is full.");
  got_symbol_indexes[got_symbol_count] = symbol_index;
  got_symbol_addresses[got_symbol_count] = addr;
  got_symbol_count++;
  return (got_symbol_count - 1);
}
