package android.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.graphics.ImageFormat;
import java.io.*;
import java.nio.*;
import android.os.Environment;


public class DeadBuffer{
public int width;
public  int height;
public  int stride;
public  int usage;
public  int pixelFormat;
public  int magic;
public  int size;
public  int[] buf;
public  byte[] img_buf;
public  Bitmap bmp;

  public DeadBuffer (int width, int height, int stride, int usage, int pixelFormat, int magic, int size, int[] buf){
    this.width = width;
    this.height = height;
    this.stride = stride;
    this.usage = usage;
    this.pixelFormat = pixelFormat;
    this.magic = magic;
    this.size = size;
    this.buf = buf;
    this.img_buf = null;
  }
 
  public DeadBuffer (int width, int height, int stride, int usage, int pixelFormat, int magic, int size, byte[] buf){
    this.width = width;
    this.height = height;
    this.stride = stride;
    this.usage = usage;
    this.pixelFormat = pixelFormat;
    this.magic = magic;
    this.size = size;
    this.buf = null;
    this.img_buf = buf;
  }

  public DeadBuffer (byte[] buf){
    this.img_buf = buf;
    this.buf = null;
    this.width = 0;
  }

  public Bitmap getBitmap(){
    if(this.width == 0)
    {
      return BitmapFactory.decodeByteArray(this.img_buf, 0, this.img_buf.length);
      // This will likely fail, but we will try in the app to fix it!
    }
    else
    {
      switch(this.pixelFormat) {
        case 286:
        case 285:
        case ImageFormat.NV21:
        case ImageFormat.YUY2:
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          YuvImage yuvImage = new YuvImage(this.img_buf, ImageFormat.NV21, this.width, this.height, null);
          yuvImage.compressToJpeg(new Rect(0, 0,this.width, this.height), 100, out);
          byte[] imageBytes = out.toByteArray();
          return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        default:
          Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
          
          if (pixelFormat==1){
            //RGBA to ARGB
            for (int i=0; i<size; i++){
              //Swap 2nd and 4th byte now
              buf[i] = ( ((buf[i] << 24) >>> 8) | ((buf[i] << 8) >>> 24) | ( buf[i] & 0xff00ff00 ) );
            }
          }

          // transparency shit!
          for (int i=0; i<size; i++){
            // Turn off the transparency bits!
            buf[i] = buf[i] | 0xff000000;
          }
          
          bmp = Bitmap.createBitmap(width, height, conf); // this creates a MUTABLE bitmap
          bmp.setPixels(buf, 0, stride, 0, 0, width, height);
          return bmp;
      }
    }
  }

}
