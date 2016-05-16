#!/usr/bin/perl
print "Content-type: text/plain\n\n";
read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
print $buffer;
exit 0;
