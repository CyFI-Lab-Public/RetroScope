This directory contains the Linux versions
of some ICU tools potentially needed during
the Android build process. The tools can be
re-created using the following steps:

(1) Copy the whole "/extlibs/icu-51"
    directory somewhere else. Please
    do not use a vanilla ICU 51 for
    this, since the Android version
    includes some patches for tweaking
    file sizes.

(2) Change to the copy of the directory.

(3) Execute "./runConfigureICU Linux --enable-static=yes --enable-shared=no".

(4) Execute "make".

(5) If you're successful, the results of
    your efforts will reside in "bin".
    Copy them here.
