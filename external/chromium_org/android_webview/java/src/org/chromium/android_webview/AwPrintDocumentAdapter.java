// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.android_webview;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import android.util.Log;

import android.webkit.ValueCallback;


/**
 * Adapter for printing Webview. This class implements the abstract
 * system class PrintDocumentAdapter and hides all printing details from
 * the developer.
 */
public class AwPrintDocumentAdapter extends PrintDocumentAdapter {

    private static final String TAG = "AwPrintDocumentAdapter";

    private AwPdfExporter mPdfExporter;
    private PrintAttributes mAttributes;

    /**
     * Constructor.
     *
     * @param pdfExporter The PDF exporter to export the webview contents to a PDF file.
     */
    public AwPrintDocumentAdapter(AwPdfExporter pdfExporter) {
        mPdfExporter = pdfExporter;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
            CancellationSignal cancellationSignal, LayoutResultCallback callback,
            Bundle metadata) {
        mAttributes = newAttributes;
        // TODO(sgurun) pass a meaningful string once b/10705082 is resolved
        PrintDocumentInfo documentInfo = new PrintDocumentInfo
                .Builder("webview")
                .build();
        // TODO(sgurun) once componentization is done, do layout changes and
        // generate PDF here, set the page range information to documentinfo
        // and call onLayoutFinished with true/false depending on whether
        // layout actually changed.
        callback.onLayoutFinished(documentInfo, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
            CancellationSignal cancellationSignal, WriteResultCallback callback) {
        // TODO(sgurun) sometimes we receive spurious onWrite. ignore it.
        if (mAttributes.getMediaSize() == null) {
            throw new  IllegalArgumentException("attributes must specify a media size");
        }
        if (mAttributes.getResolution() == null) {
            throw new IllegalArgumentException("attributes must specify print resolution");
        }
        if (mAttributes.getMinMargins() == null) {
            throw new IllegalArgumentException("attributes must specify margins");
        }
        // TODO(sgurun) get rid of AwPdfExportAttributes after upstreaming
        // and move this logic to AwPdfExporter
        AwPdfExportAttributes pdfAttributes = new AwPdfExportAttributes();
        pdfAttributes.pageWidth = mAttributes.getMediaSize().getWidthMils();
        pdfAttributes.pageHeight = mAttributes.getMediaSize().getHeightMils();
        pdfAttributes.dpi = getPrintDpi(mAttributes);
        pdfAttributes.leftMargin = mAttributes.getMinMargins().getLeftMils();
        pdfAttributes.rightMargin = mAttributes.getMinMargins().getRightMils();
        pdfAttributes.topMargin = mAttributes.getMinMargins().getTopMils();
        pdfAttributes.bottomMargin = mAttributes.getMinMargins().getBottomMils();
        exportPdf(destination, pdfAttributes, cancellationSignal, callback);
    }

    private static int getPrintDpi(PrintAttributes attributes) {
        // TODO(sgurun) android print attributes support horizontal and
        // vertical DPI. Chrome has only one DPI. Revisit this.
        int horizontalDpi = attributes.getResolution().getHorizontalDpi();
        int verticalDpi = attributes.getResolution().getVerticalDpi();
        if (horizontalDpi != verticalDpi) {
            Log.w(TAG, "Horizontal and vertical DPIs differ. Using horizontal DPI " +
                    " hDpi=" + horizontalDpi + " vDPI=" + verticalDpi);
        }
        return horizontalDpi;
    }

    private void exportPdf(final ParcelFileDescriptor destination,
            final AwPdfExportAttributes attributes,
            final CancellationSignal signal,
            final WriteResultCallback callback) {
        mPdfExporter.exportToPdf(destination, attributes, new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        if (value) {
                            callback.onWriteFinished(new PageRange[] {PageRange.ALL_PAGES});
                        } else {
                            // TODO(sgurun) localized error message
                            callback.onWriteFailed(null);
                        }
                    }
                }, signal);
    }
}

