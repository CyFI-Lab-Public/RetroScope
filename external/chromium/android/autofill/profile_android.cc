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

#include "android/autofill/profile_android.h"

#include "chrome/browser/autofill/personal_data_manager.h"

// We only need the Profile constructor (but that is needed since
// ProfileImplAndroid is a subclass of Profile). To avoid compiling the file,
// the constructor is included below.
//
// Taken from chrome/browser/profiles/profile.cc
Profile::Profile()
    : restored_last_session_(false),
      accessibility_pause_level_(0) {
}

ProfileImplAndroid::ProfileImplAndroid(const FilePath& path)
  : path_(path)
{
}

ProfileImplAndroid::~ProfileImplAndroid()
{
}

Profile* ProfileImplAndroid::GetOriginalProfile()
{
  return this;
}


PersonalDataManager* ProfileImplAndroid::GetPersonalDataManager()
{
  if (!personal_data_) {
    personal_data_ = new PersonalDataManager();
    personal_data_->Init(this);
  }

  return personal_data_.get();
}


PrefService* ProfileImplAndroid::GetPrefs() {
  return NULL;
}

net::URLRequestContextGetter* ProfileImplAndroid::GetRequestContext() {
  return url_request_context_getter_.get();
}

Profile* Profile::CreateProfile(const FilePath& path) {
    return new ProfileImplAndroid(path);
}
