#/bin/bash

CVSOPTS="-Q";

cd ${0%/*}; # cd to directory where this script is located; cvssrc/ must be beneath it

# pass in specific target folder(s) or do all in cvssrc/ folder
dir="";
if [ $# -gt 0 ]; then 
  while [ $# -gt 0 ]; do
    dir=$dir" $1"; shift 1;
  done
else 
  dir="$(ls -d cvssrc/*)"; 
fi

for i in $dir; do
  echo "[`date +%H:%M:%S`] Processing $i";
  cd $i;
  cvs $CVSOPTS up -Pd .;
  f=$(mktemp)
  cvs $CVSOPTS log > $f
  echo $f | /usr/local/bin/php ../../parsecvs.php;
  rm -f $f
  cd ../..;
  echo "[`date +%H:%M:%S`] done.";
done
