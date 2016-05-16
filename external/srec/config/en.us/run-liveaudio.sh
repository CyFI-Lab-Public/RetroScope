/system/bin/SRecTestAudio -parfile baseline11k.par -tcp tcp/recognize_10_live.tcp

# mv is not supported on Android device
# mv -f recog4.res recog4_SHIP_liveaudio.res

# use cat instead
cat recog4.res > recog4_SHIP_liveaudio.res
