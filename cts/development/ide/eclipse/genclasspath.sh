#!/bin/bash

# Outputs a classpath file containing classpaths from development/ide/eclipse
# and additional classpaths from cts/development/ide/eclipse.
#
# From your $ANDROID_BUILD_TOP directory:
# ./cts/development/ide/eclipse/genclasspath.sh > .classpath

if [[ -z $ANDROID_BUILD_TOP ]]; then
  echo "Run 'lunch' to set \$ANDROID_BUILD_TOP" >&2
  exit 1
fi

echo '<?xml version="1.0" encoding="UTF-8"?>'
echo '<classpath>'
cat $ANDROID_BUILD_TOP/cts/development/ide/eclipse/.classpath | grep classpathentry
cat $ANDROID_BUILD_TOP/development/ide/eclipse/.classpath | grep classpathentry
echo '</classpath>'
