# Add a couple include paths to use stlport.
OPENCV := $(call my-dir)

# Make sure bionic is first so we can include system headers.
LOCAL_C_INCLUDES := \
	$(OPENCV)/cv/include  \
	$(OPENCV)/cxcore/include  \
	$(OPENCV)/cvaux/include  \
	$(OPENCV)/ml/include  \
	$(OPENCV)/otherlibs/highgui  \
	$(LOCAL_C_INCLUDES)
