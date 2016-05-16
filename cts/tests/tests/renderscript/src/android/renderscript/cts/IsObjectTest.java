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

package android.renderscript.cts;

import android.renderscript.*;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.util.Log;
import android.view.SurfaceHolder;
import com.android.cts.stub.R;

import junit.framework.Assert;

public class IsObjectTest extends RSBaseCompute {
    int ObjectNum = 1;
    private Allocation mIn;
    private Allocation mOut;

    Element element;
    Type type;
    Allocation allocation;
    Sampler sampler;
    Script script;

    private ScriptC_is_object ms_is_object;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        element = Element.BOOLEAN(mRS);

        type = new Type.Builder(mRS, Element.I8(mRS)).setX(1).create();
        allocation = Allocation.createTyped(mRS, type);
        sampler = new Sampler.Builder(mRS).create();
        script = new ScriptC_is_object(mRS);
        ms_is_object = new ScriptC_is_object(mRS);
    }

    /**
     * is object test
     */
    public void testIsObjectElement() {
        ScriptField__object_element_input filed = new ScriptField__object_element_input(
                mRS, 1);
        ScriptField__object_element_input.Item mItem =
                new ScriptField__object_element_input.Item();
        mItem.element = element;
        filed.set(mItem, 0, true);

        mIn = filed.getAllocation();
        mOut = Allocation.createSized(mRS, Element.I32(mRS), ObjectNum);
        try {
            ms_is_object.forEach_is_object_element(mIn, mOut);
        } catch (RSRuntimeException e) {
            Log.i("compare", "rsIsObject root fail");
        }
        int[] tmpArray = new int[ObjectNum];
        mOut.copyTo(tmpArray);

        Assert.assertTrue("rsIsObject element test fail: " + "Expect 1;value "
                + tmpArray[0], tmpArray[0] == 1);
    }

    public void testIsObjectType() {
        ScriptField__object_type_input filed = new ScriptField__object_type_input(mRS, 1);
        ScriptField__object_type_input.Item mItem = new ScriptField__object_type_input.Item();
        mItem.type = type;
        filed.set(mItem, 0, true);

        mIn = filed.getAllocation();
        mOut = Allocation.createSized(mRS, Element.I32(mRS), ObjectNum);

        try {
            ms_is_object.forEach_is_object_type(mIn, mOut);
        } catch (RSRuntimeException e) {
            Log.i("compare", "rsIsObject root fail");
        }
        int[] tmpArray = new int[ObjectNum];
        mOut.copyTo(tmpArray);

        Assert.assertTrue(
                "rsIsObject type test fail: " + "Expect 1;value " + tmpArray[0],
                tmpArray[0] == 1);
    }

    public void testIsObjectAllocation() {
        ScriptField__object_allocation_input filed = new ScriptField__object_allocation_input(
                mRS, 1);
        ScriptField__object_allocation_input.Item mItem =
                new ScriptField__object_allocation_input.Item();
        mItem.allocation = allocation;
        filed.set(mItem, 0, true);

        mIn = filed.getAllocation();
        mOut = Allocation.createSized(mRS, Element.I32(mRS), ObjectNum);

        try {
            ms_is_object.forEach_is_object_allocation(mIn, mOut);
        } catch (RSRuntimeException e) {
            Log.i("compare", "rsIsObject root fail");
        }
        int[] tmpArray = new int[ObjectNum];
        mOut.copyTo(tmpArray);

        Assert.assertTrue("rsIsObject allocation test fail: " + "Expect 1;value "
                + tmpArray[0], tmpArray[0] == 1);
    }

    public void testIsObjectSampler() {
        ScriptField__object_sampler_input filed = new ScriptField__object_sampler_input(
                mRS, 1);
        ScriptField__object_sampler_input.Item mItem =
                new ScriptField__object_sampler_input.Item();
        mItem.sampler = sampler;
        filed.set(mItem, 0, true);

        mIn = filed.getAllocation();
        mOut = Allocation.createSized(mRS, Element.I32(mRS), ObjectNum);

        try {
            ms_is_object.forEach_is_object_sampler(mIn, mOut);
        } catch (RSRuntimeException e) {
            Log.i("compare", "rsIsObject root fail");
        }
        int[] tmpArray = new int[ObjectNum];
        mOut.copyTo(tmpArray);

        Assert.assertTrue("rsIsObject sampler test fail: " + "Expect 1;value "
                + tmpArray[0], tmpArray[0] == 1);
    }

    public void testIsObjectScript() {
        ScriptField__object_script_input filed = new ScriptField__object_script_input(
                mRS, 1);
        ScriptField__object_script_input.Item mItem = new ScriptField__object_script_input.Item();
        mItem.script = script;
        filed.set(mItem, 0, true);

        mIn = filed.getAllocation();
        mOut = Allocation.createSized(mRS, Element.I32(mRS), ObjectNum);

        try {
            ms_is_object.forEach_is_object_script(mIn, mOut);
        } catch (RSRuntimeException e) {
            Log.i("compare", "rsIsObject root fail");
        }
        int[] tmpArray = new int[ObjectNum];
        mOut.copyTo(tmpArray);

        Assert.assertTrue("rsIsObject script test fail: " + "Expect 1;value "
                + tmpArray[0], tmpArray[0] == 1);
    }
}
