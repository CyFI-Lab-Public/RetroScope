#!/usr/bin/env python
# Copyright (c) 2012 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

'''Unit tests for the 'grit build' tool.
'''

import os
import sys
import tempfile
if __name__ == '__main__':
  sys.path.append(os.path.join(os.path.dirname(__file__), '../..'))

import unittest

from grit import util
from grit.tool import build


class BuildUnittest(unittest.TestCase):

  def testFindTranslationsWithSubstitutions(self):
    # This is a regression test; we had a bug where GRIT would fail to find
    # messages with substitutions e.g. "Hello [IDS_USER]" where IDS_USER is
    # another <message>.
    output_dir = tempfile.mkdtemp()
    builder = build.RcBuilder()
    class DummyOpts(object):
      def __init__(self):
        self.input = util.PathFromRoot('grit/testdata/substitute.grd')
        self.verbose = False
        self.extra_verbose = False
    builder.Run(DummyOpts(), ['-o', output_dir])


if __name__ == '__main__':
  unittest.main()
