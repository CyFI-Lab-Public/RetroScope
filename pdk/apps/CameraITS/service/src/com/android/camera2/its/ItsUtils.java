/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera2.its;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.Image.Plane;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;

public class ItsUtils {
    public static final String TAG = ItsUtils.class.getSimpleName();

    // The externally visible (over adb) base path for the files that are saved by this app
    // to the external media. Currently hardcoded to "/sdcard", which can work on any device
    // by creating a symlink to the actual mount location.
    // TODO: Fix this, by querying mount/vold to get the actual externally visible path.
    public static final String EXT_VISIBLE_BASE_PATH = "/sdcard";

    // State related to output files created by the script.
    public static final String DEFAULT_CAPTURE_DIR = "its";
    public static final String DEFAULT_IMAGE_DIR = "captures";
    public static final String FILE_PREFIX = "IMG_";
    public static final String JPEG_SUFFIX = ".jpg";
    public static final String YUV_SUFFIX = ".yuv";
    public static final String METADATA_SUFFIX = ".json";

    // The indent amount to use when printing the JSON objects out as strings.
    private static final int PPRINT_JSON_INDENT = 2;

    public static void storeCameraCharacteristics(CameraCharacteristics props,
                                               File file)
            throws ItsException {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("cameraProperties", ItsSerializer.serialize(props));
            storeJsonObject(jsonObj, file);
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
    }

    public static void storeResults(CameraCharacteristics props,
                                      CaptureRequest request,
                                      CaptureResult result,
                                      File file)
            throws ItsException {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("cameraProperties", ItsSerializer.serialize(props));
            jsonObj.put("captureRequest", ItsSerializer.serialize(request));
            jsonObj.put("captureResult", ItsSerializer.serialize(result));
            storeJsonObject(jsonObj, file);
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
    }

    public static void storeJsonObject(JSONObject jsonObj, File file)
            throws ItsException {
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.wrap(jsonObj.toString(PPRINT_JSON_INDENT).
                                  getBytes(Charset.defaultCharset()));
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
        FileChannel channel = null;
        try {
            channel = new FileOutputStream(file, false).getChannel();
            channel.write(buf);
            channel.close();
        } catch (FileNotFoundException e) {
            throw new ItsException("Failed to write file: " + file.toString() + ": ", e);
        } catch (IOException e) {
            throw new ItsException("Failed to write file: " + file.toString() + ": ", e);
        }
    }

    public static List<CaptureRequest.Builder> loadRequestList(CameraDevice device, Uri uri)
            throws ItsException {
        return ItsSerializer.deserializeRequestList(device, loadJsonFile(uri));
    }

    public static JSONObject loadJsonFile(Uri uri) throws ItsException {
        FileInputStream input = null;
        try {
            input = new FileInputStream(uri.getPath());
            byte[] fileData = new byte[input.available()];
            input.read(fileData);
            input.close();
            String text = new String(fileData, Charset.defaultCharset());
            return new JSONObject(text);
        } catch (FileNotFoundException e) {
            throw new ItsException("Failed to read file: " + uri.toString() + ": ", e);
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        } catch (IOException e) {
            throw new ItsException("Failed to read file: " + uri.toString() + ": ", e);
        }
    }

    public static int[] getJsonRectFromArray(
            JSONArray a, boolean normalized, int width, int height)
            throws ItsException {
        try {
            // Returns [x,y,w,h]
            if (normalized) {
                return new int[]{(int)Math.floor(a.getDouble(0) * width + 0.5f),
                                 (int)Math.floor(a.getDouble(1) * height + 0.5f),
                                 (int)Math.floor(a.getDouble(2) * width + 0.5f),
                                 (int)Math.floor(a.getDouble(3) * height + 0.5f) };
            } else {
                return new int[]{a.getInt(0),
                                 a.getInt(1),
                                 a.getInt(2),
                                 a.getInt(3) };
            }
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
    }

    public static int getCallbacksPerCapture(int format)
            throws ItsException {
        // Regardless of the format, there is one callback for the CaptureResult object; this
        // prepares the output metadata file.
        int n = 1;

        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.JPEG:
                // A single output image callback is made, with either the JPEG or the YUV data.
                n += 1;
                break;

            default:
                throw new ItsException("Unsupported format: " + format);
        }

        return n;
    }

    public static JSONObject getOutputSpecs(Uri uri)
            throws ItsException {
        FileInputStream input = null;
        try {
            input = new FileInputStream(uri.getPath());
            byte[] fileData = new byte[input.available()];
            input.read(fileData);
            input.close();
            String text = new String(fileData, Charset.defaultCharset());
            JSONObject jsonObjTop = new JSONObject(text);
            if (jsonObjTop.has("outputSurface")) {
                return jsonObjTop.getJSONObject("outputSurface");
            }
            return null;
        } catch (FileNotFoundException e) {
            throw new ItsException("Failed to read file: " + uri.toString() + ": ", e);
        } catch (IOException e) {
            throw new ItsException("Failed to read file: " + uri.toString() + ": ", e);
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getStorageDirectory(Context context, String dirName)
            throws ItsException {
        if (!isExternalStorageWritable()) {
            throw new ItsException(
                    "External storage is not writable, cannot save capture image");
        }
        File file = Environment.getExternalStorageDirectory();
        if (file == null) {
            throw new ItsException("No external storage available");
        }
        File newDir = new File(file, dirName);
        newDir.mkdirs();
        if (!newDir.isDirectory()) {
            throw new ItsException("Could not create directory: " + dirName);
        }
        return newDir;
    }

    public static String getExternallyVisiblePath(Context context, String path)
            throws ItsException {
        File file = Environment.getExternalStorageDirectory();
        if (file == null) {
            throw new ItsException("No external storage available");
        }
        String base = file.toString();
        String newPath = path.replaceFirst(base, EXT_VISIBLE_BASE_PATH);
        if (newPath == null) {
            throw new ItsException("Error getting external path: " + path);
        }
        return newPath;
    }

    public static String getJpegFileName(long fileNumber) {
        return String.format("%s%016x%s", FILE_PREFIX, fileNumber, JPEG_SUFFIX);
    }
    public static String getYuvFileName(long fileNumber) {
        return String.format("%s%016x%s", FILE_PREFIX, fileNumber, YUV_SUFFIX);
    }
    public static String getMetadataFileName(long fileNumber) {
        return String.format("%s%016x%s", FILE_PREFIX, fileNumber, METADATA_SUFFIX);
    }

    public static byte[] getDataFromImage(Image image)
            throws ItsException {
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride, pixelStride;
        byte[] data = null;

        // Read image data
        Plane[] planes = image.getPlanes();

        // Check image validity
        if (!checkAndroidImageFormat(image)) {
            throw new ItsException(
                    "Invalid image format passed to getDataFromImage: " + image.getFormat());
        }

        if (format == ImageFormat.JPEG) {
            // JPEG doesn't have pixelstride and rowstride, treat it as 1D buffer.
            ByteBuffer buffer = planes[0].getBuffer();
            data = new byte[buffer.capacity()];
            buffer.get(data);
            return data;
        } else if (format == ImageFormat.YUV_420_888) {
            int offset = 0;
            data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
            byte[] rowData = new byte[planes[0].getRowStride()];
            for (int i = 0; i < planes.length; i++) {
                ByteBuffer buffer = planes[i].getBuffer();
                rowStride = planes[i].getRowStride();
                pixelStride = planes[i].getPixelStride();
                // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
                int w = (i == 0) ? width : width / 2;
                int h = (i == 0) ? height : height / 2;
                for (int row = 0; row < h; row++) {
                    int bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                    if (pixelStride == bytesPerPixel) {
                        // Special case: optimized read of the entire row
                        int length = w * bytesPerPixel;
                        buffer.get(data, offset, length);
                        // Advance buffer the remainder of the row stride
                        buffer.position(buffer.position() + rowStride - length);
                        offset += length;
                    } else {
                        // Generic case: should work for any pixelStride but slower.
                        // Use use intermediate buffer to avoid read byte-by-byte from
                        // DirectByteBuffer, which is very bad for performance.
                        // Also need avoid access out of bound by only reading the available
                        // bytes in the bytebuffer.
                        int readSize = rowStride;
                        if (buffer.remaining() < readSize) {
                            readSize = buffer.remaining();
                        }
                        buffer.get(rowData, 0, readSize);
                        for (int col = 0; col < w; col++) {
                            data[offset++] = rowData[col * pixelStride];
                        }
                    }
                }
            }
            return data;
        } else {
            throw new ItsException("Unsupported image format: " + format);
        }
    }

    private static boolean checkAndroidImageFormat(Image image) {
        int format = image.getFormat();
        Plane[] planes = image.getPlanes();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return 3 == planes.length;
            case ImageFormat.JPEG:
                return 1 == planes.length;
            default:
                return false;
        }
    }

    public static File getOutputFile(Context context, String name)
        throws ItsException {
        File dir = getStorageDirectory(context, DEFAULT_CAPTURE_DIR + '/' + DEFAULT_IMAGE_DIR);
        if (dir == null) {
            throw new ItsException("Could not output file");
        }
        return new File(dir, name);
    }

    public static String writeImageToFile(Context context, ByteBuffer buf, String name)
        throws ItsException {
        File imgFile = getOutputFile(context, name);
        if (imgFile == null) {
            throw new ItsException("Failed to get path: " + name);
        }
        FileChannel channel = null;
        try {
            channel = new FileOutputStream(imgFile, false).getChannel();
            channel.write(buf);
            channel.close();
        } catch (FileNotFoundException e) {
            throw new ItsException("Failed to write file: " + imgFile.toString(), e);
        } catch (IOException e) {
            throw new ItsException("Failed to write file: " + imgFile.toString(), e);
        }
        return getExternallyVisiblePath(context, imgFile.toString());
    }
}
