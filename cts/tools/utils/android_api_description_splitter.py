#! /usr/bin/python
#
# Copyright 2008, The Android Open Source Project
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
#
# This script is used to split the jdiff xml into several smaller xml files
# so that we could avoid the xml resource limit in Android platform.
#
# Usage:
#    android_api_description.py xmlfile tagname
#
# The script will do the following:
#    1. Read the xml file and generate DOM tree
#    2. Generate xml file for each tagname.
#
# Example:
# xml source:
#    <Root>
#        <A name="i">
#            <B>1</B>
#            <B>2</B>
#        </A>
#        <A name="ii">
#            <B>3</B>
#        </A>
#    </Root>
# 
# when the tagname is specified as A, it will generate two xml files:
# first one's source:
#    <Root>
#        <A name="i">
#            <B>1</B>
#            <B>2</B>
#        </A>
#    </Root>
# second one's source:
#    <Root>
#        <A name="ii">
#            <B>3</B>
#        </A>
#    </Root>
#
# when the tagname is specified as B, it will generated three xml files:
# first one's source:
#    <Root>
#        <A name="i">
#            <B>1</B>
#        </A>
#    </Root>
# second one's source:
#    <Root>
#        <A name="i">
#            <B>2</B>
#        </A>
#    </Root>
# third one's source:
#    <Root>
#        <A name="ii">
#            <B>3</B>
#        </A>
#    </Root>
#
# NOTE: 
#    1. Currently just suppor the top level element
#    2. Use the name attribute of the specified element as the file name
#    3. Currently will remove all the doc element. - workaround for jdiff xml
#
import os, sys;
import xml.dom.minidom;

"""Split the jdiff xml into several smaller xml files by specified tag.
"""
class XMLSplitter:
    def __init__(self, xmlfile, outPath):
        self.doc = xml.dom.minidom.parse(xmlfile)
        self.root = self.doc.documentElement
        self.out = os.path.join(outPath, "xml")
        if not os.path.isdir(self.out):
            os.makedirs(self.out)
        return

    def split(self, tag):

        elemlist = self.doc.getElementsByTagName(tag)

        for elem in elemlist:
            elem = self.__trimElem(elem)
            self.__generateFile(elem)

        return

    def __trimElem(self, elem):
        children = []
        for child in elem.childNodes:
            if child.nodeType == xml.dom.minidom.Node.ELEMENT_NODE:
                children.append(child)

        for child in children:
            if child.nodeName == "doc":
                elem.removeChild(child)
                children.remove(child)

        for child in children:
            child = self.__trimElem(child)

        return elem


    def __generateFile(self, elem):
        self.__removeAllChild(self.root)

        filename = os.path.join(self.out, elem.getAttribute("name").replace(".", "_").lower() + ".xml")

        doc = xml.dom.minidom.Document()
        doc.appendChild(self.root)

        self.root.appendChild(elem)

        fd = open(filename, "w")
        fd.write(doc.toxml())
        fd.close

        return

    def __removeAllChild(self, elem):
        children = []
        for child in elem.childNodes:
            children.append(child)

        for child in children:
            elem.removeChild(child)

        return

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print "Usage: splitxml.py xmlfile outpath tagname"
        sys.exit(1)

    xmlsplitter = XMLSplitter(sys.argv[1], sys.argv[2])

    xmlsplitter.split(sys.argv[3])

