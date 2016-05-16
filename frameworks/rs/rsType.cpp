/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "rsContext.h"

#if !defined(RS_SERVER) && !defined(RS_COMPATIBILITY_LIB)
#include "system/graphics.h"
#endif

#ifdef RS_COMPATIBILITY_LIB
#include "rsCompatibilityLib.h"
#endif

using namespace android;
using namespace android::renderscript;

Type::Type(Context *rsc) : ObjectBase(rsc) {
    memset(&mHal, 0, sizeof(mHal));
    mDimLOD = false;
}

void Type::preDestroy() const {
    for (uint32_t ct = 0; ct < mRSC->mStateType.mTypes.size(); ct++) {
        if (mRSC->mStateType.mTypes[ct] == this) {
            mRSC->mStateType.mTypes.removeAt(ct);
            break;
        }
    }
}

Type::~Type() {
    clear();
}

void Type::clear() {
    if (mHal.state.lodCount) {
        delete [] mHal.state.lodDimX;
        delete [] mHal.state.lodDimY;
        delete [] mHal.state.lodDimZ;
    }
    mElement.clear();
    memset(&mHal, 0, sizeof(mHal));
}

TypeState::TypeState() {
}

TypeState::~TypeState() {
    rsAssert(!mTypes.size());
}

void Type::compute() {
    uint32_t oldLODCount = mHal.state.lodCount;
    if (mDimLOD) {
        uint32_t l2x = rsFindHighBit(mHal.state.dimX) + 1;
        uint32_t l2y = rsFindHighBit(mHal.state.dimY) + 1;
        uint32_t l2z = rsFindHighBit(mHal.state.dimZ) + 1;

        mHal.state.lodCount = rsMax(l2x, l2y);
        mHal.state.lodCount = rsMax(mHal.state.lodCount, l2z);
    } else {
        mHal.state.lodCount = 1;
    }
    if (mHal.state.lodCount != oldLODCount) {
        if (oldLODCount) {
            delete [] mHal.state.lodDimX;
            delete [] mHal.state.lodDimY;
            delete [] mHal.state.lodDimZ;
        }
        mHal.state.lodDimX = new uint32_t[mHal.state.lodCount];
        mHal.state.lodDimY = new uint32_t[mHal.state.lodCount];
        mHal.state.lodDimZ = new uint32_t[mHal.state.lodCount];
    }

    uint32_t tx = mHal.state.dimX;
    uint32_t ty = mHal.state.dimY;
    uint32_t tz = mHal.state.dimZ;
    mCellCount = 0;
    for (uint32_t lod=0; lod < mHal.state.lodCount; lod++) {
        mHal.state.lodDimX[lod] = tx;
        mHal.state.lodDimY[lod] = ty;
        mHal.state.lodDimZ[lod]  = tz;
        mCellCount += tx * rsMax(ty, 1u) * rsMax(tz, 1u);
        if (tx > 1) tx >>= 1;
        if (ty > 1) ty >>= 1;
        if (tz > 1) tz >>= 1;
    }

    if (mHal.state.faces) {
        mCellCount *= 6;
    }
#ifndef RS_SERVER
    // YUV only supports basic 2d
    // so we can stash the plane pointers in the mipmap levels.
    if (mHal.state.dimYuv) {
        mHal.state.lodDimX[1] = mHal.state.lodDimX[0] / 2;
        mHal.state.lodDimY[1] = mHal.state.lodDimY[0] / 2;
        mHal.state.lodDimX[2] = mHal.state.lodDimX[0] / 2;
        mHal.state.lodDimY[2] = mHal.state.lodDimY[0] / 2;
        mCellCount += mHal.state.lodDimX[1] * mHal.state.lodDimY[1];
        mCellCount += mHal.state.lodDimX[2] * mHal.state.lodDimY[2];

        switch(mHal.state.dimYuv) {
        case HAL_PIXEL_FORMAT_YV12:
            break;
        case HAL_PIXEL_FORMAT_YCrCb_420_SP:  // NV21
            mHal.state.lodDimX[1] = mHal.state.lodDimX[0];
            break;
#ifndef RS_COMPATIBILITY_LIB
        case HAL_PIXEL_FORMAT_YCbCr_420_888:
            break;
#endif
        default:
            rsAssert(0);
        }
    }
#endif
    mHal.state.element = mElement.get();
}

void Type::dumpLOGV(const char *prefix) const {
    char buf[1024];
    ObjectBase::dumpLOGV(prefix);
    ALOGV("%s   Type: x=%u y=%u z=%u mip=%i face=%i", prefix,
                                                      mHal.state.dimX,
                                                      mHal.state.dimY,
                                                      mHal.state.dimZ,
                                                      mHal.state.lodCount,
                                                      mHal.state.faces);
    snprintf(buf, sizeof(buf), "%s element: ", prefix);
    mElement->dumpLOGV(buf);
}

void Type::serialize(Context *rsc, OStream *stream) const {
    // Need to identify ourselves
    stream->addU32((uint32_t)getClassId());
    stream->addString(getName());

    mElement->serialize(rsc, stream);

    stream->addU32(mHal.state.dimX);
    stream->addU32(mHal.state.dimY);
    stream->addU32(mHal.state.dimZ);

    stream->addU8((uint8_t)(mHal.state.lodCount ? 1 : 0));
    stream->addU8((uint8_t)(mHal.state.faces ? 1 : 0));
}

Type *Type::createFromStream(Context *rsc, IStream *stream) {
    // First make sure we are reading the correct object
    RsA3DClassID classID = (RsA3DClassID)stream->loadU32();
    if (classID != RS_A3D_CLASS_ID_TYPE) {
        ALOGE("type loading skipped due to invalid class id\n");
        return NULL;
    }

    const char *name = stream->loadString();

    Element *elem = Element::createFromStream(rsc, stream);
    if (!elem) {
        return NULL;
    }

    uint32_t x = stream->loadU32();
    uint32_t y = stream->loadU32();
    uint32_t z = stream->loadU32();
    uint8_t lod = stream->loadU8();
    uint8_t faces = stream->loadU8();
    Type *type = Type::getType(rsc, elem, x, y, z, lod != 0, faces !=0, 0);
    elem->decUserRef();

    delete [] name;
    return type;
}

bool Type::getIsNp2() const {
    uint32_t x = getDimX();
    uint32_t y = getDimY();
    uint32_t z = getDimZ();

    if (x && (x & (x-1))) {
        return true;
    }
    if (y && (y & (y-1))) {
        return true;
    }
    if (z && (z & (z-1))) {
        return true;
    }
    return false;
}

ObjectBaseRef<Type> Type::getTypeRef(Context *rsc, const Element *e,
                                     uint32_t dimX, uint32_t dimY, uint32_t dimZ,
                                     bool dimLOD, bool dimFaces, uint32_t dimYuv) {
    ObjectBaseRef<Type> returnRef;

    TypeState * stc = &rsc->mStateType;

    ObjectBase::asyncLock();
    for (uint32_t ct=0; ct < stc->mTypes.size(); ct++) {
        Type *t = stc->mTypes[ct];
        if (t->getElement() != e) continue;
        if (t->getDimX() != dimX) continue;
        if (t->getDimY() != dimY) continue;
        if (t->getDimZ() != dimZ) continue;
        if (t->getDimLOD() != dimLOD) continue;
        if (t->getDimFaces() != dimFaces) continue;
        if (t->getDimYuv() != dimYuv) continue;
        returnRef.set(t);
        ObjectBase::asyncUnlock();
        return returnRef;
    }
    ObjectBase::asyncUnlock();


    Type *nt = new Type(rsc);
    nt->mDimLOD = dimLOD;
    returnRef.set(nt);
    nt->mElement.set(e);
    nt->mHal.state.dimX = dimX;
    nt->mHal.state.dimY = dimY;
    nt->mHal.state.dimZ = dimZ;
    nt->mHal.state.faces = dimFaces;
    nt->mHal.state.dimYuv = dimYuv;
    nt->compute();

    ObjectBase::asyncLock();
    stc->mTypes.push(nt);
    ObjectBase::asyncUnlock();

    return returnRef;
}

ObjectBaseRef<Type> Type::cloneAndResize1D(Context *rsc, uint32_t dimX) const {
    return getTypeRef(rsc, mElement.get(), dimX,
                      getDimY(), getDimZ(), getDimLOD(), getDimFaces(), getDimYuv());
}

ObjectBaseRef<Type> Type::cloneAndResize2D(Context *rsc,
                              uint32_t dimX,
                              uint32_t dimY) const {
    return getTypeRef(rsc, mElement.get(), dimX, dimY,
                      getDimZ(), getDimLOD(), getDimFaces(), getDimYuv());
}


void Type::incRefs(const void *ptr, size_t ct, size_t startOff) const {
    const uint8_t *p = static_cast<const uint8_t *>(ptr);
    const Element *e = mHal.state.element;
    uint32_t stride = e->getSizeBytes();

    p += stride * startOff;
    while (ct > 0) {
        e->incRefs(p);
        ct--;
        p += stride;
    }
}


void Type::decRefs(const void *ptr, size_t ct, size_t startOff) const {
    if (!mHal.state.element->getHasReferences()) {
        return;
    }
    const uint8_t *p = static_cast<const uint8_t *>(ptr);
    const Element *e = mHal.state.element;
    uint32_t stride = e->getSizeBytes();

    p += stride * startOff;
    while (ct > 0) {
        e->decRefs(p);
        ct--;
        p += stride;
    }
}


//////////////////////////////////////////////////
//
namespace android {
namespace renderscript {

RsType rsi_TypeCreate(Context *rsc, RsElement _e, uint32_t dimX,
                     uint32_t dimY, uint32_t dimZ, bool mips, bool faces, uint32_t yuv) {
    Element *e = static_cast<Element *>(_e);

    return Type::getType(rsc, e, dimX, dimY, dimZ, mips, faces, yuv);
}

}
}

extern "C" void rsaTypeGetNativeData(RsContext con, RsType type, uintptr_t *typeData, uint32_t typeDataSize) {
    rsAssert(typeDataSize == 6);
    // Pack the data in the follofing way mHal.state.dimX; mHal.state.dimY; mHal.state.dimZ;
    // mHal.state.lodCount; mHal.state.faces; mElement; into typeData
    Type *t = static_cast<Type *>(type);

    (*typeData++) = t->getDimX();
    (*typeData++) = t->getDimY();
    (*typeData++) = t->getDimZ();
    (*typeData++) = t->getDimLOD() ? 1 : 0;
    (*typeData++) = t->getDimFaces() ? 1 : 0;
    (*typeData++) = (uintptr_t)t->getElement();
    t->getElement()->incUserRef();
}
