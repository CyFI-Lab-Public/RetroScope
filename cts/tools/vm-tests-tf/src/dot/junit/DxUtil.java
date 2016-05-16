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

package dot.junit; 

public class DxUtil {
    private static boolean isDalvik = false;
    
    static {
        /**
         * whether in case of a failure, also ClassNotFoundException is accepted.
         * this makes sense for invalid classes that got rejected by the dx tools
         * and thus do not exist in .dex format, so that this class missing means a
         * the expected verify error (though at the dx tool level)
         */
//        String acnfS = System.getProperty("acceptCNF");
//        isDalvik = (acnfS != null && acnfS.equals("true"));
        //System.out.println("@DX:DxUtil:isDalik="+isDalvik);
    }
    
    public static void checkVerifyException(Throwable t) {
        // the dalvik vm and other vm handle verify errors differently (see the dalvik verifier)
        // the dalvik vm only throws a VerifyError, whereas other vm can throw all subclasses of
        // LinkageError:
        // - ClassCircularityError
        // - ClassFormatError
        // - ExceptionInInitializerError
        // - IncompatibleClassChangeError
        // - NoClassDefFoundError
        // - UnsatisfiedLinkError
        // - VerifyError

        // in case we are testing the dalvik, we also accept a ClassNotFoundException, 
        // since that may happen when a verify error was thrown by the dx tool and thus no
        // classes.dex was written at all. 
        //System.out.println("@dx:debug:isDalvik:"+isDalvik);
        /*
        if ((t instanceof VerifyError || 
                (isDalvik && t instanceof ClassNotFoundException) || 
                (!isDalvik && !(t instanceof NoClassDefFoundError) 
                        && t instanceof LinkageError))) {
                // ok, this is what we expected
            System.out.println("@dx:debug:vfe-ok: vfe was:"+t.getClass().getName()+", msg:"+t.getMessage());
            return;
        } else {
            throw new RuntimeException("test did not cause the expected verify error, but:"+t.getClass().getName()+", msg:"+t.getMessage());
        }
*/
        if (t instanceof VerifyError || t instanceof java.lang.IncompatibleClassChangeError ||t instanceof ClassNotFoundException) {
                // ok, this is what we expected
        } else {
            throw new RuntimeException("VerifyError expected", t);
        }

    }
}
