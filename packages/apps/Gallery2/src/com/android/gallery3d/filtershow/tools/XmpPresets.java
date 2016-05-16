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

package com.android.gallery3d.filtershow.tools;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.util.XmpUtilHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class XmpPresets {
    public static final String
            XMP_GOOGLE_FILTER_NAMESPACE = "http://ns.google.com/photos/1.0/filter/";
    public static final String XMP_GOOGLE_FILTER_PREFIX = "AFltr";
    public static final String XMP_SRC_FILE_URI = "SourceFileUri";
    public static final String XMP_FILTERSTACK = "filterstack";

    private static final String LOGTAG = "XmpPresets";

    public static class XMresults {
        public String presetString;
        public ImagePreset preset;
        public Uri originalimage;
    }

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace(
                    XMP_GOOGLE_FILTER_NAMESPACE, XMP_GOOGLE_FILTER_PREFIX);
        } catch (XMPException e) {
            Log.e(LOGTAG, "Register XMP name space failed", e);
        }
    }

    public static void writeFilterXMP(
            Context context, Uri srcUri, File dstFile, ImagePreset preset) {
        InputStream is = null;
        XMPMeta xmpMeta = null;
        try {
            is = context.getContentResolver().openInputStream(srcUri);
            xmpMeta = XmpUtilHelper.extractXMPMeta(is);
        } catch (FileNotFoundException e) {

        } finally {
            Utils.closeSilently(is);
        }

        if (xmpMeta == null) {
            xmpMeta = XMPMetaFactory.create();
        }
        try {
            xmpMeta.setProperty(XMP_GOOGLE_FILTER_NAMESPACE,
                    XMP_SRC_FILE_URI, srcUri.toString());
            xmpMeta.setProperty(XMP_GOOGLE_FILTER_NAMESPACE,
                    XMP_FILTERSTACK, preset.getJsonString(ImagePreset.JASON_SAVED));
        } catch (XMPException e) {
            Log.v(LOGTAG, "Write XMP meta to file failed:" + dstFile.getAbsolutePath());
            return;
        }

        if (!XmpUtilHelper.writeXMPMeta(dstFile.getAbsolutePath(), xmpMeta)) {
            Log.v(LOGTAG, "Write XMP meta to file failed:" + dstFile.getAbsolutePath());
        }
    }

    public static XMresults extractXMPData(
            Context context, MasterImage mMasterImage, Uri uriToEdit) {
        XMresults ret = new XMresults();

        InputStream is = null;
        XMPMeta xmpMeta = null;
        try {
            is = context.getContentResolver().openInputStream(uriToEdit);
            xmpMeta = XmpUtilHelper.extractXMPMeta(is);
        } catch (FileNotFoundException e) {
        } finally {
            Utils.closeSilently(is);
        }

        if (xmpMeta == null) {
            return null;
        }

        try {
            String strSrcUri = xmpMeta.getPropertyString(XMP_GOOGLE_FILTER_NAMESPACE,
                    XMP_SRC_FILE_URI);

            if (strSrcUri != null) {
                String filterString = xmpMeta.getPropertyString(XMP_GOOGLE_FILTER_NAMESPACE,
                        XMP_FILTERSTACK);

                Uri srcUri = Uri.parse(strSrcUri);
                ret.originalimage = srcUri;

                ret.preset = new ImagePreset();
                ret.presetString = filterString;
                boolean ok = ret.preset.readJsonFromString(filterString);
                if (!ok) {
                    return null;
                }
                return ret;
            }
        } catch (XMPException e) {
            e.printStackTrace();
        }

        return null;
    }
}
