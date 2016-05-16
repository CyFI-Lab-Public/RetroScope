#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

uint32_t width;
uint32_t height;

rs_allocation mIn;
rs_allocation mOut;

void testAllocationCopy2DRange() {
    rsAllocationCopy2DRange(mOut, 0, 0, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X,
                            width, height, mIn, 0, 0, 0,
                            RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);
}
