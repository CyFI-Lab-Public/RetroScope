"""
 File:  
 JetAudition.py
 
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

import wx
import sys
import thread
import time

from JetUtils import *
from JetDefs import *
from JetCtrls import JetListCtrl, JetTrackCtrl
from JetSegGraph import SegmentGraph, Marker
from eas import *
from JetStatusEvent import *

CMD_QUEUE_AND_CANCEL = 'QueueNCancel'
CMD_QUEUE_AND_CANCEL_CURRENT = 'QueueCancelCurrent'
CMD_MUTEALL = 'MuteAll'
CMD_UNMUTEALL = 'UnMuteAll'
CMD_ORIGINALMUTES = 'MuteOrg'
CMD_STOP = 'Stop'
CMD_PAUSE = 'Pause'
CMD_PLAY = 'Play'

STATUS_PENDING = 'Pending'
STATUS_PLAYING = 'Playing'
STATUS_COMPLETE = 'Complete'
STATUS_CANCELED = 'Canceled'
STATUS_QUEUED = 'Queued'

LOAD_QUEUE_DISPLAY = 'LOAD_QUEUE'
GRAPH_POSITION_UPDATE = 'GRAPH_POS'
NEW_SEGMENT_DISPLAY = 'NEW SEG'
CLR_INFO = 'CLR_INFO'

class Audition(wx.Dialog):
    """ Initializes Audition window controls, then spawns off a thread to be ready for playback commands """
    def __init__(self, jet_file, pSize):
        wx.Dialog.__init__(self, None, -1, title=JetDefs.DLG_AUDITION)
        
        self.jet = None
        self.playerLock = threading.RLock()
        self.jet_file = jet_file
        self.queueSegs = []
        self.keepPlaying = True
        self.nextSegNum = 0
        self.currentSegmentIndex = None
        self.currentSegmentName = ""        
        self.playCommand = ""
        self.threadShutdown = True
        
        panel = wx.Panel(self, -1)

        self.segList = JetListCtrl(panel)
        self.segList.AddCol(JetDefs.GRD_SEGMENTS, 180)
        self.segList.AddCol(JetDefs.GRD_LENGTH, 20)
        
        self.segList.Bind(wx.EVT_LIST_ITEM_ACTIVATED, self.OnQueueSegment)
        self.segList.Bind(wx.EVT_LIST_ITEM_SELECTED, self.OnSegListClick)
        
        self.queueList = JetListCtrl(panel)
        self.queueList.AddCol(JetDefs.GRD_QUEUE, 180)
        self.queueList.AddCol(JetDefs.GRD_STATUS, 20)

        self.trackList = JetTrackCtrl(panel)
        self.trackList.AddCol(JetDefs.GRD_TRACK, JetDefs.MUTEGRD_TRACK)
        self.trackList.AddCol(JetDefs.GRD_CHANNEL, JetDefs.MUTEGRD_CHANNEL)
        self.trackList.AddCol(JetDefs.GRD_NAME, JetDefs.MUTEGRD_NAME)
        self.trackList.BindCheckBox(self.OnTrackChecked)
        
        self.btnMuteAll = wx.Button(panel, -1, JetDefs.BUT_MUTEALL)
        self.btnUnMuteAll = wx.Button(panel, -1, JetDefs.BUT_MUTENONE)
        self.btnMuteOrg = wx.Button(panel, -1, JetDefs.BUT_ORGMUTES)
        hMuteButs = wx.BoxSizer(wx.HORIZONTAL)
        hMuteButs.Add(self.btnMuteAll, 1, wx.EXPAND)
        hMuteButs.Add(self.btnUnMuteAll, 1, wx.EXPAND)
        hMuteButs.Add(self.btnMuteOrg, 1, wx.EXPAND)
        vMuteButs = wx.BoxSizer(wx.VERTICAL)
        vMuteButs.Add(self.trackList, 1, wx.EXPAND)
        vMuteButs.Add((-1, 5))
        vMuteButs.Add(hMuteButs, 0, wx.EXPAND)
        
        self.btnQueue = wx.Button(panel, -1, JetDefs.BUT_QUEUE)
        self.btnCancelNQueue = wx.Button(panel, -1, JetDefs.BUT_CANCELANDQUEUE)
        hSegButs = wx.BoxSizer(wx.HORIZONTAL)
        hSegButs.Add(self.btnQueue, 1, wx.EXPAND)
        hSegButs.Add(self.btnCancelNQueue, 1, wx.EXPAND)
        vSegButs = wx.BoxSizer(wx.VERTICAL)
        vSegButs.Add(self.segList, 1, wx.EXPAND)
        vSegButs.Add((-1, 5))
        vSegButs.Add(hSegButs, 0, wx.EXPAND)
        
        self.btnQueueCancelCurrent = wx.Button(panel, -1, JetDefs.BUT_CANCELCURRENT)
        self.btnPause = wx.Button(panel, -1, JetDefs.BUT_PAUSE)
        self.btnStop = wx.Button(panel, -1, JetDefs.BUT_STOP)
        hQueueButs = wx.BoxSizer(wx.HORIZONTAL)
        hQueueButs.Add(self.btnQueueCancelCurrent, 1, wx.EXPAND)
        hQueueButs.Add(self.btnPause, 1, wx.EXPAND)
        hQueueButs.Add(self.btnStop, 1, wx.EXPAND)
        vQueueButs = wx.BoxSizer(wx.VERTICAL)
        vQueueButs.Add(self.queueList, 1, wx.EXPAND)
        vQueueButs.Add((-1, 5))
        vQueueButs.Add(hQueueButs, 0, wx.EXPAND)

        self.Bind(wx.EVT_BUTTON, self.OnQueueSegmentViaBut, id=self.btnQueue.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnCancelNQueue, id=self.btnCancelNQueue.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnStop, id=self.btnStop.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnQueueCancelCurrent, id=self.btnQueueCancelCurrent.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnPause, id=self.btnPause.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnMuteAll, id=self.btnMuteAll.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnUnMuteAll, id=self.btnUnMuteAll.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnMuteOrg, id=self.btnMuteOrg.GetId())
        
        EVT_JET_STATUS(self, self.OnJetStatusUpdate)

        BORDER = 10
        hboxTop = wx.BoxSizer(wx.HORIZONTAL)
        hboxTop.Add(vSegButs, 1, wx.EXPAND)
        hboxTop.Add((5, -1))
        hboxTop.Add(vQueueButs, 1, wx.EXPAND)
        hboxTop.Add((5, -1))
        hboxTop.Add(vMuteButs, 1, wx.EXPAND)

        self.log = wx.TextCtrl(panel, -1)
        self.graph = SegmentGraph(panel, size=(-1, 50))
        self.graph.ClickCallbackFct = self.GraphTriggerClip

        vboxBot = wx.BoxSizer(wx.VERTICAL)
        vboxBot.Add(self.log, 0, wx.EXPAND)
        vboxBot.Add((-1, 5))
        vboxBot.Add(self.graph, 1, wx.EXPAND)

        hboxMain = wx.BoxSizer(wx.VERTICAL)
        hboxMain.Add(hboxTop, 2, wx.EXPAND | wx.ALL, BORDER)
        hboxMain.Add(vboxBot, 1, wx.EXPAND | wx.ALL, BORDER)

        panel.SetSizer(hboxMain)

        self.LoadSegList()
        self.initHelp()
        
        self.SetSize(pSize)
        self.CenterOnParent()

        wx.EVT_CLOSE(self, self.OnClose)

        thread.start_new_thread(self.PlaySegs, ())
        
    def initHelp(self):
        """ Initializes context sensitive help text """
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP )        
        self.SetHelpText(GetJetHelpText(JetDefs.AUDITION_CTRLS, ''))
        self.segList.SetHelpText(GetJetHelpText(JetDefs.AUDITION_CTRLS, JetDefs.AUDITION_SEGLIST))
        self.queueList.SetHelpText(GetJetHelpText(JetDefs.AUDITION_CTRLS, JetDefs.AUDITION_QUEUELIST))
        self.trackList.SetHelpText(GetJetHelpText(JetDefs.AUDITION_CTRLS, JetDefs.AUDITION_TRACKLIST))
        self.graph.SetHelpText(GetJetHelpText(JetDefs.AUDITION_CTRLS, JetDefs.AUDITION_GRAPH))

    def OnMuteAll(self, event):
        """ Sets command to mute all tracks """
        self.SetPlayCommand(CMD_MUTEALL)

    def OnUnMuteAll(self, event):
        """ Sets command to un-mute all tracks """
        self.SetPlayCommand(CMD_UNMUTEALL)
            
    def OnMuteOrg(self, event):
        """ Sets command to set mute flags to their original values """
        self.SetPlayCommand(CMD_ORIGINALMUTES)
    
    def OnTrackChecked(self, index, checked):
        """ Mutes or un-mutes a track interactively """
        with self.playerLock:
            trackNum = self.trackList.GetTrackNumber(index) 
            self.SetMuteFlag(trackNum, checked)
            
    def SetMuteFlag(self, trackNum, mute):
        """ Mutes or un-mutes a track """
        with self.playerLock:
            try:
                sync = JetDefs.DEFAULT_MUTE_SYNC
                self.jet.SetMuteFlag(trackNum, mute, sync)
                logging.info("SetMuteFlag() Track:%d Mute:%d Sync:%d" % (trackNum, mute, sync))
                return True
            except:
                return False
            
    def LoadSegList(self):
        """ Loads the list of segments """
        with self.playerLock:
            self.segList.DeleteAllItems()
            for segment in self.jet_file.GetSegments():
                info = MidiSegInfo(segment)
                index = self.segList.InsertStringItem(sys.maxint, StrNoneChk(segment.segname))
                self.segList.SetStringItem(index, 1,  TimeStr(info.iLengthInMs))

    def GraphTriggerClip(self, sClipName, iEventId):
        """ Triggers a clip """
        with self.playerLock:
            try:
                self.jet.TriggerClip(iEventId)
                self.log.SetValue(JetDefs.PLAY_TRIGGERCLIP_MSG % (iEventId, sClipName))
                return True
            except:
                return False       

    def OnSegListClick(self, event):
        """ Sets current segment name based on what's clicked """
        with self.playerLock:
            self.currentSegmentIndex = event.m_itemIndex
            self.currentSegmentName = getColumnText(self.segList, event.m_itemIndex, 0)        
    
    def OnCancelNQueue(self, event):
        """ Sets command to cancel the currently playing segment and queues another """
        if self.currentSegmentIndex == None:
            return
        self.SetPlayCommand(CMD_QUEUE_AND_CANCEL)

    def OnPause(self, event):
        """ Sets a command to pause playback """
        if self.currentSegmentIndex == None:
            return
        self.SetPlayCommand(CMD_PAUSE)

    def OnStop(self, event):
        """ Sets a command to stop playback """
        if self.currentSegmentIndex == None:
            return
        self.SetPlayCommand(CMD_STOP)
            
    def OnQueueCancelCurrent(self, event):
        """ Sets a command to cancel the currently playing segment """
        if self.currentSegmentIndex == None:
            return
        self.SetPlayCommand(CMD_QUEUE_AND_CANCEL_CURRENT)
            
    def OnQueueSegmentViaBut(self, event):
        """ Queues a segment via the button """
        if self.currentSegmentIndex == None:
            return
        with self.playerLock:
            segNum = self.currentSegmentIndex
            segment = self.jet_file.GetSegment(self.currentSegmentName)
            self.QueueOneSegment(segment, segNum)
            
    def OnQueueSegment(self, event):
        """ Queues a segment """
        with self.playerLock:
            segNum = event.m_itemIndex
            segment = self.jet_file.GetSegment(getColumnText(self.segList, segNum, 0))
            self.QueueOneSegment(segment, segNum)
            
    def QueueOneSegment(self, segment, segNum):
        """ Queues one segment """
        with self.playerLock:
            userID = len(self.queueSegs)
            if FileExists(segment.dlsfile):
                dls_num = FindDlsNum(self.jet_file.libraries, segment.dlsfile)
            else:
                dls_num = -1
            self.queueSegs.append(QueueSeg(segment.segname, userID, segNum, dls_num, segment.repeat, segment.transpose, segment.mute_flags, STATUS_PENDING))
            self.LoadQueueDisplay()
        
    def SetKeepPlayingFlag(self, val):
        """ Sets a flag to continue play loop or shut down """
        with self.playerLock:
            self.keepPlaying = val
            
    def GetKeepPlayingFlag(self):  
        """ Gets the play flag """    
        with self.playerLock:
            return self.keepPlaying
        
    def SetThreadShutdownFlag(self, val):
        """ Set a flag to shutdown thread """
        with self.playerLock:
            self.threadShutdown = val
            
    def GetThreadShutdownFlag(self):      
        """ Gets the thread shutdown flag """
        with self.playerLock:
            return self.threadShutdown
        
    def SetPlayCommand(self, cmd):
        """ Sets a play command """
        with self.playerLock:
            self.playCommand = cmd
            
    def GetPlayCommand(self):    
        """ Gets a play command """  
        with self.playerLock:
            return self.playCommand
        
    def SetStatus(self, index, status):
        """ Sets the status of a segment """
        with self.playerLock:
            self.queueSegs[index].status = status
            
    def GetStatus(self, index):      
        """ Gets the status of a segment """
        with self.playerLock:
            return self.queueSegs[index].status
        
    def LoadQueueDisplay(self):
        """ Loads up the displayed queue list """
        with self.playerLock:
            self.queueList.DeleteAllItems()
            for item in self.queueSegs:
                index = self.queueList.InsertStringItem(sys.maxint, item.name)
                self.queueList.SetStringItem(index, 1,  item.status)
        
    def NextSegment(self):
        """ Gets the next segment in the queueu """
        with self.playerLock:
            num = len(self.queueSegs)
            for i in range(num):
                if self.queueSegs[i].status == STATUS_PENDING:
                    return i
            return -1
        
    def PlaySegs(self):
        """ Sets up a loop looking for jet file actions based on UI commands """
        self.jet = JET()
        self.jet.eas.StartWave()
        self.jet.OpenFile(self.jet_file.config.filename)
        
        self.SetKeepPlayingFlag(True)
        while self.GetKeepPlayingFlag():            
            self.SetThreadShutdownFlag(False)

            time.sleep(.5)
            index = self.NextSegment()
            if index != -1:
                lastID = -1
        
                Queue(self.jet, self.queueSegs[index])
                
                self.SetStatus(index, STATUS_QUEUED)      
                
                wx.PostEvent(self, JetStatusEvent(LOAD_QUEUE_DISPLAY, None))
                
                self.jet.Play()
                self.paused = False
                wx.PostEvent(self, JetStatusEvent(CMD_PLAY, None))
 
                while self.GetKeepPlayingFlag():
                    self.jet.Render()
                    status = self.jet.Status()
                    
                    if status.currentUserID <> lastID and status.currentUserID <> -1:
                        wx.PostEvent(self, JetStatusEvent(NEW_SEGMENT_DISPLAY, status.currentUserID))
                        if lastID != -1:
                            self.SetStatus(lastID, STATUS_COMPLETE)      
                        self.SetStatus(status.currentUserID, STATUS_PLAYING)      
                        lastID = status.currentUserID
                        wx.PostEvent(self, JetStatusEvent(LOAD_QUEUE_DISPLAY, None))
                        
                    if status.numQueuedSegments == 0:
                        break
            
                    self.jet.GetAppEvent()
                    
                    index = self.NextSegment()
                    if (index >= 0) and (status.numQueuedSegments < 2):
                        Queue(self.jet, self.queueSegs[index])
                        self.SetStatus(index, STATUS_QUEUED)      
                        wx.PostEvent(self, JetStatusEvent(LOAD_QUEUE_DISPLAY, None))
                        
                    wx.PostEvent(self, JetStatusEvent(GRAPH_POSITION_UPDATE, status.location))
                        
                    playCmd = self.GetPlayCommand()
                    if playCmd == CMD_QUEUE_AND_CANCEL or playCmd == CMD_STOP or playCmd == CMD_QUEUE_AND_CANCEL_CURRENT:
                        if playCmd == CMD_QUEUE_AND_CANCEL or playCmd == CMD_STOP:
                            num = len(self.queueSegs)
                            for i in range(num):
                                curStatus = self.GetStatus(i)
                                if curStatus == STATUS_PENDING or curStatus == STATUS_PLAYING or curStatus == STATUS_QUEUED:
                                    self.SetStatus(i, STATUS_CANCELED)      

                        if playCmd == CMD_QUEUE_AND_CANCEL_CURRENT:
                            self.SetStatus(status.currentUserID, STATUS_CANCELED)      
                            num = len(self.queueSegs)
                            for i in range(num):
                                curStatus = self.GetStatus(i)
                                if curStatus == STATUS_QUEUED:
                                    self.SetStatus(i, STATUS_PENDING)      
                        
                        if playCmd == CMD_QUEUE_AND_CANCEL:
                            segNum = self.currentSegmentIndex
                            segment = self.jet_file.GetSegment(self.currentSegmentName)
                            wx.PostEvent(self, JetStatusEvent(CMD_QUEUE_AND_CANCEL, (segment, segNum)))
                              
                        #MAC has a 'pop' when clearing the queue; not sure why so this avoids it                              
                        if OsWindows():
                            self.jet.Clear_Queue()
                        else:
                            self.jet = self.SafeJetRestart(self.playerLock, self.jet, self.jet_file.config.filename)
                    
                    if playCmd == CMD_ORIGINALMUTES:
                        wx.PostEvent(self, JetStatusEvent(CMD_ORIGINALMUTES, segment.mute_flags))

                    if playCmd == CMD_UNMUTEALL:                       
                        wx.PostEvent(self, JetStatusEvent(CMD_UNMUTEALL, None))

                    if playCmd == CMD_PAUSE:
                        wx.PostEvent(self, JetStatusEvent(CMD_PAUSE, None))

                    if playCmd == CMD_MUTEALL:
                        wx.PostEvent(self, JetStatusEvent(CMD_MUTEALL, None))
                    
                    self.SetPlayCommand('')
                        
                if self.GetStatus(lastID) != STATUS_CANCELED:
                    self.SetStatus(lastID, STATUS_COMPLETE)      
                    
                wx.PostEvent(self, JetStatusEvent(LOAD_QUEUE_DISPLAY, None))
                wx.PostEvent(self, JetStatusEvent(CLR_INFO, None))
                
        SafeJetShutdown(self.playerLock, self.jet)   
        self.SetThreadShutdownFlag(True)
                
    def OnJetStatusUpdate(self, evt):
        """ All UI needed from within thread called via postevent otherwise mac crashes """
        if evt.mode == LOAD_QUEUE_DISPLAY:
            self.LoadQueueDisplay()
        elif evt.mode == GRAPH_POSITION_UPDATE:
            self.graph.UpdateLocation(evt.data)
        elif evt.mode == NEW_SEGMENT_DISPLAY:
            self.currentSegmentName = getColumnText(self.queueList, evt.data, 0)
            segment = self.jet_file.GetSegment(self.currentSegmentName)
            info = self.graph.LoadSegment(segment)
            self.trackList.DeleteAllItems()
            if info <> None:
                for track in info.trackList:
                    self.trackList.AddTrackRow(track)       
            self.trackList.CheckTracks(segment.mute_flags)
            self.log.SetValue(self.currentSegmentName)
        elif evt.mode == CMD_QUEUE_AND_CANCEL:
            self.QueueOneSegment(evt.data[0], evt.data[1])
        elif evt.mode == CMD_ORIGINALMUTES:
            self.trackList.CheckTracks(evt.data)
        elif evt.mode == CMD_UNMUTEALL:
            num = self.trackList.GetItemCount()
            for i in range(num):
                self.trackList.CheckItem(i, False)
        elif evt.mode == CMD_MUTEALL:
            num = self.trackList.GetItemCount()
            for i in range(num):
                self.trackList.CheckItem(i)
        elif evt.mode == CLR_INFO:
            self.log.SetValue("")
            self.graph.ClearGraph()
            self.graph.UpdateLocation(0)
        elif evt.mode == CMD_PLAY:
            self.btnPause.SetLabel(JetDefs.BUT_PAUSE)
        elif evt.mode == CMD_PAUSE or evt.mode == CMD_PLAY:
            if not self.paused:
                self.jet.Pause()
                self.paused = True
                self.btnPause.SetLabel(JetDefs.BUT_RESUME)
            else:
                self.jet.Play()
                self.paused = False
                self.btnPause.SetLabel(JetDefs.BUT_PAUSE)
            
    def SafeJetRestart(self, lock, jet, filename):
        """ Shuts down the jet engine """
        SafeJetShutdown(lock, jet)
        with lock:
            jet = JET()
            jet.eas.StartWave()
            jet.OpenFile(filename)
            return jet
                         
    def OnClose(self, event):
        """ When exiting the audition window, shut down jet play thread """
        i = 0
        while(not self.GetThreadShutdownFlag() and i < 5):
            #Make sure we shutdown the playing thread, but don't wait forever
            self.SetKeepPlayingFlag(False)
            logging.info("Waiting on shutdown %d" % (self.GetThreadShutdownFlag()))
            time.sleep(.5)
            i = i + 1      
              
        #make certain we clean up
        if self.jet is not None:
            SafeJetShutdown(self.playerLock, self.jet)                
        self.Destroy()
        
