/*
 * BSD LICENSE
 *
 * mp3 header and prasing
 * Copyright (c) 2011-2012, Intel Corporation
 * All rights reserved.
 *
 * Author: Vinod Koul <vinod.koul@linux.intel.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of Intel Corporation nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * LGPL LICENSE
 *
 * mp3 header and parsing
 * Copyright (c) 2011-2012, Intel Corporation.
 *
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 2.1, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to
 * the Free Software Foundation, Inc.,
 * 51 Franklin St - Fifth Floor, Boston, MA 02110-1301 USA.
 */


#ifndef __TINYMP3_H
#define __TINYMP3_H

#if defined(__cplusplus)
extern "C" {
#endif


#define MP3_SYNC 0xe0ff

const int mp3_sample_rates[3][3] = {
	{44100, 48000, 32000},        /* MPEG-1 */
	{22050, 24000, 16000},        /* MPEG-2 */
	{11025, 12000,  8000},        /* MPEG-2.5 */
};

const int mp3_bit_rates[3][3][15] = {
	{
		/* MPEG-1 */
		{  0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448}, /* Layer 1 */
		{  0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384}, /* Layer 2 */
		{  0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320}, /* Layer 3 */
	},
	{
		/* MPEG-2 */
		{  0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256}, /* Layer 1 */
		{  0,  8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}, /* Layer 2 */
		{  0,  8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}, /* Layer 3 */
	},
	{
		/* MPEG-2.5 */
		{  0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256}, /* Layer 1 */
		{  0,  8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}, /* Layer 2 */
		{  0,  8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}, /* Layer 3 */
	},
};

enum mpeg_version {
	MPEG1  = 0,
	MPEG2  = 1,
	MPEG25 = 2
};

enum mp3_stereo_mode {
	STEREO = 0x00,
	JOINT = 0x01,
	DUAL = 0x02,
	MONO = 0x03
};

#endif
