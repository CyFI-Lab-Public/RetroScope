package com.forensix.retroscope;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ZmbSurface;
import android.view.DeadBuffer;
import android.view.View;
import android.view.ViewGroup;
import android.view.GLES20DisplayList;
import android.view.DisplayList;
import android.view.HardwareCanvas;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.view.HardwareLayer;
import android.widget.ImageView;


  public class DebugView extends View {
        MyActivity myActivity;

        public DebugView(Context context) {
            super(context);
            myActivity = (MyActivity)context;
        }

        public DebugView(Context context, AttributeSet attrs) {
            super(context, attrs);
            myActivity = (MyActivity)context;
        }

        public DebugView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            myActivity = (MyActivity)context;
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            if (myActivity.isClicked==false) {
              return;
            }
         
            GLES20DisplayList dlist = myActivity.currDL;
            HardwareCanvas hwCanvas = (HardwareCanvas) canvas;
            hwCanvas.outputDisplayList((DisplayList)dlist);
            hwCanvas.drawDisplayList(dlist);
            
        }

    }

