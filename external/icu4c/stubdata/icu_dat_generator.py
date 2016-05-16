#!/usr/bin/python
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

# Generates icudtXXl-default.dat from icudtXXl-all.dat and icu-data-default.txt.
#
# Usage:
#    icu_dat_generator.py [-v] [-h]
#
# Sample usage:
#   $ANDROID_BUILD_TOP/external/icu4c/stubdata$ ./icu_dat_generator.py --verbose

import getopt
import glob
import os
import os.path
import re
import shutil
import subprocess
import sys


def PrintHelpAndExit():
  print "Usage:"
  print "  icu_dat_generator.py [-v|--verbose] [-h|--help]"
  print "Example:"
  print "  $ANDROID_BUILD_TOP/external/icu4c/stubdata$ ./icu_dat_generator.py"
  sys.exit(1)


def InvokeIcuTool(tool, working_dir, args):
  command_list = [os.path.join(ICU_PREBUILT_DIR, tool)]
  command_list.extend(args)

  if VERBOSE:
    command = "[%s] %s" % (working_dir, " ".join(command_list))
    print command

  ret = subprocess.call(command_list, cwd=working_dir)
  if ret != 0:
    sys.exit(command_list[0:])


def ExtractAllResourceFilesToTmpDir():
  # copy icudtXXl-all.dat to icudtXXl.dat
  src_dat = os.path.join(ICU4C_DIR, "stubdata", ICU_DATA + "-all.dat")
  dst_dat = os.path.join(ICU4C_DIR, "stubdata", ICU_DATA + ".dat")
  shutil.copyfile(src_dat, dst_dat)
  InvokeIcuTool("icupkg", None, [dst_dat, "-x", "*", "-d", TMP_DAT_PATH])


def MakeDat(input_file, stubdata_dir):
  print "------ Processing '%s'..." % (input_file)
  if not os.path.isfile(input_file):
    print "%s not a file!" % input_file
    sys.exit(1)
  GenResIndex(input_file)
  CopyAndroidCnvFiles(stubdata_dir)
  # Run "icupkg -tl -s icudtXXl -a icu-data-default.txt new icudtXXl.dat".
  args = ["-tl", "-s", TMP_DAT_PATH, "-a", "add_list.txt", "new", ICU_DATA + ".dat"]
  InvokeIcuTool("icupkg", TMP_DAT_PATH, args)


def ResFilesToLocales(res_files):
  locales = []
  for res_file in res_files:
    # res_file is something like 'coll/en_US.res'.
    if not '/' in res_file:
      locales.append(res_file)
    else:
      locales.append(res_file.split('/')[1].replace('.res', ''))
  return locales


def WriteIndex(path, locales):
  empty_value = " {\"\"}\n"  # key-value pair for all locale entries

  f = open(path, "w")
  f.write("res_index:table(nofallback) {\n")
  f.write("  InstalledLocales {\n")
  for locale in sorted(locales):
    f.write(locale + empty_value)

  f.write("  }\n")
  f.write("}\n")
  f.close()


def AddResFile(collection, path):
  # There are two consumers of the the input .txt file: this script and
  # icupkg. We only care about .res files, but icupkg needs files they depend
  # on too, so it's not an error to have to ignore non-.res files here.
  end = path.find(".res")
  if end > 0:
    collection.add(path[path.find("/")+1:end])
  return


def AddAllResFiles(collection, dir_name, language):
  pattern1 = '%s/data/%s/%s.txt' % (ICU4C_DIR, dir_name, language)
  pattern2 = '%s/data/%s/%s_*.txt' % (ICU4C_DIR, dir_name, language)
  for path in glob.glob(pattern1) + glob.glob(pattern2):
    if 'TRADITIONAL' in path or 'PHONEBOOK' in path:
      continue
    parts = path.split('/')
    if dir_name == 'locales':
      path = parts[-1].replace('.txt', '')
    else:
      path = parts[-2] + '/' + parts[-1].replace('.txt', '.res')
    collection.add(path)


def DumpFile(filename):
  print ' ----------------- %s' % filename
  os.system("cat %s" % filename)
  print ' ----------------- END'


# Open input file (such as icu-data-default.txt).
# Go through the list and generate res_index.res for locales, brkitr,
# coll, et cetera.
def GenResIndex(input_file):
  brkitrs = set()
  colls = set()
  currs = set()
  langs = set()
  locales = set()
  regions = set()
  zones = set()

  languages = [
    # Group 0.
    'en',

    # Group 1.
    'ar',
    'zh',
    'nl',
    'fr',
    'de',
    'it',
    'ja',
    'ko',
    'pl',
    'pt',
    'ru',
    'es',
    'th',
    'tr',

    # Group 2.
    'bg',
    'ca',
    'hr',
    'cs',
    'da',
    'fil','tl',
    'fi',
    'el',
    'iw','he',
    'hi',
    'hu',
    'id','in',
    'lv',
    'lt',
    'nb',
    'ro',
    'sr',
    'sk',
    'sl',
    'sv',
    'uk',
    'vi',
    'fa',

    # Group 3.
    'af',
    'am',
    'bn',
    'et',
    'is',
    'ms',
    'mr',
    'sw',
    'ta',
    'zu',

    # Group 4.
    'eu',
    'gl',
    'gu',
    'kn',
    'ml',
    'te',
    'ur',

    # Group 5.
    'km',
    'lo',
    'ne',
    'si',
    'ka',
    'hy',
    'mn',
    'cy',

    # Others.
    'az',
    'be',
    'rm',
  ]

  for language in languages:
    AddAllResFiles(brkitrs, 'brkitr', language)
    AddAllResFiles(colls, 'coll', language)
    AddAllResFiles(currs, 'curr', language)
    AddAllResFiles(langs, 'lang', language)
    AddAllResFiles(regions, 'region', language)
    AddAllResFiles(zones, 'zone', language)
    AddAllResFiles(locales, 'locales', language)

  # We need to merge the human-edited icu-data-default.txt with the
  # machine-generated list of files needed to support the various languages.
  new_add_list = []

  for line in open(input_file, "r"):
    new_add_list.append(line)
    if "root." in line or "res_index" in line or "_.res" in line:
      continue
    if "brkitr/" in line:
      AddResFile(brkitrs, line)
    elif "coll/" in line:
      AddResFile(colls, line)
    elif "curr/" in line:
      AddResFile(currs, line)
    elif "lang/" in line:
      AddResFile(langs, line)
    elif "region/" in line:
      AddResFile(regions, line)
    elif "zone/" in line:
      AddResFile(zones, line)
    elif ".res" in line:
      # TODO: these should all now be misc resources!
      # We need to determine the resource is locale resource or misc resource.
      # To determine the locale resource, we assume max script length is 3.
      end = line.find(".res")
      if end <= 3 or (line.find("_") <= 3 and line.find("_") > 0):
        locales.add(line[:end])

  kind_to_res_files = {
      "brkitr": brkitrs,
      "coll": colls,
      "curr": currs,
      "lang": langs,
      "locales": locales,
      "region": regions,
      "zone": zones
  }

  # Merge the machine-generated list into the human-generated list.
  for kind, res_files in kind_to_res_files.items():
    for res_file in sorted(res_files):
      if '.' not in res_file:
        res_file = res_file + '.res'
      new_add_list.append(res_file)

  if VERBOSE:
    for kind, res_files in kind_to_res_files.items():
      print "%s=%s" % (kind, sorted(res_files))

  # Write the genrb input files.

  # First add_list.txt, the argument to icupkg -a...
  f = open(os.path.join(TMP_DAT_PATH, "add_list.txt"), "w")
  for line in new_add_list:
    if line.startswith('#'):
      continue
    f.write("%s\n" % line)
  f.close()

  # Second res_index.txt, used below by genrb.
  res_index = "res_index.txt"
  WriteIndex(os.path.join(TMP_DAT_PATH, res_index), locales)
  for kind, res_files in kind_to_res_files.items():
    if kind == "locales":
      continue
    res_index_filename = os.path.join(TMP_DAT_PATH, kind, res_index)
    WriteIndex(res_index_filename, ResFilesToLocales(res_files))
    if VERY_VERBOSE:
      DumpFile(res_index_filename)

  # Useful if you need to see the temporary input files we generated.
  if VERY_VERBOSE:
    DumpFile('%s/add_list.txt' % TMP_DAT_PATH)
    DumpFile('%s/res_index.txt' % TMP_DAT_PATH)

  # Call genrb to generate new res_index.res.
  InvokeIcuTool("genrb", TMP_DAT_PATH, [res_index])
  for kind, res_files in kind_to_res_files.items():
    if kind == "locales":
      continue
    InvokeIcuTool("genrb", os.path.join(TMP_DAT_PATH, kind), [res_index])


def CopyAndroidCnvFiles(stubdata_dir):
  android_specific_cnv = ["gsm-03.38-2000.cnv",
                          "iso-8859_16-2001.cnv",
                          "docomo-shift_jis-2012.cnv",
                          "kddi-jisx-208-2007.cnv",
                          "kddi-shift_jis-2012.cnv",
                          "softbank-jisx-208-2007.cnv",
                          "softbank-shift_jis-2012.cnv"]
  for cnv_file in android_specific_cnv:
    src_path = os.path.join(stubdata_dir, "cnv", cnv_file)
    dst_path = os.path.join(TMP_DAT_PATH, cnv_file)
    shutil.copyfile(src_path, dst_path)
    if VERBOSE:
      print "copy " + src_path + " " + dst_path


def main():
  global ANDROID_BUILD_TOP  # $ANDROID_BUILD_TOP
  global ICU4C_DIR          # $ANDROID_BUILD_TOP/external/icu4c
  global ICU_PREBUILT_DIR   # Directory containing pre-built ICU tools.
  global ICU_DATA           # e.g. "icudt50l"
  global TMP_DAT_PATH       # Temporary directory to store all resource files and
                            # intermediate dat files.
  global VERBOSE, VERY_VERBOSE

  VERBOSE = VERY_VERBOSE = False

  show_help = False
  try:
    opts, args = getopt.getopt(sys.argv[1:], "hv", ["help", "verbose", "very-verbose"])
  except getopt.error:
    PrintHelpAndExit()
  for opt, _ in opts:
    if opt in ("-h", "--help"):
      show_help = True
    elif opt in ("-v", "--verbose"):
      VERBOSE = True
    elif opt in ("--very-verbose"):
      VERY_VERBOSE = VERBOSE = True
  if args:
    show_help = True

  if show_help:
    PrintHelpAndExit()

  ANDROID_BUILD_TOP = os.environ.get("ANDROID_BUILD_TOP")
  if not ANDROID_BUILD_TOP:
    print "$ANDROID_BUILD_TOP not set! Run 'env_setup.sh'."
    sys.exit(1)
  ICU4C_DIR = os.path.join(ANDROID_BUILD_TOP, "external", "icu4c")
  stubdata_dir = os.path.join(ICU4C_DIR, "stubdata")

  # Work out the ICU version from the source .dat filename, so we can find the
  # appropriate pre-built ICU tools.
  source_dat = os.path.basename(glob.glob(os.path.join(stubdata_dir, "icudt*.dat"))[0])
  icu_version = re.sub(r"([^0-9])", "", source_dat)
  ICU_PREBUILT_DIR = os.path.join(os.environ.get("ANDROID_BUILD_TOP"),
      "prebuilts", "misc", "linux-x86_64", "icu-%s%s" % (icu_version[0], icu_version[1]))
  if not os.path.exists(ICU_PREBUILT_DIR):
    print "%s does not exist!" % ICU_PREBUILT_DIR

  ICU_DATA = "icudt" + icu_version + "l"

  # Check that icudtXXl-all.dat exists (since we build the other .dat files from that).
  full_data_filename = os.path.join(stubdata_dir, ICU_DATA + "-all.dat")
  if not os.path.isfile(full_data_filename):
    print "%s not present." % full_data_filename
    sys.exit(1)

  # Create a temporary working directory.
  TMP_DAT_PATH = os.path.join(ICU4C_DIR, "tmp")
  if os.path.exists(TMP_DAT_PATH):
    shutil.rmtree(TMP_DAT_PATH)
  os.mkdir(TMP_DAT_PATH)

  # Extract resource files from icudtXXl-all.dat to TMP_DAT_PATH.
  ExtractAllResourceFilesToTmpDir()

  input_file = os.path.join(stubdata_dir, "icu-data-default.txt")
  output_file = os.path.join(stubdata_dir, ICU_DATA + "-default.dat")
  MakeDat(input_file, stubdata_dir)
  shutil.copyfile(os.path.join(TMP_DAT_PATH, ICU_DATA + ".dat"), output_file)
  print "Generated ICU data: %s" % output_file

  # Cleanup temporary working directory and icudtXXl.dat
  shutil.rmtree(TMP_DAT_PATH)
  os.remove(os.path.join(stubdata_dir, ICU_DATA + ".dat"))

if __name__ == "__main__":
  main()
