#*******************************************************************************
# Copyright (c) 2005, 2006 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************
#!/bin/sh

# simple sample script to fire an email from the local machine to some user to 
# notify them of a change to the watched feed

# Requirements:
# tested on Debian (Kubuntu), using
# exim 3.36-16
# mailx 1:8.1.2-0.20040524cvs-4

debug=0;
feedURL="";
xpath="";
newvalue="";
oldvalue="";

while [ "$#" -gt 0 ]; do
	case $1 in
		'-debug')
			debug=$2;
			shift 1
			;;
		'-feedURL')
			feedURL=$2;
			shift 1
			;;
		'-xpath')
			xpath=$2;
			shift 1
			;;
		'-oldvalue')
			oldvalue=$2;
			shift 1
			;;
		'-newvalue')
			newvalue=$2;
			shift 1
			;;
	esac
	shift 1
done

if [ $debug -gt 0 ]; then
  echo "[sendEmailAlert] Started `date +%H:%M:%S`. Executing with the following options:"
  echo "-debug $debug";
  echo "-feedURL $feedURL";
  echo "-xpath $xpath";
  echo "-oldvalue $oldvalue";
  echo "-newvalue $newvalue";
fi

tmpfile="/tmp/sendEmailAlert.sh.tmp";
echo "" > $tmpfile;

# compose message
echo "Eclipse RSS Feed has been updated." >> $tmpfile;
echo "" >> $tmpfile;
echo "Here's what happened:" >> $tmpfile;
echo "" >> $tmpfile;

if [ "x$xpath" != "x" ]; then    echo "Changed Node: "$xpath >> $tmpfile; fi
if [ "x$oldvalue" != "x" ]; then echo "Old Value:    "$oldvalue >> $tmpfile; fi
if [ "x$newvalue" != "x" ]; then echo "New Value:    "$newvalue >> $tmpfile; fi
if [ "x$feedURL" != "x" ]; then  echo "Feed URL:     "$feedURL >> $tmpfile; fi

echo "" >> $tmpfile;

#assemble mail info
toAddress="codeslave@ca.ibm.com";
fromAddress="Eclipse Build Team <NOSUCHADDRESS@eclipse.org>";
subject="Eclipse RSS Feed Updated!";
MAIL="/usr/bin/mail";

if [ $debug -gt 0 ]; then
    echo "Sending the following email using "$MAIL":";
    echo "--";
    echo "Subject: "$subject;
    echo "To: "$toAddress
    echo "From: "$fromAddress
    echo "--";
    cat $tmpfile;
    echo "--";
fi

# send message
cat $tmpfile | $MAIL -s "$subject" -a "From: $fromAddress" $toAddress;

# cleanup
rm -fr $tmpfile;

if [ $debug -gt 0 ]; then
    echo "Done.";
fi
