#ifdef OS_darwin_10
#include <dispatch/dispatch.h>
#endif

#include <gtest/gtest.h>
#include <sys/types.h>
#include <sys/mman.h>
#import <Foundation/Foundation.h>

#include "test_utils.h"
#include "gtest_fixture_injection.h"

#ifndef OS_darwin
#error "This file should be built on Darwin only."
#endif

class Task {
 public:
  Task()  {};
  void Run() { printf("Inside Task::Run()\n"); }
};

@interface TaskOperation : NSOperation {
 @private
  Task *task_;
}

+ (id)taskOperationWithTask:(Task*)task;

- (id)initWithTask:(Task*)task;

@end  // @interface TaskOperation

@implementation TaskOperation

+ (id)taskOperationWithTask:(Task*)task {
  return [[TaskOperation alloc] initWithTask:task];
}

- (id)init {
  return [self initWithTask:NULL];
}

- (id)initWithTask:(Task*)task {
  if ((self = [super init])) {
    task_ = task;
  }
  return self;
}

- (void)main {
  if (!task_) {
    return;
  }

  task_->Run();
  delete task_;
  task_ = NULL;
}

- (void)dealloc {
  if (task_) delete task_;
  [super dealloc];
}

@end  // @implementation TaskOperation

namespace MacTests {
// Regression test for https://bugs.kde.org/show_bug.cgi?id=216837.
TEST(MacTests, WqthreadRegressionTest) {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  Task *task = new Task();
  NSOperationQueue *queue = [[NSOperationQueue alloc] init];
  [queue addOperation:[TaskOperation taskOperationWithTask:task]];

  sleep(1);  // wait for the worker thread to start.
  // If the bug is still there, ThreadSanitizer should crash after starting
  // the worker thread.
  [pool release];
}

// Regression test for a bug with passing a 32-bit value instead of a 64-bit
// as the last parameter to mmap().
TEST(MacTests, ShmMmapRegressionTest) {
#ifdef OS_darwin_9
  int md;
  void *virt_addr;
  shm_unlink("apple.shm.notification_center");
  md = shm_open("apple.shm.notification_center", 0, 0);
  if (md != -1) {
    virt_addr = mmap(0, 4096, 1, 1, md, 0);
    if (virt_addr == (void*)-1) {
      shm_unlink("apple.shm.notification_center");
      FAIL() << "mmap returned -1";
    } else {
      munmap(virt_addr, 4096);
      shm_unlink("apple.shm.notification_center");
    } 
  } else {
    printf("shm_open() returned -1\n");
  }
#endif
}

}  // namespace MacTests

#ifdef OS_darwin_10
namespace SnowLeopardTests {

int GLOB = 0;
StealthNotification sn1, sn2;

void worker_do_add(int *var, int val,
                   StealthNotification *notify,
                   StealthNotification *waitfor) {
  (*var) += val;
  fprintf(stderr, "var=%d\n", *var);
  notify->signal();
  // Make sure that the callbacks are ran on different threads.
  if (waitfor) {
    waitfor->wait();
  }
}

TEST(SnowLeopardTests, GCD_GlobalQueueRace) {
  ANNOTATE_EXPECT_RACE_FOR_TSAN(&GLOB, "SnowLeopardTests.GCD_GlobalQueueRace TP.");
  dispatch_queue_t queue = dispatch_get_global_queue(0,0);
  dispatch_block_t block_plus = ^{ worker_do_add(&GLOB, 1, &sn1, &sn2); };
  dispatch_block_t block_minus = ^{ worker_do_add(&GLOB, -1, &sn2, NULL); };
  dispatch_async(queue, block_plus);
  dispatch_async(queue, block_minus);
  sn1.wait();
  sn2.wait();
}

}  // namespace SnowLeopardTests
#endif
