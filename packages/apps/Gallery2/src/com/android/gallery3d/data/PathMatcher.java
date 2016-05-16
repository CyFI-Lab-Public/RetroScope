/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.HashMap;

public class PathMatcher {
    public static final int NOT_FOUND = -1;

    private ArrayList<String> mVariables = new ArrayList<String>();
    private Node mRoot = new Node();

    public PathMatcher() {
        mRoot = new Node();
    }

    public void add(String pattern, int kind) {
        String[] segments = Path.split(pattern);
        Node current = mRoot;
        for (int i = 0; i < segments.length; i++) {
            current = current.addChild(segments[i]);
        }
        current.setKind(kind);
    }

    public int match(Path path) {
        String[] segments = path.split();
        mVariables.clear();
        Node current = mRoot;
        for (int i = 0; i < segments.length; i++) {
            Node next = current.getChild(segments[i]);
            if (next == null) {
                next = current.getChild("*");
                if (next != null) {
                    mVariables.add(segments[i]);
                } else {
                    return NOT_FOUND;
                }
            }
            current = next;
        }
        return current.getKind();
    }

    public String getVar(int index) {
        return mVariables.get(index);
    }

    public int getIntVar(int index) {
        return Integer.parseInt(mVariables.get(index));
    }

    public long getLongVar(int index) {
        return Long.parseLong(mVariables.get(index));
    }

    private static class Node {
        private HashMap<String, Node> mMap;
        private int mKind = NOT_FOUND;

        Node addChild(String segment) {
            if (mMap == null) {
                mMap = new HashMap<String, Node>();
            } else {
                Node node = mMap.get(segment);
                if (node != null) return node;
            }

            Node n = new Node();
            mMap.put(segment, n);
            return n;
        }

        Node getChild(String segment) {
            if (mMap == null) return null;
            return mMap.get(segment);
        }

        void setKind(int kind) {
            mKind = kind;
        }

        int getKind() {
            return mKind;
        }
    }
}
