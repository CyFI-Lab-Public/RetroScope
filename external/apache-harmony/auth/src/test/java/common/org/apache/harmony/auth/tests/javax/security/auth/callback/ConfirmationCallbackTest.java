/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Maxim V. Makarov
*/

package org.apache.harmony.auth.tests.javax.security.auth.callback;

import javax.security.auth.callback.ConfirmationCallback;

import junit.framework.TestCase;

/**
 * Tests ConfirmationCallback class
 */

public class ConfirmationCallbackTest extends TestCase {

    ConfirmationCallback cc;
     int mt[] = { ConfirmationCallback.INFORMATION,
                        ConfirmationCallback.WARNING, 
                        ConfirmationCallback.ERROR 
                        };
     int ot[] = { ConfirmationCallback.YES_NO_OPTION,
                        ConfirmationCallback.YES_NO_CANCEL_OPTION ,
                        ConfirmationCallback.OK_CANCEL_OPTION
                        };
     int dopt[] = { ConfirmationCallback.YES, 
                        ConfirmationCallback.NO,
                        ConfirmationCallback.CANCEL, 
                        ConfirmationCallback.OK 
                        };

    /**
     *  Ctor test  
     */
    public final void testConfirmationCallback_01() throws IllegalArgumentException {
        try {
            for (int i = 0; i < mt.length; i++) {
                cc = new ConfirmationCallback(mt[i], ot[1], dopt[1]);
            }
        } catch (IllegalArgumentException e){}
    }
    
    /**
     * Expected IAE, if messageType is not either INFORMATION, WARNING,ERROR; 
     * if optionType is not either YES_NO_OPTION, YES_NO_CANCEL_OPTION,OK_CANCEL_OPTION;
     * if defaultOption does not lie within the array boundaries of options.
     */
    public final void testConfirmationCallback_02() throws IllegalArgumentException{
        try {
            cc = new ConfirmationCallback(3, ot[1], dopt[1]);
            fail("Message type should be either INFORMATION, WARNING or ERROR");
        }catch (IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(mt[1], 3, dopt[1]);
            fail("Option type should be either YES_NO_OPTION, YES_NO_CANCEL_OPTION or OK_CANCEL_OPTION");
        } catch (IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(mt[1], ot[1], 4);
            fail("Default option type should be either YES, NO, CANCEL or OK");
        } catch (IllegalArgumentException e){}
    }
    

    /**
     * Expected IAE, if optionType is YES_NO_OPTION and defaultOption is not YES/NO,
     * else ok.     
     */
    
    public final void testConfirmationCallback_03() throws IllegalArgumentException {
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[0], dopt[0]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[0], dopt[1]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[0], dopt[2]);
            fail("1. If option type is YES_NO_OPTION then default option should be YES/NO");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[0], dopt[3]);
            fail("2. If option type is YES_NO_OPTION then default option should be YES/NO ");
        } catch(IllegalArgumentException e){}
        
}

    
    /**
     * Expected IAE, if optionType is YES_NO_CANCEL_OPTION and defaultOption is not YES/NO/CANCEL,
     * else ok.     
     */
    public final void testConfirmationCallback_04() throws IllegalArgumentException {
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[1], dopt[0]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[1], dopt[1]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[1], dopt[2]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[1], dopt[3]);
            fail("1. If option type is YES_NO_CANCEL_OPTION then default option should be YES/NO/CANCEL");
        } catch(IllegalArgumentException e){}

    }

    /**
     * Expected IAE, if optionType is OK_CANCEL_OPTION and defaultOption is not CANCEL/OK
     * else ok.     
     */
    public final void testConfirmationCallback_05() throws IllegalArgumentException {
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[2], dopt[0]);
            fail("1. If option type is OK_CANCEL_OPTION then default option should be CANCEL/OK");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[2], dopt[1]);
            fail("2. If option type is OK_CANCEL_OPTION then default option should be CANCEL/OK");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[2], dopt[2]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ot[2], dopt[3]);
        } catch(IllegalArgumentException e){}
    }

    /**
     * Expected IAE, if optionType is null or an empty and 
     * defaultOption type does not lie within the array boundaries of options,
     * else ok.     
     */
    
    public final void testConfirmationCallback_06() {
        String[] opt = {"CONTINUE", "ABORT"};
        String[] opt1 = {"START", ""};
        String[] opt2 = {"START", null};
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, opt, 1);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, opt1, 1);
           fail("1. Option type should not be null and empty ");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, opt2, 1);
          fail("2. Option type should not be null and empty ");
      } catch(IllegalArgumentException e){}
      try {
          cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION, opt, 3);
          fail("Default Option does not lie within the array boundaries of options ");
        } catch(IllegalArgumentException e){
        }
    }
    
    /**
     * Expected IAE, if prompt is null or an empty, else ok.
     */ 
     public final void testConfirmationCallback_07() {
        try {
            cc = new ConfirmationCallback("prompt", ConfirmationCallback.INFORMATION, ot[0], dopt[0]);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback("", ConfirmationCallback.INFORMATION, ot[0], dopt[0]);
            fail("2. Prompt should not be null and empty ");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(null, ConfirmationCallback.INFORMATION, ot[0], dopt[0]);
            fail("2. Prompt should not be null and empty ");
        } catch(IllegalArgumentException e){}
    }

     /**
      * Expected IAE, if optionType and prompt is null or an empty, else ok.
      */ 
     public final void testConfirmationCallback_08() {
        String[] opt = {"CONTINUE", "ABORT"};
        String[] opt1 = {"START", ""};
        String[] opt2 = {"START", null};
        try {
            cc = new ConfirmationCallback("prompt", ConfirmationCallback.INFORMATION, opt, 1);
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback("prompt", ConfirmationCallback.INFORMATION, opt1, 1);
           fail("1. Option type should not be null and empty ");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback("prompt", ConfirmationCallback.INFORMATION, opt2, 1);
          fail("2. Option type should not be null and empty ");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback("", ConfirmationCallback.INFORMATION, opt, 1);
            fail("3. Prompt should not be null and empty ");
        } catch(IllegalArgumentException e){}
        try {
            cc = new ConfirmationCallback(null, ConfirmationCallback.INFORMATION, opt, 1);
            fail("4. Prompt should not be null and empty ");
        } catch(IllegalArgumentException e){
        }

    }

    /**
     * test of methods getOptionType(), getDefaultOption(), getMessageType()
     * getOptions(), set/getSelectedIndex
     */
    public final void testConfirmationCallback_09() throws IllegalArgumentException {
        String[] opt = {"CONTINUE", "ABORT"};
        String[] s;

            cc = new ConfirmationCallback("prompt", mt[0], opt, 1);
            assertEquals(1, cc.getDefaultOption());
            assertEquals(ConfirmationCallback.INFORMATION, cc.getMessageType());
            assertEquals(opt,cc.getOptions());
            s = cc.getOptions();
            for (int i = 0; i < opt.length; i++){
                assertEquals(opt[i], s[i]);
            }
            assertEquals("prompt", cc.getPrompt());
            assertEquals(-1, cc.getOptionType());
            assertEquals(ConfirmationCallback.UNSPECIFIED_OPTION, cc.getOptionType());
            assertNotNull(cc.getOptions());
            cc.setSelectedIndex(1);
            assertEquals(1, cc.getSelectedIndex());
    }
    
    /**
     * test of methods getOptions(), set/getSelectedIndex when options is null
     */
    public final void testConfirmationCallback_10() throws IllegalArgumentException {
        
            cc = new ConfirmationCallback("prompt", mt[0], ot[0], dopt[0]);
            assertNull(cc.getOptions());
            cc.setSelectedIndex(1);
            assertEquals(1, cc.getSelectedIndex());
    }
    
    /**
     * if optionType was specified in ctor, then selection represented as YES, NO, OK or CANCEL 
     * i.e. defaultOption, thus expected IAE if selection does not lie within the array 
     * boundaries of options.
     */
    public final void testSetSelection() throws IllegalArgumentException {
        cc = new ConfirmationCallback("prompt", mt[0], ot[0], dopt[0]);
        try {
           cc.setSelectedIndex(3);
           assertEquals(3, cc.getSelectedIndex());
           fail("There is not enough an information about setSelection in java doc");
        }catch (IllegalArgumentException e) {
        } catch (ArrayIndexOutOfBoundsException e){}
        
    }

    /*
     * Regression test for bug in setSelectedIndex
     */
    public final void testSetSelectedIndex() throws Exception {
        cc = new ConfirmationCallback("prompt", mt[0], ot[0], dopt[0]);
        cc.setSelectedIndex(ConfirmationCallback.YES);
    }
}
