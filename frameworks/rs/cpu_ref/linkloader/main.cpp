/*
 * Copyright 2011, The Android Open Source Project
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

#include "ELFObject.h"

#include "utils/serialize.h"
#include "ELF.h"

#include <llvm/ADT/OwningPtr.h>

#include <fcntl.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <map>
#include <stdio.h>
#include <stdarg.h>

using namespace std;

bool open_mmap_file(char const *filename,
                    int &fd,
                    unsigned char const *&image,
                    size_t &size);

void close_mmap_file(int fd,
                     unsigned char const *image,
                     size_t size);

void dump_and_run_file(unsigned char const *image, size_t size,
                       int argc, char **argv);

int main(int argc, char **argv) {
  // Check arguments
  if (argc < 2) {
    llvm::errs() << "USAGE: " << argv[0] << " [ELFObjectFile] [ARGS]\n";
    exit(EXIT_FAILURE);
  }

  // Filename from argument
  char const *filename = argv[1];

  // Open the file
  int fd = -1;
  unsigned char const *image = NULL;
  size_t image_size = 0;

  if (!open_mmap_file(filename, fd, image, image_size)) {
    exit(EXIT_FAILURE);
  }

  // Dump and run the file
  dump_and_run_file(image, image_size, argc - 1, argv + 1);

  // Close the file
  close_mmap_file(fd, image, image_size);

  return EXIT_SUCCESS;
}

// FIXME: I don't like these stub as well.  However, before we implement
// x86 64bit far jump stub, we have to ensure find_sym only returns
// near address.

int stub_printf(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int result = vprintf(fmt, ap);
  va_end(ap);
  return result;
}

int stub_scanf(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int result = vscanf(fmt, ap);
  va_end(ap);
  return result;
}

void stub_srand(unsigned int seed) {
  srand(seed);
}

int stub_rand() {
  return rand();
}

time_t stub_time(time_t *output) {
  return time(output);
}

void *find_sym(void *context, char const *name) {
  struct func_entry_t {
    char const *name;
    size_t name_len;
    void *addr;
  };

  static func_entry_t const tab[] = {
#define DEF(NAME, ADDR) \
    { NAME, sizeof(NAME) - 1, (void *)(ADDR) },

    DEF("printf", stub_printf)
    DEF("scanf", stub_scanf)
    DEF("__isoc99_scanf", stub_scanf)
    DEF("rand", stub_rand)
    DEF("time", stub_time)
    DEF("srand", stub_srand)
#undef DEF
  };

  static size_t const tab_size = sizeof(tab) / sizeof(func_entry_t);

  // Note: Since our table is small, we are using trivial O(n) searching
  // function.  For bigger table, it will be better to use binary
  // search or hash function.
  size_t name_len = strlen(name);
  for (size_t i = 0; i < tab_size; ++i) {
    if (name_len == tab[i].name_len && strcmp(name, tab[i].name) == 0) {
      return tab[i].addr;
    }
  }

  assert(0 && "Can't find symbol.");
  return 0;
}

template <unsigned Bitwidth, typename Archiver>
void dump_and_run_object(Archiver &AR, int argc, char **argv) {
  llvm::OwningPtr<ELFObject<Bitwidth> > object(ELFObject<Bitwidth>::read(AR));

  if (!object) {
    llvm::errs() << "ERROR: Unable to load object\n";
  }

  object->print();
  out().flush();

  ELFSectionSymTab<Bitwidth> *symtab =
    static_cast<ELFSectionSymTab<Bitwidth> *>(
        object->getSectionByName(".symtab"));

  object->relocate(find_sym, 0);
  out() << "relocate finished!\n";
  out().flush();

  int machine = object->getHeader()->getMachine();

  void *main_addr = symtab->getByName("main")->getAddress(machine);
  out() << "main address: " << main_addr << "\n";
  out().flush();

  ((int (*)(int, char **))main_addr)(argc, argv);
  fflush(stdout);
}

template <typename Archiver>
void dump_and_run_file_from_archive(bool is32bit, Archiver &AR,
                                    int argc, char **argv) {
  if (is32bit) {
    dump_and_run_object<32>(AR, argc, argv);
  } else {
    dump_and_run_object<64>(AR, argc, argv);
  }
}

void dump_and_run_file(unsigned char const *image, size_t size,
                       int argc, char **argv) {
  if (size < EI_NIDENT) {
    llvm::errs() << "ERROR: ELF identification corrupted.\n";
    return;
  }

  if (image[EI_DATA] != ELFDATA2LSB && image[EI_DATA] != ELFDATA2MSB) {
    llvm::errs() << "ERROR: Unknown endianness.\n";
    return;
  }

  if (image[EI_CLASS] != ELFCLASS32 && image[EI_CLASS] != ELFCLASS64) {
    llvm::errs() << "ERROR: Unknown machine class.\n";
    return;
  }

  bool isLittleEndian = (image[EI_DATA] == ELFDATA2LSB);
  bool is32bit = (image[EI_CLASS] == ELFCLASS32);

  if (isLittleEndian) {
    ArchiveReaderLE AR(image, size);
    dump_and_run_file_from_archive(is32bit, AR, argc, argv);
  } else {
    ArchiveReaderBE AR(image, size);
    dump_and_run_file_from_archive(is32bit, AR, argc, argv);
  }
}

bool open_mmap_file(char const *filename,
                    int &fd,
                    unsigned char const *&image,
                    size_t &size) {
  // Query the file status
  struct stat sb;
  if (stat(filename, &sb) != 0) {
    llvm::errs() << "ERROR: " << filename << " not found.\n";
    return false;
  }

  if (!S_ISREG(sb.st_mode)) {
    llvm::errs() << "ERROR: " << filename << " is not a regular file.\n";
    return false;
  }

  size = (size_t)sb.st_size;

  // Open the file in readonly mode
  fd = open(filename, O_RDONLY);
  if (fd < 0) {
    llvm::errs() << "ERROR: Unable to open " << filename << "\n";
    return false;
  }

  // Map the file image
  image = static_cast<unsigned char const *>(
    mmap(0, size, PROT_READ, MAP_PRIVATE, fd, 0));

  if (image == MAP_FAILED) {
    llvm::errs() << "ERROR: Unable to map " << filename << " to memory.\n";
    close(fd);
    return false;
  }

  return true;
}

void close_mmap_file(int fd,
                     unsigned char const *image,
                     size_t size) {
  if (image) {
    munmap((void *)image, size);
  }

  if (fd >= 0) {
    close(fd);
  }
}
