#!/usr/bin/env python
#
# Copyright (C) 2013 The Android Open Source Project
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

"""WebView postprocessor for the go/memdump tool.

Processes the output of memdump (see go/memdump) aggregating memory usage
information for WebView analysis (both classic and chromium).

Usage: adb shell /path/to/target/memdump <target-PID> | ./memreport.py > out.csv
"""


import os
import re
import sys

from sets import Set


_ENTRIES = [
    ('Total', '.* r... .*'),
    (' Read-only', '.* r--. .*'),
    (' Read-write', '.* rw.. .*'),
    ('  Read-write (no x)', '.* rw-. .*'),
    (' Executable', '.* ..x. .*'),
    ('Anonymous total', '.* .... .* .*shared_other=[0-9]+ ($|.*dlmalloc.*)'),
    (' Anonymous executable (JIT)', '.* ..x. .* shared_other=[0-9]+ ($|.*dlmalloc.*)'),
    (' Anonymous read-write', '.* rw.. .* .*shared_other=[0-9]+ ($|.*dlmalloc.*)'),
    ('  Native heap (dlmalloc)', '.* r... .* /.*dlmalloc.*'),
    ('File total', '.* .... .* /((?!dev/ashmem/dlmalloc).*)'),
    (' File executable', '.* ..x. .* /((?!dev/ashmem/dlmalloc).*)'),
    (' File read-write', '.* rw.. .* /((?!dev/ashmem/dlmalloc).*)'),
    (' Dalvik', '.* rw.. .* /.*dalvik.*'),
    ('  Dalvik heap', '.* rw.. .* /.*dalvik-heap.*'),
    (' Ashmem', '.* rw.. .* /dev/ashmem .*'),
    (' libwebcore.so total', '.* r... .* /.*libwebcore.so'),
    ('  libwebcore.so read-only', '.* r--. .* /.*libwebcore.so'),
    ('  libwebcore.so read-write', '.* rw-. .* /.*libwebcore.so'),
    ('  libwebcore.so executable', '.* r.x. .* /.*libwebcore.so'),
    (' libwebviewchromium.so total', '.* r... .* /.*libwebviewchromium.so'),
    ('  libwebviewchromium.so read-only', '.* r--. .* /.*libwebviewchromium.so'),
    ('  libwebviewchromium.so read-write', '.* rw-. .* /.*libwebviewchromium.so'),
    ('  libwebviewchromium.so executable', '.* r.x. .* /.*libwebviewchromium.so'),
    (' Driver mappings', '.* .... .* /dev/\w+$'),
    ('  /dev/maliN total', '.* .... .* /dev/mali.*'),
    ('OTHER (non file non anon)', '.* .... .*shared_other=[0-9]+ [^/]+'),
    (' DMA buffers', '.* .... .*shared_other=[0-9]+ .*dmabuf.*'),
    ]


def _CollectMemoryStats(memdump, region_filters):
  processes = []
  mem_usage_for_regions = None
  regexps = {}
  for region_filter in region_filters:
    regexps[region_filter] = re.compile(region_filter)
  for line in memdump:
    if 'PID=' in line:
      mem_usage_for_regions = {}
      processes.append(mem_usage_for_regions)
      continue
    matched_regions = Set([])
    for region_filter in region_filters:
      if regexps[region_filter].match(line.rstrip('\r\n')):
        matched_regions.add(region_filter)
        if not region_filter in mem_usage_for_regions:
          mem_usage_for_regions[region_filter] = {
              'private_unevictable': 0,
              'private': 0,
              'shared_app': 0.0,
              'shared_other_unevictable': 0,
              'shared_other': 0,
          }
    for matched_region in matched_regions:
      mem_usage = mem_usage_for_regions[matched_region]
      for key in mem_usage:
        for token in line.split(' '):
          if (key+'=') in token:
            field = token.split('=')[1]
            if key != 'shared_app':
              mem_usage[key] += int(field)
            else:  # shared_app=[\d,\d...]
              array = eval(field)
              for i in xrange(len(array)):
                mem_usage[key] += float(array[i]) / (i + 2)
            break
  return processes


def _ConvertMemoryField(field):
  return str(field / (1024))


def _DumpCSV(processes_stats):
  total_map = {}
  i = 0
  for process in processes_stats:
    i += 1
    print ',private,private_unevictable,shared_other,shared_other_unevictable,'
    for (k, v) in _ENTRIES:
      header_column = k + ',' if 'NOHEADER' not in os.environ else ','
      if not v in process:
        print header_column + '0,0,0,0,'
        continue
      if not v in total_map:
        total_map[v] = 0
      total_map[v] += process[v]['private'] + process[v]['shared_app']
      print (
          header_column +
          _ConvertMemoryField(process[v]['private']) + ',' +
          _ConvertMemoryField(process[v]['private_unevictable']) + ',' +
          _ConvertMemoryField(process[v]['shared_other']) + ',' +
          _ConvertMemoryField(process[v]['shared_other_unevictable']) + ','
          )


def main(argv):
  _DumpCSV(_CollectMemoryStats(sys.stdin, [value for (key, value) in _ENTRIES]))


if __name__ == '__main__':
  main(sys.argv)
