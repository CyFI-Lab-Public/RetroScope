// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.android_webview;

/**
 * The android_webview specific PDF export attributes.
 */
public class AwPdfExportAttributes {

    public int dpi;
    // For parameters below, units are in mils (1/1000 of inches).
    public int pageWidth;
    public int pageHeight;
    public int leftMargin;
    public int rightMargin;
    public int topMargin;
    public int bottomMargin;
};