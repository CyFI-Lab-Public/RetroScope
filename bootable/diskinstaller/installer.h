/* commands/sysloader/installer/installer.h
 *
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __COMMANDS_SYSLOADER_INSTALLER_INSTALLER_H
#define __COMMANDS_SYSLOADER_INSTALLER_INSTALLER_H

#include <stdint.h>

/* image types */
#define INSTALL_IMAGE_RAW          1
#define INSTALL_IMAGE_EXT2         2
#define INSTALL_IMAGE_EXT3         3
#define INSTALL_IMAGE_EXT4         4
#define INSTALL_IMAGE_TARGZ        10


/* flags */
#define INSTALL_FLAG_RESIZE        0x1
#define INSTALL_FLAG_ADDJOURNAL    0x2

#endif /* __COMMANDS_SYSLOADER_INSTALLER_INSTALLER_H */

