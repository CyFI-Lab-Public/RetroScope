#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root(const int4* in) {
    int id = in->x;
    int data[4];
    data[0] = in->y;
    data[1] = in->z;
    data[2] = in->w;
    rsSendToClient(id, data, sizeof(data));
}
