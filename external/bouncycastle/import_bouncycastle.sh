#!/bin/bash
#
# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# This script imports new versions of Bouncy Castle
# (http://bouncycastle.org) into the Android source tree.  To run, (1)
# fetch the appropriate tarballs (bcprov and bcpkix) from the Bouncy
# Castle repository, (2) check the checksum, and then (3) run:
#   ./import_bouncycastle.sh import bcprov-jdk*-*.tar.gz
#
# IMPORTANT: See README.android for additional details.

# turn on exit on error as well as a warning when it happens
set -e
trap  "echo WARNING: Exiting on non-zero subprocess exit code" ERR;

function die() {
  declare -r message=$1

  echo $message
  exit 1
}

function usage() {
  declare -r message=$1

  if [ ! "$message" = "" ]; then
    echo $message
  fi
  echo "Usage:"
  echo "  ./import_bouncycastle.sh import </path/to/bcprov-jdk*-*.tar.gz>"
  echo "  ./import_bouncycastle.sh regenerate <patch/*.patch>"
  echo "  ./import_bouncycastle.sh generate <patch/*.patch> </path/to/bcprov-jdk*-*.tar.gz>"
  exit 1
}

function main() {
  if [ ! -d patches ]; then
    die "Bouncy Castle patch directory patches/ not found"
  fi

  if [ ! -f bouncycastle.version ]; then
    die "bouncycastle.version not found"
  fi

  source bouncycastle.version
  if [ "$BOUNCYCASTLE_JDK" == "" -o "$BOUNCYCASTLE_VERSION" == "" ]; then
    die "Invalid bouncycastle.version; see README.android for more information"
  fi

  BOUNCYCASTLE_BCPROV_DIR=bcprov-jdk$BOUNCYCASTLE_JDK-$BOUNCYCASTLE_VERSION
  BOUNCYCASTLE_BCPROV_DIR_ORIG=$BOUNCYCASTLE_BCPROV_DIR.orig

  BOUNCYCASTLE_BCPKIX_DIR=bcpkix-jdk$BOUNCYCASTLE_JDK-$BOUNCYCASTLE_VERSION
  BOUNCYCASTLE_BCPKIX_DIR_ORIG=$BOUNCYCASTLE_BCPKIX_DIR.orig

  if [ ! -f bouncycastle.config ]; then
    die "bouncycastle.config not found"
  fi

  source bouncycastle.config
  if [ "$UNNEEDED_BCPROV_SOURCES" == "" -o "$NEEDED_BCPROV_SOURCES" == "" \
    -o "$UNNEEDED_BCPKIX_SOURCES" == "" -o "$NEEDED_BCPKIX_SOURCES" == "" ]; then
    die "Invalid bouncycastle.config; see README.android for more information"
  fi

  declare -r command=$1
  shift || usage "No command specified. Try import, regenerate, or generate."
  if [ "$command" = "import" ]; then
    declare -r bcprov_tar=$1
    shift || usage "No tar file specified."
    declare -r bcpkix_tar=`echo $bcprov_tar | sed s/bcprov/bcpkix/`
    import $bcprov_tar $BOUNCYCASTLE_BCPROV_DIR $BOUNCYCASTLE_BCPROV_DIR_ORIG bcprov "$BOUNCYCASTLE_BCPROV_PATCHES" "$NEEDED_BCPROV_SOURCES" "$UNNEEDED_BCPROV_SOURCES"
    import $bcpkix_tar $BOUNCYCASTLE_BCPKIX_DIR $BOUNCYCASTLE_BCPKIX_DIR_ORIG bcpkix "$BOUNCYCASTLE_BCPKIX_PATCHES" "$NEEDED_BCPKIX_SOURCES" "$UNNEEDED_BCPKIX_SOURCES"
  elif [ "$command" = "regenerate" ]; then
    declare -r patch=$1
    shift || usage "No patch file specified."
    if [[ $BOUNCYCASTLE_BCPROV_PATCHES == *$patch* ]]; then
      [ -d $BOUNCYCASTLE_BCPROV_DIR ] || usage "$BOUNCYCASTLE_BCPROV_DIR not found, did you mean to use generate?"
      [ -d $BOUNCYCASTLE_BCPROV_DIR_ORIG ] || usage "$BOUNCYCASTLE_BCPROV_DIR_ORIG not found, did you mean to use generate?"
      regenerate $patch $BOUNCYCASTLE_BCPROV_DIR $BOUNCYCASTLE_BCPROV_DIR_ORIG
    elif [[ $BOUNCYCASTLE_BCPKIX_PATCHES == *$patch* ]]; then
      [ -d $BOUNCYCASTLE_BCPKIX_DIR ] || usage "$BOUNCYCASTLE_BCPROV_DIR not found, did you mean to use generate?"
      [ -d $BOUNCYCASTLE_BCPKIX_DIR_ORIG ] || usage "$BOUNCYCASTLE_BCPKIX_DIR_ORIG not found, did you mean to use generate?"
      regenerate $patch $BOUNCYCASTLE_BCPKIX_DIR $BOUNCYCASTLE_BCPKIX_DIR_ORIG
    else
      usage "Unknown patch file $patch specified"
    fi
  elif [ "$command" = "generate" ]; then
    declare -r patch=$1
    shift || usage "No patch file specified."
    declare -r bcprov_tar=$1
    shift || usage "No tar file specified."
    declare -r bcpkix_tar=`echo $bcprov_tar | sed s/bcprov/bcpkix/`
    if [[ $BOUNCYCASTLE_BCPROV_PATCHES == *$patch* ]]; then
      generate $patch $bcprov_tar $BOUNCYCASTLE_BCPROV_DIR $BOUNCYCASTLE_BCPROV_DIR_ORIG bcprov "$BOUNCYCASTLE_BCPROV_PATCHES" "$NEEDED_BCPROV_SOURCES" "$UNNEEDED_BCPROV_SOURCES"
    elif [[ $BOUNCYCASTLE_BCPKIX_PATCHES == *$patch* ]]; then
      generate $patch $bcpkix_tar $BOUNCYCASTLE_BCPKIX_DIR $BOUNCYCASTLE_BCPKIX_DIR_ORIG bcpkix "$BOUNCYCASTLE_BCPKIX_PATCHES" "$NEEDED_BCPKIX_SOURCES" "$UNNEEDED_BCPKIX_SOURCES"
    else
      usage "Unknown patch file $patch specified"
    fi
  else
    usage "Unknown command specified $command. Try import, regenerate, or generate."
  fi
}

function import() {
  declare -r bouncycastle_source=$1
  declare -r bouncycastle_dir=$2
  declare -r bouncycastle_dir_orig=$3
  declare -r bouncycastle_out_dir=$4
  declare -r bouncycastle_patches=$5
  declare -r needed_sources=$6
  declare -r unneeded_sources=$7

  untar $bouncycastle_source $bouncycastle_dir $bouncycastle_dir_orig "$unneeded_sources"
  applypatches $bouncycastle_dir "$bouncycastle_patches" "$unneeded_sources"

  cd $bouncycastle_dir

  sed 's/<p>/& <BR>/g' LICENSE.html | html2text -width 102 -nobs -ascii > ../NOTICE
  touch ../MODULE_LICENSE_BSD_LIKE

  cd ..

  rm -r $bouncycastle_out_dir/src
  mkdir -p $bouncycastle_out_dir/src/main/java/
  for i in $needed_sources; do
    echo "Updating $i"
    mv $bouncycastle_dir/$i $bouncycastle_out_dir/src/main/java/
  done

  cleantar $bouncycastle_dir $bouncycastle_dir_orig
}

function regenerate() {
  declare -r patch=$1
  declare -r bouncycastle_dir=$2
  declare -r bouncycastle_dir_orig=$3

  generatepatch $patch $bouncycastle_dir $bouncycastle_dir_orig
}

function update_timestamps() {
  declare -r git_dir="$1"
  declare -r target_dir="$2"

  echo -n "Restoring timestamps for ${target_dir}... "

  find "$git_dir" -type f -print0 | while IFS= read -r -d $'\0' file; do
    file_rev="$(git rev-list -n 1 HEAD "$file")"
    if [ "$file_rev" == "" ]; then
      echo
      echo -n "WARNING: No file revision for file $file..."
      continue
    fi
    file_time="$(git show --pretty=format:%ai --abbrev-commit "$file_rev" | head -n 1)"
    touch -d "$file_time" "${target_dir}${file#$git_dir}"
  done

  echo "done."
}

function generate() {
  declare -r patch=$1
  declare -r bouncycastle_source=$2
  declare -r bouncycastle_dir=$3
  declare -r bouncycastle_dir_orig=$4
  declare -r bouncycastle_out_dir=$5
  declare -r bouncycastle_patches=$6
  declare -r needed_sources=$7
  declare -r unneeded_sources=$8

  untar $bouncycastle_source $bouncycastle_dir $bouncycastle_dir_orig "$unneeded_sources"
  applypatches $bouncycastle_dir "$bouncycastle_patches" "$unneeded_sources"

  for i in $needed_sources; do
    echo "Restoring $i"
    rm -r $bouncycastle_dir/$i
    cp -rf $bouncycastle_out_dir/src/main/java/$i $bouncycastle_dir/$i
    update_timestamps $bouncycastle_out_dir/src/main/java/$i $bouncycastle_dir/$i
  done

  generatepatch $patch $bouncycastle_dir $bouncycastle_dir_orig
  cleantar $bouncycastle_dir $bouncycastle_dir_orig
}

function untar() {
  declare -r bouncycastle_source=$1
  declare -r bouncycastle_dir=$2
  declare -r bouncycastle_dir_orig=$3
  declare -r unneeded_sources=$4

  # Remove old source
  cleantar $bouncycastle_dir $bouncycastle_dir_orig

  # Process new source
  tar -zxf $bouncycastle_source
  mv $bouncycastle_dir $bouncycastle_dir_orig
  find $bouncycastle_dir_orig -type f -print0 | xargs -0 chmod a-w
  (cd $bouncycastle_dir_orig && unzip -q src.zip)
  tar -zxf $bouncycastle_source
  (cd $bouncycastle_dir && unzip -q src.zip)

  # Prune unnecessary sources
  echo "Removing $unneeded_sources"
  (cd $bouncycastle_dir_orig && rm -rf $unneeded_sources)
  (cd $bouncycastle_dir      && rm -r  $unneeded_sources)
}

function cleantar() {
  declare -r bouncycastle_dir=$1
  declare -r bouncycastle_dir_orig=$2

  rm -rf $bouncycastle_dir_orig
  rm -rf $bouncycastle_dir
}

function applypatches () {
  declare -r bouncycastle_dir=$1
  declare -r bouncycastle_patches=$2
  declare -r unneeded_sources=$3

  cd $bouncycastle_dir

  # Apply appropriate patches
  for i in $bouncycastle_patches; do
    echo "Applying patch $i"
    patch -p1 --merge < ../$i || die "Could not apply patches/$i. Fix source and run: $0 regenerate $i"

    # make sure no unneeded sources got into the patch
    problem=0
    for s in $unneeded_sources; do
      if [ -e $s ]; then
        echo Unneeded source $s restored by patch $i
        problem=1
      fi
    done
    if [ $problem = 1 ]; then
      exit 1
    fi
  done

  # Cleanup patch output
  find . -type f -name "*.orig" -print0 | xargs -0 rm -f

  cd ..
}

function generatepatch() {
  declare -r patch=$1
  declare -r bouncycastle_dir=$2
  declare -r bouncycastle_dir_orig=$3

  # Cleanup stray files before generating patch
  find $bouncycastle_dir -type f -name "*.orig" -print0 | xargs -0 rm -f
  find $bouncycastle_dir -type f -name "*~" -print0 | xargs -0 rm -f

  rm -f $patch
  LC_ALL=C TZ=UTC0 diff -Naur $bouncycastle_dir_orig $bouncycastle_dir >> $patch && die "ERROR: No diff for patch $path in file $i"
  echo "Generated patch $patch"
}

main $@
