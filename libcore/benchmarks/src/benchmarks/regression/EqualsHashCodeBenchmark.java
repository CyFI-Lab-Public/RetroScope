/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.regression;

import com.google.caliper.Param;
import com.google.caliper.SimpleBenchmark;
import java.net.URI;
import java.net.URL;

public final class EqualsHashCodeBenchmark extends SimpleBenchmark {
    private enum Type {
        URI() {
            @Override Object newInstance(String text) throws Exception {
                return new URI(text);
            }
        },
        URL() {
            @Override Object newInstance(String text) throws Exception {
                return new URL(text);
            }
        };
        abstract Object newInstance(String text) throws Exception;
    }

    @Param Type type;

    Object a1;
    Object a2;
    Object b1;
    Object b2;

    @Override protected void setUp() throws Exception {
        a1 = type.newInstance("https://mail.google.com/mail/u/0/?shva=1#inbox");
        a2 = type.newInstance("https://mail.google.com/mail/u/0/?shva=1#inbox");
        b1 = type.newInstance("http://developer.android.com/reference/java/net/URI.html");
        b2 = type.newInstance("http://developer.android.com/reference/java/net/URI.html");
    }

    public void timeEquals(int reps) {
        for (int i = 0; i < reps; i+=3) {
            a1.equals(b1);
            a1.equals(a2);
            b1.equals(b2);
        }
    }

    public void timeHashCode(int reps) {
        for (int i = 0; i < reps; i+=2) {
            a1.hashCode();
            b1.hashCode();
        }
    }
}
