/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <malloc.h>
#include <string.h>

#include "RenderScript.h"
#include "rsCppInternal.h"

// from system/graphics.h
enum {
    HAL_PIXEL_FORMAT_YV12   = 0x32315659, // YCrCb 4:2:0 Planar
    HAL_PIXEL_FORMAT_YCrCb_420_SP       = 0x11, // NV21
};

using namespace android;
using namespace RSC;

void Type::calcElementCount() {
    bool hasLod = hasMipmaps();
    uint32_t x = getX();
    uint32_t y = getY();
    uint32_t z = getZ();
    uint32_t faces = 1;
    if (hasFaces()) {
        faces = 6;
    }
    if (x == 0) {
        x = 1;
    }
    if (y == 0) {
        y = 1;
    }
    if (z == 0) {
        z = 1;
    }

    uint32_t count = x * y * z * faces;
    while (hasLod && ((x > 1) || (y > 1) || (z > 1))) {
        if(x > 1) {
            x >>= 1;
        }
        if(y > 1) {
            y >>= 1;
        }
        if(z > 1) {
            z >>= 1;
        }

        count += x * y * z * faces;
    }
    mElementCount = count;
}


Type::Type(void *id, sp<RS> rs) : BaseObj(id, rs) {
    mDimX = 0;
    mDimY = 0;
    mDimZ = 0;
    mDimMipmaps = false;
    mDimFaces = false;
    mElement = NULL;
    mYuvFormat = RS_YUV_NONE;
}

void Type::updateFromNative() {
    // We have 6 integer to obtain mDimX; mDimY; mDimZ;
    // mDimLOD; mDimFaces; mElement;

    /*
    int[] dataBuffer = new int[6];
    mRS.nTypeGetNativeData(getID(), dataBuffer);

    mDimX = dataBuffer[0];
    mDimY = dataBuffer[1];
    mDimZ = dataBuffer[2];
    mDimMipmaps = dataBuffer[3] == 1 ? true : false;
    mDimFaces = dataBuffer[4] == 1 ? true : false;

    int elementID = dataBuffer[5];
    if(elementID != 0) {
        mElement = new Element(elementID, mRS);
        mElement.updateFromNative();
    }
    calcElementCount();
    */
}

sp<const Type> Type::create(sp<RS> rs, sp<const Element> e, uint32_t dimX, uint32_t dimY, uint32_t dimZ) {
    void * id = RS::dispatch->TypeCreate(rs->getContext(), e->getID(), dimX, dimY, dimZ, false, false, 0);
    Type *t = new Type(id, rs);

    t->mElement = e;
    t->mDimX = dimX;
    t->mDimY = dimY;
    t->mDimZ = dimZ;
    t->mDimMipmaps = false;
    t->mDimFaces = false;

    t->calcElementCount();

    return t;
}

Type::Builder::Builder(sp<RS> rs, sp<const Element> e) {
    mRS = rs;
    mElement = e;
    mDimX = 0;
    mDimY = 0;
    mDimZ = 0;
    mDimMipmaps = false;
    mDimFaces = false;
}

void Type::Builder::setX(uint32_t value) {
    if(value < 1) {
        ALOGE("Values of less than 1 for Dimension X are not valid.");
    }
    mDimX = value;
}

void Type::Builder::setY(uint32_t value) {
    if(value < 1) {
        ALOGE("Values of less than 1 for Dimension Y are not valid.");
    }
    mDimY = value;
}

void Type::Builder::setZ(uint32_t value) {
    if(value < 1) {
        ALOGE("Values of less than 1 for Dimension Z are not valid.");
    }
    mDimZ = value;
}

void Type::Builder::setYuvFormat(RSYuvFormat format) {
    if (format != RS_YUV_NONE && !(mElement->isCompatible(Element::YUV(mRS)))) {
        ALOGE("Invalid element for use with YUV.");
        return;
    }

    if (format >= RS_YUV_MAX) {
        ALOGE("Invalid YUV format.");
        return;
    }
    mYuvFormat = format;
}


void Type::Builder::setMipmaps(bool value) {
    mDimMipmaps = value;
}

void Type::Builder::setFaces(bool value) {
    mDimFaces = value;
}

sp<const Type> Type::Builder::create() {
    if (mDimZ > 0) {
        if ((mDimX < 1) || (mDimY < 1)) {
            ALOGE("Both X and Y dimension required when Z is present.");
        }
        if (mDimFaces) {
            ALOGE("Cube maps not supported with 3D types.");
        }
    }
    if (mDimY > 0) {
        if (mDimX < 1) {
            ALOGE("X dimension required when Y is present.");
        }
    }
    if (mDimFaces) {
        if (mDimY < 1) {
            ALOGE("Cube maps require 2D Types.");
        }
    }

    uint32_t nativeYuv;
    switch(mYuvFormat) {
    case(RS_YUV_YV12):
        nativeYuv = HAL_PIXEL_FORMAT_YV12;
        break;
    case (RS_YUV_NV21):
        nativeYuv = HAL_PIXEL_FORMAT_YCrCb_420_SP;
        break;
    default:
        nativeYuv = 0;
    }

    void * id = RS::dispatch->TypeCreate(mRS->getContext(), mElement->getID(), mDimX, mDimY, mDimZ,
                                         mDimMipmaps, mDimFaces, 0);
    Type *t = new Type(id, mRS);
    t->mElement = mElement;
    t->mDimX = mDimX;
    t->mDimY = mDimY;
    t->mDimZ = mDimZ;
    t->mDimMipmaps = mDimMipmaps;
    t->mDimFaces = mDimFaces;

    t->calcElementCount();
    return t;
}

