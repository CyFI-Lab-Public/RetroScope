/*---------------------------------------------------------------------------*
 *  ptrd.h  *
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

#ifndef PTRD_H
#define PTRD_H




#ifdef USE_THREAD

#include "PortPrefix.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"

#define STACKSIZE_S2G_SINKER      12*1024
#define STACKSIZE_S2G_RECOGNIZER  25*1024
#define STACKSIZE_DEFAULT         18*1024

#ifdef _WIN32
typedef unsigned int PTHREAD_ID;
#define PtrdGetCurrentThreadId GetCurrentThreadId
#elif defined(POSIX)

#if defined(__vxworks) && !defined(REAL_PTHREADS)
#include "pthread_vx.h"
#else
#include <pthread.h>

#ifndef _POSIX_THREADS
#error "Thread is not defined!"
#endif
#endif /* #if defined(__vxworks) && !defined(REAL_PTHREADS) */

typedef pthread_t PTHREAD_ID;
#define PtrdGetCurrentThreadId pthread_self
#else
#error Portable Synchronization not defined for this OS!
#endif /* os dependant basic types */

/**
 * @addtogroup PtrdModule PThread API functions
 * Library for basic thread and monitor functionality to ensure portability.
 * Call PtrdInit() to initialize and PtrdShutdown() to shutdown module.
 *
 * Every thread has a priority. Threads with higher priority are executed in preference
 * to threads with lower priority. When code running in some thread creates a new Thread
 * object, the new thread has its priority initially set equal to the priority of the creating
 * thread.
 *
 *
 * @{
 */

/** Typedef */
typedef struct PtrdMonitor_t PtrdMonitor;
/** Typedef */
typedef struct PtrdMutex_t PtrdMutex;
/** Typedef */
typedef struct PtrdSemaphore_t PtrdSemaphore;
/** Typedef */
typedef struct PtrdThread_t PtrdThread;


/**
 * Blocks the current thread for the specified amount of time.
 *
 * @param sleepTimeMs number of milliseconds to sleep.  A value of 0 is
 * equivalent to a thread yield.
 *
 * @return ESR_SUCCESS if success, or something else to indicate a failure.
 */
PORTABLE_API ESR_ReturnCode PtrdSleep(asr_uint32_t sleepTimeMs);

/**
 * Creates a thread monitor.  Thread monitors can be locked, unlocked, can be
 * waited on and can be notified.  Monitors implement so-called recursive
 * locking, meaning that a thread owning the monitor can call lock without
 * blocking and will have to call unlock() as many times as lock() was called.
 *
 * @param  monitor  Handle to the created monitor
 *
 * @return ESR_SUCCESS if succes, or something else to indicate a failure.  In
 * particular, it will return ESR_INVALID_STATE if the threading API is not
 * properly initialized.
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorCreate(PtrdMonitor **monitor);

/**
 * Destroys a monitor.
 *
 * @param  monitor  Handle to the monitor to destroy
 *
 * @return ESR_SUCCESS if success; ESR_INVALID_STATE if this function is called after the thread
 * library is shutdown, or cannot lock on mutex; ESR_INVALID_ARGUMENT if monitor is null
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorDestroy(PtrdMonitor *monitor);

/**
 * Locks a monitor.
 *
 * @param  monitor  Handle to the monitor to lock
 * @param  fname Filename of code requesting a lock
 * @param  line Line of code requesting a lock
 *
 * @return ESR_SUCCESS if success; ESR_INVALID_ARGUMENT if monitor is null; ESR_FATAL_ERROR if waiting on the mutex failed
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorLockWithLine(PtrdMonitor *monitor, const LCHAR *fname, int line);
/**
 * Locks a monitor.
 *
 * @param  monitor  Handle to the monitor to lock
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.
 */
#define PtrdMonitorLock(monitor) PtrdMonitorLockWithLine(monitor, L(__FILE__), __LINE__)

/**
 * Unlock a Monitor
 *
 * @param  monitor  Handle to the monitor to unlock
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.  In particular, it will return ESR_INVALID_STATE if the current
 * thread does not hold the monitor.
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorUnlock(PtrdMonitor *monitor);

/**
 * Causes current thread to wait until another thread invokes the
 * <code>PtrdMonitorNotify()</code> method or the
 * <code>PtrdMonitorNotifyAll()</code> method for this monitor.
 *
 * <p>
 *
 * The current thread must own this monitor. The thread releases ownership of
 * this monitor and waits until another thread notifies threads waiting on
 * this object's monitor to wake up either through a call to the
 * <code>PtrdMonitorNotify</code> method or the
 * <code>PtrdMonitorNotifyAll</code> method. The thread then waits until it
 * can re-obtain ownership of the monitor and resumes execution.
 *
 * @param monitor The monitor on which to wait.
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.  In particular, it will return ESR_INVALID_STATE if the current
 * thread does not hold the monitor.
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorWait(PtrdMonitor *monitor);


/**
 * Causes current thread to wait until either another thread invokes the
 * <code>PtrdMonitorNotify()</code> method or the
 * <code>PtrdMonitorNotifyAll()</code> method for this monitor, or a specified
 * amount of time has elapsed.
 *
 * @param monitor The monitor on which to wait.
 *
 * @param timeoutMs The amount of time (in millisecs) to wait for
 * notification.
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.  In particular, it will return ESR_INVALID_STATE if the current
 * thread does not hold the monitor, or ESR_TIMED_OUT if the timeout expired
 * without a notification.
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorWaitTimeout(PtrdMonitor *monitor,
    asr_uint32_t timeoutMs);
    
/**
 * Wakes up a single thread that is waiting on this monitor. If more than one
 * thread are waiting on this object, one of them is arbitrarily chosen to be
 * awakened. A thread waits on the monitor by calling
 * <code>PtrdMonitorWait</code> or <code>PtrdMonitorWaitTimeout</code>.
 *
 * <p>
 *
 * The awakened thread will not be able to proceed until the current thread
 * relinquishes the lock on this object. The awakened thread will compete in
 * the usual manner with any other threads that might be actively competing to
 * synchronize on this object; for example, the awakened thread enjoys no
 * reliable privilege or disadvantage in being the next thread to lock this
 * monitor.
 *
 * <p>
 *
 * This method should only be called by a thread that is the owner of this
 * monitor.
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.  In particular, it will return ESR_INVALID_STATE if the current
 * thread does not hold the monitor, or ESR_TIMED_OUT if the timeout expired
 * without a notification.
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorNotify(PtrdMonitor *monitor);

/**
 * Wakes up all threads that are waiting on this monitor. A thread waits on
 * a monitor by calling <code>PtrdMonitorWait</code> or
 * <code>PtrdMonitorWaitTimeout</code>
 *
 * <p>
 *
 * The awakened threads will not be able to proceed until the current thread
 * relinquishes the monitor. The awakened threads will compete in the usual
 * manner with any other threads that might be actively competing to
 * synchronize on this monitor; for example, the awakened threads enjoy no
 * reliable privilege or disadvantage in being the next thread to lock this
 * object.
 *
 * <p>
 *
 * This method should only be called by a thread that is the owner of this
 * object's monitor.
 *
 * @param monitor The monitor on which to wait.
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.  In particular, it will return ESR_INVALID_STATE if the current
 * thread does not hold the monitor.
 */
PORTABLE_API ESR_ReturnCode PtrdMonitorNotifyAll(PtrdMonitor *monitor);

/**
 * Creates a thread mutex.  Thread mutexes are similar to thread monitors
 * except that they do not support wait and notify mechanism and require less
 * resources from the OS.  In situations where this mechanism is not required,
 * using mutexes instead of monitors is preferable. Mutexes implement
 * so-called recursive locking, meaning that a thread owning the mutex can
 * call lock without blocking and will have to call unlock() as many times as
 * lock() was called.
 *
 * @param  mutex  Handle to the created mutex
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.
 */
PORTABLE_API ESR_ReturnCode PtrdMutexCreate(PtrdMutex **mutex);

/**
 * Destroys a mutex.
 *
 * @param  mutex  Handle to the mutex to destroy
 *
 * @return        ESR_ReturnCode 0 on success
 */
PORTABLE_API ESR_ReturnCode PtrdMutexDestroy(PtrdMutex *mutex);

/**
 * Lock a mutex
 *
 * @param  mutex  Handle to the mutex to lock
 * @param  fname Filename of code requesting a lock
 * @param  line Line of code requesting a lock
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.
 */
PORTABLE_API ESR_ReturnCode PtrdMutexLockWithLine(PtrdMutex *mutex, const LCHAR *fname, int line);
/**
 * Lock a mutex
 *
 * @param  mutex  Handle to the mutex to lock
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.
 */
#define PtrdMutexLock(mutex) PtrdMutexLockWithLine(mutex, L(__FILE__), __LINE__)

/**
 * Unlock a Mutex
 *
 * @param  mutex  Handle to the mutex to unlock
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure. In particular, it will return ESR_INVALID_STATE if the current
 * thread does not hold the mutex.
 */
PORTABLE_API ESR_ReturnCode PtrdMutexUnlock(PtrdMutex *mutex);


/**
 * Creates a thread semaphore.
 *
 * @param  semaphore  Handle to the created semaphore.
 * @param  initValue  Initial semaphore value
 * @param  maxValue   Maximum semaphore value
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.
 */
PORTABLE_API ESR_ReturnCode PtrdSemaphoreCreate(unsigned int initValue,
    unsigned int maxValue,
    PtrdSemaphore **semaphore);
    
/**
 * Destroy a semaphore
 *
 * @param  semaphore  Handle to the semaphore to destroy
 *
 * @return ESR_SUCCESS if success, or an an error indicating the cause of the
 * failure.
 */
PORTABLE_API ESR_ReturnCode PtrdSemaphoreDestroy(PtrdSemaphore *semaphore);

/**
 * Decrements the semaphore.  If the semaphore's current value is 0, the
 * current thread waits until the semaphore's value is greater than 0.
 *
 * @param  semaphore  Handle to the semaphore to acquire.
 *
 * @return ESR_SUCCESS if successful, or a status code indicating the nature of
 * the error.
 */
PORTABLE_API ESR_ReturnCode PtrdSemaphoreAcquire(PtrdSemaphore *semaphore);


/**
 * Decrements the semaphore.  If the semaphore's current value is 0, the
 * current thread waits until the semaphore's value is greater than 0 or until
 * the timeout expires.
 *
 * @param  semaphore  Handle to the semaphore to acquire.
 * @param  timeoutMs  Timeout in milliseconds.
 *
 * @return ESR_SUCCESS if wait is successful, ESR_TIMED_OUT if timed out, or an
 * error status indicating the nature of the error in other situations.
 */
PORTABLE_API ESR_ReturnCode PtrdSemaphoreAcquireTimeout(PtrdSemaphore *semaphore,
    asr_uint32_t timeoutMs);
    
/**
 * Increments a semaphore.
 *
 * @param semaphore Handle to the semaphore to release.
 *
 * @return ESR_SUCCESS success or an error status indicating the nature of the
 * error.  In particular, it will return ESR_INVALID_STATE if the semaphore is
 * currently at its maximum value.
 */
PORTABLE_API ESR_ReturnCode PtrdSemaphoreRelease(PtrdSemaphore *semaphore);


/**
 * Function signature invoked on the new thread by PtrdThreadCreate(), and
 * the argument to that function.
 */
typedef void* PtrdThreadArg;
/**
 * Function prototype that launched threads must conform to.
 *
 * @param userData Data passed in by caller of PtrdThreadCreate
 */
typedef void(*PtrdThreadStartFunc)(PtrdThreadArg userData);

/**
 * Minimum thread priority.
 */
#define PtrdThreadMinPriority 0

/**
 * Maximum thread priority.
 */
#define PtrdThreadMaxPriority UINT16_TMAX

/**
 * Normal thread priority.
 */
#define PtrdThreadNormalPriority (PtrdThreadMaxPriority / 2)

/**
 * Creates a thread.
 *
 * Execution starts on the thread immediately. To pause execution use a
 * monitor or a mutex between the thread and the thread creator.
 *
 * @param  thread       Handle to the thread that is created
 * @param  startFunc    Function for the thread to start execution on
 * @param  arg          Argument to the thread function
 *
 * @return ESR_INVALID_ARGUMENT if thread or startFunc are null; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_THREAD_CREATION_ERROR if thread cannot be created
 */
PORTABLE_API ESR_ReturnCode PtrdThreadCreate(PtrdThreadStartFunc startFunc, PtrdThreadArg arg,
    PtrdThread** thread);
    
/**
 * Destroys a thread handle.
 *
 * Note: this does NOT stop or destroy the thread, it just releases
 * the handle for accessing it. If this is not done, a memory leak
 * occurs, so if the creator of the thread never needs to communicate
 * with the thread again it should call this immediately after the
 * create if the create was successful.
 *
 * @return ESR_SUCCESS on failure or an error indicating the nature of the
 * error.
 */
PORTABLE_API ESR_ReturnCode PtrdThreadDestroy(PtrdThread *thread);

/**
 * Wait for the termination of a specified thread
 *
 * @param thread Handle to the thread to wait for
 *
 * @return ESR_INVALID_ARGUMENT if thread is null
 */
PORTABLE_API ESR_ReturnCode PtrdThreadJoin(PtrdThread *thread);

/**
 * Returns the thread priority.
 *
 * @param thread PtrdThread handle
 * @param value [out] Thread priority
 *
 * @return ESR_INVALID_ARGUMENT if thread or value are null; ESR_INVALID_STATE if thread priority cannot be
 * retrieved
 */
PORTABLE_API ESR_ReturnCode PtrdThreadGetPriority(PtrdThread *thread, asr_uint16_t* value);

/**
 * Sets the thread priority.
 *
 * @param thread PtrdThread handle
 * @param value Thread priority
 *
 * @return ESR_INVALID_ARGUMENT if thread or value are null; ESR_INVALID_STATE if thread priority cannot be
 * set
 */
PORTABLE_API ESR_ReturnCode PtrdThreadSetPriority(PtrdThread *thread, asr_uint16_t value);

/**
 * Yields execution of the current thread to other threads.
 *
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode PtrdThreadYield(void);

/**
 * Initializes the thread library.  This should be called before creating the
 * first thread or the first monitor.
 *
 * @return ESR_INVALID_STATE if the Ptrd module has already been initialized;
 * ESR_MUTEX_CREATION_ERROR if mutex cannot be created
 */
PORTABLE_API ESR_ReturnCode PtrdInit(void);

/**
 * Indicates if thread library has been initialized.
 *
 * @param enabled [out] True if library is initialized
 * @return ESR_INVALID_ARGUMENT if enabled is null
 */
PORTABLE_API ESR_ReturnCode PtrdIsEnabled(ESR_BOOL* enabled);

/**
 * Shutdowns the thread library.  All thread and monitor should be terminated
 * and destroyed before calling this function.
 *
 * @return ESR_INVALID_STATE if Ptrd module is not running
 * error.
 */
PORTABLE_API ESR_ReturnCode PtrdShutdown(void);

/**
 * @}
 */

#else


//#error "Including ptrd.h on a non-threaded platform."


#endif /* USE_THREAD */
#endif  
