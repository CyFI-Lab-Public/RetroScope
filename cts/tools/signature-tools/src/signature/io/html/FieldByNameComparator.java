/*
 * Copyright (C) 2009 The Android Open Source Project
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

package signature.io.html;

import java.util.Comparator;

import signature.compare.model.IMemberDelta;
import signature.model.IField;

public class FieldByNameComparator<T extends IMemberDelta<?>> implements
        Comparator<T> {

    public int compare(T a, T b) {
        assert a.getType() == b.getType();
        IField aField = null;
        IField bField = null;

        // FIXME File javac or jdt bug.
        // Note: Casts are required by javac 1.5.0_16.
        if (a.getFrom() != null) {
            aField = (IField) a.getFrom();
            bField = (IField) b.getFrom();
        } else {
            aField = (IField) a.getTo();
            bField = (IField) b.getTo();
        }
        return aField.getName().compareTo(bField.getName());
    }
}
