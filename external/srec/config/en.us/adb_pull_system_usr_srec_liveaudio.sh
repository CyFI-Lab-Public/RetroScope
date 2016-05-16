# Run this from Ubuntu to copy files from device

export TESTDIR=/system/usr/srec

adb pull $TESTDIR/config/en.us/recog4_SHIP_liveaudio.res recog4_SHIP_liveaudio.res
