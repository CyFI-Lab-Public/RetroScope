/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.quicksearchbox.util;

/**
 * {@link NowOrLater} class that converts from one type to another.
 */
public abstract class NowOrLaterWrapper<A, B> implements NowOrLater<B> {

    private final NowOrLater<A> mWrapped;

    public NowOrLaterWrapper(NowOrLater<A> wrapped) {
        mWrapped = wrapped;
    }

    public void getLater(final Consumer<? super B> consumer) {
        mWrapped.getLater(new Consumer<A>(){
            public boolean consume(A value) {
                return consumer.consume(get(value));
            }});
    }

    public B getNow() {
        return get(mWrapped.getNow());
    }

    public boolean haveNow() {
        return mWrapped.haveNow();
    }

    /**
     * Perform the appropriate conversion. This will be called once for every call to 
     * {@link #getLater(Consumer)} or {@link #getNow()}. The thread that it's called on will depend
     * on the behaviour of the wrapped object and the caller.
     */
    public abstract B get(A value);

}
