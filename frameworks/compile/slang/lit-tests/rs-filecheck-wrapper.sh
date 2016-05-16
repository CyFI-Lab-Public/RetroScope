#!/bin/bash -e

# RS Invocation script to FileCheck
# Usage: rs-filecheck-wrapper.sh <output-directory> <path-to-FileCheck> <source>

OUTDIR=$1
FILECHECK=$2
SOURCEFILE=$3

FILECHECK_INPUTFILE=`basename $SOURCEFILE | sed 's/\.rs\$/.ll/'`

$FILECHECK -input-file $OUTDIR/$FILECHECK_INPUTFILE $SOURCEFILE
