#!/bin/bash
#
# Copyright 2012 Google Inc. All Rights Reserved.
# Author: keir@google.com (Keir Mierle)
#
# Import the latest version of Ceres into google3.

set -e
set -x

if [[ "google3" != "$(basename $(pwd))" ]] ; then
  echo "ERROR: Not in toplevel google3 directory. Bailing."
  exit 1
fi

declare -r google3_dir="$(pwd)"

declare -r temp_repo="/tmp/ceres-solver"
rm -rf $temp_repo
git clone \
  https://ceres-solver.googlesource.com/ceres-solver \
  $temp_repo

cd $temp_repo
declare -r commit="$(git log | head -1 | cut -d ' ' -f2)"
rm -rf .git

# Get rid of the internal gtest and gmock code until the upstream
# version moves it around appropriately.
rm -rf internal/ceres/gtest*
rm -rf internal/ceres/gmock*
rm -rf internal/ceres/mock_log.h

cd $google3_dir
cp -R $temp_repo/* third_party/ceres

cd third_party/ceres

declare -r temp_readme="/tmp/README.google"
rm -f $temp_readme

echo "URL: https://ceres-solver.googlesource.com/ceres-solver/+archive/$commit.tar.gz" >> $temp_readme
echo "Version: $commit" >> $temp_readme
tail -n +3 README.google >> $temp_readme
cp $temp_readme README.google
