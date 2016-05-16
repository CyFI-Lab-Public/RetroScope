/*
 * Copyright 2010, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifdef ANDROID

// TODO: This file factors the Autofill callbacks out of RenderViewHost
// so that we don't need to pull in the whole RenderViewHost on Android.
// It would be good to refactor the Chromium RenderViewHost to use this
// class too so that AutoFill doesn't need the RVH at all.

#ifndef CHROME_BROWSER_AUTOFILL_AUTOFILL_HOST_H_
#define CHROME_BROWSER_AUTOFILL_AUTOFILL_HOST_H_
#pragma once

#include "base/string16.h"
#include "webkit/glue/form_data.h"

#include <vector>

class AutoFillHost {
public:
  virtual ~AutoFillHost() { }
  virtual void AutoFillSuggestionsReturned(const std::vector<string16>& values, const std::vector<string16>& labels, const std::vector<string16>& icons, const std::vector<int>& unique_ids) = 0;
  virtual void AutoFillFormDataFilled(int query_id, const webkit_glue::FormData& form) = 0;
};

#endif // CHROME_BROWSER_AUTOFILL_AUTOFILL_HOST_H_
#endif // ANDROID
