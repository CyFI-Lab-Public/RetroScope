#ifdef _MSC_VER
#include <io.h>
#else
#include <unistd.h>
#endif  // _MSC_VER

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <fstream>
#include <sstream>

#include <marisa_alpha/io.h>

#include "assert.h"

namespace {

void TestFilename() {
  TEST_START();

  {
    marisa_alpha::Writer writer;
    writer.open("io-test.dat");
    marisa_alpha::UInt32 value = 123;
    writer.write(value);
    writer.write(value);
    double values[] = { 345, 456 };
    writer.write(values, 2);
    EXCEPT(writer.write(values, 1U << 30), MARISA_ALPHA_SIZE_ERROR);
  }

  {
    marisa_alpha::Writer writer;
    writer.open("io-test.dat", false, 4, SEEK_SET);
    marisa_alpha::UInt32 value = 234;
    writer.write(value);
  }

  {
    marisa_alpha::Writer writer;
    writer.open("io-test.dat", false, 0, SEEK_END);
    double value = 567;
    writer.write(value);
  }

  {
    marisa_alpha::Reader reader;
    reader.open("io-test.dat");
    marisa_alpha::UInt32 value;
    reader.read(&value);
    ASSERT(value == 123);
    reader.read(&value);
    ASSERT(value == 234);
    double values[3];
    reader.read(values, 3);
    ASSERT(values[0] == 345);
    ASSERT(values[1] == 456);
    ASSERT(values[2] == 567);
    char byte;
    EXCEPT(reader.read(&byte), MARISA_ALPHA_IO_ERROR);
  }

  {
    marisa_alpha::Mapper mapper;
    mapper.open("io-test.dat");
    marisa_alpha::UInt32 value;
    mapper.map(&value);
    ASSERT(value == 123);
    mapper.map(&value);
    ASSERT(value == 234);
    const double *values;
    mapper.map(&values, 3);
    ASSERT(values[0] == 345);
    ASSERT(values[1] == 456);
    ASSERT(values[2] == 567);
    char byte;
    EXCEPT(mapper.map(&byte), MARISA_ALPHA_IO_ERROR);
  }

  {
    marisa_alpha::Writer writer;
    writer.open("io-test.dat");
  }

  {
    marisa_alpha::Reader reader;
    reader.open("io-test.dat");
    char byte;
    EXCEPT(reader.read(&byte), MARISA_ALPHA_IO_ERROR);
  }

  TEST_END();
}

void TestFd() {
  TEST_START();

  {
#ifdef _MSC_VER
    int fd = -1;
    ASSERT(::_sopen_s(&fd, "io-test.dat",
        _O_BINARY | _O_CREAT | _O_WRONLY | _O_TRUNC,
        _SH_DENYRW, _S_IREAD | _S_IWRITE) == 0);
#else  // _MSC_VER
    int fd = ::creat("io-test.dat", 0644);
    ASSERT(fd != -1);
#endif  // _MSC_VER
    marisa_alpha::Writer writer(fd);
    marisa_alpha::UInt32 value = 345;
    writer.write(value);
    double values[] = { 456, 567, 678 };
    writer.write(values, 3);
#ifdef _MSC_VER
    ASSERT(::_close(fd) == 0);
#else  // _MSC_VER
    ASSERT(::close(fd) == 0);
#endif  // _MSC_VER
  }

  {
#ifdef _MSC_VER
    int fd = -1;
    ASSERT(::_sopen_s(&fd, "io-test.dat", _O_BINARY | _O_RDONLY,
        _SH_DENYRW, _S_IREAD) == 0);
#else  // _MSC_VER
    int fd = ::open("io-test.dat", O_RDONLY);
    ASSERT(fd != -1);
#endif  // _MSC_VER
    marisa_alpha::Reader reader(fd);
    marisa_alpha::UInt32 value;
    reader.read(&value);
    ASSERT(value == 345);
    double values[3];
    reader.read(values, 3);
    ASSERT(values[0] == 456);
    ASSERT(values[1] == 567);
    ASSERT(values[2] == 678);
    char byte;
    EXCEPT(reader.read(&byte), MARISA_ALPHA_IO_ERROR);
#ifdef _MSC_VER
    ASSERT(::_close(fd) == 0);
#else  // _MSC_VER
    ASSERT(::close(fd) == 0);
#endif  // _MSC_VER
  }

  TEST_END();
}

void TestFile() {
  TEST_START();

  {
#ifdef _MSC_VER
    FILE *file = NULL;
    ASSERT(::fopen_s(&file, "io-test.dat", "wb") == 0);
#else  // _MSC_VER
    FILE *file = std::fopen("io-test.dat", "wb");
    ASSERT(file != NULL);
#endif  // _MSC_VER
    marisa_alpha::Writer writer(file);
    marisa_alpha::UInt32 value = 345;
    writer.write(value);
    double values[3] = { 456, 567, 678 };
    writer.write(values, 3);
    ASSERT(std::fclose(file) == 0);
  }

  {
#ifdef _MSC_VER
    FILE *file = NULL;
    ASSERT(::fopen_s(&file, "io-test.dat", "rb") == 0);
#else  // _MSC_VER
    FILE *file = std::fopen("io-test.dat", "rb");
    ASSERT(file != NULL);
#endif  // _MSC_VER
    marisa_alpha::Reader reader(file);
    marisa_alpha::UInt32 value;
    reader.read(&value);
    ASSERT(value == 345);
    double values[3];
    reader.read(values, 3);
    ASSERT(values[0] == 456);
    ASSERT(values[1] == 567);
    ASSERT(values[2] == 678);
    char byte;
    EXCEPT(reader.read(&byte), MARISA_ALPHA_IO_ERROR);
    ASSERT(std::fclose(file) == 0);
  }

  TEST_END();
}

void TestStream() {
  TEST_START();

  {
    std::ofstream file("io-test.dat", std::ios::binary);
    ASSERT(file.is_open());
    marisa_alpha::Writer writer(&file);
    marisa_alpha::UInt32 value = 345;
    writer.write(value);
    double values[3] = { 456, 567, 678 };
    writer.write(values, 3);
  }

  {
    std::ifstream file("io-test.dat", std::ios::binary);
    ASSERT(file.is_open());
    marisa_alpha::Reader reader(&file);
    marisa_alpha::UInt32 value;
    reader.read(&value);
    ASSERT(value == 345);
    double values[3];
    reader.read(values, 3);
    ASSERT(values[0] == 456);
    ASSERT(values[1] == 567);
    ASSERT(values[2] == 678);
    char byte;
    EXCEPT(reader.read(&byte), MARISA_ALPHA_IO_ERROR);
  }

  TEST_END();
}

}  // namespace

int main() {
  TestFilename();
  TestFd();
  TestFile();
  TestStream();

  return 0;
}
