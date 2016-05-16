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

#include <new>
#include <cutils/atomic.h>
#include <cutils/atomic-inline.h> // for android_memory_barrier()
#include <media/SingleStateQueue.h>

namespace android {

template<typename T> SingleStateQueue<T>::Mutator::Mutator(Shared *shared)
    : mSequence(0), mShared((Shared *) shared)
{
    // exactly one of Mutator and Observer must initialize, currently it is Observer
    //shared->init();
}

template<typename T> int32_t SingleStateQueue<T>::Mutator::push(const T& value)
{
    Shared *shared = mShared;
    int32_t sequence = mSequence;
    sequence++;
    android_atomic_acquire_store(sequence, &shared->mSequence);
    shared->mValue = value;
    sequence++;
    android_atomic_release_store(sequence, &shared->mSequence);
    mSequence = sequence;
    // consider signalling a futex here, if we know that observer is waiting
    return sequence;
}

template<typename T> bool SingleStateQueue<T>::Mutator::ack()
{
    return mShared->mAck - mSequence == 0;
}

template<typename T> bool SingleStateQueue<T>::Mutator::ack(int32_t sequence)
{
    // this relies on 2's complement rollover to detect an ancient sequence number
    return mShared->mAck - sequence >= 0;
}

template<typename T> SingleStateQueue<T>::Observer::Observer(Shared *shared)
    : mSequence(0), mSeed(1), mShared((Shared *) shared)
{
    // exactly one of Mutator and Observer must initialize, currently it is Observer
    shared->init();
}

template<typename T> bool SingleStateQueue<T>::Observer::poll(T& value)
{
    Shared *shared = mShared;
    int32_t before = shared->mSequence;
    if (before == mSequence) {
        return false;
    }
    for (int tries = 0; ; ) {
        const int MAX_TRIES = 5;
        if (before & 1) {
            if (++tries >= MAX_TRIES) {
                return false;
            }
            before = shared->mSequence;
        } else {
            android_memory_barrier();
            T temp = shared->mValue;
            int32_t after = android_atomic_release_load(&shared->mSequence);
            if (after == before) {
                value = temp;
                shared->mAck = before;
                mSequence = before;
                return true;
            }
            if (++tries >= MAX_TRIES) {
                return false;
            }
            before = after;
        }
    }
}

#if 0
template<typename T> SingleStateQueue<T>::SingleStateQueue(void /*Shared*/ *shared)
{
    ((Shared *) shared)->init();
}
#endif

}   // namespace android

// hack for gcc
#ifdef SINGLE_STATE_QUEUE_INSTANTIATIONS
#include SINGLE_STATE_QUEUE_INSTANTIATIONS
#endif
