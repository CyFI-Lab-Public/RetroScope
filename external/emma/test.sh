#!/bin/bash
#
# Copyright 2009 Google Inc. All Rights Reserved.
# Author: weasel@google.com (Tim Baverstock)
#
# This program and the accompanying materials are made available under
# the terms of the Common Public License v1.0 which accompanies this
# distribution, and is available at http://www.eclipse.org/legal/cpl-v10.html
#
# This script tests the emma jar from the sources in this directory.
# This script has to be run from its current directory ONLY.
# Sample usages:
# To just test emma.jar:
# ./test.sh

TESTDIR=/tmp/test-emma/$$
JAVADIR=$TESTDIR/android3/java
SOURCEDIR=$JAVADIR/com/android/bunnies
mkdir -p $SOURCEDIR

cat <<END >$SOURCEDIR/Bunny.java
package com.android.bunnies;

import java.util.Random;

public class Bunny {
  int randomNumber1 = (new Random()).nextInt();

  int randomNumber2;

  {
    Random r = new Random();
    randomNumber2 = r.nextInt();
  }

  int addOne(int a) {
    int b = a + 1;
    return identity(a + 1)
            ? 1
            : 0;
  }

  int dontAddOne(int a) {
    return a;
  }

  boolean identity(int a) {
    return a != a;
  }

  public static void main(String[] args) {
    Bunny thisThing = new Bunny();
    SubBunny thatThing = new SubBunny();
    System.out.println(thisThing.addOne(2));
    System.out.println(thatThing.addOne(2));
  }
}
END
cat <<END >$SOURCEDIR/SubBunny.java
package com.android.bunnies;
import com.android.bunnies.Bunny;
class SubBunny extends Bunny {
  int addOne(int a) {
    int b = a + 2;
    return identity(a) && identity(b) || identity(b)
            ? 1
            : 0;
  }

  boolean identity(int a) {
    return a == a;
  }
}
END

GOLDEN=$TESTDIR/golden.lcov
cat <<END >$GOLDEN
SF:com/android/bunnies/SubBunny.java
FN:5,SubBunny::addOne (int): int
FNDA:1,SubBunny::addOne (int): int
FN:12,SubBunny::identity (int): boolean
FNDA:1,SubBunny::identity (int): boolean
FN:3,SubBunny::SubBunny (): void
FNDA:1,SubBunny::SubBunny (): void
DA:3,1
DA:5,1
DA:6,1
DA:12,1
end_of_record
SF:com/android/bunnies/Bunny.java
FN:23,Bunny::dontAddOne (int): int
FNDA:0,Bunny::dontAddOne (int): int
FN:27,Bunny::identity (int): boolean
FNDA:1,Bunny::identity (int): boolean
FN:16,Bunny::addOne (int): int
FNDA:1,Bunny::addOne (int): int
FN:5,Bunny::Bunny (): void
FNDA:1,Bunny::Bunny (): void
FN:31,Bunny::main (String []): void
FNDA:1,Bunny::main (String []): void
DA:5,1
DA:6,1
DA:11,1
DA:12,1
DA:13,1
DA:16,1
DA:17,1
DA:23,0
DA:27,1
DA:31,1
DA:32,1
DA:33,1
DA:34,1
DA:35,1
end_of_record
END

javac -g $(find $SOURCEDIR -name \*.java)

COVERAGE=$TESTDIR/coverage.dat
java -cp dist/emma.jar emmarun -r lcov -cp $JAVADIR \
     -sp $JAVADIR -Dreport.lcov.out.file=$COVERAGE com.android.bunnies.Bunny

# Don't really need to test these separately, but it's useful to me for now.

if ! diff <(sort $GOLDEN) <(sort $COVERAGE) >$TESTDIR/diff-sorted; then
  echo Tests failed: Additional or missing lines: See $TESTDIR/diff-sorted
  exit
fi
if ! diff $GOLDEN $COVERAGE >$TESTDIR/diff-ordered; then
  echo Tests failed: same lines, different order: See $TESTDIR/diff-ordered
  exit
fi
rm -rf $TESTDIR
echo Tests passed.

