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

package android.view.cts;


import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;

/**
 * Test {@link MotionEvent}.
 */
public class MotionEventTest extends AndroidTestCase {
    private MotionEvent mMotionEvent1;
    private MotionEvent mMotionEvent2;
    private long mDownTime;
    private long mEventTime;
    private static final float X_3F           = 3.0f;
    private static final float Y_4F           = 4.0f;
    private static final int META_STATE       = KeyEvent.META_SHIFT_ON;
    private static final float PRESSURE_1F    = 1.0f;
    private static final float SIZE_1F        = 1.0f;
    private static final float X_PRECISION_3F  = 3.0f;
    private static final float Y_PRECISION_4F  = 4.0f;
    private static final int DEVICE_ID_1      = 1;
    private static final int EDGE_FLAGS       = MotionEvent.EDGE_TOP;
    private static final float DELTA          = 0.01f;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mDownTime = SystemClock.uptimeMillis();
        mEventTime = SystemClock.uptimeMillis();
        mMotionEvent1 = MotionEvent.obtain(mDownTime, mEventTime,
                MotionEvent.ACTION_MOVE, X_3F, Y_4F, META_STATE);
        mMotionEvent2 = MotionEvent.obtain(mDownTime, mEventTime,
                MotionEvent.ACTION_MOVE, X_3F, Y_4F, PRESSURE_1F, SIZE_1F, META_STATE,
                X_PRECISION_3F, Y_PRECISION_4F, DEVICE_ID_1, EDGE_FLAGS);
    }

    @Override
    protected void tearDown() throws Exception {
        if (null != mMotionEvent1) {
            mMotionEvent1.recycle();
        }
        if (null != mMotionEvent2) {
            mMotionEvent2.recycle();
        }
        super.tearDown();
    }

    public void testObtain1() {
        mMotionEvent1 = MotionEvent.obtain(mDownTime, mEventTime,
                MotionEvent.ACTION_DOWN, X_3F, Y_4F, META_STATE);
        assertNotNull(mMotionEvent1);
        assertEquals(mDownTime, mMotionEvent1.getDownTime());
        assertEquals(mEventTime, mMotionEvent1.getEventTime());
        assertEquals(MotionEvent.ACTION_DOWN, mMotionEvent1.getAction());
        assertEquals(X_3F, mMotionEvent1.getX(), DELTA);
        assertEquals(Y_4F, mMotionEvent1.getY(), DELTA);
        assertEquals(X_3F, mMotionEvent1.getRawX(), DELTA);
        assertEquals(Y_4F, mMotionEvent1.getRawY(), DELTA);
        assertEquals(META_STATE, mMotionEvent1.getMetaState());
        assertEquals(0, mMotionEvent1.getDeviceId());
        assertEquals(0, mMotionEvent1.getEdgeFlags());
        assertEquals(PRESSURE_1F, mMotionEvent1.getPressure(), DELTA);
        assertEquals(SIZE_1F, mMotionEvent1.getSize(), DELTA);
        assertEquals(1.0f, mMotionEvent1.getXPrecision(), DELTA);
        assertEquals(1.0f, mMotionEvent1.getYPrecision(), DELTA);
    }

    public void testObtain2() {
        MotionEvent motionEvent = MotionEvent.obtain(mDownTime, mEventTime,
                MotionEvent.ACTION_DOWN, X_3F, Y_4F, META_STATE);
        mMotionEvent1 = MotionEvent.obtain(motionEvent);
        assertNotNull(mMotionEvent1);
        assertEquals(motionEvent.getDownTime(), mMotionEvent1.getDownTime());
        assertEquals(motionEvent.getEventTime(), mMotionEvent1.getEventTime());
        assertEquals(motionEvent.getAction(), mMotionEvent1.getAction());
        assertEquals(motionEvent.getX(), mMotionEvent1.getX(), DELTA);
        assertEquals(motionEvent.getY(), mMotionEvent1.getY(), DELTA);
        assertEquals(motionEvent.getX(), mMotionEvent1.getRawX(), DELTA);
        assertEquals(motionEvent.getY(), mMotionEvent1.getRawY(), DELTA);
        assertEquals(motionEvent.getMetaState(), mMotionEvent1.getMetaState());
        assertEquals(motionEvent.getDeviceId(), mMotionEvent1.getDeviceId());
        assertEquals(motionEvent.getEdgeFlags(), mMotionEvent1.getEdgeFlags());
        assertEquals(motionEvent.getPressure(), mMotionEvent1.getPressure(), DELTA);
        assertEquals(motionEvent.getSize(), mMotionEvent1.getSize(), DELTA);
        assertEquals(motionEvent.getXPrecision(), mMotionEvent1.getXPrecision(), DELTA);
        assertEquals(motionEvent.getYPrecision(), mMotionEvent1.getYPrecision(), DELTA);
    }

    public void testObtain3() {
        mMotionEvent1 = null;
        mMotionEvent1 = MotionEvent.obtain(mDownTime, mEventTime,
                MotionEvent.ACTION_DOWN, X_3F, Y_4F, PRESSURE_1F, SIZE_1F, META_STATE,
                X_PRECISION_3F, Y_PRECISION_4F, DEVICE_ID_1, EDGE_FLAGS);
        assertNotNull(mMotionEvent1);
        assertEquals(mDownTime, mMotionEvent1.getDownTime());
        assertEquals(mEventTime, mMotionEvent1.getEventTime());
        assertEquals(MotionEvent.ACTION_DOWN, mMotionEvent1.getAction());
        assertEquals(X_3F, mMotionEvent1.getX(), DELTA);
        assertEquals(Y_4F, mMotionEvent1.getY(), DELTA);
        assertEquals(X_3F, mMotionEvent1.getRawX(), DELTA);
        assertEquals(Y_4F, mMotionEvent1.getRawY(), DELTA);
        assertEquals(META_STATE, mMotionEvent1.getMetaState());
        assertEquals(DEVICE_ID_1, mMotionEvent1.getDeviceId());
        assertEquals(EDGE_FLAGS, mMotionEvent1.getEdgeFlags());
        assertEquals(PRESSURE_1F, mMotionEvent1.getPressure(), DELTA);
        assertEquals(SIZE_1F, mMotionEvent1.getSize(), DELTA);
        assertEquals(X_PRECISION_3F, mMotionEvent1.getXPrecision(), DELTA);
        assertEquals(Y_PRECISION_4F, mMotionEvent1.getYPrecision(), DELTA);
    }

    public void testAccessAction() {
        assertEquals(MotionEvent.ACTION_MOVE, mMotionEvent1.getAction());

        mMotionEvent1.setAction(MotionEvent.ACTION_UP);
        assertEquals(MotionEvent.ACTION_UP, mMotionEvent1.getAction());

        mMotionEvent1.setAction(MotionEvent.ACTION_MOVE);
        assertEquals(MotionEvent.ACTION_MOVE, mMotionEvent1.getAction());

        mMotionEvent1.setAction(MotionEvent.ACTION_CANCEL);
        assertEquals(MotionEvent.ACTION_CANCEL, mMotionEvent1.getAction());

        mMotionEvent1.setAction(MotionEvent.ACTION_DOWN);
        assertEquals(MotionEvent.ACTION_DOWN, mMotionEvent1.getAction());
    }

    public void testDescribeContents() {
        // make sure this method never throw any exception.
        mMotionEvent2.describeContents();
    }

    public void testAccessEdgeFlags() {
        assertEquals(EDGE_FLAGS, mMotionEvent2.getEdgeFlags());

        int edgeFlags = 10;
        mMotionEvent2.setEdgeFlags(edgeFlags);
        assertEquals(edgeFlags, mMotionEvent2.getEdgeFlags());
    }

    public void testWriteToParcel() {
        Parcel parcel = Parcel.obtain();
        mMotionEvent2.writeToParcel(parcel, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        parcel.setDataPosition(0);

        MotionEvent motionEvent = MotionEvent.CREATOR.createFromParcel(parcel);
        assertEquals(mMotionEvent2.getRawY(), motionEvent.getRawY(), DELTA);
        assertEquals(mMotionEvent2.getRawX(), motionEvent.getRawX(), DELTA);
        assertEquals(mMotionEvent2.getY(), motionEvent.getY(), DELTA);
        assertEquals(mMotionEvent2.getX(), motionEvent.getX(), DELTA);
        assertEquals(mMotionEvent2.getAction(), motionEvent.getAction());
        assertEquals(mMotionEvent2.getDownTime(), motionEvent.getDownTime());
        assertEquals(mMotionEvent2.getEventTime(), motionEvent.getEventTime());
        assertEquals(mMotionEvent2.getEdgeFlags(), motionEvent.getEdgeFlags());
        assertEquals(mMotionEvent2.getDeviceId(), motionEvent.getDeviceId());
    }

    public void testToString() {
        // make sure this method never throw exception.
        mMotionEvent2.toString();
    }

    public void testOffsetLocation() {
        assertEquals(X_3F, mMotionEvent2.getX(), DELTA);
        assertEquals(Y_4F, mMotionEvent2.getY(), DELTA);

        float offsetX = 1.0f;
        float offsetY = 1.0f;
        mMotionEvent2.offsetLocation(offsetX, offsetY);
        assertEquals(X_3F + offsetX, mMotionEvent2.getX(), DELTA);
        assertEquals(Y_4F + offsetY, mMotionEvent2.getY(), DELTA);
    }

    public void testSetLocation() {
        assertEquals(X_3F, mMotionEvent2.getX(), DELTA);
        assertEquals(Y_4F, mMotionEvent2.getY(), DELTA);

        float newLocationX = 0.0f;
        float newLocationY = 0.0f;
        mMotionEvent2.setLocation(newLocationX, newLocationY);
        assertEquals(newLocationX, mMotionEvent2.getX(), DELTA);
        assertEquals(newLocationY, mMotionEvent2.getY(), DELTA);

        newLocationX = 2.0f;
        newLocationY = 2.0f;
        mMotionEvent2.setLocation(newLocationX, newLocationY);
        assertEquals(newLocationX, mMotionEvent2.getX(), DELTA);
        assertEquals(newLocationY, mMotionEvent2.getY(), DELTA);
    }

    public void testGetHistoricalX() {
        float x = X_3F + 5.0f;
        mMotionEvent2.addBatch(mEventTime, x, 5.0f, 1.0f, 0.0f, 0);
        assertEquals(X_3F, mMotionEvent2.getHistoricalX(0), DELTA);

        mMotionEvent2.addBatch(mEventTime, X_3F + 10.0f, 10.0f, 0.0f, 1.0f, 0);
        assertEquals(x, mMotionEvent2.getHistoricalX(1), DELTA);
    }

    public void testGetHistoricalY() {
        float y = Y_4F + 5.0f;
        mMotionEvent2.addBatch(mEventTime, 5.0f, y, 1.0f, 0.0f, 0);
        assertEquals(Y_4F, mMotionEvent2.getHistoricalY(0), DELTA);

        mMotionEvent2.addBatch(mEventTime, 15.0f, Y_4F + 15.0f, 0.0f, 1.0f, 0);
        assertEquals(y, mMotionEvent2.getHistoricalY(1), DELTA);
    }

    public void testGetHistoricalSize() {
        float size = 0.5f;
        mMotionEvent2.addBatch(mEventTime, 5.0f, 5.0f, 1.0f, size, 0);
        assertEquals(SIZE_1F, mMotionEvent2.getHistoricalSize(0), DELTA);

        mMotionEvent2.addBatch(mEventTime, 15.0f, 15.0f, 1.0f, 0.0f, 0);
        assertEquals(size, mMotionEvent2.getHistoricalSize(1), DELTA);
    }

    public void testGetHistoricalPressure() {
        float pressure = 0.5f;
        mMotionEvent2.addBatch(mEventTime, 5.0f, 5.0f, pressure, 0.0f, 0);
        assertEquals(PRESSURE_1F, mMotionEvent2.getHistoricalPressure(0), DELTA);

        mMotionEvent2.addBatch(mEventTime, 15.0f, 15.0f, 0.0f, 0.0f, 0);
        assertEquals(pressure, mMotionEvent2.getHistoricalPressure(1), DELTA);
    }

    public void testGetHistoricalEventTime() {
        long eventTime = mEventTime + 5l;
        mMotionEvent2.addBatch(eventTime, 5.0f, 5.0f, 0.0f, 1.0f, 0);
        assertEquals(mEventTime, mMotionEvent2.getHistoricalEventTime(0));

        mMotionEvent2.addBatch(mEventTime + 10l, 15.0f, 15.0f, 1.0f, 0.0f, 0);
        assertEquals(eventTime, mMotionEvent2.getHistoricalEventTime(1));
    }

    public void testAddBatch() {
        long eventTime = SystemClock.uptimeMillis();
        float x = 10.0f;
        float y = 20.0f;
        float pressure = 1.0f;
        float size = 1.0f;

        // get original attribute values.
        long origEventTime = mMotionEvent2.getEventTime();
        float origX = mMotionEvent2.getX();
        float origY = mMotionEvent2.getY();
        float origPressure = mMotionEvent2.getPressure();
        float origSize = mMotionEvent2.getSize();

        assertEquals(0, mMotionEvent2.getHistorySize());
        mMotionEvent2.addBatch(eventTime, x, y, pressure, size, 0);
        assertEquals(1, mMotionEvent2.getHistorySize());
        assertEquals(origEventTime, mMotionEvent2.getHistoricalEventTime(0));
        assertEquals(origX, mMotionEvent2.getHistoricalX(0), DELTA);
        assertEquals(origY, mMotionEvent2.getHistoricalY(0), DELTA);
        assertEquals(origPressure, mMotionEvent2.getHistoricalPressure(0), DELTA);
        assertEquals(origSize, mMotionEvent2.getHistoricalSize(0), DELTA);

        mMotionEvent2.addBatch(mEventTime, 6, 6, 0.1f, 0, 0);
        assertEquals(2, mMotionEvent2.getHistorySize());
        assertEquals(eventTime, mMotionEvent2.getHistoricalEventTime(1));
        assertEquals(x, mMotionEvent2.getHistoricalX(1), DELTA);
        assertEquals(y, mMotionEvent2.getHistoricalY(1), DELTA);
        assertEquals(pressure, mMotionEvent2.getHistoricalPressure(1), DELTA);
        assertEquals(size, mMotionEvent2.getHistoricalSize(1), DELTA);
    }

    public void testGetHistorySize() {
        long eventTime = SystemClock.uptimeMillis();
        float x = 10.0f;
        float y = 20.0f;
        float pressure = 1.0f;
        float size = 1.0f;

        mMotionEvent2.setAction(MotionEvent.ACTION_DOWN);
        assertEquals(0, mMotionEvent2.getHistorySize());

        mMotionEvent2.setAction(MotionEvent.ACTION_MOVE);
        mMotionEvent2.addBatch(eventTime, x, y, pressure, size, 0);
        assertEquals(1, mMotionEvent2.getHistorySize());
    }

    public void testRecycle() {
        mMotionEvent2.setAction(MotionEvent.ACTION_MOVE);
        assertEquals(0, mMotionEvent2.getHistorySize());
        mMotionEvent2.addBatch(mEventTime, 10.0f, 5.0f, 1.0f, 0.0f, 0);
        assertEquals(1, mMotionEvent2.getHistorySize());

        mMotionEvent2.recycle();
        
        try {
            mMotionEvent2.recycle();
            fail("recycle() should throw an exception when the event has already been recycled.");
        } catch (RuntimeException ex) {
        }
        
        mMotionEvent2 = null; // since it was recycled, don't try to recycle again in tear down
    }

    public void testTransformShouldThrowWhenMatrixIsNull() {
        try {
            mMotionEvent1.transform(null);
            fail("transform() should throw an exception when matrix is null.");
        } catch (IllegalArgumentException ex) {
        }
    }

    public void testTransformShouldApplyMatrixToPointsAndPreserveRawPosition() {
        // Generate some points on a circle.
        // Each point 'i' is a point on a circle of radius ROTATION centered at (3,2) at an angle
        // of ARC * i degrees clockwise relative to the Y axis.
        // The geometrical representation is irrelevant to the test, it's just easy to generate
        // and check rotation.  We set the orientation to the same angle.
        // Coordinate system: down is increasing Y, right is increasing X.
        final float PI_180 = (float) (Math.PI / 180);
        final float RADIUS = 10;
        final float ARC = 36;
        final float ROTATION = ARC * 2;

        final int pointerCount = 11;
        final int[] pointerIds = new int[pointerCount];
        final PointerCoords[] pointerCoords = new PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            final PointerCoords c = new PointerCoords();
            final float angle = (float) (i * ARC * PI_180);
            pointerIds[i] = i;
            pointerCoords[i] = c;
            c.x = (float) (Math.sin(angle) * RADIUS + 3);
            c.y = (float) (- Math.cos(angle) * RADIUS + 2);
            c.orientation = angle;
        }
        final MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                pointerCount, pointerIds, pointerCoords, 0, 0, 0, 0, 0, 0, 0);
        final float originalRawX = 0 + 3;
        final float originalRawY = - RADIUS + 2;
        dump("Original points.", event);

        // Check original raw X and Y assumption.
        assertEquals(originalRawX, event.getRawX(), 0.001);
        assertEquals(originalRawY, event.getRawY(), 0.001);

        // Now translate the motion event so the circle's origin is at (0,0).
        event.offsetLocation(-3, -2);
        dump("Translated points.", event);

        // Offsetting the location should preserve the raw X and Y of the first point.
        assertEquals(originalRawX, event.getRawX(), 0.001);
        assertEquals(originalRawY, event.getRawY(), 0.001);

        // Apply a rotation about the origin by ROTATION degrees clockwise.
        Matrix matrix = new Matrix();
        matrix.setRotate(ROTATION);
        event.transform(matrix);
        dump("Rotated points.", event);

        // Check the points.
        for (int i = 0; i < pointerCount; i++) {
            final PointerCoords c = pointerCoords[i];
            event.getPointerCoords(i, c);

            final float angle = (float) ((i * ARC + ROTATION) * PI_180);
            assertEquals(Math.sin(angle) * RADIUS, c.x, 0.001);
            assertEquals(- Math.cos(angle) * RADIUS, c.y, 0.001);
            assertEquals(Math.tan(angle), Math.tan(c.orientation), 0.1);
        }

        // Applying the transformation should preserve the raw X and Y of the first point.
        assertEquals(originalRawX, event.getRawX(), 0.001);
        assertEquals(originalRawY, event.getRawY(), 0.001);
    }

    private void dump(String label, MotionEvent ev) {
        if (false) {
            StringBuilder msg = new StringBuilder();
            msg.append(label).append("\n");

            msg.append("  Raw: (").append(ev.getRawX()).append(",").append(ev.getRawY()).append(")\n");
            int pointerCount = ev.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                msg.append("  Pointer[").append(i).append("]: (")
                        .append(ev.getX(i)).append(",").append(ev.getY(i)).append("), orientation=")
                        .append(ev.getOrientation(i) * 180 / Math.PI).append(" deg\n");
            }

            android.util.Log.i("TEST", msg.toString());
        }
    }

    public void testPointerCoordsDefaultConstructor() {
        PointerCoords coords = new PointerCoords();

        assertEquals(0f, coords.x);
        assertEquals(0f, coords.y);
        assertEquals(0f, coords.pressure);
        assertEquals(0f, coords.size);
        assertEquals(0f, coords.touchMajor);
        assertEquals(0f, coords.touchMinor);
        assertEquals(0f, coords.toolMajor);
        assertEquals(0f, coords.toolMinor);
        assertEquals(0f, coords.orientation);
    }

    public void testPointerCoordsCopyConstructor() {
        PointerCoords coords = new PointerCoords();
        coords.x = 1;
        coords.y = 2;
        coords.pressure = 3;
        coords.size = 4;
        coords.touchMajor = 5;
        coords.touchMinor = 6;
        coords.toolMajor = 7;
        coords.toolMinor = 8;
        coords.orientation = 9;
        coords.setAxisValue(MotionEvent.AXIS_GENERIC_1, 10);

        PointerCoords copy = new PointerCoords(coords);
        assertEquals(1f, copy.x);
        assertEquals(2f, copy.y);
        assertEquals(3f, copy.pressure);
        assertEquals(4f, copy.size);
        assertEquals(5f, copy.touchMajor);
        assertEquals(6f, copy.touchMinor);
        assertEquals(7f, copy.toolMajor);
        assertEquals(8f, copy.toolMinor);
        assertEquals(9f, copy.orientation);
        assertEquals(10f, coords.getAxisValue(MotionEvent.AXIS_GENERIC_1));
    }

    public void testPointerCoordsCopyFrom() {
        PointerCoords coords = new PointerCoords();
        coords.x = 1;
        coords.y = 2;
        coords.pressure = 3;
        coords.size = 4;
        coords.touchMajor = 5;
        coords.touchMinor = 6;
        coords.toolMajor = 7;
        coords.toolMinor = 8;
        coords.orientation = 9;
        coords.setAxisValue(MotionEvent.AXIS_GENERIC_1, 10);

        PointerCoords copy = new PointerCoords();
        copy.copyFrom(coords);
        assertEquals(1f, copy.x);
        assertEquals(2f, copy.y);
        assertEquals(3f, copy.pressure);
        assertEquals(4f, copy.size);
        assertEquals(5f, copy.touchMajor);
        assertEquals(6f, copy.touchMinor);
        assertEquals(7f, copy.toolMajor);
        assertEquals(8f, copy.toolMinor);
        assertEquals(9f, copy.orientation);
        assertEquals(10f, coords.getAxisValue(MotionEvent.AXIS_GENERIC_1));
    }

    public void testPointerPropertiesDefaultConstructor() {
        PointerProperties properties = new PointerProperties();

        assertEquals(MotionEvent.INVALID_POINTER_ID, properties.id);
        assertEquals(MotionEvent.TOOL_TYPE_UNKNOWN, properties.toolType);
    }

    public void testPointerPropertiesCopyConstructor() {
        PointerProperties properties = new PointerProperties();
        properties.id = 1;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;

        PointerProperties copy = new PointerProperties(properties);
        assertEquals(1, copy.id);
        assertEquals(MotionEvent.TOOL_TYPE_MOUSE, copy.toolType);
    }

    public void testPointerPropertiesCopyFrom() {
        PointerProperties properties = new PointerProperties();
        properties.id = 1;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;

        PointerProperties copy = new PointerProperties();
        copy.copyFrom(properties);
        assertEquals(1, copy.id);
        assertEquals(MotionEvent.TOOL_TYPE_MOUSE, copy.toolType);
    }
}
