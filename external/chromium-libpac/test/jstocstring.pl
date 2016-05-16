#!/usr/bin/perl

print "Reading from $ARGV[0]\nWriting to $ARGV[1]\n";
open(LS, "ls $ARGV[0]|");
open(FILE, "> $ARGV[1]");
print FILE "// This file is auto generated using the following command.\n";
print FILE "// Do not modify.\n";
print FILE "// \t./jstocstring.pl $ARGV[0] $ARGV[1]\n";
print FILE "#ifndef PROXY_TEST_SCRIPT_H_\n";
print FILE "#define PROXY_TEST_SCRIPT_H_\n\n";

while (<LS>) {
    chomp();
    open(FH, "cat $ARGV[0]/$_|");
    if (s/\.js/_JS/) {
        $upper = uc();
        print FILE "#define $upper \\\n";
        while (<FH>) {
            s/\"/\\\"/g;
            chomp();
            print FILE "  \"",$_,"\\n\" \\\n";
        }
    }
    print FILE "\n"
}
print FILE "#endif //PROXY_TEST_SCRIPT_H_\n";
close(FILE);
