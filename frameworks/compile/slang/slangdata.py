#!/usr/bin/env python
#
# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


"""Convert Slang data files to assembly output."""

import sys


def PrintHeader(var_name):
  """Print out header for assembly file."""
  sys.stdout.write("""
#ifdef __APPLE_CC__
/*\n\
 * The mid-2007 version of gcc that ships with Macs requires a\n\
 * comma on the .section line, but the rest of the world thinks\n\
 * that's a syntax error. It also wants globals to be explicitly\n\
 * prefixed with \"_\" as opposed to modern gccs that do the\n\
 * prefixing for you.\n\
 */\n\
.globl _%s\n\
  .section .rodata,\n\
  .align 8\n\
_%s:\n\
#else\n\
.globl %s\n\
  .section .rodata\n\
  .align 8\n\
%s:\n\
#endif\n\
""" % (var_name, var_name, var_name, var_name))


def File2Asm(var_name):
  """Convert file to assembly output."""
  PrintHeader(var_name)

  input_size = 0
  col = 0
  while True:
    buf = sys.stdin.read(1024)
    if len(buf) <= 0:
      break
    input_size += len(buf)
    for c in buf:
      if col == 0:
        sys.stdout.write(".byte ")
      sys.stdout.write("0x%02x" % ord(c))
      col += 1
      if col == 16:
        sys.stdout.write("\n")
        col = 0
      elif col % 4 == 0:
        sys.stdout.write(", ")
      else:
        sys.stdout.write(",")
  # always ends with 0x0
  sys.stdout.write("0x00")
  if col != 0:
    sys.stdout.write("\n")

  # encode file size
  PrintHeader(var_name + "_size")
  sys.stdout.write("  .long %d\n" % input_size)


def main(argv):
  if len(argv) < 2:
    print "usage: %s <name>" % argv[0]
    return 1

  File2Asm(argv[1])

if __name__ == "__main__":
  main(sys.argv)
