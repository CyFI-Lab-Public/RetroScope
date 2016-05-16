/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef RSL_ASSERT_H
#define RSL_ASSERT_H

#if defined(__cplusplus)
extern "C" {
#endif

extern void ASSERT_FAILED(char const *file,
                          unsigned line,
                          char const *expr);

#if defined(__cplusplus)
} // extern "C"
#endif

#ifdef RSL_NDEBUG

#define rsl_assert(EXPR) \
  do { } while (0)

#else

#define rsl_assert(EXPR)                                      \
  do {                                                        \
    if (!(EXPR)) {                                            \
      ASSERT_FAILED(__FILE__, __LINE__, #EXPR);               \
    }                                                         \
  } while (0)

#endif

#endif // RSL_ASSERT_H
