/*
* Header file for using yaffs in an application via
* a direct interface.
*/


#ifndef __YAFFSCFG_H__
#define __YAFFSCFG_H__


#include "devextras.h"

#define YAFFSFS_N_HANDLES 200


typedef struct {
	const char *prefix;
	struct yaffs_DeviceStruct *dev;
} yaffsfs_DeviceConfiguration;


void yaffsfs_Lock(void);
void yaffsfs_Unlock(void);

__u32 yaffsfs_CurrentTime(void);

void yaffsfs_SetError(int err);

#endif

