#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation aFailed;
rs_allocation aOutput;

void set_output_void_int(void *out, uint32_t x, uint32_t y) {
    int *out_int = (int *)out;
    *out_int = x + y;
}

void __attribute__((kernel))check_output_int(const int in, uint32_t x, uint32_t y)
{
    if (in != x + y) {
        rsSetElementAt_int(aFailed, 1, 0);
    }
}

void set_output_void_char(void *out, uint32_t x, uint32_t y) {
    uchar *out_int = (uchar *)out;
    *out_int = x + y;
}

void __attribute__((kernel))check_output_char(const uchar in, uint32_t x, uint32_t y)
{
    if (in != x + y) {
        rsSetElementAt_int(aFailed, 1, 0);
    }
}

int __attribute__((kernel)) set_output_int(uint32_t x, uint32_t y) {
    return x + y;
}

void copy_void_int(const void *in, uint32_t x, uint32_t y)
{
    int *in_int = (int*) in;
    rsSetElementAt_int(aOutput, *in_int, x, y);
}

uchar __attribute__((kernel)) set_output_char(uint32_t x, uint32_t y) {
    return x + y;
}

void copy_void_char(const void *in, uint32_t x, uint32_t y)
{
    uchar *in_uchar = (uchar*) in;
    rsSetElementAt_uchar(aOutput, *in_uchar, x, y);
}
