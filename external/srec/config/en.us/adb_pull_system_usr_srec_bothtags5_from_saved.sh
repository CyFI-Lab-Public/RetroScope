# Run this from Ubuntu to copy files from device

export TESTDIR=/system/usr/srec

adb pull $TESTDIR/config/en.us/out_SHIP_bothtags5_from_saved.txt out_SHIP_bothtags5_from_saved.txt
adb pull $TESTDIR/config/en.us/recog4_SHIP_bothtags5_from_saved.res recog4_SHIP_bothtags5_from_saved.res

adb pull $TESTDIR/config/en.us/linux_ship_a1__VCE_Pete_Gonzalez_from_saved.raw  linux_ship_a1__VCE_Pete_Gonzalez_from_saved.raw
adb pull $TESTDIR/config/en.us/linux_ship_a2__VCE_Andrew_Evans_from_saved.raw   linux_ship_a2__VCE_Andrew_Evans_from_saved.raw
adb pull $TESTDIR/config/en.us/linux_ship_a3__VCE_Peter_Wilson_from_saved.raw   linux_ship_a3__VCE_Peter_Wilson_from_saved.raw
adb pull $TESTDIR/config/en.us/linux_ship_a4__VCE_Edgar_Young_from_saved.raw    linux_ship_a4__VCE_Edgar_Young_from_saved.raw
adb pull $TESTDIR/config/en.us/linux_ship_a5__VCE_John_Martinez_from_saved.raw  linux_ship_a5__VCE_John_Martinez_from_saved.raw

adb pull $TESTDIR/config/en.us/bothtags5_saved.g2g bothtags5_saved.g2g

