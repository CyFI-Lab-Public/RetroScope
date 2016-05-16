/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.media.cts;

import com.android.cts.media.R;

public class SoundPoolOggTest extends SoundPoolTest {

    @Override
    protected int getSoundA() {
        return R.raw.a_4;
    }

    @Override
    protected int getSoundCs() {
        return R.raw.c_sharp_5;
    }

    @Override
    protected int getSoundE() {
        return R.raw.e_5;
    }

    @Override
    protected int getSoundB() {
        return R.raw.b_5;
    }

    @Override
    protected int getSoundGs() {
        return R.raw.g_sharp_5;
    }

    @Override
    protected String getFileName() {
        return "a_4.ogg";
    }
}
