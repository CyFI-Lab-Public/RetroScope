This re-generates the file IID_to_MPH.c using gperf.
You need to do this after modifying either OpenSLES_IID.c or MPH.c.

Prerequisites:
 * GNU make
 * GNU gperf perfect hash generator

Usage:
Type 'make'.
Diff the old file in ../../src/autogen vs. the newly generated IID_to_MPH.c here.
If the differences look OK, then copy the new IID_to_MPH.c back to
  its stable location in ../../src/autogen using 'make install'.
Build and test the usage of the new IID.
Then do 'make clean' or 'make distclean' here.

hashgen is known to work on Linux with GNU gperf 3.0.3 and GNU sed
version 4.2.1.  There are reports of problems on Mac OS X.
