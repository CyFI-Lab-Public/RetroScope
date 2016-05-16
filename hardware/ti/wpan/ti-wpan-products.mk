# This file lists the firmware, software that are specific to
# WiLink connectivity chip on OMAPx platforms.

PRODUCT_PACKAGES += uim-sysfs \
	bt_sco_app \
	kfmapp     \
        BluetoothSCOApp \
        FmRxApp \
        FmTxApp \
        FmService \
        libfmradio \
        fmradioif \
        com.ti.fm.fmradioif.xml \
        libbt-vendor

#copy firmware
PRODUCT_COPY_FILES += \
  device/ti/proprietary-open/wl12xx/wpan/bluetooth/TIInit_10.6.15.bts:system/etc/firmware/TIInit_10.6.15.bts \
  device/ti/proprietary-open/wl12xx/wpan/bluetooth/TIInit_7.2.31.bts:system/etc/firmware/TIInit_7.2.31.bts \
  device/ti/proprietary-open/wl12xx/wpan/bluetooth/TIInit_7.6.15.bts:system/etc/firmware/TIInit_7.6.15.bts \
  device/ti/proprietary-open/wl12xx/wpan/bluetooth/TIInit_12.7.27.bts:system/etc/firmware/TIInit_12.7.27.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fmc_ch8_1283.2.bts:system/etc/firmware/fmc_ch8_1283.2.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fm_rx_ch8_1283.2.bts:system/etc/firmware/fm_rx_ch8_1283.2.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fm_tx_ch8_1283.2.bts:system/etc/firmware/fm_tx_ch8_1283.2.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fmc_init_1273.2.bts:system/etc/firmware/fmc_init_1273.2.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fm_rx_init_1273.2.bts:system/etc/firmware/fm_rx_init_1273.2.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fm_tx_init_1273.2.bts:system/etc/firmware/fm_tx_init_1273.2.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fm_tx_ch8_1273.1.bts:system/etc/firmware/fm_tx_ch8_1273.1.bts \
  device/ti/proprietary-open/wl12xx/wpan/fmradio/fm_tx_ch8_1273.2.bts:system/etc/firmware/fm_tx_ch8_1273.2.bts