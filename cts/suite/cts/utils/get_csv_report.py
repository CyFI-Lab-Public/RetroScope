#!/usr/bin/env python
#
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import re
import subprocess
import sys
from xml.dom import Node
from xml.dom import minidom

def getChildrenWithTag(parent, tagName):
  children = []
  for child in  parent.childNodes:
    if (child.nodeType == Node.ELEMENT_NODE) and (child.tagName == tagName):
      #print "parent " + parent.getAttribute("name") + " " + tagName +\
      #  " " + child.getAttribute("name")
      children.append(child)
  return children

def getText(tag):
  return str(tag.firstChild.nodeValue)

class TestCase(object):
  def __init__(self, name, summary, details, result):
    self.name = name
    self.summary = summary
    self.details = details
    self.result = result

  def getName(self):
    return self.name

  def getSummary(self):
    return self.summary

  def getDetails(self):
    return self.details

  def getResult(self):
    return self.result

def parseSuite(suite, parentName):
  if parentName != "":
    parentName += '.'
  cases = {}
  childSuites = getChildrenWithTag(suite, "TestSuite")
  for child in childSuites:
    cases.update(parseSuite(child, parentName + child.getAttribute("name")))
  childTestCases = getChildrenWithTag(suite, "TestCase")
  for child in childTestCases:
    className = parentName + child.getAttribute("name")
    for test in getChildrenWithTag(child, "Test"):
      methodName = test.getAttribute("name")
      # do not include this
      if methodName == "testAndroidTestCaseSetupProperly":
        continue
      caseName = str(className + "#" + methodName)
      result = str(test.getAttribute("result"))
      summary = {}
      details = {}
      if result == "pass":
        sts = getChildrenWithTag(test, "Summary")
        dts = getChildrenWithTag(test, "Details")
        if len(sts) == len(dts) == 1:
          summary[sts[0].getAttribute("message")] = getText(sts[0])
          for d in getChildrenWithTag(dts[0], "ValueArray"):
            values = []
            for c in getChildrenWithTag(d, "Value"):
              values.append(getText(c))
            details[d.getAttribute("message")] = values
        else:
          result = "no results"
      testCase = TestCase(caseName, summary, details, result)
      cases[caseName] = testCase
  return cases


class Result(object):
  def __init__(self, reportXml):
    self.results = {}
    self.infoKeys = []
    self.infoValues = []
    doc = minidom.parse(reportXml)
    testResult = doc.getElementsByTagName("TestResult")[0]
    buildInfos = testResult.getElementsByTagName("BuildInfo")
    if buildInfos != None and len(buildInfos) > 0:
      buildInfo = buildInfos[0]
      buildId = buildInfo.getAttribute("buildID")
      deviceId = buildInfo.getAttribute("deviceID")
      deviceName = buildInfo.getAttribute("build_device")
      boardName = buildInfo.getAttribute("build_board")
      partitions = buildInfo.getAttribute("partitions")
      m = re.search(r'.*;/data\s+([\w\.]+)\s+([\w\.]+)\s+([\w\.]+)\s+([\w\.]+);', partitions)
      dataPartitionSize = m.group(1)
      self.addKV("device", deviceName)
      self.addKV("board", boardName)
      self.addKV("serial", deviceId)
      self.addKV("build", buildId)
      self.addKV("data size", dataPartitionSize)
    packages = getChildrenWithTag(testResult, "TestPackage")
    for package in packages:
      casesFromChild = parseSuite(package, "")
      self.results.update(casesFromChild)
    #print self.results.keys()

  def addKV(self, key, value):
    self.infoKeys.append(key)
    self.infoValues.append(value)

  def getResults(self):
    return self.results

  def getKeys(self):
    return self.infoKeys

  def getValues(self):
    return self.infoValues

  def getDeviceName(self):
    return self.getInfoV("device")

  def getInfoV(self, key):
    if key in self.infoKeys:
      return self.infoValues[self.infoKeys.index(key)]
    else:
      return "unknown"

def executeWithResult(command):
  p = subprocess.Popen(command.split(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
  out, err = p.communicate()
  return out

def parseReports(path):
  deviceResults = []
  xmls = executeWithResult("find " + path + " -name testResult.xml -print")
  print "xml files found :"
  print xmls
  for xml in xmls.splitlines():
    result = Result(xml)
    deviceResults.append(result)
  reportInfo = {}
  keys = ["device", "board", "serial", "build", "data size"]
  numDevices = len(deviceResults)
  for i in xrange(len(keys)):
    values = []
    for j in xrange(numDevices):
      values.append(str(deviceResults[j].getInfoV(keys[i])))
    reportInfo[keys[i]] = values
  #print reportInfo

  tests = []
  for deviceResult in deviceResults:
    for key in deviceResult.getResults().keys():
      if not key in tests:
        tests.append(key)
  #print tests

  reportTests = {}
  for i in xrange(len(tests)):
    test = tests[i]
    reportTests[test] = []
    for j in xrange(numDevices):
      values = {}
      if deviceResults[j].getResults().has_key(test):
        result = deviceResults[j].getResults()[test]
        values["result"] = result.getResult()
        values["summary"] = result.getSummary()
        values["details"] = result.getDetails()
        values["device"] = deviceResults[j].getDeviceName()
      # even if report does not have test, put empty dict
      # otherwise, there is no way to distinguish results from the same device
      reportTests[test].append(values)
  #print reportTests
  return (reportInfo, reportTests)

def main(argv):
  if len(argv) < 3:
    print "get_csv_report.py cts_report_dir output_file"
    sys.exit(1)
  reportPath = os.path.abspath(argv[1])
  outputCsv = os.path.abspath(argv[2])

  (reportInfo, reportTests) = parseReports(reportPath)

  with open(outputCsv, 'w') as f:
    for key in reportInfo:
      f.write(key)
      for value in reportInfo[key]:
        f.write(',')
        f.write(value)
      f.write('\n')
    sortedTest = sorted(reportTests)
    for test in sortedTest:
      f.write(test)
      for report in reportTests[test]:
        f.write(',')
        if 'summary' in report:
          summaryValues = report['summary'].values()
          if len(summaryValues) > 0:
            f.write(summaryValues[0])
        # else: no data printed but just empty cell
      # close a test with line
      f.write('\n')

if __name__ == '__main__':
  main(sys.argv)
