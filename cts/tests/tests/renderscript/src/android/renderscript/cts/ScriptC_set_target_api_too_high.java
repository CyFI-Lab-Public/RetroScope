/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

/*
 * This file is auto-generated. DO NOT MODIFY!
 * The source Renderscript file: set_target_api_16.rs
 */
package android.renderscript.cts;

import android.renderscript.*;
import android.content.res.Resources;

/**
 * @hide
 */
public class ScriptC_set_target_api_too_high extends ScriptC {
    private static final String __rs_resource_name = "set_target_api_too_high";
    // Constructor
    public  ScriptC_set_target_api_too_high(RenderScript rs) {
        this(rs,
             rs.getApplicationContext().getResources(),
             rs.getApplicationContext().getResources().getIdentifier(
                 __rs_resource_name, "raw",
                 rs.getApplicationContext().getPackageName()));
    }

    public  ScriptC_set_target_api_too_high(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
    }

    private final static int mExportFuncIdx_check = 0;
    public void invoke_check(int version) {
        FieldPacker check_fp = new FieldPacker(4);
        check_fp.addI32(version);
        invoke(mExportFuncIdx_check, check_fp);
    }

}

