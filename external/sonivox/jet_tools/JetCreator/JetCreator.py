"""
 File:  
 JetCreator.py
 
 Contents and purpose:
 Jet file creation utility for JET sound engine
 
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
import copy
import wx.html
import operator

from wx.lib.mixins.listctrl import CheckListCtrlMixin, ListCtrlAutoWidthMixin
from eas import *
from JetFile import *
from JetUtils import *
from JetCtrls import *
from JetDialogs import *
from JetSegGraph import SegmentGraph, Marker
from JetAudition import *
from JetStatusEvent import *

import img_favicon
import img_New

provider = wx.SimpleHelpProvider()
wx.HelpProvider_Set(provider)


class JetCreator(wx.Frame):
    """ Main window of JetCreator utility """
    def __init__(self, parent, id, jetConfigFile, importFlag=False):
        wx.Frame.__init__(self, parent, id, size=(1050, 720), style=wx.DEFAULT_FRAME_STYLE | wx.MINIMIZE_BOX | wx.MAXIMIZE_BOX)

        self.myicon = img_favicon.getIcon() 
        self.SetIcon(self.myicon) 
        self.UndoStack = []
        self.RedoStack = []
        self.queueSegs = []
        self.clipBoard = None
        self.jet = None
        self.playerLock = threading.RLock()
        self.SetKeepPlayingFlag(True)
        self.currentSegmentName = None
        self.currentSegmentIndex = None
        self.currentEventName = None
        self.currentEventIndex = None
        self.currentCtrl = ""
        self.currentJetConfigFile = jetConfigFile
        self.paused = False
        self.eventlistSort = (0, 1)
        self.seglistSort = (0, 1)
        if self.currentJetConfigFile == "":
            FileKillClean(JetDefs.UNTITLED_FILE)
            self.currentJetConfigFile = JetDefs.UNTITLED_FILE
        
        self.jet_file = JetFile(self.currentJetConfigFile, "")
        
        if not ValidateConfig(self.jet_file):
            FileKillClean(JetDefs.UNTITLED_FILE)
            self.currentJetConfigFile = JetDefs.UNTITLED_FILE
            self.jet_file = JetFile(self.currentJetConfigFile, "")

        if self.currentJetConfigFile == JetDefs.UNTITLED_FILE:
            self.LoadDefaultProperties()
            
        self.initLayout()
        self.initStatusBar()
        self.createMenuBar()
        self.createToolbar()
        self.SetCurrentFile(self.currentJetConfigFile)
        self.initHelp()
        
        self.graph.ClickCallbackFct = self.GraphTriggerClip
        
        EVT_JET_STATUS(self, self.OnJetStatusUpdate)
        
        wx.EVT_CLOSE(self, self.OnClose)

        self.Centre()
        self.Show(True)
        
        if importFlag:
            self.OnJetImportArchive(None)
     
        self.eventList.OnSortOrderChangedAlert = self.OnEventSortOrderChanged
        self.segList.OnSortOrderChangedAlert = self.OnSegSortOrderChanged
        
    def initLayout(self):
        """ Initializes the screen layout """
        panel = wx.Panel(self, -1)

        hboxMain = wx.BoxSizer(wx.HORIZONTAL)

        leftPanel = wx.Panel(panel, -1)
        leftTopPanel = wx.Panel(leftPanel, -1)
        leftBotPanel = wx.Panel(leftPanel, -1)
        rightPanel = wx.Panel(panel, -1)
        
        self.segList = JetCheckListCtrl(rightPanel)
        for title, width, fld in JetDefs.SEGMENT_GRID:
            self.segList.AddCol(title, width)

        self.eventList = JetListCtrl(rightPanel)
        for title, width, fld in JetDefs.CLIPS_GRID:
            self.eventList.AddCol(title, width)

        self.eventList.Bind(wx.EVT_LIST_ITEM_SELECTED, self.OnEventListClick)
        self.segList.Bind(wx.EVT_LIST_ITEM_SELECTED, self.OnSegListClick)
        self.segList.Bind(wx.EVT_LIST_ITEM_ACTIVATED, self.OnSegmentUpdate)
        self.eventList.Bind(wx.EVT_LIST_ITEM_ACTIVATED, self.OnEventUpdate)
        
        self.segList.BindCheckBox(self.OnSegmentChecked)
        
        BUT_SIZE = (95, 25)
        self.btnAddSeg = wx.Button(leftTopPanel, -1, JetDefs.BUT_ADD, size=BUT_SIZE)
        self.btnRevSeg = wx.Button(leftTopPanel, -1, JetDefs.BUT_REVISE, size=BUT_SIZE)
        self.btnDelSeg = wx.Button(leftTopPanel, -1, JetDefs.BUT_DELETE, size=BUT_SIZE)
        self.btnMoveSeg = wx.Button(leftTopPanel, -1, JetDefs.BUT_MOVE, size=BUT_SIZE)

        self.btnQueueAll = wx.Button(leftTopPanel, -1, JetDefs.BUT_QUEUEALL, size=BUT_SIZE)
        self.btnDequeueAll = wx.Button(leftTopPanel, -1, JetDefs.BUT_DEQUEUEALL, size=BUT_SIZE)
        self.btnPlay = wx.Button(leftTopPanel, -1, JetDefs.BUT_PLAY, size=BUT_SIZE)
        self.btnPause = wx.Button(leftTopPanel, -1, JetDefs.BUT_PAUSE, size=BUT_SIZE)
        self.btnAudition = wx.Button(leftTopPanel, -1, JetDefs.BUT_AUDITION, size=BUT_SIZE)
        
        self.btnAddEvt = wx.Button(leftBotPanel, -1, JetDefs.BUT_ADD, size=BUT_SIZE)
        self.btnRevEvt = wx.Button(leftBotPanel, -1, JetDefs.BUT_REVISE, size=BUT_SIZE)
        self.btnDelEvt = wx.Button(leftBotPanel, -1, JetDefs.BUT_DELETE, size=BUT_SIZE)
        self.btnMoveEvents = wx.Button(leftBotPanel, -1, JetDefs.BUT_MOVE, size=BUT_SIZE)
        
        self.Bind(wx.EVT_BUTTON, self.OnSegmentAdd, id=self.btnAddSeg.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnSegmentUpdate, id=self.btnRevSeg.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnSegmentDelete, id=self.btnDelSeg.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnSegmentsMove, id=self.btnMoveSeg.GetId())

        self.Bind(wx.EVT_BUTTON, self.OnSelectAll, id=self.btnQueueAll.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnDeselectAll, id=self.btnDequeueAll.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnPlay, id=self.btnPlay.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnPause, id=self.btnPause.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnAudition, id=self.btnAudition.GetId())
        
        self.Bind(wx.EVT_BUTTON, self.OnEventAdd, id=self.btnAddEvt.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnEventUpdate, id=self.btnRevEvt.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnEventDelete, id=self.btnDelEvt.GetId())
        self.Bind(wx.EVT_BUTTON, self.OnEventsMove, id=self.btnMoveEvents.GetId())
        
        BORDER = 5
        BUT_SPACE = 3
        vBoxLeftTop = wx.BoxSizer(wx.VERTICAL)
        vBoxLeftBot = wx.BoxSizer(wx.VERTICAL)

        vBoxLeftTop.Add(self.btnAddSeg, 0, wx.TOP, BORDER)
        vBoxLeftTop.Add(self.btnRevSeg, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add(self.btnDelSeg, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add(self.btnMoveSeg, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add((-1, 12))
        vBoxLeftTop.Add(self.btnQueueAll, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add(self.btnDequeueAll, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add(self.btnPlay, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add(self.btnPause, 0, wx.TOP, BUT_SPACE)
        vBoxLeftTop.Add(self.btnAudition, 0, wx.TOP, BUT_SPACE)

        vBoxLeftBot.Add(self.btnAddEvt, 0)
        vBoxLeftBot.Add(self.btnRevEvt, 0, wx.TOP, BUT_SPACE)
        vBoxLeftBot.Add(self.btnDelEvt, 0, wx.TOP, BUT_SPACE)
        vBoxLeftBot.Add(self.btnMoveEvents, 0, wx.TOP, BUT_SPACE)
        
        leftTopPanel.SetSizer(vBoxLeftTop)
        leftBotPanel.SetSizer(vBoxLeftBot)

        vboxLeft = wx.BoxSizer(wx.VERTICAL)
        vboxLeft.Add(leftTopPanel, 1, wx.EXPAND)
        vboxLeft.Add(leftBotPanel, 1, wx.EXPAND)
        vboxLeft.Add((-1, 25))
        
        leftPanel.SetSizer(vboxLeft)

        self.log = wx.TextCtrl(rightPanel, -1)
        self.graph = SegmentGraph(rightPanel, size=(-1, 50))
        
        vboxRight = wx.BoxSizer(wx.VERTICAL)
        vboxRight.Add(self.segList, 4, wx.EXPAND | wx.TOP, BORDER)
        vboxRight.Add((-1, 10))
        vboxRight.Add(self.eventList, 3, wx.EXPAND | wx.TOP, BORDER)
        vboxRight.Add((-1, 10))
        vboxRight.Add(self.log, 0, wx.EXPAND)
        vboxRight.Add((-1, 5))
        vboxRight.Add(self.graph, 1, wx.EXPAND)
        vboxRight.Add((-1, 10))

        rightPanel.SetSizer(vboxRight)

        hboxMain.Add(leftPanel, 0, wx.EXPAND | wx.RIGHT | wx.LEFT, BORDER)
        hboxMain.Add(rightPanel, 1, wx.EXPAND)
        hboxMain.Add((BORDER, -1))

        panel.SetSizer(hboxMain)

        pnlGraph = wx.Panel(leftBotPanel, -1)
        graphSizer1 = wx.BoxSizer(wx.VERTICAL)
        pnlGraph.SetSizer(graphSizer1)
        
        graphBox = wx.StaticBox(pnlGraph, wx.ID_ANY, label='Graph')
        graphSizer2 = wx.StaticBoxSizer(graphBox, wx.VERTICAL)

        self.chkGraphLabels = wx.CheckBox(pnlGraph, -1, JetDefs.GRAPH_LBLS)
        self.chkGraphClips = wx.CheckBox(pnlGraph, -1, JetDefs.GRAPH_TRIGGER)
        self.chkGraphAppEvts = wx.CheckBox(pnlGraph, -1, JetDefs.GRAPH_APP)
        
        graphSizer2.Add(self.chkGraphLabels, 0, wx.TOP, BUT_SPACE)
        graphSizer2.Add(self.chkGraphClips, 0, wx.TOP, BUT_SPACE)
        graphSizer2.Add(self.chkGraphAppEvts, 0, wx.TOP | wx.BOTTOM, BUT_SPACE)
        graphSizer1.Add((-1, 10))
        graphSizer1.Add(graphSizer2)

        vBoxLeftBot.Add(pnlGraph, 0, wx.TOP, BUT_SPACE)

        self.Bind(wx.EVT_CHECKBOX, self.OnSetGraphOptions, id=self.chkGraphLabels.GetId())
        self.Bind(wx.EVT_CHECKBOX, self.OnSetGraphOptions, id=self.chkGraphClips.GetId())
        self.Bind(wx.EVT_CHECKBOX, self.OnSetGraphOptions, id=self.chkGraphAppEvts.GetId())
        

    def initHelp(self):
        """ Initializes the help text for screen elements """
        self.SetHelpText(GetJetHelpText(JetDefs.MAIN_DLG_CTRLS, ''))
        self.segList.SetHelpText(GetJetHelpText(JetDefs.MAIN_DLG_CTRLS, JetDefs.MAIN_SEGLIST))
        self.eventList.SetHelpText(GetJetHelpText(JetDefs.MAIN_DLG_CTRLS, JetDefs.MAIN_EVENTLIST))
        self.graph.SetHelpText(GetJetHelpText(JetDefs.AUDITION_CTRLS, JetDefs.AUDITION_GRAPH))

    def initStatusBar(self):
        """ Initializes the status bar """
        self.statusbar = self.CreateStatusBar()

    def OnSelectAll(self, event):
        """ Called from select all button """
        num = self.segList.GetItemCount()
        for i in range(num-1, -1, -1):
            self.segList.CheckItem(i)

    def OnDeselectAll(self, event):
        """ Called from deselect all button """
        num = self.segList.GetItemCount()
        for i in range(num-1, -1, -1):
            self.segList.CheckItem(i, False)

    def LoadSegList(self):
        """ Loads up the list of segments """
        self.seglistSort = (IniGetValue(self.currentJetConfigFile, JetDefs.INI_SEGSORT, JetDefs.INI_SEGSORT_0, 'int', 0), IniGetValue(self.currentJetConfigFile, JetDefs.INI_SEGSORT, JetDefs.INI_SEGSORT_1, 'int', 1))
        segments = self.jet_file.GetSegments()
        if self.seglistSort[0] == 0:
            self.SegSort(segments, "segname")
        elif self.seglistSort[0] == 1:
            self.SegSort(segments, "filename")
        elif self.seglistSort[0] == 2:
            self.SegSort(segments, "dlsfile")
        elif self.seglistSort[0] == 3:
            self.SegSort(segments, "start")
        elif self.seglistSort[0] == 4:
            self.SegSort(segments, "end")
        elif self.seglistSort[0] == 5:
            self.SegSort(segments, "quantize")
        elif self.seglistSort[0] == 6:
            self.SegSort(segments, "transpose")
        elif self.seglistSort[0] == 7:
            self.SegSort(segments, "repeat")
        elif self.seglistSort[0] == 8:
            self.SegSort(segments, "mute_flags")
        listDataMap = []
        self.currentSegmentIndex = None
        self.currentSegmentName = None
        self.segList.DeleteAllItems()
        self.eventList.DeleteAllItems()
        self.menuItems[JetDefs.MNU_UPDATE_SEG].Enable(False)
        self.menuItems[JetDefs.MNU_DELETE_SEG].Enable(False)
        self.menuItems[JetDefs.MNU_MOVE_SEG].Enable(False)
        
        self.menuItems[JetDefs.MNU_ADD_EVENT].Enable(False)
        self.menuItems[JetDefs.MNU_MOVE_EVENT].Enable(False)
        self.menuItems[JetDefs.MNU_UPDATE_EVENT].Enable(False)
        self.menuItems[JetDefs.MNU_DELETE_EVENT].Enable(False)
        self.menuItems[JetDefs.MNU_MOVE_EVENT].Enable(False)
        for segment in self.jet_file.GetSegments():
            index = self.segList.InsertStringItem(sys.maxint, StrNoneChk(segment.segname))
            self.segList.SetStringItem(index, 1, FileJustName(StrNoneChk(segment.filename)))
            self.segList.SetStringItem(index, 2, FileJustName(StrNoneChk(segment.dlsfile)))
            self.segList.SetStringItem(index, 3, mbtFct(segment.start, 1))
            self.segList.SetStringItem(index, 4, mbtFct(segment.end, 1))
            self.segList.SetStringItem(index, 5, StrNoneChk(segment.quantize))
            self.segList.SetStringItem(index, 6, StrNoneChk(segment.transpose))
            self.segList.SetStringItem(index, 7, StrNoneChk(segment.repeat))
            self.segList.SetStringItem(index, 8, StrNoneChk(segment.mute_flags))
            
            self.segList.SetItemData(index, index)
            listDataMap.append((getColumnText(self.segList, index, 0).upper(), getColumnText(self.segList, index, 1).upper(), getColumnText(self.segList, index, 2).upper(), MbtVal(getColumnText(self.segList, index, 3)), MbtVal(getColumnText(self.segList, index, 4)), int(getColumnText(self.segList, index, 5)), int(getColumnText(self.segList, index, 6)), int(getColumnText(self.segList, index, 7)), int(getColumnText(self.segList, index, 8))))
            
        self.segList.itemDataMap = listDataMap
        self.segList.InitSorting(9)
        self.graph.ClearGraph()
            
    def LoadEventsForSeg(self, segName):
        """ Loads up the events associated with a segment """
        self.currentEventIndex = None
        self.eventList.DeleteAllItems()
        self.menuItems[JetDefs.MNU_UPDATE_EVENT].Enable(False)
        self.menuItems[JetDefs.MNU_DELETE_EVENT].Enable(False)
        self.menuItems[JetDefs.MNU_MOVE_EVENT].Enable(False)
        self.eventlistSort = (IniGetValue(self.currentJetConfigFile, JetDefs.INI_EVENTSORT, JetDefs.INI_EVENTSORT_0, 'int', 0), IniGetValue(self.currentJetConfigFile, JetDefs.INI_EVENTSORT, JetDefs.INI_EVENTSORT_1, 'int', 1))
        segment = self.jet_file.GetSegment(self.currentSegmentName)
        if segment is not None:
            if self.eventlistSort[0] == 0:
                self.EventSort(segment.jetevents, "event_name")
            elif self.eventlistSort[0] == 1:
                self.EventSort(segment.jetevents, "event_type")
            elif self.eventlistSort[0] == 2:
                self.EventSort(segment.jetevents, "event_start")
            elif self.eventlistSort[0] == 3:
                self.EventSort(segment.jetevents, "event_end")
            elif self.eventlistSort[0] == 4:
                self.EventSort(segment.jetevents, "track_num")
            elif self.eventlistSort[0] == 5:
                self.EventSort(segment.jetevents, "channel_num")
            elif self.eventlistSort[0] == 6:
                self.EventSort(segment.jetevents, "event_id")
            listDataMap = []
            for jet_event in self.jet_file.GetEvents(segName):
                index = self.eventList.InsertStringItem(sys.maxint, StrNoneChk(jet_event.event_name))
                self.eventList.SetStringItem(index, 1, StrNoneChk(jet_event.event_type))
                self.eventList.SetStringItem(index, 2, mbtFct(jet_event.event_start, 1))
                self.eventList.SetStringItem(index, 3, mbtFct(jet_event.event_end, 1))
                self.eventList.SetStringItem(index, 4, StrNoneChk(jet_event.track_num))
                self.eventList.SetStringItem(index, 5, StrNoneChk(jet_event.channel_num + 1))
                self.eventList.SetStringItem(index, 6, StrNoneChk(jet_event.event_id))
                
                self.eventList.SetItemData(index, index)
                listDataMap.append((getColumnText(self.eventList, index, 0).upper(), getColumnText(self.eventList, index, 1).upper(), MbtVal(getColumnText(self.eventList, index, 2)), MbtVal(getColumnText(self.eventList, index, 3)), int(getColumnText(self.eventList, index, 4)), int(getColumnText(self.eventList, index, 5)), int(getColumnText(self.eventList, index, 6))))
                    
            self.eventList.itemDataMap = listDataMap
            self.eventList.InitSorting(7)

    def OnEventListClick(self, event):
        """ Sets the current event variable when selecting from the list """
        self.currentCtrl = "eventList"
        self.currentEventIndex = event.m_itemIndex
        self.currentEventName = getColumnText(self.eventList, event.m_itemIndex, 0)       
        self.menuItems[JetDefs.MNU_UPDATE_EVENT].Enable(True)
        self.menuItems[JetDefs.MNU_DELETE_EVENT].Enable(True)
        self.menuItems[JetDefs.MNU_MOVE_EVENT].Enable(True)
               
    def OnSegmentChecked(self, index, checked):
        """ Selects the segment when checkbox clicked """
        ClearRowSelections(self.segList)
        SetRowSelection(self.segList, index, True)
        
    def SelectSegment(self, segName):
        """ Selects a segment by segment name """
        itm = self.segList.FindItem(-1, segName)
        self.segList.EnsureVisible(itm)
        ClearRowSelections(self.segList)
        SetRowSelection(self.segList, itm, True)
        
    def SelectEvent(self, eventName):
        """ Selects an event by event name """
        itm = self.eventList.FindItem(-1, eventName)
        self.eventList.EnsureVisible(itm)
        ClearRowSelections(self.eventList)
        SetRowSelection(self.eventList, itm, True)
        
    def OnSegListClick(self, event):
        """ Loads up a segment when the list is clicked """
        self.currentCtrl = "segList"
        self.currentSegmentIndex = event.m_itemIndex
        self.currentSegmentName = getColumnText(self.segList, event.m_itemIndex, 0)
        self.LoadEventsForSeg(getColumnText(self.segList, event.m_itemIndex, 0))
        self.menuItems[JetDefs.MNU_UPDATE_SEG].Enable(True)
        self.menuItems[JetDefs.MNU_DELETE_SEG].Enable(True)
        self.menuItems[JetDefs.MNU_MOVE_SEG].Enable(True)
        self.menuItems[JetDefs.MNU_ADD_EVENT].Enable(True)
        info = self.graph.LoadSegment(self.jet_file.GetSegment(self.currentSegmentName), showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
        if info == None:
            self.log.SetValue("")
        else:
            iLength = info.iLengthInMs
            if iLength > 0:
                self.log.SetValue("%s      %.2f Seconds" % (self.currentSegmentName, iLength / 1000.00))
            else:
                self.log.SetValue("%s" % (self.currentSegmentName))    
    
    def OnSegmentAdd(self, event):
        """ Calls the dialog box for adding a segment """
        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
        
        dlg = SegEdit(JetDefs.MAIN_ADDSEGTITLE, self.currentJetConfigFile)
       
        for filename in self.jet_file.GetMidiFiles():
            dlg.je.ctrls[JetDefs.F_MIDIFILE].Append(filename)
        for library in self.jet_file.GetLibraries():
            dlg.je.ctrls[JetDefs.F_DLSFILE].Append(library)
        
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            if len(dlg.lstReplicate) > 0:
                if dlg.chkReplaceMatching:
                    self.jet_file.DeleteSegmentsMatchingPrefix(dlg.replicatePrefix) 
                
                for replicate in dlg.lstReplicate:
                    self.jet_file.AddSegment(replicate[0], dlg.GetValue(JetDefs.F_MIDIFILE), 
                                             mbtFct(replicate[1],-1), mbtFct(replicate[2],-1), 
                                             JetDefs.MBT_ZEROSTR, 
                                             SegmentOutputFile(dlg.GetValue(JetDefs.F_SEGNAME), self.currentJetConfigFile), 
                                             dlg.GetValue(JetDefs.F_QUANTIZE),
                                             [], dlg.GetValue(JetDefs.F_DLSFILE),
                                             None,
                                             dlg.GetValue(JetDefs.F_TRANSPOSE),
                                             dlg.GetValue(JetDefs.F_REPEAT),
                                             dlg.GetValue(JetDefs.F_MUTEFLAGS))

                self.LoadSegList()
                self.SelectSegment(dlg.lstReplicate[0][0])
            else:
                self.jet_file.AddSegment(dlg.GetValue(JetDefs.F_SEGNAME), dlg.GetValue(JetDefs.F_MIDIFILE), 
                                     dlg.GetValue(JetDefs.F_START), dlg.GetValue(JetDefs.F_END), 
                                     JetDefs.MBT_ZEROSTR, 
                                     SegmentOutputFile(dlg.GetValue(JetDefs.F_SEGNAME), self.currentJetConfigFile), 
                                     dlg.GetValue(JetDefs.F_QUANTIZE),
                                     [], dlg.GetValue(JetDefs.F_DLSFILE),
                                     None,
                                     dlg.GetValue(JetDefs.F_TRANSPOSE),
                                     dlg.GetValue(JetDefs.F_REPEAT),
                                     dlg.GetValue(JetDefs.F_MUTEFLAGS))
                self.LoadSegList()
                self.SelectSegment(dlg.GetValue(JetDefs.F_SEGNAME))
            self.UndoAdd(saveState)
        dlg.Destroy()
        
    def OnSegmentUpdate(self, event):
        """ Calls the dialog box for updating a segment """
        if self.currentSegmentName is None:
            return
        
        segment = self.jet_file.GetSegment(self.currentSegmentName)
        if segment == None:
            return
        
        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
        
        dlg = SegEdit(JetDefs.MAIN_REVSEGTITLE, self.currentJetConfigFile)
       
        for filename in self.jet_file.GetMidiFiles():
            dlg.je.ctrls[JetDefs.F_MIDIFILE].Append(filename)
        for library in self.jet_file.GetLibraries():
            dlg.je.ctrls[JetDefs.F_DLSFILE].Append(library)
                    
        dlg.SetValue(JetDefs.F_SEGNAME, segment.segname)
        dlg.SetValue(JetDefs.F_MUTEFLAGS, segment.mute_flags)
        dlg.SetValue(JetDefs.F_MIDIFILE, segment.filename)
        dlg.SetValue(JetDefs.F_DLSFILE, segment.dlsfile)
        dlg.SetValue(JetDefs.F_START, segment.start)
        dlg.SetValue(JetDefs.F_END, segment.end)
        dlg.SetValue(JetDefs.F_QUANTIZE, segment.quantize)
        dlg.SetValue(JetDefs.F_TRANSPOSE, segment.transpose)
        dlg.SetValue(JetDefs.F_REPEAT, segment.repeat)
        dlg.jetevents = segment.jetevents
        
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            self.jet_file.UpdateSegment(self.currentSegmentName, dlg.GetValue(JetDefs.F_SEGNAME), 
                                     dlg.GetValue(JetDefs.F_MIDIFILE), 
                                     dlg.GetValue(JetDefs.F_START), dlg.GetValue(JetDefs.F_END), 
                                     JetDefs.MBT_ZEROSTR, #dlg.GetValue(JetDefs.F_LENGTH), 
                                     SegmentOutputFile(dlg.GetValue(JetDefs.F_SEGNAME), self.currentJetConfigFile), 
                                     dlg.GetValue(JetDefs.F_QUANTIZE),
                                     [], dlg.GetValue(JetDefs.F_DLSFILE),
                                     None,
                                     dlg.GetValue(JetDefs.F_TRANSPOSE),
                                     dlg.GetValue(JetDefs.F_REPEAT),
                                     dlg.GetValue(JetDefs.F_MUTEFLAGS))
            
            if len(dlg.lstReplicate) > 0:
                if dlg.chkReplaceMatching:
                    self.jet_file.DeleteSegmentsMatchingPrefix(dlg.replicatePrefix) 
                
                for replicate in dlg.lstReplicate:
                    self.jet_file.AddSegment(replicate[0], dlg.GetValue(JetDefs.F_MIDIFILE), 
                                             mbtFct(replicate[1],-1), mbtFct(replicate[2],-1), 
                                             JetDefs.MBT_ZEROSTR, 
                                             SegmentOutputFile(dlg.GetValue(JetDefs.F_SEGNAME), self.currentJetConfigFile), 
                                             dlg.GetValue(JetDefs.F_QUANTIZE),
                                             [], dlg.GetValue(JetDefs.F_DLSFILE),
                                             None,
                                             dlg.GetValue(JetDefs.F_TRANSPOSE),
                                             dlg.GetValue(JetDefs.F_REPEAT),
                                             dlg.GetValue(JetDefs.F_MUTEFLAGS))
    
                self.LoadSegList()
                self.SelectSegment(dlg.lstReplicate[0][0])
            else:          
                self.LoadSegList()
                self.SelectSegment(dlg.GetValue(JetDefs.F_SEGNAME))
            self.UndoAdd(saveState)
        dlg.Destroy()
        
    def OnSegmentDelete(self, event):
        """ Confirms the deletion segment(s) by user action """
        if self.currentSegmentName is None:
            return
        
        segment = self.jet_file.GetSegment(self.currentSegmentName)
        if segment == None:
            return
        
        count = 0
        deleteMsg = ''
        item = self.segList.GetFirstSelected()
        while item != -1:
            if count == 0:
                deleteMsg = getColumnText(self.segList,item,0)
            else:
                if count == 40:
                    deleteMsg = deleteMsg + "\n" + "....more"
                elif count < 40:
                    deleteMsg = deleteMsg + "\n" + getColumnText(self.segList,item,0)
            count = count + 1
            item = self.segList.GetNextSelected(item)
        
        if YesNo(JetDefs.MAIN_CONFIRM, deleteMsg + JetDefs.MAIN_CONFIRM_SEG_DLT, False):
            item = self.segList.GetFirstSelected()
            while item != -1:
                segName = getColumnText(self.segList,item,0)
                self.SegmentDelete(segName)
                item = self.segList.GetNextSelected(item)
            
            self.graph.ClearGraph()
            self.LoadSegList()
         
    def SegmentDelete(self, segName):
        """ Deletes a segment """
        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
        self.jet_file.DeleteSegment(segName)
        self.UndoAdd(saveState)
    
    def OnSegmentsMove(self, event):
        """ Move segment(s) """
        if self.currentSegmentName is None:
            return
        
        lstMoveItems = []
        count = 0
        item = self.segList.GetFirstSelected()
        while item != -1:
            max = GetMidiInfo(self.jet_file.GetSegment(getColumnText(self.segList,item,0)).filename).endMbtStr
            lstMoveItems.append((getColumnText(self.segList,item,0), mbtFct(getColumnText(self.segList,item,3),-1), mbtFct(getColumnText(self.segList,item,4),-1), max))
            count = count + 1
            item = self.segList.GetNextSelected(item)

        if count == 0:
            InfoMsg("Move", "Select one or more items to move.")
            return
        
        dlg = JetMove("Move Segments")
        dlg.lstMoveItems = lstMoveItems
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            if len(dlg.lstMoveMbt) > 0:
                saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
                
                for moveitem in dlg.lstMoveMbt:
                    self.jet_file.MoveSegment(moveitem[0], moveitem[1], moveitem[2])
                
                self.LoadSegList()
                self.UndoAdd(saveState)
                
        dlg.Destroy()
        
    def UndoAdd(self, saveState):
        """ Adds the current state to the undo stack """
        self.UndoStack.append(saveState)
        self.menuItems[JetDefs.MNU_UNDO].Enable(True)
       
    def RedoAdd(self, saveState):
        """ Adds the current state the the redo stack """
        self.RedoStack.append(saveState)
        self.menuItems[JetDefs.MNU_REDO].Enable(True)
       
    def OnRedo(self, event):
        """ Redo if there's one in the stack """
        if len(self.RedoStack) > 0:
            self.UndoAdd(JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex))
            state = self.RedoStack.pop()
            self.jet_file = copy.deepcopy(state.jet_file)
            self.LoadSegList()
            self.currentSegmentIndex = state.currentSegmentIndex
            if self.currentSegmentIndex != None:
                SetRowSelection(self.segList, self.currentSegmentIndex, True)
            if len(self.RedoStack) == 0:
                self.menuItems[JetDefs.MNU_REDO].Enable(False)
                
    def OnUndo(self, event):
        """ Undo if there's one in the stack """
        if len(self.UndoStack) > 0:
            self.RedoAdd(JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex))
            state = self.UndoStack.pop()
            self.jet_file = copy.deepcopy(state.jet_file)
            self.LoadSegList()
            self.currentSegmentIndex = state.currentSegmentIndex
            if self.currentSegmentIndex != None:
                SetRowSelection(self.segList, self.currentSegmentIndex, True)
            if len(self.UndoStack) == 0:
                self.menuItems[JetDefs.MNU_UNDO].Enable(False)

    def OnEventAdd(self, event):
        """ Calls a dialog box to add an event to the current segment """
        if self.currentSegmentName is None:
            return
        
        segment = self.jet_file.GetSegment(self.currentSegmentName)
        if segment == None:
            return
        
        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
        
        dlg = EventEdit(JetDefs.MAIN_ADDEVENTTITLE, self.currentJetConfigFile)
        editSegment = copy.deepcopy(segment)
        dlg.SetSegment(editSegment)
        dlg.SetEventId()
                      
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            if dlg.GetValue(JetDefs.F_ETYPE) == JetDefs.E_EOS:
                #check for an existing EOS event
                events = self.jet_file.GetEvents(self.currentSegmentName)
                for evt in events:
                    if evt.event_type == JetDefs.E_EOS:
                        self.jet_file.DeleteEvent(self.currentSegmentName, evt.event_name)
                dlg.SetValue(JetDefs.F_ESTART, dlg.GetValue(JetDefs.F_EEND))
            
            if len(dlg.lstReplicate) > 0:
                if dlg.chkReplaceMatching:
                    self.jet_file.DeleteEventsMatchingPrefix(self.currentSegmentName, dlg.replicatePrefix) 
                
                for replicate in dlg.lstReplicate:
                    self.jet_file.AddEvent(self.currentSegmentName, replicate[0],
                                         dlg.GetValue(JetDefs.F_ETYPE), 
                                         dlg.GetValue(JetDefs.F_EEVENTID), 
                                         dlg.GetValue(JetDefs.F_ETRACK), 
                                         dlg.GetValue(JetDefs.F_ECHANNEL), 
                                         mbtFct(replicate[1],-1),
                                         mbtFct(replicate[2],-1))
                self.SelectSegment(self.currentSegmentName)
                self.SelectEvent(dlg.lstReplicate[0][0])
            else:
                self.jet_file.AddEvent(self.currentSegmentName, dlg.GetValue(JetDefs.F_ENAME),
                                     dlg.GetValue(JetDefs.F_ETYPE), 
                                     dlg.GetValue(JetDefs.F_EEVENTID), 
                                     dlg.GetValue(JetDefs.F_ETRACK), 
                                     dlg.GetValue(JetDefs.F_ECHANNEL), 
                                     dlg.GetValue(JetDefs.F_ESTART),
                                     dlg.GetValue(JetDefs.F_EEND))
            
                self.SelectSegment(self.currentSegmentName)
                self.SelectEvent(dlg.GetValue(JetDefs.F_ENAME))
                
            self.UndoAdd(saveState)
        dlg.Destroy()
        
    def OnEventUpdate(self, event):
        """ Calls the dialog box to update the current event """
        if self.currentSegmentName is None:
            return

        if self.currentEventName  is None:
            return
        
        segment = self.jet_file.GetSegment(self.currentSegmentName)
        if segment == None:
            return
        
        curEvent = copy.deepcopy(self.jet_file.GetEvent(self.currentSegmentName, self.currentEventName))
        if curEvent == None:
            return
        
        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
        
        #only want the event we are editing to show up in graph
        editSegment = copy.deepcopy(segment)
        editSegment.jetevents = []
        editSegment.jetevents.append(curEvent)
        
        dlg = EventEdit(JetDefs.MAIN_REVEVENTTITLE, self.currentJetConfigFile)
        dlg.SetSegment(editSegment)
        dlg.SetValue(JetDefs.F_ENAME, curEvent.event_name)
        dlg.SetValue(JetDefs.F_ETYPE, curEvent.event_type)
        dlg.SetValue(JetDefs.F_ESTART, curEvent.event_start)
        dlg.SetValue(JetDefs.F_EEND, curEvent.event_end)
        dlg.SetValue(JetDefs.F_ETRACK, curEvent.track_num)
        dlg.SetValue(JetDefs.F_ECHANNEL, curEvent.channel_num)
        dlg.SetValue(JetDefs.F_EEVENTID, curEvent.event_id)
        dlg.OnEventSelect()   
        
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            if dlg.GetValue(JetDefs.F_ETYPE) == JetDefs.E_EOS:
                dlg.SetValue(JetDefs.F_ESTART, dlg.GetValue(JetDefs.F_EEND))
                
            self.jet_file.UpdateEvent(self.currentSegmentName, 
                                      self.currentEventName, 
                                     dlg.GetValue(JetDefs.F_ENAME),
                                     dlg.GetValue(JetDefs.F_ETYPE), 
                                     dlg.GetValue(JetDefs.F_EEVENTID), 
                                     dlg.GetValue(JetDefs.F_ETRACK), 
                                     dlg.GetValue(JetDefs.F_ECHANNEL), 
                                     dlg.GetValue(JetDefs.F_ESTART),
                                     dlg.GetValue(JetDefs.F_EEND))

            if len(dlg.lstReplicate) > 0:
                if dlg.chkReplaceMatching:
                    self.jet_file.DeleteEventsMatchingPrefix(self.currentSegmentName, dlg.replicatePrefix) 
                
                for replicate in dlg.lstReplicate:
                    self.jet_file.AddEvent(self.currentSegmentName, replicate[0],
                                         dlg.GetValue(JetDefs.F_ETYPE), 
                                         dlg.GetValue(JetDefs.F_EEVENTID), 
                                         dlg.GetValue(JetDefs.F_ETRACK), 
                                         dlg.GetValue(JetDefs.F_ECHANNEL), 
                                         mbtFct(replicate[1],-1),
                                         mbtFct(replicate[2],-1))
                self.SelectSegment(self.currentSegmentName)
                self.SelectEvent(dlg.lstReplicate[0][0])
            else:            
                self.SelectSegment(self.currentSegmentName)
                self.SelectEvent(dlg.GetValue(JetDefs.F_ENAME))
            self.UndoAdd(saveState)
        dlg.Destroy()
        
    def OnEventDelete(self, event):
        """ Confirms the deletion of event(s) """
        if self.currentSegmentName is None:
            return
        
        if self.currentEventName  is None:
            return
        
        curEvent = self.jet_file.GetEvent(self.currentSegmentName, self.currentEventName)
        if curEvent == None:
            return

        count = 0
        deleteMsg = ''
        item = self.eventList.GetFirstSelected()
        while item != -1:
            if count == 0:
                deleteMsg = getColumnText(self.eventList,item,0)
            else:
                if count == 40:
                    deleteMsg = deleteMsg + "\n" + "....more"
                elif count < 40:
                    deleteMsg = deleteMsg + "\n" + getColumnText(self.eventList,item,0)
            count = count + 1
            item = self.eventList.GetNextSelected(item)
        

        if YesNo(JetDefs.MAIN_CONFIRM, deleteMsg + JetDefs.MAIN_CONRIRM_EVT_DLT, False):
            item = self.eventList.GetFirstSelected()
            while item != -1:
                eventName = getColumnText(self.eventList,item,0)
                self.EventDelete(eventName)
                item = self.eventList.GetNextSelected(item)

            self.SelectSegment(self.currentSegmentName)
            self.LoadEventsForSeg(self.currentSegmentName)
          
    def EventDelete(self, eventName):
        """ Deletes an event """
        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
        self.jet_file.DeleteEvent(self.currentSegmentName, eventName)
        self.UndoAdd(saveState)

    def OnEventsMove(self, event):
        """ Move event(s) """
        if self.currentSegmentName is None:
            return
        
        if self.currentEventName  is None:
            return
        
        segment = self.jet_file.GetSegment(self.currentSegmentName)
        if segment == None:
            return
        
        curEvent = self.jet_file.GetEvent(self.currentSegmentName, self.currentEventName)
        if curEvent == None:
            return

        lstMoveItems = []
        count = 0
        item = self.eventList.GetFirstSelected()
        while item != -1:
            lstMoveItems.append((getColumnText(self.eventList,item,0), mbtFct(getColumnText(self.eventList,item,2),-1), mbtFct(getColumnText(self.eventList,item,3),-1), segment.end))
            count = count + 1
            item = self.eventList.GetNextSelected(item)

        if count == 0:
            InfoMsg("Move", "Select one or more items to move.")
            return
        
        dlg = JetMove("Move Events")
        dlg.lstMoveItems = lstMoveItems
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            if len(dlg.lstMoveMbt) > 0:
                saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
                
                for moveitem in dlg.lstMoveMbt:
                    self.jet_file.MoveEvent(self.currentSegmentName, moveitem[0], moveitem[1], moveitem[2])
                
                self.SelectSegment(self.currentSegmentName)
                self.LoadEventsForSeg(self.currentSegmentName)
                
                self.UndoAdd(saveState)
                
        dlg.Destroy()
        
    def OnJetOpen(self, event):
        """ Calls a dialog box to get a jet config file to open """
        dlg = JetOpen()
        result = dlg.ShowModal()
        if result == JetDefs.ID_JET_OPEN:
            self.jet_file = JetFile(dlg.fileName , "")
            if not ValidateConfig(self.jet_file):
                FileKillClean(JetDefs.UNTITLED_FILE)
                self.currentJetConfigFile = JetDefs.UNTITLED_FILE
                self.jet_file = JetFile(self.currentJetConfigFile, "")
            else:
                self.SetCurrentFile(dlg.fileName)
        elif result == JetDefs.ID_JET_NEW:
            self.jet_file = JetFile("" , "")
            self.SetCurrentFile(JetDefs.UNTITLED_FILE)
            self.LoadDefaultProperties()
        elif result == JetDefs.ID_JET_IMPORT:
            self.OnJetImportArchive(event)
        dlg.Destroy()

    def OnJetSaveAs(self, event): 
        """ Calls a dialog box to allow saving the current jet file as another name """
        defDir = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.JTC_FILE_SPEC, 'str', str(os.getcwd()))
        dialog = wx.FileDialog(None, JetDefs.SAVE_PROMPT, defDir, "", JetDefs.JTC_FILE_SPEC, wx.SAVE | wx.OVERWRITE_PROMPT )
        if dialog.ShowModal() == wx.ID_OK:
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.JTC_FILE_SPEC, str(FileJustPath(dialog.GetPath())))
            self.currentJetConfigFile = FileJustRoot(dialog.GetPath()) + ".jtc"            
            self.jet_file.config.filename = FileJustRoot(self.currentJetConfigFile)  + ".jet"
            self.jet_file.SaveJetConfig(self.currentJetConfigFile)
            self.jet_file.WriteJetFileFromConfig(self.currentJetConfigFile)
            self.SetCurrentFile(self.currentJetConfigFile)
        dialog.Destroy()
    
    def OnJetSave(self, event):
        """ Saves the current jet file to disk """
        if self.currentJetConfigFile == JetDefs.UNTITLED_FILE:
            self.OnJetSaveAs(event)
        else:
            self.jet_file.SaveJetConfig(self.currentJetConfigFile)
            self.jet_file.WriteJetFileFromConfig(self.currentJetConfigFile)
        
    def OnJetNew(self, event):
        """ Initializes the state to a new jet file """
        self.jet_file = JetFile("" , "")
        self.SetCurrentFile(JetDefs.UNTITLED_FILE)
        self.LoadDefaultProperties()
                
    def SetCurrentFile(self, fileName):
        """ Sets the state for the current jet file """
        self.clipBoard = None
        self.currentJetConfigFile = fileName
        self.SetTitle(JetDefs.MAIN_TITLEPREFIX + FileJustName(fileName))
        AppendRecentJetFile(fileName)
        self.LoadSegList()
        self.graph.ClearGraph()
        self.chkGraphLabels.SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'))
        self.chkGraphClips.SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'))
        self.chkGraphAppEvts.SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))

    def createMenuBar(self):
        """ Creates a menu bar """
        self.menuItems = {}
        menuBar = wx.MenuBar()
        for eachMenuData in JetDefs.MENU_SPEC:
            menuLabel = eachMenuData[0]
            menuItems = eachMenuData[1:]
            menuBar.Append(self.createMenu(menuItems), menuLabel)
        self.SetMenuBar(menuBar)

    def createMenu(self, menuData):
        """ Creates a menu from the structure menuData in JetDefs """
        menu = wx.Menu()
        for eachLabel, eachStatus, eachHandler, eachEnable in menuData:
            if not eachLabel:
                menu.AppendSeparator()
                continue
            self.menuItems[eachLabel] = menu.Append(-1, eachLabel, eachStatus)
            self.menuItems[eachLabel].Enable(eachEnable)
            try:
                self.Bind(wx.EVT_MENU, getattr(self, eachHandler) , self.menuItems[eachLabel])
            except:
                print("def " + eachHandler + "(self, event): pass")  
        return menu

    def createToolbar(self):
        """ Creates the toolbar """
        toolbar = self.CreateToolBar()
        toolbar.SetToolBitmapSize((32,32))
        self.toolItems = {}
        for eachTool in JetDefs.TOOLBAR_SPEC:
            if eachTool[0] == '-':
                toolbar.AddSeparator()
            else:
                b = __import__(eachTool[1])
                bitMap = b.getBitmap()
                self.toolItems[eachTool[0]] = toolbar.AddLabelTool(-1, label=eachTool[0], 
                        bitmap=bitMap, 
                                     shortHelp=eachTool[0], longHelp=eachTool[2])
                self.Bind(wx.EVT_TOOL, getattr(self, eachTool[3]) , self.toolItems[eachTool[0]])
        toolbar.Realize()
        
    def OnAudition(self, event):
        """ Calls the audition window for simple preview of jet file """
        jet_file = CreateTempJetFile(self.jet_file)

        w, h = self.GetSize()
        w = w - 50
        if w < 900:
            w = 900
        h = h - 50
        if h < 650:
            h = 650
        dlg = Audition(jet_file, (w,h))
        dlg.ShowModal()   
        CleanupTempJetFile(jet_file)
        
    def SetKeepPlayingFlag(self, val):
        """ Sets a flag to communicate playing state to the play thread """
        with self.playerLock:
            self.keepPlaying = val

    def GetKeepPlayingFlag(self):   
        """ Gets the playing state flag """   
        with self.playerLock:
            return self.keepPlaying
        
    def GraphTriggerClip(self, sClipName, iEventId):
        """ Triggers a clip when they click on the graph """
        with self.playerLock:
            try:
                self.jet.TriggerClip(iEventId)
                self.log.SetValue(JetDefs.PLAY_TRIGGERCLIP_MSG % (iEventId, sClipName))
                return True
            except:
                return False       
        
    def OnHelpJet(self, event):
        """ Loads the jet help file """
        import webbrowser
        webbrowser.open(JetDefs.MAIN_HELPFILE)
        return

    def OnHelpJetGuidelines(self, event):
        """ Loads the authoring guidelines file """
        import webbrowser
        webbrowser.open(JetDefs.MAIN_HELPGUIDELINESFILE)
        return
            
    def OnAbout(self, event): 
        """ Loads the about dialog box """
        dlg = JetAbout()
        result = dlg.ShowModal()
        dlg.Destroy()

    def OnJetImportArchive(self, event):
        """ Imports a jet archive file """
        defDir = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.ARCHIVE_FILE_SPEC, 'str', str(os.getcwd()))
        dialog = wx.FileDialog(None, JetDefs.IMPORT_ARCHIVE_PROMPT, defDir, "", JetDefs.ARCHIVE_FILE_SPEC, wx.OPEN)
        if dialog.ShowModal() == wx.ID_OK:
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.ARCHIVE_FILE_SPEC, str(FileJustPath(dialog.GetPath())))
            defDir = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.ARCHIVE_FILE_SPEC + "Dir", 'str', str(os.getcwd()))
            dlg1 = wx.DirDialog(self, JetDefs.IMPORT_ARCHIVEDIR_PROMPT, style=wx.DD_DEFAULT_STYLE, defaultPath=defDir)
            if dlg1.ShowModal() == wx.ID_OK:
                IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.ARCHIVE_FILE_SPEC + "Dir", str(FileJustPath(dlg1.GetPath())))
                if YesNo(JetDefs.MAIN_IMPORTTITLE, JetDefs.MAIN_IMPORTMSG % (dialog.GetPath(),dlg1.GetPath()), False):
                    projectPath = dlg1.GetPath()
                    zipFile = dialog.GetPath()
                    z = __import__('zipfile')
                    
                    if not z.is_zipfile(zipFile):
                        wx.MessageBox(JetDefs.IMPORT_ARCHIVE_NO_JTC)
                    else:
                        zip = z.ZipFile(zipFile, 'r')
                        
                        jtcFile = ""
                        fileList = zip.namelist()
                        
                        isArchive = False
                        for myFile in fileList:
                            if myFile == 'JetArchive':
                                isArchive = True
                                break
                        if not isArchive:
                            wx.MessageBox(JetDefs.IMPORT_NOT_JET_ARCHIVE)
                        else:
                            for myFile in fileList:
                                if FileJustExt(myFile) == '.JTC':
                                    jtcFile = myFile
                                    break
                            if jtcFile == "":
                                wx.MessageBox(JetDefs.IMPORT_ARCHIVE_NO_JTC)
                            else:
                                for name in zip.namelist(): 
                                    ext = FileJustExt(name)
                                    if ext == '.MID' or ext == '.DLS'  or ext == '.JET':
                                        file(FileFixPath(projectPath + "/" + name), 'wb').write(zip.read(name))
                                    else:
                                        if len(ext) > 0 and ext != '.DS_STORE':
                                            file(FileFixPath(projectPath + "/" + name), 'w').write(zip.read(name))
                            zip.close()
                            self.currentJetConfigFile = FileFixPath(projectPath + "/") + jtcFile
                            self.jet_file = JetFile(self.currentJetConfigFile, "")
                            
                            #fix paths
                            self.jet_file.config.filename = FileJustRoot(self.currentJetConfigFile) + ".JET"
                            for index, segment in enumerate(self.jet_file.segments):
                                self.jet_file.segments[index].filename = FileFixPath(projectPath + "/" + segment.filename)
                                if segment.dlsfile > "":
                                    self.jet_file.segments[index].dlsfile = FileFixPath(projectPath + "/" + segment.dlsfile)
                                self.jet_file.segments[index].output = FileFixPath(projectPath + "/" + segment.output)                        
                            
                            for index, library in enumerate(self.jet_file.libraries):
                                self.jet_file.libraries[index] = FileFixPath(projectPath + "/" + library)
                                
                            if ValidateConfig(self.jet_file):
                                self.jet_file.SaveJetConfig(self.currentJetConfigFile)
                                self.jet_file.WriteJetFileFromConfig(self.currentJetConfigFile)
                                self.jet_file = JetFile(self.currentJetConfigFile , "")
                                self.SetCurrentFile(self.currentJetConfigFile)

            dlg1.Destroy()
        dialog.Destroy()        
       
    def OnJetExportArchive(self, event):
        """ Exports the current setup to an archive file """
        self.OnJetSave(event)
        defDir = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.ARCHIVE_FILE_SPEC, 'str', str(os.getcwd()))
        dialog = wx.FileDialog(None, JetDefs.EXPORT_ARCHIVE_PROMPT, defDir, "", JetDefs.ARCHIVE_FILE_SPEC, wx.SAVE | wx.OVERWRITE_PROMPT )
        if dialog.ShowModal() == wx.ID_OK:
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, JetDefs.ARCHIVE_FILE_SPEC, str(FileJustPath(dialog.GetPath())))
            ExportJetArchive(FileJustRoot(dialog.GetPath()) + ".zip", self.currentJetConfigFile, self.jet_file)
        dialog.Destroy()
        
    def OnCopy(self, event):
        """ Copies the current segment or event to the clipboard """
        if self.currentCtrl == JetDefs.MAIN_SEGLIST:
            if self.currentSegmentName is None:
                return ""
            
            segment = self.jet_file.GetSegment(self.currentSegmentName)
            if segment == None:
                return ""
            self.clipBoard = JetCutCopy(self.currentCtrl, segment, self.currentSegmentName)
            return self.currentCtrl
        elif self.currentCtrl == JetDefs.MAIN_EVENTLIST:
            if self.currentSegmentName is None:
                return ""
    
            if self.currentEventName  is None:
                return ""
            
            segment = self.jet_file.GetSegment(self.currentSegmentName)
            if segment == None:
                return ""
            
            curEvent = self.jet_file.GetEvent(self.currentSegmentName, self.currentEventName)
            if curEvent == None:
                return ""
            self.clipBoard = JetCutCopy(self.currentCtrl, curEvent, self.currentSegmentName)
            return self.currentCtrl
            
    def OnCut(self, event):
        """ Cuts the current segment or event to the clipboard """
        retCopy = self.OnCopy(event)
        if retCopy == JetDefs.MAIN_SEGLIST:
            self.SegmentDelete(self.currentSegmentName) 
            self.LoadSegList()
        elif retCopy == JetDefs.MAIN_EVENTLIST:
            self.EventDelete(self.currentEventName)
            self.LoadEventsForSeg(self.currentSegmentName)
        
    def OnPaste(self, event):
        """ Pastes the current segment or event from the clipboard """
        if self.clipBoard is not None:
            if self.currentCtrl == JetDefs.MAIN_SEGLIST and self.clipBoard.objType == JetDefs.MAIN_SEGLIST:
                saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
                self.jet_file.segments.append(self.clipBoard.GetObj(self.jet_file.segments))
                self.UndoAdd(saveState)
                self.LoadSegList()
            elif self.currentCtrl == JetDefs.MAIN_EVENTLIST and self.clipBoard.objType == JetDefs.MAIN_EVENTLIST and self.clipBoard.currentSegmentName == self.currentSegmentName:
                for segment in self.jet_file.segments:
                    if segment.segname == self.currentSegmentName:
                        saveState = JetState(self.jet_file, self.currentSegmentIndex, self.currentEventIndex)
                        segment.jetevents.append(self.clipBoard.GetObj(segment.jetevents))
                        self.UndoAdd(saveState)
                        self.LoadEventsForSeg(self.currentSegmentName)
                        self.graph.LoadSegment(self.jet_file.GetSegment(self.currentSegmentName), showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
                        
    def OnJetProperties(self, event):
        """ Opens a dialog box for jetfile properties """
        dlg = JetPropertiesDialog()
        dlg.SetValue(JetDefs.F_JETFILENAME, self.jet_file.config.filename)
        dlg.SetValue(JetDefs.F_COPYRIGHT, StrNoneChk(self.jet_file.config.copyright))
        dlg.SetValue(JetDefs.F_CHASECONTROLLERS, StrNoneChk(self.jet_file.config.chase_controllers))
        dlg.SetValue(JetDefs.F_DELETEEMPTYTRACKS, StrNoneChk(self.jet_file.config.delete_empty_tracks))
        
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            self.jet_file.config.filename = dlg.je.ctrls[JetDefs.F_JETFILENAME].GetValue()
            self.jet_file.config.copyright = dlg.je.ctrls[JetDefs.F_COPYRIGHT].GetValue()
            self.jet_file.config.chase_controllers = dlg.je.ctrls[JetDefs.F_CHASECONTROLLERS].GetValue()
            self.jet_file.config.delete_empty_tracks = dlg.je.ctrls[JetDefs.F_DELETEEMPTYTRACKS].GetValue()
        dlg.Destroy()
       
    def OnPreferences(self, event):
        """ Opens a dialog box to capture preferences and saves them to a configuration file """
        dlg = JetPreferences()
        dlg.SetValue(JetDefs.F_COPYRIGHT, IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_COPYRIGHT))
        dlg.SetValue(JetDefs.F_CHASECONTROLLERS, IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_CHASECONTROLLERS, 'bool', True))
        dlg.SetValue(JetDefs.F_DELETEEMPTYTRACKS, IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_DELETEEMPTYTRACKS, 'bool', False))
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_COPYRIGHT, dlg.GetValue(JetDefs.F_COPYRIGHT))
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_CHASECONTROLLERS, dlg.GetValue(JetDefs.F_CHASECONTROLLERS))
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_DELETEEMPTYTRACKS, dlg.GetValue(JetDefs.F_DELETEEMPTYTRACKS))
        dlg.Destroy()

    def LoadDefaultProperties(self):
        """ Loads default properties from the a configuration file """
        self.jet_file.config.copyright = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_COPYRIGHT)
        self.jet_file.config.chase_controllers = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_CHASECONTROLLERS, 'bool', True)
        self.jet_file.config.delete_empty_tracks = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_PREF_SECTION, JetDefs.F_DELETEEMPTYTRACKS, 'bool', False)               
                
    def OnClose(self, event):
        """ Called upon closing the JetCreator main window """
        if self.isDirty():
            ret = YesNoCancel(JetDefs.MAIN_JETCREATOR, JetDefs.MAIN_SAVEBEFOREEXIT, True)
            if ret == wx.ID_YES:
                self.OnJetSave(None)
            if ret == wx.ID_CANCEL:
                return
            
        if self.jet is not None:
            SafeJetShutdown(self.playerLock, self.jet)                
        self.Destroy()
        
    def OnPlay(self, event):
        """ Plays the currently queued segments """
        if self.btnPlay.GetLabel() == JetDefs.BUT_PLAY:
            if not ValidateConfig(self.jet_file):
                return
            
            #make sure something is queued
            iQueued = False
            num = self.segList.GetItemCount()
            for seg_num in range(num):
                if self.segList.IsChecked(seg_num):
                    iQueued = True
            if not iQueued:
                InfoMsg(JetDefs.MAIN_PLAYSEG, JetDefs.MAIN_PLAYSEGMSG)
                return
        
            for segment in self.jet_file.segments:
                if FileExists(segment.dlsfile):
                    if not segment.dlsfile in self.jet_file.libraries:
                        self.jet_file.libraries.append(segment.dlsfile)
                    
            self.eventList.DeleteAllItems()
            num = self.segList.GetItemCount()
            for seg_num in range(num):
                if seg_num == 0: self.log.Clear()
                if self.segList.IsChecked(seg_num):
                    segment = self.jet_file.GetSegment(getColumnText(self.segList, seg_num, 0))
                    if segment != None:
                        #so we can determine which segment is playing, make these the same
                        userID = seg_num
                        if FileExists(segment.dlsfile):
                            dls_num = FindDlsNum(self.jet_file.libraries, segment.dlsfile)
                        else:
                            dls_num = -1
                        self.queueSegs.append(QueueSeg(segment.segname, userID, seg_num, dls_num, segment.repeat, segment.transpose, segment.mute_flags))
                    
            if len(self.queueSegs) == 0:
                return
            
            self.btnPlay.SetLabel(JetDefs.BUT_STOP)
            
            thread.start_new_thread(self.PlaySegs, ())
        else:
            with self.playerLock:
                self.jet.Clear_Queue()
            self.SetKeepPlayingFlag(False)
    
    def PlaySegs(self):
        """ Thread writes a temporary copy of the jet file, and calls the library to play it """
        if len(self.queueSegs) == 0:
            return
        
        jet_file = CreateTempJetFile(self.jet_file)
        
        if not ValidateConfig(jet_file):
            CleanupTempJetFile(jet_file)
            return
             
        self.jet = JET()
        self.jet.eas.StartWave()
        self.jet.OpenFile(jet_file.config.filename)

        lastID = -1

        # queue first segment and start playback
        Queue(self.jet, self.queueSegs[0])
        index = 1
        self.jet.Play()
        self.paused = False
    
        # continue playing until all segments are done
        self.SetKeepPlayingFlag(True)
        while self.GetKeepPlayingFlag():
            self.jet.Render()
            status = self.jet.Status()
            
            if status.currentUserID <> lastID and status.currentUserID <> -1:
                wx.PostEvent(self, JetStatusEvent(JetDefs.PST_PLAY, status.currentUserID))
                lastID = status.currentUserID

            # if no more segments - we're done
            if status.numQueuedSegments == 0:
                break
    
            self.jet.GetAppEvent()
    
            # if less than 2 segs queued - queue another one            
            if (index < len(self.queueSegs)) and (status.numQueuedSegments < 2):
                Queue(self.jet, self.queueSegs[index])
                index += 1
                
            wx.PostEvent(self, JetStatusEvent(JetDefs.PST_UPD_LOCATION, status.location))
        
        
        SafeJetShutdown(self.playerLock, self.jet)   
        
        self.queueSegs = []
        
        CleanupTempJetFile(jet_file)
        
        wx.PostEvent(self, JetStatusEvent(JetDefs.PST_DONE, None))
                
    def OnJetStatusUpdate(self, evt):
        """ These are screen updates called for from within the thread.  Communication 
            is via a postevent call.
        """
        if evt.mode == JetDefs.PST_PLAY:
            segName = getColumnText(self.segList, evt.data, 0)
            self.LoadEventsForSeg(segName)
            self.log.SetValue(segName)
            ClearRowSelections(self.segList)
            SetRowSelection(self.segList, evt.data, True)
        elif evt.mode == JetDefs.PST_UPD_LOCATION:
            self.graph.UpdateLocation(evt.data)
        elif evt.mode == 3:
            self.graph.UpdateLocation(0)
            ClearRowSelections(self.segList)
            self.eventList.DeleteAllItems()
            self.SetKeepPlayingFlag(False)
            self.btnPlay.SetLabel(JetDefs.BUT_PLAY)
            self.btnPause.SetLabel(JetDefs.BUT_PAUSE)            
            
    def OnPause(self, evt):
        """ Pauses the playback """
        if self.jet is None:
            return
        if not self.paused:
            self.jet.Pause()
            self.paused = True
            self.btnPause.SetLabel(JetDefs.BUT_RESUME)
        else:
            self.jet.Play()
            self.paused = False
            self.btnPause.SetLabel(JetDefs.BUT_PAUSE)
           
    def isDirty(self):
        if len(self.UndoStack) == 0 and len(self.RedoStack) == 0:
            return False
        else:
            return True
        
    def OnSetGraphOptions(self, evt):
        """ Sets graph options """
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, self.chkGraphLabels.GetValue())
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, self.chkGraphClips.GetValue())
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, self.chkGraphAppEvts.GetValue())
        self.graph.LoadSegment(self.jet_file.GetSegment(self.currentSegmentName), showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))

    def OnEventSortOrderChanged(self):
        """ Called when sort order has changed """
        IniSetValue(self.currentJetConfigFile, JetDefs.INI_EVENTSORT, JetDefs.INI_EVENTSORT_0, self.eventList.GetSortState()[0])
        IniSetValue(self.currentJetConfigFile, JetDefs.INI_EVENTSORT, JetDefs.INI_EVENTSORT_1, self.eventList.GetSortState()[1])
        self.LoadEventsForSeg(self.currentSegmentName)
        self.graph.LoadSegment(self.jet_file.GetSegment(self.currentSegmentName), showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))

    def EventSortCmp(self, a, b):
        """ Sorts based on selected sort order """
        if self.eventlistSort[0] == 0 or self.eventlistSort[0] == 1:
            if self.eventlistSort[1] == 1:
                return cmp(a[0].upper(), b[0].upper())
            else:
                return cmp(b[0].upper(), a[0].upper())                
        elif self.eventlistSort[0] == 2 or self.eventlistSort[0] == 3: 
            if self.eventlistSort[1] == 1:
                return cmp(MbtVal(a[0]), MbtVal(b[0]))
            else:
                return cmp(MbtVal(b[0]), MbtVal(a[0]))   
        else:
            return cmp(a[0], b[0])             

    def EventSort2(self, seq, attr):
        """ Does Sorting """
        intermed = map(None, map(getattr, seq, (attr,)*len(seq)), xrange(len(seq)), seq)
        intermed.sort(self.EventSortCmp)
        return map(operator.getitem, intermed, (-1,) * len(intermed))

    def EventSort(self, lst, attr):
        """ Does Sorting """
        lst[:] = self.EventSort2(lst, attr)
        
    def OnSegSortOrderChanged(self):
        """ Called when sort order has changed """
        IniSetValue(self.currentJetConfigFile, JetDefs.INI_SEGSORT, JetDefs.INI_SEGSORT_0, self.segList.GetSortState()[0])
        IniSetValue(self.currentJetConfigFile, JetDefs.INI_SEGSORT, JetDefs.INI_SEGSORT_1, self.segList.GetSortState()[1])
        self.LoadEventsForSeg(self.currentSegmentName)
        self.graph.LoadSegment(self.jet_file.GetSegment(self.currentSegmentName), showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))

    def SegSortCmp(self, a, b):
        """ Sorts based on selected sort order """
        if self.seglistSort[0] == 0:
            if self.seglistSort[1] == 1:
                return cmp(a[0].upper(), b[0].upper())
            else:
                return cmp(b[0].upper(), a[0].upper())   
        elif self.seglistSort[0] == 1 or self.seglistSort[0] == 2:   
            if self.seglistSort[1] == 1:
                return cmp(FileJustName(a[0]).upper(), FileJustName(b[0]).upper())
            else:
                return cmp(FileJustName(b[0]).upper(), FileJustName(a[0]).upper())   
        elif self.seglistSort[0] == 3 or self.seglistSort[0] == 4: 
            if self.seglistSort[1] == 1:
                return cmp(MbtVal(a[0]), MbtVal(b[0]))
            else:
                return cmp(MbtVal(b[0]), MbtVal(a[0]))   
        else:
            return cmp(a[0], b[0])             

    def SegSort2(self, seq, attr):
        """ Does Sorting """
        intermed = map(None, map(getattr, seq, (attr,)*len(seq)), xrange(len(seq)), seq)
        intermed.sort(self.SegSortCmp)
        return map(operator.getitem, intermed, (-1,) * len(intermed))

    def SegSort(self, lst, attr):
        """ Does Sorting """
        lst[:] = self.SegSort2(lst, attr)

if __name__ == '__main__':
    """ Sets the logging level, then calls the open dialog box before initializing main window"""
    
    logLevel = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_LOGGING, JetDefs.INI_LOGGING)
    if logLevel == 'DEBUG':
        logging.basicConfig(level=logging.DEBUG, format='%(funcName)s[%(lineno)d]: %(message)s')
    elif logLevel == 'INFO':
        logging.basicConfig(level=logging.INFO, format='%(funcName)s[%(lineno)d]: %(message)s')
    elif logLevel == 'ERROR':
        logging.basicConfig(level=logging.ERROR, format='%(funcName)s[%(lineno)d]: %(message)s')
    elif logLevel == 'WARNING':
        logging.basicConfig(level=logging.WARNING, format='%(funcName)s[%(lineno)d]: %(message)s')
    else:
        install_release_loggers()
        
    app = wx.App(None)
    
    if not os.path.isdir(JetDefs.TEMP_JET_DIR):
        os.mkdir(JetDefs.TEMP_JET_DIR)
        
    openFile = ""
    dlg = JetOpen()
    result = dlg.ShowModal()
    if result == wx.ID_CANCEL:
        dlg.Destroy()
    elif result == JetDefs.ID_JET_IMPORT:
        dlg.Destroy()
    
        au = JetCreator(None, -1, "", importFlag=True)
        app.MainLoop()
    else:
        openFile = dlg.fileName
        dlg.Destroy()
    
        au = JetCreator(None, -1, openFile)
        app.MainLoop()

