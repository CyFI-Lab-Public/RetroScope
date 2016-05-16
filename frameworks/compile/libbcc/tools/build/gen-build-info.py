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

import datetime
import os
import re
import sys
import subprocess

try:
    import hashlib
    sha1 = hashlib.sha1
except ImportError, e:
    import sha
    sha1 = sha.sha

def get_repo_revision(repo_dir):
    if not os.path.exists(os.path.join(repo_dir, '.git')):
        return 'Unknown (not git)'

    # Get the HEAD revision
    proc = subprocess.Popen(['git', 'log', '-1', '--format=%H'],
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            cwd=repo_dir)
    out, err = proc.communicate()
    proc.wait()

    rev_sha1 = out.strip()

    # Working Directory Modified
    proc = subprocess.Popen(['git', 'status'],
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            cwd=repo_dir)
    out, err = proc.communicate()
    proc.wait()

    if out.find('(working directory clean)') == -1:
      mod = ' modified'
    else:
      mod = ''

    return rev_sha1 + mod + ' (git)'

def compute_sha1(path, global_hasher = None):
    f = open(path, 'rb')
    hasher = sha1()
    while True:
        buf = f.read(512)
        hasher.update(buf)
        if global_hasher:
            global_hasher.update(buf)
        if len(buf) < 512:
            break
    f.close()
    return hasher.hexdigest()

def compute_sha1_list(paths):
    hasher = sha1()
    sha1sums = []
    for path in paths:
        sha1sums.append(compute_sha1(path, hasher))
    return (hasher.hexdigest(), sha1sums)

def quote_str(s):
    result = '"'
    for c in s:
        if c == '\\':
            result += '\\\\'
        elif c == '\r':
            result += '\\r'
        elif c == '\n':
            result += '\\n'
        elif c == '\t':
            result += '\\t'
        elif c == '\"':
            result += '\\"'
        elif c == '\'':
            result += '\\\''
        else:
            result += c
    result += '"'
    return result

def main():
    # Check Argument
    if len(sys.argv) < 2:
        print >> sys.stderr, 'USAGE:', sys.argv[0], '[REPO] [LIBs]'
        sys.exit(1)

    # Record Build Time
    build_time = datetime.datetime.now().strftime('%Y/%m/%d %H:%M:%S')

    # Repository Directory (For build revision)
    repo_dir = sys.argv[1]
    build_rev = get_repo_revision(repo_dir)

    # Compute SHA1
    lib_list = list(set(sys.argv[2:]))
    lib_list.sort()
    build_sha1, sha1sum_list = compute_sha1_list(lib_list)

    # Build file list string
    lib_list_str = ''
    for i, path in enumerate(lib_list):
        lib_list_str += '   %s %s\n' % (sha1sum_list[i], path)

    # Print the automatically generated code
    print """/* Automatically generated file (DON'T MODIFY) */

/* Repository directory: %s */

/* File list:
%s*/

#include "bcc/Config/BuildInfo.h"

using namespace bcc;

const char* BuildInfo::GetBuildTime() {
  return %s;
}

const char *BuildInfo::GetBuildRev() {
  return %s;
}

const char *BuildInfo::GetBuildSourceBlob() {
  return %s;
}

""" % (os.path.abspath(repo_dir),
       lib_list_str,
       quote_str(build_time),
       quote_str(build_rev),
       quote_str(build_sha1))

if __name__ == '__main__':
    main()
