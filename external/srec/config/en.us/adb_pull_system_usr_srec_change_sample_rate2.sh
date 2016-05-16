# Run this from Ubuntu to copy files from device

export TESTDIR=/system/usr/srec

adb pull $TESTDIR/config/en.us/out_SHIP_change_sample_rate2.txt out_SHIP_change_sample_rate2.txt
adb pull $TESTDIR/config/en.us/recog4_SHIP_change_sample_rate2.res recog4_SHIP_change_sample_rate2.res
