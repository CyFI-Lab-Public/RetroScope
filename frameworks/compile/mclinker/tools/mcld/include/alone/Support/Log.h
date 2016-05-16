//===- Log.h --------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#ifndef ALONE_SUPPORT_LOG_H
#define ALONE_SUPPORT_LOG_H

#include <cstdio>

#define ALOGE(fmt, args...) \
printf("%s:%s:%d: " fmt, __FILE__, __FUNCTION__, __LINE__, args)

#endif // ALONE_SUPPORT_LOG_H
