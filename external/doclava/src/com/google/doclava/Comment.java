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

package com.google.doclava;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

public class Comment {
  static final Pattern FIRST_SENTENCE =
      Pattern.compile("((.*?)\\.)[ \t\r\n\\<](.*)", Pattern.DOTALL);

  private static final String[] KNOWN_TAGS = new String[] {
          "@author",
          "@since",
          "@version",
          "@deprecated",
          "@undeprecate",
          "@docRoot",
          "@sdkCurrent",
          "@inheritDoc",
          "@more",
          "@samplecode",
          "@sample",
          "@include",
          "@serial",
      };

  public Comment(String text, ContainerInfo base, SourcePositionInfo sp) {
    mText = text;
    mBase = base;
    // sp now points to the end of the text, not the beginning!
    mPosition = SourcePositionInfo.findBeginning(sp, text);
  }

  private void parseCommentTags(String text) {
      int i = 0;
      int length = text.length();
      while (i < length  && isWhitespaceChar(text.charAt(i++))) {}

      if (i <=  0) {
          return;
      }

      text = text.substring(i-1);
      length = text.length();

      if ("".equals(text)) {
          return;
      }

      int start = 0;
      int end = findStartOfBlock(text, start);


      // possible scenarios
      //    main and block(s)
      //    main only (end == -1)
      //    block(s) only (end == 0)

      switch (end) {
          case -1: // main only
              parseMainDescription(text, start, length);
              return;
          case 0: // block(s) only
              break;
          default: // main and block

              // find end of main because end is really the beginning of @
              parseMainDescription(text, start, findEndOfMainOrBlock(text, start, end));
              break;
      }

      // parse blocks
      for (start = end; start < length; start = end) {
          end = findStartOfBlock(text, start+1);

          if (end == -1) {
              parseBlock(text, start, length);
              break;
          } else {
              parseBlock(text, start, findEndOfMainOrBlock(text, start, end));
          }
      }

      // for each block
      //    make block parts
      //        end is either next @ at beginning of line or end of text
  }

  private int findEndOfMainOrBlock(String text, int start, int end) {
      for (int i = end-1; i >= start; i--) {
          if (!isWhitespaceChar(text.charAt(i))) {
              end = i+1;
              break;
          }
      }
      return end;
  }

  private void parseMainDescription(String mainDescription, int start, int end) {
      if (mainDescription == null) {
          return;
      }

      SourcePositionInfo pos = SourcePositionInfo.add(mPosition, mText, 0);
      while (start < end) {
          int startOfInlineTag = findStartIndexOfInlineTag(mainDescription, start, end);

          // if there are no more tags
          if (startOfInlineTag == -1) {
              tag(null, mainDescription.substring(start, end), true, pos);
              return;
          }

          //int endOfInlineTag = mainDescription.indexOf('}', startOfInlineTag);
          int endOfInlineTag = findEndIndexOfInlineTag(mainDescription, startOfInlineTag, end);

          // if there was only beginning tag
          if (endOfInlineTag == -1) {
              // parse all of main as one tag
              tag(null, mainDescription.substring(start, end), true, pos);
              return;
          }

          endOfInlineTag++; // add one to make it a proper ending index

          // do first part without an inline tag - ie, just plaintext
          tag(null, mainDescription.substring(start, startOfInlineTag), true, pos);

          // parse the rest of this section, the inline tag
          parseInlineTag(mainDescription, startOfInlineTag, endOfInlineTag, pos);

          // keep going
          start = endOfInlineTag;
      }
  }

  private int findStartIndexOfInlineTag(String text, int fromIndex, int toIndex) {
      for (int i = fromIndex; i < (toIndex-3); i++) {
          if (text.charAt(i) == '{' && text.charAt(i+1) == '@' && !isWhitespaceChar(text.charAt(i+2))) {
              return i;
          }
      }

      return -1;
  }

  private int findEndIndexOfInlineTag(String text, int fromIndex, int toIndex) {
      for (int i = fromIndex; i < toIndex; i++) {
          if (text.charAt(i) == '}') {
              return i;
          }
      }

      return -1;
  }

  private void parseInlineTag(String text, int start, int end, SourcePositionInfo pos) {
      int index = start+1;
      //int len = text.length();
      char c = text.charAt(index);
      // find the end of the tag name "@something"
      // need to do something special if we have '}'
      while (index < end && !isWhitespaceChar(c)) {

          // if this tag has no value, just return with tag name only
          if (c == '}') {
              // TODO - should value be "" or null?
              tag(text.substring(start+1, end), null, true, pos);
              return;
          }
          c = text.charAt(index++);
      }

      // don't parse things that don't have at least one extra character after @
      // probably should be plus 3
      // TODO - remove this - think it's fixed by change in parseMainDescription
      if (index == start+3) {
          return;
      }

      int endOfFirstPart = index-1;

      // get to beginning of tag value
      while (index < end && isWhitespaceChar(text.charAt(index++))) {}
      int startOfSecondPart = index-1;

      // +1 to get rid of opening brace and -1 to get rid of closing brace
      // maybe i wanna make this more elegant
      String tagName = text.substring(start+1, endOfFirstPart);
      String tagText = text.substring(startOfSecondPart, end-1);
      tag(tagName, tagText, true, pos);
  }


  /**
   * Finds the index of the start of a new block comment or -1 if there are
   * no more starts.
   * @param text The String to search
   * @param start the index of the String to start searching
   * @return The index of the start of a new block comment or -1 if there are
   * no more starts.
   */
  private int findStartOfBlock(String text, int start) {
      // how to detect we're at a new @
      //       if the chars to the left of it are \r or \n, we're at one
      //       if the chars to the left of it are ' ' or \t, keep looking
      //       otherwise, we're in the middle of a block, keep looking
      int index = text.indexOf('@', start);

      // no @ in text or index at first position
      if (index == -1 ||
              (index == 0 && text.length() > 1 && !isWhitespaceChar(text.charAt(index+1)))) {
          return index;
      }

      index = getPossibleStartOfBlock(text, index);

      int i = index-1; // start at the character immediately to the left of @
      char c;
      while (i >= 0) {
          c = text.charAt(i--);

          // found a new block comment because we're at the beginning of a line
          if (c == '\r' || c == '\n') {
              return index;
          }

          // there is a non whitespace character to the left of the @
          // before finding a new line, keep searching
          if (c != ' ' && c != '\t') {
              index = getPossibleStartOfBlock(text, index+1);
              i = index-1;
          }

          // some whitespace character, so keep looking, we might be at a new block comment
      }

      return -1;
  }

  private int getPossibleStartOfBlock(String text, int index) {
      while (isWhitespaceChar(text.charAt(index+1)) || !isWhitespaceChar(text.charAt(index-1))) {
          index = text.indexOf('@', index+1);

          if (index == -1 || index == text.length()-1) {
              return -1;
          }
      }

      return index;
  }

  private void parseBlock(String text, int startOfBlock, int endOfBlock) {
      SourcePositionInfo pos = SourcePositionInfo.add(mPosition, mText, startOfBlock);
      int index = startOfBlock;

      for (char c = text.charAt(index);
              index < endOfBlock && !isWhitespaceChar(c); c = text.charAt(index++)) {}

      //
      if (index == startOfBlock+1) {
          return;
      }

      int endOfFirstPart = index-1;
      if (index == endOfBlock) {
          // TODO - should value be null or ""
          tag(text.substring(startOfBlock,
                  findEndOfMainOrBlock(text, startOfBlock, index)), "", false, pos);
          return;
      }


      // get to beginning of tag value
      while (index < endOfBlock && isWhitespaceChar(text.charAt(index++))) {}
      int startOfSecondPart = index-1;

      tag(text.substring(startOfBlock, endOfFirstPart),
              text.substring(startOfSecondPart, endOfBlock), false, pos);
  }

  private boolean isWhitespaceChar(char c) {
      return c == ' ' || c == '\r' || c == '\t' || c == '\n';
  }

  private void tag(String name, String text, boolean isInline, SourcePositionInfo pos) {
    /*
     * String s = isInline ? "inline" : "outofline"; System.out.println("---> " + s + " name=[" +
     * name + "] text=[" + text + "]");
     */
    if (name == null) {
      mInlineTagsList.add(new TextTagInfo("Text", "Text", text, pos));
    } else if (name.equals("@param")) {
      mParamTagsList.add(new ParamTagInfo("@param", "@param", text, mBase, pos));
    } else if (name.equals("@see")) {
      mSeeTagsList.add(new SeeTagInfo("@see", "@see", text, mBase, pos));
    } else if (name.equals("@link") || name.equals("@linkplain")) {
      mInlineTagsList.add(new SeeTagInfo(name, "@see", text, mBase, pos));
    } else if (name.equals("@value")) {
      mInlineTagsList.add(new SeeTagInfo(name, "@value", text, mBase, pos));
    } else if (name.equals("@throws") || name.equals("@exception")) {
      mThrowsTagsList.add(new ThrowsTagInfo("@throws", "@throws", text, mBase, pos));
    } else if (name.equals("@return")) {
      mReturnTagsList.add(new ParsedTagInfo("@return", "@return", text, mBase, pos));
    } else if (name.equals("@deprecated")) {
      if (text.length() == 0) {
        Errors.error(Errors.MISSING_COMMENT, pos, "@deprecated tag with no explanatory comment");
        text = "No replacement.";
      }
      mDeprecatedTagsList.add(new ParsedTagInfo("@deprecated", "@deprecated", text, mBase, pos));
    } else if (name.equals("@literal")) {
      mInlineTagsList.add(new LiteralTagInfo(text, pos));
    } else if (name.equals("@code")) {
      mInlineTagsList.add(new CodeTagInfo(text, pos));
    } else if (name.equals("@hide") || name.equals("@pending") || name.equals("@doconly")) {
      // nothing
    } else if (name.equals("@attr")) {
      AttrTagInfo tag = new AttrTagInfo("@attr", "@attr", text, mBase, pos);
      mAttrTagsList.add(tag);
      Comment c = tag.description();
      if (c != null) {
        for (TagInfo t : c.tags()) {
          mInlineTagsList.add(t);
        }
      }
    } else if (name.equals("@undeprecate")) {
      mUndeprecateTagsList.add(new TextTagInfo("@undeprecate", "@undeprecate", text, pos));
    } else if (name.equals("@include") || name.equals("@sample")) {
      mInlineTagsList.add(new SampleTagInfo(name, "@include", text, mBase, pos));
    } else {
      boolean known = false;
      for (String s : KNOWN_TAGS) {
        if (s.equals(name)) {
          known = true;
          break;
        }
      }
      if (!known) {
        known = Doclava.knownTags.contains(name);
      }
      if (!known) {
        Errors.error(Errors.UNKNOWN_TAG, pos == null ? null : new SourcePositionInfo(pos),
            "Unknown tag: " + name);
      }
      TagInfo t = new TextTagInfo(name, name, text, pos);
      if (isInline) {
        mInlineTagsList.add(t);
      } else {
        mTagsList.add(t);
      }
    }
  }

  private void parseBriefTags() {
    int N = mInlineTagsList.size();

    // look for "@more" tag, which means that we might go past the first sentence.
    int more = -1;
    for (int i = 0; i < N; i++) {
      if (mInlineTagsList.get(i).name().equals("@more")) {
        more = i;
      }
    }
    if (more >= 0) {
      for (int i = 0; i < more; i++) {
        mBriefTagsList.add(mInlineTagsList.get(i));
      }
    } else {
      for (int i = 0; i < N; i++) {
        TagInfo t = mInlineTagsList.get(i);
        if (t.name().equals("Text")) {
          Matcher m = FIRST_SENTENCE.matcher(t.text());
          if (m.matches()) {
            String text = m.group(1);
            TagInfo firstSentenceTag = new TagInfo(t.name(), t.kind(), text, t.position());
            mBriefTagsList.add(firstSentenceTag);
            break;
          }
        }
        mBriefTagsList.add(t);

      }
    }
  }

  public TagInfo[] tags() {
    init();
    return mInlineTags;
  }

  public TagInfo[] tags(String name) {
    init();
    ArrayList<TagInfo> results = new ArrayList<TagInfo>();
    int N = mInlineTagsList.size();
    for (int i = 0; i < N; i++) {
      TagInfo t = mInlineTagsList.get(i);
      if (t.name().equals(name)) {
        results.add(t);
      }
    }
    return results.toArray(new TagInfo[results.size()]);
  }

  public ParamTagInfo[] paramTags() {
    init();
    return mParamTags;
  }

  public SeeTagInfo[] seeTags() {
    init();
    return mSeeTags;
  }

  public ThrowsTagInfo[] throwsTags() {
    init();
    return mThrowsTags;
  }

  public TagInfo[] returnTags() {
    init();
    return mReturnTags;
  }

  public TagInfo[] deprecatedTags() {
    init();
    return mDeprecatedTags;
  }

  public TagInfo[] undeprecateTags() {
    init();
    return mUndeprecateTags;
  }

  public AttrTagInfo[] attrTags() {
    init();
    return mAttrTags;
  }

  public TagInfo[] briefTags() {
    init();
    return mBriefTags;
  }

  public boolean isHidden() {
    if (mHidden != -1) {
      return mHidden != 0;
    } else {
      if (Doclava.checkLevel(Doclava.SHOW_HIDDEN)) {
        mHidden = 0;
        return false;
      }
      boolean b = mText.indexOf("@hide") >= 0 || mText.indexOf("@pending") >= 0;
      mHidden = b ? 1 : 0;
      return b;
    }
  }

  public boolean isDocOnly() {
    if (mDocOnly != -1) {
      return mDocOnly != 0;
    } else {
      boolean b = (mText != null) && (mText.indexOf("@doconly") >= 0);
      mDocOnly = b ? 1 : 0;
      return b;
    }
  }
  
  public boolean isDeprecated() {
    if (mDeprecated != -1) {
      return mDeprecated != 0;
    } else {
      boolean b = (mText != null) && (mText.indexOf("@deprecated") >= 0);
      mDeprecated = b ? 1 : 0;
      return b;
    }
  }

  private void init() {
    if (!mInitialized) {
      initImpl();
    }
  }

  private void initImpl() {
    isHidden();
    isDocOnly();
    isDeprecated();

    // Don't bother parsing text if we aren't generating documentation.
    if (Doclava.parseComments()) {
        parseCommentTags(mText);
        parseBriefTags();
    } else {
      // Forces methods to be recognized by findOverriddenMethods in MethodInfo.
      mInlineTagsList.add(new TextTagInfo("Text", "Text", mText,
          SourcePositionInfo.add(mPosition, mText, 0)));
    }

    mText = null;
    mInitialized = true;

    mInlineTags = mInlineTagsList.toArray(new TagInfo[mInlineTagsList.size()]);
    mParamTags = mParamTagsList.toArray(new ParamTagInfo[mParamTagsList.size()]);
    mSeeTags = mSeeTagsList.toArray(new SeeTagInfo[mSeeTagsList.size()]);
    mThrowsTags = mThrowsTagsList.toArray(new ThrowsTagInfo[mThrowsTagsList.size()]);
    mReturnTags =
        ParsedTagInfo.joinTags(mReturnTagsList.toArray(new ParsedTagInfo[mReturnTagsList.size()]));
    mDeprecatedTags =
        ParsedTagInfo.joinTags(mDeprecatedTagsList.toArray(new ParsedTagInfo[mDeprecatedTagsList
            .size()]));
    mUndeprecateTags = mUndeprecateTagsList.toArray(new TagInfo[mUndeprecateTagsList.size()]);
    mAttrTags = mAttrTagsList.toArray(new AttrTagInfo[mAttrTagsList.size()]);
    mBriefTags = mBriefTagsList.toArray(new TagInfo[mBriefTagsList.size()]);

    mParamTagsList = null;
    mSeeTagsList = null;
    mThrowsTagsList = null;
    mReturnTagsList = null;
    mDeprecatedTagsList = null;
    mUndeprecateTagsList = null;
    mAttrTagsList = null;
    mBriefTagsList = null;
  }

  boolean mInitialized;
  int mHidden = -1;
  int mDocOnly = -1;
  int mDeprecated = -1;
  String mText;
  ContainerInfo mBase;
  SourcePositionInfo mPosition;
  int mLine = 1;

  TagInfo[] mInlineTags;
  TagInfo[] mTags;
  ParamTagInfo[] mParamTags;
  SeeTagInfo[] mSeeTags;
  ThrowsTagInfo[] mThrowsTags;
  TagInfo[] mBriefTags;
  TagInfo[] mReturnTags;
  TagInfo[] mDeprecatedTags;
  TagInfo[] mUndeprecateTags;
  AttrTagInfo[] mAttrTags;

  ArrayList<TagInfo> mInlineTagsList = new ArrayList<TagInfo>();
  ArrayList<TagInfo> mTagsList = new ArrayList<TagInfo>();
  ArrayList<ParamTagInfo> mParamTagsList = new ArrayList<ParamTagInfo>();
  ArrayList<SeeTagInfo> mSeeTagsList = new ArrayList<SeeTagInfo>();
  ArrayList<ThrowsTagInfo> mThrowsTagsList = new ArrayList<ThrowsTagInfo>();
  ArrayList<TagInfo> mBriefTagsList = new ArrayList<TagInfo>();
  ArrayList<ParsedTagInfo> mReturnTagsList = new ArrayList<ParsedTagInfo>();
  ArrayList<ParsedTagInfo> mDeprecatedTagsList = new ArrayList<ParsedTagInfo>();
  ArrayList<TagInfo> mUndeprecateTagsList = new ArrayList<TagInfo>();
  ArrayList<AttrTagInfo> mAttrTagsList = new ArrayList<AttrTagInfo>();


}
