#!/bin/bash

OLD="$1"
NEW="$2"
REALOLD="$1"

# sanity check in input args
if [ -z "$OLD" ] || [ -z "$NEW" ]; then
    cat <<EOF
Usage: $0 <old> <new>
Changes the ADT plugin revision number.
Example:
  cd tools/eclipse
  scripts/update_version.sh 0.1.2 0.2.3
EOF
    exit 1
fi

# sanity check on current dir
if [ `basename "$PWD"` != "eclipse" ]; then
    echo "Please run this from tools/eclipse."
    exit 1
fi

# quote dots for regexps
OLD="${OLD//./\.}\.qualifier"
NEW="${NEW//./\.}\.qualifier"

# Now find the same files but this time use sed to replace in-place with
# the new pattern. Old files get backuped with the .old extension.
grep -rl "$OLD" * | grep -E "\.xml$|\.MF$" | xargs -n 1 sed -i "" "s/$OLD/$NEW/g"

echo "Remaining instances of $REALOLD"
# do another grep for older version without the qualifier. We don't
# want to replace those automatically as it could be something else.
# Printing out occurence helps find ones to update manually.
grep -r "$REALOLD" *

