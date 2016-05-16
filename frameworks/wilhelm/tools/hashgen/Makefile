all : IID_to_MPH.c

install : IID_to_MPH.c
	cp IID_to_MPH.c ../../src/autogen

CFLAGS = -I../../include -g -DNDEBUG

%.o : %.c
	gcc -c -o $@ $(CFLAGS) $<

IID_to_MPH.c : part1.c part2.c part3.c part4.c part5.c part6.c part7.c part8.c
	$(RM) $@
	cat part1.c part2.c part3.c part4.c part5.c part6.c part7.c part8.c >> $@

# part1.c is human-generated

part2.c : part23in.c
	$(RM) $@
	grep '#define MAX_HASH_VALUE' part23in.c > $@

part3.c : part23in.c
	$(RM) $@
# was 26,54p
	sed -n '/static const unsigned .* asso_values.. =/,/^    };/p' < part23in.c >> $@

# part4.c is human-generated

part5.c : part5gen
	./part5gen > $@

# part6.c is human-generated

part7.c : part23in.c hash.sed
# was 55p
	sed -n '/return.*asso_values/p' < part23in.c | sed -f hash.sed >> $@

# part8.c is human_generated

part23in.c : part23in.gperf
	gperf --null-strings --readonly-tables --compare-lengths part23in.gperf > part23in.c

part23in.gperf : frag1
	./frag1 > part23in.gperf

frag1 : frag1.o OpenSLES_IID.o
	gcc -o $@ $(CFLAGS) frag1.o OpenSLES_IID.o

part5gen.c : frag2.c part23in.c frag3.c
	$(RM) $@
	cp frag2.c $@
	grep '#define MAX_HASH_VALUE' part23in.c >> $@
# was 81,156p
	sed -n '/static const char . const wordlist.. =/,/^    };/p' < part23in.c >> $@
	cat frag3.c >> $@

part5gen : part5gen.o OpenSLES_IID.o interfaces.o
	gcc -o $@ $(CFLAGS) part5gen.o OpenSLES_IID.o interfaces.o

clean :
	$(RM) part5gen frag1 *.o
	$(RM) part23in.gperf part5gen.c part23in.c
	$(RM) part2.c part3.c part5.c part7.c

distclean : clean
	$(RM) IID_to_MPH.c
