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

import java.util.ArrayList;

public abstract class MemberInfo extends DocInfo implements Comparable, Scoped {
  public MemberInfo(String rawCommentText, String name, String signature,
      ClassInfo containingClass, ClassInfo realContainingClass, boolean isPublic,
      boolean isProtected, boolean isPackagePrivate, boolean isPrivate, boolean isFinal,
      boolean isStatic, boolean isSynthetic, String kind, SourcePositionInfo position,
      ArrayList<AnnotationInstanceInfo> annotations) {
    super(rawCommentText, position);
    mName = name;
    mSignature = signature;
    mContainingClass = containingClass;
    mRealContainingClass = realContainingClass;
    mIsPublic = isPublic;
    mIsProtected = isProtected;
    mIsPackagePrivate = isPackagePrivate;
    mIsPrivate = isPrivate;
    mIsFinal = isFinal;
    mIsStatic = isStatic;
    mIsSynthetic = isSynthetic;
    mKind = kind;
    mAnnotations = annotations;
  }

  public abstract boolean isExecutable();

  @Override
  public boolean isHidden() {
    if (mAnnotations != null) {
      for (AnnotationInstanceInfo info : mAnnotations) {
        if (Doclava.showAnnotations.contains(info.type().qualifiedName())) {
          return false;
        }
      }
    }
    return super.isHidden();
  }

  public String anchor() {
    if (mSignature != null) {
      return mName + mSignature;
    } else {
      return mName;
    }
  }

  public String htmlPage() {
    return mContainingClass.htmlPage() + "#" + anchor();
  }

  public int compareTo(Object that) {
    return this.htmlPage().compareTo(((MemberInfo) that).htmlPage());
  }

  public String name() {
    return mName;
  }

  public String signature() {
    return mSignature;
  }

  public ClassInfo realContainingClass() {
    return mRealContainingClass;
  }

  public ClassInfo containingClass() {
    return mContainingClass;
  }

  public boolean isPublic() {
    return mIsPublic;
  }

  public boolean isProtected() {
    return mIsProtected;
  }

  public boolean isPackagePrivate() {
    return mIsPackagePrivate;
  }

  public boolean isPrivate() {
    return mIsPrivate;
  }
  
  public String scope() {
    if (isPublic()) {
      return "public";
    } else if (isProtected()) {
      return "protected";
    } else if (isPackagePrivate()) {
      return "";
    } else if (isPrivate()) {
      return "private";
    } else {
      throw new RuntimeException("invalid scope for object " + this);
    }
  }

  public boolean isStatic() {
    return mIsStatic;
  }

  public boolean isFinal() {
    return mIsFinal;
  }

  public boolean isSynthetic() {
    return mIsSynthetic;
  }

  @Override
  public ContainerInfo parent() {
    return mContainingClass;
  }

  public boolean checkLevel() {
    return Doclava.checkLevel(mIsPublic, mIsProtected, mIsPackagePrivate, mIsPrivate, isHidden());
  }

  public String kind() {
    return mKind;
  }

  public ArrayList<AnnotationInstanceInfo> annotations() {
    return mAnnotations;
  }

  ClassInfo mContainingClass;
  ClassInfo mRealContainingClass;
  String mName;
  String mSignature;
  boolean mIsPublic;
  boolean mIsProtected;
  boolean mIsPackagePrivate;
  boolean mIsPrivate;
  boolean mIsFinal;
  boolean mIsStatic;
  boolean mIsSynthetic;
  String mKind;
  private ArrayList<AnnotationInstanceInfo> mAnnotations;

}
