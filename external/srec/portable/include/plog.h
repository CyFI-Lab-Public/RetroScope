/*---------------------------------------------------------------------------*
 *  plog.h  *
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

#ifndef PLOG_H
#define PLOG_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#ifdef USE_STACKTRACE
#include "PStackTrace.h"
#endif
#include "passert.h"
#include "PFileSystem.h"
#include "ptypes.h"

/**
 * @addtogroup PLogModule PLogger API functions
 * Logging API.
 *
 * Must call pmemInit() before using this module.
 *
 * The logging API is composed of a Logger.
 * A Logger is an object who implements an API to actually write log messages.
 * The logging API uses the logger when logging is to be performed but does
 * not depend on an actual implementation of this API.
 *
 * When a request for logging is performed, the current level of the logging
 * API is compared with the current stack-trace level.  If the logger's log
 * level is greater than or equal to the stack-trace level, then the
 * message is being logged through the use of the Logger.  Otherwise, the
 * message is not logged.  Setting the log level of the API to UINT_MAX is
 * equivalent to unconditionally log all messages from all modules. Conversely,
 * setting the log level of the API to 0 is equivalent to disabling logging globally.
 *
 * @{
 */


/**
 * Portable logging framework.
 */
typedef struct PLogger_t
{
  /**
   * Prints and formats a message to the log.
   *
   * @param self the Logger.
   *
   * @param format the format string specifying the next arguments (a la
   * printf).
   *
   * @return ESR_SUCCESS if success, otherwise a status code indicating the
   * nature of the error.
   */
  ESR_ReturnCode(*printf)(struct PLogger_t *self,
                          const LCHAR *format, ...);
                          
  /**
   * Flushes internal buffer.  This function can be left unimplemented if no
   * buffering is performed by the logger.
  
   * @param self the Logger
   *
   * @return ESR_SUCCESS if success, otherwise a status code indicating the nature of the error.
   */
  ESR_ReturnCode(*flush)(struct PLogger_t *self);
  
  /**
   * Destroys the logger.  This function is responsible to deallocate any
   * resources used by the logger.  In particular, if buffering is internally
   * used, it needs to flush the buffer.
   */
  void(*destroy)(struct PLogger_t *self);
}
PLogger;

/**
 * Type used to control output format.
 */
typedef asr_uint16_t LOG_OUTPUT_FORMAT;

/**
 * Specifies that no extra information is to be output.
 */
#define LOG_OUTPUT_FORMAT_NONE 0x0000

/**
 * Specifies that the date and time is to be output.
 */
#define LOG_OUTPUT_FORMAT_DATE_TIME 0x0001

/**
 * Specifies that thread id of thread generating the message is to be output.
 */
#define LOG_OUTPUT_FORMAT_THREAD_ID 0x0002

/**
 * Specifies that the module name of the module generating the message is to
 * be output.
 */
#define LOG_OUTPUT_FORMAT_MODULE_NAME 0x0004

/**
 * Initializes the LOG library.  This function must be called before any
 * logging can take place. PtrdInit() must be called before this function on
 * platforms that support threads.
 *
 * @param logger The logger to be used to output the messages.  If NULL, then
 * logging goes to PSTDERR.
 *
 * @param logLevel The level of logging requested.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error. In particular, it returns ESR_INVALID_STATE if already
 * initialized.
 */
PORTABLE_API ESR_ReturnCode PLogInit(PLogger *logger, unsigned int logLevel);

/**
 * Indicates if PLog module is initialized.
 *
 * @param isInit True if module is initialized
 * @return ESR_INVALID_ARGUMENT if isLocked is null
 */
PORTABLE_API ESR_ReturnCode PLogIsInitialized(ESR_BOOL* isInit);

/**
 * Indicates if PLog module is locked inside a critical section. This is for internal use only.
 *
 * @param isLocked True if module is locked
 * @return ESR_INVALID_ARGUMENT if isLocked is null
 */
PORTABLE_API ESR_ReturnCode PLogIsLocked(ESR_BOOL* isLocked);

/**
 * Shutdowns the LOG library.  Once this function is called, no logging
 * activity can be performed.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.  In particular, it returns ESR_INVALID_STATE if not
 * initialized or already shutted down.
 */
PORTABLE_API ESR_ReturnCode PLogShutdown(void);

/**
 * Sets the format of the logging messages.  If this function is never called,
 * the default format is
 *
 * <code>LOG_OUTPUT_FORMAT_MODULE_NAME | LOG_OUTPUT_FORMAT_DATE_TIME</code>.
 *
 * @param format the format specification for new messages.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.
 */
PORTABLE_API ESR_ReturnCode PLogSetFormat(LOG_OUTPUT_FORMAT format);

/**
 * Gets the current log level of the LOG API.
 *
 * @param logLevel A pointer to where the log level is to be stored.  If NULL,
 * the function returns ESR_INVALID_ARGUMENT.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.  In particular, it returns ESR_INVALID_STATE if the
 * API is not initialized.
 */
PORTABLE_API ESR_ReturnCode PLogGetLevel(unsigned int *logLevel);


/**
 * Sets the current log level of the LOG API.
 *
 * @param logLevel The new log level.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.  In particular, it returns ESR_INVALID_STATE if the
 * API is not initialized.
 */
PORTABLE_API ESR_ReturnCode PLogSetLevel(unsigned int logLevel);

/**
 * Conditionally Logs a message.  The message is logged only if module is enabled.
 *
 * @param msg The message format specification (ala printf).
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.  In particular, it returns ESR_INVALID_STATE if
 * the API is not initialized.
 */
PORTABLE_API ESR_ReturnCode PLogMessage(const LCHAR* msg, ...);

/**
 * Unconditionally logs an error message.
 *
 * @param msg The message format specification (ala printf).
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.  In particular, it returns ESR_INVALID_STATE if
 * the API is not initialized.
 */
PORTABLE_API ESR_ReturnCode PLogError(const LCHAR* msg, ...);


/**
 *
 * Creates a logger that logs to a file.
 *
 * @param file The file to log to.
 * @param logger logger handle receiving the created logger.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.
 */
PORTABLE_API ESR_ReturnCode PLogCreateFileLogger(PFile* file,
    PLogger** logger);
    
/**
 * Creates a logger that logs to a circular file.
 *
 * @param filename The name of the file to be created.
 * @param maxsize The maximum number of bytes that the file may have.
 * @param logger logger handle receiving the created logger.
 *
 * @return ESR_SUCCESS if success, otherwise a status code indicating the
 * nature of the error.
 */
PORTABLE_API ESR_ReturnCode PLogCreateCircularFileLogger(const LCHAR* filename,
    unsigned int maxsize,
    PLogger** logger);
    
    
    
/**
 * Runs a function, checks its return-code. In case of an error, logs it and jumps to
 * the CLEANUP label.
 */
/* show more information for vxworks due to lack of stack trace */
#define CHKLOG(rc, function) do { rc = (function); if (rc != ESR_SUCCESS) { PLogError("%s in %s:%d", ESR_rc2str(rc),  __FILE__, __LINE__); goto CLEANUP; } } while (0)
/**
 * Invokes the function with args and if it is not ESR_SUCCESS, logs and
 * returns it.
 *
 * @param rc Used to store the function return value
 * @param function Function name
 * @param args Function arguments
 */
#define PLOG_CHKRC_ARGS(rc, function, args) do { if((rc = (function args)) != ESR_SUCCESS) { PLogError(ESR_rc2str(rc)); return rc; } } while (0)

/**
 * Checks the function return-code and if it is not ESR_SUCCESS, logs and
 * returns it.
 */
#define PLOG_CHKRC(rc, function) do { rc = (function); if (rc != ESR_SUCCESS) { PLogError(rc); return rc; } } while (0)

#if defined(_DEBUG) && !defined(ENABLE_PLOG_TRACE) && ENABLE_STACKTRACE
#define ENABLE_PLOG_TRACE
#endif

/**
 * Macro used to have logging enabled on debug build only.
 */
#ifdef ENABLE_PLOG_TRACE

#define PLOG_DBG_ERROR(msg) ((void) (PLogError msg))
/**
 * Usage: PLOG_DBG_TRACE((printf-arguments))
 *
 * The reason we require double brackets is to allow the use of printf-style variable
 * argument listings in a macro.
 */
#define PLOG_DBG_TRACE(args) ((void) (PLogMessage args))
#define PLOG_DBG_BLOCK(block) block
#define PLOG_DBG_API_ENTER() \
  do \
  { \
    LCHAR text[P_MAX_FUNCTION_NAME]; \
    size_t len = P_MAX_FUNCTION_NAME; \
    ESR_ReturnCode rc; \
    \
    rc = PStackTraceGetFunctionName(text, &len); \
    if (rc==ESR_SUCCESS) \
      PLogMessage(L("%s entered."), text); \
    else if (rc!=ESR_NOT_SUPPORTED) \
      pfprintf(PSTDERR, L("[%s:%d] PStackTraceGetValue failed with %s\n"), __FILE__, __LINE__, ESR_rc2str(rc)); \
  } while (0)

#define PLOG_DBG_API_EXIT(rc) \
  \
  do \
  { \
    LCHAR text[P_MAX_FUNCTION_NAME]; \
    size_t len = P_MAX_FUNCTION_NAME; \
    ESR_ReturnCode rc2; \
    \
    rc2 = PStackTraceGetFunctionName(text, &len); \
    if (rc2==ESR_SUCCESS) \
      PLogMessage(L("%s returned %s"), text, ESR_rc2str(rc)); \
    else if (rc!=ESR_NOT_SUPPORTED) \
      pfprintf(PSTDERR, "[%s:%d] PStackTraceGetValue failed with %s\n", __FILE__, __LINE__, ESR_rc2str(rc2)); \
  } while (0)

#else

#ifndef DOXYGEN_SHOULD_SKIP_THIS
#define PLOG_DBG_ERROR(msg) ((void) 0)
#define PLOG_DBG_MODULE(name, logLevel)
#define PLOG_DBG_TRACE(args) ((void) 0)
#define PLOG_DBG_BLOCK(block)
#define PLOG_DBG_API_ENTER() ((void) 0)
#define PLOG_DBG_API_EXIT(rc) ((void) 0)
#endif /* DOXYGEN_SHOULD_SKIP_THIS */
#endif /* ENABLE_PLOG_TRACE */

/**
 * @}
 */


#endif 
