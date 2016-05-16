/system/bin/SRecTest -parfile baseline11k.par -tcp tcp/change_sample_rate2.tcp -datapath wave/ >out_SHIP_change_sample_rate2.txt 2>&1

# mv is not supported on Android device
# mv -f recog4.res recog4_SHIP_change_sample_rate2.res

# use cat instead
cat recog4.res > recog4_SHIP_change_sample_rate2.res
