"""
 File:  
 JetDefs.py
 
 Contents and purpose:
 Holds definitions used throughout JetCreator
 
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

class JetDefs():
    def CreateHelpIniFile(self):
        """ Used to create the help data file for context sensitive help """
        self.CreateHelpIniForDialog("SEGDLG_CTRLS")
        self.CreateHelpIniForDialog("EVTDLG_CTRLS")
        self.CreateHelpIniForDialog("PREFERENCES_CTRLS")
        self.CreateHelpIniForDialog("JET_PROPERTIES_CTRLS")
        self.CreateHelpIniForDialog("REPLICATE_CTRLS")
        self.CreateHelpIniForDialog("MOVE_CTRLS")
        
    def CreateHelpIniForDialog(self, dlgName):
        """ Used to create the help data file for context sensitive help """
        print("\n" + dlgName)
        lst = getattr(self, dlgName)
        u = __import__('JetUtils')
        for ctrl in lst:
            fld = ctrl[0]
            if fld[0:2] != "fr":
                if u.IniGetValue(self.JETCREATOR_HLP, dlgName, fld) == "":
                    u.IniSetValue(self.JETCREATOR_HLP, dlgName, fld, "")
                    print(fld)

    DEFAULT_MUTE_SYNC = False
    
    TEMP_JET_DIR = "./Tmp/"
    TEMP_JET_CONFIG_FILE = "./Tmp/Temp.jtc"
    UNTITLED_FILE = "Untitled.jtc"
    JETCREATOR_INI = "JetCreator.ini"
    JETMIDIFILES_INI = "JetMidiFiles.ini"
    JETCREATOR_HLP = "JetCreatorhlp.dat"
        
    #Postevent message defines
    PST_UPD_LOCATION = 1
    PST_PLAY = 2
    PST_DONE = 3
    PST_PAUSE = 4
    PST_RESUME = 5
    PST_MIDI_INFO = 6
    
    #Dialog titles and prompts
    DLG_JETOPEN = "Open Jet File"
    DLG_PREFERENCES = "Preferences"
    DLG_ABOUT = "About"
    DLG_PROPERTIES = "Jet Project Properties"
    DLG_AUDITION = "Audition Jet File"
    DLG_REPLICATE = "Replicate Event"
    DLG_MOVE = "Move Events"
    MAIN_TITLEPREFIX = 'Jet Creator - '
    MAIN_DLG_CTRLS = 'JET_CREATOR'
    MAIN_SEGLIST = 'segList'
    MAIN_EVENTLIST = 'eventList'
    MAIN_ADDSEGTITLE = "Add Segments"
    MAIN_REVSEGTITLE = "Revise Segments"
    MAIN_ADDEVENTTITLE = "Add Event"
    MAIN_REVEVENTTITLE = "Revise Event"
    MAIN_CONFIRM = "Confirm Deletion"
    MAIN_CONFIRM_SEG_DLT = "\n\nOkay to delete segment(s)?"
    MAIN_CONRIRM_EVT_DLT = "\n\nOkay to delete event(s)?"
    MAIN_PLAYSEG = "Play Segments"
    MAIN_PLAYSEGMSG = "Queue one or more segments by checking them in the list, then play."
    MAIN_HELPTITLE = "Jet Creator Help"
    MAIN_HELPFILE = "JET Creator User Manual.htm"
    MAIN_HELPGUIDELINESTITLE = "Jet Authoring Guidelines"
    MAIN_HELPGUIDELINESFILE = "JET Authoring Guidelines.htm"
    MAIN_IMPORTTITLE = "Import Project"
    MAIN_IMPORTMSG = "Okay to import\n\n%s\n\ninto\n\n%s?"
    MAIN_SAVEBEFOREEXIT = "Save project before exiting?"
    MAIN_JETCREATOR = "Jet Creator"
    
    #Audition window defines
    AUDITION_CTRLS = 'AUDITION_CTRLS'
    AUDITION_SEGLIST = 'segList'
    AUDITION_QUEUELIST = 'queueList'
    AUDITION_TRACKLIST = 'trackList'
    AUDITION_GRAPH = 'graph'
    
    PLAY_TRIGGERCLIP_MSG = 'Triggered Clip %d: %s'
    
    #Config file defines
    RECENT_SECTION = "Recent"
    DIR_SECTION = "Directories"
    IMAGES_DIR = "ImagesDir"
    INI_PREF_SECTION = "Preferences"
    INI_PROJECT_DIRS = "chkProjectDir"
    INI_LOGGING = "Logging"
    INI_DEFAULTDIRS = "Directories"
    INI_DISPEMPTYTRACKS = "DisplayEmptyTracks"
    INI_EVENTSORT = "EventSort"
    INI_EVENTSORT_0 = "EventSort0"
    INI_EVENTSORT_1 = "EventSort1"
    INI_SEGSORT = "SegSort"
    INI_SEGSORT_0 = "SegSort0"
    INI_SEGSORT_1 = "SegSort1"
    
    #Mbt defines
    MBT_DEFAULT = (0,0,0)
    MBT_MIN = 0
    MBT_ZEROSTR = "0:0:0"
    
    #File open dialog specs
    APPLICATION_TITLE = "Jet Creator"
    MIDI_FILE_SPEC = 'MIDI Files (*.mid)|*.mid|All Files (*.*)|*.*'
    DLS_FILE_SPEC = 'DLS Files (*.dls)|*.dls|All Files (*.*)|*.*'
    JTC_FILE_SPEC = 'Jet Content Files (*.jtc)|*.jtc|All Files (*.*)|*.*'
    ARCHIVE_FILE_SPEC = 'Jet Archive Files (*.zip)|*.zip|All Files (*.*)|*.*'
    OPEN_PROMPT = "Open Jet Creator File"
    SAVE_PROMPT = "Save Jet Creator File"
    EXPORT_ARCHIVE_PROMPT = "Save Jet Archive"
    MUST_SAVE_FIRST = "You must save your JetCreator project before exporting it."
    IMPORT_ARCHIVE_PROMPT = "Select the Jet Archive to import"
    IMPORT_ARCHIVEDIR_PROMPT = "Choose a directory:\n\nYour imported project files will be placed there."
    IMPORT_ARCHIVE_NO_JTC = "This does not appear to be a JetCreator archive file."
    IMPORT_NOT_JET_ARCHIVE = "Not a recognized Jet Archive file."
    
    #Button texts
    BUT_ADD = 'Add'
    BUT_REVISE = 'Revise'
    BUT_DELETE = 'Delete'
    BUT_PLAY = 'Play'
    BUT_STOP = 'Stop'
    BUT_MOVE = 'Move'
    BUT_QUEUEALL = 'Queue All'
    BUT_DEQUEUEALL = 'Dequeue All'
    BUT_UNMUTE = 'Un-Mute'
    BUT_MUTE = 'Mute'
    BUT_AUDITION = 'Audition'
    BUT_QUEUE = 'Queue'
    BUT_MUTEALL = 'Mute All'
    BUT_MUTENONE = 'Mute None'
    BUT_ORGMUTES = 'Original Mutes'
    BUT_CANCELANDQUEUE = 'Cancel && Queue'
    BUT_CANCELCURRENT = 'Next'
    BUT_PAUSE = 'Pause'
    BUT_RESUME = 'Resume'
    BUT_PLAYSEG = 'Play Segment'
    BUT_PLAYMIDI = 'Play MIDI File'
    
    #Grid defines
    GRD_TRACK = "Track"
    GRD_CHANNEL = "Channel"
    GRD_NAME = "Name"
    GRD_SEGMENTS = "Segments"
    GRD_LENGTH = "Length"
    GRD_QUEUE = "Queue"
    GRD_STATUS = "Status"
    
    #Menu defines
    MNU_ADD_SEG = "Add Segment"
    MNU_UPDATE_SEG = "Revise Segment"
    MNU_DELETE_SEG = "Delete Segment"
    MNU_MOVE_SEG = "Move Segment(s)"
    MNU_ADD_EVENT = "Add Event"
    MNU_UPDATE_EVENT = "Revise Event"
    MNU_DELETE_EVENT = "Delete Event"
    MNU_MOVE_EVENT = "Move Events(s)"
    MNU_UNDO = "Undo\tctrl+z"
    MNU_REDO = "Redo\tctrl+y"
    
    HLP_QUANTIZE = "The quantize element is optional and defaults to 0 if omitted.\nThis value sets a window size in ticks for the breaks in\n a segment when notes are extracted from a larger file. \nSee the section on Quantization for further detail \non the operation of this parameter."
    
    #Status bar messages
    SB_NEW = "New JET Creator file"
    SB_OPEN = "Open JET Creator file"
    SB_SAVE = "Save Jet Creator file and generate .JET output file"
    SB_SAVEAS = "Save JET Creator file as another file"
    SB_EXIT = "Exit the application"
    SB_CUT = "Cuts the current segment or event to the clipboard"
    SB_COPY = "Copies the current segment or event to the clipboard"
    SB_PASTE = "Pastes the current segment or event from the clipboard"
    SB_UNDO = "Undo the last segment or event edit."
    SB_REDO = "Reverse the last segment or event undo edit."
    SB_IMPORT_PROJ = "Imports a JetCreator project archive."
    SB_EXPORT_PROJ = "Saves all project files to an archive."
    
    #Defines the menus
    MENU_SPEC = (("&File",
                    ("&New", SB_NEW, 'OnJetNew', True),
                    ("&Open...", SB_OPEN, 'OnJetOpen', True),
                    ("&Save", SB_SAVE, 'OnJetSave', True),
                    ("Save As...", SB_SAVEAS, 'OnJetSaveAs', True),
                    ("", "", "", True),
                    ("Import Project...", SB_IMPORT_PROJ, "OnJetImportArchive", True),
                    ("Export Project...", SB_EXPORT_PROJ, "OnJetExportArchive", True),
                    ("Properties...", "Sets properties specific to this Jet project", 'OnJetProperties', True),
                    ("", "", "", True),
                    ("Exit", SB_EXIT, 'OnClose', True)),
                ("&Edit",
                    (MNU_UNDO, "Undo", 'OnUndo', False),
                    (MNU_REDO, "Redo", 'OnRedo', False),
                    ("C&ut\tctrl+x", "Cut", 'OnCut', True),
                    ("&Copy\tctrl+c", "Copy", 'OnCopy', True),
                    ("&Paste\tctrl+v", "Paste", 'OnPaste', True)),
                ("Jet",
                    ("Preferences", "Set user preferences including defaults for new project files.", 'OnPreferences', True)),
                ("Segments",
                    (MNU_ADD_SEG, "Add a new segment to the segment list", 'OnSegmentAdd', True),
                    (MNU_UPDATE_SEG, "Revise the segment attributes", 'OnSegmentUpdate', False),
                    (MNU_DELETE_SEG, "Delete the segment from the segment list", 'OnSegmentDelete', False),
                    (MNU_MOVE_SEG, "Move one or more segments by incrementing or decrementing their time values", 'OnSegmentsMove', False)),
                ("Events",
                    (MNU_ADD_EVENT, "Add a new event for the currently selected segment", 'OnEventAdd', False),
                    (MNU_UPDATE_EVENT, "Revise the current event's attributes", 'OnEventUpdate', False),
                    (MNU_DELETE_EVENT, "Delete the event from the event list for this segment", 'OnEventDelete', False),
                    (MNU_MOVE_EVENT, "Move one or more events by incrementing or decrementing their time values", 'OnEventsMove', False)),
                ("Help",
                    ("JET Creator User Manual", "Get help on the JET Creator", "OnHelpJet", True),
                    ("JET Authoring Guidelines", "Guidelines helpful for JET content creation", "OnHelpJetGuidelines", True),
                    ("About", "About the JET Creator", "OnAbout", True))
                    )

    #Define the toolbar
    TOOLBAR_SPEC = (
                    ("-", "", "", ""),
                    ("New", "img_New", SB_NEW, "OnJetNew"),
                    ("Open", "img_Open", SB_OPEN, "OnJetOpen"),
                    ("Save", "img_Save", SB_SAVE, "OnJetSave"),
                    ("-", "", "", ""),
                    ("Cut", "img_Cut", SB_CUT, "OnCut"),
                    ("Copy", "img_Copy", SB_COPY, "OnCopy"),
                    ("Paste", "img_Paste", SB_PASTE, "OnPaste"),
                    ("-", "", "", ""),
                    ("Undo", "img_Undo", SB_UNDO, "OnUndo"),
                    ("Redo", "img_Redo", SB_REDO, "OnRedo"),
                    )
 
    F_HLPBUT = "hlpButton"
    F_OK = "btnOk"
    F_CANCEL = "btnCancel"
    F_MIDIFILE = "filecmbMidiFile"
    F_DLSFILE = "filecmbDlsFile"
    F_SEGNAME = "txtSegName"
    F_START = "tmStart"
    F_END = "tmEnd"
    F_QUANTIZE = "spnQuantize"
    F_REPEAT = "spnRepeat"
    F_TRANSPOSE = "spnTranspose"
    F_MUTEFLAGS = "grd2MuteFlags"
    F_SYNCMUTE = "chkSync"
    F_ETYPE = "cmbEventType"
    F_ENAME = "txtEventName"
    F_ESTART = "tmEventStart"
    F_EEND = "tmEventEnd"
    F_EID = "spnEventID"
    F_ETRACK = "spnEventTrack"
    F_ECHANNEL = "spn1EventChannel"
    F_EEVENTID = "spnEventID"
    F_EMUTEBUT = "btnMute"
    F_ETRIGGERBUT = "btnTriggerClip"
    F_GRAPH = "graphPlay"
    F_PAUSE = "btnPause"
    F_ADDSEG = "btnAddSeg"
    F_UPDSEG = "btnUpdateSeg"
    F_DELSEG = "btnDeleteSeg"
    F_PLAY = "btnPlay"
    F_PLAYMIDI = "btnPlayMidi"
    F_EASPLAY = "btnEasPlay"
    F_ADDCLIP = "btnAddEvent"
    F_UPDCLIP = "btnUpdateEvent"
    F_DELCLIP = "btnDeleteEvent"
    F_EXPORT = "btnOkExport"
    F_JETFILENAME = "filecmbJetFileName"
    F_COPYRIGHT = "txtCopyright"
    F_JFILE = "filetxtJetFileName"
    F_JOPEN = "btnOpen"
    F_JNEW = "btnNew"
    F_JIMPORT = "btnImport"
    F_JLIST = "lstRecent"
    F_ERRGRID = "grdErrors"
    F_CHASECONTROLLERS = "chkChaseControllers"
    F_DELETEEMPTYTRACKS = "chkDeleteEmptyTracks"
    F_OPTMIDI = "optMidiGraph"
    F_OPTSEG = "optSegGraph"
    F_RDOGRAPH = "rdoboxGraphType"
    F_DISPEMPTYTRACKS = "chkDisplayEmptyTracks"
    F_GRAPHLABELS = "chkGraphLabels"
    F_GRAPHCLIPS = "chkGraphClips"
    F_GRAPHAPPEVTS = "chkGraphAppEvts"
    F_REPLICATE = "btnReplicate"
        
    GRAPH_LBLS = "Labels"
    GRAPH_TRIGGER = "Trigger Clips"
    GRAPH_APP = "App Events"
    
    #IDs for dialogs
    ID_JET_OPEN = 0
    ID_JET_NEW = 1
    ID_JET_IMPORT = 2
        
    #Event types
    E_CLIP = 'TriggerClip'
    E_EOS = 'End of Segment'
    E_APP = 'App Controller'
       
    INTWIDTH = 70
    TIMEWIDTH = 70
   
    #Definitions of fields in the edit frame
    TM_WIDTH = 100
    TRACK_MIN = 1
    TRACK_MAX = 32
    EVENTID_MIN = 1
    EVENTID_MAX = 63
    APPCONTROLLERID_MIN = 80
    APPCONTROLLERID_MAX = 83
    #NEEDS TO DEFAULT TO RANGE OF BOTH POSSIBLE TYPES
    DEFAULTID_MIN = 1
    DEFAULTID_MAX = 100
    
    #Mins and maxs for dialog values
    QUANTIZE_MIN = 0
    QUANTIZE_MAX = 9
    CHANNEL_MIN = 1
    CHANNEL_MAX = 16
    TRANSPOSE_MIN = -12
    TRANSPOSE_MAX = 12
    REPEAT_MIN = -1
    REPEAT_MAX = 100
    
    #Standardize the columns
    BUTSIZE = wx.DefaultSize
    COLSIZE = 120
    COL1 = 30
    COL2 = COL1 + COLSIZE
    COL3 = COL2 + COLSIZE
    COL4 = COL3 + COLSIZE
    COL5 = COL4 + COLSIZE
    COL6 = COL5+ COLSIZE
    COL7 = COL6 + COLSIZE
    ROWSIZE = 50
    ROW1 = 40
    ROW2 = ROW1 + ROWSIZE
    ROW3 = ROW2 + ROWSIZE
    ROW4 = ROW3 + ROWSIZE
    ROW5 = ROW4 + ROWSIZE
    ROW6 = ROW5 + ROWSIZE
    ROW7 = ROW6 + ROWSIZE
    BUTOFF = 25
    BUTROW1 = 25
    FILEPATH_GRIDWIDTH = 120
    FILEPATH_WIDTH = 250
    
    #Segment grid column definitions
    SEGMENT_GRID = [('Segment Name', 200, F_SEGNAME),
                    ('MIDI File', FILEPATH_GRIDWIDTH, F_MIDIFILE),
                    ('DLS File', FILEPATH_GRIDWIDTH, F_DLSFILE),
                    ('Start', TIMEWIDTH, F_START),
                    ('End', TIMEWIDTH, F_END),
                    ('Quantize', 0, F_QUANTIZE),
                    ('Transpose', 0, F_TRANSPOSE),
                    ('Repeat', 0, F_REPEAT),
                    ('Mute Flags', 0, F_MUTEFLAGS)
                   ]
    
    #Clips grid column definitions
    CLIPS_GRID =   [('Event Name', 200, F_ENAME),
                    ('Type', 100, F_ETYPE),
                    ('Start',TIMEWIDTH, F_ESTART),
                    ('End',TIMEWIDTH, F_EEND),
                    ('Track',0, F_ETRACK),
                    ('Channel',0, F_ECHANNEL),
                    ('EventID',0, F_EEVENTID)
                   ]
    
    #Jet open dialog control definitions
    JETOPEN_SIZE = (365+200,360)
    JETOPEN_CTRLS = [
             ('Jet Creator Files', 'frCreator', 20, 20, (234+200, 244 + ROWSIZE), 0, 0, -1, [], "", True, ""),
             ('Open', F_JOPEN, BUTROW1, COL3+200, BUTSIZE, 0, 0, ID_JET_OPEN, [], "OnOk", True, ""),
             ('New', F_JNEW, BUTROW1+BUTOFF*1, COL3+200, BUTSIZE, 0, 0, ID_JET_NEW, [], "OnNew", True, ""),
             ('Import', F_JIMPORT, BUTROW1+BUTOFF*2, COL3+200, BUTSIZE, 0, 0, ID_JET_IMPORT, [], "OnJetImport", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*3, COL3+200, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "", True, ""),
             ('', F_JFILE, ROW1, COL1, 200+200, 0, 0, -1, JTC_FILE_SPEC, "", True, ""),
             ('Recent Files', F_JLIST, ROW2, COL1, (200+200,200), 0, 0, -1, [], "", True, ""),
             ]

    #Jet properties dialog control definitions
    JET_PROPERTIES_SIZE = (465,460)
    JET_PROPERTIES_CTRLS = [
             ('Jet Project Properties', 'frProperties', 20, 20, (334, 344 + ROWSIZE), 0, 0, -1, [], "", True, ""),
             ('Ok', F_OK, BUTROW1, COL3+100, BUTSIZE, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*1, COL3+100, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "", True, ""),
             ('Jet File', F_JETFILENAME, ROW1, COL1, 300, 0, 0, -1, JTC_FILE_SPEC, "", True, ""),
             ('Copyright', F_COPYRIGHT, ROW2, COL1, 300, 0, 0, -1, [], "", True, ""),
             ('Chase Controllers', F_CHASECONTROLLERS, ROW3, COL1, 200, 0, 0, -1, [], "", True, ""),
             ('Delete Empty Tracks', F_DELETEEMPTYTRACKS, ROW4 - ROWSIZE/2, COL1, 200, 0, 0, -1, [], "", True, ""),
             ]
    
    #Preferences dialog control definitions
    PREFERENCES_SIZE = (465,460)
    PREFERENCES_CTRLS = [
             ('Preferences', 'frPreferences', 20, 20, (334, 344 + ROWSIZE), 0, 0, -1, [], "", True, ""),
             ('Ok', F_OK, BUTROW1, COL3+100, BUTSIZE, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*1, COL3+100, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "", True, ""),
             ('Copyright', F_COPYRIGHT, ROW1, COL1, 300, 0, 0, -1, [], "", True, ""),
             ('Chase Controllers', F_CHASECONTROLLERS, ROW2, COL1, 200, 0, 0, -1, [], "", True, ""),
             ('Delete Empty Tracks', F_DELETEEMPTYTRACKS, ROW3 - ROWSIZE/2, COL1, 200, 0, 0, -1, [], "", True, ""),
#             ('Use Project Directories', INI_PROJECT_DIRS, ROW1, COL1, 150, 0, 0, -1, [], "", True, ""),
             ]
    
    #Error dialog control definitions
    ERRORCOLW = 220
    ERRORDLG_SIZE = (600,400)
    ERRORDLG = [
             ('Ok', F_OK, BUTROW1, 500, BUTSIZE, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('', F_ERRGRID, BUTROW1, COL1, (200,300), 0, 0, -1, [], "", True, ""),
             ]
    
    #Event dialog control definitions
    BGR = 100
    EVT_OFFSET = 525+BGR
    EVTDLG_SIZE = (375+EVT_OFFSET,530)
    ID_MUTE = 124
    ID_MIDIFILE = 123
    ID_TRIGGERCLIP = 122
    SEGFRAME_SIZE = (500+BGR, 344 + ROWSIZE)
    TRACKGRD_SIZE = (70, SEGFRAME_SIZE[1]-50)
    GRAPH_SIZE = (760, 50)
    AUDCOL=190
    EVTDLG_CTRLS = [
             ('Segment', 'frSeg', 20, 20, SEGFRAME_SIZE, 0, 0, -1, [], "", False, ""),
             ('Segment Name', F_SEGNAME, ROW1, COL1, 200+BGR, 0, 0, -1, [], "", False, ""),
             ('MIDI File', F_MIDIFILE, ROW2, COL1, FILEPATH_WIDTH+BGR, 0, 0, ID_MIDIFILE, MIDI_FILE_SPEC, "", False, ""),
             ('DLS File', F_DLSFILE, ROW3, COL1, FILEPATH_WIDTH+BGR, 0, 0, -1, DLS_FILE_SPEC, "", False, ""),
             ('Starting M/B/T', F_START, ROW4, COL1, TM_WIDTH, 0, 0, -1, [], "", False, ""),
             ('Ending M/B/T', F_END, ROW5, COL1, TM_WIDTH, 0, 0, -1, [], "", False, ""),
             ('Quantize', F_QUANTIZE, ROW6, COL1, INTWIDTH, QUANTIZE_MIN, QUANTIZE_MAX, -1, [], "", False, HLP_QUANTIZE),
             ('Repeat', F_REPEAT, ROW4, AUDCOL, INTWIDTH, REPEAT_MIN, REPEAT_MAX, -1, [], "", False, ""),
             ('Transpose', F_TRANSPOSE, ROW5, AUDCOL, INTWIDTH, TRANSPOSE_MIN, TRANSPOSE_MAX, -1, [], "", False, ""),
             ('Event', 'frEventg', 20+EVT_OFFSET, 20, (234, 344 + ROWSIZE), 0, 0, -1, [], "", True, ""),
             ('Ok', F_ADDCLIP, BUTROW1, COL3+EVT_OFFSET, BUTSIZE, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*1, COL3+EVT_OFFSET, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "OnClose", True, ""),
             ('Replicate', F_REPLICATE, BUTROW1+BUTOFF*2, COL3+EVT_OFFSET, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "OnReplicate", True, ""),
             ('Event Name', F_ENAME, ROW1, COL1+EVT_OFFSET, 200, 0, 0, -1, [], "", True, ""),
             ('Event Type', F_ETYPE, ROW2, COL1+EVT_OFFSET, 120, 0, 0, -1, [E_CLIP, E_EOS, E_APP], "OnEventSelect", True, ""),
             ('Starting M/B/T', F_ESTART, ROW3, COL1+EVT_OFFSET, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Ending M/B/T', F_EEND, ROW4, COL1+EVT_OFFSET, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Track', F_ETRACK, ROW5, COL1+EVT_OFFSET, INTWIDTH, TRACK_MIN, TRACK_MAX, -1, [], "", True, ""),
             ('Track Mutes', F_MUTEFLAGS, ROW1, COL3 + 15+BGR, TRACKGRD_SIZE, 0, 0, -1, [], "", False, ""),
             ('Channel', F_ECHANNEL, ROW6, COL1+EVT_OFFSET, INTWIDTH, CHANNEL_MIN, CHANNEL_MAX, -1, [], "", True, ""),
             ('EventID', F_EEVENTID, ROW7, COL1+EVT_OFFSET, INTWIDTH, DEFAULTID_MIN, DEFAULTID_MAX, -1, [], "", True, ""),
             ('Play', F_PLAY, BUTROW1+BUTOFF*4, COL3+EVT_OFFSET, BUTSIZE, 0, 0, -1, [], "OnPlay", True, ""),
             ('Trigger', F_ETRIGGERBUT, BUTROW1+BUTOFF*5, COL3+EVT_OFFSET, BUTSIZE, 0, 0, ID_TRIGGERCLIP, [], "OnTriggerClip", False, ""),
             ('Un-Mute', F_EMUTEBUT, BUTROW1+BUTOFF*6, COL3+EVT_OFFSET, BUTSIZE, 0, 0, ID_MUTE, [], "OnMute", False, ""),
             ('Pause', F_PAUSE, BUTROW1+BUTOFF*7, COL3+EVT_OFFSET, BUTSIZE, 0, 0, -1, [], "OnPause", False, ""),
             ('Graph', F_GRAPH, 430, 20, (EVTDLG_SIZE[0]-40,60), 0, 0, -1, [], "", True, ""),
             (GRAPH_LBLS, F_GRAPHLABELS, (BUTROW1+BUTOFF*10)+70, COL3+EVT_OFFSET+5, 200, 0, 0, -1, [], "OnSetGraphOptions", True, ""),
             (GRAPH_APP, F_GRAPHCLIPS, (BUTROW1+BUTOFF*10)+90, COL3+EVT_OFFSET+5, 200, 0, 0, -1, [], "OnSetGraphOptions", True, ""),
             (GRAPH_TRIGGER, F_GRAPHAPPEVTS, (BUTROW1+BUTOFF*10)+110, COL3+EVT_OFFSET+5, 200, 0, 0, -1, [], "OnSetGraphOptions", True, ""),
             ("Graph", "boxGraph", (BUTROW1+BUTOFF*10)+45, COL3+EVT_OFFSET, (90,95), 0, 0, -1, [], "", True, ""),

          ]
      
    #Segment dialog control definitions
    BGR = 100
    AUDCOL = 560
    COLADD = 500 + BGR
    SEGDLG_SIZE = (890+BGR,530)
    SEGFRAME_SIZE = (375+BGR, 394)
    AUDFRAME_SIZE = (350, 394)
    TRACKGRD_SIZE = (200, AUDFRAME_SIZE[1]-60)
    MUTEGRD_TRACK = 50
    MUTEGRD_CHANNEL = 60
    MUTEGRD_NAME = 100
    BIGBUT = (100, 25)
    FILEPATH_WIDTH = 350
    SEGDLG_CTRLS = [
             ('Segment', 'frSeg', 20, 20, SEGFRAME_SIZE, 0, 0, -1, [], "", True, ""),
             ('Audition', 'frAudition', SEGFRAME_SIZE[0]+30, 20, AUDFRAME_SIZE, 0, 0, -1, [], "", True, ""),
             ('Segment Name', F_SEGNAME, ROW1, COL1, 200+BGR, 0, 0, -1, [], "", True, ""),
             ('MIDI File', F_MIDIFILE, ROW2, COL1, FILEPATH_WIDTH+BGR, 0, 0, ID_MIDIFILE, MIDI_FILE_SPEC, "", True, ""),
             ('DLS File', F_DLSFILE, ROW3, COL1, FILEPATH_WIDTH+BGR, 0, 0, -1, DLS_FILE_SPEC, "", True, ""),
             ('Starting M/B/T', F_START, ROW4, COL1, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Ending M/B/T', F_END, ROW5, COL1, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Quantize', F_QUANTIZE, ROW6, COL1, INTWIDTH, QUANTIZE_MIN, QUANTIZE_MAX, -1, [], "", True, HLP_QUANTIZE),
             ('Repeat', F_REPEAT, ROW1, AUDCOL+100+BGR, INTWIDTH, REPEAT_MIN, REPEAT_MAX, -1, [], "", True, ""),
             ('Transpose', F_TRANSPOSE, ROW2, AUDCOL+100+BGR, INTWIDTH, TRANSPOSE_MIN, TRANSPOSE_MAX, -1, [], "", True, ""),
             ('Track Mutes', F_MUTEFLAGS, ROW1, COL3 + 145+BGR, TRACKGRD_SIZE, 0, 0, -1, [], "", True, ""),
             ('Display Empty Tracks', F_DISPEMPTYTRACKS, ROW1+TRACKGRD_SIZE[1]+20, COL3 + 145+BGR, 200, 0, 0, -1, [], "OnSetTrackDisplayOption", True, ""),
             ('Ok', F_ADDSEG, BUTROW1, COL3 + COLADD, BIGBUT, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*1, COL3 + COLADD, BIGBUT, 0, 0, wx.ID_CANCEL, [], "OnClose", True, ""),
             ('Replicate', F_REPLICATE, BUTROW1+BUTOFF*2, COL3 + COLADD, BIGBUT, 0, 0, wx.ID_CANCEL, [], "OnReplicate", True, ""),

             ('Play Segment', F_PLAY, BUTROW1+BUTOFF*4, COL3 + COLADD, BIGBUT, 0, 0, -1, [], "OnPlay", True, ""),
             ('Play MIDI File', F_PLAYMIDI, BUTROW1+BUTOFF*5, COL3 + COLADD, BIGBUT, 0, 0, -1, [], "OnPlayMidi", True, ""),
             ('Pause', F_PAUSE, BUTROW1+BUTOFF*6, COL3 + COLADD, BIGBUT, 0, 0, -1, [], "OnPause", False, ""),
             ('Graph', F_GRAPH, 430, 20, (SEGDLG_SIZE[0]-40,60), 0, 0, -1, [], "", True, ""),
             ('Graph', F_RDOGRAPH, (BUTROW1+BUTOFF*10), COL3 + COLADD, (100,140), 0, 0, -1, ["MIDI File", "Segment"], "OnSetGraphType", True, ""),

             (GRAPH_LBLS, F_GRAPHLABELS, (BUTROW1+BUTOFF*10)+70, COL3 + COLADD+5, 200, 0, 0, -1, [], "OnSetGraphOptions", True, ""),
             (GRAPH_APP, F_GRAPHCLIPS, (BUTROW1+BUTOFF*10)+90, COL3 + COLADD+5, 200, 0, 0, -1, [], "OnSetGraphOptions", True, ""),
             (GRAPH_TRIGGER, F_GRAPHAPPEVTS, (BUTROW1+BUTOFF*10)+110, COL3 + COLADD+5, 200, 0, 0, -1, [], "OnSetGraphOptions", True, ""),
             ]        


    REPLICATE_MAX = 999
    F_RPINCREMENT = "tmIncrement"
    F_RPGRDPREVIEW = "grdPreview"
    F_RPPREFIX = "txtPrefix"
    F_RPREPLACE = "chkReplaceMatching"
    F_RPMOVE = "chkMoveMatching"
    F_RPNUMBER = "spnNumber"
    F_RPBUT = "btnPreview"
    REPLICATE_GRID =   [('Event Name', 200, F_ENAME),
                        ('Start',TIMEWIDTH, F_ESTART),
                        ('End',TIMEWIDTH, F_EEND)
                        ]
    REPLICATE_SIZE = (515,550)
    REPLICATEGRID_SIZE = (350,310)
    REPLICATE_CTRLS = [
             ('Replicate', 'frRep', 20, 20, (384, 480), 0, 0, -1, [], "", True, ""),
             ('Ok', F_OK, BUTROW1, COL3+150, BUTSIZE, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*1, COL3+150, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "", True, ""),
             ('Preview', F_RPBUT , BUTROW1+BUTOFF*2, COL3+150, BUTSIZE, 0, 0, -1, [], "OnPreview", True, ""),
             ('Name Prefix', F_RPPREFIX, ROW1, COL1, 300, 0, 0, -1, [], "", True, ""),
             ('Replace Existing Items Matching Prefix', F_RPREPLACE, ROW3, COL1, 200, 0, 0, -1, [], "", True, ""),
             ('Preview', F_RPGRDPREVIEW, ROW4-20, COL1, REPLICATEGRID_SIZE, 0, 0, -1, [], "", True, ""),
             ('Starting M/B/T', F_ESTART, ROW2, COL1, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Increment M/B/T', F_RPINCREMENT, ROW2, COL2+20, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Number', F_RPNUMBER, ROW2, COL3+40, INTWIDTH, 1, REPLICATE_MAX, -1, [], "", True, ""),
             ]
    
    
    MOVE_SIZE = (350,390)
    MOVE_CTRLS = [
             ('Move', 'frRep', 20, 20, (384, 480), 0, 0, -1, [], "", True, ""),
             ('Ok', F_OK, BUTROW1, COL3+150, BUTSIZE, 0, 0, wx.ID_OK, [], "OnOk", True, ""),
             ('Cancel', F_CANCEL, BUTROW1+BUTOFF*1, COL3+150, BUTSIZE, 0, 0, wx.ID_CANCEL, [], "", True, ""),
             ('Preview', F_RPBUT , BUTROW1+BUTOFF*2, COL3+150, BUTSIZE, 0, 0, -1, [], "OnPreview", True, ""),
             ('Starting M/B/T', F_ESTART, ROW1, COL1, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Increment M/B/T', F_RPINCREMENT, ROW1, COL2+20, TM_WIDTH, 0, 0, -1, [], "", True, ""),
             ('Preview', F_RPGRDPREVIEW, ROW2, COL1, MOVE_SIZE, 0, 0, -1, [], "", True, ""),
             ]
   
if __name__ == '__main__':
    jd = JetDefs()
    jd.CreateHelpIniFile()
