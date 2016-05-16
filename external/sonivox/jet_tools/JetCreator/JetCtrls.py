"""
 File:  
 JetCtrls.py
 
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

import wx
import sys

from wx.lib.mixins.listctrl import CheckListCtrlMixin, ListCtrlAutoWidthMixin, ColumnSorterMixin
from JetUtils import *
from JetDefs import *

class JetSpin(wx.SpinCtrl):
    """ Spin control """
    def __init__(self, parent, id=-1,value=wx.EmptyString,pos=wx.DefaultPosition,size=wx.DefaultSize,style=wx.SP_ARROW_KEYS,min=0,max=100,initial=0):
        wx.SpinCtrl.__init__(self, parent, id=id,value=value,pos=(pos[0]-MacOffset(),pos[1]),size=size,style=style,min=min,max=max,initial=initial)

    def SetValue(self, val):
        try:
            if type(val).__name__=='str':
                wx.SpinCtrl.SetValue(self, int(val))
            else:
                wx.SpinCtrl.SetValue(self, val)
        except:
            wx.SpinCtrl.SetValue(self, 0)
            
class JetSpinOneBased(JetSpin):
    """ Spin control that's one based """
    def __init__(self, parent, id=-1,value=wx.EmptyString,pos=wx.DefaultPosition,size=wx.DefaultSize,style=wx.SP_ARROW_KEYS,min=0,max=100,initial=0):
        wx.SpinCtrl.__init__(self, parent, id=id,value=value,pos=(pos[0]-MacOffset(),pos[1]),size=size,style=style,min=min,max=max,initial=initial)

    def SetValue(self, val):
        try:
            if type(val).__name__=='str':
                wx.SpinCtrl.SetValue(self, int(val) + 1)
            else: 
                wx.SpinCtrl.SetValue(self, val + 1)
        except:
            wx.SpinCtrl.SetValue(self, 1)
            
    def GetValue(self):
        val = wx.SpinCtrl.GetValue(self)
        val = val - 1
        return val
            
class JetCheckBox(wx.CheckBox):
    """ Checkbox control """
    def __init__(self, parent, id=-1,label=wx.EmptyString,pos=wx.DefaultPosition,size=wx.DefaultSize):
        wx.CheckBox.__init__(self, parent, id=id, label=label, pos=pos, size=size)
        
    def SetValue(self, val):
        try:
            if type(val).__name__=='str':
                if val == 'True':
                    val = True
                else:
                    val = False
                wx.CheckBox.SetValue(self, val)
            else:
                wx.CheckBox.SetValue(self, val)
        except:
            wx.CheckBox.SetValue(self, False)
             
class JetRadioButton(wx.RadioButton):
    """ Radio button control """
    def __init__(self, parent, id=-1,label=wx.EmptyString,pos=wx.DefaultPosition,size=wx.DefaultSize):
        wx.RadioButton.__init__(self, parent, id=id, label=label, pos=pos, size=size)
        
    def SetValue(self, val):
        try:
            if type(val).__name__=='str':
                if val == 'True':
                    val = True
                else:
                    val = False
                wx.RadioButton.SetValue(self, val)
            else:
                wx.RadioButton.SetValue(self, val)
        except:
            wx.RadioButton.SetValue(self, False)
             
class JetListCtrl(wx.ListCtrl, ListCtrlAutoWidthMixin, ColumnSorterMixin):
    """ List control """
    def __init__(self, parent, id=-1, pos=wx.DefaultPosition, size=wx.DefaultSize):
        wx.ListCtrl.__init__(self, parent, id, pos=pos, size=size, style=wx.LC_REPORT | wx.SUNKEN_BORDER)
        ListCtrlAutoWidthMixin.__init__(self)
        self.iCol = 0
        self.iWidth = 0
        self.OnSortOrderChangedAlert = None
        self.iInitialized = False

    def AddCol(self, title, width):
        self.InsertColumn(self.iCol, title)
        if width > 0:
            self.SetColumnWidth(self.iCol, width)
        else:
            width = self.GetColumnWidth(self.iCol)
        self.iCol += 1
        self.iWidth = self.iWidth + width
        self.SetSize((self.iWidth + 10, -1))

    def AddRows(self, values):
        for value in values:
            iCol = 0
            for row in value:
                if iCol == 0:
                    index = self.InsertStringItem(sys.maxint, row)
                else:
                    self.SetStringItem(index, iCol, row) 
                iCol = iCol + 1

    # Used by the ColumnSorterMixin, see wx/lib/mixins/listctrl.py
    def GetListCtrl(self):
        return self
    
    def InitSorting(self, cols):
        if not self.iInitialized:
            ColumnSorterMixin.__init__(self, cols)
            self.iInitialized = True
        
    def OnSortOrderChanged(self):
        if self.OnSortOrderChangedAlert is not None:
            self.OnSortOrderChangedAlert()

    def __OnColClick(self, evt):
        oldCol = self._col
        self._col = col = evt.GetColumn()
        self._colSortFlag[col] = int(not self._colSortFlag[col])
        self.OnSortOrderChanged()
        
class JetCheckListCtrl(wx.ListCtrl, CheckListCtrlMixin, ListCtrlAutoWidthMixin, ColumnSorterMixin):
    """ List control with checkboxes on each line """
    def __init__(self, parent, id=-1, pos=wx.DefaultPosition, size=wx.DefaultSize, style=wx.LC_REPORT | wx.SUNKEN_BORDER):
        wx.ListCtrl.__init__(self, parent, id, pos=pos, size=size, style=style)
        CheckListCtrlMixin.__init__(self)
        ListCtrlAutoWidthMixin.__init__(self)

        self.iCol = 0
        self.iWidth = 0
        self.OnSortOrderChangedAlert = None
        self.iInitialized = False

    def AddCol(self, title, width):
        self.InsertColumn(self.iCol, title)
        if width > 0:
            self.SetColumnWidth(self.iCol, width)
        else:
            width = self.GetColumnWidth(self.iCol)
        self.iCol += 1
        self.iWidth = self.iWidth + width
        self.SetSize((self.iWidth + 10, -1))

    def OnCheckItem(self, index, flag):
        if hasattr(self, 'BindCheckBoxFct'):
            self.BindCheckBoxFct(index, flag)
        
    def BindCheckBox(self, fct):
        self.BindCheckBoxFct = fct

    def AddRows(self, values):
        for value in values:
            iCol = 0
            for row in value:
                if iCol == 0:
                    index = self.InsertStringItem(sys.maxint, row)
                else:
                    self.SetStringItem(index, iCol, row) 
                iCol = iCol + 1

    # Used by the ColumnSorterMixin, see wx/lib/mixins/listctrl.py
    def GetListCtrl(self):
        return self
    
    def InitSorting(self, cols):
        if not self.iInitialized:
            ColumnSorterMixin.__init__(self, cols)
            self.iInitialized = True
     
    def OnSortOrderChanged(self):
        if self.OnSortOrderChangedAlert is not None:
            self.OnSortOrderChangedAlert()
        
    def __OnColClick(self, evt):
        oldCol = self._col
        self._col = col = evt.GetColumn()
        self._colSortFlag[col] = int(not self._colSortFlag[col])
        self.OnSortOrderChanged()

class JetTrackCtrl(JetCheckListCtrl):
    """ List control specifically designed to show tracks in midi file """
    def __init__(self, parent, id=-1, pos=wx.DefaultPosition, size=wx.DefaultSize, style=wx.LC_REPORT | wx.SUNKEN_BORDER):
        wx.ListCtrl.__init__(self, parent, id, pos=pos, size=size, style=style)
        CheckListCtrlMixin.__init__(self)
        ListCtrlAutoWidthMixin.__init__(self)

        self.iCol = 0
        self.iWidth = 0
        self.muteFlags = 0

    def SetValue(self, muteFlags):
        self.muteFlags = muteFlags
        
    def GetValue(self):
        return self.muteFlags

    def CheckTracks(self, muteFlags):
        num = self.GetItemCount()
        for iRow in range(num):
            track_num = self.GetTrackNumber(iRow)
            self.CheckItem(iRow, GetMute(track_num, muteFlags))
            
    def AddTrackRow(self, track, loadEmpty=False):
        if loadEmpty or not track.empty: 
            index = self.InsertStringItem(sys.maxint, str(track.track))
            self.SetStringItem(index, 1, str(track.channel))
            self.SetStringItem(index, 2, str(track.name))
               
    def GetTrackNumber(self, index):
        return getColumnValue(self, index, 0)
                          
class JetFileCombo():
    """ Combo box with file open button """
    def __init__(self, parent, pos=(0,0), size=(200,-1), title='Open File', spec='*.*', id=-1):
        self.spec = spec
        self.title = title
        self.EventFire = False
        BUTWIDTH = 20
        BORDER = 5
        w = size[0] - (BUTWIDTH + BORDER)
        col = pos[0] + w + BORDER
        
        self.cmb = wx.ComboBox(parent, id, "", pos=(pos[0]-MacOffset(),pos[1]), size=(w, -1), style=wx.CB_DROPDOWN)
        self.btn = wx.Button(parent, -1, "...", pos=(col, pos[1]+MacOffset()), size=(BUTWIDTH,self.cmb.GetSize()[1]))
        self.btn.Bind(wx.EVT_BUTTON, self.OnBrowse, self.btn) 

    def OnBrowse(self, event):
        os = __import__('os')
        defDir = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, self.spec, 'str', str(os.getcwd()))
        if OsWindows():
            defDir = defDir.replace('/','\\')
        else:
            defDir = defDir.replace('\\', '/')
            
        dlg = wx.FileDialog(None, self.title, defDir, '', self.spec, wx.FD_OPEN)
        ret = dlg.ShowModal()
        if ret == wx.ID_OK:
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, self.spec, str(FileJustPath(dlg.GetPath())))
            val = dlg.GetPath()

            self.Append(val)
            self.cmb.SetValue(val)
            
            if self.EventFire:
                SendEvent(self.cmb, wx.EVT_COMBOBOX.evtType[0])
        dlg.Destroy()
        
    def SetEventFire(self, fire):
        self.EventFire = fire
        
    def GetValue(self):
        return StrNoneChk(self.cmb.GetValue())   
    
    def SetValue(self, val):
        try:
            self.cmb.SetValue(val)
        except:
            pass

    def Append(self, val):
        try:
            self.cmb.Append(val)
        except:
            pass
        
    def SetFocus(self):
        self.cmb.SetFocus()
        
    def SetListValues(self, list):
        self.cmb.AppendItems(list)
        
    def Enable(self, enable):
        self.cmb.Enable(enable)
        self.btn.Enable(enable)
                
    def SetHelpText(self, Lbl):
        self.cmb.SetHelpText(Lbl)
        self.btn.SetHelpText(Lbl)
        
class JetFileText():
    """ Capture a filename with a button to browse for a file """
    def __init__(self, parent, pos=(0,0), size=(200,-1), title='Open File', spec='*.*', id=-1):
        self.spec = spec
        self.title = title
        BUTWIDTH = 20
        BORDER = 5
        w = size[0] - (BUTWIDTH + BORDER)
        col = pos[0] + w + BORDER
        
        self.txt = wx.TextCtrl(parent, id, "", pos=(pos[0]-MacOffset(),pos[1]), size=(w, -1))
        self.btn = wx.Button(parent, -1, "...", pos=(col, pos[1]), size=(BUTWIDTH,self.txt.GetSize()[1]))
        self.btn.Bind(wx.EVT_BUTTON, self.OnBrowse, self.btn) 
            
    def OnBrowse(self, event):
        os = __import__('os')
        defDir = IniGetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, self.spec, 'str', str(os.getcwd()))
        if OsWindows():
            defDir = defDir.replace('/','\\')
        else:
            defDir = defDir.replace('\\', '/')
        
        dlg = wx.FileDialog(None, self.title, defDir, '', self.spec, wx.FD_OPEN)
        ret = dlg.ShowModal()
        if ret == wx.ID_OK:
            IniSetValue(JetDefs.JETCREATOR_INI, JetDefs.INI_DEFAULTDIRS, self.spec, str(FileJustPath(dlg.GetPath())))
            val = dlg.GetPath()
            self.txt.SetValue(val)
        dlg.Destroy()
        
    def GetValue(self):
        return StrNoneChk(self.txt.GetValue())   
    
    def SetValue(self, val):
        try:
            self.txt.SetValue(val)
        except:
            pass

    def Append(self, val):
        try:
            self.txt.Append(val)
        except:
            pass
        
    def SetFocus(self):
        self.txt.SetFocus()

    def Enable(self, enable):
        self.txt.Enable(enable)
        self.btn.Enable(enable)

    def SetHelpText(self, Lbl):
        self.txt.SetHelpText(Lbl)
        self.btn.SetHelpText(Lbl)
        
def YesNo(title, question, default):
    """ Simple Yes/No question box """
    dlg = wx.MessageDialog(None, question, title, wx.YES_NO | wx.ICON_QUESTION) 
    if dlg.ShowModal() == wx.ID_YES:
        result = True
    else:
        result = False
    dlg.Destroy()
    return result

def YesNoCancel(title, question, default):
    """ Simple Yes/No question box """
    dlg = wx.MessageDialog(None, question, title, wx.YES_NO | wx.CANCEL | wx.ICON_QUESTION)
    result = dlg.ShowModal() 
    dlg.Destroy()
    return result

def ErrorMsg(title, message):
    """ Dipslay an error message """
    dlg = wx.MessageDialog(None, message, title, wx.ICON_ERROR) 
    dlg.ShowModal()
    dlg.Destroy()

def InfoMsg(title, message):
    """ Displays an informational message """
    dlg = wx.MessageDialog(None, message, title, wx.ICON_INFORMATION) 
    dlg.ShowModal()
    dlg.Destroy()

class TimeCtrl(wx.Frame):
    """ Combination of controls to capture measure, beat, tick times """
    def __init__(self, parent, pos=(0,0), minimums=(1,1,0), maximums=(999,4,480), value=JetDefs.MBT_DEFAULT, ctlName=''):
        wx.Frame.__init__(self, parent, -1)
        
        self.ChangeCallbackFct = None
        self.ctlName = ctlName
        self.mx = maximums
        self.mn = minimums
        self.maxTicks = 0
        self.iCtrl = 0
        p1 = pos[0]
        top = pos[1] + MacOffset()
        w1 = 30
        self.time = (wx.TextCtrl(parent, -1, str(value[0]), pos=(p1, top), size=(w1, -1), style=wx.TE_NOHIDESEL),
                wx.TextCtrl(parent, -1, str(value[1]), pos=(p1 + (w1 + 3), top), size=(w1, -1), style=wx.TE_NOHIDESEL),
                wx.TextCtrl(parent, -1, str(value[2]), pos=(p1 + (w1 + 3) *2, top), size=(w1, -1), style=wx.TE_NOHIDESEL),
                )
        h = self.time[2].GetSize().height
        w = self.time[2].GetSize().width + self.time[2].GetPosition().x + 8
        
        self.spin = wx.SpinButton(parent, -1, (w, top), (h*2/3, h), wx.SP_VERTICAL)
        self.spin.SetValue(1)
        self.spin.SetRange(-999,999)
        
        self.spin.Bind(wx.EVT_SPIN_UP, self.OnSpinUp, self.spin)
        self.spin.Bind(wx.EVT_SPIN_DOWN, self.OnSpinDown, self.spin)
        
        self.time[0].Bind(wx.EVT_SET_FOCUS, self.OnFocusMeasure, self.time[0] )
        self.time[1].Bind(wx.EVT_SET_FOCUS, self.OnFocusBeat, self.time[1] )
        self.time[2].Bind(wx.EVT_SET_FOCUS, self.OnFocusTick, self.time[2] ) 
                
        self.time[0].Bind(wx.EVT_KILL_FOCUS, self.OnChangeVal, self.time[0] )
        self.time[1].Bind(wx.EVT_KILL_FOCUS, self.OnChangeVal, self.time[1] )
        self.time[2].Bind(wx.EVT_KILL_FOCUS, self.OnChangeVal, self.time[2] )         

        self.SetValue(value)

    def UnBindKillFocus(self):
        self.time[0].Unbind(wx.EVT_KILL_FOCUS, self.time[0])
        self.time[1].Unbind(wx.EVT_KILL_FOCUS, self.time[1])
        self.time[2].Unbind(wx.EVT_KILL_FOCUS, self.time[2])         
                
    def SetChangeCallbackFct(self, ChangeCallbackFct):
        self.ChangeCallbackFct = ChangeCallbackFct
        
    def OnChangeVal(self, event=None):
        if not OsWindows():
            self.time[self.iCtrl].SetSelection(-1,-1)
        if int(self.time[self.iCtrl].GetValue()) > self.mx[self.iCtrl]:
            self.time[self.iCtrl].SetValue(str(self.mx[self.iCtrl]))
        if int(self.time[self.iCtrl].GetValue()) < self.mn[self.iCtrl]:
            self.time[self.iCtrl].SetValue(str(self.mn[self.iCtrl]))
        if self.ChangeCallbackFct is not None:
            self.ChangeCallbackFct()
        if event is not None:
            event.Skip()
        
    def OnSpinUp(self, event):
        if int(self.time[self.iCtrl].GetValue()) < self.mx[self.iCtrl]:
            self.time[self.iCtrl].SetValue(str(int(self.time[self.iCtrl].GetValue()) + 1))
            self.OnChangeVal()
                                             
    def OnSpinDown(self, event):
        if int(self.time[self.iCtrl].GetValue()) > self.mn[self.iCtrl]:
            self.time[self.iCtrl].SetValue(str(int(self.time[self.iCtrl].GetValue()) - 1))
            self.OnChangeVal()
                                               
    def OnFocusMeasure(self, event):
        self.iCtrl = 0

    def OnFocusBeat(self, event):
        self.iCtrl = 1

    def OnFocusTick(self, event):
        self.iCtrl = 2
        
    def SetValue(self, mbt):
        try:
            if type(mbt).__name__=='str' or type(mbt).__name__=='unicode':
                mbt = ConvertStrTimeToTuple(mbt)
            mbt = mbtFct(mbt, 1)
            self.time[0].SetValue(str(mbt[0]))
            self.time[1].SetValue(str(mbt[1]))
            self.time[2].SetValue(str(mbt[2]))
        except:
            self.time[0].SetValue(str(self.mn[0]))
            self.time[1].SetValue(str(self.mn[1]))
            self.time[2].SetValue(str(self.mn[2]))
        if not OsWindows():
            self.time[0].SetSelection(-1,-1)
            self.time[1].SetSelection(-1,-1)
            self.time[2].SetSelection(-1,-1)
           
    def GetValue(self, typ='str'):
        try:
            if typ == 'str':
                ret = "%d:%d:%d" % (int(self.time[0].GetValue()), int(self.time[1].GetValue()), int(self.time[2].GetValue()))
            else:
                ret = (int(self.time[0].GetValue()), int(self.time[1].GetValue()), int(self.time[2].GetValue()))
        except:
            ret = self.minimums
        return mbtFct(ret, -1)

    def Enable(self, enable):
        self.time[0].Enable(enable)
        self.time[1].Enable(enable)
        self.time[2].Enable(enable)
        self.spin.Enable(enable)

    def SetFocus(self):
        self.time[0].SetFocus()
        
    def SetMaxMbt(self, m, b, t):
        self.mx = (m,b,t)

    def GetMaxMbt(self):
        return "%d:%d:%d" % self.mx

    def SetMinMbt(self, m, b, t):
        self.mn = (m,b,t)
          
    def SetMaxTicks(self, maxTicks): 
        self.maxTicks = maxTicks
        
    def GetMaxTicks(self): 
        return self.maxTicks
    
    def SetHelpText(self, Lbl):
        self.spin.SetHelpText(Lbl)
        self.time[0].SetHelpText(Lbl)
        self.time[1].SetHelpText(Lbl)
        self.time[2].SetHelpText(Lbl)
        
    def GetMeasure(self):
        return int(self.time[0].GetValue())
    
    def GetBeat(self):
        return int(self.time[1].GetValue())
    
    def GetTick(self):
        return int(self.time[2].GetValue())
 
if __name__ == '__main__':
    """ Test code for controls """
    class TestFrame(wx.Frame):
        def __init__(self, parent, id, title):
            wx.Frame.__init__(self, parent, id, title, size=(350, 220))
    
            panel = wx.Panel(self, -1)
            
            self.tc = TimeCtrl(panel, pos=(30, 20), maximums=(25, 4, 120), value=(2, 3, 4))
            #tc.Enable(True)
            #tc.SetValue((2,3,4))
            #tc.SetValue("1:2:3")
            #print(tc.GetValue())
            
            js = JetSpin(panel, -1, pos=(30, 100))
            js.SetValue("1")
            #js.SetValue(1)
            
            #fl = JetFileCombo(panel)

            wx.EVT_CLOSE(self, self.OnClose)
            
            self.Centre()
            self.Show(True)
    
        def OnClose(self, event):
            self.tc.UnBindKillFocus()
            self.Destroy()
            
    app = wx.App(None)
    TestFrame(None, -1, 'TestFrame')
    app.MainLoop()

    
