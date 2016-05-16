# Copyright (C) 2012 The Android Open Source Project
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

function bb_webview_set_lunch_type() {
  case "$1" in
    clank-webview)
      LUNCH_TYPE="nakasi-eng"
      ;;
    clank-webview-tot)
      LUNCH_TYPE="nakasi-eng"
      ;;
    *)
      LUNCH_TYPE=""
      echo "Unable to determine lunch type from: ${BUILDBOT_BUILDERNAME}"
      echo "@@@STEP_FAILURE@@@"
      exit 2
      ;;
  esac
  echo "Using lunch type: $LUNCH_TYPE"
}

function bb_webview_build_android() {
  echo "@@@BUILD_STEP Compile Android@@@"

  local MAKE_COMMAND="make"
  if [ "$USE_GOMA" -eq 1 ]; then
    echo "Building using GOMA"
    MAKE_COMMAND="${GOMA_DIR}/goma-android-make"
  fi

  MAKE_TARGET="webviewchromium"

  bb_run_step $MAKE_COMMAND $MAKE_PARAMS showcommands $MAKE_TARGET

  if [ "$USE_GOMA" -eq 1 ]; then
    bb_stop_goma_internal
  fi
}

function bb_webview_goma_setup() {
  # Set to 0 to disable goma in case of problems.
  USE_GOMA=1
  if [ -z "$GOMA_DIR" ]; then
    export GOMA_DIR=/b/build/goma
  fi
  if [ ! -d $GOMA_DIR ]; then
    USE_GOMA=0
  fi

  if [ "$USE_GOMA" -eq 1 ]; then
    MAKE_PARAMS="-j150 -l20"
  else
    MAKE_PARAMS="-j16"
  fi

  bb_setup_goma_internal
}

# Basic setup for all bots to run after a source tree checkout.
# Args:
#   $1: Android source root.
function bb_webview_baseline_setup {
  SRC_ROOT="$1"
  cd $SRC_ROOT

  echo "@@@BUILD_STEP Environment setup@@@"
  . build/envsetup.sh

  bb_webview_set_lunch_type $BUILDBOT_BUILDERNAME
  lunch $LUNCH_TYPE

  if [[ $BUILDBOT_CLOBBER ]]; then
    echo "@@@BUILD_STEP Clobber@@@"

    rm -rf ${ANDROID_PRODUCT_OUT}
    rm -rf ${ANDROID_HOST_OUT}
  fi

  # Add the upstream build/android folder to the Python path.
  # This is required since we don't want to check out the clank scripts into a
  # subfolder of the upstream chromium_org checkout (that would make repo think
  # those are uncommited changes and cause potential issues).
  export PYTHONPATH="$PYTHONPATH:${BB_DIR}/../"

  # The CTS bot runs using repo only.
  export CHECKOUT="repo"

  bb_webview_goma_setup
}

function bb_webview_smart_sync {
  echo "@@@BUILD_STEP Smart Sync (sync -s) @@@"
  bb_run_step repo sync -s -j8 -df

  # We always want to roll with the latest code in this project, regardless of
  # what smartsync thinks.
  echo "@@@BUILD_STEP Sync frameworks/webview@@@"
  repo sync frameworks/webview
}

function bb_webview_remove_chromium_org {
  echo "@@@BUILD_STEP Removing chromium_org project@@@"
  # This generates a local manifest that will exclude any projects from the
  # external/chromium_org folder.
  bb_run_step python ${WEBVIEW_TOOLS_DIR}/generate_local_manifest.py \
    ${ANDROID_SRC_ROOT} external/chromium_org
  bb_webview_smart_sync
}

function bb_webview_sync_upstream_chromium {
  echo "@@@BUILD_STEP Sync upstream chromium@@@"
  local CHROMIUM_TOT_DIR=${ANDROID_SRC_ROOT}/external/chromium_tot
  local CHROMIUM_ORG_DIR=${ANDROID_SRC_ROOT}/external/chromium_org
  if [ ! -e ${CHROMIUM_TOT_DIR} ]; then
    echo "No chromium_tot checkout detected. Creating new one.."
    mkdir -p ${CHROMIUM_TOT_DIR}
    cd ${CHROMIUM_TOT_DIR}

    if [ ! -e ${CHROMIUM_ORG_DIR} ]; then
      echo "Symlinking chromium_org to chromium_tot/src"
      ln -s ${CHROMIUM_TOT_DIR}/src ${CHROMIUM_ORG_DIR}
    else
      echo "${CHROMIUM_ORG_DIR} should have been removed by local manifest."
      echo "@@@STEP_FAILURE@@@"
      exit 2
    fi

    # Prevent Android make recursing into this folder since we're
    # exposing the src folder via a symlink.
    touch ${CHROMIUM_TOT_DIR}/Android.mk

    echo "Cloning chromium_tot"
    git clone --template=${DEPOT_TOOLS_DIR}/git-templates \
      https://chromium.googlesource.com/chromium/src.git
    cd ${CHROMIUM_TOT_DIR}/src
    git config target.os android
  fi

  cd ${CHROMIUM_TOT_DIR}/src
  echo "Updating"
  git crup -j8

  echo "@@@BUILD_STEP Print checked out chromium revision@@@"
  git log -1

  cd ${ANDROID_SRC_ROOT}
}

function bb_webview_gyp {
  echo "@@@BUILD_STEP Run gyp_webview@@@"
  cd ${ANDROID_SRC_ROOT}/external/chromium_org
  bb_run_step "./android_webview/tools/gyp_webview"
  cd ${ANDROID_SRC_ROOT}
}
