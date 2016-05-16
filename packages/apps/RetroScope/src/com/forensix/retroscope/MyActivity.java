package com.forensix.retroscope;

import java.io.FileOutputStream;
import java.lang.InterruptedException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
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
import android.view.GLES20Canvas;
import android.view.GLES20Layer;
import android.view.GLES20RenderLayer;
import android.view.HardwareCanvas;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.view.HardwareLayer;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.concurrent.Semaphore;
import java.util.Random;

public class MyActivity extends Activity {

    ZmbSurface zmbSurface = null;
    boolean isClicked = false;
    GLES20DisplayList currDL;

    @Override
    protected void onStart() {
        super.onStart();
        new Thread( new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(10000);
            } catch (InterruptedException e){
            }
            
            final Semaphore mutex = new Semaphore(0);
            runOnUiThread(new Runnable() {
             @Override public void run() {
               TextView t=(TextView)findViewById(R.id.Dialog); 
               t.setText("Using Memory Image /sdcard/mem.m");
               t.append("\nRunning...");
               t.append("\nRetroScope will close when finished.");
               mutex.release();
             }
            });
            try {
              mutex.acquire();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
  
            runOnUiThread(new Runnable() {
             @Override public void run() {
              if (!isClicked) {
                isClicked = true;
                long startTime = System.currentTimeMillis();

                WindowManager wm = getWindowManager();
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int bitmap_width = size.x, bitmap_height = size.y;
                Bitmap bitmap = Bitmap.createBitmap(bitmap_width, bitmap_height, Bitmap.Config.ARGB_8888);
                Bitmap emptyBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                DebugView dlView = ((DebugView)findViewById(R.id.buffer));
                
                zmbSurface = new ZmbSurface(wm, bitmap_width, bitmap_height);
                if (!zmbSurface.more_dls) return;
  
                currDL = zmbSurface.getDL(); 
                int i=0;
                while (currDL != null) {
                  Log.e("JAVA_ZMB", "DRAWING displaylist");
                  
                  i++;

                  
                  GLES20Layer layer = new GLES20RenderLayer(bitmap_width, bitmap_height, true);
                  HardwareCanvas hwCanvas = layer.start(null);
                  
                  dlView.draw(hwCanvas);
                  layer.end(null);
  
                  Log.e("JAVA_ZMB", "Should Have Drawn DL " + i);
                  Log.e("JAVA_ZMB", "Got a Layer!");
                  Log.e("JAVA_ZMB", "Got a New Bitmap!");
                  layer.copyInto(bitmap);
                  Log.e("JAVA_ZMB", "Copied Layer into Bitmap " + i);
                  Log.e("RetroScope", "Copied Layer into Bitmap " + i);
                  if (!bitmap.sameAs(emptyBitmap)) {
                    try {
                      String path = Environment.getExternalStorageDirectory().getPath() + "/generatedImages/" + i + ".png";
                      boolean streamFlushed = false;
                      while (!streamFlushed) {
                        FileOutputStream out = new FileOutputStream(path);
                        streamFlushed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.close();
                      }
                      Log.e("JAVA_ZMB", "Copied Bitmap to File " + path);
                      Log.e("RetroScope", "Copied Bitmap to File " + path + "\n\n\n\n\n\n");
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                  else {
                     Log.e("JAVA_ZMB", "Not Writing Bitmap " + i + " to file");
                  }

                  currDL = zmbSurface.getDL();
                }

                long endTime = System.currentTimeMillis();
                
                Log.e("JAVA_ZMB", "ZMB SUCCESS! It Took " + (endTime-startTime) + " ms.");
  
//                isClicked = false;
//                dlView.invalidate();
                MyActivity.this.finish();
                System.exit(0);
              }
             }
            });
               

//Thread's run method
          }
      }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
    }

}


