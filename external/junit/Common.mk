# -*- mode: makefile -*-
# List of junit files include in documentation.
# Shared with frameworks/base.
# based off libcore/Docs.mk


# List of source to build into the core-junit library
#
core-junit-files := \
src/junit/framework/Assert.java \
src/junit/framework/AssertionFailedError.java \
src/junit/framework/ComparisonCompactor.java \
src/junit/framework/ComparisonFailure.java \
src/junit/framework/Protectable.java \
src/junit/framework/Test.java \
src/junit/framework/TestCase.java \
src/junit/framework/TestFailure.java \
src/junit/framework/TestListener.java \
src/junit/framework/TestResult.java \
src/junit/framework/TestSuite.java

# List of source to build into the android.test.runner library
#
junit-runner-files := \
src/junit/runner/BaseTestRunner.java \
src/junit/runner/TestRunListener.java \
src/junit/runner/TestSuiteLoader.java \
src/junit/runner/StandardTestSuiteLoader.java \
src/junit/runner/Version.java \
src/junit/textui/ResultPrinter.java \
src/junit/textui/TestRunner.java

# List of junit javadoc source files for Android public API
#
# $(1): directory for search (to support use from frameworks/base)
define junit_to_document
 $(core-junit-files) \
 $(junit-runner-files)
endef
