LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PREBUILT_JAVA_LIBRARIES := \
	org.eclipse.jface_3.4.2.M20090107-0800$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.equinox.common_3.4.0.v20080421-2006$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.core.commands_3.4.0.I20080509-2000$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.jface_3.6.2.M20110210-1200$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.equinox.common_3.6.0.v20100503$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.osgi_3.6.2.R36x_v20110210$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.core.commands_3.6.0.I20100512-1500$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.core.expressions_3.4.200.v20100505$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.core.runtime_3.6.0.v20100505$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.ui.workbench.texteditor_3.6.1.r361_v20100714-0800$(COMMON_JAVA_PACKAGE_SUFFIX) \
	org.eclipse.ui.workbench_3.6.2.M20110210-1200$(COMMON_JAVA_PACKAGE_SUFFIX)

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_PREBUILT)
