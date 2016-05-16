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

package com.replica.replicaisland;

public class GameComponentPool extends TObjectPool<GameComponent> {
    public Class<?> objectClass;
    
    public GameComponentPool(Class<?> type) {
        super();
        objectClass = type;
        fill();
    }
    
    public GameComponentPool(Class<?> type, int size) {
        super(size);
        objectClass = type;
        fill();
    }
    
    @Override
    protected void fill() {
        if (objectClass != null) {
            for (int x = 0; x < getSize(); x++) {
                try {
                    getAvailable().add(objectClass.newInstance());
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}



