/*
 * Copyright (C) 2008 The Android Open Source Project
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

#define LOG_NDEBUG 0

#include <stdlib.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <cutils/properties.h>

#define LOG_TAG "NatController"
#include <cutils/log.h>
#include <logwrap/logwrap.h>

#include "NatController.h"
#include "SecondaryTableController.h"
#include "NetdConstants.h"

const char* NatController::LOCAL_FORWARD = "natctrl_FORWARD";
const char* NatController::LOCAL_NAT_POSTROUTING = "natctrl_nat_POSTROUTING";
const char* NatController::LOCAL_TETHER_COUNTERS_CHAIN = "natctrl_tether_counters";

NatController::NatController(SecondaryTableController *ctrl) {
    secondaryTableCtrl = ctrl;
}

NatController::~NatController() {
}

struct CommandsAndArgs {
    /* The array size doesn't really matter as the compiler will barf if too many initializers are specified. */
    const char *cmd[32];
    bool checkRes;
};

int NatController::runCmd(int argc, const char **argv) {
    int res;

    res = android_fork_execvp(argc, (char **)argv, NULL, false, false);

#if !LOG_NDEBUG
    std::string full_cmd = argv[0];
    argc--; argv++;
    /*
     * HACK: Sometimes runCmd() is called with a ridcously large value (32)
     * and it works because the argv[] contains a NULL after the last
     * true argv. So here we use the NULL argv[] to terminate when the argc
     * is horribly wrong, and argc for the normal cases.
     */
    for (; argc && argv[0]; argc--, argv++) {
        full_cmd += " ";
        full_cmd += argv[0];
    }
    ALOGV("runCmd(%s) res=%d", full_cmd.c_str(), res);
#endif
    return res;
}

int NatController::setupIptablesHooks() {
    int res;
    res = setDefaults();
    if (res < 0) {
        return res;
    }

    struct CommandsAndArgs defaultCommands[] = {
        /*
         * Chain for tethering counters.
         * This chain is reached via --goto, and then RETURNS.
         */
        {{IPTABLES_PATH, "-F", LOCAL_TETHER_COUNTERS_CHAIN,}, 0},
        {{IPTABLES_PATH, "-X", LOCAL_TETHER_COUNTERS_CHAIN,}, 0},
        {{IPTABLES_PATH, "-N", LOCAL_TETHER_COUNTERS_CHAIN,}, 1},
    };
    for (unsigned int cmdNum = 0; cmdNum < ARRAY_SIZE(defaultCommands); cmdNum++) {
        if (runCmd(ARRAY_SIZE(defaultCommands[cmdNum].cmd), defaultCommands[cmdNum].cmd) &&
            defaultCommands[cmdNum].checkRes) {
                return -1;
        }
    }

    return 0;
}

int NatController::setDefaults() {
    /*
     * The following only works because:
     *  - the defaultsCommands[].cmd array is padded with NULL, and
     *  - the 1st argc of runCmd() will just be the max for the CommandsAndArgs[].cmd, and
     *  - internally it will be memcopied to an array and terminated with a NULL.
     */
    struct CommandsAndArgs defaultCommands[] = {
        {{IPTABLES_PATH, "-F", LOCAL_FORWARD,}, 1},
        {{IPTABLES_PATH, "-A", LOCAL_FORWARD, "-j", "DROP"}, 1},
        {{IPTABLES_PATH, "-t", "nat", "-F", LOCAL_NAT_POSTROUTING}, 1},
        {{IP_PATH, "rule", "flush"}, 0},
        {{IP_PATH, "-6", "rule", "flush"}, 0},
        {{IP_PATH, "rule", "add", "from", "all", "lookup", "default", "prio", "32767"}, 0},
        {{IP_PATH, "rule", "add", "from", "all", "lookup", "main", "prio", "32766"}, 0},
        {{IP_PATH, "-6", "rule", "add", "from", "all", "lookup", "default", "prio", "32767"}, 0},
        {{IP_PATH, "-6", "rule", "add", "from", "all", "lookup", "main", "prio", "32766"}, 0},
        {{IP_PATH, "route", "flush", "cache"}, 0},
    };
    for (unsigned int cmdNum = 0; cmdNum < ARRAY_SIZE(defaultCommands); cmdNum++) {
        if (runCmd(ARRAY_SIZE(defaultCommands[cmdNum].cmd), defaultCommands[cmdNum].cmd) &&
            defaultCommands[cmdNum].checkRes) {
                return -1;
        }
    }

    natCount = 0;

    return 0;
}

bool NatController::checkInterface(const char *iface) {
    if (strlen(iface) > IFNAMSIZ) return false;
    return true;
}

int NatController::routesOp(bool add, const char *intIface, const char *extIface, char **argv, int addrCount) {
    int tableNumber = secondaryTableCtrl->findTableNumber(extIface);
    int ret = 0;

    if (tableNumber != -1) {
        for (int i = 0; i < addrCount; i++) {
            if (add) {
                ret |= secondaryTableCtrl->modifyFromRule(tableNumber, ADD, argv[5+i]);
                ret |= secondaryTableCtrl->modifyLocalRoute(tableNumber, ADD, intIface, argv[5+i]);
            } else {
                ret |= secondaryTableCtrl->modifyLocalRoute(tableNumber, DEL, intIface, argv[5+i]);
                ret |= secondaryTableCtrl->modifyFromRule(tableNumber, DEL, argv[5+i]);
            }
        }
        const char *cmd[] = {
                IP_PATH,
                "route",
                "flush",
                "cache"
        };
        runCmd(ARRAY_SIZE(cmd), cmd);
    }
    return ret;
}

//  0    1       2       3       4            5
// nat enable intface extface addrcnt nated-ipaddr/prelength
int NatController::enableNat(const int argc, char **argv) {
    int i;
    int addrCount = atoi(argv[4]);
    const char *intIface = argv[2];
    const char *extIface = argv[3];
    int tableNumber;

    ALOGV("enableNat(intIface=<%s>, extIface=<%s>)",intIface, extIface);

    if (!checkInterface(intIface) || !checkInterface(extIface)) {
        ALOGE("Invalid interface specified");
        errno = ENODEV;
        return -1;
    }

    /* Bug: b/9565268. "enableNat wlan0 wlan0". For now we fail until java-land is fixed */
    if (!strcmp(intIface, extIface)) {
        ALOGE("Duplicate interface specified: %s %s", intIface, extIface);
        errno = EINVAL;
        return -1;
    }

    if (argc < 5 + addrCount) {
        ALOGE("Missing Argument");
        errno = EINVAL;
        return -1;
    }
    if (routesOp(true, intIface, extIface, argv, addrCount)) {
        ALOGE("Error setting route rules");
        routesOp(false, intIface, extIface, argv, addrCount);
        errno = ENODEV;
        return -1;
    }

    // add this if we are the first added nat
    if (natCount == 0) {
        const char *cmd[] = {
                IPTABLES_PATH,
                "-t",
                "nat",
                "-A",
                LOCAL_NAT_POSTROUTING,
                "-o",
                extIface,
                "-j",
                "MASQUERADE"
        };
        if (runCmd(ARRAY_SIZE(cmd), cmd)) {
            ALOGE("Error seting postroute rule: iface=%s", extIface);
            // unwind what's been done, but don't care about success - what more could we do?
            routesOp(false, intIface, extIface, argv, addrCount);
            setDefaults();
            return -1;
        }
    }


    if (setForwardRules(true, intIface, extIface) != 0) {
        ALOGE("Error setting forward rules");
        routesOp(false, intIface, extIface, argv, addrCount);
        if (natCount == 0) {
            setDefaults();
        }
        errno = ENODEV;
        return -1;
    }

    /* Always make sure the drop rule is at the end */
    const char *cmd1[] = {
            IPTABLES_PATH,
            "-D",
            LOCAL_FORWARD,
            "-j",
            "DROP"
    };
    runCmd(ARRAY_SIZE(cmd1), cmd1);
    const char *cmd2[] = {
            IPTABLES_PATH,
            "-A",
            LOCAL_FORWARD,
            "-j",
            "DROP"
    };
    runCmd(ARRAY_SIZE(cmd2), cmd2);

    natCount++;
    return 0;
}

int NatController::setTetherCountingRules(bool add, const char *intIface, const char *extIface) {

    /* We only ever add tethering quota rules so that they stick. */
    if (!add) {
        return 0;
    }
    char *quota_name, *proc_path;
    int quota_fd;
    asprintf(&quota_name, "%s_%s", intIface, extIface);

    asprintf(&proc_path, "/proc/net/xt_quota/%s", quota_name);
    quota_fd = open(proc_path, O_RDONLY);
    if (quota_fd >= 0) {
        /* quota for iface pair already exists */
        free(proc_path);
        free(quota_name);
        return 0;
    }
    close(quota_fd);
    free(proc_path);

    const char *cmd2b[] = {
            IPTABLES_PATH,
            "-A",
            LOCAL_TETHER_COUNTERS_CHAIN,
            "-i",
            intIface,
            "-o",
            extIface,
            "-m",
            "quota2",
            "--name",
            quota_name,
            "--grow",
            "-j",
          "RETURN"
    };

    if (runCmd(ARRAY_SIZE(cmd2b), cmd2b) && add) {
        free(quota_name);
        return -1;
    }
    free(quota_name);

    asprintf(&quota_name, "%s_%s", extIface, intIface);
    asprintf(&proc_path, "/proc/net/xt_quota/%s", quota_name);
    quota_fd = open(proc_path, O_RDONLY);
    if (quota_fd >= 0) {
        /* quota for iface pair already exists */
        free(proc_path);
        free(quota_name);
        return 0;
    }
    close(quota_fd);
    free(proc_path);

    const char *cmd3b[] = {
            IPTABLES_PATH,
            "-A",
            LOCAL_TETHER_COUNTERS_CHAIN,
            "-i",
            extIface,
            "-o",
            intIface,
            "-m",
            "quota2",
            "--name",
            quota_name,
            "--grow",
            "-j",
            "RETURN"
    };

    if (runCmd(ARRAY_SIZE(cmd3b), cmd3b) && add) {
        // unwind what's been done, but don't care about success - what more could we do?
        free(quota_name);
        return -1;
    }
    free(quota_name);
    return 0;
}

int NatController::setForwardRules(bool add, const char *intIface, const char *extIface) {
    const char *cmd1[] = {
            IPTABLES_PATH,
            add ? "-A" : "-D",
            LOCAL_FORWARD,
            "-i",
            extIface,
            "-o",
            intIface,
            "-m",
            "state",
            "--state",
            "ESTABLISHED,RELATED",
            "-g",
            LOCAL_TETHER_COUNTERS_CHAIN
    };
    int rc = 0;

    if (runCmd(ARRAY_SIZE(cmd1), cmd1) && add) {
        return -1;
    }

    const char *cmd2[] = {
            IPTABLES_PATH,
            add ? "-A" : "-D",
            LOCAL_FORWARD,
            "-i",
            intIface,
            "-o",
            extIface,
            "-m",
            "state",
            "--state",
            "INVALID",
            "-j",
            "DROP"
    };

    const char *cmd3[] = {
            IPTABLES_PATH,
            add ? "-A" : "-D",
            LOCAL_FORWARD,
            "-i",
            intIface,
            "-o",
            extIface,
            "-g",
            LOCAL_TETHER_COUNTERS_CHAIN
    };

    if (runCmd(ARRAY_SIZE(cmd2), cmd2) && add) {
        // bail on error, but only if adding
        rc = -1;
        goto err_invalid_drop;
    }

    if (runCmd(ARRAY_SIZE(cmd3), cmd3) && add) {
        // unwind what's been done, but don't care about success - what more could we do?
        rc = -1;
        goto err_return;
    }

    if (setTetherCountingRules(add, intIface, extIface) && add) {
        rc = -1;
        goto err_return;
    }

    return 0;

err_return:
    cmd2[1] = "-D";
    runCmd(ARRAY_SIZE(cmd2), cmd2);
err_invalid_drop:
    cmd1[1] = "-D";
    runCmd(ARRAY_SIZE(cmd1), cmd1);
    return rc;
}

// nat disable intface extface
//  0    1       2       3       4            5
// nat enable intface extface addrcnt nated-ipaddr/prelength
int NatController::disableNat(const int argc, char **argv) {
    int i;
    int addrCount = atoi(argv[4]);
    const char *intIface = argv[2];
    const char *extIface = argv[3];
    int tableNumber;

    if (!checkInterface(intIface) || !checkInterface(extIface)) {
        ALOGE("Invalid interface specified");
        errno = ENODEV;
        return -1;
    }

    if (argc < 5 + addrCount) {
        ALOGE("Missing Argument");
        errno = EINVAL;
        return -1;
    }

    setForwardRules(false, intIface, extIface);
    routesOp(false, intIface, extIface, argv, addrCount);
    if (--natCount <= 0) {
        // handle decrement to 0 case (do reset to defaults) and erroneous dec below 0
        setDefaults();
    }
    return 0;
}
