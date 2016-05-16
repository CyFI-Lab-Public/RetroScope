#include "test_utils.h"
int   var = 0;
extern "C" { // TODO: make this line empty when ignore vs. mangling is fixed.
void Thread1() {
  usleep(100000);
  var = 1;
}

void Empty() {
}

void X() {
  if (var) {
    Empty();
  }
  var = 2;
}

void Y() {
  if (var) {
    Empty();
  }
  X();
}

void Thread2() {
  Y();
}
} // TODO: make this line empty when ignore vs. mangling is fixed.
int main() {
  ANNOTATE_TRACE_MEMORY(&var);
  var = 0;
  MyThread t1(Thread1, NULL, "test-thread-1");
  MyThread t2(Thread2, NULL, "test-thread-2");
  t1.Start();
  t2.Start();
  t1.Join();
  t2.Join();
}
