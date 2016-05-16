#!/usr/bin/python2.4

# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Utility classes for CTS."""

import re
import xml.dom.minidom as minidom


class TestPackage(object):
  """This class represents a test package.

  Each test package consists of one or more suites, each containing one or more subsuites and/or
  one or more test cases. Each test case contains one or more tests.

  The package structure is currently stored using Python dictionaries and lists. Translation
  to XML is done via a DOM tree that gets created on demand.

  TODO: Instead of using an internal data structure, using a DOM tree directly would increase
  the usability. For example, one could easily create an instance by parsing an existing XML.
  """

  class TestSuite(object):
    """A test suite."""

    def __init__(self, is_root=False):
      self.is_root = is_root
      self.test_cases = {}
      self.test_suites = {}

    def Add(self, names):
      if len(names) == 2:
        # names contains the names of the test case and the test
        test_case = self.test_cases.setdefault(names[0], [])
        test_case.append(names[1])
      else:
        sub_suite = self.test_suites.setdefault(names[0], TestPackage.TestSuite())
        sub_suite.Add(names[1:])

    def WriteDescription(self, doc, parent):
      """Recursively append all suites and testcases to the parent tag."""
      for (suite_name, suite) in self.test_suites.iteritems():
        child = doc.createElement('TestSuite')
        child.setAttribute('name', suite_name)
        parent.appendChild(child)
        # recurse into child suites
        suite.WriteDescription(doc, child)
      for (case_name, test_list) in self.test_cases.iteritems():
        child = doc.createElement('TestCase')
        child.setAttribute('name', case_name)
        parent.appendChild(child)
        for test_name in test_list:
          test = doc.createElement('Test')
          test.setAttribute('name', test_name)
          child.appendChild(test)

  def __init__(self, package_name, app_package_name=''):
    self.encoding = 'UTF-8'
    self.attributes = {'name': package_name, 'AndroidFramework': 'Android 1.0',
                       'version': '1.0', 'targetNameSpace': '', 'targetBinaryName': '',
                       'jarPath': '', 'appPackageName': app_package_name}
    self.root_suite = self.TestSuite(is_root=True)

  def AddTest(self, name):
    """Add a test to the package.

    Test names are given in the form "testSuiteName.testSuiteName.TestCaseName.testName".
    Test suites can be nested to any depth.

    Args:
      name: The name of the test to add.
    """
    parts = name.split('.')
    self.root_suite.Add(parts)

  def AddAttribute(self, name, value):
    """Add an attribute to the test package itself."""
    self.attributes[name] = value

  def GetDocument(self):
    """Returns a minidom Document representing the test package structure."""
    doc = minidom.Document()
    package = doc.createElement('TestPackage')
    for (attr, value) in self.attributes.iteritems():
      package.setAttribute(attr, value)
    self.root_suite.WriteDescription(doc, package)
    doc.appendChild(package)
    return doc

  def WriteDescription(self, writer):
    """Write the description as XML to the given writer."""
    doc = self.GetDocument()
    doc.writexml(writer, addindent='  ', newl='\n', encoding=self.encoding)
    doc.unlink()


class TestPlan(object):
  """A CTS test plan generator."""

  def __init__(self, all_packages):
    """Instantiate a test plan with a list of available package names.

    Args:
      all_packages: The full list of available packages. Subsequent calls to Exclude() and
          Include() select from the packages given here.
    """
    self.all_packages = all_packages
    self.map = None

  def Exclude(self, pattern):
    """Exclude all packages matching the given regular expression from the plan.

    If this is the first call to Exclude() or Include(), all packages are selected before applying
    the exclusion.

    Args:
      pattern: A regular expression selecting the package names to exclude.
    """
    if not self.map:
      self.Include('.*')
    exp = re.compile(pattern)
    for package in self.all_packages:
      if exp.match(package):
        self.map[package] = False

  def Include(self, pattern):
    """Include all packages matching the given regular expressions in the plan.

    Args:
      pattern: A regular expression selecting the package names to include.
    """
    if not self.map:
      self.map = {}
      for package in self.all_packages:
        self.map[package] = False
    exp = re.compile(pattern)
    for package in self.all_packages:
      if exp.match(package):
        self.map[package] = True

  def Write(self, file_name):
    """Write the test plan to the given file.

    Requires Include() or Exclude() to be called prior to calling this method.

    Args:
      file_name: The name of the file into which the test plan should be written.
    """
    doc = minidom.Document()
    plan = doc.createElement('TestPlan')
    plan.setAttribute('version', '1.0')
    doc.appendChild(plan)
    for package in self.all_packages:
      if self.map[package]:
        entry = doc.createElement('Entry')
        entry.setAttribute('uri', package)
        plan.appendChild(entry)
    stream = open(file_name, 'w')
    doc.writexml(stream, addindent='  ', newl='\n', encoding='UTF-8')
    stream.close()


class XmlFile(object):
  """This class parses Xml files and allows reading attribute values by tag and attribute name."""

  def __init__(self, path):
    """Instantiate the class using the manifest file denoted by path."""
    self.doc = minidom.parse(path)

  def GetAndroidAttr(self, tag, attr_name):
    """Get the value of the given attribute in the first matching tag.

    Args:
      tag: The name of the tag to search.
      attr_name: An attribute name in the android manifest namespace.

    Returns:
      The value of the given attribute in the first matching tag.
    """
    element = self.doc.getElementsByTagName(tag)[0]
    return element.getAttributeNS('http://schemas.android.com/apk/res/android', attr_name)

  def GetAttr(self, tag, attr_name):
    """Return the value of the given attribute in the first matching tag.

    Args:
      tag: The name of the tag to search.
      attr_name: An attribute name in the default namespace.

    Returns:
      The value of the given attribute in the first matching tag.
    """
    element = self.doc.getElementsByTagName(tag)[0]
    return element.getAttribute(attr_name)
