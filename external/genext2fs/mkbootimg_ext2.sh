#!/bin/bash

KERNEL=
RAMDISK=
CMDLINE=
GENEXT2FS=
GRUBCONF=
TMPDIR=
OUTPUT=

while [ $# -gt 0 ]; do
    case $1 in
        --kernel)
            KERNEL=$2
            shift
            ;;

        --ramdisk)
            RAMDISK=$2
            shift
            ;;

        --cmdline)
            CMDLINE=$2
            shift
            ;;

        --genext2fs)
            GENEXT2FS=$2
            shift
            ;;

        --tmpdir)
            TMPDIR=$2
            shift
            ;;
        --grubconf)
            GRUBCONF=$2
            shift
            ;;
        --output)
            OUTPUT=$2
            shift
            ;;

        --help)
            echo "Usage: $0 OPTIONS"
            echo "Create an ext2 image that contains everything necessary"
            echo -e "to be the boot file system.\n"
            echo "The following options exist:"
            echo "  --genext2fs <genext2fs>   The location of genext2fs binary"
            echo "  --kernel <kernel>         The kernel to boot"
            echo "  --ramdisk <ramdisk>       The ramdisk to be used"
            echo "  --cmdline <cmdline>       The command line to use"
            echo "  --grubconf <confile>      The path to grub conf file"
            echo "  --tmpdir <tmpdir>         The temporary dir where files"\
                 "can be copied"
            echo "  --output <output>         The filename of the output image"
            echo
            exit 0
            ;;

        *)
            echo "Unknown option $1."
            exit 1
            ;;
    esac
    shift
done

if [ -z "$KERNEL" -o -z "$RAMDISK" -o -z "$GENEXT2FS" -o -z "$TMPDIR" -o \
     -z "$OUTPUT" ]; then
    echo "Missing required arguments."
    exit 1
fi

if ! [ -x "$GENEXT2FS" -a -f "$RAMDISK" -a -f "$KERNEL" ]; then
    echo "Must provide path to a valid genext2fs binary."
    exit 1
fi

rm -rf $TMPDIR
mkdir -p $TMPDIR

echo -n "$CMDLINE" > $TMPDIR/cmdline
cp -f $KERNEL $TMPDIR/kernel
cp -f $RAMDISK $TMPDIR/ramdisk

if [ -f "$GRUBCONF" ]; then
    mkdir -p $TMPDIR/boot/grub
    cp -f $GRUBCONF $TMPDIR/boot/grub/menu.lst
fi

num_blocks=`du -sk $TMPDIR | tail -n1 | awk '{print $1;}'`

# add 1%
extra=`expr $num_blocks / 100`
reserve=10
[ $extra -lt $reserve ] && extra=$reserve

num_blocks=`expr $num_blocks + $extra`
num_inodes=`find $TMPDIR | wc -l`

$GENEXT2FS -d $TMPDIR -b $num_blocks -N $num_inodes -m 0 $OUTPUT

exit 0
