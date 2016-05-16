/*
 * Copyright (C) 2011 The Android Open Source Project
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
package android.opengl.cts;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import com.android.cts.stub.R;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES20;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

public class CompressedTextureLoader {
    private static final String TAG = "CompressedTextureLoader";

    public static final String TEXTURE_UNCOMPRESSED = "UNCOMPRESSED";
    public static final String TEXTURE_ETC1 = "ETC1";
    public static final String TEXTURE_S3TC = "S3TC";
    public static final String TEXTURE_ATC = "ATC";
    public static final String TEXTURE_PVRTC = "PVRTC";

    public static class Texture {
        public Texture(int width, int height, int internalformat, ByteBuffer data,
                       String formatName) {
            mWidth = width;
            mHeight = height;
            mInternalFormat = internalformat;
            mData = data;
            mFormatName = formatName;
        }

        /**
         * Get the width of the texture in pixels.
         * @return the width of the texture in pixels.
         */
        public int getWidth() { return mWidth; }

        /**
         * Get the height of the texture in pixels.
         * @return the width of the texture in pixels.
         */
        public int getHeight() { return mHeight; }

        /**
         * Get the compressed data of the texture.
         * @return the texture data.
         */
        public ByteBuffer getData() { return mData; }

        /**
         * Get the format of the texture.
         * @return the internal format.
         */
        public int getFormat() { return mInternalFormat; }

        /**
         * Get the format of the texture.
         * @return the internal format.
         */
        public boolean isSupported() { return isFormatSupported(mFormatName); }

        private int mWidth;
        private int mHeight;
        private int mInternalFormat;
        private ByteBuffer mData;
        private String mFormatName;
    }

    /*  .pvr header is described by the following c struct
        typedef struct PVR_TEXTURE_HEADER_TAG{
            unsigned int  dwHeaderSize;   // size of the structure
            unsigned int  dwHeight;    // height of surface to be created
            unsigned int  dwWidth;    // width of input surface
            unsigned int  dwMipMapCount;   // number of MIP-map levels requested
            unsigned int  dwpfFlags;   // pixel format flags
            unsigned int  dwDataSize;   // Size of the compress data
            unsigned int  dwBitCount;   // number of bits per pixel
            unsigned int  dwRBitMask;   // mask for red bit
            unsigned int  dwGBitMask;   // mask for green bits
            unsigned int  dwBBitMask;   // mask for blue bits
            unsigned int  dwAlphaBitMask;   // mask for alpha channel
            unsigned int  dwPVR;    // should be 'P' 'V' 'R' '!'
            unsigned int  dwNumSurfs;   //number of slices for volume textures or skyboxes
        } PVR_TEXTURE_HEADER;
    */
    static final int PVR_HEADER_SIZE = 13 * 4;
    static final int PVR_2BPP = 24;
    static final int PVR_4BPP = 25;
    static final int PVR_MAGIC_NUMBER = 559044176;

    static final int GL_COMPRESSED_RGB_PVRTC_4BPPV1_IMG = 0x8C00;
    static final int GL_COMPRESSED_RGB_PVRTC_2BPPV1_IMG = 0x8C01;
    static final int GL_COMPRESSED_RGBA_PVRTC_4BPPV1_IMG = 0x8C02;
    static final int GL_COMPRESSED_RGBA_PVRTC_2BPPV1_IMG = 0x8C03;

    static class PVRHeader {
        int mHeaderSize;   // size of the structure
        int mHeight;    // height of surface to be created
        int mWidth;    // width of input surface
        int mMipMapCount;   // number of MIP-map levels requested
        int mpfFlags;   // pixel format flags
        int mDataSize;   // Size of the compress data
        int mBitCount;   // number of bits per pixel
        int mRBitMask;   // mask for red bit
        int mGBitMask;   // mask for green bits
        int mBBitMask;   // mask for blue bits
        int mAlphaBitMask;   // mask for alpha channel
        int mPVR;    // should be 'P' 'V' 'R' '!'
        int mNumSurfs;   //number of slices for volume textures or skyboxes
    }

    protected static PVRHeader readPVRHeader(InputStream is) {

        byte[] headerData = new byte[PVR_HEADER_SIZE];
        try {
            is.read(headerData);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read data");
        }

        ByteBuffer headerBuffer = ByteBuffer.allocateDirect(PVR_HEADER_SIZE)
                .order(ByteOrder.nativeOrder());
        headerBuffer.put(headerData, 0, PVR_HEADER_SIZE).position(0);

        PVRHeader header = new PVRHeader();

        header.mHeaderSize = headerBuffer.getInt();
        header.mHeight = headerBuffer.getInt();
        header.mWidth = headerBuffer.getInt();
        header.mMipMapCount = headerBuffer.getInt();
        header.mpfFlags = headerBuffer.getInt();
        header.mDataSize = headerBuffer.getInt();
        header.mBitCount = headerBuffer.getInt();
        header.mRBitMask = headerBuffer.getInt();
        header.mGBitMask = headerBuffer.getInt();
        header.mBBitMask = headerBuffer.getInt();
        header.mAlphaBitMask = headerBuffer.getInt();
        header.mPVR = headerBuffer.getInt();
        header.mNumSurfs = headerBuffer.getInt();

        if (header.mHeaderSize != PVR_HEADER_SIZE ||
            header.mPVR != PVR_MAGIC_NUMBER) {
            throw new RuntimeException("Invalid header data");
        }

        return header;
    }

    public static Texture loadTextureATC(Resources res, int id) {
        Texture tex = new Texture(0, 0, 0, null, "Stub!");
        return tex;
    }

    private static ETC1Util.ETC1Texture compressTexture(Buffer input,
                                                        int width, int height,
                                                        int pixelSize, int stride){
        int encodedImageSize = ETC1.getEncodedDataSize(width, height);
        ByteBuffer compressedImage = ByteBuffer.allocateDirect(encodedImageSize).
            order(ByteOrder.nativeOrder());
        ETC1.encodeImage(input, width, height, pixelSize, stride, compressedImage);
        return new ETC1Util.ETC1Texture(width, height, compressedImage);
    }

    public static Texture createFromUncompressedETC1(Bitmap bitmap) {
        int dataSize = bitmap.getRowBytes() * bitmap.getHeight();

        ByteBuffer dataBuffer;
        dataBuffer = ByteBuffer.allocateDirect(dataSize).order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(dataBuffer);
        dataBuffer.position(0);

        int bytesPerPixel = bitmap.getRowBytes() / bitmap.getWidth();
        ETC1Util.ETC1Texture compressed = compressTexture(dataBuffer,
                                                          bitmap.getWidth(),
                                                          bitmap.getHeight(),
                                                          bytesPerPixel,
                                                          bitmap.getRowBytes());

        Texture tex = new Texture(compressed.getWidth(), compressed.getHeight(),
                                  ETC1.ETC1_RGB8_OES, compressed.getData(), TEXTURE_ETC1);

        return tex;
    }

    private static ByteBuffer read(InputStream is, int dataSize) {
        ByteBuffer dataBuffer;
        dataBuffer = ByteBuffer.allocateDirect(dataSize).order(ByteOrder.nativeOrder());
        byte[] ioBuffer = new byte[4096];
        for (int i = 0; i < dataSize; ) {
            int chunkSize = Math.min(ioBuffer.length, dataSize - i);
            try {
                is.read(ioBuffer, 0, chunkSize);
            } catch (Exception e) {
                throw new RuntimeException("Unable to read data");
            }
            dataBuffer.put(ioBuffer, 0, chunkSize);
            i += chunkSize;
        }
        dataBuffer.position(0);
        return dataBuffer;
    }

    public static Texture loadTexturePVRTC(Resources res, int id) {
        InputStream is = null;
        try {
            is = res.openRawResource(id);
        } catch (Exception e) {
            throw new RuntimeException("Unable to open resource " + id);
        }

        PVRHeader header = readPVRHeader(is);

        int format = header.mpfFlags & 0xFF;
        int internalFormat = 0;
        if (format == PVR_2BPP && header.mAlphaBitMask == 1) {
            internalFormat = GL_COMPRESSED_RGBA_PVRTC_2BPPV1_IMG;
        } else if (format == PVR_2BPP && header.mAlphaBitMask == 0) {
            internalFormat = GL_COMPRESSED_RGB_PVRTC_2BPPV1_IMG;
        } else if (format == PVR_4BPP && header.mAlphaBitMask == 1) {
            internalFormat = GL_COMPRESSED_RGBA_PVRTC_4BPPV1_IMG;
        } else if (format == PVR_4BPP && header.mAlphaBitMask == 0) {
            internalFormat = GL_COMPRESSED_RGB_PVRTC_4BPPV1_IMG;
        }

        // only load the first mip level for now
        int dataSize = (header.mWidth * header.mHeight * header.mBitCount) >> 3;
        ByteBuffer dataBuffer = read(is, dataSize);
        Texture tex = new Texture(header.mWidth, header.mHeight,
                                  internalFormat, dataBuffer,
                                  TEXTURE_PVRTC);
        try {
            is.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to close resource stream " + id);
        }
        return tex;
    }

    /* DDS Header is described by the following structs
       typedef struct {
          DWORD           dwSize;
          DWORD           dwFlags;
          DWORD           dwHeight;
          DWORD           dwWidth;
          DWORD           dwPitchOrLinearSize;
          DWORD           dwDepth;
          DWORD           dwMipMapCount;
          DWORD           dwReserved1[11];
          DDS_PIXELFORMAT ddspf;
          DWORD           dwCaps;
          DWORD           dwCaps2;
          DWORD           dwCaps3;
          DWORD           dwCaps4;
          DWORD           dwReserved2;
        } DDS_HEADER;

        struct DDS_PIXELFORMAT {
          DWORD dwSize;
          DWORD dwFlags;
          DWORD dwFourCC;
          DWORD dwRGBBitCount;
          DWORD dwRBitMask;
          DWORD dwGBitMask;
          DWORD dwBBitMask;
          DWORD dwABitMask;
        };

        In the file it looks like this
        DWORD               dwMagic;
        DDS_HEADER          header;
        DDS_HEADER_DXT10    header10; // If the DDS_PIXELFORMAT dwFlags is set to DDPF_FOURCC
                                      // and dwFourCC is DX10

    */

    static final int DDS_HEADER_STRUCT_SIZE = 124;
    static final int DDS_PIXELFORMAT_STRUCT_SIZE = 32;
    static final int DDS_HEADER_SIZE = 128;
    static final int DDS_MAGIC_NUMBER = 0x20534444;
    static final int DDS_DDPF_FOURCC = 0x4;
    static final int DDS_DXT1 = 0x31545844;
    static final int DDS_DXT5 = 0x35545844;

    static final int COMPRESSED_RGB_S3TC_DXT1_EXT = 0x83F0;
    static final int COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1;
    static final int COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;

    static class DDSHeader {
        int mMagic;
        int mSize;
        int mFlags;
        int mHeight;
        int mWidth;
        int mPitchOrLinearSize;
        int mDepth;
        int mMipMapCount;
        int[] mReserved1;
        // struct DDS_PIXELFORMAT {
            int mPixelFormatSize;
            int mPixelFormatFlags;
            int mPixelFormatFourCC;
            int mPixelFormatRGBBitCount;
            int mPixelFormatRBitMask;
            int mPixelFormatGBitMask;
            int mPixelFormatBBitMask;
            int mPixelFormatABitMask;
        // };
        int mCaps;
        int mCaps2;
        int mCaps3;
        int mCaps4;
        int mReserved2;

        DDSHeader() {
            mReserved1 = new int[11];
        }
    }

    protected static DDSHeader readDDSHeader(InputStream is) {

        byte[] headerData = new byte[DDS_HEADER_SIZE];
        try {
            is.read(headerData);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read data");
        }

        ByteBuffer headerBuffer = ByteBuffer.allocateDirect(DDS_HEADER_SIZE)
                .order(ByteOrder.nativeOrder());
        headerBuffer.put(headerData, 0, DDS_HEADER_SIZE).position(0);

        DDSHeader header = new DDSHeader();

        header.mMagic = headerBuffer.getInt();
        header.mSize = headerBuffer.getInt();
        header.mFlags = headerBuffer.getInt();
        header.mHeight = headerBuffer.getInt();
        header.mWidth = headerBuffer.getInt();
        header.mPitchOrLinearSize = headerBuffer.getInt();
        header.mDepth = headerBuffer.getInt();
        header.mMipMapCount = headerBuffer.getInt();
        for (int i = 0; i < header.mReserved1.length; i ++) {
            header.mReserved1[i] = headerBuffer.getInt();
        }
        // struct DDS_PIXELFORMAT {
            header.mPixelFormatSize = headerBuffer.getInt();
            header.mPixelFormatFlags = headerBuffer.getInt();
            header.mPixelFormatFourCC = headerBuffer.getInt();
            header.mPixelFormatRGBBitCount = headerBuffer.getInt();
            header.mPixelFormatRBitMask = headerBuffer.getInt();
            header.mPixelFormatGBitMask = headerBuffer.getInt();
            header.mPixelFormatBBitMask = headerBuffer.getInt();
            header.mPixelFormatABitMask = headerBuffer.getInt();
        // };
        header.mCaps = headerBuffer.getInt();
        header.mCaps2 = headerBuffer.getInt();
        header.mCaps3 = headerBuffer.getInt();
        header.mCaps4 = headerBuffer.getInt();
        header.mReserved2 = headerBuffer.getInt();

        if (header.mSize != DDS_HEADER_STRUCT_SIZE ||
            header.mPixelFormatSize != DDS_PIXELFORMAT_STRUCT_SIZE ||
            header.mMagic != DDS_MAGIC_NUMBER) {
            throw new RuntimeException("Invalid header data");
        }

        return header;
    }

    // Very simple loader that only reads in the header and a DXT1 mip level 0
    public static Texture loadTextureDXT(Resources res, int id) {
        InputStream is = null;
        try {
            is = res.openRawResource(id);
        } catch (Exception e) {
            throw new RuntimeException("Unable to open resource " + id);
        }

        DDSHeader header = readDDSHeader(is);

        if (header.mPixelFormatFlags != DDS_DDPF_FOURCC) {
            throw new RuntimeException("Unsupported DXT data");
        }

        int internalFormat = 0;
        int bpp = 0;
        switch (header.mPixelFormatFourCC) {
        case DDS_DXT1:
            internalFormat = COMPRESSED_RGB_S3TC_DXT1_EXT;
            bpp = 4;
            break;
        case DDS_DXT5:
            internalFormat = COMPRESSED_RGBA_S3TC_DXT5_EXT;
            bpp = 8;
            break;
        default:
            throw new RuntimeException("Unsupported DXT data");
        }

        // only load the first mip level for now
        int dataSize = (header.mWidth * header.mHeight * bpp) >> 3;
        if (dataSize != header.mPitchOrLinearSize) {
            throw new RuntimeException("Expected data and header mismatch");
        }
        ByteBuffer dataBuffer = read(is, dataSize);

        Texture tex = new Texture(header.mWidth, header.mHeight, internalFormat,
                                  dataBuffer, TEXTURE_S3TC);
        return tex;
    }

    static HashMap<String, Boolean> sExtensionMap;
    static HashMap<String, Boolean> sFormatMap;

    private static synchronized void updateSupportedFormats() {
        if (sExtensionMap != null) {
            return;
        }

        sExtensionMap = new HashMap<String, Boolean>();
        sFormatMap = new HashMap<String, Boolean>();
        String extensionList = GLES20.glGetString(GLES20.GL_EXTENSIONS);

        for (String extension : extensionList.split(" ")) {
            sExtensionMap.put(extension, true);
        }

        // Check ETC1
        sFormatMap.put(TEXTURE_ETC1, ETC1Util.isETC1Supported());
        // Check ATC
        if (sExtensionMap.get("GL_AMD_compressed_ATC_texture") != null ||
            sExtensionMap.get("GL_ATI_compressed_texture_atitc") != null ||
            sExtensionMap.get("GL_ATI_texture_compression_atitc") != null) {
            sFormatMap.put(TEXTURE_ATC, true);
        }
        // Check DXT
        if (sExtensionMap.get("GL_EXT_texture_compression_dxt1") != null ||
            sExtensionMap.get("GL_EXT_texture_compression_s3tc") != null ||
            sExtensionMap.get("OES_texture_compression_S3TC") != null) {
            sFormatMap.put(TEXTURE_S3TC, true);
        }
        // Check DXT
        if (sExtensionMap.get("GL_IMG_texture_compression_pvrtc") != null) {
            sFormatMap.put(TEXTURE_PVRTC, true);
        }

        /*Log.i(TAG, "mIsSupportedETC1 " + sFormatMap.get(TEXTURE_ETC1));
        Log.i(TAG, "mIsSupportedATC " + sFormatMap.get(TEXTURE_ATC));
        Log.i(TAG, "mIsSupportedDXT " + sFormatMap.get(TEXTURE_S3TC));
        Log.i(TAG, "mIsSupportedPVRTC " + sFormatMap.get(TEXTURE_PVRTC));*/
    }

    private static boolean isFormatSupported(String format) {
        updateSupportedFormats();
        Boolean supported = sFormatMap.get(format);
        return supported != null ? supported : false;
    }
}
