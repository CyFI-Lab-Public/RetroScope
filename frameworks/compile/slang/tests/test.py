#!/usr/bin/python
#
# Copyright 2010-2012 Google Inc. All Rights Reserved.

"""Renderscript Compiler Test.

Runs subdirectories of tests for the Renderscript compiler.
"""

import filecmp
import glob
import os
import re
import shutil
import subprocess
import sys

__author__ = 'Android'


class Options(object):
  def __init__(self):
    return
  verbose = 0
  cleanup = 1
  updateCTS = 0


def CompareFiles(actual, expect):
  """Compares actual and expect for equality."""
  if not os.path.isfile(actual):
    if Options.verbose:
      print 'Could not find %s' % actual
    return False
  if not os.path.isfile(expect):
    if Options.verbose:
      print 'Could not find %s' % expect
    return False

  return filecmp.cmp(actual, expect, False)


def UpdateFiles(src, dst):
  """Update dst if it is different from src."""
  if not CompareFiles(src, dst):
    print 'Copying from %s to %s' % (src, dst)
    shutil.copyfile(src, dst)


def GetCommandLineArgs(filename):
  """Extracts command line arguments from first comment line in a file."""
  f = open(filename, 'r')
  line = f.readline()
  if line[0] == '/' and line [1] == '/':
    return line[2:].strip()
  else:
    return ''


def ExecTest(dirname):
  """Executes an llvm-rs-cc test from dirname."""
  passed = True

  if Options.verbose != 0:
    print 'Testing %s' % dirname

  os.chdir(dirname)
  stdout_file = open('stdout.txt', 'w+')
  stderr_file = open('stderr.txt', 'w+')

  cmd_string = ('../../../../../out/host/linux-x86/bin/llvm-rs-cc '
                '-o tmp/ -p tmp/ '
                '-MD '
                '-I ../../../../../frameworks/rs/scriptc/ '
                '-I ../../../../../external/clang/lib/Headers/')
  base_args = cmd_string.split()
  rs_files = glob.glob('*.rs')
  fs_files = glob.glob('*.fs')
  rs_files += fs_files;
  rs_files.sort()

  # Extra command line arguments can be placed as // comments at the start of
  # any .rs file. We automatically bundle up all of these extra args and invoke
  # llvm-rs-cc with them.
  extra_args_str = ''
  for rs_file in rs_files:
    extra_args_str += GetCommandLineArgs(rs_file)
  extra_args = extra_args_str.split()

  args = base_args + extra_args + rs_files

  if Options.verbose > 1:
    print 'Executing:',
    for arg in args:
      print arg,
    print

  # Execute the command and check the resulting shell return value.
  # All tests that are expected to FAIL have directory names that
  # start with 'F_'. Other tests that are expected to PASS have
  # directory names that start with 'P_'.
  ret = 0
  try:
    ret = subprocess.call(args, stdout=stdout_file, stderr=stderr_file)
  except:
    passed = False

  stdout_file.flush()
  stderr_file.flush()

  if Options.verbose > 1:
    stdout_file.seek(0)
    stderr_file.seek(0)
    for line in stdout_file:
      print 'STDOUT>', line,
    for line in stderr_file:
      print 'STDERR>', line,

  stdout_file.close()
  stderr_file.close()

  if dirname[0:2] == 'F_':
    if ret == 0:
      passed = False
      if Options.verbose:
        print 'Command passed on invalid input'
  elif dirname[0:2] == 'P_':
    if ret != 0:
      passed = False
      if Options.verbose:
        print 'Command failed on valid input'
  else:
    passed = (ret == 0)
    if Options.verbose:
      print 'Test Directory name should start with an F or a P'

  if not CompareFiles('stdout.txt', 'stdout.txt.expect'):
    passed = False
    if Options.verbose:
      print 'stdout is different'
  if not CompareFiles('stderr.txt', 'stderr.txt.expect'):
    passed = False
    if Options.verbose:
      print 'stderr is different'

  if Options.updateCTS:
    # Copy resulting files to appropriate CTS directory (if different).
    if passed and glob.glob('IN_CTS'):
      cts_path = '../../../../../cts/'
      cts_res_raw_path = cts_path + 'tests/res/raw/'
      cts_src_path = cts_path + 'tests/tests/renderscript/src/'
      for bc_src in glob.glob('tmp/*.bc'):
        bc_dst = re.sub('tmp\/', cts_res_raw_path, bc_src, 1)
        UpdateFiles(bc_src, bc_dst)
      for java_src in glob.glob('tmp/android/renderscript/cts/*.java'):
        java_dst = re.sub('tmp\/', cts_src_path, java_src, 1)
        UpdateFiles(java_src, java_dst)

  if Options.cleanup:
    try:
      os.remove('stdout.txt')
      os.remove('stderr.txt')
      shutil.rmtree('tmp/')
    except:
      pass

  os.chdir('..')
  return passed


def Usage():
  """Print out usage information."""
  print ('Usage: %s [OPTION]... [TESTNAME]...'
         'Renderscript Compiler Test Harness\n'
         'Runs TESTNAMEs (all tests by default)\n'
         'Available Options:\n'
         '  -h, --help          Help message\n'
         '  -n, --no-cleanup    Don\'t clean up after running tests\n'
         '  -u, --update-cts    Update CTS test versions\n'
         '  -v, --verbose       Verbose output\n'
        ) % (sys.argv[0]),
  return


def main():
  passed = 0
  failed = 0
  files = []
  failed_tests = []

  for arg in sys.argv[1:]:
    if arg in ('-h', '--help'):
      Usage()
      return 0
    elif arg in ('-n', '--no-cleanup'):
      Options.cleanup = 0
    elif arg in ('-u', '--update-cts'):
      Options.updateCTS = 1
    elif arg in ('-v', '--verbose'):
      Options.verbose += 1
    else:
      # Test list to run
      if os.path.isdir(arg):
        files.append(arg)
      else:
        print >> sys.stderr, 'Invalid test or option: %s' % arg
        return 1

  if not files:
    tmp_files = os.listdir('.')
    # Only run tests that are known to PASS or FAIL
    # Disabled tests can be marked D_ and invoked explicitly
    for f in tmp_files:
      if os.path.isdir(f) and (f[0:2] == 'F_' or f[0:2] == 'P_'):
        files.append(f)

  for f in files:
    if os.path.isdir(f):
      if ExecTest(f):
        passed += 1
      else:
        failed += 1
        failed_tests.append(f)

  print 'Tests Passed: %d\n' % passed,
  print 'Tests Failed: %d\n' % failed,
  if failed:
    print 'Failures:',
    for t in failed_tests:
      print t,

  return failed != 0


if __name__ == '__main__':
  sys.exit(main())
