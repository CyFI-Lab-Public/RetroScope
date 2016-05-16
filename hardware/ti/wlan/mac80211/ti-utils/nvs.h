#ifndef __NVS_H
#define __NVS_H

#define WL127X_NVS_FILE_SZ        912
#define WL128X_NVS_FILE_SZ        1113

int prepare_nvs_file(void *arg, char *file_name);

void cfg_nvs_ops(struct wl12xx_common *cmn);

int create_nvs_file(struct wl12xx_common *cmn);

int update_nvs_file(const char *nvs_file, struct wl12xx_common *cmn);

int dump_nvs_file(const char *nvs_file, struct wl12xx_common *cmn);

int set_nvs_file_autofem(const char *nvs_file, unsigned char val,
    struct wl12xx_common *cmn);

int set_nvs_file_fem_manuf(const char *nvs_file, unsigned char val,
    struct wl12xx_common *cmn);

#endif /* __NVS_H */
