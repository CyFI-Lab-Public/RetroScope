/*
 * Copyright (C) 2011 The Android Open Source Project
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
/**
 ******************************************************************************
 * @file        M4VIFI_Clip.h
 * @brief        Global Table definition
 * @note        This file defines the Clipping and Division table address
 ******************************************************************************
*/

#ifndef    _M4VIFI_CLIP_H_
#define    _M4VIFI_CLIP_H_

/* Clipping matrix for RGB values */
EXTERN CNST M4VIFI_UInt8    *M4VIFI_ClipTable_zero;
/* Division table for (65535/x); x = 0 to 512 */
EXTERN CNST M4VIFI_UInt16    *M4VIFI_DivTable_zero;

#endif /* _M4VIFI_CLIP_H_ */

/* End of file M4VIFI_Clip.h */

