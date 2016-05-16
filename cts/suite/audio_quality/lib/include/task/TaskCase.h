/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


#ifndef CTSAUDIO_TASKCASE_H
#define CTSAUDIO_TASKCASE_H

#include <stdint.h>
#include <map>
#include <list>
#include <utility>
#include <utils/String8.h>
#include <utils/StrongPointer.h>
#include "Log.h"
#include "audio/Buffer.h"
#include "TaskGeneric.h"

class RemoteAudio;
class ClientInterface;

class TaskCase: public TaskGeneric {
public:
    TaskCase();
    virtual ~TaskCase();
    virtual bool addChild(TaskGeneric* child);
    virtual TaskGeneric::ExecutionResult run();

    bool getCaseName(android::String8& name) const;

    bool registerBuffer(const android::String8& name, android::sp<Buffer>& buffer);
    // update already existing buffer. Actually the old buffer will be deleted.
    bool updateBuffer(const android::String8& name, android::sp<Buffer>& buffer);
    /// find buffer with given id. sp will be NULL if not found
    android::sp<Buffer> findBuffer(const android::String8& name);
    typedef std::pair<android::String8, android::sp<Buffer> > BufferPair;
    /// find all buffers with given regular expression. returns NULL if not found
    std::list<BufferPair>*  findAllBuffers(const android::String8& re);

    android::sp<RemoteAudio>& getRemoteAudio();

    class Value {
    public:
        enum Type {
            ETypeDouble,
            ETypeI64
        };
        inline Value(): mType(ETypeDouble) {};
        inline Value(Type type): mType(type) {};
        inline Value(double val): mType(ETypeDouble) {
            setDouble(val);
        };
        inline Value(int64_t val): mType(ETypeI64) {
            setInt64(val);
        };
        inline Type getType() {
            return mType;
        };
        inline void setType(Type type) {
            mType = type;
        };
        inline void setDouble(double val) {
            mValue[0] = val;
            mType = ETypeDouble;
            //LOGD("Value set %f 0x%x", val, this);
        };
        inline double getDouble() {
            //LOGD("Value get %f 0x%x", mValue[0], this);
            return mValue[0];
        };
        inline void setInt64(int64_t val) {
            int64_t* data = reinterpret_cast<int64_t*>(mValue);
            data[0] = val;
            mType = ETypeI64;
            //LOGD("Value set %lld 0x%x", val, this);
        }
        inline int64_t getInt64() {
            int64_t* data = reinterpret_cast<int64_t*>(mValue);
            //LOGD("Value get %lld 0x%x", data[0], this);
            return data[0];
        }
        void* getPtr() {
            return mValue;
        }
        bool operator ==(const Value& b) const {
            return ((mValue[0] == b.mValue[0]) && (mType == b.mType));
        };

    private:
        double mValue[1];
        Type mType;
    };

    bool registerValue(const android::String8& name, Value& val);
    bool updateValue(const android::String8& name, Value& val);
    bool findValue(const android::String8& name, Value& val);
    typedef std::pair<android::String8, Value> ValuePair;
    /// find all Values with given regular expression. returns NULL if not found
    std::list<ValuePair>*  findAllValues(const android::String8& re);

    bool registerIndex(const android::String8& name, int value = -1);
    bool updateIndex(const android::String8& name, int value);
    bool findIndex(const android::String8& name, int& val);
    typedef std::pair<android::String8, int> IndexPair;
    /// find all Indices with given regular expression. returns NULL if not found
    std::list<IndexPair>*  findAllIndices(const android::String8& re);

    /**
     * Translate variable name like $i into index variable
     * All xxxValue and xxxBuffer calls do translation inside.
     */
    bool translateVarName(const android::String8& orig, android::String8& translated);

    void setDetails(android::String8 details);
    const android::String8& getDetails() const;
private:
    void releaseRemoteAudio();

private:
    std::map<android::String8, android::sp<Buffer> > mBufferList;
    std::map<android::String8, int> mIndexList;
    std::map<android::String8, Value> mValueList;
    ClientInterface* mClient;
    android::String8 mDetails;
};


#endif // CTSAUDIO_TASKCASE_H
