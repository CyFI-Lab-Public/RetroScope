#include <algorithm>
#include <stdexcept>

#include "trie.h"

namespace marisa_alpha {

Trie::Trie()
    : louds_(), labels_(), terminal_flags_(), link_flags_(), links_(),
      trie_(), tail_(), num_first_branches_(0), num_keys_(0) {}

void Trie::mmap(Mapper *mapper, const char *filename,
    long offset, int whence) {
  MARISA_ALPHA_THROW_IF(mapper == NULL, MARISA_ALPHA_PARAM_ERROR);
  Mapper temp_mapper;
  temp_mapper.open(filename, offset, whence);
  map(temp_mapper);
  temp_mapper.swap(mapper);
}

void Trie::map(const void *ptr, std::size_t size) {
  Mapper mapper(ptr, size);
  map(mapper);
}

void Trie::map(Mapper &mapper) {
  Trie temp;
  temp.louds_.map(mapper);
  temp.labels_.map(mapper);
  temp.terminal_flags_.map(mapper);
  temp.link_flags_.map(mapper);
  temp.links_.map(mapper);
  temp.tail_.map(mapper);
  mapper.map(&temp.num_first_branches_);
  mapper.map(&temp.num_keys_);

  if (temp.has_link() && !temp.has_tail()) {
    temp.trie_.reset(new (std::nothrow) Trie);
    MARISA_ALPHA_THROW_IF(!temp.has_trie(), MARISA_ALPHA_MEMORY_ERROR);
    temp.trie_->map(mapper);
  }
  temp.swap(this);
}

void Trie::load(const char *filename,
    long offset, int whence) {
  Reader reader;
  reader.open(filename, offset, whence);
  read(reader);
}

void Trie::fread(std::FILE *file) {
  Reader reader(file);
  read(reader);
}

void Trie::read(int fd) {
  Reader reader(fd);
  read(reader);
}

void Trie::read(std::istream &stream) {
  Reader reader(&stream);
  read(reader);
}

void Trie::read(Reader &reader) {
  Trie temp;
  temp.louds_.read(reader);
  temp.labels_.read(reader);
  temp.terminal_flags_.read(reader);
  temp.link_flags_.read(reader);
  temp.links_.read(reader);
  temp.tail_.read(reader);
  reader.read(&temp.num_first_branches_);
  reader.read(&temp.num_keys_);

  if (temp.has_link() && !temp.has_tail()) {
    temp.trie_.reset(new (std::nothrow) Trie);
    MARISA_ALPHA_THROW_IF(!temp.has_trie(), MARISA_ALPHA_MEMORY_ERROR);
    temp.trie_->read(reader);
  }
  temp.swap(this);
}

void Trie::save(const char *filename, bool trunc_flag,
    long offset, int whence) const {
  Writer writer;
  writer.open(filename, trunc_flag, offset, whence);
  write(writer);
}

void Trie::fwrite(std::FILE *file) const {
  Writer writer(file);
  write(writer);
}

void Trie::write(int fd) const {
  Writer writer(fd);
  write(writer);
}

void Trie::write(std::ostream &stream) const {
  Writer writer(&stream);
  write(writer);
}

void Trie::write(Writer &writer) const {
  louds_.write(writer);
  labels_.write(writer);
  terminal_flags_.write(writer);
  link_flags_.write(writer);
  links_.write(writer);
  tail_.write(writer);
  writer.write(num_first_branches_);
  writer.write(num_keys_);
  if (has_trie()) {
    trie_->write(writer);
  }
}

std::size_t Trie::num_tries() const {
  return has_trie() ? (trie_->num_tries() + 1) : (louds_.empty() ? 0 : 1);
}

std::size_t Trie::num_nodes() const {
  if (louds_.empty()) {
    return 0;
  }
  std::size_t num_nodes = (louds_.size() / 2) - 1;
  if (has_trie()) {
    num_nodes += trie_->num_nodes();
  }
  return num_nodes;
}

std::size_t Trie::total_size() const {
  return louds_.total_size() + labels_.total_size()
      + terminal_flags_.total_size() + link_flags_.total_size()
      + links_.total_size() + (has_trie() ? trie_->total_size() : 0)
      + tail_.total_size() + sizeof(num_first_branches_) + sizeof(num_keys_);
}

void Trie::clear() {
  Trie().swap(this);
}

void Trie::swap(Trie *rhs) {
  MARISA_ALPHA_THROW_IF(rhs == NULL, MARISA_ALPHA_PARAM_ERROR);
  louds_.swap(&rhs->louds_);
  labels_.swap(&rhs->labels_);
  terminal_flags_.swap(&rhs->terminal_flags_);
  link_flags_.swap(&rhs->link_flags_);
  links_.swap(&rhs->links_);
  Swap(&trie_, &rhs->trie_);
  tail_.swap(&rhs->tail_);
  Swap(&num_first_branches_, &rhs->num_first_branches_);
  Swap(&num_keys_, &rhs->num_keys_);
}

}  // namespace marisa_alpha
