/* 
 * Copyright (C) 2010 The Android Open Source Project
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

#include <jni.h>
#include <stdio.h>
#include <cutils/log.h>
#include <linux/xattr.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/xattr.h>
#include <sys/capability.h>
#include <grp.h>
#include <pwd.h>
#include <string.h>

static jfieldID gFileStatusDevFieldID;
static jfieldID gFileStatusInoFieldID;
static jfieldID gFileStatusModeFieldID;
static jfieldID gFileStatusNlinkFieldID;
static jfieldID gFileStatusUidFieldID;
static jfieldID gFileStatusGidFieldID;
static jfieldID gFileStatusSizeFieldID;
static jfieldID gFileStatusBlksizeFieldID;
static jfieldID gFileStatusBlocksFieldID;
static jfieldID gFileStatusAtimeFieldID;
static jfieldID gFileStatusMtimeFieldID;
static jfieldID gFileStatusCtimeFieldID;

/*
 * Native methods used by
 * cts/tests/tests/permission/src/android/permission/cts/FileUtils.java
 *
 * Copied from hidden API: frameworks/base/core/jni/android_os_FileUtils.cpp
 */

jboolean android_permission_cts_FileUtils_getFileStatus(JNIEnv* env, jobject thiz,
        jstring path, jobject fileStatus, jboolean statLinks)
{
    const char* pathStr = env->GetStringUTFChars(path, NULL);
    jboolean ret = false;
    struct stat s;

    int res = statLinks == true ? lstat(pathStr, &s) : stat(pathStr, &s);

    if (res == 0) {
        ret = true;
        if (fileStatus != NULL) {
            env->SetIntField(fileStatus, gFileStatusDevFieldID, s.st_dev);
            env->SetIntField(fileStatus, gFileStatusInoFieldID, s.st_ino);
            env->SetIntField(fileStatus, gFileStatusModeFieldID, s.st_mode);
            env->SetIntField(fileStatus, gFileStatusNlinkFieldID, s.st_nlink);
            env->SetIntField(fileStatus, gFileStatusUidFieldID, s.st_uid);
            env->SetIntField(fileStatus, gFileStatusGidFieldID, s.st_gid);
            env->SetLongField(fileStatus, gFileStatusSizeFieldID, s.st_size);
            env->SetIntField(fileStatus, gFileStatusBlksizeFieldID, s.st_blksize);
            env->SetLongField(fileStatus, gFileStatusBlocksFieldID, s.st_blocks);
            env->SetLongField(fileStatus, gFileStatusAtimeFieldID, s.st_atime);
            env->SetLongField(fileStatus, gFileStatusMtimeFieldID, s.st_mtime);
            env->SetLongField(fileStatus, gFileStatusCtimeFieldID, s.st_ctime);
        }
    }

    env->ReleaseStringUTFChars(path, pathStr);

    return ret;
}

jstring android_permission_cts_FileUtils_getUserName(JNIEnv* env, jobject thiz,
        jint uid)
{
    struct passwd *pwd = getpwuid(uid);
    return env->NewStringUTF(pwd->pw_name);
}

jstring android_permission_cts_FileUtils_getGroupName(JNIEnv* env, jobject thiz,
        jint gid)
{
    struct group *grp = getgrgid(gid);
    return env->NewStringUTF(grp->gr_name);
}

static jboolean isPermittedCapBitSet(JNIEnv* env, jstring path, size_t capId)
{
    const char* pathStr = env->GetStringUTFChars(path, NULL);
    jboolean ret = false;

    struct vfs_cap_data capData;
    memset(&capData, 0, sizeof(capData));

    ssize_t result = getxattr(pathStr, XATTR_NAME_CAPS, &capData,
                              sizeof(capData));
    if (result > 0) {
      ret = (capData.data[CAP_TO_INDEX(capId)].permitted &
             CAP_TO_MASK(capId)) != 0;
      ALOGD("isPermittedCapBitSet(): getxattr(\"%s\") call succeeded, "
            "cap bit %u %s",
            pathStr, capId, ret ? "set" : "unset");
    } else {
      ALOGD("isPermittedCapBitSet(): getxattr(\"%s\") call failed: "
            "return %d (error: %s (%d))\n",
            pathStr, result, strerror(errno), errno);
    }

    env->ReleaseStringUTFChars(path, pathStr);
    return ret;
}

jboolean android_permission_cts_FileUtils_hasSetUidCapability(JNIEnv* env,
        jobject clazz, jstring path)
{
    return isPermittedCapBitSet(env, path, CAP_SETUID);
}

jboolean android_permission_cts_FileUtils_hasSetGidCapability(JNIEnv* env,
        jobject clazz, jstring path)
{
    return isPermittedCapBitSet(env, path, CAP_SETGID);
}

static JNINativeMethod gMethods[] = {
    {  "getFileStatus", "(Ljava/lang/String;Landroid/permission/cts/FileUtils$FileStatus;Z)Z",
            (void *) android_permission_cts_FileUtils_getFileStatus  },
    {  "getUserName", "(I)Ljava/lang/String;",
            (void *) android_permission_cts_FileUtils_getUserName  },
    {  "getGroupName", "(I)Ljava/lang/String;",
            (void *) android_permission_cts_FileUtils_getGroupName  },
    {  "hasSetUidCapability", "(Ljava/lang/String;)Z",
            (void *) android_permission_cts_FileUtils_hasSetUidCapability   },
    {  "hasSetGidCapability", "(Ljava/lang/String;)Z",
            (void *) android_permission_cts_FileUtils_hasSetGidCapability   },
};

int register_android_permission_cts_FileUtils(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/permission/cts/FileUtils");

    jclass fileStatusClass = env->FindClass("android/permission/cts/FileUtils$FileStatus");
    gFileStatusDevFieldID = env->GetFieldID(fileStatusClass, "dev", "I");
    gFileStatusInoFieldID = env->GetFieldID(fileStatusClass, "ino", "I");
    gFileStatusModeFieldID = env->GetFieldID(fileStatusClass, "mode", "I");
    gFileStatusNlinkFieldID = env->GetFieldID(fileStatusClass, "nlink", "I");
    gFileStatusUidFieldID = env->GetFieldID(fileStatusClass, "uid", "I");
    gFileStatusGidFieldID = env->GetFieldID(fileStatusClass, "gid", "I");
    gFileStatusSizeFieldID = env->GetFieldID(fileStatusClass, "size", "J");
    gFileStatusBlksizeFieldID = env->GetFieldID(fileStatusClass, "blksize", "I");
    gFileStatusBlocksFieldID = env->GetFieldID(fileStatusClass, "blocks", "J");
    gFileStatusAtimeFieldID = env->GetFieldID(fileStatusClass, "atime", "J");
    gFileStatusMtimeFieldID = env->GetFieldID(fileStatusClass, "mtime", "J");
    gFileStatusCtimeFieldID = env->GetFieldID(fileStatusClass, "ctime", "J");

    return env->RegisterNatives(clazz, gMethods, 
            sizeof(gMethods) / sizeof(JNINativeMethod)); 
}
