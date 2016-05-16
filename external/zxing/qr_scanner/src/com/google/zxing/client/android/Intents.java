/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android;

/**
 * This class provides the constants to use when sending an Intent to Barcode Scanner.
 * These strings are effectively API and cannot be changed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class Intents {
  private Intents() {
  }

  public static final class Scan {
    /**
     * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return
     * the results.
     */
    public static final String ACTION = "com.google.zxing.client.android.YOUTUBE_SCAN";

    /**
     * @see com.google.zxing.DecodeHintType#CHARACTER_SET
     */
    public static final String CHARACTER_SET = "CHARACTER_SET";

    /**
     * Optional parameters to specify the width and height of the scanning rectangle in pixels.
     * The app will try to honor these, but will clamp them to the size of the preview frame.
     * You should specify both or neither, and pass the size as an int.
     */
    public static final String WIDTH = "SCAN_WIDTH";
    public static final String HEIGHT = "SCAN_HEIGHT";

    private Scan() {
    }
  }
}
