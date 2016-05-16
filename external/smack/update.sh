#!/bin/bash

rm -rf src
cd asmack-master
rm -rf src build
./build.bash
mv build/src/trunk ../src
rm -rf src build
