/*
 * Copyright (C) 2010 NXP Semiconductors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*!
 * \file  phOsalNfc_Msg.h
 *
 * Project: NFC FRI / OSAL
 *
 * $Workfile:: phOsalNfc.h                   $ 
 * $Modtime::                                    $ 
 * $Author: frq09147 $
 * $Revision: 1.1 $
 *
 */

#ifndef PHOSALNFC_MSG_H
#define PHOSALNFC_MSG_H
#ifdef  _WIN32
#include <phOsalNfc_MsgX86.h>
#else
#ifdef __linux__
#include <phOsalNfc.h>
#else
#include <phOsalNfc_MsgRtk.h>
#endif
#endif

#endif  /*  PHOSALNFC_MSG_H  */
