LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#####################################################################
# Compiled grammars, TCP test scripts, audio data and shell scripts #
#####################################################################

copy_from :=                                       \
    ../config/en.us/tcp/bothtags5.tcp              \
    ../config/en.us/tcp/bothtags5_from_saved.tcp   \
    ../config/en.us/tcp/change_sample_rate2.tcp    \
    ../config/en.us/tcp/recognize_1_live.tcp       \
    ../config/en.us/tcp/recognize_10_live.tcp      \
    ../config/en.us/tcp/set_get_param.tcp          \
    ../config/en.us/audio/v139/v139_024.nwv        \
    ../config/en.us/audio/v139/v139_254.nwv        \
    ../config/en.us/audio/v139/v139_127.nwv        \
    ../config/en.us/audio/v139/v139_107.nwv        \
    ../config/en.us/audio/v139/v139_248.nwv        \
    ../config/en.us/audio/v139/v139_077.nwv        \
    ../config/en.us/audio/v139/v139_040.nwv        \
    ../config/en.us/audio/v139/v139_021.nwv        \
    ../config/en.us/audio/v139/v139_206.nwv        \
    ../config/en.us/audio/v139/v139_103.nwv        \
    ../config/en.us/audio/v139/v139_113.nwv        \
    ../config/en.us/audio/v139/v139_067.nwv        \
    ../config/en.us/audio/v139/v139_202.nwv        \
    ../config/en.us/audio/v139/v139_007.nwv        \
    ../config/en.us/audio/v139/v139_189.nwv        \
    ../config/en.us/audio/m252/m252a12e.nwv        \
    ../config/en.us/audio/m252/m252a22e.nwv        \
    ../config/en.us/audio/m252/m252a10e.nwv        \
    ../config/en.us/audio/m252/m252a3fe.nwv        \
    ../config/en.us/audio/m252/m252a11e.nwv        \
    ../config/en.us/audio/m252/m252a21e.nwv        \
    ../config/en.us/audio/m252/m252a24e.nwv        \
    ../config/en.us/wave/dallas/0000/S072.nwf      \
    ../config/en.us/wave/dallas/0000/S074.nwf      \
    ../config/en.us/wave/dallas/0000/S075.nwf      \
    ../config/en.us/wave/dallas/0000/S076.nwf      \
    ../config/en.us/wave/dallas/0000/S077.nwf      \
    ../config/en.us/wave/dallas/0000/S079.nwf      \
    ../config/en.us/wave/dallas/0000/S080.nwf      \
    ../config/en.us/wave/dallas/0000/S083.nwf      \
    ../config/en.us/wave/dallas/0000/S086.nwf      \
    ../config/en.us/wave/dallas/0000/S088.nwf      \
    ../config/en.us/wave/dallas/0300/S052.nwf      \
    ../config/en.us/wave/dallas/0300/S053.nwf      \
    ../config/en.us/wave/dallas/0300/S057.nwf      \
    ../config/en.us/wave/dallas/0300/S063.nwf      \
    ../config/en.us/wave/dallas/0300/S065.nwf      \
    ../config/en.us/wave/dallas/0303/S080.nwf      \
    ../config/en.us/wave/dallas/0303/S083.nwf      \
    ../config/en.us/wave/dallas/0303/S084.nwf      \
    ../config/en.us/wave/dallas/0303/S087.nwf      \
    ../config/en.us/wave/dallas/0303/S088.nwf      \
    ../config/en.us/wave/dallas/0303/S089.nwf      \
    ../config/en.us/wave/dallas/0303/S090.nwf      \
    ../config/en.us/wave/dallas/0304/S052.nwf      \
    ../config/en.us/wave/dallas/0304/S054.nwf      \
    ../config/en.us/wave/dallas/0304/S055.nwf      \
    ../config/en.us/wave/dallas/0304/S074.nwf      \
    ../config/en.us/wave/dallas/0304/S075.nwf      \
    ../config/en.us/wave/dallas/0304/S076.nwf      \
    ../config/en.us/wave/dallas/0304/S077.nwf      \
    ../config/en.us/wave/dallas-8kHz/0301/S078.nwf \
    ../config/en.us/wave/dallas-8kHz/0301/S079.nwf \
    ../config/en.us/wave/dallas-8kHz/0301/S080.nwf \
    ../config/en.us/wave/dallas-8kHz/0301/S082.nwf \
    ../config/en.us/wave/dallas-8kHz/0301/S083.nwf \
    ../config/en.us/wave/dallas-8kHz/0301/S089.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S051.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S052.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S053.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S054.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S065.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S070.nwf \
    ../config/en.us/wave/dallas-8kHz/0302/S071.nwf \
    ../config/en.us/run-bothtags5.sh               \
    ../config/en.us/run-bothtags5-from-saved.sh    \
    ../config/en.us/run-change-sample-rate2.sh     \
    ../config/en.us/run-liveaudio.sh               \
    ../config/en.us/run-set-get-param.sh           \
    ../config/en.us/run-chmod.sh                   \

copy_to := $(addprefix $(TARGET_OUT)/usr/srec/config/,$(copy_from))

$(copy_to) : $(TARGET_OUT)/usr/srec/config/% : $(LOCAL_PATH)/% | $(ACP)
	$(transform-prebuilt-to-target)


#####################################################################
# Shell scripts for UAPI tests                                      #
#####################################################################

copy_from_scripts :=     \
    run_contacts.sh      \
    run_ERT.sh           \
    run_parameters.sh    \
    run_robustness1.sh   \
    run_robustness2.sh   \
    run_robustness3.sh   \
    run_UAPI_SrecTest.sh \
    run_UAPI_Test.sh     \
    run_voicetags1.sh    \
    run_chmod.sh         \

copy_to_scripts := $(addprefix $(TARGET_OUT)/usr/srec/,$(copy_from_scripts))

$(copy_to_scripts) : $(TARGET_OUT)/usr/srec/% : $(LOCAL_PATH)/% | $(ACP)
	$(transform-prebuilt-to-target)


#####################################################################
# PCM input for UAPI tests                                          #
#####################################################################

copy_from_pcm :=      \
    ./pcm/yes_08k.pcm \
    ./pcm/yes_11k.pcm \

#   ./pcm/yes_16k.pcm \
#   ./pcm/yes_22k.pcm \

copy_to_pcm := $(addprefix $(TARGET_OUT)/usr/srec/,$(copy_from_pcm))

$(copy_to_pcm) : $(TARGET_OUT)/usr/srec/% : $(LOCAL_PATH)/% | $(ACP)
	$(transform-prebuilt-to-target)


#####################################################################
# Run "make srec_test_files" to install the above files.            #
# By default they are not copied to /system/usr/srec since they are #
# only required by the SREC and UAPI tests.                         #
#####################################################################

srec_test_files : $(copy_to) $(copy_to_scripts) $(copy_to_pcm) srec_grammars
