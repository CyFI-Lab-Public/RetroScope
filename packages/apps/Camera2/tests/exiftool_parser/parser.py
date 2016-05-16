#!/usr/bin/env python
#
# This parser parses the output from Phil Harvey's exiftool (version 9.02)
# and convert it to xml format. It reads exiftool's output from stdin and
# write the xml format to stdout.
#
# In order to get the raw infomation from exiftool, we need to enable the verbose
# flag (-v2) of exiftool.
#
# Usage:
#      exiftool -v2 img.jpg | ./parser.py >> output.xml
#
#

import os
import sys
import re

text = sys.stdin.read()

print """<?xml version="1.0" encoding="utf-8"?>"""
print "<exif>"

# find the following two groups of string:
#
# 1. tag:
#
# | | | x) name = value
# | | |     - Tag 0x1234
#
# 2. IFD indicator:
#
# | | | + [xxx directory with xx entries]
#
p = re.compile(
        "(((?:\| )+)[0-9]*\)(?:(?:.*? = .*?)|(?:.*? \(SubDirectory\) -->))\n.*?- Tag 0x[0-9a-f]{4})" + "|"
        + "(((?:\| )*)\+ \[.*? directory with [0-9]+ entries]$)"
        , re.M)
tags = p.findall(text)

layer = 0
ifds = []

for s in tags:
    # IFD indicator
    if s[2]:
        l = len(s[3])
        ifd = s[2][l + 3:].split()[0]
        new_layer = l / 2 + 1
        if new_layer > layer:
            ifds.append(ifd)
        else:
            for i in range(layer - new_layer):
                ifds.pop()
            ifds[-1] = ifd
        layer = new_layer
    else:
        l = len(s[1])
        s = s[0]
        new_layer = l / 2
        if new_layer < layer:
            for i in range(layer - new_layer):
                ifds.pop()
        layer = new_layer

        # find the ID
        _id = re.search("0x[0-9a-f]{4}", s)
        _id = _id.group(0)

        # find the name
        name = re.search("[0-9]*?\).*?(?:(?: = )|(?: \(SubDirectory\) -->))", s)
        name = name.group(0).split()[1]

        # find the raw value in the parenthesis
        value = re.search("\(SubDirectory\) -->", s)
        if value:
            value = "NO_VALUE"
        else:
            value = re.search("\(.*\)\n", s)
            if (name != 'Model' and value):
                value = value.group(0)[1:-2]
            else:
                value = re.search("=.*\n", s)
                value = value.group(0)[2:-1]
                if "[snip]" in value:
                    value = "NO_VALUE"

        print ('    <tag ifd="' + ifds[-1] + '" id="'
            + _id + '" name="' + name +'">' + value + "</tag>")
print "</exif>"
