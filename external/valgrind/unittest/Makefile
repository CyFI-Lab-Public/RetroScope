SVN_ROOT=..
include ../common.mk

ifeq ($(OS), darwin)
DEFINES=-DOS_$(OS) -DARCH_$(ARCH) -DOS_$(DARWIN_VERSION)
else
DEFINES=-DOS_$(OS) -DARCH_$(ARCH)
endif

CFLAGS+=$(OPTFLAGS) $(ARCHFLAGS) $(OS_CFLAGS) $(DEFINES) $(EXTRA_CFLAGS)
CXXFLAGS+=$(OPTFLAGS) $(ARCHFLAGS) $(OS_CXXFLAGS) $(DEFINES) $(EXTRA_CXXFLAGS)
OPT_CXXFLAGS=$(CXXFLAGS) -O2 -g $(ARCHFLAGS) $(OS_CXXFLAGS) $(DEFINES) $(EXTRA_CXXFLAGS) -foptimize-sibling-calls
ifeq ($(OS), darwin)
LDFLAGS=$(LDOPT) $(ARCHFLAGS) $(STATICFLAGS) -framework Foundation
else
LDFLAGS=$(LDOPT) $(ARCHFLAGS) $(STATICFLAGS)
endif

# OS = linux, darwin, windows
# ARCH= x86, amd64
# OX = O0, O1, ...
# S= <empty>, -static
BUILD=${OS}-${ARCH}-${OX}${S}${EXTRA_BUILD_SUFFIX}
BIN=bin/

ifeq ($(OS), linux)
  OS_CXXFLAGS=-Wall -Werror -Wno-sign-compare -Wshadow -Wno-unused-function -std=c++0x
  OS_CFLAGS=-Wall -Werror -Wno-sign-compare -Wshadow -Wno-unused-function -std=c89
  LIBS=-lpthread -lrt
else ifeq ($(OS), darwin)
  OS_CXXFLAGS=-Wall -Werror -Wno-sign-compare -Wshadow -Wno-unused-function
  OS_CFLAGS=-Wall -Werror -Wno-sign-compare -Wshadow -Wno-unused-function -std=c89
  LIBS=-lpthread
else ifeq ($(OS), windows)
  OS_CXXFLAGS=/EHs- /EHa- /wd4530   /D_CRT_SECURE_NO_DEPRECATE /D_SECURE_SCL=0 /nologo /Gy /D "WIN32"
  OS_CFLAGS=/EHs- /EHa- /wd4530   /D_CRT_SECURE_NO_DEPRECATE /D_SECURE_SCL=0 /nologo /Gy /D "WIN32"
else
  OS=UNKNOWN_OS
endif

OUTPUT_TESTS_LIST=$(patsubst output_tests/%.cc,%,$(wildcard output_tests/*.cc))
OUTPUT_TESTS_OBJS=$(patsubst %,$(BIN)output_tests/%-$(BUILD)$(EXE),$(OUTPUT_TESTS_LIST))

all: $(BIN)racecheck_unittest-$(BUILD)$(EXE) $(BIN)demo_tests-$(BUILD)$(EXE) \
     $(BIN)bigtest-$(BUILD)$(EXE) output_tests \
		 $(BIN)memory_unittest-$(BUILD)$(EXE)
ifeq ($(OS), linux)
all: $(BIN)deadlock_unittest-$(BUILD)$(EXE)
endif

output_tests: bin/output_tests $(OUTPUT_TESTS_OBJS)

run_output_tests: output_tests
	BUILD=$(BUILD) BIN=$(BIN) ./run_output_tests.sh $(OUTPUT_TESTS_LIST)

w:
	$(MAKE) all OS=windows ARCH=x86
m: m32 m64
m32:
	$(MAKE) all OS=darwin ARCH=x86
m64:
	$(MAKE) all OS=darwin ARCH=amd64
l: l32 l64
l32:
	$(MAKE) all OS=linux ARCH=x86
l64:
	$(MAKE) all OS=linux ARCH=amd64

bin:
	mkdir -p bin
bin/output_tests:
	mkdir -p bin/output_tests

DYNAMIC_ANNOTATIONS_DIR=../dynamic_annotations
DYNAMIC_ANNOTATIONS_H=${DYNAMIC_ANNOTATIONS_DIR}/dynamic_annotations.h
DYNAMIC_ANNOTATIONS_C=${DYNAMIC_ANNOTATIONS_DIR}/dynamic_annotations.c
ifndef OMIT_DYNAMIC_ANNOTATIONS_IMPL
DYNAMIC_ANNOTATIONS_O=$(BIN)dynamic_annotations-$(BUILD).$(OBJ)
else
DYNAMIC_ANNOTATIONS_O=
endif
DYNAMIC_ANNOTATIONS=${DYNAMIC_ANNOTATIONS_C} ${DYNAMIC_ANNOTATIONS_H}

TEST_UTILS_O=$(BIN)test_utils-$(BUILD).$(OBJ)

$(BIN)racecheck_unittest-$(BUILD)$(EXE): $(BIN)racecheck_unittest-$(BUILD).$(OBJ) \
	                                 $(BIN)old_test_suite-$(BUILD).$(OBJ) \
					 $(BIN)opt_tests-$(BUILD).$(OBJ) \
					 $(BIN)atomicity_tests-$(BUILD).$(OBJ) \
		$(TEST_UTILS_O) $(GTEST_LIB)  ${DYNAMIC_ANNOTATIONS_O} | bin
	${LD} ${LDFLAGS} $^ $(LIBS) $(LINKO)$@

ifeq ($(OS), windows)
$(BIN)racecheck_unittest-$(BUILD)$(EXE): $(BIN)windows_tests-$(BUILD).$(OBJ)
else
$(BIN)racecheck_unittest-$(BUILD)$(EXE): $(BIN)posix_tests-$(BUILD).$(OBJ)
endif

ifeq ($(OS), linux)
ifndef OMIT_CPP0X
$(BIN)racecheck_unittest-$(BUILD)$(EXE): $(BIN)libstdc++_tests-$(BUILD).$(OBJ)
endif
endif

ifeq ($(OS), darwin)
$(BIN)racecheck_unittest-$(BUILD)$(EXE): $(BIN)mac_tests-$(BUILD).$(OBJ)
endif

$(BIN)deadlock_unittest-$(BUILD)$(EXE): $(BIN)deadlock_unittest-$(BUILD).$(OBJ) $(BIN)old_test_suite-$(BUILD).$(OBJ) \
		$(TEST_UTILS_O) $(GTEST_LIB)  ${DYNAMIC_ANNOTATIONS_O} | bin
	${LD} ${LDFLAGS} $^ $(LIBS) $(LINKO)$@

$(BIN)demo_tests-$(BUILD)$(EXE): $(BIN)demo_tests-$(BUILD).$(OBJ) \
		$(TEST_UTILS_O) $(GTEST_LIB) ${DYNAMIC_ANNOTATIONS_O} | bin
	${LD} $(LDFLAGS) $^ $(LIBS) $(LINKO)$@

$(BIN)output_tests/%-$(BUILD)$(EXE): $(BIN)output_tests/%-$(BUILD).$(OBJ) \
		${TEST_UTILS_O} ${DYNAMIC_ANNOTATIONS_O} | bin
	${LD} $(LDFLAGS) $^ $(LIBS) $(LINKO)$@

$(BIN)bigtest-$(BUILD)$(EXE): $(BIN)bigtest-$(BUILD).$(OBJ) ${DYNAMIC_ANNOTATIONS_O} | bin
	${LD} $(LDFLAGS) $^ $(LIBS) $(LINKO)$@

$(BIN)memory_unittest-$(BUILD)$(EXE): $(BIN)memory_unittest-$(BUILD).$(OBJ) $(BIN)old_test_suite-$(BUILD).$(OBJ) \
		$(TEST_UTILS_O) $(GTEST_LIB) ${DYNAMIC_ANNOTATIONS_O} | bin
	${LD} $(LDFLAGS) $^ $(LIBS) $(LINKO)$@

$(BIN)%-$(BUILD).$(OBJ): %.cc ${DYNAMIC_ANNOTATIONS} thread_wrappers.h \
		thread_wrappers_win.h thread_wrappers_pthread.h old_test_suite.h \
		test_utils.h gtest_fixture_injection.h | bin
	${CXX} ${CXXFLAGS} \
		 -I${DYNAMIC_ANNOTATIONS_DIR} -I${GTEST_ROOT}/include -I. \
		 -DDYNAMIC_ANNOTATIONS_ENABLED=1 -c $< $(O)$@

$(BIN)opt_tests-$(BUILD).$(OBJ): opt_tests.cc ${DYNAMIC_ANNOTATIONS} thread_wrappers.h thread_wrappers_win.h thread_wrappers_pthread.h old_test_suite.h test_utils.h | bin
	${CXX} ${OPT_CXXFLAGS} \
		 -I${DYNAMIC_ANNOTATIONS_DIR} -I${GTEST_ROOT}/include -I. \
		 -DDYNAMIC_ANNOTATIONS_ENABLED=1 -c $< $(O)$@

$(BIN)%-$(BUILD).$(OBJ): %.mm ${DYNAMIC_ANNOTATIONS} thread_wrappers.h thread_wrappers_win.h thread_wrappers_pthread.h old_test_suite.h test_utils.h | bin
	${CXX} ${CXXFLAGS} \
		 -I${DYNAMIC_ANNOTATIONS_DIR} -I${GTEST_ROOT}/include -I. \
		 -DDYNAMIC_ANNOTATIONS_ENABLED=1 -c $< $(O)$@


${DYNAMIC_ANNOTATIONS_O}: ${DYNAMIC_ANNOTATIONS_C}
	${CC} ${CFLAGS} $< \
		-I${DYNAMIC_ANNOTATIONS_DIR} -DDYNAMIC_ANNOTATIONS_ENABLED=1 \
		-c $(O)$@

clean: GTEST_CLEAN
	rm -rf bin racecheck_unittest demo_tests bigtest *.o *.O1 *.O2 *.obj *.dSYM *.exe *.pdb *.ilk *.idb *.manifest
