"""
 File:  
 midifile.py
 
 Contents and purpose:
 Utilities used throughout JetCreator
 
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

import logging
import struct
import copy
import array

# JET events
JET_EVENT_MARKER = 102
JET_MARKER_LOOP_END = 0
JET_EVENT_TRIGGER_CLIP = 103

# header definitions
SMF_HEADER_FMT = '>4slHHH'
SMF_RIFF_TAG = 'MThd'

SMF_TRACK_HEADER_FMT = '>4sl'
SMF_TRACK_RIFF_TAG = 'MTrk'

# defaults
DEFAULT_PPQN = 120
DEFAULT_BEATS_PER_MEASURE = 4
DEFAULT_TIME_FORMAT = '%03d:%02d:%03d'

# force note-offs to end of list
MAX_SEQ_NUM = 0x7fffffff

# MIDI messages
NOTE_OFF = 0x80
NOTE_ON = 0x90
POLY_KEY_PRESSURE = 0xa0
CONTROL_CHANGE = 0xb0
PROGRAM_CHANGE = 0xc0
CHANNEL_PRESSURE = 0xd0
PITCH_BEND = 0xe0

# System common messages
SYSEX = 0xf0
MIDI_TIME_CODE = 0xf1
SONG_POSITION_POINTER = 0xf2
SONG_SELECT = 0xf3
RESERVED_F4 = 0xf4
RESERVED_F5 = 0xf5
TUNE_REQUEST = 0xf6
END_SYSEX = 0xf7

# System real-time messages
TIMING_CLOCK = 0xf8
RESERVED_F9 = 0xf9
START = 0xfa
CONTINUE = 0xfb
STOP = 0xfc
RESERVED_FD = 0xfd
ACTIVE_SENSING = 0xfe
SYSTEM_RESET = 0xff

ONE_BYTE_MESSAGES = (
	TUNE_REQUEST,
	TIMING_CLOCK, 
	RESERVED_F9, 
	START, 
	CONTINUE, 
	STOP,
	RESERVED_FD,
	ACTIVE_SENSING,
	SYSTEM_RESET)

THREE_BYTE_MESSAGES = (
	NOTE_OFF, 
	NOTE_ON, 
	POLY_KEY_PRESSURE, 
	CONTROL_CHANGE, 
	PITCH_BEND)

MIDI_MESSAGES = (
	NOTE_OFF, 
	NOTE_ON, 
	POLY_KEY_PRESSURE, 
	CONTROL_CHANGE, 
	CHANNEL_PRESSURE, 
	PITCH_BEND, 
	SYSEX)

# Meta-events
META_EVENT = 0xff
META_EVENT_SEQUENCE_NUMBER = 0x00
META_EVENT_TEXT_EVENT = 0x01
META_EVENT_COPYRIGHT_NOTICE = 0x02
META_EVENT_SEQUENCE_TRACK_NAME = 0x03
META_EVENT_INSTRUMENT_NAME = 0x04
META_EVENT_LYRIC = 0x05
META_EVENT_MARKER = 0x06
META_EVENT_CUE_POINT = 0x07
META_EVENT_MIDI_CHANNEL_PREFIX = 0x20
META_EVENT_END_OF_TRACK = 0x2f
META_EVENT_SET_TEMPO = 0x51
META_EVENT_SMPTE_OFFSET = 0x54
META_EVENT_TIME_SIGNATURE = 0x58
META_EVENT_KEY_SIGNATURE = 0x59
META_EVENT_SEQUENCER_SPECIFIC = 0x7f

# recurring error messages
MSG_NOT_SMF_FILE = 'Not an SMF file - aborting parse!'
MSG_INVALID_TRACK_HEADER = 'Track header is invalid'
MSG_TYPE_MISMATCH = 'msg_type does not match event type'

LARGE_TICK_WARNING = 1000

# default control values
CTRL_BANK_SELECT_MSB = 0
CTRL_MOD_WHEEL = 1
CTRL_RPN_DATA_MSB = 6
CTRL_VOLUME = 7
CTRL_PAN = 10
CTRL_EXPRESSION = 11
CTRL_BANK_SELECT_LSB = 32
CTRL_RPN_DATA_LSB = 38
CTRL_SUSTAIN = 64
CTRL_RPN_LSB = 100
CTRL_RPN_MSB = 101
CTRL_RESET_CONTROLLERS = 121

RPN_PITCH_BEND_SENSITIVITY = 0
RPN_FINE_TUNING = 1
RPN_COARSE_TUNING = 2

MONITOR_CONTROLLERS = (
	CTRL_BANK_SELECT_MSB, 
	CTRL_MOD_WHEEL, 
	CTRL_RPN_DATA_MSB,
	CTRL_VOLUME, 
	CTRL_PAN, 
	CTRL_EXPRESSION, 
	CTRL_BANK_SELECT_LSB,
	CTRL_RPN_DATA_LSB,
	CTRL_SUSTAIN,
	CTRL_RPN_LSB,
	CTRL_RPN_MSB)

MONITOR_RPNS = (
	RPN_PITCH_BEND_SENSITIVITY,
	RPN_FINE_TUNING,
	RPN_COARSE_TUNING)

RPN_PITCH_BEND_SENSITIVITY = 0
RPN_FINE_TUNING = 1
RPN_COARSE_TUNING = 2

DEFAULT_CONTROLLER_VALUES = {
	CTRL_BANK_SELECT_MSB : 121,
	CTRL_MOD_WHEEL : 0,
	CTRL_RPN_DATA_MSB : 0,
	CTRL_VOLUME : 100,
	CTRL_PAN : 64,
	CTRL_EXPRESSION : 127,
	CTRL_RPN_DATA_LSB : 0,
	CTRL_BANK_SELECT_LSB : 0,
	CTRL_SUSTAIN : 0,
	CTRL_RPN_LSB : 0x7f,
	CTRL_RPN_MSB : 0x7f}

DEFAULT_RPN_VALUES = {
	RPN_PITCH_BEND_SENSITIVITY : 0x100,
	RPN_FINE_TUNING : 0,
	RPN_COARSE_TUNING : 1}

# initialize logger
midi_file_logger = logging.getLogger('MIDI_file')
midi_file_logger.setLevel(logging.NOTSET)


class trackGrid(object):
	def __init__ (self, track, channel, name, empty):
		self.track = track
		self.channel = channel
		self.name = name
		self.empty = empty
	def __str__ (self):
		return "['%s', '%s', '%s']" % (self.track, self.channel, self.name)
		

#---------------------------------------------------------------
# MIDIFileException
#---------------------------------------------------------------
class MIDIFileException (Exception):
	def __init__ (self, stream, msg):
		stream.error_loc = stream.tell()
		self.stream = stream
		self.msg = msg
	def __str__ (self):
		return '[%d]: %s' % (self.stream.error_loc, self.msg)

#---------------------------------------------------------------
# TimeBase
#---------------------------------------------------------------
class TimeBase (object):
	def __init__ (self, ppqn=DEFAULT_PPQN, beats_per_measure=DEFAULT_BEATS_PER_MEASURE):
		self.ppqn = ppqn
		self.beats_per_measure = beats_per_measure

	def ConvertToTicks (self, measures, beats, ticks):
		total_beats = beats + (measures * self.beats_per_measure)
		total_ticks = ticks + (total_beats * self.ppqn)
		return total_ticks

	def ConvertTicksToMBT (self, ticks):
		beats = ticks / self.ppqn
		ticks -= beats * self.ppqn
		measures = beats / self.beats_per_measure
		beats -= measures * self.beats_per_measure
		return (measures, beats, ticks)

	def ConvertTicksToStr (self, ticks, format=DEFAULT_TIME_FORMAT):
		measures, beats, ticks = self.ConvertTicksToMBT(ticks)
		return format % (measures, beats, ticks)

	def ConvertStrTimeToTuple(self, s):
		try:
			measures, beats, ticks = s.split(':',3)
			return (int(measures), int(beats), int(ticks))
		except:
			return (0,0,0)

	def ConvertStrTimeToTicks(self, s):
		measures, beats, ticks = self.ConvertStrTimeToTuple(s)
		return self.ConvertToTicks(measures, beats, ticks)
	
	def MbtDifference(self, mbt1, mbt2):
		t1 = self.ConvertToTicks(mbt1[0], mbt1[1], mbt1[2])
		t2 = self.ConvertToTicks(mbt2[0], mbt2[1], mbt2[2])
		return abs(t1-t2)
	
		
#---------------------------------------------------------------
# Helper functions
#---------------------------------------------------------------
def ReadByte (stream):
	try:
		return ord(stream.read(1))
	except TypeError:
		stream.error_loc = stream.tell()
		raise MIDIFileException(stream, 'Unexpected EOF')

def ReadBytes (stream, length):
	bytes = []
	for i in range(length):
		bytes.append(ReadByte(stream))
	return bytes

def ReadVarLenQty (stream):
	value = 0
	while 1:
		byte = ReadByte(stream)
		value = (value << 7) + (byte & 0x7f)
		if byte & 0x80 == 0:
			return value

def WriteByte (stream, value):
	stream.write(chr(value))
	
def WriteBytes (stream, bytes):
	for byte in bytes:
		WriteByte(stream, byte)			
	
def WriteVarLenQty (stream, value):
	bytes = [value & 0x7f]
	value = value >> 7
	while value > 0:
		bytes.append((value & 0x7f) | 0x80)
		value = value >> 7
	bytes.reverse()
	WriteBytes(stream, bytes)

#---------------------------------------------------------------
# EventFilter
#---------------------------------------------------------------
class EventFilter (object):
	pass

class EventTypeFilter (object):
	def __init__ (self, events, exclude=True):
		self.events = events
		self.exclude = exclude
	def Check (self, event):
		if event.msg_type in self.events:
			return not self.exclude
		return self.exclude
	
class NoteFilter (EventFilter):
	def __init__ (self, notes, exclude=True):
		self.notes = notes
		self.exclude = exclude
	def Check (self, event):
		if event.msg_type in (NOTE_ON, NOTE_OFF):
			if event.note in self.notes:
				return not self.exclude
		return self.exclude

class ChannelFilter (EventFilter):
	def __init__ (self, channel, exclude=True):
		self.channel = channel
		self.exclude = exclude
	def Check (self, event):
		if event.msg_type in (NOTE_ON, NOTE_OFF, POLY_KEY_PRESSURE, CONTROL_CHANGE, CHANNEL_PRESSURE, PITCH_BEND):
			if event.channel in self.channel:
				return not self.exclude
		return self.exclude

#---------------------------------------------------------------
# MIDIEvent
#---------------------------------------------------------------
class MIDIEvent (object):
	"""Factory for creating MIDI events from a stream."""
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		if msg_type == SYSEX:
			return SysExEvent.ReadFromStream(stream, seq, ticks, msg_type)
		elif msg_type == END_SYSEX:
			return SysExContEvent.ReadFromStream(stream, seq, ticks, msg_type)
		elif msg_type == META_EVENT:
			return MetaEvent.ReadFromStream(stream, seq, ticks, msg_type)
		else:
			high_nibble = msg_type & 0xf0
			if high_nibble == NOTE_OFF:
				return NoteOffEvent.ReadFromStream(stream, seq, ticks, msg_type)
			elif high_nibble == NOTE_ON:
				return NoteOnEvent.ReadFromStream(stream, seq, ticks, msg_type)
			elif high_nibble == POLY_KEY_PRESSURE:
				return PolyKeyPressureEvent.ReadFromStream(stream, seq, ticks, msg_type)
			elif high_nibble == CONTROL_CHANGE:
				return ControlChangeEvent.ReadFromStream(stream, seq, ticks, msg_type)
			elif high_nibble == PROGRAM_CHANGE:
				return ProgramChangeEvent.ReadFromStream(stream, seq, ticks, msg_type)
			elif high_nibble == CHANNEL_PRESSURE:
				return ChannelPressureEvent.ReadFromStream(stream, seq, ticks, msg_type)
			elif high_nibble == PITCH_BEND:
				return PitchBendEvent.ReadFromStream(stream, seq, ticks, msg_type)
			else:
				stream.Warning('Ignoring unexpected message type 0x%02x' % msg_type)
	def WriteTicks (self, stream, track):
		WriteVarLenQty(stream, self.ticks - track.ticks)
		track.ticks = self.ticks
	def WriteRunningStatus (self, stream, track, filters, msg, data1, data2=None):
		if not self.CheckFilters(filters):
			return
		self.WriteTicks(stream, track)
		status = msg + self.channel
		if track.running_status != status:
			WriteByte(stream, status)
			track.running_status = status
		WriteByte(stream, data1)
		if data2 is not None:
			WriteByte(stream, data2)
	def CheckFilters (self, filters):
		if filters is None or not len(filters):
			return True
			
		# never filter meta-events
		if (self.msg_type == META_EVENT) and (self.meta_type == META_EVENT_END_OF_TRACK):
			return True

		# check all filters 
		for f in filters:
			if not f.Check(self):
				return False
		return True

	def TimeEventStr (self, timebase):
		return '[%s]: %s' % (timebase.ConvertTicksToStr(self.ticks), self.__str__())

#---------------------------------------------------------------
# NoteOffEvent
#---------------------------------------------------------------
class NoteOffEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, note, velocity):
		self.name = 'NoteOff'
		self.msg_type = NOTE_OFF
		self.seq = seq
		self.ticks = ticks
		self.channel = channel
		self.note = note
		self.velocity = velocity
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		ticks = ticks
		channel = msg_type & 0x0f
		note = ReadByte(stream)
		velocity = ReadByte(stream)
		if msg_type & 0xf0 != NOTE_OFF:
			stream.seek(-2,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return NoteOffEvent(ticks, seq, channel, note, velocity)
	def WriteToStream (self, stream, track, filters=None):
		# special case for note-off using zero velocity
		if self.velocity > 0:
			self.WriteRunningStatus(stream, track, filters, NOTE_ON, self.note, self.velocity)
		if track.running_status == (NOTE_OFF + self.channel):
			self.WriteRunningStatus(stream, track, filters, NOTE_ON, self.note, self.velocity)
		else:
			self.WriteRunningStatus(stream, track, filters, NOTE_ON, self.note, 0)
	def __str__ (self):
		return '%s: ch=%d n=%d v=%d' % (self.name, self.channel, self.note, self.velocity)

#---------------------------------------------------------------
# NoteOnEvent
#---------------------------------------------------------------
class NoteOnEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, note, velocity, note_length, note_off_velocity):
		self.name = 'NoteOn'
		self.msg_type = NOTE_ON
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.note = note
		self.velocity = velocity
		self.note_length = note_length
		self.note_off_velocity = note_off_velocity
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		note = ReadByte(stream)
		velocity = ReadByte(stream)
		if msg_type & 0xf0 != NOTE_ON:
			stream.seek(-2,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		if velocity == 0:
			return NoteOffEvent(ticks, seq, channel, note, velocity)
		return NoteOnEvent(ticks, seq, channel, note, velocity, None, None)
	def WriteToStream (self, stream, track, filters=None):
		self.WriteRunningStatus(stream, track, filters, NOTE_ON, self.note, self.velocity)
	def __str__ (self):
		if self.note_length is not None:
			return '%s: ch=%d n=%d v=%d l=%d' % (self.name, self.channel, self.note, self.velocity, self.note_length)
		else:
			return '%s: ch=%d n=%d v=%d' % (self.name, self.channel, self.note, self.velocity)

#---------------------------------------------------------------
# PolyKeyPressureEvent
#---------------------------------------------------------------
class PolyKeyPressureEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, note, value):
		self.name = 'PolyKeyPressure'
		self.msg_type = POLY_KEY_PRESSURE
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.note = note
		self.value = value
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		note = ReadByte(stream)
		value = ReadByte(stream)
		if msg_type & 0xf0 != POLY_KEY_PRESSURE:
			stream.seek(-2,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return PolyKeyPressureEvent(ticks, seq, channel, note, value)
	def WriteToStream (self, stream, track, filters=None):
		self.WriteRunningStatus(stream, track, filters, POLY_KEY_PRESSURE, self.note, self.value)
	def __str__ (self):
		return '%s: ch=%d n=%d v=%d' % (self.name, self.channel, self.note, self.value)

#---------------------------------------------------------------
# ControlChangeEvent
#---------------------------------------------------------------
class ControlChangeEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, controller, value):
		self.name = 'ControlChange'
		self.msg_type = CONTROL_CHANGE
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.controller = controller
		self.value = value
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		controller = ReadByte(stream)
		value = ReadByte(stream)
		if msg_type & 0xf0 != CONTROL_CHANGE:
			stream.seek(-2,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		if controller >= 120:
			return ChannelModeEvent(ticks, seq, channel, controller, value)
		return ControlChangeEvent(ticks, seq, channel, controller, value)
	def WriteToStream (self, stream, track, filters=None):
		self.WriteRunningStatus(stream, track, filters, CONTROL_CHANGE, self.controller, self.value)
	def __str__ (self):
		return '%s: ch=%d c=%d v=%d' % (self.name, self.channel, self.controller, self.value)

#---------------------------------------------------------------
# ChannelModeEvent
#---------------------------------------------------------------
class ChannelModeEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, controller, value):
		self.name = 'ChannelMode'
		self.msg_type = CONTROL_CHANGE
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.controller = controller
		self.value = value
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		controller = ReadByte(stream)
		value = ReadByte(stream)
		if msg_type & 0xf0 != CONTROL_CHANGE:
			stream.seek(-2,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		if  controller < 120:
			return ControlChangeEvent(ticks, seq, channel, controller, value)
		return ChannelModeEvent(ticks, seq, channel, value)
	def WriteToStream (self, stream, track, filters=None):
		self.WriteRunningStatus(stream, track, filters, CONTROL_CHANGE, self.controller, self.value)
	def __str__ (self):
		return '%s: ch=%d c=%d v=%d' % (self.name, self.channel, self.controller, self.value)

#---------------------------------------------------------------
# ProgramChangeEvent
#---------------------------------------------------------------
class ProgramChangeEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, program):
		self.name = 'ProgramChange'
		self.msg_type = PROGRAM_CHANGE
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.program = program
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		program = ReadByte(stream)
		if msg_type & 0xf0 != PROGRAM_CHANGE:
			stream.seek(-1,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return ProgramChangeEvent(ticks, seq, channel, program)
	def WriteToStream (self, stream, track, filters=None):
		self.WriteRunningStatus(stream, track, filters, PROGRAM_CHANGE, self.program)
	def __str__ (self):
		return '%s: ch=%d p=%d' % (self.name, self.channel, self.program)

#---------------------------------------------------------------
# ChannelPressureEvent
#---------------------------------------------------------------
class ChannelPressureEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, value):
		self.name = 'ChannelPressure'
		self.msg_type = CHANNEL_PRESSURE
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.value = value
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		value = ReadByte(stream)
		if msg_type & 0xf0 != CHANNEL_PRESSURE:
			stream.seek(-1,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return ChannelPressureEvent(ticks, seq, channel, value)
	def WriteToStream (self, stream, track, filters=None):
		self.WriteRunningStatus(stream, track, filters, CHANNEL_PRESSURE, self.value)
	def __str__ (self):
		return '%s: ch=%d v=%d' % (self.name, self.channel, self.value)

#---------------------------------------------------------------
# PitchBendEvent
#---------------------------------------------------------------
class PitchBendEvent (MIDIEvent):
	def __init__ (self, ticks, seq, channel, value):
		self.name = 'PitchBend'
		self.msg_type = PITCH_BEND
		self.ticks = ticks
		self.seq = seq
		self.channel = channel
		self.value = value
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		channel = msg_type & 0x0f
		value = (ReadByte(stream) << 7) + ReadByte(stream) - 0x2000
		if msg_type & 0xf0 != PITCH_BEND:
			stream.seek(-2,1)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return PitchBendEvent(ticks, seq, channel, value)
	def WriteToStream (self, stream, track, filters=None):
		value = self.value + 0x2000
		if value < 0:
			value = 0
		if value > 0x3fff:
			value = 0x3fff
		self.WriteRunningStatus(stream, track, filters, PITCH_BEND, value >> 7, value & 0x7f)
	def __str__ (self):
		return '%s: ch=%d v=%d' % (self.name, self.channel, self.value)

#---------------------------------------------------------------
# SysExEvent
#---------------------------------------------------------------
class SysExEvent (MIDIEvent):
	def __init__ (self, ticks, seq, msg):
		self.name = 'SysEx'
		self.msg_type = SYSEX
		self.ticks = ticks
		self.seq = seq
		self.length = len(msg)
		self.msg = msg
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		pos = stream.tell()
		length = ReadVarLenQty(stream)
		msg = ReadBytes(stream, length)
		if msg_type != SYSEX:
			stream.seek(pos,0)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return SysExEvent(ticks, seq, msg)
	def WriteToStream (self, stream, track, filters=None):
		if not self.CheckFilters(filters):
			return
		self.WriteTicks(stream, track)
		WriteByte(stream, SYSEX)
		WriteVarLenQty(stream, self.length)
		WriteBytes(stream, self.msg)
		track.running_status = None
	def __str__ (self):
		fmt_str = '%s: f0' + ' %02x'*self.length
		return fmt_str % ((self.name,) + tuple(self.msg))

#---------------------------------------------------------------
# SysExContEvent
#---------------------------------------------------------------
class SysExContEvent (MIDIEvent):
	def __init__ (self, ticks, seq, msg):
		self.name = 'SysEx+'
		self.msg_type = END_SYSEX
		self.ticks = ticks
		self.seq = seq
		self.length = len(msg)
		self.msg = msg
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		pos = stream.tell()
		length = ReadVarLenQty(stream)
		msg = ReadBytes(stream, length)
		if msg_type != END_SYSEX:
			stream.seek(pos,0)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		return SysExContEvent(ticks, seq, msg)
	def WriteToStream (self, stream, track, filters=None):
		if not self.CheckFilters(filters):
			return
		self.WriteTicks(stream, track)
		WriteByte(stream, END_SYSEX)
		WriteVarLenQty(stream, self.length)
		WriteBytes(stream, self.msg)
		track.running_status = None
	def __str__ (self):
		fmt_str = '%s:' + ' %02x'*self.length
		return fmt_str % ((self.name,) + tuple(self.msg))

#---------------------------------------------------------------
# MetaEvent
#---------------------------------------------------------------
class MetaEvent (MIDIEvent):
	def __init__ (self, ticks, seq, meta_type, msg):
		self.name = 'MetaEvent'
		self.msg_type = META_EVENT
		self.ticks = ticks
		self.seq = seq
		self.meta_type = meta_type
		self.length = len(msg)
		self.msg = msg
	@staticmethod
	def ReadFromStream (stream, seq, ticks, msg_type):
		pos = stream.tell()
		meta_type = ReadByte(stream)
		length = ReadVarLenQty(stream)
		msg = ReadBytes(stream, length)
		if msg_type != META_EVENT:
			stream.seek(pos,0)
			raise MIDIFileException(stream, MSG_TYPE_MISMATCH)
		obj = MetaEvent(ticks, seq, meta_type, msg)
		return obj
	def WriteToStream (self, stream, track, filters=None):
		if not self.CheckFilters(filters):
			return
		self.WriteTicks(stream, track)
		WriteByte(stream, META_EVENT)
		WriteByte(stream, self.meta_type)
		WriteVarLenQty(stream, self.length)
		WriteBytes(stream, self.msg)
		track.running_status = None
	def __str__ (self):
		fmt_str = '%s: %02x' + ' %02x'*self.length
		return fmt_str % ((self.name, self.meta_type) + tuple(self.msg))

#---------------------------------------------------------------
# MIDIControllers
#---------------------------------------------------------------
class MIDIControllers (object):
	def __init__ (self):
		self.controllers = []
		self.rpns = []
		for channel in range(16):
			self.controllers.append({})
			self.controllers[channel] = copy.deepcopy(DEFAULT_CONTROLLER_VALUES)
			self.rpns.append({})
			self.rpns[channel] = copy.deepcopy(DEFAULT_RPN_VALUES)
		self.pitchbend = [0] * 16
		self.program = [-1] * 16
		self.pressure = [0] * 16

	def __str__ (self):
		output = []
		for channel in range(16):
			output.append('channel=%d' % channel)
			output.append('  program=%d' % self.program[channel])
			output.append('  pressure=%d' % self.pressure[channel])

			output.append('  controllers')
			for controller in self.controllers[channel].keys():
				output.append('    %03d: %03d' % (controller, self.controllers[channel][controller]))

			output.append('  rpns')
			for rpn in self.rpns[channel].keys():
				output.append('    %05d: %05d>' % (controller, self.rpns[channel][rpn]))
		return '\n'.join(output)


	def Event (self, event):
		"""Process an event and save any changes in controller values"""
		# process control changes
		if event.msg_type == CONTROL_CHANGE:
			self.ControlChange(event)
		elif event.msg_type == CHANNEL_PRESSURE:
			self.PressureChange(event)
		elif event.msg_type == PROGRAM_CHANGE:
			self.ProgramChange(event)
		elif event.msg_type == PITCH_BEND:
			self.PitchBendChange(event)

	def PitchBendChange (self, event):
		"""Monitor pitch bend change."""
		self.pitchbend[event.channel] = event.value

	def ProgramChange (self, event):
		"""Monitor program change."""
		self.program[event.channel] = event.program

	def ControlChange (self, event):
		"""Monitor control change."""
		controller = event.controller
		if controller in MONITOR_CONTROLLERS:
			channel = event.channel
			self.controllers[channel][controller] = event.value
			if (controller == CTRL_RPN_DATA_MSB) or (controller == CTRL_RPN_DATA_LSB):
				rpn = (self.controllers[channel][CTRL_RPN_MSB] << 7) + self.controllers[channel][CTRL_RPN_LSB]
				if rpn in MONITOR_RPNS:
					value = (self.controllers[channel][CTRL_RPN_DATA_MSB] << 7) + self.controllers[channel][CTRL_RPN_DATA_LSB]
					self.rpns[channel][rpn] = value

		# reset controllers
		elif event.controller == CTRL_RESET_CONTROLLERS:
			self.ResetControllers[event.channel]

	def PressureChange (self, event):
		"""Monitor pressure change."""
		self.pressure[event.channel] = event.value

	def ResetControllers (self, channel):
		"""Reset controllers to default."""
		self.controllers[channel] = DEFAULT_CONTROLLER_VALUES
		self.rpns[channel] = DEFAULT_RPN_VALUES
		self.pressure[channel] = 0

	def GenerateEventList (self, ticks, ref_values=None):
		"""Generate an event list based on controller differences."""
		events = EventList()

		# if no reference values, based on default values
		if ref_values is None:
			ref_values = MIDIControllers()

		# iterate through 16 MIDI channels
		for channel in range(16):

			# generate RPN changes
			for rpn in self.rpns[channel].keys():
				value = self.rpns[channel][rpn]
				if value != ref_values.rpns[channel][rpn]:
					events.append(ControlChangeEvent(ticks, -1, channel, CTRL_RPN_MSB, rpn >> 7))
					events.append(ControlChangeEvent(ticks, -1, channel, CTRL_RPN_LSB, rpn & 0x7f))
					events.append(ControlChangeEvent(ticks, -1, channel, CTRL_RPN_DATA_MSB, value >> 7))
					events.append(ControlChangeEvent(ticks, -1, channel, CTRL_RPN_DATA_LSB, value & 0x7f))

			# generate controller changes
			for controller in self.controllers[channel].keys():
				if self.controllers[channel][controller] != ref_values.controllers[channel][controller]:
					events.append(ControlChangeEvent(ticks, -1, channel, controller, self.controllers[channel][controller]))

			# generate pressure changes
			if self.pressure[channel] != ref_values.pressure[channel]:
				events.append(ChannelPressureEvent(ticks, -1, channel, self.pressure[channel]))

			# generate program changes
			if self.program[channel] != ref_values.program[channel]:
				if self.program[channel] in range(128):
					events.append(ProgramChangeEvent(ticks, -1, channel, self.program[channel]))

			# generate pitch bend changes
			if self.pitchbend[channel] != ref_values.pitchbend[channel]:
				if self.pitchbend[channel] in range(-8192,8191):
					events.append(PitchBendEvent(ticks, -1, channel, self.pitchbend[channel]))

		return events

#---------------------------------------------------------------
# EventList
#---------------------------------------------------------------
class EventList (list):
	def __init__ (self):
		list.__init__(self)

	def FixNoteLengths (self):
		midi_file_logger.debug('Fix note lengths')

		# search for note-on's in event list
		for index in range(len(self)):
			event = self[index]
			if event.msg_type == NOTE_ON:
				note_off_ticks = event.ticks + event.note_length

				# check for note-on occuring before end of current note
				for i in range(index + 1, len(self)):
					event_to_check = self[i]
					if event_to_check.ticks >= note_off_ticks:
						break

					# adjust note length
					if (event_to_check.msg_type == NOTE_ON) and (event_to_check.note == event.note):
						midi_file_logger.debug('Adjusting note length @ %d' % event.ticks)
						event.note_length = event_to_check.ticks - event.ticks
						break

	def ChaseControllers (self, end_seq, start_seq = 0, values = None):
		midi_file_logger.debug('ChaseControllers from %d to %d' % (start_seq, end_seq))

		# initialize controller values
		if values is None:
			values = MIDIControllers()

		# chase controllers in track
		for i in range(start_seq, min(end_seq, len(self))):
			values.Event(self[i])

		# return new values
		return values
		
	def SelectEvents (self, start, end):
		midi_file_logger.debug('SelectEvents: %d to %d' % (start, end))
		selected = EventList()
		for event in self:
			if event.ticks >= start:
				if event.ticks >= end:
					break
				midi_file_logger.debug('SelectEvent: %s' % event.__str__())
				selected.append(event)
		return selected

	def MergeEvents (self, events):
		# copy events and sort them by ticks/sequence#
		self.extend(events)
		self.SortEvents()
		
	def InsertEvents (self, events, seq):
		self[seq:seq] = events
		self.RenumberSeq()

	def DeleteEvents (self, start_index, end_index, move_meta_events=None):
		# default parameters
		if start_index is None:
			start_index = 0
		if end_index is None:
			end_index = len(self)

		#print("\n")
		#for evt in self[start_index:end_index]:
		#	print("%d %s" % (evt.ticks, evt))

		# delete events
		delete_count = 0
		move_count = 0
		for event in self[start_index:end_index]:
			#Bth; Added this so we always get clip end events; clips that ended on last measure wouldn't end on repeat
			if (event.msg_type == CONTROL_CHANGE) and \
			        (event.controller == JET_EVENT_TRIGGER_CLIP) and \
			        ((event.value & 0x40) != 0x40):
				pass
			else:
				if (move_meta_events is None) or (event.msg_type != META_EVENT):
					self.remove(event)
					delete_count += 1
					
				# move meta-events
				else:
					event.ticks = move_meta_events
					move_count += 1
				
		midi_file_logger.debug('DeleteEvents: deleted %d events in range(%s:%s)' % (delete_count, start_index, end_index))
		midi_file_logger.debug('DeleteEvents: moved %d events in range(%s:%s)' % (move_count, start_index, end_index))

			
	def SeekEvent (self, pos):
		for i in range(len(self)):
			if self[i].ticks >= pos:
				return i
		return None

	def RenumberSeq (self):
		seq = 0
		for event in self:
			event.seq = seq
			seq += 1

	def SortEvents (self):
		self.sort(self.EventSorter)
		self.RenumberSeq()

	@staticmethod
	def EventSorter (x, y):
		if x.ticks == y.ticks:
			return cmp(x.seq, y.seq)
		else:
			return cmp(x.ticks, y.ticks)

	def DumpEvents (self, output, timebase):
		if output is not None:
			for event in self:
				output.write('%s\n' % event.TimeEventStr(timebase))
		else:
			for event in self:
				midi_file_logger.debug(event.TimeEventStr(timebase))

#---------------------------------------------------------------
# MIDITrack
#---------------------------------------------------------------
class MIDITrack (object):
	"""The MIDITrack class implements methods for reading, parsing,
	modifying, and writing tracks in Standard MIDI Files (SMF).

	"""
	def __init__ (self):
		self.length = 0
		self.events = EventList()
		self.end_of_track = None
		self.channel = None
		self.name = None
	
	def ReadFromStream (self, stream, offset, file_size):
		self.stream = stream
		ticks = 0
		seq = 0
		running_status = None
		tick_warning_level = stream.timebase.ppqn * LARGE_TICK_WARNING
		
		# read the track header - verify it's an SMF track
		stream.seek(offset)
		bytes = stream.read(struct.calcsize(SMF_TRACK_HEADER_FMT))
		riff_tag, track_len = struct.unpack(SMF_TRACK_HEADER_FMT, bytes)
		midi_file_logger.debug('SMF track header\n  Tag:      %s\n  TrackLen: %d' % (riff_tag, track_len))
		if (riff_tag != SMF_TRACK_RIFF_TAG):
			raise MIDIFileException(stream, MSG_INVALID_TRACK_HEADER)
		self.start = stream.tell()
		
		# check for valid track length
		if (self.start + track_len) > file_size:
			stream.Warning('Ignoring illegal track length - %d exceeds length of file' % track_len)
			track_len = None
			
		# read the entire track
		note_on_list = []
		while 1:

			# save current position
			pos = stream.tell()

			# check for end of track
			if track_len is not None:
				if (pos - self.start) >= track_len:
					break

			# are we past end of track?
			if self.end_of_track:
				stream.Warning('Ignoring data encountered beyond end-of-track meta-event')
				break;
		
			# read delta timestamp
			delta = ReadVarLenQty(stream)
			if ticks > tick_warning_level:
				stream.Warning('Tick value is excessive - possibly corrupt data?')
			ticks += delta
				
			# get the event type and process it
			msg_type = ReadByte(stream)

			# if data byte, check for running status
			if msg_type & 0x80 == 0:

				# use running status
				msg_type = running_status

				# back up so event can process data
				stream.seek(-1,1)

				# if no running status, we have a problem
				if not running_status:
					stream.Warning('Ignoring data byte received with no running status')

			# create event type from stream
			event = MIDIEvent.ReadFromStream(stream, seq, ticks, msg_type)
			
			if self.channel == None:
				try:
					self.channel = event.channel
				except AttributeError:
					pass
					
			# track note-ons
			if event.msg_type == NOTE_ON:

				"""
				Experimental code to clean up overlapping notes
				Clean up now occurs during write process
				
				for note_on in note_on_list:
					if (event.channel == note_on.channel) and (event.note == note_on.note):
						stream.Warning('Duplicate note-on\'s encountered without intervening note-off')
						stream.Warning('  [%s]: %s' % (stream.timebase.ConvertTicksToStr(event.ticks), event.__str__()))
						note_on.note_length = event.ticks - note_on.ticks - 1
						if note_on.note_length <= 0:
							stream.Warning('Eliminating duplicate note-on')
							event.ticks = note_on.ticks
							self.events.remove(note_on)
				"""
				
				note_on_list.append(event)

			# process note-offs
			if event.msg_type == NOTE_OFF:
				for note_on in note_on_list[:]:
					if (event.channel == note_on.channel) and (event.note == note_on.note):
						note_on.note_length = event.ticks - note_on.ticks
						note_on.note_off_velocity = event.velocity
						note_on_list.remove(note_on)
						break
				#else:
				#	stream.Warning('Note-off encountered without corresponding note-on')
				#	stream.Warning('  [%s]: %s' % (stream.timebase.ConvertTicksToStr(event.ticks), event.__str__()))

			# check for end of track
			elif event.msg_type == META_EVENT and event.meta_type == META_EVENT_END_OF_TRACK:
				self.end_of_track = event.ticks

			# BTH; get track name
			elif event.msg_type == META_EVENT and event.meta_type == META_EVENT_SEQUENCE_TRACK_NAME:
				self.name = array.array('B', event.msg).tostring()
				
			# append event to event list
			else:
				self.events.append(event)
				seq += 1

			# save position for port-mortem
			stream.last_good_event = pos

			# update running statusc_str(
			if msg_type < 0xf0:
				running_status = msg_type
			elif (msg_type < 0xf8) or (msg_type == 0xff):
				running_status = None

		# check for stuck notes
		#if len(note_on_list):
		#	stream.Warning('Note-ons encountered without corresponding note-offs')

		# check for missing end-of-track meta-event
		if self.end_of_track is None:
			self.last_tick = self.events[-1].ticks
			stream.Warning('End of track encountered with no end-of-track meta-event')

		# if track length was bad, correct it
		if track_len is None:
			track_len = stream.tell() - offset - 8

		return track_len

	def Write (self, stream, filters=None):
		# save current file position so we can write header
		header_loc = stream.tell()
		stream.seek(header_loc + struct.calcsize(SMF_TRACK_HEADER_FMT))

		# save a copy of the event list so we can restore it
		save_events = copy.copy(self.events)

		# create note-off events
		index = 0
		while 1:
			if index >= len(self.events):
				break

			# if note-on event, create a note-off event
			event = self.events[index]
			index += 1
			if event.msg_type == NOTE_ON:
				note_off = NoteOffEvent(event.ticks + event.note_length, index, event.channel, event.note, event.note_off_velocity)

				# insert note-off in list
				for i in range(index, len(self.events)):
					if self.events[i].ticks >= note_off.ticks:
						self.events.insert(i, note_off)
						break
				else:
					self.events.append(note_off)

		# renumber list
		self.events.RenumberSeq()

		# write the events
		self.running_status = None
		self.ticks = 0
		for event in self.events:

			# write event
			event.WriteToStream(stream, self, filters)

		# restore original list (without note-off events)
		self.events = save_events

		# write the end-of-track meta-event
		MetaEvent(self.end_of_track, 0, META_EVENT_END_OF_TRACK,[]).WriteToStream(stream, self, None)

		# write track header
		end_of_track = stream.tell()
		track_len = end_of_track - header_loc - struct.calcsize(SMF_TRACK_HEADER_FMT)
		stream.seek(header_loc)
		bytes = struct.pack(SMF_TRACK_HEADER_FMT, SMF_TRACK_RIFF_TAG, track_len)
		stream.write(bytes)
		stream.seek(end_of_track)

	def Trim (self, start, end, slide=True, chase_controllers=True, delete_meta_events=False, quantize=0):
		controllers = None

		if quantize:
			# quantize events just before start
			for event in self.events.SelectEvents(start - quantize, start):
				midi_file_logger.debug('Trim: Moving event %s to %d' % (event.__str__(), start))
				event.ticks = start

			# quantize events just before end
			for event in self.events.SelectEvents(end - quantize, end):
				midi_file_logger.debug('Trim: Moving event %s to %d' % (event.__str__(), end))
				event.ticks = end

		# trim start
		if start:

			# find first event inside trim
			start_event = self.events.SeekEvent(start)
			if start_event is not None:

				# chase controllers to cut point
				if chase_controllers:
					controllers = self.events.ChaseControllers(self.events[start_event].seq)
					controller_events = controllers.GenerateEventList(0)
					midi_file_logger.debug('Trim: insert new controller events at %d:' % start)
					controller_events.DumpEvents(None, self.stream.timebase)
					self.events.InsertEvents(controller_events, start_event)

				# delete events					
				midi_file_logger.debug('Trim: deleting events up to event %d' % start_event)
				if delete_meta_events:
					self.events.DeleteEvents(None, start_event, None)
				else:
					self.events.DeleteEvents(None, start_event, start)

			# delete everything except metadata
			else:
				self.events.DeleteEvents(None, None, start)

		# trim end
		end_event = self.events.SeekEvent(end)
		if end_event is not None:
			midi_file_logger.debug('Trim: trimming section starting at event %d' % end_event)
			self.events.DeleteEvents(end_event, None)

		# trim any notes that extend past the end
		for event in self.events:
			if event.msg_type == NOTE_ON:
				if (event.ticks + event.note_length) > end:
					midi_file_logger.debug('Trim: trimming note that extends past end %s' % event.TimeEventStr(self.stream.timebase))
					event.note_length = end - event.ticks
					if event.note_length <= 0:
						raise 'Error in note length - note should have been deleted'

		midi_file_logger.debug('Trim: initial end-of-track: %d' % self.end_of_track)
		self.end_of_track = min(self.end_of_track, end)

		# slide events to start of track to fill hole
		if slide and start:
			midi_file_logger.debug('Trim: sliding events: %d' % start)
			for event in self.events:
				if event.ticks > start:
					event.ticks -= start
				else:
					event.ticks = 0
			self.end_of_track = max(0, self.end_of_track - start)
		midi_file_logger.debug('Trim: new end-of-track: %d' % self.end_of_track)

		self.events.RenumberSeq()
		self.events.FixNoteLengths()

	def DumpEvents (self, output):
		self.events.DumpEvents(output, self.stream.timebase)
		if output is not None:
			output.write('[%s]: end-of-track\n' % self.stream.timebase.ConvertTicksToStr(self.end_of_track))
		else:
			midi_file_logger.debug('[%s]: end-of-track' % self.stream.timebase.ConvertTicksToStr(self.end_of_track))


#---------------------------------------------------------------
# MIDIFile
#---------------------------------------------------------------
class MIDIFile (file):
	"""The MIDIFile class implements methods for reading, parsing,
	modifying, and writing Standard MIDI Files (SMF).

	"""
	def __init__ (self, name, mode):
		file.__init__(self, name, mode)
		self.timebase = TimeBase()

	def ReadFromStream (self, start_offset=0, file_size=None):
		"""Parse the MIDI file creating a list of properties, tracks,
		and events based on the contents of the file.

		"""

		# determine file size - without using os.stat
		if file_size == None:
			self.start_offset = start_offset
			self.seek(0,2)
			file_size = self.tell() - self.start_offset
			self.seek(start_offset,0)
		else:
			file_size = file_size

		# for error recovery
		self.last_good_event = None
		self.error_loc = None

		# read the file header - verify it's an SMF file
		bytes = self.read(struct.calcsize(SMF_HEADER_FMT))
		riff_tag, self.hdr_len, self.format, self.num_tracks, self.timebase.ppqn = struct.unpack(SMF_HEADER_FMT, bytes)
		midi_file_logger.debug('SMF header\n  Tag:       %s\n  HeaderLen: %d\n  Format:    %d\n  NumTracks: %d\n  PPQN:      %d\n' % \
			(riff_tag, self.hdr_len, self.format, self.num_tracks, self.timebase.ppqn))

		# sanity check on header
		if (riff_tag != SMF_RIFF_TAG) or (self.format not in range(2)):
			raise MIDIFileException(self, MSG_NOT_SMF_FILE)

		# check for odd header size
		if self.hdr_len + 8 != struct.calcsize(SMF_HEADER_FMT):
			self.Warning('SMF file has unusual header size: %d bytes' % self.hdr_len)

		# read each of the tracks
		offset = start_offset + self.hdr_len + 8
		self.tracks = []
		self.end_of_file = 0
		for i in range(self.num_tracks):
			#print("Track: %d" % i)

			# parse the track
			track = MIDITrack()
			length = track.ReadFromStream(self, offset, file_size)
			track.trackNum = i
			
			self.tracks.append(track)

			# calculate offset to next track
			offset += length + 8

			# determine time of last event
			self.end_of_file = max(self.end_of_file, track.end_of_track)

		# if start_offset is zero, the final offset should match the file length
		if (offset - start_offset) != file_size:
			self.Warning('SMF file size is incorrect - should be %d, was %d' % (file_size, offset))
		
	def Save (self, offset=0, filters=None):
		"""Save this file back to disk with modifications."""
		if (not 'w' in self.mode) and (not '+' in self.mode):
			raise MIDIFileException(self, 'Cannot write to file in read-only mode')
		self.Write(self, offset, filters)

	def SaveAs (self, filename, offset=0, filters=None):
		"""Save MIDI data to new file."""
		output_file = MIDIFile(filename, 'wb')
		self.Write(output_file, offset, filters)
		output_file.close()

	def Write (self, output_file, offset=0, filters=None):
		"""This function does the actual work of writing the file."""
		# write the file header
		output_file.seek(offset)
		bytes = struct.pack(SMF_HEADER_FMT, SMF_RIFF_TAG, struct.calcsize(SMF_HEADER_FMT) - 8, self.format, self.num_tracks, self.timebase.ppqn)
		output_file.write(bytes)

		# write out the tracks
		for track in self.tracks:
			track.Write(output_file, filters)

		# flush the data to disk
		output_file.flush()

	def ConvertToType0 (self):
		"""Convert a file to type 0."""
		if self.format == 0:
			midi_file_logger.warning('File is already type 0 - ignoring request to convert')
			return

		# convert to type 0
		for track in self.tracks[1:]:
			self.tracks[0].MergeEvents(track.events)
		self.tracks = self.tracks[:1]
		self.num_tracks = 1
		self.format = 0

	def DeleteEmptyTracks (self):
		"""Delete any tracks that do not contain MIDI messages"""
		track_num = 0
		for track in self.tracks[:]:
			for event in self.tracks.events:
				if event.msg_type in MIDI_MESSAGES:
					break;
				else:
					midi_file_logger.debug('Deleting track %d' % track_num)
					self.tracks.remove(track)
			track_num += 1

	def ConvertToTicks (self, measures, beats, ticks):
		return self.timebase.ConvertToTicks(measures, beats, ticks)

	def Trim (self, start, end, quantize=0, chase_controllers=True):
		track_num = 0
		for track in self.tracks:
			midi_file_logger.debug('Trimming track %d' % track_num)
			track.Trim(start, end, quantize=quantize, chase_controllers=chase_controllers)
			track_num += 1

	def DumpTracks (self, output=None):
		track_num = 0
		for track in self.tracks:
			if output is None:
				midi_file_logger.debug('*** Track %d ***' % track_num)
			else:
				output.write('*** Track %d ***' % track_num)
			track.DumpEvents(output)
			track_num += 1

	def Warning (self, msg):
		midi_file_logger.warning('[%d]: %s' % (self.tell(), msg))

	def Error (self, msg):
		midi_file_logger.error('[%d]: %s' % (self.tell(), msg))

	def DumpError (self):
		if self.last_good_event:
			midi_file_logger.error('Dumping from last good event:')
			pos = self.last_good_event - 16
			length = self.error_loc - pos + 16
		elif self.error_loc:
			midi_file_logger.error('Dumping from 16 bytes prior to error:')
			pos = self.error_loc
			length = 32
		else:
			midi_file_logger.error('No dump information available')
			return

		self.seek(pos, 0)
		for i in range(length):
			if i % 16 == 0:
				if i:
					midi_file_logger.error(' '.join(debug_out))
				debug_out = ['%08x:' % (pos + i)]
			byte = self.read(1)
			if len(byte) == 0:
				break;
			debug_out.append('%02x' % ord(byte))
		if i % 16 > 0:
			midi_file_logger.error(' '.join(debug_out))

def GetMidiInfo(midiFile):
	"""Bth; Get MIDI info"""
	
	class midiData(object):
		def __init__ (self):
			self.err = 1
			self.endMbt = "0:0:0"
			self.totalTicks = 0
			self.maxTracks = 0
			self.maxMeasures = 0
			self.maxBeats = 0
			self.maxTicks = 0
			self.totalTicks = 0
			self.timebase = None
			self.ppqn = 0
			self.beats_per_measure = 0
			self.trackList = []
			
	md = midiData()
	
	try:
		m = MIDIFile(midiFile, 'rb')
		m.ReadFromStream()
		
		for track in m.tracks:
			if track.channel is not None:
				empty = False 
				trk = track.channel + 1
			else:
				empty = True	
				trk = ''		
			md.trackList.append(trackGrid(track.trackNum, trk, track.name, empty))
				
		md.endMbt = m.timebase.ConvertTicksToMBT(m.end_of_file)
		md.endMbtStr = "%d:%d:%d" % (md.endMbt[0], md.endMbt[1], md.endMbt[2])
		md.maxMeasures = md.endMbt[0]
		md.maxBeats = 4
		md.maxTicks = m.timebase.ppqn
		md.maxTracks = m.num_tracks
		md.totalTicks = m.end_of_file
		md.timebase = m.timebase
		md.ppqn = m.timebase.ppqn
		md.beats_per_measure = m.timebase.beats_per_measure

		#add above if more added
		md.err = 0
		
		m.close()
	except:
		raise
		pass
	
	return md
	
		


#---------------------------------------------------------------
# main
#---------------------------------------------------------------
if __name__ == '__main__':
	sys = __import__('sys')
	os = __import__('os')

	# initialize root logger
	root_logger = logging.getLogger('')
	root_logger.setLevel(logging.NOTSET)

	# initialize console handler
	console_handler = logging.StreamHandler()
	console_handler.setFormatter(logging.Formatter('%(message)s'))
	console_handler.setLevel(logging.DEBUG)
	root_logger.addHandler(console_handler)

	files = []
	dirs = []
	last_arg = None
	sysex_filter = False
	drum_filter = False
	convert = False

	# process args
	for arg in sys.argv[1:]:

		# previous argument implies this argument
		if last_arg is not None:
			if last_arg == '-DIR':
				dirs.append(arg)
				last_arg = None

		# check for switch
		elif arg[0] == '-':
			if arg == '-DIR':
				last_arg = arg
			elif arg == '-SYSEX':
				sysex_filter = True
			elif arg == '-DRUMS':
				drum_filter = True
			elif arg == '-CONVERT':
				convert = True
			else:
				midi_file_logger.error('Bad option %s' % arg)

		# must be a filename
		else:
			files.append(arg)

	# setup filters
	filters = []
	if sysex_filter:
		filters.append(EventTypeFilter((SYSEX,)))
	if drum_filter:
		filters.append(ChannelFilter((9,),False))
		

	# process dirs
	for d in dirs:
		for root, dir_list, file_list in os.walk(d):
			for f in file_list:
				if f.endswith('.mid'):
					files.append(os.path.join(root, f))

	# process files
	bad_files = []
	for f in files:
		midi_file_logger.info('Processing file %s' % f)
		midiFile = MIDIFile(f, 'rb')
		try:
			midiFile.ReadFromStream()
			
			#midiFile.DumpTracks()
			#print('[%s]: end-of-track\n' % midiFile.timebase.ConvertTicksToStr(midiFile.end_of_file))

			# convert to type 0
			if convert and (midiFile.format == 1):
				midiFile.Convert(0)
				converted = True
			else:
				converted = False

			# write processed file
			if converted or len(filters):
				midiFile.SaveAs(f[:-4] + '-mod.mid', filters)
				
		except MIDIFileException, X:
			bad_files.append(f)
			midi_file_logger.error('Error in file %s' % f)
			midi_file_logger.error(X)
			midiFile.DumpError()
		midiFile.close()

	# dump problem files
	if len(bad_files):
		midi_file_logger.info('The following file(s) had errors:')
		for f in bad_files:
			midi_file_logger.info(f)
	else:
		midi_file_logger.info('All files read successfully')

