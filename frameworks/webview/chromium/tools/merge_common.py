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

"""Common data/functions for the Chromium merging scripts."""

import logging
import os
import re
import subprocess


REPOSITORY_ROOT = os.path.join(os.environ['ANDROID_BUILD_TOP'],
                               'external/chromium_org')


# Whitelist of projects that need to be merged to build WebView. We don't need
# the other upstream repositories used to build the actual Chrome app.
# Different stages of the merge process need different ways of looking at the
# list, so we construct different combinations below.

THIRD_PARTY_PROJECTS_WITH_FLAT_HISTORY = [
    'third_party/WebKit',
]

THIRD_PARTY_PROJECTS_WITH_FULL_HISTORY = [
    'sdch/open-vcdiff',
    'testing/gtest',
    'third_party/angle_dx11',
    'third_party/eyesfree/src/android/java/src/com/googlecode/eyesfree/braille',
    'third_party/freetype',
    'third_party/icu',
    'third_party/leveldatabase/src',
    'third_party/libjingle/source/talk',
    'third_party/libphonenumber/src/phonenumbers',
    'third_party/libphonenumber/src/resources',
    'third_party/mesa/src',
    'third_party/openssl',
    'third_party/opus/src',
    'third_party/ots',
    'third_party/skia/include',
    'third_party/skia/gyp',
    'third_party/skia/src',
    'third_party/smhasher/src',
    'third_party/yasm/source/patched-yasm',
    'tools/grit',
    'tools/gyp',
    'v8',
]

PROJECTS_WITH_FLAT_HISTORY = ['.'] + THIRD_PARTY_PROJECTS_WITH_FLAT_HISTORY
PROJECTS_WITH_FULL_HISTORY = THIRD_PARTY_PROJECTS_WITH_FULL_HISTORY

THIRD_PARTY_PROJECTS = (THIRD_PARTY_PROJECTS_WITH_FLAT_HISTORY +
                        THIRD_PARTY_PROJECTS_WITH_FULL_HISTORY)

ALL_PROJECTS = ['.'] + THIRD_PARTY_PROJECTS


# Directories to be removed when flattening history.
PRUNE_WHEN_FLATTENING = {
    'third_party/WebKit': [
        'LayoutTests',
    ],
}


# Only projects that have their history flattened can have directories pruned.
assert all(p in PROJECTS_WITH_FLAT_HISTORY for p in PRUNE_WHEN_FLATTENING)


class MergeError(Exception):
  """Used to signal an error that prevents the merge from being completed."""


class CommandError(MergeError):
  """This exception is raised when a process run by GetCommandStdout fails."""

  def __init__(self, returncode, cmd, cwd, stdout, stderr):
    super(CommandError, self).__init__()
    self.returncode = returncode
    self.cmd = cmd
    self.cwd = cwd
    self.stdout = stdout
    self.stderr = stderr

  def __str__(self):
    return ("Command '%s' returned non-zero exit status %d. cwd was '%s'.\n\n"
            "===STDOUT===\n%s\n===STDERR===\n%s\n" %
            (self.cmd, self.returncode, self.cwd, self.stdout, self.stderr))


class TemporaryMergeError(MergeError):
  """A merge error that can potentially be resolved by trying again later."""


def GetCommandStdout(args, cwd=REPOSITORY_ROOT, ignore_errors=False):
  """Gets stdout from runnng the specified shell command.

  Similar to subprocess.check_output() except that it can capture stdout and
  stderr separately for better error reporting.

  Args:
    args: The command and its arguments as an iterable.
    cwd: The working directory to use. Defaults to REPOSITORY_ROOT.
    ignore_errors: Ignore the command's return code and stderr.
  Returns:
    stdout from running the command.
  Raises:
    CommandError: if the command exited with a nonzero status.
  """
  p = subprocess.Popen(args=args, cwd=cwd, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
  stdout, stderr = p.communicate()
  if p.returncode == 0 or ignore_errors:
    return stdout
  else:
    raise CommandError(p.returncode, ' '.join(args), cwd, stdout, stderr)


def CheckNoConflictsAndCommitMerge(commit_message, unattended=False,
                                   cwd=REPOSITORY_ROOT):
  """Checks for conflicts and commits once they are resolved.

  Certain conflicts are resolved automatically; if any remain, the user is
  prompted to resolve them. The user can specify a custom commit message.

  Args:
    commit_message: The default commit message.
    unattended: If running unattended, abort on conflicts.
    cwd: Working directory to use.
  Raises:
    TemporaryMergeError: If there are conflicts in unattended mode.
  """
  status = GetCommandStdout(['git', 'status', '--porcelain'], cwd=cwd)
  conflicts_deleted_by_us = re.findall(r'^(?:DD|DU) ([^\n]+)$', status,
                                       flags=re.MULTILINE)
  if conflicts_deleted_by_us:
    logging.info('Keeping ours for the following locally deleted files.\n  %s',
                 '\n  '.join(conflicts_deleted_by_us))
    GetCommandStdout(['git', 'rm', '-rf', '--ignore-unmatch'] +
                     conflicts_deleted_by_us, cwd=cwd)

  # If upstream renames a file we have deleted then it will conflict, but
  # we shouldn't just blindly delete these files as they may have been renamed
  # into a directory we don't delete. Let them get re-added; they will get
  # re-deleted if they are still in a directory we delete.
  conflicts_renamed_by_them = re.findall(r'^UA ([^\n]+)$', status,
                                         flags=re.MULTILINE)
  if conflicts_renamed_by_them:
    logging.info('Adding theirs for the following locally deleted files.\n %s',
                 '\n  '.join(conflicts_renamed_by_them))
    GetCommandStdout(['git', 'add', '-f'] + conflicts_renamed_by_them, cwd=cwd)

  while True:
    status = GetCommandStdout(['git', 'status', '--porcelain'], cwd=cwd)
    conflicts = re.findall(r'^((DD|AU|UD|UA|DU|AA|UU) [^\n]+)$', status,
                           flags=re.MULTILINE)
    if not conflicts:
      break
    if unattended:
      GetCommandStdout(['git', 'reset', '--hard'], cwd=cwd)
      raise TemporaryMergeError('Cannot resolve merge conflicts.')
    conflicts_string = '\n'.join([x[0] for x in conflicts])
    new_commit_message = raw_input(
        ('The following conflicts exist and must be resolved.\n\n%s\n\nWhen '
         'done, enter a commit message or press enter to use the default '
         '(\'%s\').\n\n') % (conflicts_string, commit_message))
    if new_commit_message:
      commit_message = new_commit_message

  GetCommandStdout(['git', 'commit', '-m', commit_message], cwd=cwd)
