package com.android.ex.carousel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

public class CarouselViewUtilities {
    /**
     * Debug utility to write the given bitmap to a file.
     *
     * @param context calling context
     * @param bitmap the bitmap to write
     * @param filename the name of the file to write
     * @return
     */
    public static boolean writeBitmapToFile(Context context, Bitmap bitmap, String filename) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File file = new File(path, filename);
        boolean result = false;
        try {
            path.mkdirs();
            OutputStream os = new FileOutputStream(file);
            MediaScannerConnection.scanFile(context, new String[] { file.toString() }, null, null);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            result = true;
        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
        return result;
    }

}
