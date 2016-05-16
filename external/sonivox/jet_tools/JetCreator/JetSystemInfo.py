"""
 File:  
 JetSystemInfo.py
 
 Contents and purpose:
 Displays the system info regarding os version, wxPython version, and Python version
 
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

import sys
import os
import string
import wx

print("")
print("wxPython Version:")
print(wx.__version__)

print("")
print("Python Version:")
print(sys.version)

print("")
print("Python Paths:")
for dir in string.split(os.environ['PYTHONPATH'], os.pathsep):
    print dir

