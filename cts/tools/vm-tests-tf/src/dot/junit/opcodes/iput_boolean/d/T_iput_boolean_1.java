/*
 * Copyright (C) 2008 The Android Open Source Project
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

package dot.junit.opcodes.iput_boolean.d;

public class T_iput_boolean_1 {
    public  boolean st_i1;
    protected  boolean st_p1;
    private  boolean st_pvt1;
    
    public void run() {
        st_i1 = true;
    }
    
    public  boolean getPvtField()
    {
        return st_pvt1;
    }
}
