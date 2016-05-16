/**
 * @file jvmti_oprofile.c
 * JVMTI agent implementation to report jitted JVM code to Oprofile
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

#include <stdio.h>
#include <jvmti.h>
#include <string.h>
#include <stdint.h>
#include <stdlib.h>
#include <errno.h>

#include "opagent.h"

static int debug = 0;
static int can_get_line_numbers = 0;
static op_agent_t agent_hdl;

/**
 * Handle an error or a warning, return 0 if the checked error is 
 * JVMTI_ERROR_NONE, i.e. success
 */
static int handle_error(jvmtiError err, char const * msg, int severe)
{
	if (err != JVMTI_ERROR_NONE) {
		fprintf(stderr, "%s: %s, err code %i\n",
			severe ? "Error" : "Warning", msg, err);
	}
	return err != JVMTI_ERROR_NONE;
}


/**
 * returned array is map_length length, params map and map_length != 0
 * format of lineno information is JVMTI_JLOCATION_JVMBCI, map is an array
 * of { address, code byte index }, table_ptr an array of { byte code index,
 * lineno }
 */
static struct debug_line_info * 
create_debug_line_info(jint map_length, jvmtiAddrLocationMap const * map,
		       jint entry_count, jvmtiLineNumberEntry* table_ptr,
		       char const * source_filename)
{
	struct debug_line_info * debug_line;
	int i, j;
	if (debug) {
		fprintf(stderr, "Source %s\n", source_filename);
		for (i = 0; i < map_length; ++i) {
			fprintf(stderr, "%p %lld\t",
			        map[i].start_address,
			        (long long)map[i].location);
		}
		fprintf(stderr, "\n");
		for (i = 0; i < entry_count; ++i) {
			fprintf(stderr, "%lld %d\t",
				(long long)table_ptr[i].start_location,
				table_ptr[i].line_number);
		}
		fprintf(stderr, "\n");
	}

	debug_line = calloc(map_length, sizeof(struct debug_line_info));
	if (!debug_line)
		return 0;

	for (i = 0; i < map_length; ++i) {
		/* FIXME: likely to need a lower_bound on the array, but
		 * documentation is a bit obscure about the contents of these
		 * arrray
		 **/
		for (j = 0; j < entry_count - 1; ++j) {
			if (table_ptr[j].start_location > map[i].location)
				break;
		}
		debug_line[i].vma = (unsigned long)map[i].start_address;
		debug_line[i].lineno = table_ptr[j].line_number;
		debug_line[i].filename = source_filename;
	}

	if (debug) {
		for (i = 0; i < map_length; ++i) {
			fprintf(stderr, "%lx %d\t", debug_line[i].vma,
				debug_line[i].lineno);
		}
		fprintf(stderr, "\n");
	}
	
	return debug_line;
}


static void JNICALL cb_compiled_method_load(jvmtiEnv * jvmti,
	jmethodID method, jint code_size, void const * code_addr,
	jint map_length, jvmtiAddrLocationMap const * map,
	void const * compile_info)
{
	jclass declaring_class;
	char * class_signature = NULL;
 	char * method_name = NULL;
 	char * method_signature = NULL;
	jvmtiLineNumberEntry* table_ptr = NULL;
	char * source_filename = NULL;
	struct debug_line_info * debug_line = NULL;
 	jvmtiError err;

	/* shut up compiler warning */
	compile_info = compile_info;

	err = (*jvmti)->GetMethodDeclaringClass(jvmti, method,
						&declaring_class);
	if (handle_error(err, "GetMethodDeclaringClass()", 1))
		goto cleanup2;

	if (can_get_line_numbers && map_length && map) {
		jint entry_count;

		err = (*jvmti)->GetLineNumberTable(jvmti, method,
						   &entry_count, &table_ptr);
		if (err == JVMTI_ERROR_NONE) {
			err = (*jvmti)->GetSourceFileName(jvmti,
				declaring_class, &source_filename);
			if (err ==  JVMTI_ERROR_NONE) {
				debug_line =
					create_debug_line_info(map_length, map,
						entry_count, table_ptr,
						source_filename);
			} else if (err != JVMTI_ERROR_ABSENT_INFORMATION) {
				handle_error(err, "GetSourceFileName()", 1);
			}
		} else if (err != JVMTI_ERROR_NATIVE_METHOD &&
			   err != JVMTI_ERROR_ABSENT_INFORMATION) {
			handle_error(err, "GetLineNumberTable()", 1);
		}
	}

	err = (*jvmti)->GetClassSignature(jvmti, declaring_class,
					  &class_signature, NULL);
	if (handle_error(err, "GetClassSignature()", 1))
		goto cleanup1;

	err = (*jvmti)->GetMethodName(jvmti, method, &method_name,
				      &method_signature, NULL);
	if (handle_error(err, "GetMethodName()", 1))
		goto cleanup;

	if (debug) {
		fprintf(stderr, "load: declaring_class=%p, class=%s, "
			"method=%s, signature=%s, addr=%p, size=%i \n",
			declaring_class, class_signature, method_name,
			method_signature, code_addr, code_size);
	}

	{
	int cnt = strlen(method_name) + strlen(class_signature) +
		strlen(method_signature) + 2;
	char buf[cnt];
	strncpy(buf, class_signature, cnt - 1);
	strncat(buf, method_name, cnt - strlen(buf) - 1);
	strncat(buf, method_signature, cnt - strlen(buf) - 1);
	if (op_write_native_code(agent_hdl, buf,
				 (uint64_t)(uintptr_t) code_addr,
				 code_addr, code_size)) {
		perror("Error: op_write_native_code()");
		goto cleanup;
	}
	}

	if (debug_line)
		if (op_write_debug_line_info(agent_hdl, code_addr, map_length,
					     debug_line))
			perror("Error: op_write_debug_line_info()");

cleanup:
	(*jvmti)->Deallocate(jvmti, (unsigned char *)method_name);
	(*jvmti)->Deallocate(jvmti, (unsigned char *)method_signature);
cleanup1:
	(*jvmti)->Deallocate(jvmti, (unsigned char *)class_signature);
	(*jvmti)->Deallocate(jvmti, (unsigned char *)table_ptr);
	(*jvmti)->Deallocate(jvmti, (unsigned char *)source_filename);
cleanup2:
	free(debug_line);
}


static void JNICALL cb_compiled_method_unload(jvmtiEnv * jvmti_env,
	jmethodID method, void const * code_addr)
{
	/* shut up compiler warning */
	jvmti_env = jvmti_env;
	method = method;

	if (debug)
		fprintf(stderr, "unload: addr=%p\n", code_addr);
	if (op_unload_native_code(agent_hdl, (uint64_t)(uintptr_t) code_addr))
		perror("Error: op_unload_native_code()");
}


static void JNICALL cb_dynamic_code_generated(jvmtiEnv * jvmti_env,
	char const * name, void const * code_addr, jint code_size)
{
	/* shut up compiler warning */
	jvmti_env = jvmti_env;
	if (debug) {
		fprintf(stderr, "dyncode: name=%s, addr=%p, size=%i \n",
			name, code_addr, code_size);
	}
	if (op_write_native_code(agent_hdl, name,
				 (uint64_t)(uintptr_t) code_addr,
				 code_addr, code_size))
		perror("Error: op_write_native_code()");
}


JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM * jvm, char * options, void * reserved)
{
	jint rc;
	jvmtiEnv * jvmti = NULL;
	jvmtiEventCallbacks callbacks;
	jvmtiCapabilities caps;
	jvmtiJlocationFormat format;
	jvmtiError error;

	/* shut up compiler warning */
	reserved = reserved;

	if (options && !strcmp("version", options)) {
		fprintf(stderr, "jvmti_oprofile: current libopagent version %i.%i.\n",
		        op_major_version(), op_minor_version());
		return -1;
	}

	if (options && !strcmp("debug", options))
		debug = 1;

	if (debug)
		fprintf(stderr, "jvmti_oprofile: agent activated\n");

	agent_hdl = op_open_agent();
	if (!agent_hdl) {
		perror("Error: op_open_agent()");
		return -1;
	}

	rc = (*jvm)->GetEnv(jvm, (void *)&jvmti, JVMTI_VERSION_1);
	if (rc != JNI_OK) {
		fprintf(stderr, "Error: GetEnv(), rc=%i\n", rc);
		return -1;
	}

	memset(&caps, '\0', sizeof(caps));
	caps.can_generate_compiled_method_load_events = 1;
	error = (*jvmti)->AddCapabilities(jvmti, &caps);
	if (handle_error(error, "AddCapabilities()", 1))
		return -1;

	/* FIXME: settable through command line, default on/off? */
	error = (*jvmti)->GetJLocationFormat(jvmti, &format);
	if (!handle_error(error, "GetJLocationFormat", 1) &&
	    format == JVMTI_JLOCATION_JVMBCI) {
		memset(&caps, '\0', sizeof(caps));
		caps.can_get_line_numbers = 1;
		caps.can_get_source_file_name = 1;
		error = (*jvmti)->AddCapabilities(jvmti, &caps);
		if (!handle_error(error, "AddCapabilities()", 1))
			can_get_line_numbers = 1;
	}

	memset(&callbacks, 0, sizeof(callbacks));
	callbacks.CompiledMethodLoad = cb_compiled_method_load;
	callbacks.CompiledMethodUnload = cb_compiled_method_unload;
	callbacks.DynamicCodeGenerated = cb_dynamic_code_generated;
	error = (*jvmti)->SetEventCallbacks(jvmti, &callbacks,
					    sizeof(callbacks));
	if (handle_error(error, "SetEventCallbacks()", 1))
		return -1;

	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
			JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
	if (handle_error(error, "SetEventNotificationMode() "
			 "JVMTI_EVENT_COMPILED_METHOD_LOAD", 1))
		return -1;
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
			JVMTI_EVENT_COMPILED_METHOD_UNLOAD, NULL);
	if (handle_error(error, "SetEventNotificationMode() "
			 "JVMTI_EVENT_COMPILED_METHOD_UNLOAD", 1))
		return -1;
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
			JVMTI_EVENT_DYNAMIC_CODE_GENERATED, NULL);
	if (handle_error(error, "SetEventNotificationMode() "
			 "JVMTI_EVENT_DYNAMIC_CODE_GENERATED", 1))
		return -1;
	return 0;
}


JNIEXPORT void JNICALL Agent_OnUnload(JavaVM * jvm)
{
	/* shut up compiler warning */
	jvm = jvm;
	if (op_close_agent(agent_hdl))
		perror("Error: op_close_agent()");
}
