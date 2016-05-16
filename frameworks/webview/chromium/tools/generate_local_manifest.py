#!/usr/bin/env python
# Copyright (c) 2013 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""Generate local manifest in an Android repository.

This is used to generate a local manifest in an Android repository. The purpose
of the generated manifest is to remove the set of projects that exist under a
certain path.
"""

import os
import sys
import xml.etree.ElementTree as ET

def createLocalManifest(manifest_path, local_manifest_path, path_to_exclude):
  manifest_tree = ET.parse(manifest_path)
  local_manifest_root = ET.Element('manifest')
  for project in manifest_tree.getroot().findall('project'):
    project_path = project.get('path')
    project_name = project.get('name')
    exclude_project = ((project_path != None and
                        project_path.startswith(path_to_exclude)) or
                       (project_path == None and
                        project_name.startswith(path_to_exclude)))
    if not exclude_project:
      continue
    print 'Excluding project name="%s" path="%s"' % (project_name,
                                                     project_path)
    remove_project = ET.SubElement(local_manifest_root, 'remove-project')
    remove_project.set('name', project.get('name'))

  local_manifest_tree = ET.ElementTree(local_manifest_root)
  local_manifest_dir = os.path.dirname(local_manifest_path)
  if not os.path.exists(local_manifest_dir):
    os.makedirs(local_manifest_dir)
  local_manifest_tree.write(local_manifest_path,
                            xml_declaration=True,
                            encoding='UTF-8',
                            method='xml')

def main():
  if len(sys.argv) < 3:
    print 'Too few arguments.'
    sys.exit(-1)

  android_build_top = sys.argv[1]
  path_to_exclude = sys.argv[2]

  manifest_filename = 'default.xml'
  if len(sys.argv) >= 4:
    manifest_filename = sys.argv[3]

  manifest_path = os.path.join(android_build_top, '.repo/manifests',
                               manifest_filename)
  local_manifest_path = os.path.join(android_build_top,
                                     '.repo/local_manifest.xml')


  print 'Path to exclude: %s' % path_to_exclude
  print 'Path to manifest file: %s' % manifest_path
  createLocalManifest(manifest_path, local_manifest_path, path_to_exclude)
  print 'Local manifest created in: %s' % local_manifest_path

if __name__ == '__main__':
  main()
