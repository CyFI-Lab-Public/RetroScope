#!/bin/bash

set -e            # fail on errors

if [[ $(uname) == "Darwin" ]]; then
  PROG_DIR=$(dirname "$0")
else
  PROG_DIR=$(readlink -f $(dirname "$0"))
fi
cd "$PROG_DIR"

DRY="echo"        # default to dry mode unless -f is specified
MK_MERGE_MSG="1"  # 1 to update the MERGE_MSG, empty to do not generate it
MERGE_MSG=""      # msg to generate
JAR_DETECT=""
NAMES_FILTER=""

while [[ -n "$1" ]]; do
  if [[ "$1" == "-f" ]]; then
    DRY=""
  elif [[ "$1" == "-m" ]]; then
    MK_MERGE_MSG=""
  elif [[ "$1" == "-u" ]]; then
    JAR_DETECT="auto"
  elif [[ "$1" == "-o" ]]; then
    JAR_DETECT="only"
  elif [[ $JAR_DETECT == "only" && $1 =~ ^[a-z]+ ]]; then
    NAMES_FILTER="$NAMES_FILTER $1"
  else
    echo "Unknown argument: $1"
    echo "Usage: $0 [-f] [-m] [-u | -o name1.jar ... nameN.jar]"
    echo "       -f: actual do thing. Default is dry-run."
    echo "       -m: do NOT generate a .git/MERGE_MSG"
    echo "       -u: detect and git-revert unchanged JAR files"
    echo "       -o: only keep the given *leaf* filenames (.jar can be omitted)"
    exit 1
  fi
  shift
done

if [[ $JAR_DETECT == "only" && -z "$NAMES_FILTER" ]]; then
  echo "Error: -o must be followed by names of files to keep."
  exit 1
fi

function update() {
  echo
  local repo=$1

  echo "# Build tools/$repo"

  local SHA1=$( cd ../../tools/$repo ; git show-ref --head --hash HEAD )
  MERGE_MSG="$MERGE_MSG
tools/$repo: @ $SHA1"

  ( $DRY cd ../../tools/$repo && $DRY ./gradlew clean publishLocal pushDistribution )
}

function merge_msg() {
  local dst=.git/MERGE_MSG
  if [[ -n $DRY ]]; then
    echo "The following would be output to $dst (use -m to prevent this):"
    dst=/dev/stdout
  fi
  cat >> $dst <<EOMSG
Update SDK prebuilts.
Origin:
$MERGE_MSG

EOMSG
}

function preserve_jars() {
  JAR_TMP_DIR=`mktemp -d -t prebuilt_update_tmp.XXXXXXXX`
  N=0
  for i in `find . -type f  | grep -v "^\./\."` ; do
    tmpf=`echo $i | tr "./" "__"`
    dstf="$JAR_TMP_DIR/$tmpf"
    cp "$i" "$dstf"
    N=$((N+1))
  done
  echo "# Copied $N files to" $(basename $JAR_TMP_DIR)
}

function revert_unchanged_jars() {
  local i tmpf dstf tmp_hash local_hash
  for i in `find . -type f  | grep -v "^\./\."` ; do
    tmpf=`echo $i | tr "./" "__"`
    dstf="$JAR_TMP_DIR/$tmpf"
    tmp_hash=`get_hash $dstf`
    local_hash=`get_hash $i`
    if [[ $dst_hash == $src_hash ]]; then
      echo "# Revert unchanged file $i"
      $DRY cp "$dstf" "$i"
    else
      echo "!--> Keep changed file $i"
    fi
  done
  if [[ -d $JAR_TMP_DIR ]]; then
    echo "# Cleanup" $(basename $JAR_TMP_DIR)
    rm -rf $JAR_TMP_DIR
  fi
}

function revert_filter_jars() {
  local i j tmpf dstf keep
  for i in `find . -type f  | grep -v "^\./\."` ; do
    tmpf=`echo $i | tr "./" "__"`
    dstf="$JAR_TMP_DIR/$tmpf"
    if ! diff -q $dstf $i 1>/dev/null ; then
      j=$(basename "$i")
      for f in $NAMES_FILTER; do
        if [[ "$j" == "$f" || "$j" == "$f.jar" ]]; then
          echo "!--> Keep changed file $i"
          i=""
          break
        fi
      done
      if [[ -f "$i" ]]; then
        echo "# Revert file $i"
        $DRY cp "$dstf" "$i"
      fi
    fi
  done
  if [[ -d $JAR_TMP_DIR ]]; then
    echo "# Cleanup" $(basename $JAR_TMP_DIR)
    rm -rf $JAR_TMP_DIR
  fi
}

function get_hash() {
  # $1: the file to hash
  if [[ "${1: -3}" == "jar" ]]; then
    # Explanation:
    # - unzip -v prints a "verbose" list of a zip's content including each file path, size, timestamp and CRC32
    # - we don't want the timestamp so we use sed to first remove the time (12:34) and the date (13-14-15).
    # - finally get a md5 of the zip output.
    # if the md5 changes, the zip's content has changed (new file, different content size, different CRC32)
    unzip -v $1 | sed -n -e "/[0-9][0-9]:[0-9][0-9]/s/[0-9][0-9]:[0-9][0-9]// ; s/[0-9][0-9]-[0-9][0-9]-[0-9][0-9]//p" | md5sum -b | cut -d " " -f 1
  else
    md5sum -b "$1" | cut -d " " -f 1
  fi
}


if [[ -n $JAR_DETECT ]]; then preserve_jars; fi
for r in base swt; do
  update $r
done
if [[ -n $MK_MERGE_MSG ]]; then merge_msg; fi
if [[ $JAR_DETECT == "auto" ]]; then
  revert_unchanged_jars
elif [[ $JAR_DETECT == "only" ]]; then
  revert_filter_jars
fi
if [[ -n $DRY ]]; then
  echo
  echo "## WARNING: DRY MODE. Run with -f to actually copy files."
fi

