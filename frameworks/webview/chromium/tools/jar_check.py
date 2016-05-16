#!/usr/bin/env python
#
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Check that a jar file contains only allowed packages.

Given a jar file (typically, the result of running jarjar to rename packages)
and a whitelist file of allowed package names, one per line, check that all the
classes in the jar are in one of those packages.
"""

import contextlib
import sys
import zipfile


def JarCheck(jar_path, whitelist_path):
  """Checks that the files in the jar are in whitelisted packages.

  Args:
    jar_path: The path to the .jar file to be checked.
    whitelist_path: The path to the whitelist file.
  Returns:
    A list of files that are not in whitelisted packages.
  """
  with open(whitelist_path) as whitelist_file:
    allowed_packages = tuple(x.replace('.', '/').replace('\n', '/')
                             for x in whitelist_file)

  with contextlib.closing(zipfile.ZipFile(jar_path)) as jar:
    jar_contents = jar.namelist()

  invalid_files = []
  for filename in jar_contents:
    # Zipfile entries with a trailing / are directories, we can ignore these.
    # Also ignore jar meta-info.
    if filename.endswith('/') or filename.startswith('META-INF/'):
      continue
    if not filename.startswith(allowed_packages):
      invalid_files.append(filename)

  return invalid_files


def main(argv):
  if len(argv) != 3:
    print >>sys.stderr, 'Usage: %s jar_file whitelist_file' % argv[0]
    return 2
  invalid_files = JarCheck(argv[1], argv[2])
  invalid_file_count = len(invalid_files)
  if invalid_file_count == 0:
    return 0
  print >>sys.stderr, ('jar_check found %s files not in a whitelisted package:'
                       % invalid_file_count)
  for f in invalid_files:
    print >>sys.stderr, f
  return 1


if __name__ == '__main__':
  sys.exit(main(sys.argv))
