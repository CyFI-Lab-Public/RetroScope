/*
 * Copyright (C) 2009 The Android Open Source Project
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

package dex.structure;

import java.util.List;

/**
 * {@code DexAnnotation} represents an annotation.
 */
public interface DexAnnotation {

    /**
     * {@code Visibility} indicates the retention of a {@code DexAnnotation}.
     */
    enum Visibility {
        /**
         * Intended only to be visible at build time (e.g., during compilation
         * of other code).
         */
        VISIBILITY_BUILD((byte) 0x00),
        /**
         * Intended to be visible at runtime. FIXME missing words in spec
         */
        VISIBILITY_RUNTIME((byte) 0X01),
        /**
         * Intended to be visible at runtime, but only to the underlying system
         * (and not to regular user code). FIXME missing words in spec
         */
        VISIBILITY_SYSTEM((byte) 0x02);

        @SuppressWarnings("unused")
        private byte value;

        private Visibility(byte value) {
            this.value = value;
        }

        /**
         * Returns the {@code Visibility} identified by the given {@code byte}.
         * pre: 0 <= {@code value} <=2
         * 
         * @param value
         *            the {@code byte} value which identifies a {@code
         *            Visibility}
         * @return the {@code Visibility} for the given {@code byte}
         */
        public static Visibility get(byte value) {
            // FIXME loop and compare instead of switch?
            switch (value) {
            case 0x00:
                return VISIBILITY_BUILD;
            case 0x01:
                return VISIBILITY_RUNTIME;
            case 0x02:
                return VISIBILITY_SYSTEM;
            default:
                throw new IllegalArgumentException("Visibility doesn't exist!");
            }
        }
    }

    /**
     * Returns the {@code Visibility} of this {@code DexAnnotation}.
     * 
     * @return the {@code Visibility} of this {@code DexAnnotation}
     */
    Visibility getVisibility();

    /**
     * Returns the attributes of this {@code DexAnnotation}.
     * 
     * @return the attributes of this {@code DexAnnotation}
     */
    List<DexAnnotationAttribute> getAttributes();

    /**
     * Returns the class name of this {@code DexAnnotation}.
     * 
     * @return the class name of this {@code DexAnnotation}
     */
    String getTypeName();
}
