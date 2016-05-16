# Run this from Ubuntu to copy files from device

export TESTDIR=/system/usr/srec

adb pull $TESTDIR/config/en.us/out_SHIP_set_get_param.txt out_SHIP_set_get_param.txt
adb pull $TESTDIR/config/en.us/recog4_SHIP_set_get_param.res recog4_SHIP_set_get_param.res
