/*---------------------------------------------------------------------------*
 *  voc_read.c  *
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


#ifndef _RTT
#include <stdio.h>
#endif
#include <stdlib.h>
#include <math.h>
#include <assert.h>

#if defined(__cplusplus) && defined(_MSC_VER)
extern "C"
{
#include <string.h>
}
#else
#include <string.h>
#endif

#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32
#define stat _stat
#else
#include <unistd.h>
#endif


#include <fcntl.h>
#include <sys/mman.h>

#include <zipfile/zipfile.h>


#include "hmmlib.h"
#include "duk_io.h"
#include "LCHAR.h"
#include "portable.h"

#include "memmove.h"

static const char voc_read[] = "$Id: voc_read.c,v 1.14.6.18 2008/03/05 21:18:44 dahan Exp $";


#define cr_or_nl(ch) ((ch) == '\n' || (ch) == '\r')


#ifndef _RTT

/**
 *  Read word models and their phoneme transcriptions from .ok or .voc files.
 *  returns -1 on error
 */
int read_word_transcription(const LCHAR* basename, vocab_info* voc, ESR_Locale* locale)
{
  const char *ok;
  ESR_ReturnCode rc;
  int result;
  int i;
  char token[256];

  ASSERT(voc);

  if (basename == NULL || strlen(basename) == 0) {
    PLogError("Error: invalid arg to read_word_transcription()\n");
    goto CLEANUP;
  }

  if (mmap_zip(basename, (void**)&voc->ok_file_data, (size_t*)&voc->ok_file_data_length)) {
    PLogError("read_word_transcription: mmap_zip failed for %s\n", basename);
    goto CLEANUP;
  }

  /* this assumption eliminates simplifies bounds checking when parsing */
  if (!cr_or_nl(voc->ok_file_data[voc->ok_file_data_length - 1])) {
    PLogError(L("read_word_transcription: last character in %s not newline\n"), basename);
    goto CLEANUP;
  }

  /* set up point to walk the data */
  ok = voc->ok_file_data;

  /* verify the header */
  i = 0;
  while (*ok != '=') {
    if (cr_or_nl(*ok)) {
      PLogError(L("%s was missing '=' in #LANG=en-us header"), basename);
      goto CLEANUP;
    }
    token[i++] = *ok++;
  }
  token[i] = 0;
  ok++;
  CHKLOG(rc, lstrcasecmp(token, L("#lang"), &result));
  if (result != 0)
  {
    PLogError(L("%s was missing #LANG=en-us header"), basename);
    goto CLEANUP;
  }
  i = 0;
  while (!cr_or_nl(*ok)) token[i++] = *ok++;
  token[i] = 0;
  ok++;
  CHKLOG(rc, ESR_str2locale(token, locale));

  /* set up first and last entries */
  voc->first_entry = strchr(voc->ok_file_data, '\n') + 1;
  voc->last_entry = voc->ok_file_data + voc->ok_file_data_length - 2;
  while (*voc->last_entry != '\n') voc->last_entry--; /* header forces termination */
  voc->last_entry++;

  /* determine if there are any upper case entries */
  voc->hasUpper = 1;
  while (ok < voc->ok_file_data + voc->ok_file_data_length) {
    int ch = *ok;
    if ('A' <= ch && ch <= 'Z') {
      voc->hasUpper = 1;
      break;
    }
    else if ('Z' < ch) {
      voc->hasUpper = 0;
      break;
    }
    /* scan to the next entry */
    while (*ok++ != '\n') ;
  }

  return 0;

CLEANUP:
  delete_word_transcription(voc);

  PLogError(L("read_word_transcription: failed to read '%s'"), basename);

  return -1;
}
#endif

/* the label is terminated with 0 and the entry terminated with ' ' */
static int kompare(const char* label, const char* entry) {
  while (*label == *entry) {
    label++;
    entry++;
  }
  return (*label ? *label : ' ') - *entry;
}

int get_prons(const vocab_info* voc, const char* label, char* prons, int prons_len) {
  int num_prons;
  const char* low;
  const char* middle;
  const char* high;

  //PLogError(L("get_prons '%s'"), label);

  /* dictionaries are usually lower case, so do this for speed */
  if (!voc->hasUpper && 'A' <= *label && *label <= 'Z') return 0;

  /* binary search to find matching entry */
  low = voc->first_entry;
  high = voc->last_entry;
  while (1) {
    /* pick a point in the middle and align to next entry */
    middle = low + ((high - low) >> 1) - 1;
    while (*middle++ != '\n') ;

    /* compare 'label' to 'middle' */
    int diff = kompare(label, middle);
    if (diff == 0) break;

    /* nothing found */
    if (low == high) return 0;

    /* 'middle' aligned to 'high', so move 'high' down */
    if (middle == high) {
      high -= 2;
      while (*high != '\n') high--;
      high++;
      continue;
    }

    if (diff > 0) low = middle;
    else high = middle;
  }

  /* back up to find the first entry equal to 'label' */
  low = middle;
  while (voc->first_entry < low) {
    const char* lo;
    for (lo = low - 2; *lo != '\n'; lo--) ;
    lo++;
    if (kompare(label, lo)) break;
    low = lo;
  }

  /* move forward to the last entry equal to 'label' */
  high = middle;
  while (high < voc->last_entry) {
    const char* hi;
    for (hi = high; *hi != '\n'; hi++) ;
    hi++;
    if (kompare(label, hi)) break;
    high = hi;
  }

  /* loop over all the entries */
  num_prons = 0;
  while (low <= high) {
    /* scan over the label */
    while (*low++ != ' ') ;

    /* skip the whitespace */
    while (*low == ' ') low++;

    /* copy the pron */
    while (*low != '\n') {
      if (--prons_len <= 2) return -1;
      *prons++ = *low++;
    }
    *prons++ = 0;
    low++;
    num_prons++;
  }
  *prons++ = 0;

  return num_prons;
}

void delete_word_transcription(vocab_info* voc)
{
  ASSERT(voc);

  voc->first_entry = 0;
  voc->last_entry = 0;
  if (voc->ok_file_data) munmap_zip(voc->ok_file_data, voc->ok_file_data_length);
  voc->ok_file_data = NULL;
  voc->ok_file_data_length = 0;
}


/**************************************************/
/* may want to move these functions to 'portable' */
/**************************************************/

static int endeql(const char* string, const char* end) {
  return strlen(end) <= strlen(string) && !strcmp(string + strlen(string) - strlen(end), end);
}

/* decompress_entry requires an oversize destination buffer, so... */
static size_t inflateSize(size_t size) {
  return size + size / 1000 + 1;
}

int mmap_zip(const char* fname, void** buf, size_t* size) {
    int fd = -1;
    struct stat statbuf;
    zipfile_t zf = 0;
    zipentry_t ze = 0;
    char entryname[FILENAME_MAX];
    size_t size2 = 0;
    void* buf2 = 0;

    /* open data file, determine size, map it, and close fd */
    fd = open(fname, O_RDONLY);
    if (fd < 0) goto FAILED;

    /* determine length */
    if (fstat(fd, &statbuf) < 0) goto FAILED;

    /* mmap it */
    *size = statbuf.st_size;
    *buf = mmap(0, inflateSize(statbuf.st_size), PROT_READ|PROT_WRITE, MAP_PRIVATE, fd, 0);
    if (*buf == MAP_FAILED) goto FAILED;

    /* close fd, since we can */
    close(fd);
    fd = -1;

    /* if not a zip file, we are done! */
    if (!endeql(fname, ".zip")) return 0;

    /* set up zipfiler */
    zf = init_zipfile(*buf, *size);
    if (!zf) goto FAILED;

    /* get entry */
    strcpy(entryname, strrchr(fname, '/') ? strrchr(fname, '/') + 1 : fname);
    entryname[strlen(entryname) - strlen(".zip")] = 0;
    ze = lookup_zipentry(zf, entryname);
    if (!ze) goto FAILED;

    /* mmap anon memory to hold unzipped entry */
    size2 = get_zipentry_size(ze);
    buf2 = mmap(0, inflateSize(size2), PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANON, -1, 0);
    if (buf2 == (void*)-1) goto FAILED;

    /* unzip entry */
    if (decompress_zipentry(ze, buf2, size2)) goto FAILED;

    /* release unzipper */
    release_zipfile(zf);
    zf = 0;

    /* release mmapped file */
    munmap(*buf, inflateSize(*size));

    /* set return values */
    *buf = buf2;
    *size = size2;

    return 0;

FAILED:
    if (fd != -1) close(fd);
    if (zf) release_zipfile(zf);
    if (buf2) munmap(buf2, inflateSize(size2));
    if (*buf && *buf != (void*)-1) munmap(*buf, inflateSize(*size));
    *buf = 0;
    *size = 0;
    return -1;
}

int munmap_zip(void* buf, size_t size) {
    return munmap(buf, inflateSize(size));
}

