This is UIM utility required for TI's shared transport driver in order
to open UART and to deliver the control of it to the driver.

The author of the utility is Pavan Savoy <pavan_savoy@ti.com>

--- Running UIM utility

It needs to be run at boot, Since linux flavors might require Bluetooth or
GPS to be turned on at boot. For this have the UIM entry in your either one
of your rc.S files or you can have special udev rule based on the platform
driver addition of device "kim".

For Android, the following entry in init.rc should suffice,
service uim /system/bin/uim-sysfs
    user root
    group media bluetooth
    oneshot
[edit]

For Angstrom, the following command should be done,
./uim /dev/ttyO1 3686400 none 22 &

--- Building UIM utility
make uim

