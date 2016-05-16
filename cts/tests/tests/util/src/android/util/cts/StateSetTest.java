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

package android.util.cts;

import android.R;
import android.test.AndroidTestCase;
import android.util.StateSet;

/**
 * Test StateSet
 */
public class StateSetTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testTrimStateSet() {
        // state set's old size is equal to new size
        int[] stateSet = {1, 2, 3};
        assertEquals(stateSet, StateSet.trimStateSet(stateSet, 3));

        int [] stateSet2 = StateSet.trimStateSet(stateSet, 2);
        assertEquals(2, stateSet2.length);
        for (int i : stateSet2) {
            assertEquals(stateSet2[i - 1], stateSet[i - 1]);
        }
    }

    public void testDump() {
        int[] stateSet = {R.attr.state_window_focused,
                          R.attr.state_pressed,
                          R.attr.state_selected,
                          R.attr.state_focused,
                          R.attr.state_enabled,
                          1234325}; // irrelevant value
        String string = StateSet.dump(stateSet);
        assertEquals("W P S F E ", string);
    }

    public void testStateSetMatches() throws Exception {
         int[] stateSpec1 = new int[2];
         int[] stateSet1 = new int[3];
         // Single states in both sets - match
         stateSpec1[0] = 1;
         stateSet1[0] = 1;
         assertTrue(StateSet.stateSetMatches(stateSpec1, stateSet1));
         // Single states in both sets - non-match
         stateSet1[0] = 2;
         assertFalse(StateSet.stateSetMatches(stateSpec1, stateSet1));
         // Add another state to the spec which the stateSet doesn't match
         stateSpec1[1] = 2;
         assertFalse(StateSet.stateSetMatches(stateSpec1, stateSet1));
         // Add the missing matching element to the stateSet
         stateSet1[1] = 1;
         assertTrue(StateSet.stateSetMatches(stateSpec1, stateSet1));
         // Add an irrelevent state to the stateSpec
         stateSet1[2] = 12345;
         assertTrue(StateSet.stateSetMatches(stateSpec1, stateSet1));


        int[] stateSpec2 = new int[2];
        int[] stateSet2 = new int[2];
         // One element in stateSpec which we must match and one which we must
         // not match.  stateSet only contains the match.
         stateSpec2[0] = 1;
         stateSpec2[1] = -2;
         stateSet2[0] = 1;
         assertTrue(StateSet.stateSetMatches(stateSpec2, stateSet2));
         // stateSet now contains just the element we must not match
         stateSet2[0] = 2;
         assertFalse(StateSet.stateSetMatches(stateSpec2, stateSet2));
         // Add another matching state to the the stateSet.  We still fail
         // because stateSet contains a must-not-match element
         stateSet2[1] = 1;
         assertFalse(StateSet.stateSetMatches(stateSpec2, stateSet2));
         // Switch the must-not-match element in stateSet with a don't care
         stateSet2[0] = 12345;
         assertTrue(StateSet.stateSetMatches(stateSpec2, stateSet2));


        int[] stateSpec3 = new int[2];
        int[] stateSet3 = new int[3];
        // Single states in both sets - match
        stateSpec3[0] = -1;
        stateSet3[0] = 2;
        assertTrue(StateSet.stateSetMatches(stateSpec3, stateSet3));
        // Add another arrelevent state to the stateSet
        stateSet3[1] = 12345;
        assertTrue(StateSet.stateSetMatches(stateSpec3, stateSet3));
        // Single states in both sets - non-match
        stateSet3[0] = 1;
        assertFalse(StateSet.stateSetMatches(stateSpec3, stateSet3));
        // Add another state to the spec which the stateSet doesn't match
        stateSpec3[1] = -2;
        assertFalse(StateSet.stateSetMatches(stateSpec3, stateSet3));
        // Add an irrelevent state to the stateSet
        stateSet3[2] = 12345;
        assertFalse(StateSet.stateSetMatches(stateSpec3, stateSet3));


        int[] stateSpec4 = {-12345, -6789};
        int[] stateSet4 = new int[0];
        assertTrue(StateSet.stateSetMatches(stateSpec4, stateSet4));
        int[] stateSet4b = {0};
        assertTrue(StateSet.stateSetMatches(stateSpec4, stateSet4b));


        int[] stateSpec5 = {12345};
        int[] stateSet5a = new int[0];
        assertFalse(StateSet.stateSetMatches(stateSpec5, stateSet5a));
        int[] stateSet5b = {0};
        assertFalse(StateSet.stateSetMatches(stateSpec5, stateSet5b));


        int[] stateSpec6 = StateSet.WILD_CARD;
        int[] stateSet6a = new int[0];
        assertTrue(StateSet.stateSetMatches(stateSpec6, stateSet6a));
        int[] stateSet6b = {0};
        assertTrue(StateSet.stateSetMatches(stateSpec6, stateSet6b));


        int[] stateSpec7 = new int[3];
        int[] stateSet7 = null;
        //  non-match
        stateSpec7[0] = 1;
        assertFalse(StateSet.stateSetMatches(stateSpec7, stateSet7));
        // non-match
        stateSpec7[1] = -1;
        assertFalse(StateSet.stateSetMatches(stateSpec7, stateSet7));
        // match
        stateSpec7 = StateSet.WILD_CARD;
        assertTrue(StateSet.stateSetMatches(stateSpec7, stateSet7));


        int[] stateSpec8 = new int[2];
        int state1;
        //  match
        stateSpec8[0] = 1;
        state1 = 1;
        assertTrue(StateSet.stateSetMatches(stateSpec8, state1));
        // non-match
        state1 = 2;
        assertFalse(StateSet.stateSetMatches(stateSpec8, state1));
        // add irrelevant must-not-match
        stateSpec8[1] = -12345;
        assertFalse(StateSet.stateSetMatches(stateSpec8, state1));


        int[] stateSpec9 = new int[2];
        int state2;
        //  match
        stateSpec9[0] = -1;
        state2 = 1;
        assertFalse(StateSet.stateSetMatches(stateSpec9, state2));
        // non-match
        state2 = 2;
        assertTrue(StateSet.stateSetMatches(stateSpec9, state2));
        // add irrelevant must-not-match
        stateSpec9[1] = -12345;
        assertTrue(StateSet.stateSetMatches(stateSpec9, state2));


        int[] stateSpec10 = new int[3];
        int state3 = 0;
        //  non-match
        stateSpec10[0] = 1;
        assertFalse(StateSet.stateSetMatches(stateSpec10, state3));
        // non-match
        stateSpec10[1] = -1;
        assertFalse(StateSet.stateSetMatches(stateSpec10, state3));
        // match
        stateSpec10 = StateSet.WILD_CARD;
        assertTrue(StateSet.stateSetMatches(stateSpec10, state3));
    }

}
