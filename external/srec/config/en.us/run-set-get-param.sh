/system/bin/SRecTest -parfile baseline11k.par -tcp tcp/set_get_param.tcp -datapath wave/ >out_SHIP_set_get_param.txt 2>&1

# mv is not supported on Android device
# mv -f recog4.res recog4_SHIP_set_get_param.res

# use cat instead
cat recog4.res > recog4_SHIP_set_get_param.res
