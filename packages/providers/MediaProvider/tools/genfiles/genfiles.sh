#!/bin/bash

#
# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.




# This script queries a media provider database, and generates a script to
# approximately recreate the same file system structure on another device,
# using dummy files.

EXTERNAL=$2
if [ "$EXTERNAL" == "" ]
then
    EXTERNAL="/storage"
fi


if [ "$1" == "" ]
then
    echo "Usage: $0 <file.db> [external storage root]"
    exit 2
fi

if [ ! -f "$1" ]
then
    echo "Couldn't find file $1"
    exit 3
fi

# generate script to generate directory structure and content
sqlite3 $1 "select format, media_type, mime_type, case when substr(_data,-1) is '\' then substr(_data,1,length(_data)-1) else _data end from files where _data like '"$EXTERNAL"/%';" | {

MKDIRS=/tmp/mkdirs$$
CPFILES=/tmp/cpfiles$$

echo "# create directories" > $MKDIRS
echo "# copy files" > $CPFILES

IFS="|"
while read format mediatype mimetype data;
do
    if [ "$format" == "14337" ]
    then
        # jpeg
        echo "cat /storage/sdcard0/proto.jpg > \"$data\"" >> $CPFILES
    elif [ "$format" == "14347" ]
    then
        # png
        echo "cat /storage/sdcard0/proto.png > \"$data\"" >> $CPFILES
    elif [ "$format" == "14343" -a "$mediatype" == "0" ]
    then
        # gif
        echo "cat /storage/sdcard0/proto.gif > \"$data\"" >> $CPFILES
    elif [ "$format" == "12292" -a "$mediatype" == "0" ]
    then
        # txt
        echo "cat /storage/sdcard0/proto.txt > \"$data\"" >> $CPFILES
    elif [ "$format" == "12293" -a "$mediatype" == "0" ]
    then
        # html
        echo "cat /storage/sdcard0/proto.html > \"$data\"" >> $CPFILES
    elif [ "$format" == "12297" ]
    then
        # mp3
        echo "cat /storage/sdcard0/proto.mp3 > \"$data\"" >> $CPFILES
    elif [ "$format" == "12296" ]
    then
        # wav
        echo "cat /storage/sdcard0/proto.wav > \"$data\"" >> $CPFILES
    elif [ "$format" == "12299" -a "$mediatype" == "0" ]
    then
        # m4v
        echo "cat /storage/sdcard0/proto.m4v > \"$data\"" >> $CPFILES
    elif [ "$format" == "12299" -a "$mediatype" == "3" ]
    then
        # mp4
        echo "cat /storage/sdcard0/proto.m4v > \"$data\"" >> $CPFILES
    elif [ "$format" == "12299" -a "$mediatype" == "2" ]
    then
        # m4a
        echo "cat /storage/sdcard0/proto.m4a > \"$data\"" >> $CPFILES
    elif [ "$format" == "47492" ]
    then
        # 3gp
        echo "cat /storage/sdcard0/proto.3gp > \"$data\"" >> $CPFILES
    elif [ "$format" == "47362" ]
    then
        # ogg
        echo "cat /storage/sdcard0/proto.ogg > \"$data\"" >> $CPFILES
    elif [ "$format" == "47747" ]
    then
        # doc
        echo "cat /storage/sdcard0/proto.doc > \"$data\"" >> $CPFILES
    elif [ "$format" == "12288" -a "$mediatype" == "0" ]
    then
        # unknown type
        echo "cat /storage/sdcard0/proto.dat > \"$data\"" >> $CPFILES
    elif [ "$format" == "12289" ]
    then
        # directory, ignore
        true
    elif [ "$format" == "12288" -a "$mediatype" == "4" ]
    then
        # playlist, ignore
        true
    else
        echo ignored: $format '|' $mediatype '|' $mimetype '|' $data
    fi
    echo mkdir -p \"$(dirname "$data")\" >> $MKDIRS
done

sort -u $MKDIRS > mkfiles.sh
cat $CPFILES >> mkfiles.sh
rm -rf $MKDIRS $CPFILES

}

# generate playlist files
sqlite3 $1 "select audio_playlists._data, audio._data from audio_playlists left outer join audio_playlists_map on audio_playlists._id=audio_playlists_map.playlist_id left outer join audio on audio_playlists_map.audio_id=audio._id order by audio_playlists_map.playlist_id,audio_playlists_map.play_order;" | {

IFS="|"
while read plist entry
do
    echo "echo \"$(basename $entry)\" >> \"$plist\"" >> mkfiles.sh
done
}

echo mkfiles.sh generated. Now run:
grep sdcard0\/proto mkfiles.sh |sed 's/cat \/storage\/sdcard0\//adb push protos\//' | sed 's/ > .*/ \/storage\/sdcard0\//'|sort -u
echo adb push mkfiles.sh /storage/sdcard0
echo adb shell sh /storage/sdcard0/mkfiles.sh

