"""
 File:  
 JetPreview.py
 
 Contents and purpose:
 Plays the preview of a segment or event via the dialog box
 
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
import threading

from JetDefs import *
from JetCtrls import *
from JetFile import *
from JetUtils import *
from eas import *
from JetStatusEvent import *

class PreviewPlayer(wx.Frame):
    """ Segment player """
    def __init__ (self, play_button, segment):
        self.segment = segment
        self.play_button = play_button
        self.mute_button = None
        self.trigger_button = None
        self.playerLock = threading.RLock()
        self.SetKeepPlayingFlag(False)
        self.graph = None
       
    def SetGraphCtrl(self, graph, parentWin):
        """ Sets the graph control for the player """
        self.graph = graph
        self.parentWin = parentWin
        
    def SetGraphCallbackFct(self, ClickCallbackFct):
        """ Sets the callback function for the graph control to update """
        self.ClickCallbackFct = ClickCallbackFct
        
    def GraphTriggerClip(self, sClipName, iEventId):
        """ Triggers a clip by clicking on it """
        with self.playerLock:
            try:
                self.jet.TriggerClip(iEventId)
                return True
            except:
                return False       
            
    def SetMuteFlag(self, trackNum, mute):
        """ Sets a mute flag """
        sync = JetDefs.DEFAULT_MUTE_SYNC
        with self.playerLock:
            try:
                self.jet.SetMuteFlag(trackNum, mute, sync)
                logging.info("SetMuteFlag() Track:%d Mute:%d Sync:%d" % (trackNum, mute, sync))
                return True
            except:
                return False
        
    def TriggerClip(self, eventID):
        """ Triggers a clip via function """
        with self.playerLock:
            try:
                self.jet.TriggerClip(eventID)
                logging.info("TriggerClip() eventID: %d" % eventID)
                return True
            except:
                return False       
        
    def MuteTrackViaButton(self, button, trackNum):
        """ Mutes a track via a button """
        with self.playerLock:
            self.mute_button = button
            if button.GetLabel() == JetDefs.BUT_MUTE:
                if self.SetMuteFlag(trackNum, True):
                    button.SetLabel(JetDefs.BUT_UNMUTE)
            else:    
                if self.SetMuteFlag(trackNum, False):
                    button.SetLabel(JetDefs.BUT_MUTE)
                   
    def Start(self):
        """ Starts the playback.  Called as a thread from dialog boxes """
        self.paused = False
              
        wx.PostEvent(self.parentWin, JetStatusEvent(JetDefs.PST_PLAY, None))

        # create a temporary config file, and jet output file
        FileKillClean(JetDefs.TEMP_JET_CONFIG_FILE)
        
        self.jet_file = JetFile(JetDefs.TEMP_JET_CONFIG_FILE, "")
        
        self.jet_file.AddSegment(self.segment.segname, 
                                 self.segment.filename, 
                                 self.segment.start, 
                                 self.segment.end, 
                                 self.segment.length, 
                                 SegmentOutputFile(self.segment.segname, JetDefs.TEMP_JET_CONFIG_FILE), 
                                 self.segment.quantize,
                                 self.segment.jetevents,
                                 self.segment.dlsfile,
                                 None,
                                 self.segment.transpose,
                                 self.segment.repeat,
                                 self.segment.mute_flags)
        userID = 0
        dls_num = -1
        seg_num = 0
        
        if len(self.segment.dlsfile) > 0:
            self.jet_file.libraries.append(self.segment.dlsfile)
            dls_num = 0
                        
        self.jet_file.SaveJetConfig(JetDefs.TEMP_JET_CONFIG_FILE)
        self.jet_file.WriteJetFileFromConfig(JetDefs.TEMP_JET_CONFIG_FILE)
        
        if not ValidateConfig(self.jet_file):
            return

        self.queueSegs = []
        self.queueSegs.append(QueueSeg(self.segment.segname, userID, seg_num, dls_num, self.segment.repeat, self.segment.transpose, self.segment.mute_flags))

        self.jet = JET()
        self.jet.eas.StartWave()
        self.jet.OpenFile(self.jet_file.config.filename)

        # queue first segment and start playback
        index = 0
        Queue(self.jet, self.queueSegs[index])
        
        index += 1
        self.jet.Play()
    
        self.SetKeepPlayingFlag(True)
        while self.GetKeepPlayingFlag():
            self.jet.Render()
            status = self.jet.Status()
            
            # if no more segments - we're done
            if status.numQueuedSegments == 0:
                break
    
            self.jet.GetAppEvent()

            # if less than 2 segs queued - queue another one            
            if (index < len(self.queueSegs)) and (status.numQueuedSegments < 2):
                Queue(self.jet, self.queueSegs[index])
                index += 1

            wx.PostEvent(self.parentWin, JetStatusEvent(JetDefs.PST_UPD_LOCATION, status.location))

        SafeJetShutdown(self.playerLock, self.jet)   
        
        FileKillClean(SegmentOutputFile(self.segment.segname, JetDefs.TEMP_JET_CONFIG_FILE))
        FileKillClean(JetDefs.TEMP_JET_CONFIG_FILE)
        FileKillClean(self.jet_file.config.filename)
        
        self.SetKeepPlayingFlag(False)
        
        wx.PostEvent(self.parentWin, JetStatusEvent(JetDefs.PST_DONE, None))

        wx.PostEvent(self.parentWin, JetStatusEvent(JetDefs.PST_UPD_LOCATION, 0))
        
    def SetKeepPlayingFlag(self, val):
        """ Sets the flag to tell us wheter to keep playing """
        with self.playerLock:
            self.keepPlaying = val
            
    def GetKeepPlayingFlag(self): 
        """ Gets the keep playing flag """     
        with self.playerLock:
            return self.keepPlaying

    def Pause(self):
        """ Pauses playback """
        if self.jet is None:
            return
        if not self.paused:
            self.jet.Pause()
            self.paused = True
            wx.PostEvent(self.parentWin, JetStatusEvent(JetDefs.PST_PAUSE, None))
        else:
            self.jet.Play()
            self.paused = False
            wx.PostEvent(self.parentWin, JetStatusEvent(JetDefs.PST_RESUME, None))

