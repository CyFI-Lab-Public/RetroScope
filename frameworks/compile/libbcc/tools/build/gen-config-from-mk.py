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

import re
import sys

def extract_config(f):
    conf_patt = re.compile('# Configurations')
    split_patt = re.compile('#={69}')
    var_patt = re.compile('libbcc_([A-Z_]+)\\s*:=\\s*([01])')

    STATE_PRE_CONFIG = 0
    STATE_FOUND_CONFIG = 1
    STATE_IN_CONFIG = 2

    state = STATE_PRE_CONFIG

    for line in f:
        if state == STATE_PRE_CONFIG:
            if conf_patt.match(line.strip()):
                print '#ifndef BCC_CONFIG_FROM_MK_H'
                print '#define BCC_CONFIG_FROM_MK_H'
                state = STATE_FOUND_CONFIG

        elif state == STATE_FOUND_CONFIG:
            if split_patt.match(line.strip()):
                # Start reading the configuration
                print '/* BEGIN USER CONFIG */'
                state = STATE_IN_CONFIG

        elif state == STATE_IN_CONFIG:
            match = var_patt.match(line.strip())
            if match:
                print '#define', match.group(1), match.group(2)

            elif split_patt.match(line.strip()):
                # Stop reading the configuration
                print '/* END USER CONFIG */'
                print '#endif // BCC_CONFIG_FROM_MK_H'
                break

def main():
    if len(sys.argv) != 1:
        print >> sys.stderr, 'USAGE:', sys.argv[0]
        sys.exit(1)

    extract_config(sys.stdin)


if __name__ == '__main__':
    main()
