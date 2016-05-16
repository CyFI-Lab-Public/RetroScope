#!/usr/bin/python

# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Module for generating CTS test descriptions and test plans."""

import glob
import os
import re
import shutil
import subprocess
import sys
import xml.dom.minidom as dom
from cts import tools
from multiprocessing import Pool

def GetSubDirectories(root):
  """Return all directories under the given root directory."""
  return [x for x in os.listdir(root) if os.path.isdir(os.path.join(root, x))]


def GetMakeFileVars(makefile_path):
  """Extracts variable definitions from the given make file.

  Args:
    makefile_path: Path to the make file.

  Returns:
    A dictionary mapping variable names to their assigned value.
  """
  result = {}
  pattern = re.compile(r'^\s*([^:#=\s]+)\s*:=\s*(.*?[^\\])$', re.MULTILINE + re.DOTALL)
  stream = open(makefile_path, 'r')
  content = stream.read()
  for match in pattern.finditer(content):
    result[match.group(1)] = match.group(2)
  stream.close()
  return result


class CtsBuilder(object):
  """Main class for generating test descriptions and test plans."""

  def __init__(self, argv):
    """Initialize the CtsBuilder from command line arguments."""
    if len(argv) != 6:
      print 'Usage: %s <testRoot> <ctsOutputDir> <tempDir> <androidRootDir> <docletPath>' % argv[0]
      print ''
      print 'testRoot:       Directory under which to search for CTS tests.'
      print 'ctsOutputDir:   Directory in which the CTS repository should be created.'
      print 'tempDir:        Directory to use for storing temporary files.'
      print 'androidRootDir: Root directory of the Android source tree.'
      print 'docletPath:     Class path where the DescriptionGenerator doclet can be found.'
      sys.exit(1)
    self.test_root = sys.argv[1]
    self.out_dir = sys.argv[2]
    self.temp_dir = sys.argv[3]
    self.android_root = sys.argv[4]
    self.doclet_path = sys.argv[5]

    self.test_repository = os.path.join(self.out_dir, 'repository/testcases')
    self.plan_repository = os.path.join(self.out_dir, 'repository/plans')
    
    #dirty hack to copy over prepopulated CTS test plans, stable vs flaky, for autoCTS
    self.definedplans_repository = os.path.join(self.android_root, 'cts/tests/plans')

  def GenerateTestDescriptions(self):
    """Generate test descriptions for all packages."""
    pool = Pool(processes=2)

    # individually generate descriptions not following conventions
    pool.apply_async(GenerateSignatureCheckDescription, [self.test_repository])

    # generate test descriptions for android tests
    results = []
    pool.close()
    pool.join()
    return sum(map(lambda result: result.get(), results))

  def __WritePlan(self, plan, plan_name):
    print 'Generating test plan %s' % plan_name
    plan.Write(os.path.join(self.plan_repository, plan_name + '.xml'))

  def GenerateTestPlans(self):
    """Generate default test plans."""
    # TODO: Instead of hard-coding the plans here, use a configuration file,
    # such as test_defs.xml
    packages = []
    descriptions = sorted(glob.glob(os.path.join(self.test_repository, '*.xml')))
    for description in descriptions:
      doc = tools.XmlFile(description)
      packages.append(doc.GetAttr('TestPackage', 'appPackageName'))
    # sort the list to give the same sequence based on name
    packages.sort()

    plan = tools.TestPlan(packages)
    plan.Exclude('android\.performance.*')
    self.__WritePlan(plan, 'CTS')
    self.__WritePlan(plan, 'CTS-TF')

    plan = tools.TestPlan(packages)
    plan.Exclude('android\.performance.*')
    plan.Exclude('android\.media\.cts\.StreamingMediaPlayerTest.*')
    # Test plan to not include media streaming tests
    self.__WritePlan(plan, 'CTS-No-Media-Stream')

    plan = tools.TestPlan(packages)
    plan.Exclude('android\.performance.*')
    self.__WritePlan(plan, 'SDK')

    plan.Exclude(r'android\.tests\.sigtest')
    plan.Exclude(r'android\.core.*')
    self.__WritePlan(plan, 'Android')

    plan = tools.TestPlan(packages)
    plan.Include(r'android\.core\.tests.*')
    self.__WritePlan(plan, 'Java')

    plan = tools.TestPlan(packages)
    plan.Include(r'android\.core\.vm-tests-tf')
    self.__WritePlan(plan, 'VM-TF')

    plan = tools.TestPlan(packages)
    plan.Include(r'android\.tests\.sigtest')
    self.__WritePlan(plan, 'Signature')

    plan = tools.TestPlan(packages)
    plan.Include(r'android\.tests\.appsecurity')
    self.__WritePlan(plan, 'AppSecurity')

    # hard-coded white list for PDK plan
    plan.Exclude('.*')
    plan.Include('android\.aadb')
    plan.Include('android\.bluetooth')
    plan.Include('android\.graphics.*')
    plan.Include('android\.hardware')
    plan.Include('android\.media')
    plan.Exclude('android\.mediastress')
    plan.Include('android\.net')
    plan.Include('android\.opengl.*')
    plan.Include('android\.renderscript')
    plan.Include('android\.telephony')
    plan.Include('android\.nativemedia.*')
    plan.Include('com\.android\.cts\..*')#TODO(stuartscott): Should PDK have all these?
    #TODO(stuartscott): Maybe move away from com.android.* to android.* - less typing
    self.__WritePlan(plan, 'PDK')

    #dirty hack to copy over pre-populated CTS plans - flaky vs stable - to streamline autoCTS
    shutil.copyfile(os.path.join(self.definedplans_repository, 'CTS-flaky.xml'),
        os.path.join(self.plan_repository, 'CTS-flaky.xml'))
    shutil.copyfile(os.path.join(self.definedplans_repository, 'CTS-stable.xml'),
        os.path.join(self.plan_repository, 'CTS-stable.xml'))

def LogGenerateDescription(name):
  print 'Generating test description for package %s' % name

def GenerateSignatureCheckDescription(test_repository):
  """Generate the test description for the signature check."""
  LogGenerateDescription('android.tests.sigtest')
  package = tools.TestPackage('SignatureTest', 'android.tests.sigtest')
  package.AddAttribute('appNameSpace', 'android.tests.sigtest')
  package.AddAttribute('signatureCheck', 'true')
  package.AddAttribute('runner', '.InstrumentationRunner')
  package.AddTest('android.tests.sigtest.SignatureTest.testSignature')
  description = open(os.path.join(test_repository, 'SignatureTest.xml'), 'w')
  package.WriteDescription(description)
  description.close()

if __name__ == '__main__':
  builder = CtsBuilder(sys.argv)
  result = builder.GenerateTestDescriptions()
  if result != 0:
    sys.exit(result)
  builder.GenerateTestPlans()

