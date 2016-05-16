#!/bin/sh

INPUT_FILE=$1

function fix_tr1() {
  grep $1 $INPUT_FILE > /dev/null 2>&1
  if [ $? -eq 0 ] ; then
    echo "Modifying $INPUT_FILE ..."
    sed 's/$1/$2/g' $INPUT_FILE > .tmp
    if [ $? -ne 0 ] ; then
      echo "sed failed!"
      return 1
    fi
    mv -f .tmp $INPUT_FILE
  fi
  return 0
}

fix_tr1 "tr1\/unordered_map" "unordered_map"
fix_tr1 "tr1/unordered_set" "unordered_set"
fix_tr1 "tr1::hash" "hash"
