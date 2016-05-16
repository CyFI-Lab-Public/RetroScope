all:
	gcc -O3 -Wall -o racoon -I. -Isrc/include-glibc -Isrc/libipsec \
	-Isrc/racoon -Isrc/racoon/missing -DHAVE_CONFIG_H -lcrypto \
	src/libipsec/pfkey.c \
	src/libipsec/ipsec_strerror.c \
	src/racoon/algorithm.c \
	src/racoon/crypto_openssl.c \
	src/racoon/genlist.c \
	src/racoon/handler.c \
	src/racoon/isakmp.c \
	src/racoon/isakmp_agg.c \
	src/racoon/isakmp_base.c \
	src/racoon/isakmp_cfg.c \
	src/racoon/isakmp_frag.c \
	src/racoon/isakmp_ident.c \
	src/racoon/isakmp_inf.c \
	src/racoon/isakmp_newg.c \
	src/racoon/isakmp_quick.c \
	src/racoon/isakmp_unity.c \
	src/racoon/isakmp_xauth.c \
	src/racoon/ipsec_doi.c \
	src/racoon/nattraversal.c \
	src/racoon/oakley.c \
	src/racoon/pfkey.c \
	src/racoon/policy.c \
	src/racoon/proposal.c \
	src/racoon/remoteconf.c \
	src/racoon/schedule.c \
	src/racoon/sockmisc.c \
	src/racoon/str2val.c \
	src/racoon/strnames.c \
	src/racoon/vendorid.c \
	src/racoon/vmbuf.c \
	main.c \
	setup.c
