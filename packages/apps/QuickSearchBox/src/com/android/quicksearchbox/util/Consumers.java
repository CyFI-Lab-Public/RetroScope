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

import android.os.Handler;

/**
 * Consumer utilities.
 */
public class Consumers {

    private Consumers() {}

    public static <A extends QuietlyCloseable> void consumeCloseable(Consumer<A> consumer,
            A value) {
        boolean accepted = false;
        try {
            accepted = consumer.consume(value);
        } finally {
            if (!accepted && value != null) value.close();
        }
    }

    public static <A> void consumeAsync(Handler handler,
            final Consumer<A> consumer, final A value) {
        if (handler == null) {
            consumer.consume(value);
        } else {
            handler.post(new Runnable() {
                public void run() {
                    consumer.consume(value);
                }
            });
        }
    }

    public static <A extends QuietlyCloseable> void consumeCloseableAsync(Handler handler,
            final Consumer<A> consumer, final A value) {
        if (handler == null) {
            consumeCloseable(consumer, value);
        } else {
            handler.post(new Runnable() {
                public void run() {
                    consumeCloseable(consumer, value);
                }
            });
        }
    }

    public static <A> Consumer<A> createAsyncConsumer(
            final Handler handler, final Consumer<A> consumer) {
        return new Consumer<A>() {
            public boolean consume(A value) {
                consumeAsync(handler, consumer, value);
                return true;
            }
        };
    }

    public static <A extends QuietlyCloseable> Consumer<A> createAsyncCloseableConsumer(
            final Handler handler, final Consumer<A> consumer) {
        return new Consumer<A>() {
            public boolean consume(A value) {
                consumeCloseableAsync(handler, consumer, value);
                return true;
            }
        };
    }

}
