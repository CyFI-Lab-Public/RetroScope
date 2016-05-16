package android.view;

import android.view.GLES20DisplayList;
import java.lang.String;
import android.view.WindowManager;
import android.graphics.Canvas;

public class ZmbSurface{
    public boolean more_dls;
    public native boolean initImage(WindowManager wm, int bitmap_width, int bitmap_height);
    public native GLES20DisplayList getDL(); 
    public ZmbSurface(WindowManager wm, int bitmap_width, int bitmap_height){
      more_dls = initImage(wm, bitmap_width, bitmap_height);
    }
}

