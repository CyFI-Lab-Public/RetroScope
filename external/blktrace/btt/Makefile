#
# OCFLAGS:
# 	COUNT_IOS	- Counts struct io's left at end
# 	DEBUG		- Various and sundy debug asserts
# 	NDEBUG		- Defined: no asserts, Undefined: asserts
#

CC	= gcc
CFLAGS	= -Wall -W -O2 -g
INCS	= -I. -I..
XCFLAGS	= -D_GNU_SOURCE -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64
override CFLAGS += $(INCS) $(XCFLAGS)

PROGS	= btt
LIBS	= $(PLIBS) $(ELIBS)
OBJS	= args.o bt_timeline.o devmap.o devs.o dip_rb.o iostat.o latency.o \
	  misc.o output.o proc.o seek.o trace.o trace_complete.o trace_im.o \
	  trace_issue.o trace_queue.o trace_remap.o trace_requeue.o \
	  ../rbtree.o mmap.o trace_plug.o bno_dump.o unplug_hist.o q2d.o \
	  aqd.o plat.o

all: depend $(PROGS)

.PHONY : depend
depend: $(patsubst %.o,%.c,$(filter %.o,$(OBJS)))
	@$(CC) -MM $(CFLAGS) -I.. $^ 1> .depend

docs:
	$(MAKE) -C doc all

docsclean:
	$(MAKE) -C doc clean

clean: docsclean
	-rm -f *.o $(PROGS) .depend

%.o: %.c
	$(CC) $(CFLAGS) -c -o $*.o $<

btt: $(OBJS)
	$(CC) $(CFLAGS) -o $@ $(filter %.o,$^) $(LIBS)

ifneq ($(wildcard .depend),)
include .depend
endif
