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
import os
import sys
from xml.dom import Node
from xml.dom import minidom

def getChildrenWithTag(parent, tagName):
    children = []
    for child in  parent.childNodes:
        if (child.nodeType == Node.ELEMENT_NODE) and (child.tagName == tagName):
            #print "parent " + parent.getAttribute("name") + " " + tagName +\
            #    " " + child.getAttribute("name")
            children.append(child)
    return children

def parseSuite(suite, parentName):
    if parentName != "":
        parentName += '.'
    failedCases = []
    childSuites = getChildrenWithTag(suite, "TestSuite")
    for child in childSuites:
        for failure in parseSuite(child, parentName + child.getAttribute("name")):
            failedCases.append(failure)
    childTestCases = getChildrenWithTag(suite, "TestCase")
    for child in childTestCases:
        className = parentName + child.getAttribute("name")
        for test in getChildrenWithTag(child, "Test"):
            if test.getAttribute("result") != "pass":
                failureName = className + "#" + test.getAttribute("name")
                failedCases.append(failureName)
    #if len(failedCases) > 0:
    #    print failedCases
    return failedCases

def getFailedCases(resultXml):
    failedCases = []
    doc = minidom.parse(resultXml)
    testResult = doc.getElementsByTagName("TestResult")[0]
    packages = getChildrenWithTag(testResult, "TestPackage")
    for package in packages:
        casesFromChild = parseSuite(package, "")
        for case in casesFromChild:
            if case not in failedCases:
                failedCases.append(case)

    return failedCases

def main(argv):
    if len(argv) < 3:
        print "rerun.py cts_path result_xml [-s serial]"
        print " cts_path should end with android-cts"
        sys.exit(1)
    ctsPath = os.path.abspath(argv[1])
    resultXml = os.path.abspath(argv[2])
    deviceSerial = ""
    if len(argv) > 3:
        if argv[3] == "-s":
            deviceSerial = argv[4]

    failedCases = getFailedCases(resultXml)
    print "Re-run follwong cases:"
    for failure in failedCases:
        print " " + failure
    for failure in failedCases:
        [className, methodName] = failure.split('#')
        command = ctsPath + "/tools/cts-tradefed run singleCommand cts"
        if deviceSerial != "":
            command += " --serial " + deviceSerial
        command += " --class " + className + " --method " + methodName
        print command
        os.system(command)

if __name__ == '__main__':
    main(sys.argv)
