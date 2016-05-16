#!/usr/bin/env python
#
# Copyright (C) 2012 The Android Open Source Project
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

"""
Executes WebView CTS tests and verifies results against known failures.
"""

import re
import signal
import subprocess
import sys

# Eventually this list will be empty!
# If you add or remove tests from this lists please update the CTS
# spreadsheet!
EXPECTED_FAILURES = set([
  'android.webkit.cts.WebViewClientTest#testOnScaleChanged',
  'android.webkit.cts.WebViewTest#testCapturePicture',
  # BUG=crbug.com/162967
  'android.webkit.cts.WebViewTest#testPageScroll',
  'android.webkit.cts.WebViewTest#testRequestChildRectangleOnScreen',
  'android.webkit.cts.WebViewTest#testScrollBarOverlay',
  'android.webkit.cts.WebViewTest#testSetInitialScale',
  'android.webkit.cts.WebViewTest#testSetScrollBarStyle',
  'android.webkit.cts.WebViewTest#testSetWebViewClient',
  'android.webkit.cts.WebViewTest#testZoom',
  # b/8763845
  'android.webkit.cts.WebViewTest#testFindAll',
  # b/8763944
  'android.webkit.cts.WebViewTest#testGetContentHeight',
  # See b/8231433 for Geolocation test failures.
  'android.webkit.cts.GeolocationTest#testSimpleGeolocationRequestAcceptAlways',
  'android.webkit.cts.GeolocationTest#testSimpleGeolocationRequestReject',
  'android.webkit.cts.GeolocationTest#testSimpleGeolocationRequestAcceptOnce',
  # b/9159785
  'android.webkit.cts.WebViewTest#testFindNext',
  # b/5006389
  'android.webkit.cts.WebViewTest#testFlingScroll',
  # b/9103603
  'android.webkit.cts.WebViewTest#testRequestImageRef',
  # b/9121594
  'android.webkit.cts.WebHistoryItemTest#testWebHistoryItem',
  # Don't forget to update the spreadsheet! :)
])

def main():
  proc = None

  # Send INT signal to test runner and exit gracefully so not to lose all
  # output information in a run.
  def handler(signum, frame):
    if proc:
      proc.send_signal(signum)
  signal.signal(signal.SIGINT, handler)

  proc = subprocess.Popen(
      ['cts-tradefed', 'run', 'singleCommand', 'cts', '-p', 'android.webkit', '--screenshot-on-failure'],
      stdout=subprocess.PIPE, stderr=subprocess.PIPE)

  (stdout, stderr) = proc.communicate();

  passes = set(re.findall(r'.*: (.*) PASS', stdout))
  failures = set(re.findall(r'.*: (.*) FAIL', stdout))
  test_results = '%d passes; %d failures' % (len(passes), len(failures))

  unexpected_passes = EXPECTED_FAILURES.difference(failures)
  if len(unexpected_passes) > 0:
    test_results += '\n' + 'UNEXPECTED PASSES (update expectations!):'
    for test in unexpected_passes:
      test_results += '\n' + '\t%s' % (test)

  unexpected_failures = failures.difference(EXPECTED_FAILURES)
  if len(unexpected_failures) > 0:
    test_results += '\n' + 'UNEXPECTED FAILURES (please fix!):'
    for test in unexpected_failures:
      test_results += '\n' + '\t%s' % (test)

  unexpected_failures_count = len(unexpected_failures)
  unexpected_passes_count = len(unexpected_passes)

  # on the buildbot this is most useful at the start
  print test_results

  print '\nstdout dump follows...'
  print stdout
  print '\n'

  # on the cmd line this is most useful at the end
  print test_results

  # Allow buildbot script to distinguish failures and possibly out of date
  # test expectations.
  if len(passes) + len(failures) < 100:
    print 'Ran less than 100 cts tests? Something must be wrong'
    return 2
  elif unexpected_failures_count > 0:
    return 1
  elif unexpected_passes_count >= 5:
    print ('More than 5 new passes? Either you''re running webview classic, or '
           'it really is time to fix failure expectations.')
    return 2
  elif unexpected_passes_count > 0:
    return 3  # STEP_WARNINGS
  else:
    return 0


if __name__ == '__main__':
  sys.exit(main())
