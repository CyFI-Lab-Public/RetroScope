// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef BASE_BASE_API_H_
#define BASE_BASE_API_H_
#pragma once

#ifdef ANDROID
// Do not upstream, this will be added later upstream,
// after this file is renamed to base_export.h. Not
// cherry-picking that since the #define's also change
#define BASE_API __attribute__((visibility("default")))
#else

#if defined(WIN32) && defined(BASE_DLL)
#if defined(BASE_IMPLEMENTATION)
#define BASE_API __declspec(dllexport)
#else
#define BASE_API __declspec(dllimport)
#endif  // defined(BASE_IMPLEMENTATION)
#else
#define BASE_API
#endif

#endif // ANDROID

#endif  // BASE_BASE_API_H_
