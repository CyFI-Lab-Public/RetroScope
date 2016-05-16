# Config files to be installed.
SREC_CONFIG_TARGET_FILES := \
    $(addprefix $(TARGET_OUT)/usr/srec/config/en.us/, \
        baseline11k.par              \
        baseline8k.par               \
        baseline.par                 \
        dictionary/basic.ok          \
        dictionary/enroll.ok         \
        dictionary/cmu6plus.ok.zip   \
        g2p/en-US-ttp.data           \
        models/generic.swiarb        \
        models/generic11.lda         \
        models/generic11_f.swimdl    \
        models/generic11_m.swimdl    \
        models/generic8.lda          \
        models/generic8_f.swimdl     \
        models/generic8_m.swimdl) \
    $(addprefix $(TARGET_OUT)/usr/srec/config/en.us/grammars/, \
        VoiceDialer.g2g \
        boolean.g2g \
        phone_type_choice.g2g)
