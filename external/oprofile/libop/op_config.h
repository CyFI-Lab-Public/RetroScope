/**
 * @file op_config.h
 *
 * Parameters a user may want to change. See
 * also op_config_24.h
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * @Modifications Daniel Hansel
 */

#ifndef OP_CONFIG_H
#define OP_CONFIG_H

#if defined(__cplusplus)
extern "C" {
#endif
  
/** 
 * must be called to initialize the paths below.
 * @param session_dir  the non-NULL value of the base session directory
 */
void init_op_config_dirs(char const * session_dir);

#if !defined(ANDROID) || defined(ANDROID_HOST)
#define OP_SESSION_DIR_DEFAULT "/var/lib/oprofile/"
#endif

/* 
 * various paths, corresponding to opcontrol, that should be
 * initialized by init_op_config_dirs() above. 
 */
extern char op_session_dir[];
extern char op_samples_dir[];
extern char op_samples_current_dir[];
extern char op_lock_file[];
extern char op_log_file[];
extern char op_pipe_file[];
extern char op_dump_status[];

#if ANDROID
#define OP_DRIVER_BASE  "/dev/oprofile"
#define OP_DATA_DIR     "/data/oprofile"
#endif

/* Global directory that stores debug files */
#ifndef DEBUGDIR
#define DEBUGDIR "/usr/lib/debug"
#endif

#define OPD_MAGIC "DAE\n"
#define OPD_VERSION 0x11

#define OP_MIN_CPU_BUF_SIZE 2048
#define OP_MAX_CPU_BUF_SIZE 131072

#if defined(__cplusplus)
}
#endif

#endif /* OP_CONFIG_H */
