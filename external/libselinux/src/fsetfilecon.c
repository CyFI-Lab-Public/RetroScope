#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/xattr.h>
#include "selinux_internal.h"
#include "policy.h"

int fsetfilecon(int fd, const security_context_t context)
{
	return fsetxattr(fd, XATTR_NAME_SELINUX, context, strlen(context) + 1,
			 0);
}

