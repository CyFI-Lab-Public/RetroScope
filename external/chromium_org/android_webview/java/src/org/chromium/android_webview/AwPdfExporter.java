// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.android_webview;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ValueCallback;

import java.io.OutputStream;

import org.chromium.base.CalledByNative;
import org.chromium.base.JNINamespace;

/**
 * Export the android webview as a PDF.
 * @TODO(sgurun) explain the ownership of this class and its native counterpart
 */
@JNINamespace("android_webview")
public class AwPdfExporter {

    private static final String TAG = "AwPdfExporter";
    private int mNativeAwPdfExporter;
    // TODO(sgurun) result callback should return an int/object indicating errors.
    // potential errors: invalid print parameters, already pending, IO error
    private ValueCallback<Boolean> mResultCallback;
    private AwPdfExportAttributes mAttributes;
    private ParcelFileDescriptor mFd;
    // Maintain a reference to the top level object (i.e. WebView) since in a common
    // use case (offscreen webview) application may expect the framework's print manager
    // to own the Webview (via PrintDocumentAdapter).
    private final ViewGroup mContainerView;

    AwPdfExporter(ViewGroup containerView) {
        mContainerView = containerView;
    }

    public void exportToPdf(final ParcelFileDescriptor fd, AwPdfExportAttributes attributes,
            ValueCallback<Boolean> resultCallback, CancellationSignal cancellationSignal) {

        if (fd == null) {
            throw new IllegalArgumentException("fd cannot be null");
        }
        if (resultCallback == null) {
            throw new IllegalArgumentException("resultCallback cannot be null");
        }
        if (mResultCallback != null) {
            throw new IllegalStateException("printing is already pending");
        }
        if (mNativeAwPdfExporter == 0) {
            resultCallback.onReceiveValue(false);
            return;
        }
        mResultCallback = resultCallback;
        mAttributes = attributes;
        mFd = fd;
        nativeExportToPdf(mNativeAwPdfExporter, mFd.getFd(), cancellationSignal);
    }

    @CalledByNative
    private void setNativeAwPdfExporter(int nativePdfExporter) {
        mNativeAwPdfExporter = nativePdfExporter;
        // Handle the cornercase that Webview.Destroy is called before the native side
        // has a chance to complete the pdf exporting.
        if (nativePdfExporter == 0 && mResultCallback != null) {
            mResultCallback.onReceiveValue(false);
            mResultCallback = null;
        }
    }

    @CalledByNative
    private void didExportPdf(boolean success) {
        mResultCallback.onReceiveValue(success);
        mResultCallback = null;
        mAttributes = null;
        // The caller should close the file.
        mFd = null;
    }

    @CalledByNative
    private int getPageWidth() {
        return mAttributes.pageWidth;
    }

    @CalledByNative
    private int getPageHeight() {
        return mAttributes.pageHeight;
    }

    @CalledByNative
    private int getDpi() {
        return mAttributes.dpi;
    }

    @CalledByNative
    private int getLeftMargin() {
        return mAttributes.leftMargin;
    }

    @CalledByNative
    private int getRightMargin() {
        return mAttributes.rightMargin;
    }

    @CalledByNative
    private int getTopMargin() {
        return mAttributes.topMargin;
    }

    @CalledByNative
    private int getBottomMargin() {
        return mAttributes.bottomMargin;
    }

    private native void nativeExportToPdf(int nativeAwPdfExporter, int fd,
            CancellationSignal cancellationSignal);
}
