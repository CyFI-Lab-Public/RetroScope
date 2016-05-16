#include "shared.rsh"

#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_sampler gNearest;
rs_sampler gLinear;
rs_sampler gMipNearest;
rs_sampler gMipLinear;

uint8_t *gAllocPtr;

static uchar4 lod0Color = {255, 255, 0, 0};
static uchar4 lod1Color = {255, 0, 255, 0};
static uchar4 lod2Color = {0, 255, 255, 0};
static uchar4 lod3Color = {255, 255, 255, 0};

// Allocation has been bound to gAllocPtr
void init_RGBA(rs_allocation a) {
    // Fill base level with one color, mips with something else
    uchar4 *allocPtr = (uchar4*)gAllocPtr;
    uint32_t dimX = rsAllocationGetDimX(a);
    uint32_t dimY = rsAllocationGetDimY(a);
    uint32_t minSize = 1;
    dimX = max(dimX, minSize);
    dimY = max(dimY, minSize);

    uint32_t numPixels = dimX * dimY;
    for (uint32_t i = 0; i < numPixels; i ++) {
        (*allocPtr++) = lod0Color;
    }
    dimX = max(dimX >> 1, minSize);
    dimY = max(dimY >> 1, minSize);
    numPixels = dimX * dimY;
    for (uint32_t i = 0; i < numPixels; i ++) {
        (*allocPtr++) = lod1Color;
    }
    dimX = max(dimX >> 1, minSize);
    dimY = max(dimY >> 1, minSize);
    numPixels = dimX * dimY;
    for (uint32_t i = 0; i < numPixels; i ++) {
        (*allocPtr++) = lod2Color;
    }
    dimX = max(dimX >> 1, minSize);
    dimY = max(dimY >> 1, minSize);
    numPixels = dimX * dimY;
    for (uint32_t i = 0; i < numPixels; i ++) {
        (*allocPtr++) = lod3Color;
    }
}

static bool compare(float4 expected, float4 value) {
    float allowedDelta = 10.0f;
    float4 diff = fabs(expected - value);
    if (diff.x > allowedDelta || diff.y > allowedDelta ||
        diff.z > allowedDelta || diff.w > allowedDelta) {
        return false;
    }
    return true;
}

static bool sub_test_RGBA_1D(rs_allocation alloc1D, float location, float lod,
                          float4 expected0, float4 expected1, float4 expected2, float4 expected3) {
    bool failed = false;
    float4 result = rsSample(alloc1D, gNearest, location, lod);
    _RS_ASSERT(compare(expected0, result));

    result = rsSample(alloc1D, gLinear, location, lod);
    _RS_ASSERT(compare(expected1, result));

    result = rsSample(alloc1D, gMipNearest, location, lod);
    _RS_ASSERT(compare(expected2, result));

    result = rsSample(alloc1D, gMipLinear, location, lod);
    _RS_ASSERT(compare(expected3, result));
    return !failed;
}

static bool sub_test_RGBA_2D(rs_allocation alloc2D, float2 location, float lod,
                          float4 expected0, float4 expected1, float4 expected2, float4 expected3) {
    bool failed = false;
    float4 result = rsSample(alloc2D, gNearest, location, lod);
    _RS_ASSERT(compare(expected0, result));

    result = rsSample(alloc2D, gLinear, location, lod);
    _RS_ASSERT(compare(expected1, result));

    result = rsSample(alloc2D, gMipNearest, location, lod);
    _RS_ASSERT(compare(expected2, result));

    result = rsSample(alloc2D, gMipLinear, location, lod);
    _RS_ASSERT(compare(expected3, result));
    return !failed;
}

void test_RGBA(rs_allocation alloc1D, rs_allocation alloc2D) {
    bool failed = false;
    float4 result;

    float4 fLOD0 = rsUnpackColor8888(lod0Color);
    float4 fLOD1 = rsUnpackColor8888(lod1Color);
    float4 fLOD2 = rsUnpackColor8888(lod2Color);
    float4 fLOD3 = rsUnpackColor8888(lod3Color);

    float4 fLOD04 = fLOD0*0.6f + fLOD1*0.4f;
    float4 fLOD06 = fLOD0*0.4f + fLOD1*0.6f;

    // Test for proper LOD sampling behaviour
    uint32_t numSamples = 5;
    for (uint32_t i = 0; i < numSamples; i ++) {
        float location = (float)i/(float)(numSamples - 1);

        // No lod specified, should be lod 0
        result = rsSample(alloc1D, gNearest, location);
        _RS_ASSERT(compare(fLOD0, result));

        result = rsSample(alloc1D, gLinear, location);
        _RS_ASSERT(compare(fLOD0, result));

        result = rsSample(alloc1D, gMipNearest, location);
        _RS_ASSERT(compare(fLOD0, result));

        result = rsSample(alloc1D, gMipLinear, location);
        _RS_ASSERT(compare(fLOD0, result));

        // Mid lod test
        float lod = 0.4f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD0, fLOD04));

        lod = 0.6f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD1, fLOD06));

        // Base lod sample
        lod = 0.0f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD0, fLOD0));

        // lod 1 test
        lod = 1.0f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD1, fLOD1));

        // lod 2 test
        lod = 2.0f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD2, fLOD2));

        // lod 3 test
        lod = 3.0f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD3, fLOD3));

        // lod 4 test, but only have 3 lods
        lod = 4.0f;
        _RS_ASSERT(sub_test_RGBA_1D(alloc1D, location, lod, fLOD0, fLOD0, fLOD3, fLOD3));

        // 2D case
        float2 f2Location;
        f2Location.x = location;
        f2Location.y = location;
        // No lod specified, should be lod 0
        result = rsSample(alloc2D, gNearest, f2Location);
        _RS_ASSERT(compare(fLOD0, result));

        result = rsSample(alloc2D, gLinear, f2Location);
        _RS_ASSERT(compare(fLOD0, result));

        result = rsSample(alloc2D, gMipNearest, f2Location);
        _RS_ASSERT(compare(fLOD0, result));

        result = rsSample(alloc2D, gMipLinear, f2Location);
        _RS_ASSERT(compare(fLOD0, result));

        // Mid lod test
        lod = 0.4f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD0, fLOD04));

        lod = 0.6f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD1, fLOD06));

        // Base lod sample
        lod = 0.0f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD0, fLOD0));

        // lod 1 test
        lod = 1.0f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD1, fLOD1));

        // lod 2 test
        lod = 2.0f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD2, fLOD2));

        // lod 2 test
        lod = 3.0f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD3, fLOD3));

        // lod 4 test, but only have 3 lods
        lod = 4.0f;
        _RS_ASSERT(sub_test_RGBA_2D(alloc2D, f2Location, lod, fLOD0, fLOD0, fLOD3, fLOD3));
    }

    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

