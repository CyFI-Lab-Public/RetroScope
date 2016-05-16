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

package com.android.gallery3d.data;

import android.graphics.Rect;

import com.android.gallery3d.common.Utils;

import java.util.StringTokenizer;

public class Face implements Comparable<Face> {
    private String mName;
    private String mPersonId;
    private Rect mPosition;

    public Face(String name, String personId, String rect) {
        mName = name;
        mPersonId = personId;
        Utils.assertTrue(mName != null && mPersonId != null && rect != null);
        StringTokenizer tokenizer = new StringTokenizer(rect);
        mPosition = new Rect();
        while (tokenizer.hasMoreElements()) {
            mPosition.left = Integer.parseInt(tokenizer.nextToken());
            mPosition.top = Integer.parseInt(tokenizer.nextToken());
            mPosition.right = Integer.parseInt(tokenizer.nextToken());
            mPosition.bottom = Integer.parseInt(tokenizer.nextToken());
        }
    }

    public Rect getPosition() {
        return mPosition;
    }

    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Face) {
            Face face = (Face) obj;
            return mPersonId.equals(face.mPersonId);
        }
        return false;
    }

    @Override
    public int compareTo(Face another) {
        return mName.compareTo(another.mName);
    }
}
