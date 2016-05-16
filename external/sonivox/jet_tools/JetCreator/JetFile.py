"""
 File:  
 JetFile.py
 
 Contents and purpose:
 Auditions a jet file to simulate interactive music functions
 
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

from __future__ import with_statement

import logging
import ConfigParser
import struct
import os
import sys
import midifile

from JetUtils import *
from JetDefs import *

VERSION = '0.1'

# JET file defines
JET_HEADER_STRUCT = '<4sl'
JET_HEADER_TAG = 'JET '
JET_VERSION = 0x01000000

# JET chunk tags
JET_INFO_CHUNK = 'JINF'
JET_SMF_CHUNK = 'JSMF'
JET_DLS_CHUNK = 'JDLS'

# JINF defines
JINF_STRUCT = '<4sl4sl4sl4sl'
JINF_JET_VERSION = 'JVER'
JINF_NUM_SMF_CHUNKS = 'SMF#'
JINF_NUM_DLS_CHUNKS = 'DLS#'

# JCOP defines
JCOP_STRUCT = '<4sl'
JCOP_CHUNK = 'JCOP'

# JAPP defines
JAPP_STRUCT = '<4sl'
JAPP_CHUNK = 'JAPP'

# config file defines
OUTPUT_SECTION = 'output'
OUTPUT_FILENAME = 'filename'
OUTPUT_COPYRIGHT = 'copyright'
OUTPUT_APP_DATA = 'app_data'
OUTPUT_CHASE_CONTROLLERS = 'chase_controllers'
OUTPUT_OMIT_EMPTY_TRACKS = 'omit_empty_tracks'
SEGMENT_SECTION = 'segment'
SEGMENT_FILENAME = 'filename'
SEGMENT_DLSFILE = 'dlsfile'
SEGMENT_NAME = 'segname'
SEGMENT_START = 'start'
SEGMENT_END = 'end'
SEGMENT_END_MARKER = 'end_marker'
SEGMENT_QUANTIZE = 'quantize'
SEGMENT_OUTPUT = 'output'
SEGMENT_LENGTH = 'length'
SEGMENT_DUMP_FILE = 'dump'
SEGMENT_TRANSPOSE = 'transpose'
SEGMENT_REPEAT = 'repeat'
SEGMENT_MUTE_FLAGS = 'mute_flags'
LIBRARY_SECTION = 'libraries'
LIBRARY_FILENAME = 'lib'
CLIP_PREFIX = 'clip'
APP_PREFIX = 'app'

# JET events
JET_EVENT_MARKER = 102
JET_MARKER_LOOP_END = 0
JET_EVENT_TRIGGER_CLIP = 103

class JetSegment (object):
    """ Class to hold segments """
    def __init__ (self, segname, filename, start=None, end=None, length=None, output=None, quantize=None, jetevents=[], dlsfile=None, dump_file=None, transpose=0, repeat=0, mute_flags=0):
        self.segname = segname
        self.filename = filename
        self.dlsfile = dlsfile
        self.start = start
        self.end = end
        self.length = length
        self.output = output
        self.quantize = quantize
        self.dump_file = dump_file
        self.jetevents = jetevents
        #API FIELDS FOR UI
        self.transpose = transpose
        self.repeat = repeat
        self.mute_flags = mute_flags        

class JetEvent (object):
    """ Class to hold events """
    def __init__(self, event_name, event_type, event_id, track_num, channel_num, event_start, event_end):
        self.event_name = event_name
        self.event_type = event_type
        self.event_id = event_id
        self.track_num = track_num
        self.channel_num = channel_num
        self.event_start = event_start
        self.event_end = event_end
  
class JetFileException (Exception):
    """ Exceptions class """
    def __init__ (self, msg):
        self.msg = msg
    def __str__ (self):
        return self.msg

class JetSegmentFile (midifile.MIDIFile):
    def ConvertMusicTimeToTicks (self, s):
        measures, beats, ticks = s.split(':',3)
        return self.ConvertToTicks(int(measures), int(beats), int(ticks))

    def ExtractEvents (self, start, end, length, quantize, chase_controllers):
        if (start is None) and (end is None) and (length is None):
            logging.debug('ExtractEvents: No change')
            return

        if start is not None:
            start = self.ConvertMusicTimeToTicks(start)
        else:
            start = 0
        if end is not None:
            end = self.ConvertMusicTimeToTicks(end)
        elif length is not None:
            length = self.ConvertMusicTimeToTicks(length)
            end = start + length

        if quantize is not None:
            quantize = int(quantize)
        else:
            quantize = 0

        self.Trim(start, end, quantize, chase_controllers=chase_controllers)
        #self.DumpTracks()

    def SyncClips (self):
        """Add controller events to the start of a clip to keep it synced."""
        values = None
        last_seq = 0
        for track in self.tracks:
            for event in track.events:

                # find start of clip and chase events from last save point
                if (event.msg_type == midifile.CONTROL_CHANGE) and \
                        (event.controller == JET_EVENT_TRIGGER_CLIP) and \
                        ((event.value & 0x40) == 0x40):
                    logging.debug('Syncing clip at %d ticks' % event.ticks)
                    values = track.events.ChaseControllers(event.seq, last_seq, values)
                    
                    #BTH; Seems to fix chase controller bug when multiple clips within segment
                    #last_seq = event.seq

                    # generate event list from default values
                    clip_events = values.GenerateEventList(event.ticks)
                    
                    #for evt in clip_events:
                    #    logging.info(evt)
                        
                    track.events.InsertEvents(clip_events, event.seq + 1)

    def AddJetEvents (self, jetevents):
        for jet_event in jetevents:
            if jet_event.event_type == JetDefs.E_CLIP:
                #DumpEvent(jet_event)
                
                # sanity check
                if jet_event.track_num >= len(self.tracks):
                    raise JetFileException('Track number %d of out of range for clip' % jet_event.track_num)
                if jet_event.channel_num > 15:
                    raise JetFileException('Channel number %d of out of range for clip' % jet_event.channel_num)
                if jet_event.event_id > 63:
                    raise JetFileException('event_id %d of out of range for clip' % jet_event.event_id)
    
                logging.debug('Adding trigger event for clip %d @ %s and %s' % (jet_event.event_id, jet_event.event_start, jet_event.event_end))
    
                events = midifile.EventList()
                events.append(midifile.ControlChangeEvent(
                    self.ConvertMusicTimeToTicks(jet_event.event_start),
                    0,
                    jet_event.channel_num,
                    JET_EVENT_TRIGGER_CLIP,
                    jet_event.event_id | 0x40))
    
                events.append(midifile.ControlChangeEvent(
                    self.ConvertMusicTimeToTicks(jet_event.event_end),
                    sys.maxint,
                    jet_event.channel_num,
                    JET_EVENT_TRIGGER_CLIP,
                    jet_event.event_id))
    
                # merge trigger events
                self.tracks[jet_event.track_num].events.MergeEvents(events)
                
            elif jet_event.event_type == JetDefs.E_EOS:
                if jet_event.track_num >= len(self.tracks):
                    raise JetFileException('Track number %d of out of range for end marker' % jet_event.track_num)
                if jet_event.channel_num > 15:
                    raise JetFileException('Channel number %d of out of range for end marker' % jet_event.channel_num)
    
                events = midifile.EventList()
                logging.debug('Adding end marker at %s' % jet_event.event_start)
                events.append(midifile.ControlChangeEvent(
                    self.ConvertMusicTimeToTicks(jet_event.event_start),
                    0,
                    jet_event.channel_num,
                    JET_EVENT_MARKER,
                    JET_MARKER_LOOP_END))
                self.tracks[jet_event.track_num].events.MergeEvents(events)

            elif jet_event.event_type == JetDefs.E_APP:
                if jet_event.track_num >= len(self.tracks):
                    raise JetFileException('Track number %d of out of range for app marker' % jet_event.track_num)
                if jet_event.channel_num > 15:
                    raise JetFileException('Channel number %d of out of range for app marker' % jet_event.channel_num)
                if jet_event.event_id > 83 or jet_event.event_id < 80:
                    raise JetFileException('EventID %d out of range for application controller' % jet_event.event_id)
    
                events = midifile.EventList()
                logging.debug('Adding application controller at %s' % jet_event.event_start)
                events.append(midifile.ControlChangeEvent(
                    self.ConvertMusicTimeToTicks(jet_event.event_start),
                    0,
                    jet_event.channel_num,
                    jet_event.event_id,
                    jet_event.event_id))
                self.tracks[jet_event.track_num].events.MergeEvents(events)

class JetFile (object):
    """Write a JET file based on a configuration file."""
    def __init__ (self, config_file, options):
        self.config_file = config_file
        self.config = config = ConfigParser.ConfigParser()
        if self.config_file == "":
            self.InitializeConfig(JetDefs.UNTITLED_FILE)
        if not FileExists(self.config_file):
            self.InitializeConfig(self.config_file)
        
        config.read(self.config_file)
        self.ParseConfig(options)

    def DumpConfig (self):
        """Drump configuration to log file."""
        # dump configuration
        config = self.config
        for section in config.sections():
            logging.debug('[%s]' % section)
            for option, value in config.items(section):
                logging.debug('%s: %s' % (option, value))

    def ParseConfig (self, options):
        """Validate the configuration."""
        # check for output name
        config = self.config
        if config.has_option(OUTPUT_SECTION, OUTPUT_FILENAME):
            config.filename = config.get(OUTPUT_SECTION, OUTPUT_FILENAME)
        else:
            raise JetFileException('No output filename in configuration file')
        if config.filename == '' or config.filename == None:
            config.filename = FileJustRoot(self.config_file) + ".JET"
        config.chase_controllers = True
        if config.has_option(OUTPUT_SECTION, OUTPUT_CHASE_CONTROLLERS):
            try:
                config.chase_controllers = config.getboolean(OUTPUT_SECTION, OUTPUT_CHASE_CONTROLLERS)
            except:
                pass

        config.delete_empty_tracks = False
        if config.has_option(OUTPUT_SECTION, OUTPUT_OMIT_EMPTY_TRACKS):
            try:
                config.delete_empty_tracks = config.getboolean(OUTPUT_SECTION, OUTPUT_OMIT_EMPTY_TRACKS)
            except:
                pass
                
        config.copyright = None
        if config.has_option(OUTPUT_SECTION, OUTPUT_COPYRIGHT):
            config.copyright = config.get(OUTPUT_SECTION, OUTPUT_COPYRIGHT)

        config.app_data = None
        if config.has_option(OUTPUT_SECTION, OUTPUT_APP_DATA):
            config.app_data = config.get(OUTPUT_SECTION, OUTPUT_APP_DATA)

        # count segments
        segments = []
        seg_num = 0
        while 1:

            # check for segment section
            segment_name = SEGMENT_SECTION + str(seg_num)
            if not config.has_section(segment_name):
                break

            # initialize some parameters
            start = end = length = output = end_marker = dlsfile = dump_file = None
            transpose = repeat = mute_flags = 0
            jetevents = []

            # get the segment parameters
            segname = config.get(segment_name, SEGMENT_NAME)
            filename = config.get(segment_name, SEGMENT_FILENAME)
            if config.has_option(segment_name, SEGMENT_DLSFILE):
                dlsfile = config.get(segment_name, SEGMENT_DLSFILE)
            if config.has_option(segment_name, SEGMENT_START):
                start = config.get(segment_name, SEGMENT_START)
            if config.has_option(segment_name, SEGMENT_END):
                end = config.get(segment_name, SEGMENT_END)
            if config.has_option(segment_name, SEGMENT_LENGTH):
                length = config.get(segment_name, SEGMENT_LENGTH)
            if config.has_option(segment_name, SEGMENT_OUTPUT):
                output = config.get(segment_name, SEGMENT_OUTPUT)
            if config.has_option(segment_name, SEGMENT_QUANTIZE):
                quantize = config.get(segment_name, SEGMENT_QUANTIZE)
            if config.has_option(segment_name, SEGMENT_DUMP_FILE):
                dump_file = config.get(segment_name, SEGMENT_DUMP_FILE)
            #API FIELDS
            if config.has_option(segment_name, SEGMENT_TRANSPOSE):
                transpose = config.get(segment_name, SEGMENT_TRANSPOSE)
            if config.has_option(segment_name, SEGMENT_REPEAT):
                repeat = config.get(segment_name, SEGMENT_REPEAT)
            if config.has_option(segment_name, SEGMENT_MUTE_FLAGS):
                mute_flags = config.get(segment_name, SEGMENT_MUTE_FLAGS)
            
            if config.has_option(segment_name, SEGMENT_END_MARKER):
                end_marker = config.get(segment_name, SEGMENT_END_MARKER)
                track_num, channel_num, event_time = end_marker.split(',',2)
                #jetevents.append((JetDefs.E_EOS, 0, int(track_num), int(channel_num), event_time, ''))
                jetevents.append(JetEvent(JetDefs.E_EOS, JetDefs.E_EOS, 0, int(track_num), int(channel_num), event_time, event_time))

            # check for jetevents
            for jetevent, location in config.items(segment_name):
                if jetevent.startswith(CLIP_PREFIX):
                    event_name, event_id, track_num, channel_num, event_start, event_end = location.split(',', 5)
                    jetevents.append(JetEvent(event_name, JetDefs.E_CLIP, int(event_id), int(track_num), int(channel_num), event_start, event_end))

            # check for appevents
            for jetevent, location in config.items(segment_name):
                if jetevent.startswith(APP_PREFIX):
                    event_name, event_id, track_num, channel_num, event_start, event_end = location.split(',', 5)
                    jetevents.append(JetEvent(event_name, JetDefs.E_APP, int(event_id), int(track_num), int(channel_num), event_start, event_end))
            
            segments.append(JetSegment(segname, filename, start, end, length, output, quantize, jetevents, dlsfile, dump_file, int(transpose), int(repeat), int(mute_flags)))
            seg_num += 1

        self.segments = segments
        if not len(segments):
            #TODO: Check for segments when writing
            #raise JetFileException('No segments defined in configuration file')
            pass

        # count libraries
        libraries = []
        lib_num = 0
        while 1:
            library_name = LIBRARY_FILENAME + str(lib_num)
            if not config.has_option(LIBRARY_SECTION, library_name):
                break
            libraries.append(config.get(LIBRARY_SECTION, library_name))
            lib_num += 1
        self.libraries = libraries

    def WriteJetFileFromConfig (self, options):
        """Write JET file from config file."""
        
        # open the output file and write the header
        output_file = open(self.config.filename, 'wb')
        jet_header = struct.pack(JET_HEADER_STRUCT, JET_HEADER_TAG, 0)
        output_file.write(jet_header)

        # write the JINF chunk
        jet_info = struct.pack(JINF_STRUCT,
            JET_INFO_CHUNK, struct.calcsize(JINF_STRUCT) - 8,
            JINF_JET_VERSION, JET_VERSION,
            JINF_NUM_SMF_CHUNKS, len(self.segments),
            JINF_NUM_DLS_CHUNKS, len(self.libraries))
        output_file.write(jet_info)

        # write the JCOP chunk (if any)
        if self.config.copyright is not None:
            size = len(self.config.copyright) + 1
            if size & 1:
                size += 1
                extra_byte = True
            else:
                extra_byte = False
            jet_copyright = struct.pack(JCOP_STRUCT, JCOP_CHUNK, size)
            output_file.write(jet_copyright)
            output_file.write(self.config.copyright)
            output_file.write(chr(0))
            if extra_byte:
                output_file.write(chr(0))

        # write the app data chunk (if any)
        if self.config.app_data is not None:
            size = os.path.getsize(self.config.app_data)
            if size & 1:
                size += 1
                extra_byte = True
            else:
                extra_byte = False
            jet_app_data = struct.pack(JAPP_STRUCT, JAPP_CHUNK, size)
            output_file.write(jet_app_data)
            with open(self.config.app_data, 'rb') as f:
                output_file.write(f.read())
            if extra_byte:
                output_file.write(chr(0))

        # copy the MIDI segments
        seg_num = 0
        for segment in self.segments:
            logging.debug('Writing segment %d' % seg_num)

            # open SMF file and read it
            jet_segfile = JetSegmentFile(segment.filename, 'rb')
            jet_segfile.ReadFromStream()

            # insert events
            jet_segfile.AddJetEvents(segment.jetevents)

            # trim to length specified in config file
            jet_segfile.ExtractEvents(segment.start, segment.end, segment.length, segment.quantize, self.config.chase_controllers)

            # chase controller events and fix them
            if self.config.chase_controllers:
                jet_segfile.SyncClips()

            # delete empty tracks
            if self.config.delete_empty_tracks:
                jet_segfile.DeleteEmptyTracks()

            # write separate output file if requested
            if segment.output is not None:
                jet_segfile.SaveAs(segment.output)

            # write dump file
            if segment.dump_file is not None:
                with open(segment.dump_file, 'w') as f:
                    jet_segfile.DumpTracks(f)

            # write the segment header
            header_pos = output_file.tell()
            smf_header = struct.pack(JET_HEADER_STRUCT, JET_SMF_CHUNK, 0)
            output_file.write(smf_header)
            start_pos = output_file.tell()

            # write SMF file to output file
            jet_segfile.Write(output_file, offset=start_pos)
            jet_segfile.close()

            # return to segment header and write actual size
            end_pos = output_file.tell()
            file_size = end_pos - start_pos
            if file_size & 1:
                file_size += 1
                end_pos += 1
            output_file.seek(header_pos, 0)
            smf_header = struct.pack(JET_HEADER_STRUCT, JET_SMF_CHUNK, file_size)
            output_file.write(smf_header)
            output_file.seek(end_pos, 0)

            seg_num += 1

        # copy the DLS segments
        for library in self.libraries:
            if FileExists(library):
                # open SMF file and get size
                lib_file = (open(library,'rb'))
                lib_file.seek(0,2)
                file_size = lib_file.tell()
                lib_file.seek(0)
    
                # write the library header
                dls_header = struct.pack(JET_HEADER_STRUCT, JET_DLS_CHUNK, file_size)
                output_file.write(dls_header)
    
                # copy DLS file to output file
                output_file.write(lib_file.read())
                lib_file.close()

        # write the header with the read data size
        file_size = output_file.tell()
        output_file.seek(0)
        jet_header = struct.pack(JET_HEADER_STRUCT, JET_HEADER_TAG, file_size - struct.calcsize(JET_HEADER_STRUCT))
        output_file.write(jet_header)
        output_file.close()
        
    def GetMidiFiles(self):
        """ Gets a list of midifiles """
        midiFiles = []
        for segment in self.segments:
            if segment.filename not in midiFiles:
                 midiFiles.append(segment.filename) 
        return midiFiles
            
    def GetLibraries(self):
        """ Gets the libraries """
        return self.libraries
    
    def GetEvents(self, segName):
        """ Gets the events for a segment """
        for segment in self.segments:
            if segment.segname == segName:
                return segment.jetevents
        return None
    
    def GetEvent(self, segName, eventName):
        """ Gets a single event from a segment """
        for segment in self.segments:
            if segment.segname == segName:
                for event in segment.jetevents:
                    if event.event_name == eventName:
                        return event
        return None
        
    def AddEvent(self, segname, event_name, event_type, event_id, track_num, channel_num, event_start, event_end):    
        """ Adds an event """
        for segment in self.segments:
            if segment.segname == segname:
                segment.jetevents.append(JetEvent(event_name, event_type, int(event_id), int(track_num), int(channel_num), event_start, event_end))
    
    def ReplaceEvents(self, segname, newEvents):
        """ Replaces all events """
        for segment in self.segments:
            if segment.segname == segname:
                segment.jetevents = newEvents
                return segment       
        
    def UpdateEvent(self, segname, orgeventname, event_name, event_type, event_id, track_num, channel_num, event_start, event_end):
        """ Updates an event """    
        for segment in self.segments:
            if segment.segname == segname:
                for jetevent in segment.jetevents:
                    if jetevent.event_name == orgeventname:
                        jetevent.event_name = event_name
                        jetevent.event_type = event_type
                        jetevent.event_id = event_id
                        jetevent.track_num = track_num
                        jetevent.channel_num = channel_num
                        jetevent.event_start = event_start
                        jetevent.event_end = event_end
                       
    def DeleteSegmentsMatchingPrefix(self, prefix):
        """ Deletes all segments matching name """
        iOnce = True
        iAgain = False
        while(iOnce or iAgain):
            iOnce = False
            iAgain = False
            for segment in self.segments:
                if segment.segname[0:len(prefix)].upper() == prefix.upper():
                    self.segments.remove(segment)
                    iAgain = True
                            
    def DeleteEvent(self, segname, event_name):
        """ Deletes an event """
        for segment in self.segments:
            if segment.segname == segname:
                for jetevent in segment.jetevents:
                    if jetevent.event_name == event_name:
                        segment.jetevents.remove(jetevent)

    def DeleteEventsMatchingPrefix(self, segname, prefix):
        """ Deletes all events matching name """
        for segment in self.segments:
            if segment.segname == segname:
                iOnce = True
                iAgain = False
                while(iOnce or iAgain):
                    iOnce = False
                    iAgain = False
                    for jetevent in segment.jetevents:
                        if jetevent.event_name[0:len(prefix)].upper() == prefix.upper():
                            segment.jetevents.remove(jetevent)
                            iAgain = True

    def MoveEvent(self, segname, movename, event_start, event_end):
        """ Move an event """    
        for segment in self.segments:
            if segment.segname == segname:
                for jetevent in segment.jetevents:
                    if jetevent.event_name == movename:
                        jetevent.event_start = event_start
                        jetevent.event_end = event_end
                        return
            
    def GetSegments(self):
        """ Gets all segments """
        return self.segments
   
    def GetSegment(self, segName):
        """ Gets one segment by name """
        for segment in self.segments:
            if segment.segname == segName:
                return segment
        return None
        
    def AddSegment(self, segname, filename, start, end, length, output, quantize, jetevents, dlsfile, dump_file, transpose, repeat, mute_flags):
        """ Adds a segment """
        if length == JetDefs.MBT_ZEROSTR:
            length = None
        if end == JetDefs.MBT_ZEROSTR:
            end = None
        self.segments.append(JetSegment(segname, filename, start, end, length, output, quantize, jetevents, dlsfile, dump_file, transpose, repeat, mute_flags))
    
    def UpdateSegment(self, orgsegname, segname, filename, start, end, length, output, quantize, jetevents, dlsfile, dump_file, transpose, repeat, mute_flags):
        """ Updates a segment """
        if length == JetDefs.MBT_ZEROSTR:
            length = None
        if end == JetDefs.MBT_ZEROSTR:
            end = None
        for segment in self.segments:
            if segment.segname == orgsegname:
                segment.segname = segname
                segment.filename = filename
                segment.start = start
                segment.end = end
                segment.length = length
                segment.output = output
                segment.quantize = quantize
                segment.dlsfile = dlsfile
                segment.transpose = transpose
                segment.repeat = repeat
                segment.mute_flags = mute_flags

    def MoveSegment(self, segname, start, end):
        """ Moves a segment """
        for segment in self.segments:
            if segment.segname == segname:
                segment.start = start
                segment.end = end
                return

    def DeleteSegment(self, segname):
        """ Deletes a segment """
        for segment in self.segments:
            if segment.segname == segname:
                self.segments.remove(segment)

    def SaveJetConfig(self, configFile):
        """ Saves the jet config file """
        if self.config.filename == '' or self.config.filename == None:
            self.config.filename = FileJustRoot(configFile) + ".JET"
        config = ConfigParser.ConfigParser()
        config.add_section(OUTPUT_SECTION)
        config.set(OUTPUT_SECTION, OUTPUT_FILENAME, self.config.filename)
        config.set(OUTPUT_SECTION, OUTPUT_CHASE_CONTROLLERS, self.config.chase_controllers)
        config.set(OUTPUT_SECTION, OUTPUT_OMIT_EMPTY_TRACKS, self.config.delete_empty_tracks)
        if self.config.copyright is not None:
            config.set(OUTPUT_SECTION, OUTPUT_COPYRIGHT, self.config.copyright)
        if self.config.app_data is not None:
            config.set(OUTPUT_SECTION, OUTPUT_APP_DATA, self.config.app_data)

        self.libraries = []
        seg_num = 0
        for segment in self.segments:
            segment_name = SEGMENT_SECTION + str(seg_num)
            config.add_section(segment_name)
            config.set(segment_name, SEGMENT_NAME, segment.segname)
            config.set(segment_name, SEGMENT_FILENAME, segment.filename)
            
            config.set(segment_name, SEGMENT_DLSFILE, segment.dlsfile)
            if FileExists(segment.dlsfile):
                if not segment.dlsfile in self.libraries:
                    self.libraries.append(segment.dlsfile)
            config.set(segment_name, SEGMENT_START, segment.start)
            if segment.end > JetDefs.MBT_ZEROSTR and len(segment.end) > 0:
                config.set(segment_name, SEGMENT_END, segment.end)
            if segment.length > JetDefs.MBT_ZEROSTR and len(segment.length) > 0:
                config.set(segment_name, SEGMENT_LENGTH, segment.length)
            config.set(segment_name, SEGMENT_OUTPUT, segment.output)
            config.set(segment_name, SEGMENT_QUANTIZE, segment.quantize)
            if segment.dump_file is not None:
                config.set(segment_name, SEGMENT_DUMP_FILE, segment.dump_file)
            config.set(segment_name, SEGMENT_TRANSPOSE, segment.transpose)
            config.set(segment_name, SEGMENT_REPEAT, segment.repeat)
            config.set(segment_name, SEGMENT_MUTE_FLAGS, segment.mute_flags)

            clip_num = 0
            app_num = 0
            for jet_event in segment.jetevents:
                if jet_event.event_type == JetDefs.E_CLIP:
                    clip_name = CLIP_PREFIX + str(clip_num)
                    s = "%s,%s,%s,%s,%s,%s" % (jet_event.event_name, jet_event.event_id, jet_event.track_num, jet_event.channel_num, jet_event.event_start, jet_event.event_end)
                    config.set(segment_name, clip_name, s)
                    clip_num += 1
                elif jet_event.event_type == JetDefs.E_APP:
                    app_name = APP_PREFIX + str(app_num)
                    s = "%s,%s,%s,%s,%s,%s" % (jet_event.event_name, jet_event.event_id, jet_event.track_num, jet_event.channel_num, jet_event.event_start, jet_event.event_end)
                    config.set(segment_name, app_name, s)
                    app_num += 1
                elif jet_event.event_type == JetDefs.E_EOS:
                    s = "%s,%s,%s" % (jet_event.track_num, jet_event.channel_num, jet_event.event_start)
                    config.set(segment_name, SEGMENT_END_MARKER, s)
                
            seg_num += 1
                        
        lib_num = 0
        config.add_section(LIBRARY_SECTION)
        for library in self.libraries:
            library_name = LIBRARY_FILENAME + str(lib_num)
            config.set(LIBRARY_SECTION, library_name, library)
            lib_num += 1
        
        FileKillClean(configFile)
        cfgfile = open(configFile,'w')
        config.write(cfgfile)
        cfgfile.close()
    
    def InitializeConfig(self, configFile):
        """ Initializes the values for an empty flag """
        self.config.filename = FileJustRoot(configFile)  + ".JET"
        self.config.chase_controllers = True
        self.config.delete_empty_tracks = False
        self.config.copyright = None
        self.config.app_data = None
        self.segments = []
        self.libraries = []
        self.config_file = configFile
        self.SaveJetConfig(configFile)
        
        

#---------------------------------------------------------------
# main
#---------------------------------------------------------------
if __name__ == '__main__':
    sys = __import__('sys')
    optparse = __import__('optparse')

    # parse command line options
    parser = optparse.OptionParser(version=VERSION)
    parser.set_defaults(log_level=logging.INFO, log_file=None)
    parser.add_option('-d', '--debug', action="store_const", const=logging.DEBUG, dest='log_level', help='Enable debug output')
    parser.add_option('-l', '--log_file', dest='log_file', help='Write debug output to log file')
    (options, args) = parser.parse_args()

    # get master logger
    logger = logging.getLogger('')
    logger.setLevel(options.log_level)

    # create console logger
    console_logger = logging.StreamHandler()
    console_logger.setFormatter(logging.Formatter('%(message)s'))
    logger.addHandler(console_logger)

    # create rotating file logger
    if options.log_file is not None:
        file_logger = logging.FileHandler(options.log_file, 'w')
        file_logger.setFormatter(logging.Formatter('%(message)s'))
        logger.addHandler(file_logger)

    # process files
    for arg in args:
        print arg
        jet_file = JetFile(arg, options)
        jet_file.WriteJetFileFromConfig(options)

