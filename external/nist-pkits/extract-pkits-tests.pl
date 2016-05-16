#!/usr/bin/env perl
#
# Copyright (C) 2012 The Android Open Source Project
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
#

#
# This script parses the NIST PKI Test Suite test descriptions document
# and creates a .java file with test cases.
#

use strict;

my $enabled = 0;
my $readingPath = 0;
my $sectionName;
my $testNumber;
my $testName;
my $pathEntry = "";
my $expectedOutcome;
my @pathEntries;

my @usedFiles = ();

my $delimiter = "\x{2022}";
utf8::encode($delimiter);

if ($#ARGV != 2) {
    die "Usage: $0 <text-descriptions> <java-output> <used-files-output>";
}

open(DESC_FILE, "<", $ARGV[0]);
open(OUTPUT_FILE, ">", $ARGV[1]);
open(USED_FILES, ">", $ARGV[2]);

sub trim($) {
    my $s = shift;
    $s =~ s/^\s+//g;
    $s =~ s/\s+$//g;
    return $s;
}

sub printTest() {
    my @certNames;
    my @crlNames;

    foreach my $entry (@pathEntries) {
        $entry =~ s/ //g;
        $entry =~ s/-//g;
        my @parts = split(/,/, $entry);
        for my $part (@parts) {
            if ($part =~ /CRL[0-9]*$/) {
                my $crlName = $part . ".crl";
                push(@crlNames, $crlName);
                push(@usedFiles, "crls/" . $crlName);
            } else {
                my $certName = $part . ".crt";
                push(@certNames, $certName);
                push(@usedFiles, "certs/" . $certName);
            }
        }
    }

    print OUTPUT_FILE <<EOF;
    /** NIST PKITS test ${testNumber} */
    public void test${sectionName}_${testName}() throws Exception {
EOF
    print OUTPUT_FILE " " x 8 . "String trustAnchor = \"" . (shift @certNames) . "\";\n";

    print OUTPUT_FILE <<EOF;

        String[] certs = {
EOF

    # Print the CertPath in reverse order.
    for (0..$#certNames) {
        print OUTPUT_FILE " " x 16 . "\"${certNames[$#certNames - $_]}\",\n";
    }
    print OUTPUT_FILE <<EOF;
        };

        String[] crls = {
EOF
    foreach my $crlName (@crlNames) {
        print OUTPUT_FILE " " x 16 . "\"${crlName}\",\n";
    }
    print OUTPUT_FILE <<EOF;
        };

EOF
    if ($expectedOutcome) {
        print OUTPUT_FILE <<EOF;
        assertValidPath(trustAnchor, certs, crls);
EOF
    } else {
        print OUTPUT_FILE <<EOF;
        assertInvalidPath(trustAnchor, certs, crls);
EOF
    }

        print OUTPUT_FILE <<EOF;
    }

EOF
}

sub stopReadingPath() {
    if ($readingPath) {
        if (defined($pathEntry) and $pathEntry ne "") {
            push(@pathEntries, $pathEntry);
            $pathEntry = "";
        }

        printTest();
        @pathEntries = ();
        $readingPath = 0;
    }
}


while (<DESC_FILE>) {
    chomp;

    if ($_ =~ /^\s*4 Certification Path Validation Tests$/) {
        $enabled = 1;
        next;
    }

    #
    # TODO: this script needs to be fixed to support the test cases in
    # 4.8 to 4.12
    #

    if ($_ =~ /^\s*4\.8 Certificate Policies\s*$/) {
        stopReadingPath();
        $enabled = 0;

        print OUTPUT_FILE " "x4 . "// skipping sections 4.8 to 4.12\n\n";
        next;
    }

    if ($_ =~ /^\s*4\.13 Name Constraints\s*$/) {
        $enabled = 1;
        next;
    }

    if ($_ =~ /^\s*5 Relationship to Previous Test Suite\s*[^.]/) {
        stopReadingPath();
        $enabled = 0;
        exit;
    }

    if (!$enabled) {
        next;
    }

    if ($_ =~ /^\s*4\.[0-9]+ (.*)$/) {
        stopReadingPath();
        $sectionName = $1;
        $sectionName =~ s/ //g;
        $sectionName =~ s/-//g;
    }

    if ($_ =~ /^\s*(4\.[0-9]+\.[0-9]+) (.*)$/) {
        stopReadingPath();
        $testNumber = $1;
        $testName = $2;
        $testName =~ s/ //g;
        $testName =~ s/-//g;
    }

    if ($_ =~ /Expected Result:.*(should validate|should not validate)/) {
        if ($1 eq "should validate") {
            $expectedOutcome = 1;
        } else {
            $expectedOutcome = 0;
        }
    } elsif ($_ =~ /Expected Result:/) {
        die "Can not determine expected result for test:\n\t${testName}";
    }

    if ($_ =~ /^\s*Certification Path:/) {
        $readingPath = 1;
        next;
    }

    if ($readingPath) {
        # Page number from the PDF
        if (trim($_) =~ /^[0-9]+$/) {
            do {
                $_ = <DESC_FILE>;
                if ($_ =~ /^\s*$/) {
                    next;
                }
            } while (1);
        }

        if ($_ =~ /${delimiter}\s*(.*)$/u) {
            if (defined($pathEntry) and $pathEntry ne "") {
                push(@pathEntries, $pathEntry);
            }
            $pathEntry = trim($1);
        } else {
            if ($_ =~ /The certification path is composed of the following objects:(.*)$/) {
                $pathEntry = trim($1);
            } else {
                $pathEntry .= trim($_);
            }
        }
    }
}

print USED_FILES join("\n", keys %{{map{$_ => 1} @usedFiles}});

close(DESC_FILE);
close(OUTPUT_FILE);
close(USED_FILES);
