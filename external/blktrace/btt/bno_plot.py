#! /usr/bin/env python
#
# btt blkno plotting interface
#
#  (C) Copyright 2008 Hewlett-Packard Development Company, L.P.
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
"""
bno_plot.py
	[ -h | --help       ]
	[ -K | --keys-below ]
	[ -v | --verbose    ]
	[ <file...>         ]

Utilizes gnuplot to generate a 3D plot of the block number output
from btt.  If no <files> are specified, it will utilize all files
generated after btt was run with -B blknos (meaning: all files of the
form blknos*[rw].dat).

The -K option forces bno_plot.py to put the keys below the graph,
typically all keys for input files are put in the upper right corner
of the graph. If the number of devices exceed 10, then bno_plot.py will
automatically push the keys under the graph.

To exit the plotter, enter 'quit' or ^D at the 'gnuplot> ' prompt.
"""

import getopt, glob, os, sys, tempfile

verbose	= 0
cmds	= """
set title 'btt Generated Block Accesses'
set xlabel 'Time (secs)'
set ylabel 'Block Number'
set zlabel '# Blocks per IO'
set grid
"""


#-----------------------------------------------------------------------------
def parse_args(in_args):
	global verbose

	keys_below = False
	s_opts = 'hKv'
	l_opts = [ 'help', 'keys-below', 'verbose' ]

	try:
		(opts, args) = getopt.getopt(in_args, s_opts, l_opts)
	except getopt.error, msg:
		print >>sys.stderr, msg
		print >>sys.stderr, __doc__
		sys.exit(1)

	for (o, a) in opts:
		if o in ('-h', '--help'):
			print __doc__
			sys.exit(0)
		elif o in ('-v', '--verbose'):
			verbose += 1
		elif o in ('-K', '--keys-below'):
			keys_below = True

	if len(args) > 0:	bnos = args
	else:			bnos = glob.glob('blknos*[rw].dat')

	return (bnos, keys_below)

#-----------------------------------------------------------------------------
if __name__ == '__main__':
	(bnos, keys_below) = parse_args(sys.argv[1:])

	if verbose:
		print 'Using files:',
		for bno in bnos: print bno,
		if keys_below:	print '\nKeys are to be placed below graph'
		else:		print ''

	tmpdir = tempfile.mktemp()
	os.mkdir(tmpdir)

	plot_cmd = None
	for f in bnos:
		t = '%s/%s' % (tmpdir, f)

		fo = open(t, 'w')
		for line in open(f, 'r'):
			fld = line.split(None)
			print >>fo, fld[0], fld[1], int(fld[2])-int(fld[1])
		fo.close()

		t = t[t.rfind('/')+1:]
		if plot_cmd == None: plot_cmd = "splot '%s'" % t
		else:                plot_cmd = "%s,'%s'" % (plot_cmd, t)

	fo = open('%s/plot.cmds' % tmpdir, 'w')
	print >>fo, cmds
	if len(bnos) > 10 or keys_below: print >>fo, 'set key below'
	print >>fo, plot_cmd
	fo.close()

	pid = os.fork()
	if pid == 0:
		cmd = '/usr/bin/gnuplot %s/plot.cmds -' % tmpdir

		if verbose: print 'Executing %s' % cmd

		cmd = cmd.split(None)
		os.chdir(tmpdir)
		os.execvp(cmd[0], cmd)
		sys.exit(1)

	os.waitpid(pid, 0)
	os.system('/bin/rm -rf ' + tmpdir)
