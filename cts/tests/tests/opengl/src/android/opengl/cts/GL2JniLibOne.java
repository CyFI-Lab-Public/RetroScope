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

package android.opengl.cts;

public class GL2JniLibOne {
     static {
         System.loadLibrary("opengltest_jni");
     }

     public static native void init(int category, int subcategory, int width, int height);
     public static native void step();
     public static native float[] draw(int category, int subcategory, float[] color);

     public static native int getAttachShaderError();
     public static native int getLoadShaderError();
     public static native int getProgramError();
     public static native int getAttachedShaderCount();
}
