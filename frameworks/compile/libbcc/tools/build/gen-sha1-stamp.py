#!/usr/bin/env python
#
# Copyright (C) 2011-2012 The Android Open Source Project
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

import os
import sys

try:
    import hashlib
    sha1 = hashlib.sha1
except ImportError, e:
    import sha
    sha1 = sha.sha

def compute_sha1(h, path):
    f = open(path, 'rb')
    while True:
        buf = f.read(1024)
        h.update(buf)
        if len(buf) < 1024:
            break
    f.close()

"""The result is a list of pair of file path and its SHA-1 digest"""
def compute_sha1_list(path_list):
    result = []
    for path in path_list:
        h = sha1()
        compute_sha1(h, path)
        result.append((path, h.digest()))
    return result

"""For each path like /xxx/libfoo.so, generate a symbol named libfoo_so_SHA1"""
def get_symbol_name(path):
    return os.path.basename(path).replace('.', '_') + '_SHA1';

"""Print out header for assembly file."""
def print_asm_header(symbols):
    sys.stdout.write("""
/*\
 * The mid-2007 version of gcc that ships with Macs requires a\n\
 * comma on the .section line, but the rest of the world thinks\n\
 * that's a syntax error. It also wants globals to be explicitly\n\
 * prefixed with \"_\" as opposed to modern gccs that do the\n\
 * prefixing for you.\n\
 */\n\
""")
    for sym in symbols:
        sys.stdout.write("""
#ifdef __APPLE_CC__
.globl _%s\n\
#else\n\
.globl %s\n\
#endif\
""" % (sym, sym))
    sys.stdout.write("""
#ifdef __APPLE_CC__
  .section .rodata,\n\
#else\n\
  .section .rodata\n\
#endif\
""" )

def print_asm_data(data, size):
    col = 0
    for i in xrange(size):
        c = data[i]
        if col == 0:
            sys.stdout.write(".byte ")
        elif col % 4 == 0:
            sys.stdout.write(", ")
        else:
            sys.stdout.write(",")
        sys.stdout.write("0x%02x" % ord(c))
        col += 1
        if col == 8:
            sys.stdout.write("\n")
            col = 0
    if col != 0:
        sys.stdout.write("\n")

def print_asm_symbol_data(sym, h):
    sys.stdout.write("""
.align 8
#ifdef __APPLE_CC__
_%s:\n\
#else\n\
%s:\n\
#endif\n\
""" % (sym, sym))
    print_asm_data(h, 20)

def print_asm(x):
    symbols = [get_symbol_name(item[0]) for item in x]
    print_asm_header(symbols)
    for (symbol, y) in zip(symbols, x):
        print_asm_symbol_data(symbol, y[1])

def main():
    if len(sys.argv) < 2:
        print 'USAGE:', sys.argv[0], '[OUTPUT] [INPUTs]'
        sys.exit(1)

    result = compute_sha1_list(sys.argv[1:])
    print_asm(result)

if __name__ == '__main__':
    main()
