__signature-tools__

This project contains the source code and tests for API signature comparison tools.
It consists roughly of five parts:

#Signature model : A generic model to represent the structure of an API

#Converters      : A dex -> signature model converter (utilizing the dex-tools parser)
                   A java source -> signature model converter (utilizing the doclet tools)

#Delta model     : A model to represent differences between two signature models
    
#Comparator      : Put two signature models into the comparator and you get a delta model

#Report engine   : Translates a delta model to a html output based on templates (utilizing the StringTemplate framework)


Since this folder contains a .project file, it can be imported directly into eclipse as a java project.

Feel free to improve!

_Structure_

dex.reader
  |
  src : source code
  |  |
  |	 signature :  the driver classes
  |  		|
  |     compare : the comparator code
  |     |   |
  |     |   model : the delta model
  |     |
  |     converter : the converters
  |     |
  |     io : common io interfaces
  |     |   |
  |     |   html : html report generator
  |     |
  |     model : signature model
  |
  test : source code of the test suite
  |
  templates : templates for html output generation
  |
  launches : eclipse launches for the tools
  |
  lib : required libraries
  |
  spec : various input files to try the tool 
  |
  README.txt : you are here
  |
  TODO.txt   : tasks which are still open
 
