#include <stdbool.h>
#include <errno.h>
#include <time.h>
#include <net/if.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/mman.h>

#include "calibrator.h"
#include "plt.h"
#include "ini.h"
#include "nvs.h"

SECTION(get);
SECTION(set);

static int handle_push_nvs(struct nl80211_state *state,
            struct nl_cb *cb,
            struct nl_msg *msg,
            int argc, char **argv)
{
    void *map = MAP_FAILED;
    int fd, retval = 0;
    struct nlattr *key;
    struct stat filestat;

    if (argc != 1) {
        return 1;
    }

    fd = open(argv[0], O_RDONLY);
    if (fd < 0) {
        perror("Error opening file for reading");
        return 1;
    }

    if (fstat(fd, &filestat) < 0) {
        perror("Error stating file");
        return 1;
    }

    map = mmap(0, filestat.st_size, PROT_READ, MAP_SHARED, fd, 0);
    if (map == MAP_FAILED) {
        perror("Error mmapping the file");
        goto nla_put_failure;
    }

    key = nla_nest_start(msg, NL80211_ATTR_TESTDATA);
    if (!key) {
        goto nla_put_failure;
    }

    NLA_PUT_U32(msg, WL1271_TM_ATTR_CMD_ID, WL1271_TM_CMD_NVS_PUSH);
    NLA_PUT(msg, WL1271_TM_ATTR_DATA, filestat.st_size, map);

    nla_nest_end(msg, key);

    goto cleanup;

nla_put_failure:
    retval = -ENOBUFS;

cleanup:
    if (map != MAP_FAILED) {
        munmap(map, filestat.st_size);
    }

    close(fd);

    return retval;
}

COMMAND(set, push_nvs, "<nvs filename>",
    NL80211_CMD_TESTMODE, 0, CIB_PHY, handle_push_nvs,
    "Push NVS file into the system");

#if 0
static int handle_fetch_nvs(struct nl80211_state *state,
            struct nl_cb *cb,
            struct nl_msg *msg,
            int argc, char **argv)
{
    char *end;
    void *map = MAP_FAILED;
    int fd, retval = 0;
    struct nlattr *key;
    struct stat filestat;

    if (argc != 0)
        return 1;

    key = nla_nest_start(msg, NL80211_ATTR_TESTDATA);
    if (!key)
        goto nla_put_failure;

    NLA_PUT_U32(msg, WL1271_TM_ATTR_CMD_ID, WL1271_TM_CMD_NVS_PUSH);
    NLA_PUT_U32(msg, WL1271_TM_ATTR_IE_ID, WL1271_TM_CMD_NVS_PUSH);

    nla_nest_end(msg, key);

    goto cleanup;

nla_put_failure:
    retval = -ENOBUFS;

cleanup:
    if (map != MAP_FAILED)
        munmap(map, filestat.st_size);

    close(fd);

    return retval;
}

COMMAND(set, fetch_nvs, NULL,
    NL80211_CMD_TESTMODE, 0, CIB_NETDEV, handle_fetch_nvs,
    "Send command to fetch NVS file");
#endif
static int get_nvs_mac(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    unsigned char mac_buff[12];
    int fd;

    argc -= 2;
    argv += 2;

    if (argc != 1) {
        return 2;
    }

    fd = open(argv[0], O_RDONLY);
    if (fd < 0) {
        perror("Error opening file for reading");
        return 1;
    }

    read(fd, mac_buff, 12);

    printf("MAC addr from NVS: %02x:%02x:%02x:%02x:%02x:%02x\n",
        mac_buff[11], mac_buff[10], mac_buff[6],
        mac_buff[5], mac_buff[4], mac_buff[3]);

    close(fd);

    return 0;
}

COMMAND(get, nvs_mac, "<nvs filename>", 0, 0, CIB_NONE, get_nvs_mac,
    "Get MAC addr from NVS file (offline)");

/*
 * Sets MAC address in NVS.
 * The default value for MAC is random where 1 byte zero.
 */
static int set_nvs_mac(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    unsigned char mac_buff[12];
    unsigned char in_mac[6];
    int fd;

    argc -= 2;
    argv += 2;

    if (argc < 1 || (argc == 2 && (strlen(argv[1]) != 17))) {
        return 2;
    }

    if (argc == 2) {
        sscanf(argv[1], "%2x:%2x:%2x:%2x:%2x:%2x",
        (unsigned int *)&in_mac[0], (unsigned int *)&in_mac[1],
        (unsigned int *)&in_mac[2], (unsigned int *)&in_mac[3],
        (unsigned int *)&in_mac[4], (unsigned int *)&in_mac[5]);
    } else {
        srand((unsigned)time(NULL));

        in_mac[0] = 0x0;
        in_mac[1] = rand()%256;
        in_mac[2] = rand()%256;
        in_mac[3] = rand()%256;
        in_mac[4] = rand()%256;
        in_mac[5] = rand()%256;
    }

    fd = open(argv[0], O_RDWR);
    if (fd < 0) {
        perror("Error opening file for reading");
        return 1;
    }

    read(fd, mac_buff, 12);
#if 0
    printf("Got MAC addr for NVS: %02x:%02x:%02x:%02x:%02x:%02x\n",
        in_mac[0], in_mac[1], in_mac[2],
        in_mac[3], in_mac[4], in_mac[5]);

    printf("Got MAC addr from NVS: %02x:%02x:%02x:%02x:%02x:%02x\n",
        mac_buff[11], mac_buff[10], mac_buff[6],
        mac_buff[5], mac_buff[4], mac_buff[3]);
#endif
    mac_buff[11] = in_mac[0];
    mac_buff[10] = in_mac[1];
    mac_buff[6]  = in_mac[2];
    mac_buff[5]  = in_mac[3];
    mac_buff[4]  = in_mac[4];
    mac_buff[3]  = in_mac[5];

    lseek(fd, 0L, 0);

    write(fd, mac_buff, 12);

    close(fd);

    return 0;
}

COMMAND(set, nvs_mac, "<nvs file> [<mac addr>]", 0, 0, CIB_NONE, set_nvs_mac,
    "Set MAC addr in NVS file (offline), like XX:XX:XX:XX:XX:XX");

static int set_ref_nvs(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    struct wl12xx_common cmn = {
        .arch = UNKNOWN_ARCH,
        .parse_ops = NULL,
        .dual_mode = DUAL_MODE_UNSET,
        .done_fem = NO_FEM_PARSED
    };

    argc -= 2;
    argv += 2;

    if (argc != 1) {
        return 1;
    }

    if (read_ini(*argv, &cmn)) {
        fprintf(stderr, "Fail to read ini file\n");
        return 1;
    }

    cfg_nvs_ops(&cmn);

    if (create_nvs_file(&cmn)) {
        fprintf(stderr, "Fail to create reference NVS file\n");
        return 1;
    }
#if 0
    printf("\n\tThe NVS file (%s) is ready\n\tCopy it to %s and "
        "reboot the system\n\n",
        NEW_NVS_NAME, CURRENT_NVS_NAME);
#endif
    return 0;
}

COMMAND(set, ref_nvs, "<ini file>", 0, 0, CIB_NONE, set_ref_nvs,
    "Create reference NVS file");

static int set_ref_nvs2(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    struct wl12xx_common cmn = {
        .arch = UNKNOWN_ARCH,
        .parse_ops = NULL,
        .dual_mode = DUAL_MODE_UNSET,
        .done_fem = NO_FEM_PARSED
    };

    argc -= 2;
    argv += 2;

    if (argc != 2) {
        return 1;
    }

    if (read_ini(*argv, &cmn)) {
        return 1;
    }

    argv++;
    if (read_ini(*argv, &cmn)) {
        return 1;
    }

    cfg_nvs_ops(&cmn);

    if (create_nvs_file(&cmn)) {
        fprintf(stderr, "Fail to create reference NVS file\n");
        return 1;
    }
#if 0
    printf("\n\tThe NVS file (%s) is ready\n\tCopy it to %s and "
        "reboot the system\n\n",
        NEW_NVS_NAME, CURRENT_NVS_NAME);
#endif
    return 0;
}

COMMAND(set, ref_nvs2, "<ini file> <ini file>", 0, 0, CIB_NONE, set_ref_nvs2,
    "Create reference NVS file for 2 FEMs");

static int set_upd_nvs(struct nl80211_state *state, struct nl_cb *cb,
    struct nl_msg *msg, int argc, char **argv)
{
    char *fname = NULL;
    struct wl12xx_common cmn = {
        .arch = UNKNOWN_ARCH,
        .parse_ops = NULL
    };

    argc -= 2;
    argv += 2;

    if (argc < 1) {
        return 1;
    }

    if (read_ini(*argv, &cmn)) {
        fprintf(stderr, "Fail to read ini file\n");
        return 1;
    }

    cfg_nvs_ops(&cmn);

    if (argc == 2) {
        fname = *++argv;
    }

    if (update_nvs_file(fname, &cmn)) {
        fprintf(stderr, "Fail to update NVS file\n");
        return 1;
    }
#if 0
    printf("\n\tThe updated NVS file (%s) is ready\n\tCopy it to %s and "
        "reboot the system\n\n", NEW_NVS_NAME, CURRENT_NVS_NAME);
#endif
    return 0;
}

COMMAND(set, upd_nvs, "<ini file> [<nvs file>]", 0, 0, CIB_NONE, set_upd_nvs,
    "Update values of a NVS from INI file");

static int get_dump_nvs(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    char *fname = NULL;
    struct wl12xx_common cmn = {
        .arch = UNKNOWN_ARCH,
        .parse_ops = NULL
    };

    argc -= 2;
    argv += 2;

    if (argc > 0) {
        fname = *argv;
    }

    if (dump_nvs_file(fname, &cmn)) {
        fprintf(stderr, "Fail to dump NVS file\n");
        return 1;
    }

    return 0;
}

COMMAND(get, dump_nvs, "[<nvs file>]", 0, 0, CIB_NONE, get_dump_nvs,
    "Dump NVS file, specified by option or current");

static int set_autofem(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    char *fname = NULL;
    unsigned char val;
    struct wl12xx_common cmn = {
        .arch = UNKNOWN_ARCH,
        .parse_ops = NULL
    };

    argc -= 2;
    argv += 2;

    if (argc < 1) {
        fprintf(stderr, "Missing argument\n");
        return 2;
    }

    sscanf(argv[0], "%2x", (unsigned int *)&val);

    if (argc == 2) {
        fname = argv[1];
    }

    if (set_nvs_file_autofem(fname, val, &cmn)) {
        fprintf(stderr, "Fail to set AutoFEM\n");
        return 1;
    }

    return 0;
}

COMMAND(set, autofem, "<0-manual|1-auto> [<nvs file>]", 0, 0, CIB_NONE, set_autofem,
    "Set Auto FEM detection, where 0 - manual, 1 - auto detection");

static int set_fem_manuf(struct nl80211_state *state, struct nl_cb *cb,
            struct nl_msg *msg, int argc, char **argv)
{
    char *fname = NULL;
    unsigned char val;
    struct wl12xx_common cmn = {
        .arch = UNKNOWN_ARCH,
        .parse_ops = NULL
    };

    argc -= 2;
    argv += 2;

    if (argc < 1) {
        fprintf(stderr, "Missing argument\n");
        return 2;
    }

    sscanf(argv[0], "%2x", (unsigned int *)&val);

    if (argc == 2) {
        fname = argv[1];
    }

    if (set_nvs_file_fem_manuf(fname, val, &cmn)) {
        fprintf(stderr, "Fail to set AutoFEM\n");
        return 1;
    }

    return 0;
}

COMMAND(set, fem_manuf, "<0|1> [<nvs file>]", 0, 0, CIB_NONE, set_fem_manuf,
    "Set FEM manufacturer");

