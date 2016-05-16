#
# Usage: Fill in the configuration variables.  It will download the feed
# for it, parse it, and print out test cases to add to the unit test.
#

EMAIL = "onoratoj@gmail.com"
PRIVATE_COOKIE = "432802670aefa458daf036597ec8136b"
START_DATE = ("2006","01","01")
END_DATE = ("2009","01","01")



import sys, urllib, re
from xml.dom import minidom

def fmt(n):
    if n < 10:
        return "0" + str(n)
    else:
        return str(n)

def makeDate(d):
    return d[0] + "-" + d[1] + "-" + d[2]

def makeZDate(d):
    return d[0] + d[1] + d[2] + "T000000Z"

url = "http://www.google.com/calendar/feeds/onoratoj@gmail.com/private-" \
        + PRIVATE_COOKIE + "/composite?start-min=" + makeDate(START_DATE) \
        + "&start-max=" + makeDate(END_DATE)

#data = open("out.xml")
data = urllib.urlopen(url)

DTSTART_TZID = re.compile("DTSTART;TZID=(.*):(.*)")
DTSTART = re.compile("DTSTART:(.*)")
DURATION = re.compile("DURATION:(.*)")
RRULE = re.compile("RRULE:(.*)")
TIME = re.compile("(....)-(..)-(..)T(..):(..):(..)....([+-])(..):(..)")
TIMEZ = re.compile("(....)-(..)-(..)T(..):(..):(..)....Z")

def stripTimezone(str):
    lines = str.split("\n")
    drop = False
    result = []
    for line in lines:
        if line == "BEGIN:VTIMEZONE":
            drop = True
        if not drop:
            result.append(line)
        if line == "END:VTIMEZONE":
            drop = False
    return result

def fixInstance(s):
    m = TIME.match(s[0])
    if m:
        if m.group(7) == "+":
            sign = -1
        else:
            sign = 1
        hour = int(m.group(4)) + (sign * int(m.group(8)))
        return m.group(1) + m.group(2) + m.group(3) + "T" + fmt(hour) \
                + m.group(5) + m.group(6) + "Z"
    m = TIMEZ.match(s[0])
    if m:
        return m.group(1) + m.group(2) + m.group(3) + "T" + m.group(4) \
                + m.group(5) + m.group(6) + "Z"
    return s[0]

dom = minidom.parse(data)
root = dom.documentElement

entries = root.getElementsByTagName("entry")

for entry in entries:
    recurrences = entry.getElementsByTagName("gd:recurrence")
    dtstart = ""
    tzid = ""
    duration = ""
    rrule = ""
    if len(recurrences) > 0:
        recurrence = recurrences[0]
        s = ""
        for c in recurrence.childNodes:
            s = s + c.nodeValue
        lines = stripTimezone(s)
        for s in lines:
            re_dtstart = DTSTART_TZID.match(s)
            if re_dtstart:
                dtstart = re_dtstart.group(2)
                tzid = re_dtstart.group(1)
            re_dtstart = DTSTART.match(s)
            if re_dtstart:
                dtstart = re_dtstart.group(1)
            re_duration = DURATION.match(s)
            if re_duration:
                duration = re_duration.group(1)
            re_rrule = RRULE.match(s)
            if re_rrule:
                rrule = re_rrule.group(1)
    whens = entry.getElementsByTagName("gd:when")
    instances = []
    for w in whens:
        startTime = w.getAttribute("startTime")
        endTime = w.getAttribute("endTime")
        instances.append((startTime,endTime))

    instances = map(fixInstance, instances)
    instances.sort()
    if dtstart != "":
        title = ""
        for c in entry.getElementsByTagName('title')[0].childNodes:
            title = title + c.nodeValue

        print "            // " + title
        print "            test(\"" + dtstart + "\","
        print "                    \"" + rrule + "\","
        print "                    \"" + makeZDate(START_DATE) \
                                    + "\", \"" + makeZDate(END_DATE) + "\","
        print "                    new String[] {"
        for i in instances:
            print "                        \"" + i + "\","
        print "                    });"
        print


