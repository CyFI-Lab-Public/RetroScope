# -*- mode: makefile -*-
# List of libcore directories to include in documentation.
# Shared between libcore and frameworks/base.

define libcoredoc-all-java-files-under
$(patsubst ./%,%, \
  $(shell cd $(1) ; \
          find $(2) -name "*.java" -and -not -name ".*") \
 )
endef

# List of libcore javadoc source files
# 
# Note dalvik/system is non-recursive to exclude dalvik.system.profiler
#
# $(1): directory for search (to support use from frameworks/base)
define libcore_to_document
 $(call libcoredoc-all-java-files-under,$(1), \
   dalvik/src/main/java/dalvik/system/ -maxdepth 1) \
 $(call libcoredoc-all-java-files-under,$(1), \
   dalvik/src/main/java/dalvik/annotation \
   dalvik/src/main/java/dalvik/bytecode \
   json/src/main/java \
   libdvm/src/main/java/dalvik \
   libdvm/src/main/java/java \
   luni/src/main/java/java \
   luni/src/main/java/javax \
   luni/src/main/java/org/xml/sax \
   luni/src/main/java/org/w3c \
   xml/src/main/java/org/xmlpull/v1)
endef
