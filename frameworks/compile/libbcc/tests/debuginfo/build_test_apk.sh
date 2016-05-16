#!/bin/bash -e

# Copyright 2012, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Creates and builds projects from a RenderScript testcase and a set of Java templates

HELP=no
VERBOSE=no
MINSDK=1
TARGET=1
NAME=""
OUT_DIR=
ACTIVITY=""
PACKAGE=""
SDK=""
TESTCASE_PATH=""
DRIVER=""

check_param ()
{
    if [ -z "$2" ]; then
        echo "ERROR: Missing parameter after option '$1'"
        exit 1
    fi
}

check_required_param()
{
    if [ -z "$1" ]; then
        echo "ERROR: Missing required parameter $2"
        exit 1
    fi
}

run ()
{
    if [ "$VERBOSE" = "yes" ] ; then
        echo "## COMMAND: $@"
    fi
    $@ 2>&1
}

process_template()
{
  src=$1
  dest=$2
  sed -e "s/%ACTIVITY%/$3/g" -e "s/%PACKAGE%/$4/g" -e "s/%TESTCASE%/$5/g" -e "s/%MINSDK%/$6/g" < $src > $dest;
  echo "processed $src ==> $dest"
}

while [ -n "$1" ]; do
    opt="$1"
    case "$opt" in
        --help|-h|-\?)
            HELP=yes
            ;;
        --verbose|-v)
            VERBOSE=yes
            ;;
        --sdk)
            check_param $1 $2
            SDK="$2"
            ;;
        --name)
            check_param $1 $2
            NAME="$2"
            ;;
        --out)
            check_param $1 $2
            OUT_DIR="$2"
            ;;
        --activity)
            check_param $1 $2
            ACTIVITY="$2"
            ;;
        --package)
            check_param $1 $2
            PACKAGE="$2"
            ;;
        --minsdk)
            check_param $1 $2
            MINSDK="$2"
            ;;
        --target)
            check_param $1 $2
            TARGET="$2"
            ;;
        --testcase)
            check_param $1 $2
            TESTCASE_PATH="$2"
            ;;
        --driver)
            check_param $1 $2
            DRIVER="${2%/}"
            ;;
        -*) # unknown options
            echo "ERROR: Unknown option '$opt', use --help for list of valid ones."
            exit 1
        ;;
        *)  # Simply record parameter
            if [ -z "$PARAMETERS" ] ; then
                PARAMETERS="$opt"
            else
                PARAMETERS="$PARAMETERS $opt"
            fi
            ;;
    esac
    shift
done

if [ "$HELP" = "yes" ] ; then
    echo "Usage: $PROGNAME [options]"
    echo ""
    echo "Build a test project from a RS testcase and a java driver template."
    echo ""
    echo "Required Parameters:"
    echo "    --sdk                Location of Android SDK installation"
    echo "    --out <path>         Location of your project directory"
    echo "    --testcase <name>    The .rs testcase file with which to build the project"
    echo "    --driver <name>      The java template directory with which to build the project"
    echo ""
    echo "Optional Parameters (reasonable defaults are used if not specified)"
    echo "    --activity <name>    Name for your default Activity class"
    echo "    --package <name>     Package namespace for your project"
    echo "    --target <name>      Android build target. Execute 'android list targets' to list available targets and their ID's."
    echo "    --minsdk <name>      minSdkVersion attribute to embed in AndroidManifest.xml of test project."
    echo "    --help|-h|-?         Print this help"
    echo "    --verbose|-v         Enable verbose mode"
    echo ""
    exit 0
fi

# Verify required parameters are non-empty
check_required_param "$SDK" "--sdk"
check_required_param "$OUT_DIR" "--out"
check_required_param "$TESTCASE_PATH" "--testcase"
check_required_param "$DRIVER" "--driver"

# Compute name of testcase
TESTCASE=`basename $TESTCASE_PATH .rs`

# Compute activity, appname, and java package, if not specified via parameters
if [ -z "$ACTIVITY" ]; then
  ACTIVITY="$TESTCASE";
fi

if [ -z "$NAME" ]; then
  NAME="$ACTIVITY"
fi

if [ -z "$PACKAGE" ]; then
  PACKAGE=com.android.test.rsdebug.$TESTCASE
fi

# Create the project
run $SDK/tools/android create project --target $TARGET --name $NAME --path $OUT_DIR --activity $ACTIVITY --package $PACKAGE

if [ $? != 0 ] ; then
    echo "ERROR: Could not create Android project."
    echo "       Check parameters and try again."
    exit 1
fi

# Compute name of destination source directory
DEST_SRC_DIR=$OUT_DIR/src/`echo $PACKAGE | sed 's/\./\//g'`

if [ ! -d "$DRIVER" ]; then
  # If driver directory does not exist, try to fix it up by searching the
  # testcase directory as well
  DRIVER=`dirname $TESTCASE_PATH`/"$DRIVER"
  if [ ! -d $DRIVER ]; then
    echo "unable to find driver in $DRIVER, please check --driver"
    exit 1;
  fi
fi

echo "Copying driver template from $DRIVER -> $DEST_SRC_DIR"
if [ ! -d "$DEST_SRC_DIR" ]; then
  echo "Error, destination directory does not exist: $DEST_SRC_DIR";
  exit 1;
fi
echo "Performing template substitutions:"
echo "    %ACTIVITY% ==> $ACTIVITY"
echo "    %PACKAGE% ==> $PACKAGE"
echo "    %TESTCASE% ==> $TESTCASE"
echo "    %MINSDK% ==> $MINSDK"
SUBST_PARAMS="$ACTIVITY $PACKAGE $TESTCASE $MINSDK"

# If it exists, use contents of driver-common directory to seed
# the testcase project
DRIVER_COMMON="`dirname $TESTCASE_PATH`/driver-common"
if [ -d $DRIVER_COMMON ]; then
  echo "Found common driver directory: $DRIVER_COMMON"
  ls $DRIVER_COMMON/SRC/*.java.template | while read src; do
    SRC_BASENAME=`basename $src .java.template`;
    dest=$DEST_SRC_DIR/`echo $SRC_BASENAME | sed "s/ACTIVITY/$ACTIVITY/g"`.java
    process_template $src $dest $SUBST_PARAMS
  done;

  # Copy AndroidManifest.xml
  COMMON_MANIFEST="$DRIVER_COMMON/AndroidManifest.xml"
  if [ -e $COMMON_MANIFEST ]; then
    process_template $COMMON_MANIFEST $OUT_DIR/AndroidManifest.xml $SUBST_PARAMS
  fi
fi

# Copy Java source to project directory.
ls $DRIVER/*.java.template | while read src; do
  SRC_BASENAME=`basename $src .java.template`
  dest=$DEST_SRC_DIR/`echo $SRC_BASENAME | sed "s/ACTIVITY/$ACTIVITY/g"`.java
  process_template $src $dest $SUBST_PARAMS
done;

# Copy AndroidManifest.xml override, if it exists
OVERRIDE_MANIFEST="$DRIVER/AndroidManifest.xml"
if [ -e $OVERRIDE_MANIFEST ]; then
  process_template $OVERRIDE_MANIFEST $OUT_DIR/AndroidManifest.xml $SUBST_PARAMS
fi

# Copy RS testcase to project directory.
TESTCASE_DEST=$DEST_SRC_DIR/`basename $TESTCASE_PATH`
process_template $TESTCASE_PATH $TESTCASE_DEST $SUBST_PARAMS

# Buid signed and aligned apk
cd $OUT_DIR
run ant clean debug install

if [ $? != 0 ] ; then
    echo "ERROR: Apk build and install failed"
    exit 1
fi

exit 0
