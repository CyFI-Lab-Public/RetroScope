#!/usr/bin/env python
#
# Copyright (C) 2012 The Android Open Source Project
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

#detect USB sound card from sound card lists under sys/class/sound/soundX

import os, re, sys

AUDIO_CLASS_DIR = "/sys/class/sound"

def main(argv):
  if len(argv) < 2:
    print "Usage: detect_usb_audio.py (product)+"
    print "   ex: detect_usb_audio.py MobilePre"
    sys.exit(1)
  current_argv = 1
  product_list = []
  while current_argv < len(argv):
    product_list.append(argv[current_argv])
    current_argv = current_argv + 1
  #print product_list
  sound_dev_list = os.listdir(AUDIO_CLASS_DIR)
  for sound_dev in sound_dev_list:
    m = re.search("card(\d+)$", sound_dev)
    if m != None:
      card_full_path = os.path.realpath(AUDIO_CLASS_DIR + "/" + sound_dev)
      if "usb" in card_full_path:
        f = open(card_full_path + "/id")
        line = f.readline().strip()
        if line in product_list:
          print "___CTS_AUDIO_PASS___ " + line + " " + m.group(1)
          sys.exit(0)
        f.close()
  # card not found
  sys.exit(1)

if __name__ == '__main__':
  main(sys.argv)
