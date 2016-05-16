package com.android.dreamtheater;

import android.animation.PropertyValuesHolder;
import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.HashMap;

public class BouncyDroid extends Activity {
    static final boolean DEBUG = true;
    static final boolean CENTER_DROID = true;

    public static class BouncyView extends FrameLayout
    {
        boolean mShowDebug = false;

        static final int RADIUS = 100;

        static final boolean HAS_INITIAL_IMPULSE = true;
        static final boolean HAS_GRAVITY = true;
        static final boolean HAS_FRICTION = false;
        static final boolean HAS_EDGES = true;

        static final boolean STICKY_FINGERS = true;

        static final float MAX_SPEED = 5000f;

        static final float RANDOM_IMPULSE_PROB = 0.001f;

        public static class World {
            public static final float PX_PER_METER = 100f;
            public static final float GRAVITY = 500f;
            public static class Vec {
                float x;
                float y;
                public Vec() {
                    x = y = 0;
                }
                public Vec(float _x, float _y) {
                    x = _x;
                    y = _y;
                }
                public Vec add(Vec v) {
                    return new Vec(x + v.x, y + v.y);
                }
                public Vec mul(float a) {
                    return new Vec(x * a, y * a);
                }
                public Vec sub(Vec v) {
                    return new Vec(x - v.x, y - v.y);
                }
                public float mag() {
                    return (float) Math.hypot(x, y);
                }
                public Vec norm() {
                    float k = 1/mag();
                    return new Vec(x*k, y*k);
                }
                public String toString() {
                    return "(" + x + "," + y + ")";
                }
            }
            public static class Body {
                float m, r;
                Vec p = new Vec();
                Vec v = new Vec();
                LinkedList<Vec> forces = new LinkedList<Vec>();
                LinkedList<Vec> impulses = new LinkedList<Vec>();
                public Body(float _m, Vec _p) {
                    m = _m;
                    p = _p;
                }
                public void applyForce(Vec f) {
                    forces.add(f);
                }
                public void applyImpulse(Vec f) {
                    impulses.add(f);
                }
                public void clearForces() {
                    forces.clear();
                }
                public void removeForce(Vec f) {
                    forces.remove(f);
                }
                public void step(float dt) {
                    p = p.add(v.mul(dt));
                    for (Vec f : impulses) {
                        v = v.add(f.mul(dt/m));
                    }
                    impulses.clear();
                    for (Vec f : forces) {
                        v = v.add(f.mul(dt/m));
                    }
                }
                public String toString() {
                    return "Body(m=" + m + " p=" + p + " v=" + v + ")";
                }
            }
            LinkedList<Body> mBodies = new LinkedList<Body>();
            public void addBody(Body b) {
                mBodies.add(b);
            }

            public void step(float dt) {
                for (Body b : mBodies) {
                    b.step(dt);
                }
            }
        }


        TimeAnimator mAnim;
        World mWorld;
        ImageView mBug;
        View mShowDebugView;
        HashMap<Integer, World.Vec> mFingers = new HashMap<Integer, World.Vec>();
        World.Body mBody;
        World.Vec mGrabSpot;
        int mGrabbedPointer = -1;

        public BouncyView(Context context, AttributeSet as) {
            super(context, as);

            /*
            setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if (visibility == View.STATUS_BAR_VISIBLE) {
                        ((Activity)getContext()).finish();
                    }
                }
            });
            */

            setBackgroundColor(0xFF444444);

            mBug = new ImageView(context);
            mBug.setScaleType(ImageView.ScaleType.MATRIX);
            addView(mBug, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

            if (DEBUG) {
                Button b = new Button(getContext());
                b.setText("Debugzors");
                b.setBackgroundColor(0); // very hard to see! :)
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setDebug(!mShowDebug);
                    }
                });
                addView(b, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP|Gravity.RIGHT));
            }
        }

        public void setDebug(boolean d) {
            if (d != mShowDebug) {
                if (d) {
                    mShowDebugView = new DebugView(getContext());
                    mShowDebugView.setLayoutParams(
                        new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ));
                    addView(mShowDebugView);
                    
                    mBug.setBackgroundColor(0x2000FF00);
                } else {
                    if (mShowDebugView != null) {
                        removeView(mShowDebugView);
                        mShowDebugView = null;
                    }
                    mBug.setBackgroundColor(0);
                }
                invalidate();
                mShowDebug = d;
            }
        }

        private void reset() {
            mWorld = new World();
            final float mass = 100;
            mBody = new World.Body(mass, new World.Vec(200,200));
            mBody.r = RADIUS;
            mWorld.addBody(mBody);
            mGrabbedPointer = -1;

            mAnim = new TimeAnimator();
            mAnim.setTimeListener(new TimeAnimator.TimeListener() {
                public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                    if (deltaTime > 0) {
                        int STEPS = 5;
                        final float dt = deltaTime / (float) STEPS;
                        while (STEPS-->0) {
                            mBody.clearForces();

                            if (HAS_INITIAL_IMPULSE) {
                                // initial oomph
                                if (totalTime == 0) {
                                    mBody.applyImpulse(new World.Vec(400000, -200000));
                                }
                            }

                            if (HAS_GRAVITY) {
                                // gravity points down
                                mBody.applyForce(new World.Vec(0, mass * World.GRAVITY));
                            }

                            if (mGrabbedPointer >= 0) {
                                World.Vec finger = mFingers.get(mGrabbedPointer);
                                if (finger == null) {
                                    // let go!
                                    mGrabbedPointer = -1;
                                } else {
                                    // never gonna let you go
                                    World.Vec newPos = finger.add(mGrabSpot);
                                    mBody.v = mBody.v.add(newPos.sub(mBody.p).mul(dt));
                                    mBody.p = newPos;
                                }
                            } else {
                                // springs
                                // ideal Hooke's Law plus a maximum force and a minimum length cutoff
                                for (Integer i : mFingers.keySet()) {
                                    World.Vec finger = mFingers.get(i);
                                    World.Vec springForce = finger.sub(mBody.p);
                                    float mag = springForce.mag();

                                    if (STICKY_FINGERS && mag < mBody.r*0.75) {
                                        // close enough; we'll call this a "stick"
                                        mGrabbedPointer = i;
                                        mGrabSpot = mBody.p.sub(finger);
                                        mBody.v = new World.Vec(0,0);
                                        break;
                                    }

                                    final float SPRING_K = 30000;
                                    final float FORCE_MAX = 10*SPRING_K;
                                    mag = (float) Math.min(mag * SPRING_K, FORCE_MAX); // Hooke's law
            //                    float mag = (float) (FORCE_MAX / Math.pow(springForce.mag(), 2)); // Gravitation
                                    springForce = springForce.norm().mul(mag);
                                    mBody.applyForce(springForce);
                                }
                            }

                            if (HAS_FRICTION) {
                                // sliding friction opposes movement
                                mBody.applyForce(mBody.v.mul(-0.01f * mBody.m));
                            }

                            if (HAS_EDGES) {
                                if (mBody.p.x - mBody.r < 0) {
                                    mBody.v.x = (float) Math.abs(mBody.v.x) * 
                                        (HAS_FRICTION ? 0.95f : 1f);
                                } else if (mBody.p.x + mBody.r > getWidth()) {
                                    mBody.v.x = (float) Math.abs(mBody.v.x) * 
                                        (HAS_FRICTION ? -0.95f : -1f);
                                }
                                if (mBody.p.y - mBody.r < 0) {
                                    mBody.v.y = (float) Math.abs(mBody.v.y) * 
                                        (HAS_FRICTION ? 0.95f : 1f);
                                } else if (mBody.p.y + mBody.r > getHeight()) {
                                    mBody.v.y = (float) Math.abs(mBody.v.y) * 
                                        (HAS_FRICTION ? -0.95f : -1f);
                                }
                            }

                            if (MAX_SPEED > 0) {
                                if (mBody.v.mag() > MAX_SPEED) {
                                    mBody.v = mBody.v.norm().mul(MAX_SPEED);
                                }
                            }

                            // ok, Euler, do your thing
                            mWorld.step(dt / 1000f); // dt is in sec
                        }
                    }
                    mBug.setTranslationX(mBody.p.x - mBody.r);
                    mBug.setTranslationY(mBody.p.y - mBody.r);

                    Matrix m = new Matrix();
                    m.setScale(
                        (mBody.v.x < 0)    ? -1 : 1, 
                        (mBody.v.y > 1500) ? -1 : 1, // AAAAAAAAAAAAAAAA
                        RADIUS, RADIUS);
                    mBug.setImageMatrix(m);
                    if (CENTER_DROID) {
                        mBug.setImageResource(
                            (Math.abs(mBody.v.x) < 25) 
                                ? R.drawable.bouncy_center
                                : R.drawable.bouncy);
                    }

                    if (mShowDebug) mShowDebugView.invalidate();
                }
            });
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            setSystemUiVisibility(View.STATUS_BAR_HIDDEN);

            reset();
            mAnim.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mAnim.cancel();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int i;
            for (i=0; i<event.getPointerCount(); i++) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mFingers.put(event.getPointerId(i),
                                new World.Vec(event.getX(i), event.getY(i)));
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mFingers.remove(event.getPointerId(i));
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        mFingers.clear();
                        break;
                }
            }
            // expired pointers
    //        for (; i<mFingers.length; i++) {
    //            mFingers[i] = null;
    //        }
            return true;
        }

        @Override
        public boolean isOpaque() {
            return true;
        }

        class DebugView extends View {
            public DebugView(Context ct) {
                super(ct);
            }

            private void drawVector(Canvas canvas,
                    float x, float y, float vx, float vy,
                    Paint pt) {
                final float mag = (float) Math.hypot(vx, vy);

                canvas.save();
                Matrix mx = new Matrix();
                mx.setSinCos(-vx/mag, vy/mag);
                mx.postTranslate(x, y);
                canvas.setMatrix(mx);

                canvas.drawLine(0,0, 0, mag, pt);
                canvas.drawLine(0, mag, -4, mag-4, pt);
                canvas.drawLine(0, mag, 4, mag-4, pt);

                canvas.restore();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
                pt.setColor(0xFFCC0000);
                pt.setTextSize(30f);
                pt.setStrokeWidth(1.0f);

                for (Integer id : mFingers.keySet()) {
                    World.Vec v = mFingers.get(id);
                    float x = v.x;
                    float y = v.y;
                    pt.setStyle(Paint.Style.FILL);
                    canvas.drawText("#"+id, x+38, y-38, pt);
                    pt.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(x-40, y, x+40, y, pt);
                    canvas.drawLine(x, y-40, x, y+40, pt);
                    canvas.drawCircle(x, y, 40, pt);
                }
                pt.setStyle(Paint.Style.STROKE);
                if (mBody != null) {
                    float x = mBody.p.x;
                    float y = mBody.p.y;
                    float r = mBody.r;
                    pt.setColor(0xFF6699FF);
                    RectF bounds = new RectF(x-r, y-r, x+r, y+r);
                    canvas.drawOval(bounds, pt);

                    pt.setStrokeWidth(3);
                    drawVector(canvas, x, y, mBody.v.x/100, mBody.v.y/100, pt);

                    pt.setColor(0xFF0033FF);
                    for (World.Vec f : mBody.forces) {
                        drawVector(canvas, x, y, f.x/1000, f.y/1000, pt);
                    }
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setContentView(new BouncyView(this, null));
    }
}
