"""
 File:  
 JetStatusEvent.py
 
 Contents and purpose:
 Creates an event for postevent callbacks
 
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

EVT_JET_STATUS_ID = wx.NewId()

def EVT_JET_STATUS(win, func):
    win.Connect(-1, -1, EVT_JET_STATUS_ID, func)

class JetStatusEvent(wx.PyEvent):
    """Used for posting events out of play thread back to UI"""
    def __init__(self, mode, data):
        wx.PyEvent.__init__(self)
        self.SetEventType(EVT_JET_STATUS_ID)
        self.mode = mode
        self.data = data


