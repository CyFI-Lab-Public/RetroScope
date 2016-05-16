# Run this from Ubuntu to copy files from device

export TESTDIR=/system/usr/srec

adb pull $TESTDIR/config/en.us/out_SHIP_bothtags5.txt out_SHIP_bothtags5.txt
adb pull $TESTDIR/config/en.us/recog4_SHIP_bothtags5.res recog4_SHIP_bothtags5.res

adb pull $TESTDIR/config/en.us/linux_ship_a1__VCE_Pete_Gonzalez.raw  linux_ship_a1__VCE_Pete_Gonzalez.raw
adb pull $TESTDIR/config/en.us/linux_ship_a2__VCE_Andrew_Evans.raw   linux_ship_a2__VCE_Andrew_Evans.raw
adb pull $TESTDIR/config/en.us/linux_ship_a3__VCE_Peter_Wilson.raw   linux_ship_a3__VCE_Peter_Wilson.raw
adb pull $TESTDIR/config/en.us/linux_ship_a4__VCE_Edgar_Young.raw    linux_ship_a4__VCE_Edgar_Young.raw
adb pull $TESTDIR/config/en.us/linux_ship_a5__VCE_John_Martinez.raw  linux_ship_a5__VCE_John_Martinez.raw
