use Getopt::Long;

$rc = GetOptions("pht=s" => \$phtfile,
		 "ok=s" => \$okfile,
		 "i=s" => \$okfile,
		 "otxt=s" => \$otxt,
		 "o=s" => \$otxt,
		 "showerrs" => \$showerrs);

if(defined $phtfile) {
    load_phtfile( $phtfile);
}

sub load_phtfile
{
    my $phtfile = shift(@_);
    $lphhash{"&"}++;
    $lph_for_sph{"&"} = "&";
    open(PHT, "<$phtfile") || die "error opening phtfile $phtfile\n";
    print STDERR "using phtfile $phtfile\n";
    <PHT>;  # header
    while(<PHT>) {
	s/\s+$//g;
	($trash,$lph,$sph,$num_states) = split(/\s+/);
	$lph_for_sph{$sph} = $lph;
	$lphhash{$lph}++;
    }
    close(PHT);
}

open(HH, "<$okfile") || die "error opening okfile $okfile\n";
open(OO, ">$otxt") || die "error opening output dict $otxt\n";
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
    ($word, $pron) = split(/\s+/);
    @sphlist = split(/ */, $pron);
    @lphlist = ();
    $nerrs = 0;
    foreach $sph (@sphlist) {
	$lph = $lph_for_sph{$sph};
	if(!defined $lph) {
	    warn "error: unknown sph $sph in $word $pron\n" ;
	    $lph = "($sph)";
	    $nerrs++;
	}
	push(@lphlist, $lph);
    }
    next if($nerrs && !$showerrs) ;
    print OO "$word \t @lphlist\n";
}
close(HH);
close(OO);
