/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.security.cts;

class OEMCertificateWhitelist {

  /**
   * If you fail CTS as a result of adding a root CA that is not part
   * of the Android root CA store, please see the following.
   *
   * First, this test exists because adding untrustworthy root CAs
   * to a device has a very significant security impact. In the worst
   * case, adding a rogue CA to this list can permanently compromise
   * the confidentiality and integrity of your users' network traffic.
   * Because of this risk, adding new certificates should be done
   * sparingly and as a last resort- never as a first response or
   * short term fix. Before attempting to modify this test, please
   * consider whether adding a new certificate authority is in your
   * users' best interests.
   *
   * Second, because the addition of a new root CA by an OEM can have
   * such dire consequences for so many people it is imperative that
   * it be done transparently and in the open. Any request to modify
   * this list must have a corresponding change in AOSP authored by
   * the OEM in question and including:
   *
   *     - the certificate in question.
   *
   *     - information about who created and maintains
   *       both the certificate and the corresponding keypair.
   *
   *     - information about what the certificate is to be used
   *       for and why the certificate is appropriate for inclusion.
   *
   *     - a statement from the OEM indicating that they have
   *       sufficient confidence in the security of the key, the
   *       security practices of the issuer, and the validity
   *       of the intended use that they believe adding the
   *       certificate is not detrimental to the security of the
   *       user.
   *
   * Finally, please note that this is not the usual process for
   * adding root CAs to Android. If you have a certificate that you
   * believe should be present on all Android devices, please file a
   * public bug at https://code.google.com/p/android/issues/entry or
   * http://b.android.com to seek resolution.
   *
   * For questions, comments, and code reviews please contact
   * security@android.com.
   */
  static final String[] OEM_CERTIFICATE_WHITELIST = {};

}
