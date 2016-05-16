CC	= gcc
CFLAGS	= -Wall -O2 -g -W
ALL_CFLAGS = $(CFLAGS) -D_GNU_SOURCE -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64
PROGS	= blkparse blktrace verify_blkparse blkrawverify blkiomon
LIBS	= -lpthread
SCRIPTS	= btrace

ALL = $(PROGS) $(SCRIPTS) btt/btt btreplay/btrecord btreplay/btreplay \
      btt/bno_plot.py

all: $(ALL)

btt/btt:
	$(MAKE) -C btt

btreplay/btrecord:
	$(MAKE) -C btreplay

btreplay/btreplay:
	$(MAKE) -C btreplay

%.o: %.c
	$(CC) -o $*.o -c $(ALL_CFLAGS) $<

blkparse: blkparse.o blkparse_fmt.o rbtree.o act_mask.o
	$(CC) $(ALL_CFLAGS) -o $@ $(filter %.o,$^)

blktrace: blktrace.o act_mask.o
	$(CC) $(ALL_CFLAGS) -o $@ $(filter %.o,$^) $(LIBS)

verify_blkparse: verify_blkparse.o
	$(CC) $(ALL_CFLAGS) -o $@ $(filter %.o,$^)

blkrawverify: blkrawverify.o
	$(CC) $(ALL_CFLAGS) -o $@ $(filter %.o,$^)

blkiomon: blkiomon.o rbtree.o
	$(CC) $(ALL_CFLAGS) -o $@ $(filter %.o,$^) $(LIBS) -lrt

$(PROGS): | depend

docs:
	$(MAKE) -C doc all
	$(MAKE) -C btt docs
	$(MAKE) -C btreplay docs

docsclean:
	$(MAKE) -C doc clean
	$(MAKE) -C btt clean
	$(MAKE) -C btreplay clean

depend:
	@$(CC) -MM $(ALL_CFLAGS) *.c 1> .depend

INSTALL = install
prefix = /usr/local
bindir = $(prefix)/bin
mandir = $(prefix)/man
RPMBUILD = rpmbuild
TAR = tar

export prefix INSTALL TAR

dist: btrace.spec
	git-tar-tree HEAD btrace-1.0 > btrace-1.0.tar
	@mkdir -p btrace-1.0
	@cp btrace.spec btrace-1.0
	$(TAR) rf btrace-1.0.tar btrace-1.0/btrace.spec
	@rm -rf btrace-1.0
	@bzip2 btrace-1.0.tar

rpm: dist
	$(RPMBUILD) -ta btrace-1.0.tar.bz2

clean: docsclean
	-rm -f *.o $(PROGS) .depend btrace-1.0.tar.bz2
	$(MAKE) -C btt clean
	$(MAKE) -C btreplay clean

install: all
	$(INSTALL) -m 755 -d $(DESTDIR)$(bindir)
	$(INSTALL) -m 755 -d $(DESTDIR)$(mandir)/man1
	$(INSTALL) -m 755 -d $(DESTDIR)$(mandir)/man8
	$(INSTALL) -m 755 $(ALL) $(DESTDIR)$(bindir)
	$(INSTALL) -m 644 doc/*.1 $(DESTDIR)$(mandir)/man1
	$(INSTALL) -m 644 doc/*.8 $(DESTDIR)$(mandir)/man8

ifneq ($(wildcard .depend),)
include .depend
endif
