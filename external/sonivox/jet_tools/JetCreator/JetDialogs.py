"""
 File:  
 JetDialogs.py
 
 Contents and purpose:
 Dialog boxes used in JetCreator
 
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

import wx
import thread
import wx.lib.newevent

from JetDefs import *
from JetCtrls import *
from JetFile import *
from JetUtils import *
from JetPreview import *
from JetSegGraph import *
from eas import *
from JetStatusEvent import *

PLAY_SEGMENT = 1
PLAY_MIDI = 2

class JetEdit():
    """ Class used to build dialog box controls from the definitions in JetDefs """
    def __init__(self, panel, ctrlList, callbackClass):
        LBL_OFFSET = 15
        
        ctrls = getattr(JetDefs, ctrlList)
        self.ctrls = {}
        for Lbl, Text, Row, Col, Len, Min, Max, Id, Lst, Fct, Enabled, HelpText in ctrls:
            try:
                iDisplayLbl = True
                if Text[0:3] == "btn":
                    self.ctrls[Text] = wx.Button(panel, Id, Lbl, wx.Point(Col, Row), size=Len)
                    if Fct > "":
                        self.ctrls[Text].Bind(wx.EVT_BUTTON, getattr(callbackClass, Fct), self.ctrls[Text], id=Id) 
                    if Id == wx.ID_OK:
                        self.ctrls[Text].SetDefault()
                    iDisplayLbl = False
                else:           
                    if Text[0:3] == "txt":
                        self.ctrls[Text] = wx.TextCtrl(panel, Id, "", wx.Point(Col, Row + LBL_OFFSET +3), wx.Size(Len,-1))
                    elif Text[0:4] == "spn1":
                        self.ctrls[Text] = JetSpinOneBased(panel, Id, "", wx.Point(Col, Row + LBL_OFFSET), wx.Size(Len,-1), min=Min, max=Max)
                    elif Text[0:3] == "spn":
                        self.ctrls[Text] = JetSpin(panel, Id, "", wx.Point(Col, Row + LBL_OFFSET), wx.Size(Len,-1), min=Min, max=Max)
                    elif Text[0:3] == "cmb":
                        self.ctrls[Text] = wx.ComboBox(panel, Id, "", wx.Point(Col, Row + LBL_OFFSET), wx.Size(Len,-1), Lst, wx.CB_DROPDOWN | wx.CB_READONLY )
                        self.ctrls[Text].SetValue(Lst[0])
                        if Fct > "":
                            self.ctrls[Text].Bind(wx.EVT_COMBOBOX, getattr(callbackClass, Fct), self.ctrls[Text])
                    elif Text[0:2] == "tm":
                        self.ctrls[Text] = TimeCtrl(panel, pos=(Col, Row + LBL_OFFSET), ctlName=Text)
                    elif Text[0:7] == "filecmb":
                        self.ctrls[Text] = JetFileCombo(panel, pos=(Col, Row + LBL_OFFSET), size=wx.Size(Len,-1), title=Lbl, spec=Lst, id=Id)
                    elif Text[0:7] == "filetxt":
                        self.ctrls[Text] = JetFileText(panel, pos=(Col, Row + LBL_OFFSET), size=wx.Size(Len,-1), title=Lbl, spec=Lst, id=Id)
                    elif Text[0:2] == "fr":
                        self.ctrls[Text] = wx.StaticBox(parent=panel, id=wx.ID_ANY, label=Lbl, pos=(Row, Col), size=Len)
                        iDisplayLbl = False
                    elif Text[0:3] == "chk":
                        self.ctrls[Text] = JetCheckBox(panel, Id, label=Lbl, pos=(Col, Row), size=wx.Size(Len,-1))
                        iDisplayLbl = False
                        if Fct > "":
                            self.ctrls[Text].Bind(wx.EVT_CHECKBOX , getattr(callbackClass, Fct), self.ctrls[Text])
                    elif Text[0:6] == "rdobox":
                        self.ctrls[Text] = wx.RadioBox(panel, Id, label=Lbl, pos=(Col, Row), size=Len, choices=Lst, majorDimension=1, style=wx.RA_SPECIFY_COLS)
                        iDisplayLbl = False
                        if Fct > "":
                            self.ctrls[Text].Bind(wx.EVT_RADIOBOX , getattr(callbackClass, Fct), self.ctrls[Text])
                    elif Text[0:3] == "opt":
                        self.ctrls[Text] = JetRadioButton(panel, Id, label=Lbl, pos=(Col, Row), size=wx.Size(Len,-1))
                        iDisplayLbl = False
                        self.ctrls[Text].SetValue(Lst)
                        if Fct > "":
                            self.ctrls[Text].Bind(wx.EVT_RADIOBUTTON , getattr(callbackClass, Fct), self.ctrls[Text])
                    elif Text[0:3] == "lst":
                        self.ctrls[Text] = wx.ListBox(panel, Id, pos=(Col, Row), size=Len)
                        iDisplayLbl = False
                    elif Text[0:4] == "grd2":
                        self.ctrls[Text] = JetTrackCtrl(panel, Id, pos=(Col, Row + LBL_OFFSET), size=Len, style=wx.LC_REPORT | wx.SUNKEN_BORDER)
                        iDisplayLbl = True
                    elif Text[0:3] == "grd":
                        self.ctrls[Text] = JetListCtrl(panel, Id, pos=(Col, Row), size=Len)
                        iDisplayLbl = False
                    elif Text[0:5] == "graph":
                        self.ctrls[Text] = SegmentGraph(panel, pos=(Col, Row), size=Len)
                        iDisplayLbl = False
                    elif Text[0:3] == "hlp":
                        self.ctrls[Text] = wx.ContextHelpButton(panel, Id, pos=(Col, Row))
                        iDisplayLbl = False
                    elif Text[0:3] == "lbl":
                        self.ctrls[Text] = wx.StaticText(panel, Id, Lbl, wx.Point(Col, Row), size=wx.Size(Len[0],Len[1]))
                        iDisplayLbl = False
                    elif Text[0:3] == "box":
                        self.ctrls[Text] = wx.StaticBox(panel, wx.ID_ANY, Lbl, pos=(Col, Row), size=Len)
                        iDisplayLbl = False
                        
                if iDisplayLbl:
                    self.ctrls[Lbl] = wx.StaticText(panel, Id, Lbl, wx.Point(Col, Row))
                if not Enabled:
                    self.ctrls[Text].Enable(False)
                        
                helpText = IniGetValue(JetDefs.JETCREATOR_HLP, ctrlList, Lbl)
                if helpText > "":
                    self.ctrls[Text].SetHelpText(helpText)
                    
            #except AttributeError:
                #No stub function for testing
                #print("def " + Fct + "(self, event): pass")
            except:
                raise
                            
    def GetValue(self, fld):
        """ Gets the value of a control """
        return self.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        """ Sets the value of a control """
        self.ctrls[fld].SetValue(val)

class JetOpen(wx.Dialog):
    """ Opens a jet definition file """
    def __init__(self):
        wx.Dialog.__init__(self, None, -1, JetDefs.DLG_JETOPEN)
        self.fileName = ""
        self.je = JetEdit(self, "JETOPEN_CTRLS", self)
        fileList = GetRecentJetFiles()
        self.je.ctrls[JetDefs.F_JLIST].AppendItems(fileList)
        if len(fileList) > 0:
            self.je.ctrls[JetDefs.F_JFILE].SetValue(fileList[0])
        self.Bind(wx.EVT_LISTBOX_DCLICK, self.OnOpen)
        self.Bind(wx.EVT_LISTBOX, self.OnClick)
        self.SetSize(JetDefs.JETOPEN_SIZE)
        self.CenterOnParent()
        
    def OnJetImport(self, event):
        """ Exit the dialog with flag to import """
        self.EndModal(JetDefs.ID_JET_IMPORT)

    def OnClick(self, event):
        """ Clicking on item in listbox """
        sValue = self.je.ctrls[JetDefs.F_JLIST].GetString(self.je.ctrls[JetDefs.F_JLIST].GetSelection())
        self.je.ctrls[JetDefs.F_JFILE].SetValue(sValue)
                
    def OnOpen(self, event):
        """ Double clicking on item in listbox """
        sValue = self.je.ctrls[JetDefs.F_JLIST].GetString(self.je.ctrls[JetDefs.F_JLIST].GetSelection())
        self.je.ctrls[JetDefs.F_JFILE].SetValue(sValue)
        if self.Validate():
            self.fileName = self.je.ctrls[JetDefs.F_JFILE].GetValue()
            AppendRecentJetFile(self.fileName)
            self.EndModal(JetDefs.ID_JET_OPEN)        
        
    def OnOk(self, event):
        """ Clicking the ok button """
        if self.Validate():
            self.fileName = self.je.ctrls[JetDefs.F_JFILE].GetValue()
            AppendRecentJetFile(self.fileName)
            self.EndModal(JetDefs.ID_JET_OPEN)
                        
    def OnNew(self, event):
        """ Exit the dialog with flag to create new blank jet file """
        self.EndModal(JetDefs.ID_JET_NEW)
            
    def Validate(self):
        """ Validates the filename """
        if len(self.je.ctrls[JetDefs.F_JFILE].GetValue()) == 0:
            InfoMsg("Jet Filename", "The Jet filename is blank.")
            self.je.ctrls[JetDefs.F_JFILE].SetFocus()
            return False
        if not FileExists(self.je.ctrls[JetDefs.F_JFILE].GetValue()):
            InfoMsg("MIDI File", "The file does not exist.")
            self.je.ctrls[JetDefs.F_JFILE].SetFocus()
            return False
        return True
    
class JetPreferences(wx.Dialog):
    """ Preferences dialog box """
    def __init__(self):
        wx.Dialog.__init__(self, None, -1, JetDefs.DLG_PREFERENCES)
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP)
        self.je = JetEdit(self, "PREFERENCES_CTRLS", self)
        self.SetSize(JetDefs.PREFERENCES_SIZE)
        self.CenterOnParent()

    def OnOk(self, event):
        self.EndModal(wx.ID_OK)
            
    def GetValue(self, fld):
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        self.je.ctrls[fld].SetValue(val)
        

class JetAbout(wx.Dialog):
    """ About dialog box """
    def __init__(self):
        wx.Dialog.__init__(self, None, -1, JetDefs.DLG_ABOUT)
        img = __import__('img_splash')
        bmp = img.getBitmap()
        b = wx.StaticBitmap(self, -1, bmp)
        self.SetSize((bmp.GetWidth(), bmp.GetHeight()))
        self.CenterOnParent()
        s = __import__('sys')
        print(s.platform)
        
    def OnOk(self, event):
        self.EndModal(wx.ID_OK)
            
    def GetValue(self, fld):
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        self.je.ctrls[fld].SetValue(val)
        

class JetPropertiesDialog(wx.Dialog):
    """ Properties dialog box """
    def __init__(self):
        wx.Dialog.__init__(self, None, -1, JetDefs.DLG_PROPERTIES)
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP)
        self.je = JetEdit(self, "JET_PROPERTIES_CTRLS", self)
        self.SetSize(JetDefs.JET_PROPERTIES_SIZE)
        self.CenterOnParent()

    def OnOk(self, event):
        self.EndModal(wx.ID_OK)
            
    def GetValue(self, fld):
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        self.je.ctrls[fld].SetValue(val)
        

class JetErrors(wx.Dialog):
    """ Errors dialog box """
    def __init__(self, title):
        wx.Dialog.__init__(self, None, -1, title)
        self.je = JetEdit(self, "ERRORDLG", self)
        self.SetSize(JetDefs.ERRORDLG_SIZE)
        self.CenterOnParent()

    def OnOk(self, event):
        self.EndModal(wx.ID_OK)
            
    def SetErrors(self, errors):
        self.je.ctrls[JetDefs.F_ERRGRID].AddCol("Error", JetDefs.ERRORCOLW)
        self.je.ctrls[JetDefs.F_ERRGRID].AddCol("Description", JetDefs.ERRORCOLW)
        self.je.ctrls[JetDefs.F_ERRGRID].AddRows(errors)
        
        
class SegEdit(wx.Dialog):
    """ Dialog box to edit segments """
    def __init__(self, title, currentJetConfigFile):
        wx.Dialog.__init__(self, None, -1, title)
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP)
        self.currentJetConfigFile = currentJetConfigFile
        self.je = JetEdit(self, "SEGDLG_CTRLS", self)
        self.je.ctrls[JetDefs.F_MIDIFILE].cmb.Bind(wx.EVT_KILL_FOCUS, self.OnMidiChanged)
        self.je.ctrls[JetDefs.F_MIDIFILE].cmb.Bind(wx.EVT_COMBOBOX, self.OnMidiChanged)
        self.je.ctrls[JetDefs.F_MIDIFILE].SetEventFire(True)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].AddCol(JetDefs.GRD_TRACK, JetDefs.MUTEGRD_TRACK)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].AddCol(JetDefs.GRD_CHANNEL, JetDefs.MUTEGRD_CHANNEL)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].AddCol(JetDefs.GRD_NAME, JetDefs.MUTEGRD_NAME)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].BindCheckBox(self.OnEventChecked)
        self.je.ctrls[JetDefs.F_START].SetChangeCallbackFct(self.UpdateGraph)
        self.je.ctrls[JetDefs.F_END].SetChangeCallbackFct(self.UpdateGraph)
        self.je.ctrls[JetDefs.F_DISPEMPTYTRACKS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_DISPEMPTYTRACKS, JetDefs.F_DISPEMPTYTRACKS, 'bool', 'False'))
        self.je.ctrls[JetDefs.F_GRAPHLABELS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'))
        self.je.ctrls[JetDefs.F_GRAPHCLIPS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'))
        self.je.ctrls[JetDefs.F_GRAPHAPPEVTS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
        self.replicatePrefix = ""
        self.lstReplicate = []
        self.chkReplaceMatching = False

        EVT_JET_STATUS(self, self.OnJetStatusUpdate)
        wx.EVT_CLOSE(self, self.OnClose)
        
        self.Player = None
        self.segment = None
        self.graphSegment = None
        self.jetevents = []
        self.lastMidiFile = ""
        self.lastMidiInfo = None
        self.playMode = PLAY_SEGMENT
        self.graphMode = PLAY_MIDI
        self.SetSize(JetDefs.SEGDLG_SIZE)
        self.CenterOnParent()
        
    def OnClose(self, event):
        """ Closing the dialog box """
        self.ShutdownPlayer()
        self.je.ctrls[JetDefs.F_START].UnBindKillFocus()
        self.je.ctrls[JetDefs.F_END].UnBindKillFocus()
        self.EndModal(wx.ID_CANCEL)
        
    def ShutdownPlayer(self):
        """ Shutdown player flag """
        if self.Player is not None:
            self.Player.SetKeepPlayingFlag(False)
    
    def OnMidiChanged(self, event):
        """ When new midi file selected, get its info """
        self.UpdateMaxMbt()
        event.Skip()
    
    def UpdateMaxMbt(self):
        """ Get midi info in thread so UI smoother """
        thread.start_new_thread(self.UpdateMaxMbtThread, ())
        
    def UpdateMaxMbtThread(self):
        """ Thread to get midi file info """
        if self.je.ctrls[JetDefs.F_MIDIFILE].GetValue() == self.lastMidiFile:
            return
        self.lastMidiFile = self.je.ctrls[JetDefs.F_MIDIFILE].GetValue() 
        self.lastMidiInfo = GetMidiInfo(self.je.ctrls[JetDefs.F_MIDIFILE].GetValue())
        wx.PostEvent(self, JetStatusEvent(JetDefs.PST_MIDI_INFO, self.lastMidiInfo))
            
    def GetValue(self, fld):
        """ Gets a control value """
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        """ Sets a control value """
        self.je.ctrls[fld].SetValue(val)
        if self.je.ctrls[fld] == self.je.ctrls[JetDefs.F_MIDIFILE]:
            self.UpdateMaxMbt()
            
    def OnOk(self, event):
        """ Exits dialog box """
        self.ShutdownPlayer()
        if self.Validate():
            self.je.ctrls[JetDefs.F_START].UnBindKillFocus()
            self.je.ctrls[JetDefs.F_END].UnBindKillFocus()
            self.EndModal(wx.ID_OK)
            
    def Validate(self):
        """ Validates the control values before exiting """
        if not CompareMbt(self.je.ctrls[JetDefs.F_START].GetValue(), self.je.ctrls[JetDefs.F_END].GetValue()):
            InfoMsg("Start/End", "The segment starting and ending times are illogical.")
            self.je.ctrls[JetDefs.F_START].SetFocus()
            return False
        if len(self.je.ctrls[JetDefs.F_SEGNAME].GetValue()) == 0:
            InfoMsg("Segment Name", "The segment must have a name.")
            self.je.ctrls[JetDefs.F_SEGNAME].SetFocus()
            return False
        if len(self.je.ctrls[JetDefs.F_MIDIFILE].GetValue()) == 0:
            InfoMsg("MIDI File", "The segment must have a midi file selected.")
            self.je.ctrls[JetDefs.F_MIDIFILE].SetFocus()
            return False
        if not FileExists(self.je.ctrls[JetDefs.F_MIDIFILE].GetValue()):
            InfoMsg("MIDI File", "The MIDI file does not exist.")
            self.je.ctrls[JetDefs.F_MIDIFILE].SetFocus()
            return False
        if len(self.je.ctrls[JetDefs.F_DLSFILE].GetValue()) > 0:
            if not FileExists(self.je.ctrls[JetDefs.F_DLSFILE].GetValue()):
                InfoMsg("DLS File", "The DLS file does not exist.")
                self.je.ctrls[JetDefs.F_DLSFILE].SetFocus()
                return False
        self.je.ctrls[JetDefs.F_MUTEFLAGS].SetValue(ComputeMuteFlagsFromList(self.je.ctrls[JetDefs.F_MUTEFLAGS]))
        return True
        
    def SetSegment(self, mode):
        """ Builds the segment info for graphing """
        if mode == PLAY_SEGMENT:
            jetevents = self.jetevents
            segment = JetSegment(self.GetValue(JetDefs.F_SEGNAME), 
                                 self.GetValue(JetDefs.F_MIDIFILE),
                                 self.GetValue(JetDefs.F_START), 
                                 self.GetValue(JetDefs.F_END),
                                 JetDefs.MBT_ZEROSTR,
                                 self.GetValue(JetDefs.F_SEGNAME),
                                 self.GetValue(JetDefs.F_QUANTIZE),
                                 jetevents, 
                                 self.GetValue(JetDefs.F_DLSFILE), 
                                 None,
                                 self.GetValue(JetDefs.F_TRANSPOSE),
                                 self.GetValue(JetDefs.F_REPEAT),
                                 self.GetValue(JetDefs.F_MUTEFLAGS))
        else:
            jetevents = []
            segment = JetSegment(self.GetValue(JetDefs.F_SEGNAME), 
                                 self.GetValue(JetDefs.F_MIDIFILE),
                                 JetDefs.MBT_ZEROSTR, 
                                 self.lastMidiInfo.endMbtStr,
                                 JetDefs.MBT_ZEROSTR,
                                 self.GetValue(JetDefs.F_SEGNAME),
                                 self.GetValue(JetDefs.F_QUANTIZE),
                                 jetevents, 
                                 self.GetValue(JetDefs.F_DLSFILE), 
                                 None,
                                 0,
                                 0,
                                 0)
        return segment
        
    def OnEventChecked(self, index, checked):
        """ Track is checked so mute or unmute it """
        if self.Player is not None:
            trackNum = self.je.ctrls[JetDefs.F_MUTEFLAGS].GetTrackNumber(index) 
            self.Player.SetMuteFlag(trackNum, checked)
            
    def OnPlay(self, event):
        """ Play the segment button pressed """
        if self.je.ctrls[JetDefs.F_PLAY].GetLabel() == JetDefs.BUT_STOP:
            self.Player.SetKeepPlayingFlag(False)
            return
        
        if not self.Validate():
            return

        self.playMode = PLAY_SEGMENT
        self.graphSegment = self.SetSegment(self.graphMode)
        self.UpdateGraph()
        self.Player = PreviewPlayer(self.je.ctrls[JetDefs.F_PLAY], self.SetSegment(self.playMode))
        self.Player.SetGraphCtrl(self.je.ctrls[JetDefs.F_GRAPH], self)
        self.PlayerThread = thread.start_new_thread(self.Player .Start, ())
        
    def OnPlayMidi(self, event):
        """ Play the whole midi file pressed """
        if self.je.ctrls[JetDefs.F_PLAYMIDI].GetLabel() == JetDefs.BUT_STOP:
            self.Player.SetKeepPlayingFlag(False)
            return
        
        if not self.Validate():
            return

        self.playMode = PLAY_MIDI
        self.graphSegment = self.SetSegment(self.graphMode)
        self.UpdateGraph()
        self.Player = PreviewPlayer(self.je.ctrls[JetDefs.F_PLAYMIDI], self.SetSegment(self.playMode))
        self.Player.SetGraphCtrl(self.je.ctrls[JetDefs.F_GRAPH], self)
        self.PlayerThread = thread.start_new_thread(self.Player .Start, ())
    
    def OnSetGraphType(self, event):
        """ Sets the type of graph """
        self.SetGraphType(event.GetInt())

    def SetGraphType(self, iMode):
        """ Sets the type of graph """
        if iMode == 1:
            self.graphMode = PLAY_SEGMENT
        else:            
            self.graphMode = PLAY_MIDI
        self.graphSegment = self.SetSegment(self.graphMode)
        self.UpdateGraph()
                
    def OnGraphUpdate(self, evt):
        """ Calls graph control to draw """
        self.je.ctrls[JetDefs.F_GRAPH].DoDrawing()

    def UpdateGraph(self):
        """ Updates values for graph control """
        if self.graphMode == PLAY_SEGMENT:
            self.je.ctrls[JetDefs.F_GRAPH].LoadSegment(self.graphSegment, showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
        else:
            if self.playMode == PLAY_SEGMENT:
                iMidiMode = True
            else:
                iMidiMode = False
            self.je.ctrls[JetDefs.F_GRAPH].LoadSegment(self.graphSegment,(self.GetValue(JetDefs.F_SEGNAME), self.GetValue(JetDefs.F_START), self.GetValue(JetDefs.F_END)), iMidiMode, showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
            
    def OnJetStatusUpdate(self, evt):
        """ All UI needed by thread must be called via Postevent or OS X crashes """
        if evt.mode == JetDefs.PST_UPD_LOCATION:
            self.je.ctrls[JetDefs.F_GRAPH].UpdateLocation(evt.data)
        elif evt.mode == JetDefs.PST_PLAY:
            if self.playMode == PLAY_SEGMENT:
                self.je.ctrls[JetDefs.F_PLAY].SetLabel(JetDefs.BUT_STOP)
                self.je.ctrls[JetDefs.F_PLAYMIDI].Enabled = False
            else:
                self.je.ctrls[JetDefs.F_RDOGRAPH].Enabled = False
                self.je.ctrls[JetDefs.F_PLAYMIDI].SetLabel(JetDefs.BUT_STOP)
                self.je.ctrls[JetDefs.F_PLAY].Enabled = False
                
            self.je.ctrls[JetDefs.F_PAUSE].Enabled = True
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_PAUSE)
        elif evt.mode == JetDefs.PST_DONE:
            self.je.ctrls[JetDefs.F_RDOGRAPH].Enabled = True
            if self.playMode == PLAY_SEGMENT:
                self.je.ctrls[JetDefs.F_PLAY].SetLabel(JetDefs.BUT_PLAYSEG)
                self.je.ctrls[JetDefs.F_PLAYMIDI].Enabled = True
            else:
                self.je.ctrls[JetDefs.F_PLAYMIDI].SetLabel(JetDefs.BUT_PLAYMIDI)
                self.je.ctrls[JetDefs.F_PLAY].Enabled = True

            self.je.ctrls[JetDefs.F_PAUSE].Enabled = False
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_PAUSE)
        elif evt.mode == JetDefs.PST_PAUSE:
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_RESUME)
        elif evt.mode == JetDefs.PST_RESUME:
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_PAUSE)
        elif evt.mode == JetDefs.PST_MIDI_INFO:
            ClearRowSelections(self.je.ctrls[JetDefs.F_MUTEFLAGS])
            self.md = evt.data
            if self.md.err == 0:
                self.je.ctrls[JetDefs.F_END].SetMaxMbt(self.md.maxMeasures+1,self.md.maxBeats,self.md.maxTicks)
                if self.je.ctrls[JetDefs.F_END].GetValue() == JetDefs.MBT_ZEROSTR:
                    self.je.ctrls[JetDefs.F_END].SetValue((self.md.maxMeasures,0,0)) 
                self.je.ctrls[JetDefs.F_START].SetMaxMbt(self.md.maxMeasures+1,self.md.maxBeats,self.md.maxTicks)
                self.je.ctrls[JetDefs.F_MUTEFLAGS].DeleteAllItems()
                loadEmpty = IniGetValue(self.currentJetConfigFile, JetDefs.INI_DISPEMPTYTRACKS, JetDefs.INI_DISPEMPTYTRACKS, 'bool', 'False')
                for track in self.md.trackList:
                    self.je.ctrls[JetDefs.F_MUTEFLAGS].AddTrackRow(track, loadEmpty) 
                self.je.ctrls[JetDefs.F_MUTEFLAGS].CheckTracks(self.je.ctrls[JetDefs.F_MUTEFLAGS].GetValue())
                self.graphSegment = self.SetSegment(self.graphMode)
                self.UpdateGraph()
       
    def OnSetTrackDisplayOption(self, evt):
        IniSetValue(self.currentJetConfigFile, JetDefs.INI_DISPEMPTYTRACKS, JetDefs.INI_DISPEMPTYTRACKS, self.je.ctrls[JetDefs.F_DISPEMPTYTRACKS].GetValue())
        loadEmpty = IniGetValue(self.currentJetConfigFile, JetDefs.INI_DISPEMPTYTRACKS, JetDefs.INI_DISPEMPTYTRACKS, 'bool', 'False')
        if self.md is not None:
            self.je.ctrls[JetDefs.F_MUTEFLAGS].DeleteAllItems()
            if self.md.err == 0:
                for track in self.md.trackList:
                    self.je.ctrls[JetDefs.F_MUTEFLAGS].AddTrackRow(track, loadEmpty) 
       
    def OnPause(self, evt):
        """ Pauses the playback """
        self.Player.Pause()            

    def OnSetGraphOptions(self, evt):
        """ Sets graph options """
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, self.je.ctrls[JetDefs.F_GRAPHLABELS].GetValue())
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, self.je.ctrls[JetDefs.F_GRAPHAPPEVTS].GetValue())
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, self.je.ctrls[JetDefs.F_GRAPHCLIPS].GetValue())
        self.UpdateGraph()

    def OnReplicate(self, evt):
        dlg = JetReplicate("Replicate Segment")
        dlg.SetValue(JetDefs.F_RPREPLACE, True)
        dlg.SetName(self.GetValue(JetDefs.F_SEGNAME))
        dlg.event_type = "SEGMENT"
        dlg.event_max = self.je.ctrls[JetDefs.F_START].GetMaxMbt()
        dlg.length = MbtDifference(ConvertStrTimeToTuple(self.GetValue(JetDefs.F_START)), ConvertStrTimeToTuple(self.GetValue(JetDefs.F_END)))
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            self.replicatePrefix = dlg.GetValue(JetDefs.F_RPPREFIX)
            self.lstReplicate = dlg.lstReplicate
            self.chkReplaceMatching = dlg.GetValue(JetDefs.F_RPREPLACE)
            self.EndModal(wx.ID_OK)            
        else:
            dlg.Destroy()

class EventEdit(wx.Dialog):
    """ Event edit dialog box """
    def __init__(self, title, currentJetConfigFile):
        wx.Dialog.__init__(self, None, -1, title)
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP)
        self.title = title
        self.currentJetConfigFile = currentJetConfigFile
        self.je = JetEdit(self, "EVTDLG_CTRLS", self)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].AddCol(JetDefs.GRD_TRACK, JetDefs.MUTEGRD_TRACK)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].AddCol(JetDefs.GRD_CHANNEL, JetDefs.MUTEGRD_CHANNEL)
        self.je.ctrls[JetDefs.F_MUTEFLAGS].AddCol(JetDefs.GRD_NAME, JetDefs.MUTEGRD_NAME)
        self.je.ctrls[JetDefs.F_ESTART].SetChangeCallbackFct(self.UpdateGraph)
        self.je.ctrls[JetDefs.F_EEND].SetChangeCallbackFct(self.UpdateGraph)
        self.je.ctrls[JetDefs.F_GRAPHLABELS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'))
        self.je.ctrls[JetDefs.F_GRAPHCLIPS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'))
        self.je.ctrls[JetDefs.F_GRAPHAPPEVTS].SetValue(IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
        EVT_JET_STATUS(self, self.OnJetStatusUpdate)
        self.segment = None
        self.Player = None
        self.event_id = 1
        self.replicatePrefix = ""
        self.lstReplicate = []
        self.chkReplaceMatching = False
        
        wx.EVT_CLOSE(self, self.OnClose)
        self.SetSize(JetDefs.EVTDLG_SIZE)
        self.CenterOnParent()
        
    def OnGraphUpdate(self, evt):
        """ Calls the graph module to update the graph """
        self.je.ctrls[JetDefs.F_GRAPH].DoDrawing()

    def OnJetStatusUpdate(self, evt):
        """ Updates to UI needed by play thread come through here otherwise OS X crashes """
        if evt.mode == JetDefs.PST_UPD_LOCATION:
            self.je.ctrls[JetDefs.F_GRAPH].UpdateLocation(evt.data)
        elif evt.mode == JetDefs.PST_PLAY:
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_PAUSE)
            self.je.ctrls[JetDefs.F_PLAY].SetLabel(JetDefs.BUT_STOP)
            self.je.ctrls[JetDefs.F_PAUSE].Enabled = True
            self.je.ctrls[JetDefs.F_ETRIGGERBUT].Enabled = True
            self.je.ctrls[JetDefs.F_EMUTEBUT].Enabled = True
        elif evt.mode == JetDefs.PST_DONE:
            self.je.ctrls[JetDefs.F_EMUTEBUT].SetLabel(JetDefs.BUT_UNMUTE)
            self.je.ctrls[JetDefs.F_PLAY].SetLabel(JetDefs.BUT_PLAY)
            self.je.ctrls[JetDefs.F_PAUSE].Enabled = False
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_PAUSE)
            self.je.ctrls[JetDefs.F_ETRIGGERBUT].Enabled = False
            self.je.ctrls[JetDefs.F_EMUTEBUT].Enabled = False
        elif evt.mode == JetDefs.PST_PAUSE:
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_RESUME)
        elif evt.mode == JetDefs.PST_RESUME:
            self.je.ctrls[JetDefs.F_PAUSE].SetLabel(JetDefs.BUT_PAUSE)
       
    def OnPause(self, evt):
        """ Pause the player """
        self.Player.Pause()

    def UpdateGraph(self):
        """ Called back from player thread to update the graph """
        if len(self.segment.jetevents) == 0:
            self.segment.jetevents.append(JetEvent(self.je.ctrls[JetDefs.F_ENAME].GetValue(), 
                                                   self.je.ctrls[JetDefs.F_ETYPE].GetValue(), 
                                                   1, 
                                                   self.je.ctrls[JetDefs.F_ETRACK].GetValue(), 
                                                   self.je.ctrls[JetDefs.F_ECHANNEL].GetValue(), 
                                                   self.je.ctrls[JetDefs.F_ESTART].GetValue(), 
                                                   self.je.ctrls[JetDefs.F_EEND].GetValue()))
            
        self.segment.jetevents[0].event_name = self.je.ctrls[JetDefs.F_ENAME].GetValue()
        self.segment.jetevents[0].event_type = self.je.ctrls[JetDefs.F_ETYPE].GetValue()
        self.segment.jetevents[0].event_start = self.je.ctrls[JetDefs.F_ESTART].GetValue()
        self.segment.jetevents[0].event_end = self.je.ctrls[JetDefs.F_EEND].GetValue()
        self.je.ctrls[JetDefs.F_GRAPH].LoadSegment(self.segment, showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))

    def OnClose(self, event):
        """ Called when dialog is closed """
        self.ShutdownPlayer()
        self.je.ctrls[JetDefs.F_ESTART].UnBindKillFocus()
        self.je.ctrls[JetDefs.F_EEND].UnBindKillFocus()
        self.EndModal(wx.ID_CANCEL)
            
    def ShutdownPlayer(self):
        """ Sets flag to kill play loop """
        if self.Player is not None:
            self.Player.SetKeepPlayingFlag(False)
    
    def GetValue(self, fld):
        """ Gets the value of a control """
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        """ Sets the value of a control """
        self.je.ctrls[fld].SetValue(val)
        
    def SetEventId(self):
        """ Sets the eventid value """
        if self.title == JetDefs.MAIN_ADDEVENTTITLE:
            iNextEventId = -1
            for evt in self.segment.jetevents:
                if evt.event_type == JetDefs.E_CLIP:
                    if iNextEventId < evt.event_id:
                        iNextEventId = evt.event_id
            self.je.ctrls[JetDefs.F_EEVENTID].SetValue(iNextEventId + 1)
        
    def OnEventSelect(self, event=None):
        """ Adjusts the dialog box controls for various types of events """
        eType = self.je.ctrls[JetDefs.F_ETYPE].GetValue()
        if eType == JetDefs.E_EOS:
            self.je.ctrls[JetDefs.F_ENAME].SetValue(JetDefs.E_EOS)
            self.je.ctrls[JetDefs.F_ENAME].Enable(False)            
            self.je.ctrls[JetDefs.F_ESTART].Enable(False)
            self.je.ctrls[JetDefs.F_EEND].Enable(True)
            self.je.ctrls[JetDefs.F_ETRIGGERBUT].Enable(False)
            self.je.ctrls[JetDefs.F_EEVENTID].Enable(False)
        elif eType == JetDefs.E_CLIP:
            if len(self.je.ctrls[JetDefs.F_ENAME].GetValue()) == 0 or self.je.ctrls[JetDefs.F_ENAME].GetValue() == JetDefs.E_EOS or self.je.ctrls[JetDefs.F_ENAME].GetValue() == JetDefs.E_APP:
                self.je.ctrls[JetDefs.F_ENAME].SetValue(JetDefs.E_CLIP)
            self.je.ctrls[JetDefs.F_ENAME].Enable(True)            
            self.je.ctrls[JetDefs.F_ESTART].Enable(True)   
            self.je.ctrls[JetDefs.F_EEND].Enable(True)
            self.je.ctrls[JetDefs.F_ETRIGGERBUT].Enable(True)
            self.je.ctrls[JetDefs.F_EEVENTID].Enable(True)
            self.je.ctrls[JetDefs.F_EEVENTID].SetRange(JetDefs.EVENTID_MIN, JetDefs.EVENTID_MAX)
            if self.je.ctrls[JetDefs.F_EEVENTID].GetValue() < JetDefs.EVENTID_MIN:
                self.je.ctrls[JetDefs.F_EEVENTID].SetValue(JetDefs.EVENTID_MIN)
            if self.je.ctrls[JetDefs.F_EEVENTID].GetValue() > JetDefs.EVENTID_MAX:
                self.je.ctrls[JetDefs.F_EEVENTID].SetValue(JetDefs.EVENTID_MAX)
            self.SetEventId()
        elif eType == JetDefs.E_APP:
            if len(self.je.ctrls[JetDefs.F_ENAME].GetValue()) == 0 or self.je.ctrls[JetDefs.F_ENAME].GetValue() == JetDefs.E_EOS:
                self.je.ctrls[JetDefs.F_ENAME].SetValue(JetDefs.E_APP)
            self.je.ctrls[JetDefs.F_ENAME].Enable(True)       
            self.je.ctrls[JetDefs.F_ESTART].Enable(True)
            self.je.ctrls[JetDefs.F_EEND].Enable(False)
            self.je.ctrls[JetDefs.F_ETRIGGERBUT].Enable(False)
            self.je.ctrls[JetDefs.F_EEVENTID].Enable(True)
            self.je.ctrls[JetDefs.F_EEVENTID].SetRange(JetDefs.APPCONTROLLERID_MIN, JetDefs.APPCONTROLLERID_MAX)
            if self.je.ctrls[JetDefs.F_EEVENTID].GetValue() < JetDefs.APPCONTROLLERID_MIN:
                self.je.ctrls[JetDefs.F_EEVENTID].SetValue(JetDefs.APPCONTROLLERID_MIN)
            if self.je.ctrls[JetDefs.F_EEVENTID].GetValue() > JetDefs.APPCONTROLLERID_MAX:
                self.je.ctrls[JetDefs.F_EEVENTID].SetValue(JetDefs.APPCONTROLLERID_MAX)
            
    def OnOk(self, event):
        """ Exits the dialog box """
        self.ShutdownPlayer()
        if self.Validate():
            self.je.ctrls[JetDefs.F_ESTART].UnBindKillFocus()
            self.je.ctrls[JetDefs.F_EEND].UnBindKillFocus()
            self.EndModal(wx.ID_OK)
            
    def Validate(self):
        """ Validates the control values prior to exiting """
        if len(self.je.ctrls[JetDefs.F_ENAME].GetValue()) == 0:
            InfoMsg("Event Name", "The event must have a name.")
            self.je.ctrls[JetDefs.F_ENAME].SetFocus()
            return False
        if len(self.je.ctrls[JetDefs.F_ETYPE].GetValue()) == 0:
            InfoMsg("Event Name", "The event type must be selected.")
            self.je.ctrls[JetDefs.F_ETYPE].SetFocus()
            return False
        if self.je.ctrls[JetDefs.F_ETYPE].GetValue() == JetDefs.E_CLIP:
            if not CompareMbt(self.je.ctrls[JetDefs.F_ESTART].GetValue(), self.je.ctrls[JetDefs.F_EEND].GetValue()):
                InfoMsg("Start/End", "The event starting and ending times are illogical.")
                self.je.ctrls[JetDefs.F_ESTART].SetFocus()
                return False
            if MbtVal(self.je.ctrls[JetDefs.F_ESTART].GetValue()) < MbtVal(self.je.ctrls[JetDefs.F_START].GetValue()):
                InfoMsg("Event Starting Time", "The event starting time is illogical.")
                self.je.ctrls[JetDefs.F_ESTART].SetFocus()
                return False
            if MbtVal(self.je.ctrls[JetDefs.F_EEND].GetValue()) > MbtVal(self.je.ctrls[JetDefs.F_END].GetValue()):
                InfoMsg("Event Ending Time", "The event ending time is illogical.")
                self.je.ctrls[JetDefs.F_ESTART].SetFocus()
                return False
        if self.je.ctrls[JetDefs.F_ETYPE].GetValue() == JetDefs.E_APP:
            self.je.ctrls[JetDefs.F_EEND].SetValue(self.je.ctrls[JetDefs.F_ESTART].GetValue())
        if self.je.ctrls[JetDefs.F_ETYPE].GetValue() == JetDefs.E_EOS:
            self.je.ctrls[JetDefs.F_ESTART].SetValue(self.je.ctrls[JetDefs.F_EEND].GetValue())
        return True
    
    def SetSegment(self, segment):
        """ Sets the segment values, then calls the graph update """
        self.segment = segment
        md = GetMidiInfo(segment.filename)
        if md.err == 0:
            self.SetValue(JetDefs.F_SEGNAME, segment.segname)
            self.SetValue(JetDefs.F_MUTEFLAGS, segment.mute_flags)
            self.SetValue(JetDefs.F_MIDIFILE, segment.filename)
            self.SetValue(JetDefs.F_DLSFILE, segment.dlsfile)
            self.SetValue(JetDefs.F_START, segment.start)
            self.SetValue(JetDefs.F_END, segment.end)
            self.SetValue(JetDefs.F_QUANTIZE, segment.quantize)
            self.SetValue(JetDefs.F_TRANSPOSE, segment.transpose)
            self.SetValue(JetDefs.F_REPEAT, segment.repeat)
            maxMeasures = abs(int(self.je.ctrls[JetDefs.F_END].GetValue('int')[0]))
            self.je.ctrls[JetDefs.F_EEND].SetMaxMbt(maxMeasures+1,md.maxBeats,md.maxTicks)   
            self.je.ctrls[JetDefs.F_ESTART].SetMaxMbt(maxMeasures+1,md.maxBeats,md.maxTicks)
            minMeasures = abs(int(self.je.ctrls[JetDefs.F_START].GetValue('int')[0]))
            self.je.ctrls[JetDefs.F_EEND].SetMinMbt(minMeasures+1,0,0)   
            self.je.ctrls[JetDefs.F_ESTART].SetMinMbt(minMeasures+1,0,0)                 
            self.je.ctrls[JetDefs.F_END].GetValue('int')
            self.je.ctrls[JetDefs.F_ETRACK].SetRange(1, md.maxTracks)            
            self.je.ctrls[JetDefs.F_MUTEFLAGS].DeleteAllItems()
            for track in md.trackList:
                self.je.ctrls[JetDefs.F_MUTEFLAGS].AddTrackRow(track)                    
            self.je.ctrls[JetDefs.F_MUTEFLAGS].CheckTracks(self.je.ctrls[JetDefs.F_MUTEFLAGS].GetValue())
            self.je.ctrls[JetDefs.F_MUTEFLAGS].SetTextColour(wx.SystemSettings.GetColour(wx.SYS_COLOUR_GRAYTEXT))
        self.je.ctrls[JetDefs.F_GRAPH].LoadSegment(segment, showLabels=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, 'bool', 'True'), showClips=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, 'bool', 'True'), showAppEvts=IniGetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, 'bool', 'True'))
            
    def OnPlay(self, event):
        """ Plays the segment allowing interaction with events """
        if self.je.ctrls[JetDefs.F_PLAY].GetLabel() == JetDefs.BUT_STOP:
            self.Player.SetKeepPlayingFlag(False)
            return
        
        if not self.Validate():
            return
        
        jetevents = []
        jetevents.append(JetEvent(self.GetValue(JetDefs.F_ENAME), self.GetValue(JetDefs.F_ETYPE), 
                                  self.event_id, int(self.GetValue(JetDefs.F_ETRACK)), 
                                  int(self.GetValue(JetDefs.F_ECHANNEL)), 
                                  self.GetValue(JetDefs.F_ESTART), self.GetValue(JetDefs.F_EEND)))

        segment = JetSegment(self.GetValue(JetDefs.F_SEGNAME), 
                             self.GetValue(JetDefs.F_MIDIFILE),
                             self.GetValue(JetDefs.F_START), 
                             self.GetValue(JetDefs.F_END),
                             JetDefs.MBT_ZEROSTR,
                             self.GetValue(JetDefs.F_SEGNAME),
                             self.GetValue(JetDefs.F_QUANTIZE),
                             jetevents, 
                             self.GetValue(JetDefs.F_DLSFILE), 
                             None,
                             self.GetValue(JetDefs.F_TRANSPOSE),
                             self.GetValue(JetDefs.F_REPEAT),
                             self.GetValue(JetDefs.F_MUTEFLAGS))

        self.Player = PreviewPlayer(self.je.ctrls[JetDefs.F_PLAY], segment)
        self.Player.SetGraphCtrl(self.je.ctrls[JetDefs.F_GRAPH], self)
        self.je.ctrls[JetDefs.F_GRAPH].ClickCallbackFct = self.GraphTriggerClip
        self.Player.trigger_button = self.je.ctrls[JetDefs.F_ETRIGGERBUT]
        self.Player.mute_button = self.je.ctrls[JetDefs.F_EMUTEBUT]
        thread.start_new_thread(self.Player .Start, ())
        
    def GraphTriggerClip(self, sClipName, iEventId):
        """ Triggers a clip via graph """
        if self.Player is not None:
            self.Player.GraphTriggerClip(sClipName, iEventId)
        
    def OnMute(self, event):
        """ Mutes a track """
        if self.Player is not None:
            self.Player.MuteTrackViaButton(self.je.ctrls[JetDefs.F_EMUTEBUT], 
                                           int(self.je.ctrls[JetDefs.F_ETRACK].GetValue()))

    def OnTriggerClip(self, event):
        """ Triggers a clip """
        if self.Player is not None:
            self.Player.TriggerClip(self.event_id)

    def OnSetGraphOptions(self, evt):
        """ Sets graph options """
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHLABELS, JetDefs.F_GRAPHLABELS, self.je.ctrls[JetDefs.F_GRAPHLABELS].GetValue())
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHAPPEVTS, JetDefs.F_GRAPHAPPEVTS, self.je.ctrls[JetDefs.F_GRAPHAPPEVTS].GetValue())
        IniSetValue(self.currentJetConfigFile, JetDefs.F_GRAPHCLIPS, JetDefs.F_GRAPHCLIPS, self.je.ctrls[JetDefs.F_GRAPHCLIPS].GetValue())
        self.UpdateGraph()

    def OnReplicate(self, evt):
        dlg = JetReplicate("Replicate Event")
        dlg.SetValue(JetDefs.F_RPREPLACE, True)
        dlg.SetName(self.GetValue(JetDefs.F_ENAME))
        dlg.event_type = self.GetValue(JetDefs.F_ETYPE)
        dlg.event_max = self.segment.end
        dlg.length = MbtDifference(ConvertStrTimeToTuple(self.GetValue(JetDefs.F_ESTART)), ConvertStrTimeToTuple(self.GetValue(JetDefs.F_EEND)))
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            self.replicatePrefix = dlg.GetValue(JetDefs.F_RPPREFIX)
            self.lstReplicate = dlg.lstReplicate
            self.chkReplaceMatching = dlg.GetValue(JetDefs.F_RPREPLACE)
            self.EndModal(wx.ID_OK)            
        else:
            dlg.Destroy()

class JetReplicate(wx.Dialog):
    """ Replicate dialog box """
    def __init__(self, title):
        wx.Dialog.__init__(self, None, -1, title)
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP)
        self.je = JetEdit(self, "REPLICATE_CTRLS", self)
        
        self.je.ctrls[JetDefs.F_RPINCREMENT].SetMinMbt(0,0,0)
        self.je.ctrls[JetDefs.F_RPINCREMENT].SetValue((-1,-1,-1))
        self.je.ctrls[JetDefs.F_RPNUMBER].SetValue(1)
        for title, width, fld in JetDefs.REPLICATE_GRID:
            self.je.ctrls[JetDefs.F_RPGRDPREVIEW].AddCol(title, width)
        self.lstReplicate = []
        
        self.SetSize(JetDefs.REPLICATE_SIZE)
        self.CenterOnParent()

    def OnOk(self, event):
        self.EndModal(wx.ID_OK)
            
    def GetValue(self, fld):
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        self.je.ctrls[fld].SetValue(val)
    
    def SetName(self, name):
        for i in range(len(name), 1, -1):
            if not name[i-1].isdigit() and name[i-1] <> ' ':
                break
            else:
                name = name[0:i-1]
        self.SetValue(JetDefs.F_RPPREFIX, name)
        
    def Validate(self):
        if self.GetValue(JetDefs.F_RPPREFIX) == '':
            InfoMsg("Message", "Prefix is required.")
            return False
        return True
        
    def OnPreview(self, event):
        if not self.Validate():
            return
        start = MbtVal(self.GetValue(JetDefs.F_ESTART))
        max = MbtVal(self.event_max)
        increment = MbtVal((self.je.ctrls[JetDefs.F_RPINCREMENT].GetMeasure(), self.je.ctrls[JetDefs.F_RPINCREMENT].GetBeat(), self.je.ctrls[JetDefs.F_RPINCREMENT].GetTick()))

        self.lstReplicate = []
        iTo = int(self.GetValue(JetDefs.F_RPNUMBER))
        for i in range(iTo):
            evt_name = "%s %.2d" % (self.GetValue(JetDefs.F_RPPREFIX), i)
            s_ticks = start + (i * increment) 
            s_mbt = TicksToMbt(s_ticks)
            evt_start =  "%d:%d:%d" % (s_mbt[0]+1, s_mbt[1]+1, s_mbt[2])  
            if self.event_type == JetDefs.E_CLIP or self.event_type == "SEGMENT":
                e_ticks = s_ticks + self.length
                e_mbt = TicksToMbt(e_ticks)
                evt_end = "%d:%d:%d" % (e_mbt[0]+1, e_mbt[1]+1, e_mbt[2])  
            else:
                e_ticks = s_ticks
                evt_end = evt_start
            if s_ticks <= max and e_ticks <= max:
                self.lstReplicate.append((evt_name, evt_start, evt_end))
            
        self.je.ctrls[JetDefs.F_RPGRDPREVIEW].DeleteAllItems()
        self.je.ctrls[JetDefs.F_RPGRDPREVIEW].AddRows(self.lstReplicate)

class JetMove(wx.Dialog):
    """ Move events dialog box """
    def __init__(self, title):
        wx.Dialog.__init__(self, None, -1, title)
        self.SetExtraStyle(wx.DIALOG_EX_CONTEXTHELP)
        self.je = JetEdit(self, "MOVE_CTRLS", self)
        
        self.je.ctrls[JetDefs.F_RPINCREMENT].SetMinMbt(-999,-4,-480)
        self.je.ctrls[JetDefs.F_RPINCREMENT].SetValue((-1,-1,-1))
        for title, width, fld in JetDefs.REPLICATE_GRID:
            self.je.ctrls[JetDefs.F_RPGRDPREVIEW].AddCol(title, width)
        self.lstMove = []
        self.lstMoveMbt = []    
        self.lstMoveItems = []
        
        self.SetSize(JetDefs.REPLICATE_SIZE)
        self.CenterOnParent()

    def OnOk(self, event):
        self.EndModal(wx.ID_OK)
            
    def GetValue(self, fld):
        return self.je.ctrls[fld].GetValue()
    
    def SetValue(self, fld, val):
        self.je.ctrls[fld].SetValue(val)
    
    def OnPreview(self, event):
        increment = MbtVal((abs(self.je.ctrls[JetDefs.F_RPINCREMENT].GetMeasure()), abs(self.je.ctrls[JetDefs.F_RPINCREMENT].GetBeat()), abs(self.je.ctrls[JetDefs.F_RPINCREMENT].GetTick())))
        if self.je.ctrls[JetDefs.F_RPINCREMENT].GetMeasure() < 0 or self.je.ctrls[JetDefs.F_RPINCREMENT].GetBeat() < 0 or self.je.ctrls[JetDefs.F_RPINCREMENT].GetTick() < 0:
            increment = 0 - increment
        self.lstMove = []
        self.lstMoveMbt = []
        for itm in self.lstMoveItems:
            max = MbtVal(itm[3])  
            evt_name = itm[0]
            start = MbtVal(itm[1])  
            s_ticks = start + increment
            
            s_mbt = TicksToMbt(s_ticks)
            evt_start =  "%d:%d:%d" % (s_mbt[0]+1, s_mbt[1]+1, s_mbt[2])  
            evt_start_save = "%d:%d:%d" % s_mbt 
            
            end = MbtVal(itm[2])  
            e_ticks = end + increment
            e_mbt = TicksToMbt(e_ticks)
            evt_end = "%d:%d:%d" % (e_mbt[0]+1, e_mbt[1]+1, e_mbt[2])  
            evt_end_save = "%d:%d:%d" % e_mbt 
            
            if s_ticks <= max and e_ticks <= max and s_ticks >= 0 and e_ticks >= 0:
                self.lstMove.append((evt_name, evt_start, evt_end))
                self.lstMoveMbt.append((evt_name, evt_start_save, evt_end_save))
            
        self.je.ctrls[JetDefs.F_RPGRDPREVIEW].DeleteAllItems()
        self.je.ctrls[JetDefs.F_RPGRDPREVIEW].AddRows(self.lstMove)

if __name__ == '__main1__':
    """ Test dialogs """
    app = wx.PySimpleApp()

    #dlg = JetOpen()
    #dlg = JetPropertiesDialog()
    #dlg = ExportDialog("Export Jet File")
    #dlg = JetAbout()

    dlg = JetReplicate("Replicate Event")
    dlg.SetValue(JetDefs.F_RPREPLACE, True)
    dlg.event_max = "5:0:0"
    dlg.event_type = JetDefs.E_APP
    dlg.length = 480
    dlg.SetName("abc 02")
    result = dlg.ShowModal()
    if result == wx.ID_OK:
        print("OK")

    dlg.Destroy()
        
if __name__ == '__main1__':
    """ Test Segment dialog """
    app = wx.PySimpleApp()

    helpProvider = wx.SimpleHelpProvider()
    wx.HelpProvider_Set(helpProvider)

    dlg = SegEdit("Segments", JetDefs.UNTITLED_FILE)
    dlg.SetValue(JetDefs.F_SEGNAME, "Test Segment Name")
    dlg.SetValue(JetDefs.F_MIDIFILE, '/Users/BHruska/JetContent/jenn_Burning Line.mid')
    dlg.SetValue(JetDefs.F_MIDIFILE, 'C:/_Data/JetCreator/JetDemo1/jenn_Burning Line.mid')
    dlg.SetValue(JetDefs.F_DLSFILE, '')
    dlg.SetValue(JetDefs.F_START, '4:0:0')
    dlg.SetValue(JetDefs.F_END, '8:0:0')
    dlg.SetValue(JetDefs.F_QUANTIZE, 6)

    result = dlg.ShowModal()
    dlg.Destroy()

if __name__ == '__main__':
    """ Test Event dialog """
    app = wx.PySimpleApp()

    jetevents = []
     
    segment = JetSegment("Test Segment Name", 'C:/_Data/JetCreator/JetDemo1/jenn_Burning Line.mid',
                            '0:0:0', '4:0:0', None, 
                            "Test Segment Name", 6, jetevents, 
                            '', None, 0,0,3)

    dlg = EventEdit("Event Edit", JetDefs.UNTITLED_FILE)
    dlg.SetValue(JetDefs.F_ENAME, "Test Event Name")
    dlg.SetValue(JetDefs.F_ETYPE, "TriggerClip")
    dlg.SetSegment(segment)
    
    result = dlg.ShowModal()
    dlg.Destroy()


    
