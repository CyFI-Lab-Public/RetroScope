# GNU/Linux makefile for ProGuard.

ANT_HOME = /usr/local/java/ant
WTK_HOME = /usr/local/java/wtk

PROGUARD_HOME := $(subst ./..,..,$(subst /build/..,/,$(dir $(MAKEFILE_LIST))..))
SRC     = $(PROGUARD_HOME)/src
CLASSES = $(PROGUARD_HOME)/classes
LIB     = $(PROGUARD_HOME)/lib

ANT_JAR = $(ANT_HOME)/lib/ant.jar
WTK_JAR = $(WTK_HOME)/wtklib/kenv.zip

CLASSPATH = $(ANT_JAR):$(WTK_JAR)

PROGUARD     = proguard/ProGuard
PROGUARD_GUI = proguard/gui/ProGuardGUI
RETRACE      = proguard/retrace/ReTrace
ANT_TASK     = proguard/ant/ProGuardTask
WTK_PLUGIN   = proguard/wtk/ProGuardObfuscator

TARGETS = $(PROGUARD) $(PROGUARD_GUI) $(RETRACE) $(ANT_TASK) $(WTK_PLUGIN)

JAVAC_OPTIONS = -nowarn -Xlint:none -classpath $(CLASSPATH) -sourcepath $(SRC) -d $(CLASSES)

# Command sequence definitions for creating jars.

define createjar
  jar -cfm $(LIB)/$@.jar $(SRC)/$(dir $<)MANIFEST.MF \
    -C $(CLASSES) $(dir $<)
endef

define updatejar
  jar -uf $(LIB)/proguard.jar \
    -C $(CLASSES) $(dir $<)
endef

# The various targets.

all:     basic options
basic:   proguard proguardgui retrace
options: anttask wtkplugin

proguard: $(PROGUARD)
	$(createjar)

proguardgui: proguard
proguardgui: $(PROGUARD_GUI)
	$(createjar)

retrace: $(RETRACE)
	$(createjar)

anttask: $(ANT_JAR)
anttask: $(PROGUARD)
anttask: $(ANT_TASK)
	$(updatejar)

wtkplugin: $(WTK_JAR)
wtkplugin: $(PROGUARD)
wtkplugin: $(WTK_PLUGIN)
	$(updatejar)

clean:
	-rm -fr $(CLASSES) $(LIB)


define RESOURCES
  $(shell find $(SRC)/$(dir $(1)) -maxdepth 1 \( -name \*.properties -o -name \*.png -o -name \*.gif -o -name \*.pro \) -printf $(CLASSES)/$(dir $(1))%P\\n)
endef

define TARGETRULE
  $(1): $(CLASSES) $(CLASSES)/$(1).class $(call RESOURCES,$(1)) $(LIB)
endef

$(foreach TARGET,$(TARGETS),$(eval $(call TARGETRULE,$(TARGET))))

$(CLASSES) $(LIB):
	-mkdir -p $@

$(CLASSES)/%.class: $(SRC)/%.java
	javac $(JAVAC_OPTIONS) $^

$(CLASSES)/%.properties $(CLASSES)/%.png $(CLASSES)/%.gif $(CLASSES)/%.pro:
	cp $(subst $(CLASSES),$(SRC),$@) $@

%.jar %.zip:
	echo "Please make sure the path to $@ is set"
	echo "correctly in this $(strip $(MAKEFILE_LIST))."
	echo "Alternatively, if you don't need the corresponding option,"
	echo "you can run `make' with the option -k."
	find $@

.PHONY: all basic options proguard proguardgui retrace anttask wtkplugin clean $(TARGETS) $(OPTIONAL_TARGETS)
