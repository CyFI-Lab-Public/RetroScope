"""
 File:  
 JetSegGraph.py
 
 Contents and purpose:
 Draws the event graph and progress bar
 
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


import  wx
import logging

from JetUtils import *
from JetDefs import *

GRAPH_COLORS = [
                '#C0E272',
                '#85CF89',
                '#CF9683',
                '#749EDE',
                '#9FB5B1',
                '#B095BF',
                '#FE546D',
                '#B3BB97',
                '#FFFFB8',
                
                ]

PROGRESS_BAR = '#0000CC'
EOS_BAR = '#095000'
APP_BAR = '#B3BB97'

                    
class Marker():
    """ Defines portions of the graph for events """
    def __init__(self, sEventType, iEventId, sName, sStartMbt, sEndMbt, iStartMeasure, ppqn):
        self.sEventType = sEventType
        self.iEventId = iEventId
        self.sName = sName
        self.StartMbt = ConvertStrTimeToTuple(sStartMbt)
        self.EndMbt = ConvertStrTimeToTuple(sEndMbt)
        self.iStartMeasure = iStartMeasure
        self.iStart = 0
        self.iEnd = 0
        self.iWidth = 0
        self.iHeight = 0
        self.iTop = 0
        self.iUpdate = False  
        self.sColor = '#FFFFB8'  
        self.ppqn = ppqn
        self.isDirty = False
                
    def CalcCoord(self, step, height, ColorFct):
        """ Calculates the coordinates in pixels for graphing the shaded regions """
        #measures
        iStartM = self.StartMbt[0] - self.iStartMeasure
        iEndM = self.EndMbt[0] - self.iStartMeasure
        self.iStart = step * iStartM
        self.iEnd = step * iEndM
        #beats
        self.iStart = self.iStart + ((step / 4.0) * (self.StartMbt[1]-1))
        self.iEnd = self.iEnd + ((step / 4.0) * (self.EndMbt[1]-1))
        #ticks
        pctTickOfBeat = (float(self.StartMbt[2]) / float(self.ppqn))
        self.iStart = self.iStart + ((pctTickOfBeat * (step / 4.0)))
        pctTickOfBeat = (float(self.EndMbt[2]) / float(self.ppqn))
        self.iEnd = self.iEnd + ((pctTickOfBeat * (step / 4.0)))
        
        self.iWidth = self.iEnd - self.iStart
        
        self.iHeight = height
        self.sColor = ColorFct()
        self.iUpdate = False  

class SegmentGraph(wx.Panel):
    """ Draws the player graph bar """
    def __init__(self, parent, pos=wx.DefaultPosition, size=wx.DefaultSize, ClickCallbackFct=None, showLabels=True, showClips=True, showAppEvts=True):
        wx.Panel.__init__(self, parent, -1, pos=pos, size=size, style=wx.BORDER_STATIC)
        self.iLocationInMs = 0
        self.iLengthInMs = 0
        self.iLengthInMeasures = 0
        self.iMarkerTop = 15
        self.iScaleTop = 0
        self.iEdges = 5
        self.iStartMeasure = 0
        self.iMidiMode = False
        self.ClickCallbackFct = ClickCallbackFct
        self.iColor = 0
        self.showLabels = showLabels
        self.showClips = showClips
        self.showAppEvts = showAppEvts
        
        self.font = wx.Font(8, wx.FONTFAMILY_DEFAULT, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_NORMAL, False, 'Courier')

        self.Markers = []
        self.SetBackgroundStyle(wx.BG_STYLE_CUSTOM)
        self.Bind(wx.EVT_PAINT, self.OnPaint)
        self.Bind(wx.EVT_SIZE, self.OnSize)
        self.Bind(wx.EVT_LEFT_DOWN, self.OnLeftDown)

        #initialize buffer
        self.OnSize(None)
        
    def ClearGraph(self):
        """ Clears the graph values """
        self.iLocationInMs = 0
        self.iLengthInMs = 0
        self.iLengthInMeasures = 0
        self.iMarkerTop = 15
        self.iScaleTop = 0
        self.iEdges = 5
        self.iStartMeasure = 0
        self.iMidiMode = False
        self.iColor = 0        
        self.Markers = []
        self.iLocationInMs = 0
        self.DoDrawing()
        
    def LoadSegment(self, segment, segMarker=None, iMidiMode=False, showLabels=True, showClips=True, showAppEvts=True):
        """ Loads up the segment drawing the graph """
        if segment is None:
            self.ClearGraph()
            return None
        self.iMidiMode = iMidiMode
        self.showLabels = showLabels
        self.showClips = showClips
        self.showAppEvts = showAppEvts
        self.Markers = []
        self.iLocationInMs = 0
        info = MidiSegInfo(segment)        
        #disable graph for debugging
        #return info               
        self.iLengthInMs = info.iLengthInMs
        self.ppqn = info.ppqn
        self.StartMbt = mbtFct(ConvertStrTimeToTuple(segment.start), 1)
        self.EndMbt = mbtFct(ConvertStrTimeToTuple(segment.end), 1)
        self.LengthMbt = None
        self.iStartMeasure = self.StartMbt[0]
        self.iLengthInMeasures = self.EndMbt[0] - self.StartMbt[0]
        
        for jet_event in segment.jetevents:
            if self.showClips and jet_event.event_type == JetDefs.E_CLIP:
                self.AddMarker(JetDefs.E_CLIP, jet_event.event_id, jet_event.event_name, mbtFct(jet_event.event_start,1), mbtFct(jet_event.event_end,1), self.iStartMeasure, self.ppqn)
            elif jet_event.event_type == JetDefs.E_EOS:
                self.AddMarker(JetDefs.E_EOS, jet_event.event_id, jet_event.event_name, mbtFct(jet_event.event_end,1), mbtFct(jet_event.event_end,1), self.iStartMeasure, self.ppqn)
            elif self.showAppEvts and jet_event.event_type == JetDefs.E_APP:
                self.AddMarker(JetDefs.E_APP, jet_event.event_id, jet_event.event_name, mbtFct(jet_event.event_start,1), mbtFct(jet_event.event_end,1), self.iStartMeasure, self.ppqn)
        
        if segMarker is not None:
            self.AddMarker(JetDefs.E_CLIP, 0, segMarker[0], mbtFct(segMarker[1],1), mbtFct(segMarker[2],1), self.iStartMeasure, self.ppqn)
            
        self.DoDrawing()
        return info

    def AddMarker(self, sEventType, iEventId, sName, sStartMbt, sEndMbt, iStartMeasure, ppqn):
        """ Adds a marker to the list """
        if not CompareMbt(sStartMbt, sEndMbt):
            sEndMbt = sStartMbt
        self.Markers.append(Marker(sEventType, iEventId, sName, sStartMbt, sEndMbt, iStartMeasure, ppqn))
        
    def OnLeftDown(self, event):
        """ Calls the function assicated with an event """
        pt = event.GetPosition()
        for Marker in self.Markers:
            if pt[0] >= Marker.iStart and pt[0] <= Marker.iEnd and pt[1] >= Marker.iTop and pt[1] <= Marker.iTop + Marker.iHeight:
                if self.ClickCallbackFct != None:
                    self.ClickCallbackFct(Marker.sName, Marker.iEventId)
               
    def GetAColor(self):
        """ Gets a color """
        color = GRAPH_COLORS[self.iColor]
        self.iColor = self.iColor + 1
        if self.iColor >= len(GRAPH_COLORS):
            self.iColor = 0
        return color
    
    def OnSize(self, event=None):
        """ Repaints for resizing of screen """
        if OsWindows():
            # The Buffer init is done here, to make sure the buffer is always
            # the same size as the Window
            Size  = self.GetClientSizeTuple()
    
            # Make new offscreen bitmap: this bitmap will always have the
            # current drawing in it, so it can be used to save the image to
            # a file, or whatever.
            self._Buffer = wx.EmptyBitmap(*Size)
        self.DoDrawing(None)
        if event is not None:
            event.Skip()
        
    def OnPaint(self, event=None):
        """ Painting of windows """
        if OsWindows():
            dc = wx.BufferedPaintDC(self, self._Buffer)
        else:
            dc = wx.AutoBufferedPaintDC(self)
            dc.Background = wx.Brush(wx.WHITE)
        self.DoDrawing(dc)

    def DoDrawing(self, dc=None):
        """ Does the actual drawing of the control """
        if dc is None:
            if OsWindows():
                dc = wx.BufferedDC(wx.ClientDC(self), self._Buffer)
            else:
                dc = wx.AutoBufferedPaintDC(self)
                dc.Background = wx.Brush(wx.WHITE)
                
        dc.Clear()
        
        self.iColor = 0
        gWidth, gHeight = self.GetSize()
        gWidth = gWidth - (self.iEdges * 2)
        step = int(gWidth / (self.iLengthInMeasures + .01))
        
        for Marker in self.Markers:
            Marker.CalcCoord(step, gHeight, self.GetAColor)

        """ eliminate overlaps; establish colors """
        iClips = 0
        iMarkers = 0
        for index, Marker in enumerate(self.Markers):
            if Marker.sEventType == JetDefs.E_CLIP:
                iClips = iClips + 1
                iOverlaps = 1
                for index1, Marker1 in enumerate(self.Markers):
                    if Marker.sEventType == JetDefs.E_CLIP:
                        if index != index1 and not Marker1.iUpdate:
                            if Marker.iStart <= Marker1.iStart and Marker.iEnd <= Marker1.iEnd and Marker.iEnd >= Marker1.iStart:
                                iOverlaps = iOverlaps + 1
                                Marker.iUpdate = True
                                Marker1.iUpdate = True
                            if not Marker.iUpdate and Marker.iStart >= Marker1.iStart and Marker.iEnd >= Marker1.iEnd and Marker.iStart <= Marker1.iEnd:
                                iOverlaps = iOverlaps + 1
                                Marker.iUpdate = True
                                Marker1.iUpdate = True
                if iOverlaps > 1:
                    iTop = 0
                    for index1, Marker1 in enumerate(self.Markers):
                        if Marker.sEventType == JetDefs.E_CLIP:
                            if Marker1.iUpdate:
                                Marker1.iHeight = gHeight / iOverlaps
                                Marker1.iTop = iTop * Marker1.iHeight
                                iTop = iTop + 1
            elif Marker.sEventType == JetDefs.E_APP:
                iMarkers = iMarkers + 1

        for Marker in self.Markers:
            if Marker.sEventType == JetDefs.E_CLIP:
                dc.SetPen(wx.Pen(Marker.sColor))
                dc.SetBrush(wx.Brush(Marker.sColor))
                dc.DrawRectangle(Marker.iStart + self.iEdges, Marker.iTop, Marker.iWidth, Marker.iHeight)
                width, height = dc.GetTextExtent(Marker.sName)
                k = ((Marker.iStart + Marker.iEnd) / 2) - (width/2) + self.iEdges
                if self.showLabels or self.iMidiMode:
                    dc.DrawText(Marker.sName, k, ((Marker.iTop+Marker.iHeight/2) - (height*.5)))
                if self.iMidiMode:
                    self.iMidiModeStart = Marker.iStart
            elif Marker.sEventType == JetDefs.E_EOS:
                dc.SetPen(wx.Pen(EOS_BAR))
                dc.SetBrush(wx.Brush(EOS_BAR))
                dc.DrawRectangle(Marker.iStart + self.iEdges, Marker.iTop, 1, Marker.iHeight)                
                width, height = dc.GetTextExtent(Marker.sName)
                k = Marker.iStart - (width/2) + self.iEdges
                dc.DrawText(Marker.sName, k, ((Marker.iTop+Marker.iHeight/2) - (height*.5)))
            elif Marker.sEventType == JetDefs.E_APP:
                dc.SetPen(wx.Pen(APP_BAR))
                dc.SetBrush(wx.Brush(APP_BAR))
                dc.DrawRectangle(Marker.iStart + self.iEdges, Marker.iTop, 1, Marker.iHeight)                
                width, height = dc.GetTextExtent(Marker.sName)
                k = Marker.iStart - (width/2) + self.iEdges
                if self.showLabels or self.iMidiMode:
                    dc.DrawText(Marker.sName, k, ((Marker.iTop+Marker.iHeight/2) - (height*.5)))


        """ Draw scale """
        if gWidth == 0:
            iDiv = 50
        else:
            iDiv = (gWidth)/18
        if iDiv == 0:
            iDiv = 50
        scale = ((self.iLengthInMeasures / iDiv) + 1)
        if scale == 0:
            scale = 1
        beatStep = step / 4.0
        dc.SetFont(self.font)
        j = 0
        lastEnd = 0
        num = range(self.iStartMeasure, self.iStartMeasure + self.iLengthInMeasures + 1, 1)
        dc.SetPen(wx.Pen('#5C5142'))
        for i in range(0, (self.iLengthInMeasures+1)*step, step):
            k = i + self.iEdges
            dc.DrawLine(k, self.iScaleTop, k, self.iScaleTop+8)
            if i != (self.iLengthInMeasures)*step:
                for iBeat in range(1,4):
                    k = i+(iBeat * beatStep) + self.iEdges
                    dc.DrawLine(k, self.iScaleTop, k, self.iScaleTop+4)
            width, height = dc.GetTextExtent(str(num[j]))
            k = i-(width/2) + self.iEdges
            if k > lastEnd:
                if j == 0 or (j % scale) == 0:
                    dc.DrawText(str(num[j]), k, self.iScaleTop+8)
                lastEnd = k + width
            j = j + 1

        """ Updates the location bar in case screen moved or resized """
        if self.iLocationInMs > 0 and self.iLengthInMs > 0:
            iOffset = 0
            if self.iMidiMode:
                iOffset = self.iMidiModeStart
            
            till = gWidth * (self.iLocationInMs / self.iLengthInMs)
            dc.SetPen(wx.Pen(PROGRESS_BAR))
            dc.SetBrush(wx.Brush(PROGRESS_BAR))
            dc.DrawRectangle(self.iEdges + iOffset, gHeight-6, till, 3)

    def UpdateLocation(self, iLocationInMs):
        """ Updates the location bar """
        #disable graph for debugging
        #return info               
        
        self.iLocationInMs = iLocationInMs
        if self.iLocationInMs > 0 and self.iLengthInMs > 0:
            if OsWindows():
                dc = wx.BufferedDC(wx.ClientDC(self), self._Buffer)
            else:
                dc = wx.AutoBufferedPaintDC(self)
                dc.Background = wx.Brush(wx.WHITE)
            
            iOffset = 0
            if self.iMidiMode:
                iOffset = self.iMidiModeStart
            
            gWidth, gHeight = self.GetSize()
            gWidth = gWidth - (self.iEdges * 2)
        
            till = gWidth * (self.iLocationInMs / self.iLengthInMs)
            dc.SetPen(wx.Pen(PROGRESS_BAR))
            dc.SetBrush(wx.Brush(PROGRESS_BAR))
            dc.DrawRectangle(self.iEdges + iOffset, gHeight-6, till, 3)
            self.isDirty = True
        else:
            if self.isDirty:
                self.DoDrawing()
                self.isDirty = False
