# linux
SYSCONFDIR=	/etc
SBINDIR=	/sbin
LIBEXECDIR=	/libexec
DBDIR=		/var/db
RUNDIR=		/var/run
LIBDIR=		/lib
MANDIR=		/usr/share/man
CC=		gcc
CPPFLAGS+=	-D_BSD_SOURCE -D_XOPEN_SOURCE=600
SRCS+=		if-linux.c if-linux-wireless.c lpf.c
SRCS+=		platform-linux.c
LDADD+=		-lrt
COMPAT_SRCS+=	compat/arc4random.c
COMPAT_SRCS+=	compat/closefrom.c
COMPAT_SRCS+=	compat/strlcpy.c
SERVICEEXISTS=	/usr/sbin/invoke-rc.d --query --quiet $$1 start >/dev/null 2>\&1 || [ $$? = 104 ]
SERVICECMD=	/usr/sbin/invoke-rc.d $$1 $$2
SERVICESTATUS=	service_command $$1 status >/dev/null 2>\&1
HOOKSCRIPTS=	50-ntp.conf
