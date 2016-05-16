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

package signature.compare.model.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import signature.compare.model.IDelta;
import signature.compare.model.DeltaType;

public abstract class SigDelta<T> implements IDelta<T> {
    private T from;
    private T to;

    public SigDelta(T from, T to) {
        this.from = from;
        this.to = to;
    }

    public final T getFrom() {
        return from;
    }

    public final T getTo() {
        return to;
    }

    public final DeltaType getType() {
        if (from == null && to != null) {
            return DeltaType.ADDED;
        }
        if (from != null && to == null) {
            return DeltaType.REMOVED;
        }
        return DeltaType.CHANGED;
    }

    private static <T extends IDelta<?>> Set<T> getDeltas(Set<T> deltas,
            DeltaType type) {
        Set<T> addedElements = new HashSet<T>();
        for (T delta : deltas) {
            if (type.equals(delta.getType())) {
                addedElements.add(delta);
            }
        }
        return addedElements;
    }

    public static <T extends IDelta<?>> Set<T> getAdded(Set<T> deltas) {
        return getDeltas(deltas, DeltaType.ADDED);
    }

    public static <T extends IDelta<?>> Set<T> getRemoved(Set<T> deltas) {
        return getDeltas(deltas, DeltaType.REMOVED);
    }

    public static <T extends IDelta<?>> Set<T> getChanged(Set<T> deltas) {
        return getDeltas(deltas, DeltaType.CHANGED);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(":\n");

        List<Field> allFields = new LinkedList<Field>();

        Class<?> actualClass = getClass();

        // add all fields / super classes also
        do {
            allFields.addAll(Arrays.asList(actualClass.getDeclaredFields()));
            actualClass = actualClass.getSuperclass();
        } while (actualClass != Object.class);

        builder.append("from: ");
        builder.append(from);
        builder.append("\nto:    ");
        builder.append(to);
        builder.append("\n");
        try {
            for (Field field : allFields) {
                if (!ignore.contains(field.getName())) {
                    field.setAccessible(true);
                    Object delta = field.get(this);
                    if (delta != null) {
                        builder.append(field.getName());
                        builder.append(":\n");
                        builder.append(delta);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    private static Set<String> ignore = new HashSet<String>();
    {
        ignore.add("from");
        ignore.add("to");
        ignore.add("reason");
        ignore.add("ignore"); // =)
    }
}
