
use Getopt::Long;

$rc = GetOptions("pht=s" => \$phtfile,
		 "i=s" => \$oklongfile,
		 "ok=s" => \$okfile);

load_phtfile($phtfile) if(defined $phtfile);

sub load_phtfile
{
    my $phtfile = shift(@_);
    open(PHT, "<$phtfile") || die "error opening phtfile $phtfile\n";
    print STDERR "using phtfile $phtfile\n";
    <PHT>; # header
    while(<PHT>) {
	s/\s+$//g;
	($trash,$lph,$sph,$num_states) = split(/\s+/);
	$lph_for_sph{$sph} = $lph;
	$sph_for_lph{$lph} = $sph;
	$lphhash{$lph}++;
    }
    close(PHT);
    $sph = $lph = "&";
    $lph_for_sph{$sph} = $lph;
    $sph_for_lph{$lph} = $sph;
    $lphhash{$lph}++;
    $sph = "#"; $lph = "iwt";
    $lph_for_sph{$sph} = $lph;
    $sph_for_lph{$lph} = $sph;
    $lphhash{$lph}++;
}

open(HH, "<$oklongfile") || die "error opening okfile $oklongfile\n";
open(OO, ">$okfile") || die "error opening output dict $okfile\n";
while(<HH>) {
    s/\s+$//;
    if(/^LANG\s*=\s*(\S+)/) { # LANG = EN-US
	my $language = lc($1);
	my $language_header_line = $_;
	$language =~ s/\-/\./g;
	if(!defined $phtfile) {
	    die "Error: ESRSDK is not defined\n" if(!defined $ENV{ESRSDK});
	    $phtfile = "$ENV{ESRSDK}/config/$language/models/generic.pht";
	    load_phtfile( $phtfile);
	}
	print OO "$language_header_line\n";
	next;
    }
    s/\s+$//;
    s/^\s+//;
    if(/\#\#/) {
	next if($skip_funnies);
	s/\#\#.*$//;
    }
    ($word, $pron) = split(/\s*\t\s*/, $_, 2);
    
    @lphlist = split(/\s+/, $pron);
    @sphlist = ();
    foreach $lph (@lphlist) {
	die "error: unknown lph $lph in $word\n" if(!defined  $sph_for_lph{$lph});
	push(@sphlist, $sph_for_lph{$lph});
    }
    $sphPron = join("",@sphlist);
    print OO "$word $sphPron\n";
}

close(HH);
close(OO);
		 
