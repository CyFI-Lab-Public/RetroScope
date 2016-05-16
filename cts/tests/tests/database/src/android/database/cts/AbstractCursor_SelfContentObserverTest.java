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
package android.database.cts;

import android.database.AbstractCursor;
import android.test.AndroidTestCase;

public class AbstractCursor_SelfContentObserverTest extends AndroidTestCase{

    public void testConstructor() {
        MockAbstractCursor mac = new MockAbstractCursor();
        mac.getMockSelfContentObserver();
    }

    public void testDeliverSelfNotifications() {
        MockAbstractCursor mac = new MockAbstractCursor();
        MockAbstractCursor.MockSelfContentObserver msc = mac.getMockSelfContentObserver();
        assertFalse(msc.deliverSelfNotifications());
    }

    public void testOnChange() {
        MockAbstractCursor mockCursor = new MockAbstractCursor();
        MockAbstractCursor.MockSelfContentObserver msc = mockCursor.getMockSelfContentObserver();
        mockCursor.registerContentObserver(msc);
        // here, the directly call of AbstractCurso#onChange is intended to test
        // SelfContentObserver#onChange
        mockCursor.onChange(false);
        assertTrue(msc.mIsOnChangeCalled);
        assertFalse(msc.mOnChangeResult);

        msc.mIsOnChangeCalled = false;
        mockCursor.onChange(true);
        assertFalse(msc.mIsOnChangeCalled);
        assertFalse(msc.mOnChangeResult);

        msc.mIsOnChangeCalled = false;
        msc.setDeliverSelfNotificationsValue(true);
        mockCursor.onChange(true);
        assertTrue(msc.mIsOnChangeCalled);
        assertTrue(msc.mOnChangeResult);
    }

    class MockAbstractCursor extends AbstractCursor {

        @Override
        public String[] getColumnNames() {
            return null;
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public double getDouble(int column) {
            return 0;
        }

        @Override
        public float getFloat(int column) {
            return 0;
        }

        @Override
        public int getInt(int column) {
            return 0;
        }

        @Override
        public long getLong(int column) {
            return 0;
        }

        @Override
        public short getShort(int column) {
            return 0;
        }

        @Override
        public String getString(int column) {
            return null;
        }

        @Override
        public boolean isNull(int column) {
            return false;
        }

        public MockSelfContentObserver getMockSelfContentObserver() {
            return new MockSelfContentObserver(new MockAbstractCursor());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        public class MockSelfContentObserver extends AbstractCursor.SelfContentObserver {

            public MockAbstractCursor mMockAbstractCursor;
            public boolean mIsOnChangeCalled;
            public boolean mOnChangeResult;
            private boolean mIsTrue;
            public MockSelfContentObserver(AbstractCursor cursor) {
                super(cursor);
                mMockAbstractCursor = (MockAbstractCursor) cursor;
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                mOnChangeResult = selfChange;
                mIsOnChangeCalled = true;
            }

            @Override
            public boolean deliverSelfNotifications() {
                if (mIsTrue) {
                    return true;
                }
                return super.deliverSelfNotifications();
            }

            public void setDeliverSelfNotificationsValue(boolean isTrue) {
                mIsTrue = isTrue;
            }
        }
    }

}
