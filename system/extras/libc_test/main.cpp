/*
** Copyright 2013 The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>

// Put any local test functions into the extern below.
extern "C" {
}

#define FENCEPOST_LENGTH          8

#define MAX_MEMCPY_TEST_SIZE      2048
#define MAX_MEMCPY_BUFFER_SIZE    (3 * MAX_MEMCPY_TEST_SIZE)

#define MAX_MEMSET_TEST_SIZE      2048
#define MAX_MEMSET_BUFFER_SIZE    (3 * MAX_MEMSET_TEST_SIZE)

#define MAX_STRING_TEST_SIZE      1024
#define MAX_STRING_BUFFER_SIZE    (3 * MAX_STRING_TEST_SIZE)

#define MAX_STRCAT_DST_SIZE       32

const int kStringAligns[][4] = {
  // All zeroes to use the values returned from malloc.
  { 0, 0, 0, 0 },

  { 1, 0, 1, 0 },
  { 2, 0, 2, 0 },
  { 4, 0, 4, 0 },
  { 8, 0, 8, 0 },

  { 8, 0, 4, 0 },
  { 4, 0, 8, 0 },

  { 8, 0, 8, 1 },
  { 8, 0, 8, 2 },
  { 8, 0, 8, 3 },
  { 8, 1, 8, 0 },
  { 8, 2, 8, 0 },
  { 8, 3, 8, 0 },

  { 4, 0, 4, 1 },
  { 4, 0, 4, 2 },
  { 4, 0, 4, 3 },
  { 4, 1, 4, 0 },
  { 4, 2, 4, 0 },
  { 4, 3, 4, 0 },
};

#define STRING_ALIGN_LEN  (sizeof(kStringAligns)/sizeof(int[4]))

// Return a pointer into the current string with the specified alignment.
void *getAlignedPtr(void *orig_ptr, int alignment, int or_mask) {
  uint64_t ptr = reinterpret_cast<uint64_t>(orig_ptr);
  if (alignment > 0) {
      // When setting the alignment, set it to exactly the alignment chosen.
      // The pointer returned will be guaranteed not to be aligned to anything
      // more than that.
      ptr += alignment - (ptr & (alignment - 1));
      ptr |= alignment | or_mask;
  }

  return reinterpret_cast<void*>(ptr);
}

char *setString(char *str, size_t size) {
  for (size_t i = 0; i < size; i++) {
    str[i] = (char)(32 + (i % 96));
  }
  str[size] = '\0';

  return str;
}

char *allocateString() {
  char *str = reinterpret_cast<char*>(malloc(MAX_STRING_BUFFER_SIZE+1));
  if (!str) {
    return NULL;
  }

  return setString(str, MAX_STRING_BUFFER_SIZE);
}

void setFencepost(uint8_t *buffer) {
  for (int i = 0; i < FENCEPOST_LENGTH; i += 2) {
    buffer[i] = 0xde;
    buffer[i+1] = 0xad;
  }
}

bool verifyFencepost(uint8_t *buffer) {
  for (int i = 0; i < FENCEPOST_LENGTH; i += 2) {
    if (buffer[i] != 0xde || buffer[i+1] != 0xad) {
      uint8_t expected_value;
      if (buffer[i] == 0xde) {
        i++;
        expected_value = 0xad;
      } else {
        expected_value = 0xde;
      }
      printf("    mismatch at fencepost[%d], expected %d found %d\n",
             i, expected_value, buffer[i]);
      return false;
    }
  }
  return true;
}

bool doStrcmpExpectEqual(char *string1, char *string2, const int align[4],
                         int (*test_strcmp)(const char *s1, const char *s2),
                         bool verbose) {
  char *align_str1 = (char*)getAlignedPtr(string1, align[0], align[1]);
  char *align_str2 = (char*)getAlignedPtr(string2, align[2], align[3]);

  for (size_t i = 0; i < MAX_STRING_TEST_SIZE; i++) {
    for (size_t j = 0; j < i; j++) {
      align_str1[j] = (char)(32 + (j % 96));
      align_str2[j] = align_str1[j];
    }
    align_str1[i] = '\0';
    align_str2[i] = '\0';

    // Set the characters after the string terminates to different values
    // to verify that the strcmp is not over checking.
    for (size_t j = i+1; j < i+64; j++) {
      align_str1[j] = (char)(32 + j);
      align_str2[j] = (char)(40 + j);
    }

    if (verbose) {
      printf("Testing size %d, align_str1=%p[%d,%d], align_str2=%p[%d,%d]\n",
             i, align_str1, align[0], align[1], align_str2, align[2], align[3]);
    }

    if (test_strcmp(align_str1, align_str2) != 0) {
      printf("    Failed at size %d, src1 %p, src2 %p\n",
             i, align_str1, align_str2);
      return false;
    }
  }

  return true;
}

bool doStrcmpExpectDiff(char *string1, char *string2, const int diff_align[2],
                        const int align[4], char diff_char,
                        int (*test_strcmp)(const char *s1, const char *s2),
                        bool verbose) {
  char *align_str1 = (char*)getAlignedPtr(string1, align[0], align[1]);
  char *align_str2 = (char*)getAlignedPtr(string2, align[2], align[3]);

  for (int i = 0; i < MAX_STRING_TEST_SIZE; i++) {
    // Use valid ascii characters, no unprintables characters.
    align_str1[i] = (char)(32 + (i % 96));
    if (align_str1[i] == diff_char) {
      // Assumes that one less than the diff character is still a valid
      // character.
      align_str1[i] = diff_char-1;
    }
    align_str2[i] = align_str1[i];
  }
  align_str1[MAX_STRING_TEST_SIZE] = '\0';
  align_str2[MAX_STRING_TEST_SIZE] = '\0';

  // Quick check to make sure that the strcmp knows that everything is
  // equal. If it's so broken that it already thinks the strings are
  // different, then there is no point running any of the other tests.
  if (test_strcmp(align_str1, align_str2) != 0) {
    printf("    strcmp is too broken to do difference testing.\n");
    return false;
  }

  // Get a pointer into the string at the specified alignment.
  char *bad = (char*)getAlignedPtr(align_str1+MAX_STRING_TEST_SIZE/2,
                                   diff_align[0], diff_align[1]);

  char saved_char = bad[0];
  bad[0] = diff_char;

  if (verbose) {
    printf("Testing difference, align_str1=%p[%d,%d], align_str2=%p[%d,%d]\n",
           align_str1, align[0], align[1], align_str2, align[2], align[3]);
  }
  if (test_strcmp(align_str1, align_str2) == 0) {
    printf("   Did not miscompare at size %d, src1 %p, src2 %p, diff %p\n",
           MAX_STRING_TEST_SIZE, align_str1, align_str2, bad);
    return false;
  }
  bad[0] = saved_char;

  // Re-verify that something hasn't gone horribly wrong.
  if (test_strcmp(align_str1, align_str2) != 0) {
    printf("   strcmp is too broken to do difference testing.\n");
    return false;
  }

  bad = (char*)getAlignedPtr(align_str2+MAX_STRING_TEST_SIZE/2, diff_align[0],
                             diff_align[1]);
  bad[0] = diff_char;

  if (verbose) {
    printf("Testing reverse difference, align_str1=%p[%d,%d], align_str2=%p[%d,%d]\n",
           align_str1, align[0], align[1], align_str2, align[2], align[3]);
  }
  if (test_strcmp(align_str1, align_str2) == 0) {
    printf("    Did not miscompare at size %d, src1 %p, src2 %p, diff %p\n",
           MAX_STRING_TEST_SIZE, align_str1, align_str2, bad);
    return false;
  }

  return true;
}

bool doStrcmpCheckRead(int (*test_strcmp)(const char *s1, const char *s2),
                       bool verbose) {
  // In order to verify that the strcmp is not reading past the end of the
  // string, create some strings that end near unreadable memory.
  long pagesize = sysconf(_SC_PAGE_SIZE);
  char *memory = (char*)memalign(pagesize, 2 * pagesize);
  if (memory == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  // Make the second page unreadable and unwritable.
  if (mprotect(&memory[pagesize], pagesize, PROT_NONE) != 0) {
    perror("Unable to set protection of page.\n");
    return false;
  }

  size_t max_size = pagesize < MAX_STRING_TEST_SIZE ? pagesize-1 : MAX_STRING_TEST_SIZE;
  // Allocate an extra byte beyond the string terminator to allow us to
  // extend the string to be larger than our protected string.
  char *other_string = (char *)malloc(max_size+2);
  if (other_string == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }
  char *string;
  for (size_t i = 0; i <= max_size; i++) {
    string = &memory[pagesize-i-1];
    for (size_t j = 0; j < i; j++) {
      other_string[j] = (char)(32 + (j % 96));
      string[j] = other_string[j];
    }
    other_string[i] = '\0';
    string[i] = '\0';

    if (verbose) {
      printf("Testing size %d, strings equal.\n", i);
    }
    if (test_strcmp(other_string, string) != 0) {
      printf("    Failed at size %d, src1 %p, src2 %p\n", i, other_string, string);
      return false;
    }

    if (verbose) {
      printf("Testing size %d, strings equal reverse strings.\n", i);
    }
    if (test_strcmp(string, other_string) != 0) {
      printf("    Failed at size %d, src1 %p, src2 %p\n", i, string, other_string);
      return false;
    }

    // Now make other_string longer than our protected string.
    other_string[i] = '1';
    other_string[i+1] = '\0';

    if (verbose) {
      printf("Testing size %d, strings not equal.\n", i);
    }
    if (test_strcmp(other_string, string) == 0) {
      printf("    Failed at size %d, src1 %p, src2 %p\n", i, other_string, string);
      return false;
    }

    if (verbose) {
      printf("Testing size %d, strings not equal reverse the strings.\n", i);
    }
    if (test_strcmp(string, other_string) == 0) {
      printf("    Failed at size %d, src1 %p, src2 %p\n", i, string, other_string);
      return false;
    }
  }
  return true;
}

bool runStrcmpTest(int (*test_strcmp)(const char *s1, const char *s2),
                   bool verbose) {
  char *string1 = allocateString();
  char *string2 = allocateString();
  if (string1 == NULL || string2 == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  printf("  Verifying equal sized strings at different alignments.\n");
  for (size_t i = 0; i < STRING_ALIGN_LEN; i++) {
    if (!doStrcmpExpectEqual(string1, string2, kStringAligns[i], test_strcmp,
                             verbose)) {
      return false;
    }
  }

  // Test the function finds strings with differences at specific locations.
  const int diff_aligns[][2] = {
    { 4, 0 },
    { 4, 1 },
    { 4, 2 },
    { 4, 3 },
    { 8, 0 },
    { 8, 1 },
    { 8, 2 },
    { 8, 3 },
  };
  printf("  Verifying different strings at different alignments.\n");
  for (size_t i = 0; i < sizeof(diff_aligns)/sizeof(int[2]); i++) {
    // First loop put the string terminator at the chosen alignment.
    for (size_t j = 0; j < STRING_ALIGN_LEN; j++) {
      if (!doStrcmpExpectDiff(string1, string2, diff_aligns[i],
                              kStringAligns[j], '\0', test_strcmp, verbose)) {
        return false;
      }
    }
    // Second loop put a different character at the chosen alignment.
    // This character is guaranteed not to be in the original string.
    for (size_t j = 0; j < STRING_ALIGN_LEN; j++) {
      if (!doStrcmpExpectDiff(string1, string2, diff_aligns[i],
                              kStringAligns[j], '\0', test_strcmp, verbose)) {
        return false;
      }
    }
  }

  printf("  Verifying strcmp does not read too many bytes.\n");
  if (!doStrcmpCheckRead(test_strcmp, verbose)) {
    return false;
  }

  printf("  All tests pass.\n");

  return true;
}

bool doStrlenCheck(size_t size, char *string, int align, int or_mask,
                   size_t (*test_strlen)(const char *), bool verbose) {
  char *aligned_string = reinterpret_cast<char*>(getAlignedPtr(string, align, or_mask));
  size_t len;
  if (verbose) {
    printf("Testing size %d, align=%p[%d,%d]\n", size, aligned_string, align, or_mask);
  }

  aligned_string[size] = '\0';
  len = test_strlen(aligned_string);
  if (len != size) {
    printf("Failed at size %d, length returned %u, align=%p[%d,%d]\n",
           size, len, aligned_string, align, or_mask);
    return false;
  }

  if (verbose) {
    printf("Testing size %d with extra zeros after string, align=%p[%d,%d]\n",
           size, aligned_string, align, or_mask);
  }

  for (size_t j = size+1; j <= size+16; j++) {
    aligned_string[j] = '\0';
  }

  len = test_strlen(aligned_string);
  if (len != size) {
    printf("Failed at size %d, length returned %u with zeroes after string, align=%p[%d,%d]\n",
           size, len, aligned_string, align, or_mask);
    return false;
  }

  for (size_t j = size; j <= size+16; j++) {
    aligned_string[j] = (char)(32 + (j % 96));
  }
  return true;
}

bool runStrlenTest(size_t (*test_strlen)(const char *),
                   bool verbose) {
  char *string = allocateString();
  if (string == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  // Check different string alignments. All zeroes indicates that the
  // unmodified malloc values should be used.
  const int aligns[][2] = {
    // All zeroes to use the values returned from malloc.
    { 0, 0 },

    { 1, 0 },
    { 2, 0 },
    { 4, 0 },
    { 8, 0 },
    { 16, 0 },
    { 32, 0 },

    { 8, 1 },
    { 8, 2 },
    { 8, 3 },

    { 4, 1 },
    { 4, 2 },
    { 4, 3 },
  };

  printf("  Verifying string lengths at different alignments.\n");
  for (size_t i = 0; i < sizeof(aligns)/sizeof(int[2]); i++) {
    for (size_t j = 0; j <= MAX_STRING_TEST_SIZE; j++) {
      if (!doStrlenCheck(j, string, aligns[i][0], aligns[i][1], test_strlen, verbose)) {
        return false;
      }
    }
  }

  printf("  Verifying strlen does not read past end of string.\n");

  // In order to verify that strlen is not reading past the end of the
  // string, create strings that end near unreadable memory.
  long pagesize = sysconf(_SC_PAGE_SIZE);
  char *memory = (char*)memalign(pagesize, 2 * pagesize);
  if (memory == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  // Make the second page unreadable and unwritable.
  if (mprotect(&memory[pagesize], pagesize, PROT_NONE) != 0) {
    perror("Unable to set protection of page.\n");
    return false;
  }

  size_t max_size = pagesize < MAX_STRING_TEST_SIZE ? pagesize-1 : MAX_STRING_TEST_SIZE;
  for (long i = 0; i < pagesize; i++) {
    memory[i] = (char)(32 + (i % 96));
  }

  size_t len;
  for (size_t i = 0; i < sizeof(aligns)/sizeof(int[2]); i++) {
    for (size_t j = 0; j <= max_size; j++) {
      string = &memory[pagesize-j-1];
      string[j] = '\0';

      if (verbose) {
        printf("Testing size %d overread, align=%p[%d,%d]\n",
               j, string, aligns[i][0], aligns[i][1]);
      }
      len = test_strlen(string);
      if (len != j) {
        printf("    Failed at size %u, returned %u, align=%p[%d,%d]\n",
               j, len, string, aligns[i][0], aligns[i][1]);
        return false;
      }
      string[j] = (char)(32 + (j % 96));
    }
  }

  printf("  All tests pass.\n");

  return true;
}

bool runStrcpyTest(char *(*test_strcpy)(char *, const char *),
                   bool verbose) {
  char *src = allocateString();
  if (src == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }
  char *dst = allocateString();
  if (dst == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  printf("  Verifying string lengths at different alignments.\n");
  char *src_align;
  char *dst_align;
  char *dst_ret;
  for (size_t i = 0; i < STRING_ALIGN_LEN; i++) {
    for (size_t copy_len = 0; copy_len <= MAX_STRING_TEST_SIZE; copy_len++) {
      if (kStringAligns[i][0]) {
        src_align = reinterpret_cast<char*>(getAlignedPtr(src+FENCEPOST_LENGTH, kStringAligns[i][0], kStringAligns[i][1]));
        dst_align = reinterpret_cast<char*>(getAlignedPtr(dst+FENCEPOST_LENGTH, kStringAligns[i][2], kStringAligns[i][3]));
      } else {
        src_align = src;
        dst_align = dst;
      }
      setString(src_align, copy_len);
      memset(dst_align, 0, copy_len+1);

      if (dst_align != dst) {
        setFencepost(reinterpret_cast<uint8_t*>(&dst_align[-FENCEPOST_LENGTH]));
      }
      setFencepost(reinterpret_cast<uint8_t*>(&dst_align[copy_len+1]));

      if (verbose) {
        printf("Testing copy_len %u, src_align=%p[%d,%d], dst_align=%p[%d,%d]\n",
               copy_len, src_align, kStringAligns[i][0], kStringAligns[i][1],
               dst_align, kStringAligns[i][2], kStringAligns[i][3]);
      }

      dst_ret = test_strcpy(dst_align, src_align);
      if (dst_ret != dst_align) {
        printf("copy_len %u returned incorrect value: expected %p, got %p\n",
               copy_len, dst_align, dst_ret);
        return false;
      }
      if (memcmp(src_align, dst_align, copy_len) != 0) {
        printf("copy_len %u failed to copy properly: src and dst aren't equal\n", copy_len);
        return false;
      }

      if (dst_align != dst && !verifyFencepost(reinterpret_cast<uint8_t*>(&dst_align[-FENCEPOST_LENGTH]))) {
        printf("copy_len %u fencepost before dst was overwritten\n", copy_len);
        return false;
      }

      if (!verifyFencepost(reinterpret_cast<uint8_t*>(&dst_align[copy_len+1]))) {
        printf("copy_len %u fencepost at end of dst was overwritten\n", copy_len);
        return false;
      }
    }
  }

  printf("  All tests pass.\n");

  return true;
}

bool runStrcatTest(char *(*test_strcat)(char *, const char *),
                   bool verbose) {
  char *src = allocateString();
  if (src == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }
  char *dst = allocateString();
  if (dst == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  printf("  Verifying string lengths at different alignments.\n");
  char *src_align;
  char *dst_align;
  char *dst_ret;
  for (size_t i = 0; i < STRING_ALIGN_LEN; i++) {
    for (size_t dst_len = 0; dst_len <= MAX_STRCAT_DST_SIZE; dst_len++) {
      for (size_t copy_len = 0; copy_len <= MAX_STRING_TEST_SIZE; copy_len++) {
        if (kStringAligns[i][0]) {
          src_align = reinterpret_cast<char*>(getAlignedPtr(src+FENCEPOST_LENGTH, kStringAligns[i][0], kStringAligns[i][1]));
          dst_align = reinterpret_cast<char*>(getAlignedPtr(dst+FENCEPOST_LENGTH, kStringAligns[i][2], kStringAligns[i][3]));
        } else {
          src_align = src;
          dst_align = dst;
        }
        setString(src_align, copy_len);
        memset(dst_align, 'd', dst_len);
        memset(dst_align+dst_len, 0, copy_len+1);

        if (dst_align != dst) {
          setFencepost(reinterpret_cast<uint8_t*>(&dst_align[-FENCEPOST_LENGTH]));
        }
        setFencepost(reinterpret_cast<uint8_t*>(&dst_align[copy_len+dst_len+1]));

        if (verbose) {
          printf("Testing copy_len %u, dst_len %u, src_align=%p[%d,%d], dst_align=%p[%d,%d]\n",
                 copy_len, dst_len, src_align, kStringAligns[i][0], kStringAligns[i][1],
                 dst_align, kStringAligns[i][2], kStringAligns[i][3]);
        }

        dst_ret = test_strcat(dst_align, src_align);
        if (dst_ret != dst_align) {
          printf("dst_len %u, copy_len %u returned incorrect value: expected %p, got %p\n",
                dst_len, copy_len, dst_align, dst_ret);
          return false;
        }
        for (size_t j = 0; j < dst_len; j++) {
          if (dst_align[j] != 'd') {
            printf("dst_len %u, copy_len %u: strcat overwrote dst string\n",
                   dst_len, copy_len);
            return false;
          }
        }
        if (memcmp(src_align, dst_align+dst_len, copy_len+1) != 0) {
          printf("dst_len %u, copy_len %u failed to copy properly: src and dst aren't equal\n",
                 dst_len, copy_len);
          return false;
        }

        if (dst_align != dst && !verifyFencepost(reinterpret_cast<uint8_t*>(&dst_align[-FENCEPOST_LENGTH]))) {
          return false;
        }

        if (!verifyFencepost(reinterpret_cast<uint8_t*>(&dst_align[dst_len+copy_len+1]))) {
          return false;
        }
      }
    }
  }

  printf("  All tests pass.\n");

  return true;
}

bool runMemcpyTest(void* (*test_memcpy)(void *dst, const void *src, size_t n),
                   bool verbose) {
  uint8_t *dst = reinterpret_cast<uint8_t*>(malloc(MAX_MEMCPY_BUFFER_SIZE));
  uint8_t *src = reinterpret_cast<uint8_t*>(malloc(MAX_MEMCPY_BUFFER_SIZE));
  if (dst == NULL || src == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  // Set the source to a known pattern once. The assumption is that the
  // memcpy is not so broken that it will write in to the source buffer.
  // However, do not write zeroes into the source so a very quick can be
  // made to verify the source has not been modified.
  for (int i = 0; i < MAX_MEMCPY_BUFFER_SIZE; i++) {
    src[i] = i % 256;
    if (src[i] == 0) {
      src[i] = 0xaa;
    }
  }

  const int aligns[][4] = {
    // Src and dst use pointers returned by malloc.
    { 0, 0, 0, 0 },

    // Src and dst at same alignment.
    { 1, 0, 1, 0 },
    { 2, 0, 2, 0 },
    { 4, 0, 4, 0 },
    { 8, 0, 8, 0 },
    { 16, 0, 16, 0 },
    { 32, 0, 32, 0 },
    { 64, 0, 64, 0 },
    { 128, 0, 128, 0 },

    // Different alignments between src and dst.
    { 8, 0, 4, 0 },
    { 4, 0, 8, 0 },
    { 16, 0, 4, 0 },
    { 4, 0, 16, 0 },

    // General unaligned cases.
    { 4, 0, 4, 1 },
    { 4, 0, 4, 2 },
    { 4, 0, 4, 3 },
    { 4, 1, 4, 0 },
    { 4, 2, 4, 0 },
    { 4, 3, 4, 0 },

    // All non-word aligned cases.
    { 4, 1, 4, 0 },
    { 4, 1, 4, 1 },
    { 4, 1, 4, 2 },
    { 4, 1, 4, 3 },

    { 4, 2, 4, 0 },
    { 4, 2, 4, 1 },
    { 4, 2, 4, 2 },
    { 4, 2, 4, 3 },

    { 4, 3, 4, 0 },
    { 4, 3, 4, 1 },
    { 4, 3, 4, 2 },
    { 4, 3, 4, 3 },

    { 2, 0, 4, 0 },
    { 4, 0, 2, 0 },
    { 2, 0, 2, 0 },

    // Invoke the unaligned case where the code needs to align dst to 0x10.
    { 128, 1, 128, 4 },
    { 128, 1, 128, 8 },
    { 128, 1, 128, 12 },
    { 128, 1, 128, 16 },
  };

  printf("  Verifying variable sized copies at different alignments.\n");
  uint8_t *src_align, *dst_align;
  for (size_t i = 0; i < sizeof(aligns)/sizeof(int[4]); i++) {
    for (size_t len = 0; len <= MAX_MEMCPY_TEST_SIZE; len++) {
      if (aligns[i][0]) {
        src_align = (uint8_t*)getAlignedPtr(src+FENCEPOST_LENGTH, aligns[i][0],
                                            aligns[i][1]);
        dst_align = (uint8_t*)getAlignedPtr(dst+FENCEPOST_LENGTH, aligns[i][2],
                                            aligns[i][3]);
      } else {
        src_align = src;
        dst_align = dst;
      }

      if (verbose) {
        printf("Testing size %d, src_align=%p[%d,%d], dst_align=%p[%d,%d]\n",
               len, src_align, aligns[i][0], aligns[i][1],
               dst_align, aligns[i][2], aligns[i][3]);
      }

      memset(dst_align, 0, len);

      // Don't add a pre fencepost if we are using the value from the malloc.
      if (dst_align != dst) {
        setFencepost(&dst_align[-8]);
      }
      setFencepost(&dst_align[len]);

      test_memcpy(dst_align, src_align, len);

      for (size_t j = 0; j < len; j++) {
        if (dst_align[j] != src_align[j] || !src_align[j]) {
          if (!src_align[j]) {
            printf("    src_align[%d] is 0, memcpy wrote into the source.\n", j);
          } else {
            printf("    mismatch at %d, expected %d found %d\n", j,
                   src_align[j], dst_align[j]);
          }
          printf("    Failed at size %d, src_align=%p[%d,%d], dst_align=%p[%d,%d]\n",
                 len, src_align, aligns[i][0], aligns[i][1],
                 dst_align, aligns[i][2], aligns[i][3]);
          return false;
        }
      }
      if (dst_align != dst && !verifyFencepost(&dst_align[-8])) {
        printf("    wrote before the array.\n");
        printf("    Failed at size %d, src_align=%p[%d,%d], dst_align=%p[%d,%d]\n",
               len, src_align, aligns[i][0], aligns[i][1],
               dst_align, aligns[i][2], aligns[i][3]);
        return false;
      }
      if (!verifyFencepost(&dst_align[len])) {
        printf("    wrote past the end of the array.\n");
        printf("    Failed at size %d, src_align=%p[%d,%d], dst_align=%p[%d,%d]\n",
               len, src_align, aligns[i][0], aligns[i][1],
               dst_align, aligns[i][2], aligns[i][3]);
        return false;
      }
    }
  }

  printf("  All tests pass.\n");

  return true;
}

bool runMemsetTest(void* (*test_memset)(void *s, int c, size_t n),
                   bool verbose) {
  uint8_t *buf = reinterpret_cast<uint8_t*>(malloc(MAX_MEMSET_BUFFER_SIZE));
  if (buf == NULL) {
    perror("Unable to allocate memory.\n");
    return false;
  }

  const int aligns[][2] = {
    // Use malloc return values unaltered.
    { 0, 0 },

    // Different alignments.
    { 1, 0 },
    { 2, 0 },
    { 4, 0 },
    { 8, 0 },
    { 16, 0 },
    { 32, 0 },
    { 64, 0 },

    // Different alignments between src and dst.
    { 8, 1 },
    { 8, 2 },
    { 8, 3 },
    { 8, 4 },
    { 8, 5 },
    { 8, 6 },
    { 8, 7 },
  };

  printf("  Verifying variable sized memsets at different alignments.\n");
  uint8_t *buf_align;
  for (size_t i = 0; i < sizeof(aligns)/sizeof(int[2]); i++) {
    for (size_t len = 0; len <= MAX_MEMSET_TEST_SIZE; len++) {
      if (aligns[i]) {
        buf_align = (uint8_t*)getAlignedPtr(buf+FENCEPOST_LENGTH, aligns[i][0],
                                            aligns[i][1]);
      } else {
        buf_align = buf;
      }

      if (verbose) {
        printf("Testing size %d, buf_align=%p[%d,%d]\n",
               len, buf_align, aligns[i][0], aligns[i][1]);
      }

      // Set the buffer to all zero without memset since it might be the
      // function we are testing.
      for (size_t j = 0; j < len; j++) {
        buf_align[j] = 0;
      }

      // Don't add a pre fencepost if we are using the value from the malloc.
      if (buf_align != buf) {
        setFencepost(&buf_align[-8]);
      }
      setFencepost(&buf_align[len]);

      int value = (len % 255) + 1;
      test_memset(buf_align, value, len);

      for (size_t j = 0; j < len; j++) {
        if (buf_align[j] != value) {
          printf("    Failed at size %d[%d,%d!=%d], buf_align=%p[%d,%d]\n",
                 len, j, buf_align[j], value, buf_align, aligns[i][0],
                 aligns[i][1]);
          return false;
        }
      }
      if (buf_align != buf && !verifyFencepost(&buf_align[-8])) {
        printf("    wrote before the beginning of the array.\n");
        printf("    Failed at size %d, buf_align=%p[%d,%d]\n",
               len, buf_align, aligns[i][0], aligns[i][1]);
        return false;
      }
      if (!verifyFencepost(&buf_align[len])) {
        printf("    wrote after the end of the array.\n");
        printf("    Failed at size %d, buf_align=%p[%d,%d]\n",
               len, buf_align, aligns[i][0], aligns[i][1]);
        return false;
      }
    }
  }

  printf("  All tests pass.\n");

  return true;
}

int main(int argc, char **argv) {
  bool verbose = false;
  if (argc == 2 && strcmp(argv[1], "-v") == 0) {
    verbose = true;
  }

  bool tests_passing = true;

  printf("Testing strcmp...\n");
  tests_passing = runStrcmpTest(strcmp, verbose) && tests_passing;

  printf("Testing memcpy...\n");
  tests_passing = runMemcpyTest(memcpy, verbose) && tests_passing;

  printf("Testing memset...\n");
  tests_passing = runMemsetTest(memset, verbose) && tests_passing;

  printf("Testing strlen...\n");
  tests_passing = runStrlenTest(strlen, verbose) && tests_passing;

  printf("Testing strcpy...\n");
  tests_passing = runStrcpyTest(strcpy, verbose) && tests_passing;

  printf("Testing strcat...\n");
  tests_passing = runStrcatTest(strcat, verbose) && tests_passing;

  return (tests_passing ? 0 : 1);
}
