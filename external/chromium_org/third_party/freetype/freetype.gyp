# Copyright (c) 2011 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'conditions': [
    [ 'OS == "android"', {
      'targets': [
        {
          'target_name': 'ft2',
          'type': 'static_library',
          'toolsets': ['target'],
          'sources': [
            'src/base/ftbbox.c',
            'src/base/ftbitmap.c',
            'src/base/ftfstype.c',
            'src/base/ftglyph.c',
            'src/base/ftlcdfil.c',
            'src/base/ftstroke.c',
            'src/base/ftxf86.c',
            'src/base/ftbase.c',
            'src/base/ftsystem.c',
            'src/base/ftinit.c',
            'src/base/ftgasp.c',
            'src/base/fttype1.c',
            'src/raster/raster.c',
            'src/sfnt/sfnt.c',
            'src/smooth/smooth.c',
            'src/autofit/autofit.c',
            'src/truetype/truetype.c',
            'src/cff/cff.c',
            'src/psnames/psnames.c',
            'src/pshinter/pshinter.c',
          ],
          'include_dirs': [
            'build',
            'include',
          ],
          'defines': [
            'FT2_BUILD_LIBRARY',
            'DARWIN_NO_CARBON',
          ],
          'direct_dependent_settings': {
            'include_dirs': [
              '../../third_party/freetype/include',
            ],
          },
        },
      ],
    }],
  ],
}
