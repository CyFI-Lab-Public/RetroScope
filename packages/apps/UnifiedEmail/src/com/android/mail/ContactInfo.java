/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail;

import android.graphics.Bitmap;
import android.net.Uri;

public class ContactInfo {
    public final Uri contactUri;
    public final Integer status;
    public final byte[] photoBytes;
    public final Bitmap photo;

    public ContactInfo(Uri contactUri, Integer status) {
        this(contactUri, status, null, null);
    }

    public ContactInfo(Uri contactUri, Integer status, byte[] photoBytes) {
        this(contactUri, status, photoBytes, null);
    }

    public ContactInfo(Uri contactUri, Integer status, Bitmap photo) {
        this(contactUri, status, null, photo);
    }

    private ContactInfo(Uri contactUri, Integer status, byte[] photoBytes, Bitmap photo) {
        this.contactUri = contactUri;
        this.status = status;
        this.photoBytes = photoBytes;
        this.photo = photo;
    }

    @Override
    public String toString() {
        return "{status=" + status + " photo=" + photo + "}";
    }
}
