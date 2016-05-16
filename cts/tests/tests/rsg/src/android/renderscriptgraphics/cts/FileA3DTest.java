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

package android.renderscriptgraphics.cts;

import java.io.File;

import com.android.cts.stub.R;

import android.renderscript.RSRuntimeException;
import android.renderscript.FileA3D;
import android.renderscript.FileA3D.EntryType;
import android.renderscript.FileA3D.IndexEntry;

public class FileA3DTest extends RSBaseGraphics {

    public void testCreateFromResource() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
    }

    public void testCreateFromAsset() {
        FileA3D model = FileA3D.createFromAsset(mRS, mRes.getAssets(), "sphere.a3d");
        assertTrue(model != null);
    }

    public void testGetIndexEntryCount() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
        assertTrue(model.getIndexEntryCount() == 1);
    }

    public void testGetIndexEntry() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
        assertTrue(model.getIndexEntryCount() == 1);
        assertTrue(model.getIndexEntry(0) != null);
    }

    public void testIndexEntryGetEntryType() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
        assertTrue(model.getIndexEntryCount() == 1);
        FileA3D.IndexEntry entry = model.getIndexEntry(0);
        assertTrue(entry != null);
        assertTrue(entry.getEntryType() == FileA3D.EntryType.MESH);
        boolean isOneOfEntries = false;
        for(FileA3D.EntryType et : FileA3D.EntryType.values()) {
            if (et == entry.getEntryType()) {
                isOneOfEntries = true;
                break;
            }
        }
        assertTrue(isOneOfEntries);
    }

    public void testIndexEntryGetMesh() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
        assertTrue(model.getIndexEntryCount() == 1);
        FileA3D.IndexEntry entry = model.getIndexEntry(0);
        assertTrue(entry != null);
        assertTrue(entry.getEntryType() == FileA3D.EntryType.MESH);
        assertTrue(entry.getMesh() != null);
    }

    public void testIndexEntryGetName() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
        assertTrue(model.getIndexEntryCount() == 1);
        FileA3D.IndexEntry entry = model.getIndexEntry(0);
        assertTrue(entry != null);
        assertTrue(entry.getName() != null);
    }

    public void testIndexEntryGetObject() {
        FileA3D model = FileA3D.createFromResource(mRS, mRes, R.raw.sphere);
        assertTrue(model != null);
        assertTrue(model.getIndexEntryCount() == 1);
        FileA3D.IndexEntry entry = model.getIndexEntry(0);
        assertTrue(entry != null);
        assertTrue(entry.getObject() != null);
    }

    public void testFileA3DEntryType() {
        assertEquals(FileA3D.EntryType.UNKNOWN, FileA3D.EntryType.valueOf("UNKNOWN"));
        assertEquals(FileA3D.EntryType.MESH, FileA3D.EntryType.valueOf("MESH"));
        // Make sure no new enums are added
        assertEquals(2, FileA3D.EntryType.values().length);
    }

    public void testCreateFromFile() {
        File fileDesc = new File("bogusFile");
        try {
            FileA3D model = FileA3D.createFromFile(mRS, fileDesc);
            fail("should throw RSRuntimeException.");
        } catch (RSRuntimeException e) {
        }
        try {
            FileA3D model = FileA3D.createFromFile(mRS, "bogus");
            fail("should throw RSRuntimeException.");
        } catch (RSRuntimeException e) {
        }
    }
}


