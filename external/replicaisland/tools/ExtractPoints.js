/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 /**
 * @fileoverview This implements a Photoshop script that can be used to generate
 * collision information for the AndouKun game engine.  This tool walks over
 * each path in the current document and generates a list of edges and normals
 * in a new document.  It is intended to be used on a file containing
 * graphical representations of the collision tiles used by the engine.  Each
 * path in the file must be closed and may not contain any curved points 
 * (the tool assumes that the line between any two points in a given path is
 * straight).  Only one shape may be contained per path layer (each path must go
 * in its own path layer).  This tool can also output a graphical version of its
 * edge calculation for debugging purposes.
 */
 
/* If set to true, the computation will be rendered graphically to the output
   file */
var drawOutput = false;
/* If true, the computation will be printed in a text layer in the
   output file.*/
var printOutput = true;

// Back up the ruler units that this file uses before switching to pixel units.
var defaultRulerUnits = app.preferences.rulerUnits;
app.preferences.rulerUnits = Units.PIXELS;

var tileSizeX = prompt("Tile pixel width:");
var tileSizeY = prompt("Tile pixel height:");

var documentWidth = app.activeDocument.width;
var documentHeight = app.activeDocument.height;

var tilesPerRow = documentWidth / tileSizeX;
var tilesPerColumn = documentHeight / tileSizeY;

var tiles = new Array();
tiles.length = tilesPerRow * tilesPerColumn;

// Walk the list of paths and extract edges and normals.  Store these in
// an array by tile.
var pathList = app.activeDocument.pathItems;
for (pathIndex = 0; pathIndex < pathList.length; pathIndex++) {
  var main_path = pathList[pathIndex];
  if (main_path) {
    var itemList = main_path.subPathItems;
    if (!itemList) {
      alert("Path has no sub items!");
    } else {
      for (var x = 0; x < itemList.length; x++) {
        var item = itemList[x];
        var points = item.pathPoints;
        var tile = new Object;
        tile.edges = new Array();
        
        var totalX = 0;
        var totalY = 0;
        for (var y = 0; y < points.length; y++) {
          var firstPoint = points[y];
          var lastPoint = points[(y + 1) % points.length];

          var edge = new Object;
          
          edge.startX = firstPoint.anchor[0];
          edge.startY = firstPoint.anchor[1];
          
          edge.endX = lastPoint.anchor[0];
          edge.endY = lastPoint.anchor[1];
          
          var normalX = -(edge.endY - edge.startY);
          var normalY = edge.endX - edge.startX;
          
          var normalLength = Math.sqrt((normalX * normalX) + (normalY * normalY));
          normalX /= normalLength;
          normalY /= normalLength;
          
          edge.normalX = normalX;
          edge.normalY = normalY;
          
          if (normalX == 0 && normalY == 0) {
            alert("Zero length normal calculated at path " + pathIndex);
          }
          
          var normalLength2 = Math.sqrt((normalX * normalX) + (normalY * normalY));
          if (normalLength2 > 1 || normalLength2 < 0.9) {
            alert("Normal of invalid length (" + normalLength2 + ") found at path " + pathIndex);
          }
          
          totalX += edge.endX;
          totalY += edge.endY;
          
          var width = edge.endX - edge.startX;
          var height = edge.endY - edge.startY;
          
          edge.centerX = edge.endX - (width / 2);
          edge.centerY = edge.endY - (height / 2);
          
          tile.edges.push(edge);
        }
        
        totalX /= points.length;
        totalY /= points.length;
        tile.centerX = totalX;
        tile.centerY = totalY; 
        
        var column = Math.floor(tile.centerX / tileSizeX);
        var row = Math.floor(tile.centerY / tileSizeY);
        
        tile.xOffset = column * tileSizeX;
        tile.yOffset = row * tileSizeY;
        
        tile.centerX -= tile.xOffset;
        tile.centerY -= tile.yOffset;
        
        var tileIndex = Math.floor(row * tilesPerRow + column);
        tiles[tileIndex] = tile;
        
      }
    }
  }
}

var outputString = "";

// For each tile print the edges to a string.
for (var x = 0; x < tiles.length; x++) {
  if (tiles[x]) {
    var tile = tiles[x];
    for (var y = 0; y < tile.edges.length; y++) {
      var edge = tile.edges[y];
      
      // convert to tile space
      edge.startX -= tile.xOffset;
      edge.startY -= tile.yOffset;
      edge.endX -= tile.xOffset;
      edge.endY -= tile.yOffset;
      edge.centerX -= tile.xOffset;
      edge.centerY -= tile.yOffset;
      
      // The normals that we calculated previously might be facing the wrong
      // direction.  Detect this case and correct it by checking to see if
      // adding the normal to a point on the edge moves the point closer or
      // further from the center of the shape.
      if (Math.abs(edge.centerX - tile.centerX) > 
            Math.abs((edge.centerX + edge.normalX) - tile.centerX)) {
        edge.normalX *= -1;
        edge.normalY *= -1;
      }
      
      if (Math.abs(edge.centerY - tile.centerY) > 
            Math.abs((edge.centerY + edge.normalY) - tile.centerY)) {
        edge.normalX *= -1;
        edge.normalY *= -1;
      }
      
       
      // Convert to left-handed GL space (the origin is at the bottom-left).
      edge.normalY *= -1;
      edge.startY = tileSizeY - edge.startY;
      edge.endY = tileSizeY - edge.endY;
      edge.centerY = tileSizeY - edge.centerY;
     
      outputString += x + ":" + Math.floor(edge.startX) + "," + 
          Math.floor(edge.startY) + ":" + Math.floor(edge.endX) + "," + 
          Math.floor(edge.endY) + ":" + edge.normalX + "," + edge.normalY +
          "\r";
    }
  }
}


if (outputString.length > 0) {
    
    var newDoc = app.documents.add(600, 700, 72.0, "Edge Output", 
        NewDocumentMode.RGB);
    
    if (drawOutput) {
      // Render the edges and normals to the new document.
      var pathLayer = newDoc.artLayers.add();
      newDoc.activeLayer = pathLayer;
      
      // draw the edges to make sure everything works
      var black = new SolidColor;
      black.rgb.red = 0;
      black.rgb.blue = 0;
      black.rgb.green = 0;
      
      var redColor = new SolidColor;
      redColor.rgb.red = 255;
      redColor.rgb.blue = 0;
      redColor.rgb.green = 0;
      
      var greenColor = new SolidColor;
      greenColor.rgb.red = 0;
      greenColor.rgb.blue = 0;
      greenColor.rgb.green = 255;
      
      var blueColor = new SolidColor;
      blueColor.rgb.red = 0;
      blueColor.rgb.blue = 255;
      blueColor.rgb.green = 0;
      
      var lineIndex = 0;
      for (var x = 0; x < tiles.length; x++) {
        if (tiles[x]) {
          var tile = tiles[x];
          var lineArray = new Array();
          var offsetX = Math.floor(x % tilesPerRow) * tileSizeX;
          var offsetY = Math.floor(x / tilesPerRow) * tileSizeY;
            
          for (var y = 0; y < tile.edges.length; y++) {
            var edge = tile.edges[y];
           
            lineArray[y] = Array(offsetX + edge.startX, offsetY + edge.startY);
          }
          
          // I tried to do this by stroking paths, but the documentation 
          // provided by Adobe is faulty (their sample code doesn't run).  The 
          // same thing can be accomplished with selections instead.
          newDoc.selection.select(lineArray);
          newDoc.selection.stroke(black, 2);
         
          for (var y = 0; y < tile.edges.length; y++) {
            var edge = tile.edges[y];
      
             var normalX = Math.round(tile.centerX + 
                (edge.normalX * (tileSizeX / 2)));
             var normalY = Math.round(tile.centerY + 
                (edge.normalY * (tileSizeY / 2)));
             
             var tileCenterArray = new Array();
             tileCenterArray[0] = new Array(offsetX + tile.centerX - 1, 
                offsetY + tile.centerY - 1);
             tileCenterArray[1] = new Array(offsetX + tile.centerX - 1, 
                offsetY + tile.centerY + 1);
             tileCenterArray[2] = new Array(offsetX + tile.centerX + 1, 
                offsetY + tile.centerY + 1);
             tileCenterArray[3] = new Array(offsetX + tile.centerX + 1, 
                offsetY + tile.centerY - 1);
             tileCenterArray[4] = new Array(offsetX + normalX - 1, 
                offsetY + normalY - 1);
             tileCenterArray[5] = new Array(offsetX + normalX + 1, 
                offsetY + normalY + 1);
             tileCenterArray[6] = new Array(offsetX + tile.centerX, 
                offsetY + tile.centerY);
              
             newDoc.selection.select(tileCenterArray);
             newDoc.selection.fill(redColor);
             
             var centerArray = new Array();
             centerArray[0] = new Array(offsetX + edge.centerX - 1, 
                offsetY + edge.centerY - 1);
             centerArray[1] = new Array(offsetX + edge.centerX - 1, 
                offsetY + edge.centerY + 1);
             centerArray[2] = new Array(offsetX + edge.centerX + 1, 
                offsetY + edge.centerY + 1);
             centerArray[3] = new Array(offsetX + edge.centerX + 1, 
                offsetY + edge.centerY - 1);
             
             newDoc.selection.select(centerArray);
             newDoc.selection.fill(greenColor);
             
          }
         
        }
      }
    }
    
    if (printOutput) {
      var textLayer = newDoc.artLayers.add();
      textLayer.kind = LayerKind.TEXT;
      textLayer.textItem.contents = outputString;
    }
}

preferences.rulerUnits = defaultRulerUnits;

// Convenience function for clamping negative values to zero.  Trying to select
// areas outside the canvas causes Bad Things.
function clamp(input) {
  if (input < 0) {
    return 0;
  }
  return input;
}
