#!/usr/localbin/perl

use Getopt::Long;
use File::Basename;
use lib dirname($0);

$assume_invocab = 0; # _when_semantics_missing 
$rc = GetOptions("add=s" => \@additional_fields,
		 "invocab" => \$assume_invocab, 
		 "quiet" => \$quiet,
		 "semantic" => \$try_semantic_validation,
		 "altsem=s" => \$altsemfile,
                 );

my @fields = ("file", "correct", "invocab", "gdiff", "sd", "sd13", "spf", "abs", "gdiffpf", "rejrslt", "rankc", "match", "ortho", "choice1", "choice2", "score1", "conf", "gender");

if($try_semantic_validation) {
    push(@additional_fields,"parsed_ortho");
}
push(@fields, @additional_fields);
foreach $additional_field (@additional_fields) {
    $additional_fieldh{$additional_field}++;
}

load_altsemfile($altsemfile) if($altsemfile);

$| = 1;

if(@ARGV[0] =~ /^@/) {
    $flist = substr($ARGV[0],1);
    @resfiles = `cat $flist`;
    grep { s/\s+$// } @resfiles;
} else {
    @resfiles = @ARGV;
}

foreach $resfile (@resfiles) {
    ($base = $resfile) =~ s/\.[a-z]+$//i;
    $utdfile = "$base.utd";

    # print "processing $resfile to $utdfile\n" unless($quiet);
    open(RES, "<$resfile") || die "error opening $resfile\n";
    open(UTD, ">$utdfile") || die "error opening $utdfile\n";
    $hUTD = \*UTD;
    undef %results;
    while(<RES>) {
	s/\s+$//;
        s/^\s+//;
	if(/^D:\s+(\S+)\s*$/) { # same as CREC
	    $file = $1;
	    if(defined %token) {
		process(\%token, \%results);
		dump_record($hUTD, \%token);
	    } else {
		dump_header($hUTD);
	    }
	    undef %token;
	    $token{file} = $file;
	    $file =~ /ENU-(\d\d\d)-/;
	    $token{gender} = $gender{$1};
	    $token{"snr"} = get_snr($file) if($additional_fieldh{"snr"});
	    $token{"snrr"} = sprintf("%.2d",int(get_snr($file)/5+0.5)*5) if($additional_fieldh{"snrr"});
	} elsif(/^C:\s+(.*)$/) { # same as CREC
	    $token{ortho} = normalize($1);
	} elsif(/^\s*(\S+) = (.*)$/) {
	    ($augkey,$augval) = ($1,$2);
	    if($augkey eq "feedback") {
		$token{parsed_ortho} = $augval;
		$token{invocab}++;
	    }
	} elsif(/^R:\s+(.*)$/) { # same as CREC
	    if(/<rejected/i || /<FAILED/i) {
		$token{rejrslt} = "f";
	    } else {
		# $token{topchoice} = $1;
		$token{rejrslt} = "a";
	    }
	} elsif(/^Sem[^:]+:  invocab=(\d)/) { # same as CREC
	    $token{invocab} = 1;
	} elsif(/^CSem:\s+([a-z]+.*)\s*$/i) { 
	    $token{parsed_ortho} = $1;
	} elsif(/^Sem:(\s+)(\S+)/) { # same as CREC
	    $token{invocab} = 0;
	} elsif(/^LITERAL\[\s*0\]\s*:\s*\'(.*)\'/) {
	    $choice = $1;
	    $token{choices}[0] = $choice;
	} elsif(/^LITERAL\[\s*(\d+)\]\s+:\s+\'(.*)\'/) {
	    $i = $1;
	    $choice = $2;
	    /.*\: \'(.*)\'/;
	    $choice = $1;
	    $token{choices}[$i] = $choice;
	} elsif(/^MEANING\[\s*(\d+)\]\s+:\s+\'(.*)\'/) {
	    $i = $1;
	    $choice = $2;
	    /.*\: \'(.*)\'/;
	    $choice = $1;
	    $choice =~ s/\s+$//;
	    $token{meanings}[$i] = $choice;
	} elsif(/^LITERAL\[(\d+)\]\[(\d+)\]\s+:\s+\'(.*)\'/) {
	    $i = $1;
	    $score = $2;
	    $token{scores}[$i] = $score;
	} elsif(/^RAW SCORE\s+:\s+\'(.*)\'/) {
	    $token{topscore} = $1;
	} elsif(/^gdiff\s+(.*)$/){
            $token{gdiff} = $1;
        } elsif(/^sd13\s+(.*)$/){
            $token{sd13} = $1; 
        } elsif(/^spf\s+(.*)$/){
            $token{spf} = $1;
        } elsif(/^abs\s+(.*)$/){
            $token{abs} = $1;
        } elsif(/^gdiffpf\s+(.*)$/){
            $token{gdiffpf} = $1;
        } elsif(/^sd\s+(.*)$/){ 
            $token{sd} = $1; 
        } elsif(/^CONFIDENCE SCORE\s+:\s+\'(.*)\'/) {
            $token{conf} = $1;
        }
    }
    process(\%token, \%results) if(defined %token);
    dump_record($hUTD, \%token) if(defined %token);
    close(UTD);
    close(RES);
    undef %token;
    $results{total} ||= 1;
    $rr = $results{correct}/$results{total} * 100;
    $rr = int($rr*10 + 0.5)/10;
    print sprintf("%-45s RR %4.1f %d/%d (%d oovs)\n", $base, $rr, $results{correct}, $results{total}, $results{numoovs});
}


sub process
{
    my $token = shift(@_);
    my $results = shift(@_);
    $token->{invocab} = 1 if($assume_invocab);
    if(defined $token{topchoice}) {
	$token->{choices}[0] = $token{topchoice};
    }
    if(defined $token{topscore}) {
	$token->{scores}[0] = $token{topscore};
    }
    my $ortho = lc($token->{ortho});
    my $topch = lc($token->{choices}[0]);

    $ortho =~ s/_/ /g;
    $topch =~ s/_/ /g;
    $topch =~ s/\s\s+/ /g;
    $ortho =~ s/\s\s+/ /g;
    if($token->{invocab} == 0) {
	$token->{correct} = "0"; 
	$results->{numoovs}++;
    } elsif($topch eq $ortho) {
	$results->{total}++;
	$results->{correct}++;
	$token->{correct} = "1";
    } else {
	$results->{total}++;
	# print "$token->{file} MEANINGCMP: ==$token->{meanings}[0]== ==$token->{parsed_ortho}==\n";
	if($altsemfile) {
	    if($token->{parsed_ortho} ne $csemtags{$token->{file}}) {
		# print "changing $token{parsed_ortho} ne $csemtags{$token->{file}}\n";
		$token->{parsed_ortho} = $csemtags{$token->{file}};
	    }
	}

	if(not $try_semantic_validation) {
	    $token->{correct} = "0";
	} else {
	    if($token->{meanings}[0] eq $token->{parsed_ortho} && length($token->{parsed_ortho})>0) {
		$token->{correct} = "1";
		$results->{correct}++ ;
	    } else {
		$token->{correct} = "0";
	    }
	}
    }
    $token->{rankc} = 0;
    my $nchoices = scalar(@{$token->{choices}});
    for($i=0; $i<$nchoices; $i++) {
	my $choice = lc $token->{choices}[$i];
	$choice =~ s/_/ /g;
	if($choice eq $ortho) {
	    $token->{rankc} = $i+1;
	    last;
	}
    }
    $token->{gender} = "?";
}

sub dump_record
{
    my $HH = shift(@_);
    my $token = shift(@_);
    foreach $field (@fields) {
          if ($field =~ /^sd13$/){
          print UTD "$token->{$field}" , ":";
	} elsif($field =~ /^(\S+)(\d+)$/) {
	  $name = "${1}s"; 
	  $num = $2 - 1;
	  print UTD "$token->{$name}[$num]", ":";
	} else{
          print UTD "$token->{$field}" , ":";
	} 
    }
    print UTD "\n";
}

sub dump_header
{
    my $HH = shift(@_);
    foreach $field (@fields) {
	print UTD "$field" , ":";
    }
    print UTD "\n";
}

sub normalize
{
    my $k = shift(@_);
    $k =~ s/\s\s+/ /g;
    $k =~ s/\:/\;/g;
    $k =~ s/\[[^\]]+\]//g;
    $k =~ s/^\s+//g;
    $k =~ s/\s+$//g;
    return $k;
}

sub load_altsemfile
{
    my $semfile = shift(@_);
    open(SM,"<$semfile") || die "error: opening $semfile\n";
    while(<SM>) {
	if(/D: (\S+)$/) {
	    $file = $1;
	    $file =~ s/\s+$//;
	} elsif(/^CSem:\s+([a-z]+.*)\s*$/i) { 
	    $csemtags{$file} = $1;
	    $csemtags{$file} =~ s/\s+$//;
	} elsif(/^Sem[^:]+:  invocab=(\d)/) { # same as CREC
	    $semtags{$file} = 1;
	} elsif(/^Sem:(\s+)(\S+)/) { # same as CREC
	    $semtags{$file} = 0;
	}
    }
    close(SM);
}

