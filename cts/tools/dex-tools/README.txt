__dex-tools__

This project contains the source code and tests for a dex file parser. 
The parser is able to read  a dex file and to create a datastructure based on it. 
It is designed to be fast and close to the spec. 
Support for direct navigation to super classes and similar will be implemented in a layer on top of the provided data structure.

Since this folder contains a .project file, it can be imported directly into eclipse as a java project.

Feel free to improve!

_Structure_

dex.reader
  |
  dex   : test data
  |
  doc   : dex file spec on which this parser is based
  | 
  src   : sourcecode of the parser
  |
  lib   : dx.jar the jar from the dx tool. Used for in-memory java -> byte code -> dex code -> dex model tests
  |
  test  : source code of the small test suite
  |
  README.txt : you are here
  |
  TODO.txt   : tasks which are still open
 
