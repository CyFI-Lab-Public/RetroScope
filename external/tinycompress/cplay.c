/*
 * BSD LICENSE
 *
 * tinyplay command line player for compress audio offload in alsa
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
 * tinyplay command line player for compress audio offload in alsa
 * Copyright (c) 2011-2012, Intel Corporation.
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

#include <stdint.h>
#include <linux/types.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <stdbool.h>
#include <getopt.h>
#include <sys/time.h>
#define __force
#define __bitwise
#define __user
#include "sound/compress_params.h"
#include "tinycompress/tinycompress.h"
#include "tinycompress/tinymp3.h"

static int verbose;

static void usage(void)
{
	fprintf(stderr, "usage: cplay [OPTIONS] filename\n"
		"-c\tcard number\n"
		"-d\tdevice node\n"
		"-b\tbuffer size\n"
		"-f\tfragments\n\n"
		"-v\tverbose mode\n"
		"-h\tPrints this help list\n\n"
		"Example:\n"
		"\tcplay -c 1 -d 2 test.mp3\n"
		"\tcplay -f 5 test.mp3\n");

	exit(EXIT_FAILURE);
}

void play_samples(char *name, unsigned int card, unsigned int device,
		unsigned long buffer_size, unsigned int frag);

struct mp3_header {
	uint16_t sync;
	uint8_t format1;
	uint8_t format2;
};

int parse_mp3_header(struct mp3_header *header, unsigned int *num_channels,
		unsigned int *sample_rate, unsigned int *bit_rate)
{
	int ver_idx, mp3_version, layer, bit_rate_idx, sample_rate_idx, channel_idx;

	/* check sync bits */
	if ((header->sync & MP3_SYNC) != MP3_SYNC) {
		fprintf(stderr, "Error: Can't find sync word\n");
		return -1;
	}
	ver_idx = (header->sync >> 11) & 0x03;
	mp3_version = ver_idx == 0 ? MPEG25 : ((ver_idx & 0x1) ? MPEG1 : MPEG2);
	layer = 4 - ((header->sync >> 9) & 0x03);
	bit_rate_idx = ((header->format1 >> 4) & 0x0f);
	sample_rate_idx = ((header->format1 >> 2) & 0x03);
	channel_idx = ((header->format2 >> 6) & 0x03);

	if (sample_rate_idx == 3 || layer == 4 || bit_rate_idx == 15) {
		fprintf(stderr, "Error: Can't find valid header\n");
		return -1;
	}
	*num_channels = (channel_idx == MONO ? 1 : 2);
	*sample_rate = mp3_sample_rates[mp3_version][sample_rate_idx];
	*bit_rate = (mp3_bit_rates[mp3_version][layer - 1][bit_rate_idx]) * 1000;
	if (verbose)
		printf("%s: exit\n", __func__);
	return 0;
}

int check_codec_format_supported(unsigned int card, unsigned int device, struct snd_codec *codec)
{
	if (is_codec_supported(card, device, COMPRESS_IN, codec) == false) {
		fprintf(stderr, "Error: This codec or format is not supported by DSP\n");
		return -1;
	}
	return 0;
}

static int print_time(struct compress *compress)
{
	unsigned int avail;
	struct timespec tstamp;

	if (compress_get_hpointer(compress, &avail, &tstamp) != 0) {
		fprintf(stderr, "Error querying timestamp\n");
		fprintf(stderr, "ERR: %s\n", compress_get_error(compress));
		return -1;
	} else
		fprintf(stderr, "DSP played %jd.%jd\n", (intmax_t)tstamp.tv_sec, (intmax_t)tstamp.tv_nsec*1000);
	return 0;
}

int main(int argc, char **argv)
{
	char *file;
	unsigned long buffer_size = 0;
	int c;
	unsigned int card = 0, device = 0, frag = 0;


	if (argc < 2)
		usage();

	verbose = 0;
	while ((c = getopt(argc, argv, "hvb:f:c:d:")) != -1) {
		switch (c) {
		case 'h':
			usage();
			break;
		case 'b':
			buffer_size = strtol(optarg, NULL, 0);
			break;
		case 'f':
			frag = strtol(optarg, NULL, 10);
			break;
		case 'c':
			card = strtol(optarg, NULL, 10);
			break;
		case 'd':
			device = strtol(optarg, NULL, 10);
			break;
		case 'v':
			verbose = 1;
			break;
		default:
			exit(EXIT_FAILURE);
		}
	}
	if (optind >= argc)
		usage();

	file = argv[optind];

	play_samples(file, card, device, buffer_size, frag);

	fprintf(stderr, "Finish Playing.... Close Normally\n");
	exit(EXIT_SUCCESS);
}

void play_samples(char *name, unsigned int card, unsigned int device,
		unsigned long buffer_size, unsigned int frag)
{
	struct compr_config config;
	struct snd_codec codec;
	struct compress *compress;
	struct mp3_header header;
	FILE *file;
	char *buffer;
	int size, num_read, wrote;
	unsigned int channels, rate, bits;

	if (verbose)
		printf("%s: entry\n", __func__);
	file = fopen(name, "rb");
	if (!file) {
		fprintf(stderr, "Unable to open file '%s'\n", name);
		exit(EXIT_FAILURE);
	}

	fread(&header, sizeof(header), 1, file);

	if (parse_mp3_header(&header, &channels, &rate, &bits) == -1) {
		fclose(file);
		exit(EXIT_FAILURE);
	}

	codec.id = SND_AUDIOCODEC_MP3;
	codec.ch_in = channels;
	codec.ch_out = channels;
	codec.sample_rate = compress_get_alsa_rate(rate);
	if (!codec.sample_rate) {
		fprintf(stderr, "invalid sample rate %d\n", rate);
		fclose(file);
		exit(EXIT_FAILURE);
	}
	codec.bit_rate = bits;
	codec.rate_control = 0;
	codec.profile = 0;
	codec.level = 0;
	codec.ch_mode = 0;
	codec.format = 0;
	if ((buffer_size != 0) && (frag != 0)) {
		config.fragment_size = buffer_size/frag;
		config.fragments = frag;
	} else {
		/* use driver defaults */
		config.fragment_size = 0;
		config.fragments = 0;
	}
	config.codec = &codec;

	compress = compress_open(card, device, COMPRESS_IN, &config);
	if (!compress || !is_compress_ready(compress)) {
		fprintf(stderr, "Unable to open Compress device %d:%d\n",
				card, device);
		fprintf(stderr, "ERR: %s\n", compress_get_error(compress));
		goto FILE_EXIT;
	};
	if (verbose)
		printf("%s: Opened compress device\n", __func__);
	size = config.fragment_size;
	buffer = malloc(size * config.fragments);
	if (!buffer) {
		fprintf(stderr, "Unable to allocate %d bytes\n", size);
		goto COMP_EXIT;
	}

	/* we will write frag fragment_size and then start */
	num_read = fread(buffer, 1, size * config.fragments, file);
	if (num_read > 0) {
		if (verbose)
			printf("%s: Doing first buffer write of %d\n", __func__, num_read);
		wrote = compress_write(compress, buffer, num_read);
		if (wrote < 0) {
			fprintf(stderr, "Error %d playing sample\n", wrote);
			fprintf(stderr, "ERR: %s\n", compress_get_error(compress));
			goto BUF_EXIT;
		}
		if (wrote != num_read) {
			/* TODO: Buufer pointer needs to be set here */
			fprintf(stderr, "We wrote %d, DSP accepted %d\n", num_read, wrote);
		}
	}
	printf("Playing file %s On Card %u device %u, with buffer of %lu bytes\n",
			name, card, device, buffer_size);
	printf("Format %u Channels %u, %u Hz, Bit Rate %d\n",
			SND_AUDIOCODEC_MP3, channels, rate, bits);

	compress_start(compress);
	if (verbose)
		printf("%s: You should hear audio NOW!!!\n", __func__);

	do {
		num_read = fread(buffer, 1, size, file);
		if (num_read > 0) {
			wrote = compress_write(compress, buffer, num_read);
			if (wrote < 0) {
				fprintf(stderr, "Error playing sample\n");
				fprintf(stderr, "ERR: %s\n", compress_get_error(compress));
				goto BUF_EXIT;
			}
			if (wrote != num_read) {
				/* TODO: Buffer pointer needs to be set here */
				fprintf(stderr, "We wrote %d, DSP accepted %d\n", num_read, wrote);
			}
			if (verbose) {
				print_time(compress);
				printf("%s: wrote %d\n", __func__, wrote);
			}
		}
	} while (num_read > 0);

	if (verbose)
		printf("%s: exit success\n", __func__);
	/* issue drain if it supports */
	compress_drain(compress);
	free(buffer);
	fclose(file);
	compress_close(compress);
	return;
BUF_EXIT:
	free(buffer);
COMP_EXIT:
	compress_close(compress);
FILE_EXIT:
	fclose(file);
	if (verbose)
		printf("%s: exit failure\n", __func__);
	exit(EXIT_FAILURE);
}

