/**
 * @file opagent.h
 * Interface to report symbol names and dynamically generated code to Oprofile
 *
 * @remark Copyright 2007 OProfile authors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @author Jens Wilke
 * @Modifications Daniel Hansel
 *
 * Copyright IBM Corporation 2007
 *
 */

#ifndef _LIB_OPAGENT_H
#define _LIB_OPAGENT_H

#include <sys/types.h>

#if defined(__cplusplus)
extern "C" {
#endif
  
struct debug_line_info {
	unsigned long vma;
	unsigned int lineno;
	/* The filename format is unspecified, absolute path, relative etc. */
	char const * filename;
};

typedef void * op_agent_t;

/**
 * This function must be called by agents before any other function.
 * Creates and opens a JIT dump file in /var/lib/oprofile/jitdump
 * using the naming convention <process_id>.dump.
 *
 * Returns a valid op_agent_t handle or NULL.  If NULL is returned, errno
 * is set to indicate the nature of the error.
 **/
op_agent_t op_open_agent(void);

/**
 * Frees all resources and closes open file handles.
 *
 * hdl:         Handle returned from an earlier call to op_open_agent()
 *
 * Returns 0 on success; -1 otherwise.  If -1 is returned, errno is
 * set to indicate the nature of the error.
 **/
int op_close_agent(op_agent_t hdl);

/**
 * Signal the dynamic generation of native code from a virtual machine.
 * Writes a JIT dump record to the open JIT dump file using
 * the passed information.
 *
 * hdl:         Handle returned from an earlier call to op_open_agent()
 * symbol_name: The name of the symbol being dynamically compiled.
 *              This name can (and should) contain all necessary
 *              information to disambiguate it from symbols of the
 *              same name; e.g., class, method signature.
 * vma:         The virtual memory address of the executable code.
 * code:        Pointer to the location of the compiled code.
 *		Theoretically, this may be a different location from
 *		that given by the vma argument.  For some JIT compilers,
 *		obtaining the code may be impractical.  For this (or any other)
 *		reason, the agent can choose to pass NULL for this paraemter.
 *		If NULL is passed, no code will be copied into the JIT dump
 *		file.
 * code_size:   Size of the compiled code.
 *
 * Returns 0 on success; -1 otherwise.  If -1 is returned, errno is
 * set to indicate the nature of the error.
 **/
int op_write_native_code(op_agent_t hdl, char const * symbol_name,
			 uint64_t vma, void const * code,
			 const unsigned int code_size);

/**
 * Add debug line information to a piece of code. An op_write_native_code()
 * with the same code pointer should have occurred before this call. It's not
 * necessary to provide one lineno information entry per machine instruction;
 * the array can contain hole.
 *
 * hdl:         Handle returned from an earlier call to op_open_agent()
 * code:        Pointer to the location of the code with debug info
 * nr_entry:    Number of entries in compile_map
 * compile_map: Array of struct debug_line_info.  See the JVMTI agent
 *              library implementation for an example of what information
 *              should be retrieved from a VM to fill out this data structure.
 *
 * Returns 0 on success; -1 otherwise.  If -1 is returned, errno is
 * set to indicate the nature of the error.
 **/
int op_write_debug_line_info(op_agent_t hdl, void const * code,
			     size_t nr_entry,
			     struct debug_line_info const * compile_map);

/**
 * Signal the invalidation of native code from a virtual machine.
 *
 * hdl:         Handle returned from an earlier call to op_open_agent()
 * vma:         The virtual memory address of the compiled code being unloaded.
 *              An op_write_native_code() with the same vma should have
 *              occurred before this call.
 *
 * Returns 0 on success; -1 otherwise.  If -1 is returned, errno is
 * set to indicate the nature of the error.
 **/
int op_unload_native_code(op_agent_t hdl, uint64_t vma);

/**
 * Returns the major version number of the libopagent library that will be used.
 **/
int op_major_version(void);

/**
 * Returns the minor version number of the libopagent library that will be used.
 **/
int op_minor_version(void);

/* idea how to post additional information for a piece of code.
   we use the code address as reference
int op_write_loader_name(const void* code_addr, char const * loader_name);
*/

#if defined(__cplusplus)
}
#endif

#endif
