/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.streamhtmlparser.util;

/**
 * Records (stores) characters supplied one at a time conditional on
 * whether recording is currently enabled.
 *
 * <p>When {@link #maybeRecord(char)} is called, it will add the
 * supplied character to the recording buffer but only if
 * recording is in progress. This is useful in our
 * {@link com.google.security.streamhtmlparser.HtmlParser}
 * as the caller logic to enable/disable recording is decoupled from the logic
 * of recording.
 *
 * <p>This is a specialized class - of no use to external code -
 * which aims to be 100% compatible with the corresponding logic
 * in the C-version of the HtmlParser, specifically in
 * <code>statemachine.c</code>. In particular:
 * <ul>
 *   <li>The {@code startRecording()} and {@code stopRecording()} methods
 *       may be called repeatedly without interleaving since the C version is
 *       not guaranteed to interleave them.
 *   <li>There is a size limit to the recording buffer as set in
 *       {@link #RECORDING_BUFFER_SIZE}. Once the size is
 *       reached, no further characters are recorded regardless of whether
 *       recording is currently enabled.
 * </ul>
 */
public class CharacterRecorder {

  /**
   * How many characters can be recorded before stopping to accept new
   * ones. Set to one less than in the C-version as we do not need
   * to reserve a character for the terminating null.
   */
  public static final int RECORDING_BUFFER_SIZE = 255;

  /**
   * This is where characters provided for recording are stored. Given
   * that the <code>CharacterRecorder</code> object is re-used, might as well
   * allocate the full size from the get-go.
   */
  private final StringBuilder sb;

  /** Holds whether we are currently recording characters or not. */
  private boolean recording;

  /**
   * Constructs an empty character recorder of fixed size currently
   * not recording. See {@link #RECORDING_BUFFER_SIZE} for the size.
   */
  public CharacterRecorder() {
    sb = new StringBuilder(RECORDING_BUFFER_SIZE);
    recording = false;
  }

  /**
   * Constructs a character recorder of fixed size that is a copy
   * of the one provided. In particular it has the same recording
   * setting and the same contents.
   *
   * @param aCharacterRecorder the {@code CharacterRecorder} to copy
   */
  public CharacterRecorder(CharacterRecorder aCharacterRecorder) {
    recording = aCharacterRecorder.recording;
    sb = new StringBuilder(RECORDING_BUFFER_SIZE);
    sb.append(aCharacterRecorder.getContent());
  }

  /**
   * Enables recording for incoming characters. The recording buffer is cleared
   * of content it may have contained.
   */
  public void startRecording() {
    // This is very fast, no memory (re-) allocation will take place.
    sb.setLength(0);
    recording = true;
  }

  /**
   * Disables recording further characters.
   */
  public void stopRecording() {
    recording = false;
  }

  /**
   * Records the {@code input} if recording is currently on and we
   * have space available in the buffer. If recording is not
   * currently on, this method will not perform any action.
   *
   * @param input the character to record
   */
  public void maybeRecord(char input) {
    if (recording && (sb.length() < RECORDING_BUFFER_SIZE)) {
      sb.append(input);
    }
  }

  /**
   * Empties the underlying storage but does not change the recording
   * state [i.e whether we are recording or not incoming characters].
   */
  public void clear() {
    sb.setLength(0);
  }

  /**
   * Empties the underlying storage and resets the recording indicator
   * to indicate we are not recording currently.
   */
  public void reset() {
    clear();
    recording = false;
  }

  /**
   * Returns the characters recorded in a {@code String} form. This
   * method has no side-effects, the characters remain stored as is.
   *
   * @return the contents in a {@code String} form
   */
  public String getContent() {
    return sb.toString();
  }

  /**
   * Returns whether or not we are currently recording incoming characters.
   *
   * @return {@code true} if we are recording, {@code false} otherwise
   */
  public boolean isRecording() {
    return recording;
  }

  /**
   * Returns the full state of the object in a human readable form. The
   * format of the returned {@code String} is not specified and is
   * subject to change.
   *
   * @return the full state of this object
   */
  @Override
  public String toString() {
    return String.format("In recording: %s; Value: %s", isRecording(),
                         sb.toString());
  }
}
