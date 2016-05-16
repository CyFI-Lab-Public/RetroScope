/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>
#include <fcntl.h>
#include <getopt.h>
#include <signal.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/sendfile.h>
#include <time.h>
#include <zlib.h>

#include <binder/IBinder.h>
#include <binder/IServiceManager.h>
#include <binder/Parcel.h>

#include <cutils/properties.h>

#include <utils/String8.h>
#include <utils/Trace.h>

using namespace android;

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

enum { MAX_SYS_FILES = 8 };

const char* k_traceTagsProperty = "debug.atrace.tags.enableflags";
const char* k_traceAppCmdlineProperty = "debug.atrace.app_cmdlines";

typedef enum { OPT, REQ } requiredness  ;

struct TracingCategory {
    // The name identifying the category.
    const char* name;

    // A longer description of the category.
    const char* longname;

    // The userland tracing tags that the category enables.
    uint64_t tags;

    // The fname==NULL terminated list of /sys/ files that the category
    // enables.
    struct {
        // Whether the file must be writable in order to enable the tracing
        // category.
        requiredness required;

        // The path to the enable file.
        const char* path;
    } sysfiles[MAX_SYS_FILES];
};

/* Tracing categories */
static const TracingCategory k_categories[] = {
    { "gfx",        "Graphics",         ATRACE_TAG_GRAPHICS, { } },
    { "input",      "Input",            ATRACE_TAG_INPUT, { } },
    { "view",       "View System",      ATRACE_TAG_VIEW, { } },
    { "webview",    "WebView",          ATRACE_TAG_WEBVIEW, { } },
    { "wm",         "Window Manager",   ATRACE_TAG_WINDOW_MANAGER, { } },
    { "am",         "Activity Manager", ATRACE_TAG_ACTIVITY_MANAGER, { } },
    { "audio",      "Audio",            ATRACE_TAG_AUDIO, { } },
    { "video",      "Video",            ATRACE_TAG_VIDEO, { } },
    { "camera",     "Camera",           ATRACE_TAG_CAMERA, { } },
    { "hal",        "Hardware Modules", ATRACE_TAG_HAL, { } },
    { "res",        "Resource Loading", ATRACE_TAG_RESOURCES, { } },
    { "dalvik",     "Dalvik VM",        ATRACE_TAG_DALVIK, { } },
    { "rs",         "RenderScript",     ATRACE_TAG_RS, { } },
    { "sched",      "CPU Scheduling",   0, {
        { REQ,      "/sys/kernel/debug/tracing/events/sched/sched_switch/enable" },
        { REQ,      "/sys/kernel/debug/tracing/events/sched/sched_wakeup/enable" },
    } },
    { "freq",       "CPU Frequency",    0, {
        { REQ,      "/sys/kernel/debug/tracing/events/power/cpu_frequency/enable" },
        { OPT,      "/sys/kernel/debug/tracing/events/power/clock_set_rate/enable" },
    } },
    { "membus",     "Memory Bus Utilization", 0, {
        { REQ,      "/sys/kernel/debug/tracing/events/memory_bus/enable" },
    } },
    { "idle",       "CPU Idle",         0, {
        { REQ,      "/sys/kernel/debug/tracing/events/power/cpu_idle/enable" },
    } },
    { "disk",       "Disk I/O",         0, {
        { REQ,      "/sys/kernel/debug/tracing/events/ext4/ext4_sync_file_enter/enable" },
        { REQ,      "/sys/kernel/debug/tracing/events/ext4/ext4_sync_file_exit/enable" },
        { REQ,      "/sys/kernel/debug/tracing/events/block/block_rq_issue/enable" },
        { REQ,      "/sys/kernel/debug/tracing/events/block/block_rq_complete/enable" },
    } },
    { "mmc",        "eMMC commands",    0, {
        { REQ,      "/sys/kernel/debug/tracing/events/mmc/enable" },
    } },
    { "load",       "CPU Load",         0, {
        { REQ,      "/sys/kernel/debug/tracing/events/cpufreq_interactive/enable" },
    } },
    { "sync",       "Synchronization",  0, {
        { REQ,      "/sys/kernel/debug/tracing/events/sync/enable" },
    } },
    { "workq",      "Kernel Workqueues", 0, {
        { REQ,      "/sys/kernel/debug/tracing/events/workqueue/enable" },
    } },
};

/* Command line options */
static int g_traceDurationSeconds = 5;
static bool g_traceOverwrite = false;
static int g_traceBufferSizeKB = 2048;
static bool g_compress = false;
static bool g_nohup = false;
static int g_initialSleepSecs = 0;
static const char* g_kernelTraceFuncs = NULL;
static const char* g_debugAppCmdLine = "";

/* Global state */
static bool g_traceAborted = false;
static bool g_categoryEnables[NELEM(k_categories)] = {};

/* Sys file paths */
static const char* k_traceClockPath =
    "/sys/kernel/debug/tracing/trace_clock";

static const char* k_traceBufferSizePath =
    "/sys/kernel/debug/tracing/buffer_size_kb";

static const char* k_tracingOverwriteEnablePath =
    "/sys/kernel/debug/tracing/options/overwrite";

static const char* k_currentTracerPath =
    "/sys/kernel/debug/tracing/current_tracer";

static const char* k_printTgidPath =
    "/sys/kernel/debug/tracing/options/print-tgid";

static const char* k_funcgraphAbsTimePath =
    "/sys/kernel/debug/tracing/options/funcgraph-abstime";

static const char* k_funcgraphCpuPath =
    "/sys/kernel/debug/tracing/options/funcgraph-cpu";

static const char* k_funcgraphProcPath =
    "/sys/kernel/debug/tracing/options/funcgraph-proc";

static const char* k_funcgraphFlatPath =
    "/sys/kernel/debug/tracing/options/funcgraph-flat";

static const char* k_funcgraphDurationPath =
    "/sys/kernel/debug/tracing/options/funcgraph-duration";

static const char* k_ftraceFilterPath =
    "/sys/kernel/debug/tracing/set_ftrace_filter";

static const char* k_tracingOnPath =
    "/sys/kernel/debug/tracing/tracing_on";

static const char* k_tracePath =
    "/sys/kernel/debug/tracing/trace";

// Check whether a file exists.
static bool fileExists(const char* filename) {
    return access(filename, F_OK) != -1;
}

// Check whether a file is writable.
static bool fileIsWritable(const char* filename) {
    return access(filename, W_OK) != -1;
}

// Truncate a file.
static bool truncateFile(const char* path)
{
    // This uses creat rather than truncate because some of the debug kernel
    // device nodes (e.g. k_ftraceFilterPath) currently aren't changed by
    // calls to truncate, but they are cleared by calls to creat.
    int traceFD = creat(path, 0);
    if (traceFD == -1) {
        fprintf(stderr, "error truncating %s: %s (%d)\n", path,
            strerror(errno), errno);
        return false;
    }

    close(traceFD);

    return true;
}

static bool _writeStr(const char* filename, const char* str, int flags)
{
    int fd = open(filename, flags);
    if (fd == -1) {
        fprintf(stderr, "error opening %s: %s (%d)\n", filename,
                strerror(errno), errno);
        return false;
    }

    bool ok = true;
    ssize_t len = strlen(str);
    if (write(fd, str, len) != len) {
        fprintf(stderr, "error writing to %s: %s (%d)\n", filename,
                strerror(errno), errno);
        ok = false;
    }

    close(fd);

    return ok;
}

// Write a string to a file, returning true if the write was successful.
static bool writeStr(const char* filename, const char* str)
{
    return _writeStr(filename, str, O_WRONLY);
}

// Append a string to a file, returning true if the write was successful.
static bool appendStr(const char* filename, const char* str)
{
    return _writeStr(filename, str, O_APPEND|O_WRONLY);
}

// Enable or disable a kernel option by writing a "1" or a "0" into a /sys
// file.
static bool setKernelOptionEnable(const char* filename, bool enable)
{
    return writeStr(filename, enable ? "1" : "0");
}

// Check whether the category is supported on the device with the current
// rootness.  A category is supported only if all its required /sys/ files are
// writable and if enabling the category will enable one or more tracing tags
// or /sys/ files.
static bool isCategorySupported(const TracingCategory& category)
{
    bool ok = category.tags != 0;
    for (int i = 0; i < MAX_SYS_FILES; i++) {
        const char* path = category.sysfiles[i].path;
        bool req = category.sysfiles[i].required == REQ;
        if (path != NULL) {
            if (req) {
                if (!fileIsWritable(path)) {
                    return false;
                } else {
                    ok = true;
                }
            } else {
                ok |= fileIsWritable(path);
            }
        }
    }
    return ok;
}

// Check whether the category would be supported on the device if the user
// were root.  This function assumes that root is able to write to any file
// that exists.  It performs the same logic as isCategorySupported, but it
// uses file existance rather than writability in the /sys/ file checks.
static bool isCategorySupportedForRoot(const TracingCategory& category)
{
    bool ok = category.tags != 0;
    for (int i = 0; i < MAX_SYS_FILES; i++) {
        const char* path = category.sysfiles[i].path;
        bool req = category.sysfiles[i].required == REQ;
        if (path != NULL) {
            if (req) {
                if (!fileExists(path)) {
                    return false;
                } else {
                    ok = true;
                }
            } else {
                ok |= fileExists(path);
            }
        }
    }
    return ok;
}

// Enable or disable overwriting of the kernel trace buffers.  Disabling this
// will cause tracing to stop once the trace buffers have filled up.
static bool setTraceOverwriteEnable(bool enable)
{
    return setKernelOptionEnable(k_tracingOverwriteEnablePath, enable);
}

// Enable or disable kernel tracing.
static bool setTracingEnabled(bool enable)
{
    return setKernelOptionEnable(k_tracingOnPath, enable);
}

// Clear the contents of the kernel trace.
static bool clearTrace()
{
    return truncateFile(k_tracePath);
}

// Set the size of the kernel's trace buffer in kilobytes.
static bool setTraceBufferSizeKB(int size)
{
    char str[32] = "1";
    int len;
    if (size < 1) {
        size = 1;
    }
    snprintf(str, 32, "%d", size);
    return writeStr(k_traceBufferSizePath, str);
}

// Enable or disable the kernel's use of the global clock.  Disabling the global
// clock will result in the kernel using a per-CPU local clock.
static bool setGlobalClockEnable(bool enable)
{
    return writeStr(k_traceClockPath, enable ? "global" : "local");
}

static bool setPrintTgidEnableIfPresent(bool enable)
{
    if (fileExists(k_printTgidPath)) {
        return setKernelOptionEnable(k_printTgidPath, enable);
    }
    return true;
}

// Poke all the binder-enabled processes in the system to get them to re-read
// their system properties.
static bool pokeBinderServices()
{
    sp<IServiceManager> sm = defaultServiceManager();
    Vector<String16> services = sm->listServices();
    for (size_t i = 0; i < services.size(); i++) {
        sp<IBinder> obj = sm->checkService(services[i]);
        if (obj != NULL) {
            Parcel data;
            if (obj->transact(IBinder::SYSPROPS_TRANSACTION, data,
                    NULL, 0) != OK) {
                if (false) {
                    // XXX: For some reason this fails on tablets trying to
                    // poke the "phone" service.  It's not clear whether some
                    // are expected to fail.
                    String8 svc(services[i]);
                    fprintf(stderr, "error poking binder service %s\n",
                        svc.string());
                    return false;
                }
            }
        }
    }
    return true;
}

// Set the trace tags that userland tracing uses, and poke the running
// processes to pick up the new value.
static bool setTagsProperty(uint64_t tags)
{
    char buf[64];
    snprintf(buf, 64, "%#llx", tags);
    if (property_set(k_traceTagsProperty, buf) < 0) {
        fprintf(stderr, "error setting trace tags system property\n");
        return false;
    }
    return true;
}

// Set the system property that indicates which apps should perform
// application-level tracing.
static bool setAppCmdlineProperty(const char* cmdline)
{
    if (property_set(k_traceAppCmdlineProperty, cmdline) < 0) {
        fprintf(stderr, "error setting trace app system property\n");
        return false;
    }
    return true;
}

// Disable all /sys/ enable files.
static bool disableKernelTraceEvents() {
    bool ok = true;
    for (int i = 0; i < NELEM(k_categories); i++) {
        const TracingCategory &c = k_categories[i];
        for (int j = 0; j < MAX_SYS_FILES; j++) {
            const char* path = c.sysfiles[j].path;
            if (path != NULL && fileIsWritable(path)) {
                ok &= setKernelOptionEnable(path, false);
            }
        }
    }
    return ok;
}

// Verify that the comma separated list of functions are being traced by the
// kernel.
static bool verifyKernelTraceFuncs(const char* funcs)
{
    int fd = open(k_ftraceFilterPath, O_RDONLY);
    if (fd == -1) {
        fprintf(stderr, "error opening %s: %s (%d)\n", k_ftraceFilterPath,
            strerror(errno), errno);
        return false;
    }

    char buf[4097];
    ssize_t n = read(fd, buf, 4096);
    close(fd);
    if (n == -1) {
        fprintf(stderr, "error reading %s: %s (%d)\n", k_ftraceFilterPath,
            strerror(errno), errno);
        return false;
    }

    buf[n] = '\0';
    String8 funcList = String8::format("\n%s", buf);

    // Make sure that every function listed in funcs is in the list we just
    // read from the kernel.
    bool ok = true;
    char* myFuncs = strdup(funcs);
    char* func = strtok(myFuncs, ",");
    while (func) {
        String8 fancyFunc = String8::format("\n%s\n", func);
        bool found = funcList.find(fancyFunc.string(), 0) >= 0;
        if (!found || func[0] == '\0') {
            fprintf(stderr, "error: \"%s\" is not a valid kernel function "
                "to trace.\n", func);
            ok = false;
        }
        func = strtok(NULL, ",");
    }
    free(myFuncs);

    return ok;
}

// Set the comma separated list of functions that the kernel is to trace.
static bool setKernelTraceFuncs(const char* funcs)
{
    bool ok = true;

    if (funcs == NULL || funcs[0] == '\0') {
        // Disable kernel function tracing.
        if (fileIsWritable(k_currentTracerPath)) {
            ok &= writeStr(k_currentTracerPath, "nop");
        }
        if (fileIsWritable(k_ftraceFilterPath)) {
            ok &= truncateFile(k_ftraceFilterPath);
        }
    } else {
        // Enable kernel function tracing.
        ok &= writeStr(k_currentTracerPath, "function_graph");
        ok &= setKernelOptionEnable(k_funcgraphAbsTimePath, true);
        ok &= setKernelOptionEnable(k_funcgraphCpuPath, true);
        ok &= setKernelOptionEnable(k_funcgraphProcPath, true);
        ok &= setKernelOptionEnable(k_funcgraphFlatPath, true);

        // Set the requested filter functions.
        ok &= truncateFile(k_ftraceFilterPath);
        char* myFuncs = strdup(funcs);
        char* func = strtok(myFuncs, ",");
        while (func) {
            ok &= appendStr(k_ftraceFilterPath, func);
            func = strtok(NULL, ",");
        }
        free(myFuncs);

        // Verify that the set functions are being traced.
        if (ok) {
            ok &= verifyKernelTraceFuncs(funcs);
        }
    }

    return ok;
}

// Set all the kernel tracing settings to the desired state for this trace
// capture.
static bool setUpTrace()
{
    bool ok = true;

    // Set up the tracing options.
    ok &= setTraceOverwriteEnable(g_traceOverwrite);
    ok &= setTraceBufferSizeKB(g_traceBufferSizeKB);
    ok &= setGlobalClockEnable(true);
    ok &= setPrintTgidEnableIfPresent(true);
    ok &= setKernelTraceFuncs(g_kernelTraceFuncs);

    // Set up the tags property.
    uint64_t tags = 0;
    for (int i = 0; i < NELEM(k_categories); i++) {
        if (g_categoryEnables[i]) {
            const TracingCategory &c = k_categories[i];
            tags |= c.tags;
        }
    }
    ok &= setTagsProperty(tags);
    ok &= setAppCmdlineProperty(g_debugAppCmdLine);
    ok &= pokeBinderServices();

    // Disable all the sysfs enables.  This is done as a separate loop from
    // the enables to allow the same enable to exist in multiple categories.
    ok &= disableKernelTraceEvents();

    // Enable all the sysfs enables that are in an enabled category.
    for (int i = 0; i < NELEM(k_categories); i++) {
        if (g_categoryEnables[i]) {
            const TracingCategory &c = k_categories[i];
            for (int j = 0; j < MAX_SYS_FILES; j++) {
                const char* path = c.sysfiles[j].path;
                bool required = c.sysfiles[j].required == REQ;
                if (path != NULL) {
                    if (fileIsWritable(path)) {
                        ok &= setKernelOptionEnable(path, true);
                    } else if (required) {
                        fprintf(stderr, "error writing file %s\n", path);
                        ok = false;
                    }
                }
            }
        }
    }

    return ok;
}

// Reset all the kernel tracing settings to their default state.
static void cleanUpTrace()
{
    // Disable all tracing that we're able to.
    disableKernelTraceEvents();

    // Reset the system properties.
    setTagsProperty(0);
    setAppCmdlineProperty("");
    pokeBinderServices();

    // Set the options back to their defaults.
    setTraceOverwriteEnable(true);
    setTraceBufferSizeKB(1);
    setGlobalClockEnable(false);
    setPrintTgidEnableIfPresent(false);
    setKernelTraceFuncs(NULL);
}


// Enable tracing in the kernel.
static bool startTrace()
{
    return setTracingEnabled(true);
}

// Disable tracing in the kernel.
static void stopTrace()
{
    setTracingEnabled(false);
}

// Read the current kernel trace and write it to stdout.
static void dumpTrace()
{
    int traceFD = open(k_tracePath, O_RDWR);
    if (traceFD == -1) {
        fprintf(stderr, "error opening %s: %s (%d)\n", k_tracePath,
                strerror(errno), errno);
        return;
    }

    if (g_compress) {
        z_stream zs;
        uint8_t *in, *out;
        int result, flush;

        bzero(&zs, sizeof(zs));
        result = deflateInit(&zs, Z_DEFAULT_COMPRESSION);
        if (result != Z_OK) {
            fprintf(stderr, "error initializing zlib: %d\n", result);
            close(traceFD);
            return;
        }

        const size_t bufSize = 64*1024;
        in = (uint8_t*)malloc(bufSize);
        out = (uint8_t*)malloc(bufSize);
        flush = Z_NO_FLUSH;

        zs.next_out = out;
        zs.avail_out = bufSize;

        do {

            if (zs.avail_in == 0) {
                // More input is needed.
                result = read(traceFD, in, bufSize);
                if (result < 0) {
                    fprintf(stderr, "error reading trace: %s (%d)\n",
                            strerror(errno), errno);
                    result = Z_STREAM_END;
                    break;
                } else if (result == 0) {
                    flush = Z_FINISH;
                } else {
                    zs.next_in = in;
                    zs.avail_in = result;
                }
            }

            if (zs.avail_out == 0) {
                // Need to write the output.
                result = write(STDOUT_FILENO, out, bufSize);
                if ((size_t)result < bufSize) {
                    fprintf(stderr, "error writing deflated trace: %s (%d)\n",
                            strerror(errno), errno);
                    result = Z_STREAM_END; // skip deflate error message
                    zs.avail_out = bufSize; // skip the final write
                    break;
                }
                zs.next_out = out;
                zs.avail_out = bufSize;
            }

        } while ((result = deflate(&zs, flush)) == Z_OK);

        if (result != Z_STREAM_END) {
            fprintf(stderr, "error deflating trace: %s\n", zs.msg);
        }

        if (zs.avail_out < bufSize) {
            size_t bytes = bufSize - zs.avail_out;
            result = write(STDOUT_FILENO, out, bytes);
            if ((size_t)result < bytes) {
                fprintf(stderr, "error writing deflated trace: %s (%d)\n",
                        strerror(errno), errno);
            }
        }

        result = deflateEnd(&zs);
        if (result != Z_OK) {
            fprintf(stderr, "error cleaning up zlib: %d\n", result);
        }

        free(in);
        free(out);
    } else {
        ssize_t sent = 0;
        while ((sent = sendfile(STDOUT_FILENO, traceFD, NULL, 64*1024*1024)) > 0);
        if (sent == -1) {
            fprintf(stderr, "error dumping trace: %s (%d)\n", strerror(errno),
                    errno);
        }
    }

    close(traceFD);
}

static void handleSignal(int signo)
{
    if (!g_nohup) {
        g_traceAborted = true;
    }
}

static void registerSigHandler()
{
    struct sigaction sa;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;
    sa.sa_handler = handleSignal;
    sigaction(SIGHUP, &sa, NULL);
    sigaction(SIGINT, &sa, NULL);
    sigaction(SIGQUIT, &sa, NULL);
    sigaction(SIGTERM, &sa, NULL);
}

static bool setCategoryEnable(const char* name, bool enable)
{
    for (int i = 0; i < NELEM(k_categories); i++) {
        const TracingCategory& c = k_categories[i];
        if (strcmp(name, c.name) == 0) {
            if (isCategorySupported(c)) {
                g_categoryEnables[i] = enable;
                return true;
            } else {
                if (isCategorySupportedForRoot(c)) {
                    fprintf(stderr, "error: category \"%s\" requires root "
                            "privileges.\n", name);
                } else {
                    fprintf(stderr, "error: category \"%s\" is not supported "
                            "on this device.\n", name);
                }
                return false;
            }
        }
    }
    fprintf(stderr, "error: unknown tracing category \"%s\"\n", name);
    return false;
}

static void listSupportedCategories()
{
    for (int i = 0; i < NELEM(k_categories); i++) {
        const TracingCategory& c = k_categories[i];
        if (isCategorySupported(c)) {
            printf("  %10s - %s\n", c.name, c.longname);
        }
    }
}

// Print the command usage help to stderr.
static void showHelp(const char *cmd)
{
    fprintf(stderr, "usage: %s [options] [categories...]\n", cmd);
    fprintf(stderr, "options include:\n"
                    "  -a appname      enable app-level tracing for a comma "
                        "separated list of cmdlines\n"
                    "  -b N            use a trace buffer size of N KB\n"
                    "  -c              trace into a circular buffer\n"
                    "  -k fname,...    trace the listed kernel functions\n"
                    "  -n              ignore signals\n"
                    "  -s N            sleep for N seconds before tracing [default 0]\n"
                    "  -t N            trace for N seconds [defualt 5]\n"
                    "  -z              compress the trace dump\n"
                    "  --async_start   start circular trace and return immediatly\n"
                    "  --async_dump    dump the current contents of circular trace buffer\n"
                    "  --async_stop    stop tracing and dump the current contents of circular\n"
                    "                    trace buffer\n"
                    "  --list_categories\n"
                    "                  list the available tracing categories\n"
            );
}

int main(int argc, char **argv)
{
    bool async = false;
    bool traceStart = true;
    bool traceStop = true;
    bool traceDump = true;

    if (argc == 2 && 0 == strcmp(argv[1], "--help")) {
        showHelp(argv[0]);
        exit(0);
    }

    for (;;) {
        int ret;
        int option_index = 0;
        static struct option long_options[] = {
            {"async_start",     no_argument, 0,  0 },
            {"async_stop",      no_argument, 0,  0 },
            {"async_dump",      no_argument, 0,  0 },
            {"list_categories", no_argument, 0,  0 },
            {           0,                0, 0,  0 }
        };

        ret = getopt_long(argc, argv, "a:b:ck:ns:t:z",
                          long_options, &option_index);

        if (ret < 0) {
            for (int i = optind; i < argc; i++) {
                if (!setCategoryEnable(argv[i], true)) {
                    fprintf(stderr, "error enabling tracing category \"%s\"\n", argv[i]);
                    exit(1);
                }
            }
            break;
        }

        switch(ret) {
            case 'a':
                g_debugAppCmdLine = optarg;
            break;

            case 'b':
                g_traceBufferSizeKB = atoi(optarg);
            break;

            case 'c':
                g_traceOverwrite = true;
            break;

            case 'k':
                g_kernelTraceFuncs = optarg;
            break;

            case 'n':
                g_nohup = true;
            break;

            case 's':
                g_initialSleepSecs = atoi(optarg);
            break;

            case 't':
                g_traceDurationSeconds = atoi(optarg);
            break;

            case 'z':
                g_compress = true;
            break;

            case 0:
                if (!strcmp(long_options[option_index].name, "async_start")) {
                    async = true;
                    traceStop = false;
                    traceDump = false;
                    g_traceOverwrite = true;
                } else if (!strcmp(long_options[option_index].name, "async_stop")) {
                    async = true;
                    traceStop = false;
                } else if (!strcmp(long_options[option_index].name, "async_dump")) {
                    async = true;
                    traceStart = false;
                    traceStop = false;
                } else if (!strcmp(long_options[option_index].name, "list_categories")) {
                    listSupportedCategories();
                    exit(0);
                }
            break;

            default:
                fprintf(stderr, "\n");
                showHelp(argv[0]);
                exit(-1);
            break;
        }
    }

    registerSigHandler();

    if (g_initialSleepSecs > 0) {
        sleep(g_initialSleepSecs);
    }

    bool ok = true;
    ok &= setUpTrace();
    ok &= startTrace();

    if (ok && traceStart) {
        printf("capturing trace...");
        fflush(stdout);

        // We clear the trace after starting it because tracing gets enabled for
        // each CPU individually in the kernel. Having the beginning of the trace
        // contain entries from only one CPU can cause "begin" entries without a
        // matching "end" entry to show up if a task gets migrated from one CPU to
        // another.
        ok = clearTrace();

        if (ok && !async) {
            // Sleep to allow the trace to be captured.
            struct timespec timeLeft;
            timeLeft.tv_sec = g_traceDurationSeconds;
            timeLeft.tv_nsec = 0;
            do {
                if (g_traceAborted) {
                    break;
                }
            } while (nanosleep(&timeLeft, &timeLeft) == -1 && errno == EINTR);
        }
    }

    // Stop the trace and restore the default settings.
    if (traceStop)
        stopTrace();

    if (ok && traceDump) {
        if (!g_traceAborted) {
            printf(" done\nTRACE:\n");
            fflush(stdout);
            dumpTrace();
        } else {
            printf("\ntrace aborted.\n");
            fflush(stdout);
        }
        clearTrace();
    } else if (!ok) {
        fprintf(stderr, "unable to start tracing\n");
    }

    // Reset the trace buffer size to 1.
    if (traceStop)
        cleanUpTrace();

    return g_traceAborted ? 1 : 0;
}
