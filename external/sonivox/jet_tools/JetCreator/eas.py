
from __future__ import with_statement

import threading
import logging
import time
from ctypes import *
from JetUtils import OsWindows


# stream state 
EAS_STATE_READY = 0
EAS_STATE_PLAY = 1
EAS_STATE_STOPPING = 2
EAS_STATE_PAUSING = 3
EAS_STATE_STOPPED = 4
EAS_STATE_PAUSED = 5
EAS_STATE_OPEN = 6
EAS_STATE_ERROR = 7
EAS_STATE_EMPTY = 8

# EAS error codes
EAS_SUCCESS	= 0
EAS_FAILURE = -1
EAS_ERROR_INVALID_MODULE = -2
EAS_ERROR_MALLOC_FAILED = -3
EAS_ERROR_FILE_POS = -4
EAS_ERROR_INVALID_FILE_MODE = -5
EAS_ERROR_FILE_SEEK = -6
EAS_ERROR_FILE_LENGTH = -7
EAS_ERROR_NOT_IMPLEMENTED = -8
EAS_ERROR_CLOSE_FAILED = -9
EAS_ERROR_FILE_OPEN_FAILED = -10
EAS_ERROR_INVALID_HANDLE = -11
EAS_ERROR_NO_MIX_BUFFER = -12
EAS_ERROR_PARAMETER_RANGE = -13
EAS_ERROR_MAX_FILES_OPEN = -14
EAS_ERROR_UNRECOGNIZED_FORMAT = -15
EAS_BUFFER_SIZE_MISMATCH = -16
EAS_ERROR_FILE_FORMAT = -17
EAS_ERROR_SMF_NOT_INITIALIZED = -18
EAS_ERROR_LOCATE_BEYOND_END = -19
EAS_ERROR_INVALID_PCM_TYPE = -20
EAS_ERROR_MAX_PCM_STREAMS = -21
EAS_ERROR_NO_VOICE_ALLOCATED = -22
EAS_ERROR_INVALID_CHANNEL = -23
EAS_ERROR_ALREADY_STOPPED = -24
EAS_ERROR_FILE_READ_FAILED = -25
EAS_ERROR_HANDLE_INTEGRITY = -26
EAS_ERROR_MAX_STREAMS_OPEN = -27
EAS_ERROR_INVALID_PARAMETER = -28
EAS_ERROR_FEATURE_NOT_AVAILABLE = -29
EAS_ERROR_SOUND_LIBRARY = -30
EAS_ERROR_NOT_VALID_IN_THIS_STATE = -31
EAS_ERROR_NO_VIRTUAL_SYNTHESIZER = -32
EAS_ERROR_FILE_ALREADY_OPEN = -33
EAS_ERROR_FILE_ALREADY_CLOSED = -34
EAS_ERROR_INCOMPATIBLE_VERSION = -35
EAS_ERROR_QUEUE_IS_FULL = -36
EAS_ERROR_QUEUE_IS_EMPTY = -37
EAS_ERROR_FEATURE_ALREADY_ACTIVE = -38

# special result codes
EAS_EOF = 3
EAS_STREAM_BUFFERING = 4

# buffer full error returned from Render
EAS_BUFFER_FULL = 5

# file types
file_types = (
	'Unknown',
	'SMF Type 0 (.mid)',
	'SMF Type 1 (.mid)',
	'SMAF - Unknown type (.mmf)',
	'SMAF MA-2 (.mmf)',
	'SMAF MA-3 (.mmf)',
	'SMAF MA-5 (.mmf)',
	'CMX/QualComm  (.pmd)',
	'MFi (NTT/DoCoMo i-mode)',
	'OTA/Nokia (.ott)',
	'iMelody (.imy)',
	'RTX/RTTTL (.rtx)',
	'XMF Type 0 (.xmf)',
	'XMF Type 1 (.xmf)',
	'WAVE/PCM (.wav)',
	'WAVE/IMA-ADPCM (.wav)',
	'MMAPI Tone Control (.js)'
)

stream_states = (
	'Ready',
	'Play',
	'Stopping',
	'Stopped',
	'Pausing',
	'Paused',
	'Open',
	'Error',
	'Empty'
)

# iMode play modes
IMODE_PLAY_ALL = 0
IMODE_PLAY_PARTIAL = 1

# callback type for metadata
EAS_METADATA_CBFUNC = CFUNCTYPE(c_int, c_int, c_char_p, c_ulong)

# callbacks for external audio
EAS_EXT_PRG_CHG_FUNC = CFUNCTYPE(c_int, c_void_p, c_void_p)
EAS_EXT_EVENT_FUNC = CFUNCTYPE(c_int, c_void_p, c_void_p)

# callback for aux mixer decoder
EAS_DECODER_FUNC = CFUNCTYPE(c_void_p, c_void_p, c_int, c_int)

# DLL path
if OsWindows():
	EAS_DLL_PATH = "EASDLL.dll"
else:
	EAS_DLL_PATH = "libEASLIb.dylib"

eas_dll = None

# logger
eas_logger = None

#---------------------------------------------------------------
# InitEASModule
#---------------------------------------------------------------
def InitEASModule (dll_path=None):
	global eas_dll
	global eas_logger

	
	# initialize logger
	if eas_logger is None:
		eas_logger = logging.getLogger('EAS')

	# initialize path to DLL
	if dll_path is None:
		dll_path=EAS_DLL_PATH

	# intialize DLL
	if eas_dll is None:
		eas_dll = cdll.LoadLibrary(dll_path)

#---------------------------------------------------------------
# S_JET_CONFIG
#---------------------------------------------------------------
class S_JET_CONFIG (Structure):
	_fields_ = [('appLowNote', c_ubyte)]

#---------------------------------------------------------------
# S_EXT_AUDIO_PRG_CHG 
#---------------------------------------------------------------
class S_EXT_AUDIO_PRG_CHG (Structure):
	_fields_ = [('bank', c_ushort),
				('program', c_ubyte),
				('channel', c_ubyte)]

#---------------------------------------------------------------
# S_EXT_AUDIO_EVENT 
#---------------------------------------------------------------
class S_EXT_AUDIO_EVENT (Structure):
	_fields_ = [('channel', c_ubyte),
				('note', c_ubyte),
				('velocity', c_ubyte),
				('noteOn', c_ubyte)]

#---------------------------------------------------------------
# S_MIDI_CONTROLLERS 
#---------------------------------------------------------------
class S_MIDI_CONTROLLERS (Structure):
	_fields_ = [('modWheel', c_ubyte),
				('volume', c_ubyte),
				('pan', c_ubyte),
				('expression', c_ubyte),
				('channelPressure', c_ubyte)]

#---------------------------------------------------------------
# WAVEFORMAT 
#---------------------------------------------------------------
class WAVEFORMAT (Structure):
	_fields_ = [('wFormatTag', c_ushort),
				('nChannels', c_ushort),
				('nSamplesPerSec', c_ulong),
				('nAvgBytesPerSec', c_ulong),
				('nBlockAlign', c_ushort),
				('wBitsPerSample', c_ushort)]

#---------------------------------------------------------------
# EAS_Exception 
#---------------------------------------------------------------
class EAS_Exception (Exception):
	def __init__ (self, result_code, msg, function=None):
		self.msg = msg
		self.result_code = result_code
		self.function = function
	def __str__ (self):
		return self.msg

#---------------------------------------------------------------
# Log callback function
#---------------------------------------------------------------
# map EAS severity levels to the Python logging module
severity_mapping = (logging.CRITICAL, logging.CRITICAL, logging.ERROR, logging.WARNING, logging.INFO, logging.DEBUG)
LOG_FUNC_TYPE = CFUNCTYPE(c_int, c_int, c_char_p)
def Log (level, msg):
	eas_logger.log(severity_mapping[level], msg)
	return level
LogCallback = LOG_FUNC_TYPE(Log)

#---------------------------------------------------------------
# EAS_Stream
#---------------------------------------------------------------
class EAS_Stream (object):
	def __init__ (self, handle, eas):
		eas_logger.debug('EAS_Stream.__init__')
		self.handle = handle
		self.eas = eas

	def SetVolume (self, volume):
		"""Set the stream volume"""
		eas_logger.debug('Call EAS_SetVolume: volume=%d' % volume)
		with self.eas.lock:
			result = eas_dll.EAS_SetVolume(self.eas.handle, self.handle, volume)
			if result:
				raise EAS_Exception(result, 'EAS_SetVolume error %d on file %s' % (result, self.path), 'EAS_SetVolume')

	def GetVolume (self):
		"""Get the stream volume."""
		eas_logger.debug('Call EAS_GetVolume')
		with self.eas.lock:
			volume = eas_dll.EAS_GetVolume(self.eas.handle, self.handle)
			if volume < 0:
				raise EAS_Exception(volume, 'EAS_GetVolume error %d on file %s' % (volume, self.path), 'EAS_GetVolume')
		eas_logger.debug('EAS_GetVolume: volume=%d' % volume)
		return volume

	def SetPriority (self, priority):
		"""Set the stream priority"""
		eas_logger.debug('Call EAS_SetPriority: priority=%d' % priority)
		with self.eas.lock:
			result = eas_dll.EAS_SetPriority(self.eas.handle, self.handle, priority)
			if result:
				raise EAS_Exception(result, 'EAS_SetPriority error %d on file %s' % (result, self.path), 'EAS_SetPriority')

	def GetPriority (self):
		"""Get the stream priority."""
		eas_logger.debug('Call EAS_GetPriority')
		priority = c_int(0)
		with self.eas.lock:
			result = eas_dll.EAS_GetPriority(self.eas.handle, self.handle, byref(priority))
			if result:
				raise EAS_Exception(result, 'EAS_GetPriority error %d on file %s' % (result, self.path), 'EAS_GetPriority')
		eas_logger.debug('EAS_GetPriority: priority=%d' % priority.value)
		return priority.value

	def SetTransposition (self, transposition):
		"""Set the transposition of a stream."""
		eas_logger.debug('Call EAS_SetTransposition: transposition=%d' % transposition)
		with self.eas.lock:
			result = eas_dll.EAS_SetTransposition(self.eas.handle, self.handle, transposition)
			if result:
				raise EAS_Exception(result, 'EAS_SetTransposition error %d on file %s' % (result, self.path), 'EAS_SetTransposition')

	def SetPolyphony (self, polyphony):
		"""Set the polyphony of a stream."""
		eas_logger.debug('Call EAS_SetPolyphony: polyphony=%d' % polyphony)
		with self.eas.lock:
			result = eas_dll.EAS_SetPolyphony(self.eas.handle, self.handle, polyphony)
			if result:
				raise EAS_Exception(result, 'EAS_SetPolyphony error %d on file %s' % (result, self.path), 'EAS_SetPolyphony')

	def GetPolyphony (self):
		"""Get the polyphony of a stream."""
		eas_logger.debug('Call EAS_GetPolyphony')
		polyphony = c_int(0)
		with self.eas.lock:
			result = eas_dll.EAS_GetPolyphony(self.eas.handle, self.handle, byref(polyphony))
			if result:
				raise EAS_Exception(result, 'EAS_GetPolyphony error %d on file %s' % (result, self.path), 'EAS_GetPolyphony')
		eas_logger.debug('EAS_SetPolyphony: polyphony=%d' % polyphony.value)
		return polyphony.value

	def SelectLib (self, test_lib=False):
		eas_logger.debug('Call EAS_SelectLib: test_lib=%s' % test_lib)
		with self.eas.lock:
			result = eas_dll.EAS_SelectLib(self.eas.handle, self.handle, test_lib)
			if result:
				raise EAS_Exception(result, 'EAS_SelectLib error %d on file %s' % (result, self.path), 'EAS_SelectLib')

	def LoadDLSCollection (self, path):
		eas_logger.debug('Call EAS_LoadDLSCollection: lib_path=%d' % path)
		with self.eas.lock:
			result = eas_dll.EAS_LoadDLSCollection(self.eas.handle, self.handle, path)
			if result:
				raise EAS_Exception(result, 'EAS_LoadDLSCollection error %d on file %s lib %s' % (result, self.path, path), 'EAS_LoadDLSCollection')

	def RegExtAudioCallback (self, user_data, prog_chg_func, event_func):
		"""Register an external audio callback."""
		eas_logger.debug('Call EAS_RegExtAudioCallback')
		if prog_chg_func is not None:
			prog_chg_func = EAS_EXT_PRG_CHG_FUNC(prog_chg_func)
		else:
			prog_chg_func = 0
		if event_func is not None:
			event_func = EAS_EXT_EVENT_FUNC(event_func)
		else:
			event_func = 0
		with self.eas.lock:
			result = eas_dll.EAS_RegExtAudioCallback(self.eas.handle, self.handle, user_data, prog_chg_func, event_func)
			if result:
				raise EAS_Exception(result, 'EAS_RegExtAudioCallback error %d on file %s' % (result, self.path), 'EAS_RegExtAudioCallback')

	def SetPlayMode (self, play_mode):
		"""Set play mode on a stream."""
		eas_logger.debug('Call EAS_SetPlayMode: play_mode=%d' % play_mode)
		with self.eas.lock:
			result = eas_dll.EAS_SetPlayMode(self.eas.handle, self.handle, play_mode)
			if result:
				raise EAS_Exception(result, 'EAS_SetPlayMode error %d on file %s' % (result, self.path), 'EAS_SetPlayMode')

"""
EAS_PUBLIC EAS_RESULT EAS_GetMIDIControllers (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_U8 channel, S_MIDI_CONTROLLERS *pControl);
"""

#---------------------------------------------------------------
# EAS_File
#---------------------------------------------------------------
class EAS_File (EAS_Stream):
	def __init__ (self, path, handle, eas):
		EAS_Stream.__init__(self, handle, eas)
		eas_logger.debug('EAS_File.__init__')
		self.path = path
		self.prepared = False

	def Prepare (self):
		"""Prepare an audio file for playback"""
		if self.prepared:
			eas_logger.warning('Prepare already called on file %s' % self.path)
		else:
			with self.eas.lock:
				eas_logger.debug('Call EAS_Prepare for file: %s' % self.path)
				result = eas_dll.EAS_Prepare(self.eas.handle, self.handle)
				if result:
					raise EAS_Exception(result, 'EAS_Prepare error %d on file %s' % (result, self.path), 'EAS_Prepare')
				self.prepared = True

	def State (self):
		"""Get stream state."""
		with self.eas.lock:
			eas_logger.debug('Call EAS_State for file: %s' % self.path)
			state = c_long(-1)
			result = eas_dll.EAS_State(self.eas.handle, self.handle, byref(state))
			if result:
				raise EAS_Exception(result, 'EAS_State error %d on file %s' % (result, self.path), 'EAS_State')
			eas_logger.debug('EAS_State: file=%s, state=%s' % (self.path, stream_states[state.value]))
			return state.value

	def Close (self):
		"""Close audio file."""
		if hasattr(self, 'handle'):
			with self.eas.lock:
				eas_logger.debug('Call EAS_CloseFile for file: %s' % self.path)
				result = eas_dll.EAS_CloseFile(self.eas.handle, self.handle)
				if result:
					raise EAS_Exception(result, 'EAS_CloseFile error %d on file %s' % (result, self.path), 'EAS_CloseFile')

				# remove file from the EAS object
				self.eas.audio_streams.remove(self)
				
			# clean up references
			del self.handle
			del self.eas
			del self.path

	def Pause (self):
		"""Pause a stream."""
		eas_logger.debug('Call EAS_Pause')
		with self.eas.lock:
			result = eas_dll.EAS_Pause(self.eas.handle, self.handle)
			if result:
				raise EAS_Exception(result, 'EAS_Pause error %d on file %s' % (result, self.path), 'EAS_Pause')

	def Resume (self):
		"""Resume a stream."""
		eas_logger.debug('Call EAS_Resume')
		with self.eas.lock:
			result = eas_dll.EAS_Resume(self.eas.handle, self.handle)
			if result:
				raise EAS_Exception(result, 'EAS_Resume error %d on file %s' % (result, self.path), 'EAS_Resume')

	def Locate (self, secs, offset=False):
		"""Set the playback position of a stream in seconds."""
		eas_logger.debug('Call EAS_Locate: location=%.3f, relative=%s' % (secs, offset))
		with self.eas.lock:
			result = eas_dll.EAS_Locate(self.eas.handle, self.handle, int(secs * 1000 + 0.5), offset)
			if result:
				raise EAS_Exception(result, 'EAS_Locate error %d on file %s' % (result, self.path), 'EAS_Locate')

	def GetLocation (self):
		"""Get the stream location in seconds."""
		eas_logger.debug('Call EAS_GetLocation')
		msecs = c_int(0)
		with self.eas.lock:
			result = eas_dll.EAS_GetLocation(self.eas.handle, self.handle, byref(msecs))
			if result:
				raise EAS_Exception(result, 'EAS_GetLocation error %d on file %s' % (result, self.path), 'EAS_GetLocation')
		msecs = float(msecs.value) / 1000 
		eas_logger.debug('EAS_GetLocation: location=%.3f' % msecs)
		return msecs

	def GetFileType (self):
		"""Get the file type."""
		eas_logger.debug('Call EAS_GetFileType')
		file_type = c_int(0)
		with self.eas.lock:
			result = eas_dll.EAS_GetFileType(self.eas.handle, self.handle, byref(file_type))
			if result:
				raise EAS_Exception(result, 'EAS_GetFileType error %d on file %s' % (result, self.path), 'EAS_GetFileType')
		file_type = file_type.value
		if file_type < len(file_types):
			file_desc = file_types[file_type]
		else:
			file_desc = 'Unrecognized type %d' % file_type
		eas_logger.debug('EAS_GetFileType: type=%d, desc=%s' % (file_type, file_desc))
		return (file_type, file_desc)

	def SetRepeat (self, count):
		"""Set the repeat count of a stream."""
		eas_logger.debug('Call EAS_SetRepeat: count=%d' % count)
		with self.eas.lock:
			result = eas_dll.EAS_SetRepeat(self.eas.handle, self.handle, count)
			if result:
				raise EAS_Exception(result, 'EAS_SetRepeat error %d on file %s' % (result, self.path), 'EAS_SetRepeat')

	def GetRepeat (self):
		"""Get the repeat count of a stream."""
		eas_logger.debug('Call EAS_GetRepeat')
		count = c_int(0)
		with self.eas.lock:
			result = eas_dll.EAS_GetRepeat(self.eas.handle, self.handle, byref(count))
			if result:
				raise EAS_Exception(result, 'EAS_GetRepeat error %d on file %s' % (result, self.path), 'EAS_GetRepeat')
		eas_logger.debug('EAS_GetRepeat: count=%d' % count.value)
		return count.value

	def SetPlaybackRate (self, rate):
		"""Set the playback rate of a stream."""
		eas_logger.debug('Call EAS_SetPlaybackRate')
		with self.eas.lock:
			result = eas_dll.EAS_SetPlaybackRate(self.eas.handle, self.handle, rate)
			if result:
				raise EAS_Exception(result, 'EAS_SetPlaybackRate error %d on file %s' % (result, self.path), 'EAS_SetPlaybackRate')

	def ParseMetaData (self):
		"""Parse the metadata in a file."""
		eas_logger.debug('Call EAS_ParseMetaData')
		length = c_int(0)
		with self.eas.lock:
			result = eas_dll.EAS_ParseMetaData(self.eas.handle, self.handle, byref(length))
			if result:
				raise EAS_Exception(result, 'EAS_ParseMetaData error %d on file %s' % (result, self.path), 'EAS_ParseMetaData')
		return float(length.value) / 1000.0

	def RegisterMetaDataCallback (self, func, buf, buf_size, user_data):
		"""Register a metadata callback."""
		eas_logger.debug('Call EAS_RegisterMetaDataCallback')
		with self.eas.lock:
			if func is not None:
				callback = EAS_METADATA_CBFUNC(func)
			else:
				callback = 0
			result = eas_dll.EAS_RegisterMetaDataCallback(self.eas.handle, self.handle, callback, buf, buf_size, user_data)
			if result:
				raise EAS_Exception(result, 'EAS_RegisterMetaDataCallback error %d on file %s' % (result, self.path), 'EAS_RegisterMetaDataCallback')

	def GetWaveFmtChunk (self):
		"""Get the file type."""
		eas_logger.debug('Call EAS_GetWaveFmtChunk')
		wave_fmt_chunk = c_void_p(0)
		with self.eas.lock:
			result = eas_dll.EAS_GetWaveFmtChunk(self.eas.handle, self.handle, byref(wave_fmt_chunk))
			if result:
				raise EAS_Exception(result, 'EAS_GetWaveFmtChunk error %d on file %s' % (result, self.path), 'EAS_GetWaveFmtChunk')
		return cast(wave_fmt_chunk, POINTER(WAVEFORMAT)).contents

	def Play (self, max_time=None):
		"""Plays the file to the end or max_time."""
		eas_logger.debug('EAS_File.Play')
		if not self.prepared:
			self.Prepare()
		if max_time is not None:
			max_time += self.eas.GetRenderTime()
		while self.State() not in (EAS_STATE_STOPPED, EAS_STATE_ERROR, EAS_STATE_EMPTY):
			self.eas.Render()
			if max_time is not None:
				if self.eas.GetRenderTime() >= max_time:
					eas_logger.info('Max render time exceeded - stopping playback')
					self.Pause()
					self.eas.Render()
					break

#---------------------------------------------------------------
# EAS_MIDIStream
#---------------------------------------------------------------
class EAS_MIDIStream (EAS_Stream):
	def Write(self, data):
		"""Write data to MIDI stream."""
		with self.eas.lock:
			result = eas_dll.EAS_WriteMIDIStream(self.eas.handle, self.handle, data, len(data))
			if result:
				raise EAS_Exception(result, 'EAS_WriteMIDIStream error %d' % result, 'EAS_WriteMIDIStream')

	def Close (self):
		"""Close MIDI stream."""
		if hasattr(self, 'handle'):
			with self.eas.lock:
				eas_logger.debug('Call EAS_CloseMIDIStream')
				result = eas_dll.EAS_CloseMIDIStream(self.eas.handle, self.handle)
				if result:
					raise EAS_Exception(result, 'EAS_CloseFile error %d' % result, 'EAS_CloseMIDIStream')

				# remove file from the EAS object
				self.eas.audio_streams.remove(self)
				
			# clean up references
			del self.handle
			del self.eas

#---------------------------------------------------------------
# EAS_Config 
#---------------------------------------------------------------
class EAS_Config (Structure):
	_fields_ = [('libVersion', c_ulong),
				('checkedVersion', c_int),
				('maxVoices', c_long),
				('numChannels', c_long),
				('sampleRate', c_long),
				('mixBufferSize', c_long),
				('filterEnabled', c_int),
				('buildTimeStamp', c_ulong),
				('buildGUID', c_char_p)]

#---------------------------------------------------------------
# EAS
#---------------------------------------------------------------
class EAS (object):
	def __init__ (self, handle=None, dll_path=None, log_file=None):
		if eas_dll is None:
			InitEASModule(dll_path)
		if log_file is not None:
			eas_logger.addHandler(log_file)
		eas_logger.debug('EAS.__init__')
		self.Init(handle)

	def __del__ (self):
		eas_logger.debug('EAS.__del__')
		self.Shutdown()

	def Init (self, handle=None):
		"""Initializes the EAS Library."""
		eas_logger.debug('EAS.Init')

		# if we are already initialized, shutdown first
		if hasattr(self, 'handle'):
			eas_logger.debug('EAS.Init called with library already initalized')
			self.ShutDown()

		# setup the logging function
		eas_dll.SetLogCallback(LogCallback)

		# create some members
		self.handle = c_void_p(0)
		self.audio_streams = []
		self.output_streams = []
		self.aux_mixer = None
		
		# create a sync lock
		self.lock = threading.RLock()
		with self.lock:
			# set log callback
		
			# get library configuration
			self.Config()

			# initialize library
			if handle is None:
				self.do_shutdown = True
				eas_logger.debug('Call EAS_Init')
				result = eas_dll.EAS_Init(byref(self.handle))
				if result:
					raise EAS_Exception(result, 'EAS_Init error %d' % result, 'EAS_Init')
			else:
				self.do_shutdown = False
				self.handle = handle

			# allocate audio buffer for rendering
			AudioBufferType = c_ubyte * (2 * self.config.mixBufferSize * self.config.numChannels)
			self.audio_buffer = AudioBufferType()
			self.buf_size = self.config.mixBufferSize

	def Config (self):
		"""Retrieves the EAS library configuration"""
		if not hasattr(self, 'config'):
			eas_logger.debug('Call EAS_Config')
			eas_dll.EAS_Config.restype = POINTER(EAS_Config)
			self.config = eas_dll.EAS_Config()[0]
		eas_logger.debug("libVersion=%08x, maxVoices=%d, numChannels=%d, sampleRate = %d, mixBufferSize=%d" %
			(self.config.libVersion, self.config.maxVoices, self.config.numChannels, self.config.sampleRate, self.config.mixBufferSize))

	def Shutdown (self):
		"""Shuts down the EAS library"""
		eas_logger.debug('EAS.Shutdown')
		if hasattr(self, 'handle'):
			with self.lock:
				# close audio streams
				audio_streams = self.audio_streams
				for f in audio_streams:
					eas_logger.warning('Stream was not closed before EAS_Shutdown')
					f.Close()

				# close output streams
				output_streams = self.output_streams
				for s in output_streams:
					s.close()

				# shutdown library
				if self.do_shutdown:
					eas_logger.debug('Call EAS_Shutdown')
					result = eas_dll.EAS_Shutdown(self.handle)
					if result:
						raise EAS_Exception(result, 'EAS_Shutdown error %d' % result, 'EAS_Shutdown')
				del self.handle

	def OpenFile (self, path):
		"""Opens an audio file to be played by the EAS library and
		returns an EAS_File object

		Arguments:
			path - path to audio file

		Returns:
			EAS_File
			
		"""
		with self.lock:
			eas_logger.debug('Call EAS_OpenFile for file: %s' % path)
			stream_handle = c_void_p(0)
			result = eas_dll.EAS_OpenFile(self.handle, path, byref(stream_handle))
			if result:
				raise EAS_Exception(result, 'EAS_OpenFile error %d on file %s' % (result, path), 'EAS_OpenFile')

			# create file object and save in list
			stream = EAS_File(path, stream_handle, self)
			self.audio_streams.append(stream)
			return stream

	def OpenMIDIStream (self, stream=None):
		"""Opens a MIDI stream.

		Arguments:
			stream - open stream object. If None, a new synth
			is created.

		Returns:
			EAS_MIDIStream
			
		"""
		with self.lock:
			eas_logger.debug('Call EAS_OpenMIDIStream')
			stream_handle = c_void_p(0)
			if stream.handle is not None:
				result = eas_dll.EAS_OpenMIDIStream(self.handle, byref(stream_handle), stream.handle)
			else:
				result = eas_dll.EAS_OpenMIDIStream(self.handle, byref(stream_handle), 0)
			if result:
				raise EAS_Exception(result, 'EAS_OpenMIDIStream error %d' % result, 'EAS_OpenMIDIStream')

			# create stream object and save in list
			stream = EAS_MIDIStream(stream_handle, self)
			self.audio_streams.append(stream)
			return stream

	def OpenToneControlStream (self, path):
		"""Opens an MMAPI tone control file to be played by the EAS
		library and returns an EAS_File object

		Arguments:
			path - path to audio file

		Returns:
			EAS_File
			
		"""
		with self.lock:
			eas_logger.debug('Call EAS_MMAPIToneControl for file: %s' % path)
			stream_handle = c_void_p(0)
			result = eas_dll.EAS_MMAPIToneControl(self.handle, path, byref(stream_handle))
			if result:
				raise EAS_Exception(result, 'EAS_MMAPIToneControl error %d on file %s' % (result, path), 'EAS_OpenToneControlStream')

			# create file object and save in list
			stream = EAS_File(path, stream_handle, self)
			self.audio_streams.append(stream)
			return stream

	def Attach (self, stream):
		"""Attach a file or output device to the EAS output.

		The stream object must support the following methods as
		defined in the Python wave module:
			close()
			setparams()
			writeframesraw()

		Arguments:
			stream - open wave object

		"""
		self.output_streams.append(stream)
		stream.setparams((self.config.numChannels, 2, self.config.sampleRate, 0, 'NONE', None))

	def Detach (self, stream):
		"""Detach a file or output device from the EAS output. See
		EAS.Attach for more details. It is the responsibility of
		the caller to close the wave file or stream.

		Arguments:
			stream - open and attached wave object
		"""
		self.output_streams.remove(stream)

	def StartWave (self, dev_num=0, sampleRate=None, maxBufSize=None):
		"""Route the audio output to the indicated wave device. Note
		that this can cause EASDLL.EAS_RenderWaveOut to return an
		error code if all the output buffers are full. In this case,
		the render thread should sleep a bit and try again.
		Unfortunately, due to the nature of the MMSYSTEM interface,
		there is no simple way to suspend the render thread.

		"""
		if sampleRate == None:
			sampleRate = self.config.sampleRate
		if maxBufSize == None:
			maxBufSize = self.config.mixBufferSize
		with self.lock:
			result = eas_dll.OpenWaveOutDevice(dev_num, sampleRate, maxBufSize)
			if result:
				raise EAS_Exception(result, 'OpenWaveOutDevice error %d' % result, 'OpenWaveOutDevice')

	def StopWave (self):
		"""Stop routing audio output to the audio device."""
		with self.lock:
			result = eas_dll.CloseWaveOutDevice()
			if result:
				raise EAS_Exception(result, 'CloseWaveOutDevice error %d' % result, 'CloseWaveOutDevice')

	def Render (self, count=None, secs=None):
		"""Calls EAS_Render to render audio.

		Arguments
			count - number of buffers to render
			secs - number of seconds to render

		If both count and secs are None, render a single buffer. 

		"""

		# determine number of buffers to render
		if count is None:
			if secs is not None:
				count = int(secs * float(self.config.sampleRate) / float(self.buf_size) + 0.5)
			else:
				count = 1

		# render buffers
		eas_logger.debug('rendering %d buffers' % count)
		samplesRendered = c_long(0)
		with self.lock:
			for c in range(count):
				# render a buffer of audio
				eas_logger.debug('rendering buffer')
				while 1:
					if self.aux_mixer is None:
						result = eas_dll.EAS_RenderWaveOut(self.handle, byref(self.audio_buffer), self.buf_size, byref(samplesRendered))
					else:
						result = eas_dll.EAS_RenderAuxMixer(self.handle, byref(self.audio_buffer), byref(samplesRendered))
					
					if result == 0:
						break;
					if result == EAS_BUFFER_FULL:
						time.sleep(0.01)
					else:
						raise EAS_Exception(result, 'EAS_Render error %d' % result, 'EAS_Render')

				# output to attached streams
				for s in self.output_streams:
					s.writeframesraw(self.audio_buffer)
				
	def GetRenderTime (self):
		"""Get the render time in seconds."""
		eas_logger.debug('Call EAS_GetRenderTime')
		msecs = c_int(0)
		with self.lock:
			result = eas_dll.EAS_GetRenderTime(self.handle, byref(msecs))
			if result:
				raise EAS_Exception(result, 'EAS_GetRenderTime error %d' % result, 'EAS_GetRenderTime')
		msecs = float(msecs.value) / 1000
		eas_logger.debug('EAS_GetRenderTime: time=%.3f' % msecs)
		return msecs

	def SetVolume (self, volume):
		"""Set the master volume"""
		eas_logger.debug('Call EAS_SetVolume: volume=%d' % volume)
		with self.lock:
			result = eas_dll.EAS_SetVolume(self.handle, 0, volume)
			if result:
					raise EAS_Exception(result, 'EAS_SetVolume error %d' % result, 'EAS_SetVolume')

	def GetVolume (self):
		"""Get the stream volume."""
		eas_logger.debug('Call EAS_GetVolume')
		volume = c_int(0)
		with self.lock:
			result = eas_dll.EAS_GetVolume(self.handle, 0, byref(volume))
			if result:
				raise EAS_Exception(result, 'EAS_GetVolume error %d' % result, 'EAS_GetVolume')
		eas_logger.debug('EAS_GetVolume: volume=%d' % volume.value)
		return volume.value

	def SetPolyphony (self, polyphony, synth_num=0):
		"""Set the polyphony of a synth."""
		eas_logger.debug('Call EAS_SetSynthPolyphony: synth_num=%d, polyphony=%d' % (synth_num, polyphony))
		with self.lock:
			result = eas_dll.EAS_SetSynthPolyphony(self.handle, synth_num, polyphony)
			if result:
				raise EAS_Exception(result, 'EAS_SetSynthPolyphony error %d on synth %d' % (result, synth_num), 'EAS_SetPolyphony')

	def GetPolyphony (self, synth_num=0):
		"""Get the polyphony of a synth."""
		eas_logger.debug('Call EAS_GetSynthPolyphony: synth_num=%d' % synth_num)
		polyphony = c_int(0)
		with self.lock:
			result = eas_dll.EAS_GetSynthPolyphony(self.handle, synth_num, byref(polyphony))
			if result:
				raise EAS_Exception(result, 'EAS_GetSynthPolyphony error %d on synth %d' % (result, synth_num), 'EAS_GetPolyphony')
		eas_logger.debug('Call EAS_GetSynthPolyphony: synth_num=%d, polyphony=%d' % (synth_num, polyphony.value))
		return polyphony.value

	def SetMaxLoad (self, max_load):
		"""Set the maximum parser load."""
		eas_logger.debug('Call EAS_SetMaxLoad: max_load=%d' % max_load)
		with self.lock:
			result = eas_dll.EAS_SetMaxLoad(self.handle, max_load)
			if result:
				raise EAS_Exception(result, 'EAS_SetMaxLoad error %d' % result, 'EAS_SetMaxLoad')

	def SetParameter (self, module, param, value):
		"""Set a module parameter."""
		eas_logger.debug('Call EAS_SetParameter: module=%d, param=%d, value=%d' % (module, param, value))
		with self.lock:
			result = eas_dll.EAS_SetParameter(self.handle, module, param, value)
			if result:
				raise EAS_Exception(result, 'EAS_SetParameter error %d (param=%d, value=%d)' % (result, param, value), 'EAS_SetParameter')
				
	def GetParameter (self, module, param):
		"""Get the polyphony of a synth."""
		eas_logger.debug('Call EAS_GetParameter: module=%d, param=%d' % (module, param))
		value = c_int(0)
		with self.lock:
			result = eas_dll.EAS_GetParameter(self.handle, module, param, byref(value))
			if result:
				raise EAS_Exception(result, 'EAS_SetParameter error %d (param=%d)' % (result, param), 'EAS_GetParameter')
		eas_logger.debug('Call EAS_SetParameter: module=%d, param=%d, value=%d' % (module, param, value.value))
		return value.value

	def SelectLib (self, test_lib=False):
		eas_logger.debug('Call EAS_SelectLib: test_lib=%s' % test_lib)
		easdll = cdll.LoadLibrary('EASDLL')
		with self.lock:
			result = eas_dll.EAS_SelectLib(self.handle, 0, test_lib)
			if result:
				raise EAS_Exception(result, 'EAS_SelectLib error %d' % result, 'EAS_SelectLib')

	def LoadDLSCollection (self, path):
		eas_logger.debug('Call EAS_LoadDLSCollection: lib_path=%s' % path)
		with self.lock:
			result = eas_dll.EAS_LoadDLSCollection(self.handle, 0, path)
			if result:
				raise EAS_Exception(result, 'EAS_LoadDLSCollection error %d lib %s' % (result, path), 'EAS_LoadDLSCollection')

	def SetAuxMixerHook (self, aux_mixer):

		# if aux mixer has bigger buffer, re-allocate buffer
		if (aux_mixer is not None) and (aux_mixer.buf_size > self.config.mixBufferSize):
			buf_size = aux_mixer.buf_size
		else:
			buf_size = self.config.mixBufferSize

		# allocate audio buffer for rendering
		AudioBufferType = c_ubyte * (2 * buf_size * self.config.numChannels)
		self.audio_buffer = AudioBufferType()
		self.buf_size = buf_size
		self.aux_mixer = aux_mixer

	def SetDebugLevel (self, level=3):
		"""Sets the EAS debug level."""
		with self.lock:
			eas_logger.debug('Call EAS_SetDebugLevel')
			eas_dll.EAS_DLLSetDebugLevel(self.handle, level)

#---------------------------------------------------------------
# EASAuxMixer
#---------------------------------------------------------------
class EASAuxMixer (object):
	def __init__ (self, eas=None, num_streams=3, sample_rate=44100, max_sample_rate=44100):
		eas_logger.debug('EASAuxMixer.__init__')
		self.Init(eas, num_streams, sample_rate, max_sample_rate)

	def __del__ (self):
		eas_logger.debug('EASAuxMixer.__del__')
		self.Shutdown()

	def Init (self, eas=None, num_streams=3, sample_rate=44100, max_sample_rate=44100):
		"""Initializes the EAS Auxilliary Mixer."""
		eas_logger.debug('EASAuxMixer.Init')

		if hasattr(self, 'eas'):
			raise EAS_Exception(-1, 'EASAuxMixer already initialized', 'EASAuxMixer.Init')

		# initialize EAS, if necessary
		if eas is None:
			eas_logger.debug('No EAS handle --- initializing EAS')
			eas = EAS()
			self.alloc_eas = True
		else:
			self.alloc_eas = False
		self.eas = eas

		# initialize library
		eas_logger.debug('Call EAS_InitAuxMixer')
		buf_size = c_int(0)
		result = eas_dll.EAS_InitAuxMixer(eas.handle, num_streams, sample_rate, max_sample_rate, byref(buf_size))
		if result:
			raise EAS_Exception(result, 'EAS_InitAuxMixer error %d' % result, 'EAS_InitAuxMixer')
		self.buf_size = buf_size.value
		self.streams = []
		eas.SetAuxMixerHook(self)

	def Shutdown (self):
		"""Shuts down the EAS Auxilliary Mixer"""
		eas_logger.debug('EASAuxMixer.Shutdown')
		if not hasattr(self, 'eas'):
			return
			
		with self.eas.lock:
			if len(self.streams):
				eas_logger.warning('Stream was not closed before EAS_ShutdownAuxMixer')
				for stream in self.streams:
					self.CloseStream(stream)

			self.eas.SetAuxMixerHook(None)

			# shutdown library
			eas_logger.debug('Call EAS_ShutdownAuxMixer')
			result = eas_dll.EAS_ShutdownAuxMixer(self.eas.handle)
			if result:
				raise EAS_Exception(result, 'EAS_ShutdownAuxMixer error %d' % result, 'EAS_ShutdownAuxMixer')

			# if we created the EAS reference here, shut it down
			if self.alloc_eas:
				self.eas.Shutdown()
				self.alloc_eas = False
			del self.eas

	def OpenStream (self, decoder_func, inst_data, sample_rate, num_channels):
		"""Opens an audio file to be played by the JET library and
		returns a JET_File object

		Arguments:
			callback - callback function to decode more audio

		"""
		with self.eas.lock:
			eas_logger.debug('Call EAS_OpenAudioStream')
			decoder_func = EAS_DECODER_FUNC(decoder_func)
			stream_handle = c_void_p(0)
			result = eas_dll.EAS_OpenAudioStream(self.eas.handle, decoder_func, inst_data, sample_rate, num_channels, stream_handle)
			if result:
				raise EAS_Exception(result, 'EAS_OpenAudioStream error %d on file %s' % (result, path), 'EAS_OpenAudioStream')
			self.streams.add(stream_handle)
			return stream_handle

	def CloseStream (self, stream_handle):
		"""Closes an open audio stream."""
		with self.eas.lock:
			eas_logger.debug('Call EAS_CloseAudioStream')
			result = eas_dll.JET_CloseFile(self.eas.handle, stream_handle)
			if result:
				raise EAS_Exception(result, 'EAS_CloseAudioStream error %d' % result, 'EAS_CloseAudioStream')

#---------------------------------------------------------------
# JET_Status 
#---------------------------------------------------------------
class JET_Status (Structure):
	_fields_ = [('currentUserID', c_int),
				('segmentRepeatCount', c_int),
				('numQueuedSegments', c_int),
				('paused', c_int),
				('location', c_long),
				('currentPlayingSegment', c_int),
				('currentQueuedSegment', c_int),
				]

#---------------------------------------------------------------
# JET_File
#---------------------------------------------------------------
class JET_File (object):
	def __init__ (self, handle, jet):
		eas_logger.debug('JET_File.__init__')
		self.handle = handle
		self.jet = jet

#---------------------------------------------------------------
# JET
#---------------------------------------------------------------
class JET (object):
	def __init__ (self, eas=None):
		# eas_logger.debug('JET.__init__')
		self.Init(eas)

	def __del__ (self):
		eas_logger.debug('JET.__del__')
		self.Shutdown()

	def Init (self, eas=None, config=None):
		"""Initializes the JET Library."""
		# eas_logger.debug('JET.Init')

		if hasattr(self, 'eas'):
			raise EAS_Exception(-1, 'JET library already initialized', 'Jet.Init')

		# create some members
		if eas is None:
			# eas_logger.debug('No EAS handle --- initializing EAS')
			eas = EAS()
			self.alloc_eas = True
		else:
			self.alloc_eas = False
		self.eas = eas
		self.fileOpen = False

		# handle configuration
		if config is None:
			config_handle = c_void_p(0)
			config_size = 0
		else:
			jet_config = S_JET_CONFIG()
			jet_config.appLowNote = config.appLowNote
			config_handle = c_void_p(jet_config)
			config_size = jet_config.sizeof()

		# initialize library
		# eas_logger.debug('Call JET_Init')
		result = eas_dll.JET_Init(eas.handle, config_handle, config_size)
		if result:
			raise EAS_Exception(result, 'JET_Init error %d' % result, 'JET_Init')

	def Shutdown (self):
		"""Shuts down the JET library"""
		eas_logger.debug('JET.Shutdown')
		if not hasattr(self, 'eas'):
			return
			
		with self.eas.lock:
			if self.fileOpen:
				eas_logger.warning('Stream was not closed before JET_Shutdown')
				self.CloseFile()

			# shutdown library
			eas_logger.debug('Call JET_Shutdown')
			result = eas_dll.JET_Shutdown(self.eas.handle)
			if result:
				raise EAS_Exception(result, 'JET_Shutdown error %d' % result, 'JET_Shutdown')

			# if we created the EAS reference here, shut it down
			if self.alloc_eas:
				self.eas.Shutdown()
				self.alloc_eas = False
			del self.eas

	def OpenFile (self, path):
		"""Opens an audio file to be played by the JET library and
		returns a JET_File object

		Arguments:
			path - path to audio file

		"""
		with self.eas.lock:
			eas_logger.debug('Call JET_OpenFile for file: %s' % path)
			result = eas_dll.JET_OpenFile(self.eas.handle, path)
			if result:
				raise EAS_Exception(result, 'JET_OpenFile error %d on file %s' % (result, path), 'JET_OpenFile')

	def CloseFile (self):
		"""Closes an open audio file."""
		with self.eas.lock:
			eas_logger.debug('Call JET_CloseFile')
			result = eas_dll.JET_CloseFile(self.eas.handle)
			if result:
				raise EAS_Exception(result, 'JET_CloseFile error %d' % result, 'JET_CloseFile')

	def QueueSegment (self, userID, seg_num, dls_num=-1, repeat=0, tranpose=0, mute_flags=0):
		"""Queue a segment for playback.

		Arguments:
			seg_num - segment number to queue
			repeat - repeat count (-1=repeat forever, 0=no repeat, 1+ = play n+1 times)
			tranpose - transpose amount (+/-12)
			
			"""
		with self.eas.lock:
			eas_logger.debug('Call JET_QueueSegment')
			result = eas_dll.JET_QueueSegment(self.eas.handle, seg_num, dls_num, repeat, tranpose, mute_flags, userID)
			if result:
				raise EAS_Exception(result, 'JET_QueueSegment error %d' % result, 'JET_QueueSegment')

	def Clear_Queue(self):
		"""Kills the queue."""
		with self.eas.lock:
			eas_logger.debug('Call JET_Clear_Queue')
			result = eas_dll.JET_Clear_Queue(self.eas.handle)
			if result:
				raise EAS_Exception(result, 'JET_Clear_Queue error %d' % result, 'JET_Clear_Queue')

	def GetAppEvent(self):
		"""Gets an App event."""
		with self.eas.lock:
			eas_logger.debug('Call JET_GetEvent')
			result = eas_dll.JET_GetEvent(self.eas.handle, 0, 0)
			return result    

	def Play(self):
		"""Starts JET playback."""
		with self.eas.lock:
			eas_logger.debug('Call JET_Play')
			result = eas_dll.JET_Play(self.eas.handle)
			if result:
				raise EAS_Exception(result, 'JET_Play error %d' % result, 'JET_Play')

	def Pause(self):
		"""Pauses JET playback."""
		with self.eas.lock:
			eas_logger.debug('Call JET_Pause')
			result = eas_dll.JET_Pause(self.eas.handle)
			if result:
				raise EAS_Exception(result, 'JET_Pause error %d' % result, 'JET_Pause')

	def Render (self, count=None, secs=None):
		"""Calls EAS_Render to render audio.

		Arguments
			count - number of buffers to render
			secs - number of seconds to render

		If both count and secs are None, render a single buffer. 

		"""
		# calls JET.Render
		with self.eas.lock:
			self.eas.Render(count, secs)

	def Status (self):
		"""Get JET status."""
		with self.eas.lock:
			eas_logger.debug('Call JET_Status')
			status = JET_Status()
			result = eas_dll.JET_Status(self.eas.handle, byref(status))
			if result:
				raise EAS_Exception(result, 'JET_Status error %d' % result, 'JET_Status')
			eas_logger.debug("currentUserID=%d, repeatCount=%d, numQueuedSegments=%d, paused=%d" %
				(status.currentUserID, status.segmentRepeatCount, status.numQueuedSegments, status.paused))
			return status
				
	def SetVolume (self, volume):
		"""Set the JET volume"""
		eas_logger.debug('Call JET_SetVolume')
		with self.eas.lock:
			result = eas_dll.JET_SetVolume(self.eas.handle, volume)
			if result:
					raise EAS_Exception(result, 'JET_SetVolume error %d' % result, 'JET_SetVolume')

	def SetTransposition (self, transposition):
		"""Set the transposition of a stream."""
		eas_logger.debug('Call JET_SetTransposition')
		with self.eas.lock:
			result = eas_dll.JET_SetTransposition(self.eas.handle, transposition)
			if result:
				raise EAS_Exception(result, 'JET_SetTransposition error %d' % result, 'JET_SetTransposition')

	def TriggerClip (self, clipID):
		"""Trigger a clip in the current segment."""
		eas_logger.debug('Call JET_TriggerClip')
		with self.eas.lock:
			result = eas_dll.JET_TriggerClip(self.eas.handle, clipID)
			if result:
				raise EAS_Exception(result, 'JET_SetTransposition error %d' % result, 'JET_TriggerClip')

	def SetMuteFlag (self, track_num, mute, sync=True):
		"""Trigger a clip in the current segment."""
		eas_logger.debug('Call JET_SetMuteFlag')
		with self.eas.lock:
			result = eas_dll.JET_SetMuteFlag(self.eas.handle, track_num, mute, sync)
			if result:
				raise EAS_Exception(result, 'JET_SetMuteFlag error %d' % result, 'JET_SetMuteFlag')

	def SetMuteFlags (self, mute_flags, sync=True):
		"""Trigger a clip in the current segment."""
		eas_logger.debug('Call JET_SetMuteFlags')
		with self.eas.lock:
			result = eas_dll.JET_SetMuteFlags(self.eas.handle, mute_flags, sync)
			if result:
				raise EAS_Exception(result, 'JET_SetMuteFlag error %d' % result, 'JET_SetMuteFlags')


