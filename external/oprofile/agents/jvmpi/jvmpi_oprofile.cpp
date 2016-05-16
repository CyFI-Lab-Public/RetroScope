/**
 * @file jvmpi_oprofile.cpp
 * JVMPI agent implementation to report jitted JVM code to OProfile
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
 * @author Maynard Johnson
 *
 * Copyright IBM Corporation 2007
 *
 */

#include <iostream>
#include <map>
#include <string>
#include <cstring>
#include <stdexcept>
#include <cerrno>

extern "C" {
#include <stdint.h>
#include <jvmpi.h>
#include <opagent.h>
}

using namespace std;

static bool debug = false;
static op_agent_t agent_hdl;

class class_details {
public:
	string name;
	map<jmethodID, string> method_names;
	map<jmethodID, string> method_signatures;
};


static pthread_mutex_t class_map_mutex = PTHREAD_MUTEX_INITIALIZER;
static map <jobjectID, class_details> loaded_classes;

void class_load(JVMPI_Event * event)
{
	class_details cls;
	cls.name = event->u.class_load.class_name;
	JVMPI_Method * passed_methods = event->u.class_load.methods;
	for (int i = 0; i < event->u.class_load.num_methods;
	     i++, passed_methods++) {
		cls.method_names[passed_methods->method_id] =
			passed_methods->method_name;
		cls.method_signatures[passed_methods->method_id] =
			passed_methods->method_signature;
	}

	pthread_mutex_lock(&class_map_mutex);
	loaded_classes[event->u.class_load.class_id] = cls;
	pthread_mutex_unlock(&class_map_mutex);
}

void class_unload(JVMPI_Event * event)
{
	pthread_mutex_lock(&class_map_mutex);
	loaded_classes.erase(event->u.class_load.class_id);
	pthread_mutex_unlock(&class_map_mutex);
}

JVMPI_Interface * jvmpi;

void compiled_method_load(JVMPI_Event * event)
{
	jmethodID method = event->u.compiled_method_load.method_id;
	void * code_addr =  event->u.compiled_method_load.code_addr;
	jint code_size =  event->u.compiled_method_load.code_size;

	jvmpi->DisableGC();
	 /* Get the class of the method */
	jobjectID classID = jvmpi->GetMethodClass(method);
	jvmpi->EnableGC();

	pthread_mutex_lock(&class_map_mutex);
	map<jobjectID, class_details>::iterator iter =
		loaded_classes.find(classID);
	if (iter == loaded_classes.end()) {
		throw runtime_error("Error: Cannot find class for compiled"
				    " method\n");
	}

	class_details cls_info = ((class_details)iter->second);
	map<jmethodID, string>::iterator method_it =
		cls_info.method_names.find(method);
	if (method_it == cls_info.method_names.end()) {
		throw runtime_error("Error: Cannot find method name for "
				    "compiled method\n");
	}
	char const * method_name = ((string)method_it->second).c_str();
	method_it = cls_info.method_signatures.find(method);
	if (method_it == cls_info.method_signatures.end()) {
		throw runtime_error("Error: Cannot find method signature "
				    "for compiled method\n");
	}
	char const * method_signature = ((string)method_it->second).c_str();

	string const class_signature = "L" + cls_info.name + ";";
	pthread_mutex_unlock(&class_map_mutex);

	if (debug) {
		cerr << "load: class=" << class_signature << ", method ="
		     << method_name << ", method signature = "
		     << method_signature
		     << ", addr=" << code_addr << ", size="
		     << code_size << endl;
	}

	// produce a symbol name out of class name and method name
	int cnt = strlen(method_name) + strlen(class_signature.c_str()) +
		strlen(method_signature) + 2;
	char buf[cnt];
	strncpy(buf, class_signature.c_str(), cnt - 1);
	strncat(buf, method_name, cnt - strlen(buf) - 1);
	strncat(buf, method_signature, cnt - strlen(buf) - 1);
	if (op_write_native_code(agent_hdl, buf, (uint64_t) code_addr,
				 code_addr, code_size))
		perror("Error: op_write_native_code()");
}

void compiled_method_unload(JVMPI_Event * event)
{
	void * code_addr =  event->u.compiled_method_load.code_addr;
	if (debug) {
		cerr << "unload: addr="
			<< (unsigned long long) (uintptr_t) code_addr
			<< endl;
	}
	if (op_unload_native_code(agent_hdl, (uint64_t)code_addr))
		perror("Error: op_unload_native_code()");
}

void jvm_shutdown(JVMPI_Event * event)
{
	/* Checking event here is not really necessary; added only to silence
	 * the 'unused parameter' compiler warning.
	 */
	if (event)
		if (op_close_agent(agent_hdl))
			perror("Error: op_close_agent()");
}


void jvm_notify_event(JVMPI_Event * event)
{
	switch (event->event_type) {
	case JVMPI_EVENT_COMPILED_METHOD_LOAD:
		compiled_method_load(event);
		break;
	case JVMPI_EVENT_COMPILED_METHOD_UNLOAD:
		compiled_method_unload(event);
		break;
	case JVMPI_EVENT_JVM_SHUT_DOWN:
		jvm_shutdown(event);
		break;
	case JVMPI_EVENT_CLASS_LOAD:
		class_load(event);
		break;
	case JVMPI_EVENT_CLASS_UNLOAD:
		class_unload(event);
		break;
	default:
		break;
	}
}

extern "C" {
JNIEXPORT jint JNICALL JVM_OnLoad(JavaVM * jvm, char * options,
                                  void * reserved)
{
	int err;

	if (options && strstr(options, "version")) {
		cerr << "jvmpi_oprofile: current libopagent version "
				 << op_major_version() << "." << op_minor_version()
				 << endl;
		throw runtime_error("Exiting");
	}

	if (options && strstr(options, "debug=yes")) {
		debug = true;
		/* Add something braindead to silence the 'unused parameter'
		 * compiler warning.
		 */
		if (reserved)
			debug = true;
	}

	if (debug)
		cerr << "jvmpi_oprofile: agent activated" << endl;

	agent_hdl = op_open_agent();
	if (!agent_hdl) {
		perror("Error: op_open_agent()");
		throw runtime_error("Exiting");
	}

	/* The union below is used to avoid the 'dereferencing type-punned
	 * pointer will break strict-aliasing rules' compiler warning on the
	 * GetEnv call.
	 */
	union {
		JVMPI_Interface * jvmpi_ifc;
		void * jvmpi_ifc_ptr;
	} jvmpi_GetEnv_arg;
	err = jvm->GetEnv(&jvmpi_GetEnv_arg.jvmpi_ifc_ptr, JVMPI_VERSION_1);
	if (err < 0) {
		cerr << "GetEnv failed with rc=" << err << endl;
		throw runtime_error("Exiting");
	}
	jvmpi = jvmpi_GetEnv_arg.jvmpi_ifc;
	jvmpi->EnableEvent(JVMPI_EVENT_COMPILED_METHOD_LOAD, NULL);
	jvmpi->EnableEvent(JVMPI_EVENT_COMPILED_METHOD_UNLOAD, NULL);
	jvmpi->EnableEvent(JVMPI_EVENT_JVM_SHUT_DOWN, NULL);
	jvmpi->EnableEvent(JVMPI_EVENT_CLASS_LOAD, NULL);

	jvmpi->NotifyEvent = jvm_notify_event;
	return JNI_OK;
}
}
