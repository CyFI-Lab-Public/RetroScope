###################################
# Build the libchromium_net library

LOCAL_PATH := $(call my-dir)
include external/chromium/third_party/libevent/Android.mk
include external/chromium/third_party/modp_b64/Android.mk
include external/chromium/base/third_party/dmg_fp/Android.mk

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc

LOCAL_MODULE := libchromium_net
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
INTERMEDIATES := $(call local-intermediates-dir)

LOCAL_SRC_FILES := \
    googleurl/src/gurl.cc \
    googleurl/src/url_canon_etc.cc \
    googleurl/src/url_canon_fileurl.cc \
    googleurl/src/url_canon_host.cc \
    googleurl/src/url_canon_icu.cc \
    googleurl/src/url_canon_internal.cc \
    googleurl/src/url_canon_ip.cc \
    googleurl/src/url_canon_mailtourl.cc \
    googleurl/src/url_canon_path.cc \
    googleurl/src/url_canon_pathurl.cc \
    googleurl/src/url_canon_query.cc \
    googleurl/src/url_canon_relative.cc \
    googleurl/src/url_canon_stdurl.cc \
    googleurl/src/url_parse.cc \
    googleurl/src/url_parse_file.cc \
    googleurl/src/url_util.cc \
    \
    android/content/common/url_constants.cc \
    android/execinfo.cc \
    android/jni/autofill_request_url.cc \
    android/jni/mime_utils.cc \
    android/jni/jni_utils.cc \
    android/jni/platform_file_jni.cc \
    android/net/android_network_library_impl.cc \
    android/ui/base/l10n/l10n_util.cc \
    \
    app/sql/connection.cc \
    app/sql/meta_table.cc \
    app/sql/statement.cc \
    app/sql/transaction.cc \

ifeq ($(TARGET_ARCH),x86)
LOCAL_SRC_FILES += \
    base/atomicops_internals_x86_gcc.cc
endif

LOCAL_SRC_FILES += \
    base/at_exit.cc \
    base/base64.cc \
    base/environment.cc \
    base/file_descriptor_shuffle.cc \
    base/file_path.cc \
    base/file_util.cc \
    base/file_util_posix.cc \
    base/lazy_instance.cc \
    base/logging.cc \
    base/message_loop.cc \
    base/message_loop_proxy.cc \
    base/message_loop_proxy_impl.cc \
    base/message_pump.cc \
    base/message_pump_default.cc \
    base/message_pump_libevent.cc \
    base/md5.cc \
    base/native_library_linux.cc \
    base/pickle.cc \
    base/platform_file.cc \
    base/platform_file_posix.cc \
    base/process_posix.cc \
    base/process_util.cc \
    base/process_util_linux.cc \
    base/process_util_posix.cc \
    base/rand_util.cc \
    base/rand_util_posix.cc \
    base/safe_strerror_posix.cc \
    base/sha1_portable.cc \
    base/shared_memory_posix.cc \
    base/string_number_conversions.cc \
    base/string_piece.cc \
    base/string_split.cc \
    base/string_util.cc \
    base/string16.cc \
    base/stringprintf.cc \
    base/sys_info_linux.cc \
    base/sys_info_posix.cc \
    base/sys_string_conversions_linux.cc \
    base/task.cc \
    base/time.cc \
    base/time_posix.cc \
    base/timer.cc \
    base/tracked.cc \
    base/tracked_objects.cc \
    base/utf_offset_string_conversions.cc \
    base/utf_string_conversions.cc \
    base/utf_string_conversion_utils.cc \
    base/values.cc \
    base/vlog.cc \
    \
    base/debug/debugger_posix.cc \
    base/debug/stack_trace.cc \
    base/debug/stack_trace_posix.cc \
    \
    base/i18n/file_util_icu.cc \
    base/i18n/icu_string_conversions.cc \
    base/i18n/time_formatting.cc \
    \
    base/json/json_reader.cc \
    base/json/json_writer.cc \
    base/json/string_escape.cc \
    \
    base/memory/ref_counted.cc \
    base/memory/weak_ptr.cc \
    \
    base/metrics/field_trial.cc \
    base/metrics/histogram.cc \
    base/metrics/stats_counters.cc \
    base/metrics/stats_table.cc \
    \
    base/synchronization/cancellation_flag.cc \
    base/synchronization/condition_variable_posix.cc \
    base/synchronization/lock_impl_posix.cc \
    base/synchronization/waitable_event_posix.cc \
    \
    base/threading/platform_thread_posix.cc \
    base/threading/thread.cc \
    base/threading/thread_checker_impl.cc \
    base/threading/thread_collision_warner.cc \
    base/threading/thread_local_posix.cc \
    base/threading/thread_local_storage_posix.cc \
    base/threading/worker_pool_posix.cc \
    \
    base/third_party/icu/icu_utf.cc \
    \
    base/third_party/nspr/prtime.cc \
    \
    chrome/browser/net/sqlite_persistent_cookie_store.cc \
    \
    crypto/openssl_util.cc \
    crypto/secure_hash_default.cc \
    crypto/sha2.cc \
    \
    crypto/third_party/nss/sha512.cc \
    \
    net/base/address_list.cc \
    net/base/address_list_net_log_param.cc \
    net/base/android_network_library.cc \
    net/base/auth.cc \
    net/base/backoff_entry.cc \
    net/base/bandwidth_metrics.cc \
    net/base/capturing_net_log.cc \
    net/base/cert_database.cc \
    net/base/cert_status_flags.cc \
    net/base/cert_verifier.cc \
    net/base/cert_verify_result.cc \
    net/base/connection_type_histograms.cc \
    net/base/cookie_monster.cc \
    net/base/cookie_store.cc \
    net/base/data_url.cc \
    net/base/directory_lister.cc \
    net/base/dns_util.cc \
    net/base/dnsrr_resolver.cc \
    net/base/escape.cc \
    net/base/file_stream_posix.cc \
    net/base/filter.cc \
    net/base/gzip_filter.cc \
    net/base/gzip_header.cc \
    net/base/host_cache.cc \
    net/base/host_mapping_rules.cc \
    net/base/host_port_pair.cc \
    net/base/host_resolver.cc \
    net/base/host_resolver_impl.cc \
    net/base/host_resolver_proc.cc \
    net/base/io_buffer.cc \
    net/base/ip_endpoint.cc \
    net/base/mime_util.cc \
    net/base/net_errors.cc \
    net/base/net_errors_posix.cc \
    net/base/net_log.cc \
    net/base/net_module.cc \
    net/base/net_util.cc \
    net/base/net_util_posix.cc \
    net/base/network_change_notifier.cc \
    net/base/network_change_notifier_linux.cc \
    net/base/network_change_notifier_netlink_linux.cc \
    net/base/network_delegate.cc \
    net/base/openssl_memory_private_key_store.cc \
    net/base/pem_tokenizer.cc \
    net/base/platform_mime_util_android.cc \
    net/base/registry_controlled_domain.cc \
    net/base/sdch_manager.cc \
    net/base/sdch_filter.cc \
    net/base/ssl_cert_request_info.cc \
    net/base/ssl_client_auth_cache.cc \
    net/base/ssl_config_service.cc \
    net/base/ssl_config_service_defaults.cc \
    net/base/ssl_info.cc \
    net/base/transport_security_state.cc \
    net/base/upload_data.cc \
    net/base/upload_data_stream.cc \
    net/base/x509_cert_types.cc \
    net/base/x509_certificate.cc \
    net/base/x509_certificate_openssl.cc \
    net/base/x509_certificate_openssl_android.cc \
    net/base/x509_openssl_util.cc \
    \
    net/disk_cache/addr.cc \
    net/disk_cache/backend_impl.cc \
    net/disk_cache/bitmap.cc \
    net/disk_cache/block_files.cc \
    net/disk_cache/cache_util_posix.cc \
    net/disk_cache/disk_format.cc \
    net/disk_cache/entry_impl.cc \
    net/disk_cache/eviction.cc \
    net/disk_cache/file.cc \
    net/disk_cache/file_lock.cc \
    net/disk_cache/file_posix.cc \
    net/disk_cache/hash.cc \
    net/disk_cache/in_flight_backend_io.cc \
    net/disk_cache/in_flight_io.cc \
    net/disk_cache/mapped_file_posix.cc \
    net/disk_cache/mem_backend_impl.cc \
    net/disk_cache/mem_entry_impl.cc \
    net/disk_cache/mem_rankings.cc \
    net/disk_cache/net_log_parameters.cc \
    net/disk_cache/rankings.cc \
    net/disk_cache/stats.cc \
    net/disk_cache/stats_histogram.cc \
    net/disk_cache/sparse_control.cc \
    net/disk_cache/trace.cc \
    \
    net/ftp/ftp_auth_cache.cc \
    \
    net/http/des.cc \
    net/http/disk_cache_based_ssl_host_info.cc \
    net/http/http_alternate_protocols.cc \
    net/http/http_auth.cc \
    net/http/http_auth_cache.cc \
    net/http/http_auth_controller.cc \
    net/http/http_auth_gssapi_posix.cc \
    net/http/http_auth_handler.cc \
    net/http/http_auth_handler_basic.cc \
    net/http/http_auth_handler_digest.cc \
    net/http/http_auth_handler_factory.cc \
    net/http/http_auth_handler_negotiate.cc \
    net/http/http_auth_handler_ntlm.cc \
    net/http/http_auth_handler_ntlm_portable.cc \
    net/http/http_basic_stream.cc \
    net/http/http_byte_range.cc \
    net/http/http_cache.cc \
    net/http/http_cache_transaction.cc \
    net/http/http_chunked_decoder.cc \
    net/http/http_net_log_params.cc \
    net/http/http_network_layer.cc \
    net/http/http_network_session.cc \
    net/http/http_network_transaction.cc \
    net/http/http_proxy_client_socket.cc \
    net/http/http_proxy_client_socket_pool.cc \
    net/http/http_proxy_utils.cc \
    net/http/http_request_headers.cc \
    net/http/http_request_info.cc \
    net/http/http_response_body_drainer.cc \
    net/http/http_response_headers.cc \
    net/http/http_response_info.cc \
    net/http/http_stream_factory.cc \
    net/http/http_stream_factory_impl.cc \
    net/http/http_stream_factory_impl_job.cc \
    net/http/http_stream_factory_impl_request.cc \
    net/http/http_stream_parser.cc \
    net/http/http_util.cc \
    net/http/http_util_icu.cc \
    net/http/http_vary_data.cc \
    net/http/md4.cc \
    net/http/partial_data.cc \
    \
    net/proxy/init_proxy_resolver.cc \
    net/proxy/multi_threaded_proxy_resolver.cc \
    net/proxy/proxy_bypass_rules.cc \
    net/proxy/proxy_config.cc \
    net/proxy/proxy_config_service_android.cc \
    net/proxy/proxy_config_service_fixed.cc \
    net/proxy/proxy_info.cc \
    net/proxy/proxy_list.cc \
    net/proxy/proxy_resolver_js_bindings.cc \
    net/proxy/proxy_resolver_script_data.cc \
    net/proxy/proxy_server.cc \
    net/proxy/proxy_service.cc \
    net/proxy/sync_host_resolver_bridge.cc \
    \
    net/socket/client_socket.cc \
    net/socket/client_socket_handle.cc \
    net/socket/client_socket_factory.cc \
    net/socket/client_socket_pool.cc \
    net/socket/client_socket_pool_base.cc \
    net/socket/client_socket_pool_histograms.cc \
    net/socket/client_socket_pool_manager.cc \
    net/socket/socks_client_socket.cc \
    net/socket/socks_client_socket_pool.cc \
    net/socket/socks5_client_socket.cc \
    net/socket/ssl_client_socket.cc \
    net/socket/ssl_client_socket_openssl.cc \
    net/socket/ssl_client_socket_pool.cc \
    net/socket/ssl_error_params.cc \
    net/socket/ssl_host_info.cc \
    net/socket/tcp_client_socket.cc \
    net/socket/tcp_client_socket_libevent.cc \
    net/socket/transport_client_socket_pool.cc \
    \
    net/spdy/spdy_framer.cc \
    net/spdy/spdy_frame_builder.cc \
    net/spdy/spdy_http_stream.cc \
    net/spdy/spdy_http_utils.cc \
    net/spdy/spdy_io_buffer.cc \
    net/spdy/spdy_proxy_client_socket.cc \
    net/spdy/spdy_session.cc \
    net/spdy/spdy_session_pool.cc \
    net/spdy/spdy_settings_storage.cc \
    net/spdy/spdy_stream.cc \
    \
    net/url_request/https_prober.cc \
    net/url_request/url_request.cc \
    net/url_request/url_request_context.cc \
    net/url_request/url_request_context_getter.cc \
    net/url_request/url_request_file_job.cc \
    net/url_request/url_request_file_dir_job.cc \
    net/url_request/url_request_http_job.cc \
    net/url_request/url_request_error_job.cc \
    net/url_request/url_request_job.cc \
    net/url_request/url_request_job_manager.cc \
    net/url_request/url_request_job_tracker.cc \
    net/url_request/url_request_netlog_params.cc \
    net/url_request/url_request_redirect_job.cc \
    net/url_request/url_request_throttler_entry.cc \
    net/url_request/url_request_throttler_header_adapter.cc \
    net/url_request/url_request_throttler_manager.cc \
    \
    sdch/open-vcdiff/src/addrcache.cc \
    sdch/open-vcdiff/src/blockhash.cc \
    sdch/open-vcdiff/src/codetable.cc \
    sdch/open-vcdiff/src/encodetable.cc \
    sdch/open-vcdiff/src/decodetable.cc \
    sdch/open-vcdiff/src/headerparser.cc \
    sdch/open-vcdiff/src/instruction_map.cc \
    sdch/open-vcdiff/src/logging.cc \
    sdch/open-vcdiff/src/varint_bigendian.cc \
    sdch/open-vcdiff/src/vcdecoder.cc \
    sdch/open-vcdiff/src/vcdiffengine.cc \
    sdch/open-vcdiff/src/vcencoder.cc \
    \
    ui/gfx/point.cc \

# AutoFill++ source files.
LOCAL_SRC_FILES += \
    android/autofill/android_url_request_context_getter.cc \
    android/autofill/profile_android.cc \
    android/autofill/url_fetcher_proxy.cc \
    \
    base/base_paths.cc \
    base/base_paths_linux.cc \
    base/path_service.cc \
    \
    chrome/browser/autofill/address.cc \
    chrome/browser/autofill/address_field.cc \
    chrome/browser/autofill/autofill_country.cc \
    chrome/browser/autofill/autofill_download.cc \
    chrome/browser/autofill/autofill_field.cc \
    chrome/browser/autofill/autofill_manager.cc \
    chrome/browser/autofill/autofill_metrics.cc \
    chrome/browser/autofill/autofill_profile.cc \
    chrome/browser/autofill/autofill_type.cc \
    chrome/browser/autofill/autofill_xml_parser.cc \
    chrome/browser/autofill/contact_info.cc \
    chrome/browser/autofill/credit_card.cc \
    chrome/browser/autofill/credit_card_field.cc \
    chrome/browser/autofill/fax_number.cc \
    chrome/browser/autofill/form_field.cc \
    chrome/browser/autofill/form_group.cc \
    chrome/browser/autofill/form_structure.cc \
    chrome/browser/autofill/name_field.cc \
    chrome/browser/autofill/home_phone_number.cc \
    chrome/browser/autofill/personal_data_manager.cc \
    chrome/browser/autofill/phone_field.cc \
    chrome/browser/autofill/phone_number.cc \
    chrome/browser/autofill/select_control_handler.cc \
    \
    chrome/common/guid.cc \
    chrome/common/guid_posix.cc \
    chrome/common/url_constants.cc \
    \
    chrome/common/net/url_fetcher.cc \
    chrome/common/net/url_fetcher_protect.cc \
    \
    third_party/libjingle/overrides/talk/xmllite/qname.cc \
    third_party/libjingle/source/talk/xmllite/xmlbuilder.cc \
    third_party/libjingle/source/talk/xmllite/xmlconstants.cc \
    third_party/libjingle/source/talk/xmllite/xmlelement.cc \
    third_party/libjingle/source/talk/xmllite/xmlnsstack.cc \
    third_party/libjingle/source/talk/xmllite/xmlparser.cc \
    third_party/libjingle/source/talk/xmllite/xmlprinter.cc \
    \
    webkit/glue/form_data.cc \
    webkit/glue/form_field.cc

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH) \
    $(LOCAL_PATH)/chrome \
    $(LOCAL_PATH)/chrome/browser \
    $(LOCAL_PATH)/sdch/open-vcdiff/src \
    $(LOCAL_PATH)/third_party/libevent/compat \
    external/expat \
    external/icu4c/common \
    external/icu4c/i18n \
    external/openssl/include \
    external/skia \
    external/sqlite/dist \
    external/webkit/Source/WebKit/chromium \
    external/zlib \
    external \
    $(LOCAL_PATH)/base/third_party/dmg_fp \
    $(LOCAL_PATH)/third_party/icu/public/common \
    $(LOCAL_PATH)/third_party/libevent/android \
    $(LOCAL_PATH)/third_party/libevent \
    $(LOCAL_PATH)/third_party/libjingle/overrides \
    $(LOCAL_PATH)/third_party/libjingle/source \
    vendor/google/libraries/autofill

# Chromium uses several third party libraries and headers that are already
# present on Android, but in different include paths. Generate a set of
# forwarding headers in the location that Chromium expects.

THIRD_PARTY = $(INTERMEDIATES)/third_party
SCRIPT := $(LOCAL_PATH)/android/generateAndroidForwardingHeader.pl

GEN := $(THIRD_PARTY)/expat/files/lib/expat.h
$(GEN): $(SCRIPT)
$(GEN):
	perl $(SCRIPT) $@ "lib/expat.h"
LOCAL_GENERATED_SOURCES += $(GEN)

GEN := $(THIRD_PARTY)/skia/include/core/SkBitmap.h
$(GEN): $(SCRIPT)
$(GEN):
	perl $(SCRIPT) $@ "include/core/SkBitmap.h"
LOCAL_GENERATED_SOURCES += $(GEN)

GEN := $(THIRD_PARTY)/WebKit/Source/WebKit/chromium/public/WebFormControlElement.h
$(GEN): $(SCRIPT)
$(GEN):
	perl $(SCRIPT) $@ "public/WebFormControlElement.h"
LOCAL_GENERATED_SOURCES += $(GEN)

GEN := $(THIRD_PARTY)/WebKit/Source/WebKit/chromium/public/WebRegularExpression.h
$(GEN): $(SCRIPT)
$(GEN):
	perl $(SCRIPT) $@ "public/WebRegularExpression.h"
LOCAL_GENERATED_SOURCES += $(GEN)

GEN := $(THIRD_PARTY)/WebKit/Source/WebKit/chromium/public/WebString.h
$(GEN): $(SCRIPT)
$(GEN):
	perl $(SCRIPT) $@ "public/WebString.h"
LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DANDROID -DEXPAT_RELATIVE_PATH -DALLOW_QUOTED_COOKIE_VALUES -DCOMPONENT_BUILD -DGURL_DLL
LOCAL_CPPFLAGS := -Wno-sign-promo -Wno-missing-field-initializers -fvisibility=hidden -fvisibility-inlines-hidden

# Just a few definitions not provided by bionic.
LOCAL_CFLAGS += -include "android/prefix.h"

# external/chromium/android is a directory to intercept stl headers that we do
# not support yet.
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/android \
	$(LOCAL_C_INCLUDES)

LOCAL_STATIC_LIBRARIES := libevent modp_b64 dmg_fp
LOCAL_SHARED_LIBRARIES := libstlport libexpat libcrypto libssl libz libicuuc libicui18n libsqlite libcutils liblog libdl

LOCAL_PRELINK_MODULE := false

# Including this will modify the include path
include external/stlport/libstlport.mk

ifneq ($(strip $(WITH_ADDRESS_SANITIZER)),)
    LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/asan
    LOCAL_ADDRESS_SANITIZER := true
endif

include $(BUILD_SHARED_LIBRARY)
