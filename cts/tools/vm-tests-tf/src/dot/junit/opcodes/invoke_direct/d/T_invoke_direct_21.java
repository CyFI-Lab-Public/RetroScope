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

package dot.junit.opcodes.invoke_direct.d;

public class T_invoke_direct_21 {

    
    private int test(int a, int b) {
        int i = 999;
        int j = 888;
        int k = 777;
        return a / b;
    }
    
    public int run() {
        int i = 111;
        int j = 222;
        int k = 333;
        test(50, 25);
        
        return 1;
    }
}
