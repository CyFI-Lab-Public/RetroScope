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
package android.animation.cts;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

public class PropertyValuesHolderTest extends
        ActivityInstrumentationTestCase2<AnimationActivity> {
    private AnimationActivity mActivity;
    private Animator mAnimator;
    private long mDuration = 1000;
    private float mStartY;
    private float mEndY;
    private Object mObject;
    private String mProperty;

    public PropertyValuesHolderTest() {
        super(AnimationActivity.class);
    }

    public void setUp() throws Exception {
         super.setUp();
         setActivityInitialTouchMode(false);
         mActivity = getActivity();
         mAnimator = mActivity.createAnimatorWithDuration(mDuration);
         mProperty = "y";
         mStartY = mActivity.mStartY;
         mEndY = mActivity.mStartY + mActivity.mDeltaY;
         mObject = mActivity.view.newBall;
    }

    public void testGetPropertyName() {
        float[] values = {mStartY, mEndY};
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat(mProperty, values);
        assertEquals(mProperty, pVHolder.getPropertyName());
    }

    public void testSetPropertyName() {
        float[] values = {mStartY, mEndY};
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat("", values);
        pVHolder.setPropertyName(mProperty);
        assertEquals(mProperty, pVHolder.getPropertyName());
    }

    public void testClone() {
        float[] values = {mStartY, mEndY};
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat(mProperty, values);
        PropertyValuesHolder cloneHolder = pVHolder.clone();
        assertEquals(pVHolder.getPropertyName(), cloneHolder.getPropertyName());
    }

    public void testSetValues() throws Throwable {
        float[] dummyValues = {100, 150};
        float[] values = {mStartY, mEndY};
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat(mProperty, dummyValues);
        pVHolder.setFloatValues(values);

        ObjectAnimator objAnimator = ObjectAnimator.ofPropertyValuesHolder(mObject,pVHolder);
        assertTrue(objAnimator != null);
        setAnimatorProperties(objAnimator);

        startAnimation(objAnimator);
        assertTrue(objAnimator != null);
        float[] yArray = getYPosition();
        assertResults(yArray, mStartY, mEndY);
    }

    public void testOffloat() throws Throwable {
        float[] values = {mStartY, mEndY};
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat(mProperty, values);
        assertNotNull(pVHolder);
        ObjectAnimator objAnimator = ObjectAnimator.ofPropertyValuesHolder(mObject,pVHolder);
        assertTrue(objAnimator != null);

        setAnimatorProperties(objAnimator);
        startAnimation(objAnimator);
        assertTrue(objAnimator != null);
        float[] yArray = getYPosition();
        assertResults(yArray, mStartY, mEndY);
    }

    public void testOfFloat_Property() throws Throwable {
        float[] values = {mStartY, mEndY};
        ShapeHolderYProperty property=new ShapeHolderYProperty(ShapeHolder.class.getClass(),"y");
        property.setObject(mObject);
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat(property, values);
        assertNotNull(pVHolder);
        ObjectAnimator objAnimator = ObjectAnimator.ofPropertyValuesHolder(mObject,pVHolder);
        assertTrue(objAnimator != null);

        setAnimatorProperties(objAnimator);
        startAnimation(objAnimator);
        assertTrue(objAnimator != null);
        float[] yArray = getYPosition();
        assertResults(yArray, mStartY, mEndY);
    }

    public void testOfInt() throws Throwable {
        int start = 0;
        int end = 10;
        int[] values = {start, end};
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofInt(mProperty, values);
        assertNotNull(pVHolder);
        final ObjectAnimator objAnimator = ObjectAnimator.ofPropertyValuesHolder(mObject,pVHolder);
        assertTrue(objAnimator != null);
        setAnimatorProperties(objAnimator);
        this.runTestOnUiThread(new Runnable(){
            public void run() {
                objAnimator.start();
            }
        });
        Thread.sleep(1000);
        assertTrue(objAnimator.isRunning());
        Integer animatedValue = (Integer) objAnimator.getAnimatedValue();
        assertTrue(animatedValue >= start);
        assertTrue(animatedValue <= end);
    }

    public void testOfInt_Property() throws Throwable{
        Object object = mActivity.view;
        String property = "backgroundColor";
        int startColor = mActivity.view.RED;
        int endColor = mActivity.view.BLUE;
        int values[] = {startColor, endColor};

        ViewColorProperty colorProperty=new ViewColorProperty(Integer.class.getClass(),property);
        colorProperty.setObject(object);
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofInt(colorProperty, values);
        assertNotNull(pVHolder);

        ObjectAnimator colorAnimator = ObjectAnimator.ofPropertyValuesHolder(object,pVHolder);
        colorAnimator.setDuration(1000);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator objectAnimator = (ObjectAnimator) mActivity.createAnimatorWithDuration(
            mDuration);
        startAnimation(objectAnimator, colorAnimator);
        Thread.sleep(1000);
        Integer i = (Integer) colorAnimator.getAnimatedValue();
        //We are going from less negative value to a more negative value
        assertTrue(i.intValue() <= startColor);
        assertTrue(endColor <= i.intValue());
    }

    public void testSetProperty() throws Throwable {
        float[] values = {mStartY, mEndY};
        ShapeHolderYProperty property=new ShapeHolderYProperty(ShapeHolder.class.getClass(),"y");
        property.setObject(mObject);
        PropertyValuesHolder pVHolder = PropertyValuesHolder.ofFloat("", values);
        pVHolder.setProperty(property);
        ObjectAnimator objAnimator = ObjectAnimator.ofPropertyValuesHolder(mObject,pVHolder);
        setAnimatorProperties(objAnimator);
        startAnimation(objAnimator);
        assertTrue(objAnimator != null);
        float[] yArray = getYPosition();
        assertResults(yArray, mStartY, mEndY);
    }

    class ShapeHolderYProperty extends Property {
        private ShapeHolder shapeHolder ;
        private Class type = Float.class.getClass();
        private String name = "y";
        @SuppressWarnings("unchecked")
        public ShapeHolderYProperty(Class type, String name) throws Exception {
            super(Float.class, name );
            if(!( type.equals(this.type) || ( name.equals(this.name))) ){
                throw new Exception("Type or name provided does not match with " +
                        this.type.getName() + " or " + this.name);
            }
        }

        public void setObject(Object object){
            shapeHolder = (ShapeHolder) object;
        }

        @Override
        public Object get(Object object) {
            return shapeHolder;
        }

        @Override
        public String getName() {
            return "y";
        }

        @Override
        public Class getType() {
            return super.getType();
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void set(Object object, Object value) {
            shapeHolder.setY((Float)value);
        }

    }

    class ViewColorProperty extends Property {
        private View view ;
        private Class type = Integer.class.getClass();
        private String name = "backgroundColor";
        @SuppressWarnings("unchecked")
        public ViewColorProperty(Class type, String name) throws Exception {
            super(Integer.class, name );
            if(!( type.equals(this.type) || ( name.equals(this.name))) ){
                throw new Exception("Type or name provided does not match with " +
                        this.type.getName() + " or " + this.name);
            }
        }

        public void setObject(Object object){
            view = (View) object;
        }

        @Override
        public Object get(Object object) {
            return view;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class getType() {
            return super.getType();
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void set(Object object, Object value) {
            view.setBackgroundColor((Integer)value);
        }
    }

    private void setAnimatorProperties(ObjectAnimator objAnimator) {
        objAnimator.setDuration(mDuration);
        objAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objAnimator.setInterpolator(new AccelerateInterpolator());
        objAnimator.setRepeatMode(ValueAnimator.REVERSE);
    }

    public float[] getYPosition() throws Throwable{
        float[] yArray = new float[3];
        for(int i = 0; i < 3; i++) {
            float y = mActivity.view.newBall.getY();
            yArray[i] = y;
            Thread.sleep(300);
        }
        return yArray;
    }

    public void assertResults(float[] yArray,float startY, float endY) {
        for(int i = 0; i < 3; i++){
            float y = yArray[i];
            assertTrue(y >= startY);
            assertTrue(y <= endY);
            if(i < 2) {
                float yNext = yArray[i+1];
                assertTrue(y != yNext);
            }
        }
    }

    private void startAnimation(final Animator animator) throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.startAnimation(animator);
            }
        });
    }

    private void startAnimation(final ObjectAnimator mObjectAnimator,
            final ObjectAnimator colorAnimator) throws Throwable {
        Thread mAnimationRunnable = new Thread() {
            public void run() {
                mActivity.startAnimation(mObjectAnimator, colorAnimator);
            }
        };
        this.runTestOnUiThread(mAnimationRunnable);
    }
}

