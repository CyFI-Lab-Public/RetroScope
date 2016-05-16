/* added 10/26/2010 to deal with both archs on Mac */

#ifdef __BIG_ENDIAN__
#include "gphoto2-endian-ppc.h"
#else
#include "gphoto2-endian-intel.h"
#endif
