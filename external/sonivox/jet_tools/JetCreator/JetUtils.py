"""
 File:  
 JetUtils.py
 
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

from __future__ import with_statement

import wx
import os
import copy
import ConfigParser
import logging
import time
import tempfile
  
from JetDefs import *
from JetDebug import *
from midifile import TimeBase, trackGrid

class JetCutCopy(object):
    """ Handles cut/copy/pasting of events and segments """
    def __init__ (self, objType, objSave, currentSegmentName):
        self.objType = objType
        self.objSave = copy.deepcopy(objSave)
        self.currentSegmentName = currentSegmentName
    
    def GetObj(self, objList):
        """ Gets an object """
        objSave = copy.deepcopy(self.objSave)
        if self.objType == JetDefs.MAIN_SEGLIST:
            oldName = objSave.segname
            i = len(oldName) - 1
            while i > 0:
                if not oldName[i].isdigit():
                    break
                i = i - 1
            oldName = oldName[0:i+1]
            i = 1
            while True:
                newName = oldName + str(i)
                if self.UniqueSegName(newName, objList):
                    break  
                i = i + 1
            objSave.segname = newName
        elif self.objType == JetDefs.MAIN_EVENTLIST:
            oldName = objSave.event_name
            i = len(oldName) - 1
            while i > 0:
                if not oldName[i].isdigit():
                    break
                i = i - 1
            oldName = oldName[0:i+1]
            i = 1
            while True:
                newName = oldName + str(i)
                if self.UniqueEventName(newName, objList):
                    break  
                i = i + 1
            objSave.event_name = newName
        return objSave
    
    def UniqueSegName(self, nameVal, seglist):
        for nm in seglist:
            if nm.segname == nameVal:
                return False
        return True

    def UniqueEventName(self, nameVal, eventlist):
        for nm in eventlist:
            if nm.event_name == nameVal:
                return False
        return True
        
    
class JetState(object):
    """ Saves the state for cut/copy/paste """
    def __init__ (self, jet_file, currentSegmentIndex, currentEventIndex):
        self.jet_file = copy.deepcopy(jet_file)
        self.currentSegmentIndex = currentSegmentIndex
        self.currentEventIndex = currentEventIndex
        
def Queue (jet, queueSeg):
    """ Queues a segment """
    jet.QueueSegment(queueSeg.userID, queueSeg.seg_num, queueSeg.dls_num, queueSeg.repeat, queueSeg.transpose, queueSeg.mute_flags)

class QueueSeg(object):
    """ Object representing a segment """
    def __init__ (self, name, userID, seg_num, dls_num=-1, repeat=0, transpose=0, mute_flags=0, status=''):
        self.name = name
        self.userID = userID
        self.seg_num = seg_num
        self.dls_num = dls_num
        self.repeat = repeat
        self.transpose = transpose
        self.mute_flags = mute_flags
        self.status = status
        #DumpQueueSeg(self)

def FindDlsNum(libraries, dlsfile):
    """ Looks for a dls file in the library list """
    for index, library in enumerate(libraries):
        if library == dlsfile:
            return index
    return -1

def SetRowSelection(list, row, state):
    """ Sets the selection status of a list row """
    if state:
        list.SetItemState(row, wx.LIST_STATE_SELECTED, wx.LIST_STATE_SELECTED)
    else:
        list.SetItemState(row, ~wx.LIST_STATE_SELECTED, wx.LIST_STATE_SELECTED)

def ClearRowSelections(list):
    """ Clears the list rows selection status """
    index = list.GetFirstSelected()
    while index != -1:
        SetRowSelection(list, index, False)
        index = list.GetNextSelected(index)
    
def getColumnText(list, index, col):
    """ Sets the text of a column """
    item = list.GetItem(index, col)
    return item.GetText()
        
def getColumnValue(list, index, col):
    """ Gets the text of a column """
    item = list.GetItem(index, col)
    v = str(item.GetText())
    if len(v) > 0:
        return int(item.GetText())
    else:
        return 0
            
def StrNoneChk(fld):
    """ Returns a blank string if none """
    if fld is None:
        return ""
    return str(fld)

def ConvertStrTimeToTuple(s):
    """ Converts a string time to a tuple """
    try:
        measures, beats, ticks = s.split(':',3)
        return (int(measures), int(beats), int(ticks))
    except:
        return JetDefs.MBT_DEFAULT

def FileRelativePath(target, base=os.curdir):
    """ Returns relative file path """
    if not os.path.exists(target):
        return target

    if not os.path.isdir(base):
        return target

    base_list = (os.path.abspath(base)).split(os.sep)
    target_list = (os.path.abspath(target)).split(os.sep)
    if os.name in ['nt','dos','os2'] and base_list[0] <> target_list[0]:
        return target
    for i in range(min(len(base_list), len(target_list))):
        if base_list[i] <> target_list[i]: break
    else:
        i+=1
    rel_list = [os.pardir] * (len(base_list)-i) + target_list[i:]
    return os.path.join(*rel_list)

def FileFixPath(fileSpec):
    """ Tweaks slashes """
    return fileSpec.replace("\\", "/")
    
def FileKillClean(fileName):
    """ Deletes a file skipping errors """
    try:
        os.remove(fileName)
    except:
        pass

def FileJustRoot(fileName):
    """ Gets just the root of the file name """
    try:
        return os.path.splitext(fileName)[0]
    except:
        return ""
    
def FileJustName(fileName):
    """ Gets just the filename, without the path """
    try:
        return os.path.split(fileName)[1]
    except:
        return ""

def FileJustPath(fileName):
    """ Gets just the path, without the file name """
    try:
        return os.path.split(fileName)[0]
    except:
        return ""  

def FileJustExt(fileName):
    """ Gets just the extension of the file """
    try:
        ext = os.path.splitext(fileName)[1]
        return ext.upper()
    except:
        return ""
    
def FileDateTime(fileName):
    """ Gets the date/time of a file """
    try:
        filetime = time.ctime(os.path.getmtime(fileName))
        return filetime
    except:
        return ""

def FileExists(fileName):
    """ Checks if a file exists """
    try:
        return os.path.exists(fileName)
    except:
        return False
    
def IniSetValue(configFile, section, option, value):
    """ Sets the value of a config file field """
    config = ConfigParser.ConfigParser()
    config.read(configFile)
    if not config.has_section(section):
        config.add_section(section)
    config.set(section, option, value)
    cfgfile = open(configFile,'w')
    config.write(cfgfile)
    cfgfile.close()

def IniGetValue(configFile, section, option, retType='str', default=''):
    """ Gets the value of a config file field """
    ret = default
    config = ConfigParser.ConfigParser()
    config.read(configFile)
    if config.has_section(section):
        if config.has_option(section, option):
            ret = config.get(section, option)
    if retType =='int':
        try:
            ret = int(ret)
        except:
            ret = 0   
    elif retType == 'float':
        try:
            ret = float(ret)
        except:
            ret = 0   
    elif retType == 'bool':
        try:
            if ret[0].upper()=='T':
                ret = True
            else:
                ret = False
        except:
            ret = False    
    elif retType == 'list':
        try:
            ret = eval(ret)
        except:
            ret = []    
    return ret
    
def GetRecentJetFiles():
    """ Builds a list of recent jet files """
    fileList = []
    config = ConfigParser.ConfigParser()
    config.read(JetDefs.JETCREATOR_INI)
    if config.has_section(JetDefs.RECENT_SECTION):
        for count in range(0, 10):
            sFile = "File" + str(count)
            if config.has_option(JetDefs.RECENT_SECTION, sFile):
                sFileName = config.get(JetDefs.RECENT_SECTION, sFile)
                if FileExists(sFileName):
                    if sFileName != JetDefs.UNTITLED_FILE:
                        #fileList.append(FileRelativePath(config.get(JetDefs.RECENT_SECTION, sFile)))
                        fileList.append(config.get(JetDefs.RECENT_SECTION, sFile))
    return fileList
    
def AppendRecentJetFile(jetFile):
    """ Appends to a list of recent jet files """
    addedFiles = []
    fileList = GetRecentJetFiles()
    config = ConfigParser.ConfigParser()
    config.read(JetDefs.JETCREATOR_INI)
    if config.has_section(JetDefs.RECENT_SECTION):
        config.remove_section(JetDefs.RECENT_SECTION)
    config.add_section(JetDefs.RECENT_SECTION)
    config.set(JetDefs.RECENT_SECTION, "File0", jetFile)
    addedFiles.append(jetFile)
    count = 1
    for file in fileList:
        if file not in addedFiles:
            sFile = "File" + str(count)
            config.set(JetDefs.RECENT_SECTION, sFile, file)
            addedFiles.append(file)
            count += 1
    FileKillClean(JetDefs.JETCREATOR_INI)
    cfgfile = open(JetDefs.JETCREATOR_INI,'w')
    config.write(cfgfile)
    cfgfile.close()
    
def CompareMbt(mbt1, mbt2):
    """ Compates to measure/beat/tick values """
    try:
        m1, b1, t1 = mbt1.split(':',3)
        m2, b2, t2 = mbt2.split(':',3)
        if int(m1) > int(m2):
            return False
        elif int(m1) == int(m2) and int(b1) > int(b2):
            return False
        elif int(b1) == int(b2) and int(t1) > int(t2):
            return False
        elif int(m1) == int(m2) and int(b1) == int(b2) and int(t1) == int(t2):
            return False
        else:
            return True
    except:
        return False

def MbtVal(mbt):
    """ Converts mbts to ticks """
    if type(mbt).__name__=='str' or type(mbt).__name__=='unicode':
        mbt1 = mbt
    else:
        mbt1 = "%d:%d:%d" % mbt         
    try:
        return TimeBase().ConvertStrTimeToTicks(mbt1)
    except:
        return 0

def TicksToMbt(ticks):
    """ Converts ticks to mbts """
    return TimeBase().ConvertTicksToMBT(ticks)
    
def TicksToStrMbt(ticks):
    """ Converts ticks to mbts """
    return TimeBase().ConvertTicksToStr(ticks, '%02d:%02d:%02d')
    
def MbtDifference(mbt1, mbt2):
    """ Returns difference between mbt values """
    return TimeBase().MbtDifference(mbt1, mbt2)
        
def PlayMidiFile(midiFile, dlsFile=''):
    """ Plays a midi file """
    try:
        e = __import__('eas')
        
        if midiFile == '':
            return
        eas = e.EAS()
        if dlsFile > '':
            eas.LoadDLSCollection(dlsFile)
        eas.StartWave()
        audio_file = eas.OpenFile(midiFile)
        audio_file.Prepare()
        audio_file.Play()
        audio_file.Close()
        eas.StopWave()
        eas.Shutdown()
    except:
        return
    
def SegmentOutputFile(segName, configFile):
    """ Computes a segment output file """
    configPath = FileJustPath(configFile) + "/"
    segOutput = configPath + "Seg_" + segName + ".mid"
    return segOutput

def ComputeMuteFlags(jet_file, segName):
    """ Computes mute flags """
    muteFlag = 0
    for jet_event in jet_file.GetEvents(segName):
        muteFlag = SetMute(jet_event.track_num, muteFlag)
    return muteFlag

def ComputeMuteFlagsFromList1(list):
    """ Computes mute flags from a list """
    muteFlag = 0
    num = list.GetItemCount()
    for iRow in range(num):
        track_num = list.GetTrackNumber(iRow)
        if list.IsChecked(iRow):
            muteFlag = SetMute(track_num, muteFlag)
        else:
            muteFlag = ClearMute(track_num, muteFlag)
    return muteFlag

def ComputeMuteFlagsFromList(list):
    """ Computes mute flags from a list """
    muteFlags = 0
    num = list.GetItemCount()
    for iRow in range(num):
        track_num = list.GetTrackNumber(iRow)
        if list.IsChecked(iRow):
            muteFlags = SetMute(track_num, muteFlags)            
    return muteFlags
 

def SetMuteFlag(track, muteFlag, mute): 
    """ Sets a mute flag """
    if mute:
        SetMute(track, muteFlag) 
    else:
        ClearMute(track, muteFlag)  
        
def SetMute(track, muteFlag):
    """ Sets a mute flag """
    try:
        muteFlag |= 1 << (track)
        return muteFlag
    except:
        #bad argument
        return muteFlag

def ClearMute(track, muteFlag):
    """ Clears a mute flag """
    try:
        muteFlag &= ~(1 << (track))
        return muteFlag;
    except:
        #bad argument
        return muteFlag

def GetMute(track, muteFlag):
    """ Get a mute flag """
    try:
        if (muteFlag & ( 1 << (track))) == 0:
            return False
        else:
            return True
    except:
        #bad argument
        return False

def InfoMsg(msgTitle, msgText):
    """ Display a simple informational message """
    dlg = wx.MessageDialog(None,
                           message=msgText,
                           caption=msgTitle,
                           style=wx.OK|wx.ICON_INFORMATION
                           )
    dlg.ShowModal()
    dlg.Destroy()
    
def SendEvent (mycontrol, evt):
    """ Sends an event """
    cmd = wx.CommandEvent(evt)
    cmd.SetEventObject(mycontrol)
    cmd.SetId(mycontrol.GetId())
    mycontrol.GetEventHandler().ProcessEvent(cmd)
        
def GetJetHelpText(dlgName, fld):
    """ Gets the jet help text file """
    return IniGetValue(JetDefs.JETCREATOR_HLP, dlgName, fld)

def ExportJetArchive(fileName, jetConfigFile, jetFile):
    """ Exports all files into a zip archive file """
    z = __import__('zipfile')
    zip = z.ZipFile(fileName, 'w')

    #zip the original .JET file
    if FileExists(jetFile.config.filename):
        zip.write(jetFile.config.filename, FileJustName(jetFile.config.filename))
    
    #make copy of object so we can modify it
    jet_file = copy.deepcopy(jetFile)
    
    #zip the files, without paths
    for segment in jet_file.GetSegments():
        if FileExists(segment.filename):
            if not FileJustName(segment.filename) in zip.namelist():
                zip.write(segment.filename, FileJustName(segment.filename))
        if FileExists(segment.output):
            if not FileJustName(segment.output) in zip.namelist():
                zip.write(segment.output, FileJustName(segment.output))
    
    #zip the library files
    for library in jet_file.GetLibraries():
        if FileExists(library):
            if not FileJustName(library) in zip.namelist():
                zip.write(library, FileJustName(library))
    
    #remove the paths on filenames
    for segment in jet_file.GetSegments():
        segment.filename = FileJustName(segment.filename)
        segment.dlsfile = FileJustName(segment.dlsfile)
        segment.output = FileJustName(segment.output)
    
    #remove paths
    for index, library in enumerate(jet_file.libraries):
        jet_file.libraries[index] = FileJustName(library)
        
    #create temporary .JTC file so we can modify paths to files
    tmpConfigFile = JetDefs.TEMP_JET_CONFIG_FILE
    FileKillClean(tmpConfigFile)
    
    #save the file
    jet_file.SaveJetConfig(tmpConfigFile)
    
    #zip it and rename it back to original name without path
    zip.write(tmpConfigFile, FileJustName(jetConfigFile))
     
    #create a flag file so we know this is a jet archive
    zip.write(tmpConfigFile, "JetArchive")
     
    zip.close()

    FileKillClean(tmpConfigFile)
    
def ValidateConfig(test_jet_file):
    """ Validates the contents of a config file """
    dImp = __import__('JetDialogs')
    errors = []
    fatalError = False
    for segment in test_jet_file.segments:
        logging.debug(segment.filename)
        if segment.filename is not None and len(segment.filename) > 0 and not FileExists(segment.filename):
            errors.append(("Segment MIDI file not found", segment.filename))
            fatalError = True
        if segment.dlsfile is not None and len(segment.dlsfile) > 0 and not FileExists(segment.dlsfile):
            errors.append(("Segment DLS file not found; removing from config", segment.dlsfile))
            segment.dlsfile = ""
           
    logging.debug(test_jet_file.config.filename)
        
    if len(errors) == 0:
        return True
    else:
        dlg = dImp.JetErrors("Jet Definition File Errors")
        dlg.SetErrors(errors)
        result = dlg.ShowModal()
        dlg.Destroy()
        if fatalError:
            return False
        else:
            return True
        
def release_getLogger(name):
    """  passing original handler with debug() method replaced to empty function """

    def dummy(*k, **kw):
        pass

    global __orig_getLogger
    log = __orig_getLogger(name)
    setattr(log, 'debug', dummy)
    setattr(log, 'info', dummy)
    setattr(log, 'error', dummy)
    setattr(log, 'critical', dummy)
    return log

def install_release_loggers():
    """ Save original handler, installs newer one """
    global __orig_getLogger
    __orig_getLogger = logging.getLogger
    setattr(logging, 'getLogger', release_getLogger)

def restore_getLogger():
    """ Restores original handler """
    global __orig_getLogger
    if __orig_getLogger:
        setattr(logging, 'getLogger', __orig_getLogger)

def GetMidiFileLength(midiFile):
    """ Gets the length of a midi file via eas """
    e = __import__('eas')
       
    if not FileExists(midiFile):
        return 0 
    
    eas = e.EAS()
    audio_file = eas.OpenFile(midiFile)
    audio_file.Prepare()
    midiLength = eas.audio_streams[0].ParseMetaData()
    audio_file.Close()
    eas.Shutdown()
    return midiLength

def GetMidiInfo(midiFile):
    """ Gets midi file info """
    m = __import__('midifile')
    md = m.GetMidiInfo(midiFile)
    return md

def PrintMidiInfo(midiFile):
    """ Prints info about a midi file """
    mi = GetMidiInfo(midiFile)
    if mi.err == 0:
        print("ppqn: " + str(mi.ppqn))
        print("beats_per_measure: " + str(mi.beats_per_measure))
        print("ending mbt: " + str(mi.endMbt))
        print("ending mbt str: " + mi.endMbtStr)
        print("maxMeasures: " + str(mi.maxMeasures))
        print("maxBeats: " + str(mi.maxBeats))
        print("maxTicks: " + str(mi.maxTicks))
        print("maxTracks: " + str(mi.maxTracks))
        print("totalTicks: " + str(mi.totalTicks))   
        for track in mi.trackList:
            print(track)
    else:
        print("Error opening") 
    
def MidiSegInfo(segment):
    """ Midi file info saved in config file for speed """
    class segInfo:
        iMsPerTick = 0
        bpm = 4
        ppqn = 480
        total_ticks = 0
        iLengthInMs = 0
        iTracks = 0
        trackList = []
    
    ver = "1.5"
    ret = segInfo()
    savedVer = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "Ver")
    savedDateTime = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "DateTime")
    dateTime = FileDateTime(segment.filename)
    if ver != savedVer or dateTime != savedDateTime:
        mi = GetMidiInfo(segment.filename)
        if mi.err == 0:
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "Ver", ver)
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "DateTime", str(dateTime))
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "PPQN", str(mi.ppqn))
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "BPM", str(mi.beats_per_measure))
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "totalTicks", str(mi.totalTicks))
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "maxTracks", str(mi.maxTracks))
            iLengthInMs = GetMidiFileLength(segment.filename) * 1000
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "LengthInMs", str(iLengthInMs))
            if iLengthInMs > 0:
                IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "MsPerTick", str(iLengthInMs / mi.totalTicks))
            #have to write out the tracklist in format that can be saved in INI file
            tl = []
            for track in mi.trackList:
                tl.append((track.track, track.channel, track.name))
            IniSetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "Tracks", tl)
            
    trackList = []
    tl = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "Tracks", 'list', [])
    for t in tl:
        trackList.append(trackGrid(t[0], t[1], t[2],False))
    iTracks = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "maxTracks", 'int', 0)
    iMsPerTick = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "MsPerTick", 'float', 0)
    bpm = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "BPM", 'int', 0)
    ppqn = IniGetValue(JetDefs.JETMIDIFILES_INI, segment.filename, "PPQN", 'int', 480)
    if iMsPerTick == 0 or bpm == 0 or ppqn == 0:
        return ret
    tb = TimeBase(ppqn, bpm)
    total_ticks = tb.ConvertStrTimeToTicks(segment.length)
    if total_ticks == 0:
        total_ticks = tb.MbtDifference(tb.ConvertStrTimeToTuple(segment.start), tb.ConvertStrTimeToTuple(segment.end))
    if total_ticks == 0:
        return ret

    ret.iTracks = iTracks
    ret.iMsPerTick = iMsPerTick
    ret.bpm = bpm
    ret.ppqn = ppqn
    ret.total_ticks = total_ticks
    ret.iLengthInMs = total_ticks * iMsPerTick
    ret.trackList = trackList
    return ret
    
def TimeStr(ms):
    """ Returns a time string """
    s=ms/1000
    m,s=divmod(s,60)
    h,m=divmod(m,60)
    d,h=divmod(h,24)
    if m > 0:
        return "%d Min %d Sec" % (m,s)
    else:
        return "%d Seconds" % (s)
        
def mbtFct(mbt, mod):
    """ Converts times """
    if type(mbt).__name__=='str' or type(mbt).__name__=='unicode':
        mbt = ConvertStrTimeToTuple(mbt)
        retType = 'str'
    else:
        retType = 'int'
        
    m = mbt[0]+mod
    b = mbt[1]+mod
    t = mbt[2]
    if m < 0:
        m = 0
    if b < 0:
        b = 0
    if b > 4:
        b = 4
    if t < 0:
        t = 0
        
    if retType == 'str':    
        return "%d:%d:%d" % (m, b, t)
    else:
        return (m, b, t)
 
def OsWindows():
    """ Tells us whether windows or os x """
    if os.name == 'nt':
        return True ;
    else:
        return False ;
      
def MacOffset():
    """ Mac screen coordinates funky on some controls so we finagle a few pixels """
    if not OsWindows():
        return 3
    else:
        return 0
    
def SafeJetShutdown(lock, jet):
    """ Makes sure we do the jet shutdown properly """
    with lock:
        #MAKE SURE WE CLEANUP
        #try: jet.Clear_Queue()
        #except: pass
        
        try: jet.eas.StopWave()
        except: pass
        
        try: jet.Shutdown()
        except: pass

        jet = None
    
    
def CreateTempJetFile(org_jet_file):
    """ Creates temporary jet file for playback testing """
    dirname = JetDefs.TEMP_JET_DIR
    if not os.path.isdir(dirname):
        os.mkdir(dirname)
        
    tmpConfigFile = dirname + FileJustName(org_jet_file.config_file)
    FileKillClean(tmpConfigFile)
    
    jet_file = copy.deepcopy(org_jet_file)
    
    for tmp in jet_file.segments:
        tmp.output = dirname + FileJustName(tmp.output)
    
    jet_file.config_file = tmpConfigFile        
    jet_file.config.filename = dirname + FileJustName(jet_file.config.filename)
    FileKillClean(jet_file.config.filename)
    
    jet_file.SaveJetConfig(tmpConfigFile)
    jet_file.WriteJetFileFromConfig(tmpConfigFile)

    return jet_file
    
def CleanupTempJetFile(jet_file):
    """ Cleans up temporary files """
    FileKillClean(jet_file.config.filename)
    FileKillClean(jet_file.config_file)
    for tmp in jet_file.segments:
        FileKillClean(tmp.output)

def GetNow():
    return time.asctime()


if __name__ == '__main__':
    """ Tests functions """
    pass
     
        