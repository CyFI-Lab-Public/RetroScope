"""
 File:  
 JetHelp.py
 
 Contents and purpose:
 Displays the help text
 
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
import wx.html
from JetDefs import *

app = wx.PySimpleApp()
frame = wx.Frame(None, -1, JetDefs.MAIN_HELPTITLE, size=(800,600))
html1 = wx.html.HtmlWindow(frame, -1)
html1.LoadPage(JetDefs.MAIN_HELPFILE)
frame.Center()
frame.Show()
app.MainLoop()
