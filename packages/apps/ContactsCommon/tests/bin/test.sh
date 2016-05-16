#!/bin/sh

adb shell am instrument -w com.android.contacts.common.unittest/android.test.InstrumentationTestRunner
