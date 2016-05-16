#!/bin/bash

echo -n "Bugzilla login or email address: "
read LOGIN
echo -n "Bugzilla password: "
stty -echo
read PW
stty echo
echo
echo "Logging in and fetching cookie values..."
echo

#older versions of curl (like the one on emf) don't have -k
OUT=$(curl -k 2>&1| grep unknown)
if [[ $OUT ]];
then
	CURLARG=""
else
	CURLARG="-k"
fi
HEADERS=$(mktemp)
curl -s -S $CURLARG 'https://bugs.eclipse.org/bugs/index.cgi' -d "GoAheadAndLogIn=1&Bugzilla_login=$LOGIN&Bugzilla_password=$PW" -D $HEADERS >/dev/null
PW="$RANDOM $RANDOM $RANDOM $RANDOM"
VALUES=$(grep Set-Cookie $HEADERS | sed -e 's/.\{1,\}Bugzilla_\(login\(cookie\)\?=[0-9]\{1,\}\).\{1,\}/\1/')
rm -fr $HEADERS

if [[ $VALUES ]];
then
	#alternatively, you can do ./UpdateBugStateTask.sh 2>../properties/UpdateBugStateTask.properties
	echo "Paste the following into UpdateBugStateTask.properties:"
	echo "---- 8< ---- cut here ---- 8< ----"
	echo ""
	if [[ -e /proc/self/fd/2 ]];
	then
		echo "$VALUES" >/proc/self/fd/2
	else
		echo "$VALUES"
	fi
	echo ""
	echo "---- 8< ---- cut here ---- 8< ----"
else
	echo "Bugzilla didn't send us any cookie values, this means that either:"
	echo "   - you mistyped your login/password"
	echo "   - you can't reach Bugzilla for some reason"
	echo
	echo "Make sure that you can reach <https://bugs.eclipse.org/bugs/index.cgi> and try again."
fi
