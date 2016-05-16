"""
 File:  
 JetDebug.py
 
 Contents and purpose:
 Dumps info from various jet file structures for debugging
 
 Copyright (c) 2008 Android Open Source Project
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

from JetUtils import *

def DumpEvent(evt):
    print("event_name: %s" % evt.event_name)
    print("event_type: %s" % evt.event_type)
    print("event_id: %d" % evt.event_id)
    print("track_num: %d" % evt.track_num)
    print("channel_num: %d" % evt.channel_num)
    print("event_start: %s" % evt.event_start)
    print("event_end: %s" % evt.event_end)
    
def DumpQueueSeg(queueSeg):
    print("name: %s" % queueSeg.name)
    print("userID: %d" % queueSeg.userID)
    print("seg_num: %d" % queueSeg.seg_num)
    print("dls_num: %d" % queueSeg.dls_num)
    print("repeat: %d" % queueSeg.repeat)
    print("transpose: %d" % queueSeg.transpose)
    print("mute_flags: %d" % queueSeg.mute_flags)

def DumpSegments1(segments): 
    for segment in segments:
        DumpSegment(segment)
        
def DumpSegment(segment):
        print("userID: %d" % segment.userID)
        print("name: %s" % segment.name)
        print("seg_num: %d" % segment.seg_num)
        print("dls_num: %d" % segment.dls_num)
        print("repeat: %d" % segment.repeat)
        print("transpose: %d" % segment.transpose)
        print("mute_flags: %d" % segment.mute_flags)
    
def DumpSegments(segments): 
    for segment in segments:
        DumpSegment1(segment)
        
def DumpSegment1(segment):
    print(segment.segname)
    print(segment.filename)
    print(segment.start)
    print(segment.end)
    print(segment.length)
    print(segment.output)
    print(segment.quantize)
    print(segment.dlsfile)
    print(segment.dump_file)

     
