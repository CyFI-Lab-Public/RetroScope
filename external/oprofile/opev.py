#! /usr/bin/env python
"""
Read oprofile events file, generate C data struct for Android opcontrol.

Android does not use script for opcontrol, they use a C binary, which
has embedded data structures with the event set that is supported.
Initially that is just Arm V6 and V7.

This tool allows us to convert various MIPS cpu event files for
inclusion, and should work with other processor arch's as well.

Neither Arm or Mips uses unit_masks, so that file is ignored.

Event entries in file look like this:

    event:0x1 counters:0,1 um:zero minimum:500 name:INSTRUCTIONS : Instructions completed

The format is key:value. A single : appears at the end of line
and the remaining text is the description
"""

import os, sys

def number(s):
    if s == 'zero':
        return '0'
    if s == 'one':
        return '1'
    if s[0] == 'x':
        return '0'+s
    
def parse_event(line,ovf):
    ''' return dictionary of items from one line of event file '''
    dict = {}
    fields = line.split(None, 1)
    while (fields):
        first = fields[0].split(':', 1)
        if first[0] == 'include':
            ev(first[1] + "/events", ovf)
            return None
        line = fields[1]
        if first[0] == 'um':
            first[1] = number(first[1])
        if first[0] == '':
            dict['description'] = fields[1]
            fields = None;
        else:
            dict[first[0]] = first[1]
            fields = line.split(None, 1)
    return dict

def parse_ctr(s):
    ''' convert comma separated list of integers x,y,... , to CTR(x) | CTR(y) | ... '''
    if s == 'cpuid':
        return 0
    ctrs = s.split(',')
    c = ''
    for i in range(len(ctrs)-1):
        c += ("CTR(%s) | " % ctrs[i])
    c += ("CTR(%s)" % ctrs[-1])
    return c

def ev(fname,ovf):
    ''' read file, parse, generate C data struct to file ovf '''
    evf = open(fname, "r")
    all_lines = evf.readlines()
    lines = [s.strip() for s in all_lines if s.strip()]     # strip blanks
    lines = [s for s in lines if not s.startswith('#')]     # strip comments
    eventlist = [parse_event(line,ovf) for line in lines]

    ovf.write("// events from file %s\n" % fname)
    for d in eventlist:
        if d!=None:
            ovf.write('    {%s, %s, %s, "%s",\n' % (d['event'], parse_ctr(d['counters']), d['um'], d['name']))
            ovf.write('     "%s"},\n' % d['description'])


if __name__ == "__main__" :
    if len(sys.argv) != 2:
        fname = "events/mips/24K/events"    # convenient testing
    else:
        fname = sys.argv[1]
    ovf = open(fname + ".h", "w")
    ev(fname, ovf)
