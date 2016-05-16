/system/bin/SRecTest -parfile baseline11k.par -tcp tcp/bothtags5.tcp -datapath audio/ >out_SHIP_bothtags5.txt 2>&1

# mv is not supported on Android device
# mv -f recog4.res recog4_SHIP_bothtags5.res
# mv a1__VCE_Pete_Gonzalez.raw  linux_ship_a1__VCE_Pete_Gonzalez.raw
# mv a2__VCE_Andrew_Evans.raw   linux_ship_a2__VCE_Andrew_Evans.raw
# mv a3__VCE_Peter_Wilson.raw   linux_ship_a3__VCE_Peter_Wilson.raw
# mv a4__VCE_Edgar_Young.raw    linux_ship_a4__VCE_Edgar_Young.raw
# mv a5__VCE_John_Martinez.raw  linux_ship_a5__VCE_John_Martinez.raw

# use cat instead
cat recog4.res                  > recog4_SHIP_bothtags5.res
cat a1__VCE_Pete_Gonzalez.raw  > linux_ship_a1__VCE_Pete_Gonzalez.raw
cat a2__VCE_Andrew_Evans.raw   > linux_ship_a2__VCE_Andrew_Evans.raw
cat a3__VCE_Peter_Wilson.raw   > linux_ship_a3__VCE_Peter_Wilson.raw
cat a4__VCE_Edgar_Young.raw    > linux_ship_a4__VCE_Edgar_Young.raw
cat a5__VCE_John_Martinez.raw  > linux_ship_a5__VCE_John_Martinez.raw
