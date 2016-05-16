/*
 * BSD LICENSE
 *
 * tinycompress utility functions
 * Copyright (c) 2011-2013, Intel Corporation
 * All rights reserved.
 *
 * Author: Vinod Koul <vinod.koul@intel.com>
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
 * tinycompress utility functions
 * Copyright (c) 2011-2013, Intel Corporation
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
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <linux/types.h>
#include <sys/time.h>
#define __force
#define __bitwise
#define __user
#include "tinycompress/tinycompress.h"


unsigned int compress_get_alsa_rate(unsigned int rate)
{
	switch (rate) {
	case 5512:
		return SNDRV_PCM_RATE_5512;
	case 8000:
		return SNDRV_PCM_RATE_8000;
	case 11025:
		return SNDRV_PCM_RATE_11025;
	case 16000:
		return SNDRV_PCM_RATE_16000;
	case 22050:
		return SNDRV_PCM_RATE_22050;
	case 32000:
		return SNDRV_PCM_RATE_32000;
	case 44100:
		return SNDRV_PCM_RATE_44100;
	case 48000:
		return SNDRV_PCM_RATE_48000;
	case 64000:
		return SNDRV_PCM_RATE_64000;
	case 88200:
		return SNDRV_PCM_RATE_88200;
	case 96000:
		return SNDRV_PCM_RATE_96000;
	case 176400:
		return SNDRV_PCM_RATE_176400;
	case 192000:
		return SNDRV_PCM_RATE_192000;
	default:
		return 0;
	}
}
