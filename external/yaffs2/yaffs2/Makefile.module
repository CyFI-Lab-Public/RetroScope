ifneq ($(KERNELRELEASE),)
	EXTRA_CFLAGS += -DYAFFS_OUT_OF_TREE

	obj-m := yaffs2.o

	yaffs2-objs := yaffs_mtdif.o yaffs_mtdif2.o
	yaffs2-objs += yaffs_ecc.o yaffs_fs.o yaffs_guts.o
	yaffs2-objs += yaffs_packedtags2.o
	yaffs2-objs += yaffs_tagscompat.o yaffs_tagsvalidity.o

else
	KERNELDIR ?= /lib/modules/$(shell uname -r)/build
	PWD := $(shell pwd)

modules default:
	$(MAKE) -C $(KERNELDIR) M=$(PWD) modules

mi modules_install:
	$(MAKE) -C $(KERNELDIR) M=$(PWD) modules_install

clean:
	$(MAKE) -C $(KERNELDIR) M=$(PWD) clean
endif
