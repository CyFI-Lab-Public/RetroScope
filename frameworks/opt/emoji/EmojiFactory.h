/*
 * Copyright (C) 2009 The Android Open Source Project
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

#ifndef ANDROID_EMOJI_FACTORY_H
#define ANDROID_EMOJI_FACTORY_H

namespace android {

// Abstract class for EmojiFactory.
//
// Here, PUA (or Android PUA) means Unicode PUA defined for various emoji. The
// PUA supports emoji of DoCoMo, KDDI, Softbank and Goomoji. Each PUA defined
// by the other vendors (careers) are called Vendor Specific PUA (vsp).
// For more information, go
// http://unicode.org/~mdavis/08080r-emoji-proposal/ (tentative)
class EmojiFactory {
 public:
  virtual ~EmojiFactory() {}
  // Returns binary image data corresponding to "pua". The size of binary is
  // stored to "size". Returns NULL if there's no mapping from the "pua" to a
  // specific image. Currently, image format is all (animated) gif.
  //
  // TODO(dmiyakawa): there should be a way to tell users the format of the
  // binary.
  virtual const char *GetImageBinaryFromAndroidPua(int pua, int *size) = 0;

  // Returns binary image data corresponding to "sjis", which is defined by
  // each career. Returns NULL when there's no mapping for "sjis".
  virtual const char *GetImageBinaryFromVendorSpecificSjis(unsigned short sjis,
                                                           int *size) {
    return GetImageBinaryFromAndroidPua(
        GetAndroidPuaFromVendorSpecificSjis(sjis), size);
  }

  // Returns binary image data corresponding to Vendor-specific PUA "vsp".
  // Returns NULL when there's no mapping for "vsp".
  virtual const char *GetImageBinaryFromVendorSpecificPua(int vsp,
                                                          int *size) {
    return GetImageBinaryFromAndroidPua(
        GetAndroidPuaFromVendorSpecificPua(vsp), size);
  }

  // Returns Android PUA corresponding to "sjis". Returns -1 when there's no
  // mapping from "sjis" to a Android PUA.
  virtual int GetAndroidPuaFromVendorSpecificSjis(unsigned short sjis) = 0;

  // Returns Vendor-specific Shift jis code corresponding to "pua". Returns -1
  // when ther's no mapping from "pua" to a specific sjis.
  virtual int GetVendorSpecificSjisFromAndroidPua(int pua) = 0;

  // Returns maximum Vendor-Specific PUA. This is the last valid value.
  virtual int GetMaximumVendorSpecificPua() = 0;

  // Returns minimum Vendor-Specific PUA.
  virtual int GetMinimumVendorSpecificPua() = 0;

  // Returns maximum Android PUA. This the last valid value.
  virtual int GetMaximumAndroidPua() = 0;

  // Returns minimum Android PUA.
  virtual int GetMinimumAndroidPua() = 0;

  // Returns Android PUA corresponding to Vendor-Specific Unicode "vsp". Returns
  // -1 when there's no mapping from "vsp" to a Android PUA.
  virtual int GetAndroidPuaFromVendorSpecificPua(int vsp) = 0;

  // Returns Vendor-specific PUA corresponding to "pua". Returns -1 when
  // there's no mapping from "pua" to a specific unicode.
  virtual int GetVendorSpecificPuaFromAndroidPua(int pua) = 0;

  // Returns non NULL string which defines the name of this factory.
  // e.g. "docomo", "goomoji"
  virtual const char *Name() const = 0;

  // Get a specific implementation of EmojiFactory. If there's no implementation
  // for "name", returns NULL.
  // The ownership of the instance remains to this class, so users must not
  // release it.
  static EmojiFactory *GetImplementation(const char *name);

  // Get an implementation of EmojiFactory. This assumes that, usually, there
  // should be only one possible EmojiFactory implementation. If there are more
  // than one implementations, most prefered one is returned.
  // The ownership of the instance remains to this class, so users must not
  // release it.
  static EmojiFactory *GetAvailableImplementation();
};

}  // namespace android

#endif // ANDROID_EMOJI_FACTORY_H
