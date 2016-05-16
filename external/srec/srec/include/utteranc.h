/*---------------------------------------------------------------------------*
 *  utteranc.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/



#ifndef _h_utteranc_
#define _h_utteranc_

#ifdef SET_RCSID
static const char utteranc_h[] = "$Id: utteranc.h,v 1.3.6.7 2007/08/31 17:44:53 dahan Exp $";
#endif



#include "all_defs.h"
#include "hmm_type.h"
#include "fpi_tgt.h"
#include "voicing.h"
#include "specnorm.h"
#include "channorm.h"
#include "swicms.h"
#ifndef _RTT
#include "duk_io.h"
#endif

#define DEFAULT_BUFFER_SIZE 100 /* in frames */
#define KEEP_FRAMES   40 /* in frames, past frames kept */

/*  Functions supported are
**  new, delete (by source)
**  open file/device, close file/device
**  attach and detach sink
**  read/store samples - including the header
*/

/**
 * @todo document
 */
typedef struct
{                /* label structure */
  char *label;
  long begin;
  long end;
  char *extra;
  unsigned char flag;
}
annotate;


/**
 * @todo document
 */
typedef struct
{
  int   utt_type;
  int   dim;
  fepFramePkt  *frame;
  int   num_chan;
  int   do_channorm;
  spect_dist_info **spchchan; /*  Mirrored from the Wave object */
  norm_info   *channorm; /*  Mirrored from the Wave object */
  swicms_norm_info     *swicms;    /* copy of wave obj pointer */
  spect_dist_info *backchan[MAX_CHAN_DIM];
  featdata  *last_push;
  int   voice_duration;
  int   quiet_duration;
  int   unsure_duration;
  int   start_windback;
}
utt_generic_info;

#ifndef _RTT
/**
 * @todo document
 */
typedef struct
{
  char  typ;  /* s (16 bit), c (8 bit), u (newton .utb) */
  int   endian;  /* 0 is little 1 is big */
  int   do_skip; /* skip every other frame */
  unsigned long len;  /* length of file/utterance */
  PFile* file;  /* pointer to file */
  char  name[MAX_LABEL]; /* file name */
  /*    int   op;  read or write */
  int   num_utts; /* no. of utterances in utb file */
  annotate  *utb_table; /* utb file header information */
}
utt_file_info;

/**
 * @todo document
 */
typedef struct
{
  int   utt_type;
  int   dim;
  fepFramePkt  *frame;
  int   num_chan;
  int   do_channorm;
  spect_dist_info **spchchan; /*  Mirrored from the Wave object */
  norm_info   *channorm; /*  Mirrored from the Wave object */
  swicms_norm_info    *swicms;          /* copy of wave obj pointer */
  spect_dist_info *backchan[MAX_CHAN_DIM];
  featdata  *last_push;
  int   voice_duration;
  int   quiet_duration;
  int   unsure_duration;
  int   start_windback;
  /*    voicing_info voice; */
  utt_file_info file;
}
file_utterance_info;
#endif

/**
 * @todo document
 */
typedef struct
{
  int   utt_type;
  int   dim;
  fepFramePkt  *frame;
  int   num_chan;
  int   do_channorm;
  spect_dist_info **spchchan; /*  Mirrored from the Wave object */
  norm_info   *channorm; /*  Mirrored from the Wave object */
  swicms_norm_info    *swicms;        /* copy of wave obj pointer */
  spect_dist_info *backchan[MAX_CHAN_DIM];
  featdata  *last_push;
  int   voice_duration;
  int   quiet_duration;
  int   unsure_duration;
  int   start_windback;
}
live_utterance_info;

/**
 * @todo document
 */
typedef union
{
  int   utt_type; /* live or from file */
  utt_generic_info    gen_utt; /* generic one */
#ifndef _RTT
  file_utterance_info file_utt;
#endif
  live_utterance_info live_utt;
} utterance_info;


/*
**  Size of the utb file headers and details
*/

#ifndef _RTT
#define UTT_VERSION 2
#define UTT_HEADER_SIZE 16        /*Size on disk*/
#define UTB_HEADER_SIZE 32        /*Size on disk*/
#define UTB_HEADER_USED 16        /*Size on disk*/   /* SAL */

/**
 * UTB file header.
 */
typedef struct _UttHeader
{
	/**
	 * The size of the header in bytes.
	 */
  unsigned short headerSize;
	/**
	 * The version of the file format.
	 */
  unsigned short version;
	/**
	 * The size of the payload in bytes.
	 */
  unsigned long  nBytes;
	/**
	 * The number of parameters per frame.
	 */
  unsigned short nParametersPerFrame;
	/**
	 * 0=unknown, 1=none, 2=amp-based, 3=harmonicity-based, 4=mrec style
	 */
  unsigned short channelNormalization;    
  /**
	 * 0=unknown, 1=no, 2=yes
	 */
  unsigned short speakerNormalization;
  /**
	 * 0=unknown, 1=no, 2=yes
	 */
  unsigned short imeldaization;
	/**
	 * Before imelda truncation.
	 */
  unsigned short nOriginalParameters;
	/**
	 * The number of samples per frame.
	 */
  unsigned short samplesPerFrame;
	/**
	 * The audio sample rate.
	 */
  unsigned long  sampleRate;
	/**
	 * not used in version 5.
	 */
  unsigned long  checksum;
}
UttHeader;

int    update_utb_header(file_utterance_info *utt, int frames, int samplerate,
                         int framerate);
void    init_utt_v5_header(UttHeader *uhead, int dim, int samplerate, int framerate);
int init_data_file(char *filename, file_utterance_info *utt, int dimen,
                   char typ, int endian, int do_skip);
int new_data_file(char *filename, file_utterance_info *utt, int dimen,
                  char typ, int endian);
int set_data_frame(file_utterance_info *utt, long begin);
int buffer_data_frames(file_utterance_info *utt, long f_begin, long f_end);
void more_data_frames(file_utterance_info *utt);
int save_data_frames(file_utterance_info *utt);
void close_data_stream(file_utterance_info *utt);
int init_utb_file(file_utterance_info *utt, annotate **table);
int position_utb_file(file_utterance_info *utt, long position, annotate *table);
int load_utb_data(file_utterance_info *utt, int num_frames, int do_skip);
int load_short_data(file_utterance_info *utt, int num_frames, int do_skip);
int save_utb_data(file_utterance_info *utt, int num_frames);
int save_short_data(file_utterance_info *utt, int num_frames);
int read_utt_head(UttHeader *head, PFile* datafile);
int write_utt_head(UttHeader *head, PFile* datafile);
int check_for_utb(char* filename);

/*  TCP reading routines
*/
int     read_tcp(char *filename, annotate **tag_base);
int     read_lst(char *filename, annotate *tag_base, int ntags);
int     read_utb_table(char *filename, annotate **tag_base);
void    save_tcp(char *tcpnam, annotate *tag, int ntags);
void compose_tcp_name_of_utt(char* uttname , char* tcpname);

#endif

void init_utterance(utterance_info *utt, int utt_type, int dimen,
                    int buffer_size, int keep_frames, int num_chan, int do_voicing);
void set_voicing_durations(utterance_info *utt, int voice_duration,
                           int quiet_duration, int unsure_duration,
                           int start_windback);
void free_utterance(utterance_info *utt);
int utterance_started(utterance_info *utt);
int utterance_ended(utterance_info *utt);
int load_utterance_frame(utterance_info *utt, unsigned char* pUttFrame, int voicing);
int copy_utterance_frame(utterance_info *oututt, utterance_info *inutt);

#endif /* _h_utteranc_ */
