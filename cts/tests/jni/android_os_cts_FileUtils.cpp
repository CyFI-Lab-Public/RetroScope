/* 
 * Copyright (C) 2011 The Android Open Source Project
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

#include <grp.h>
#include <jni.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/errno.h>
#include <sys/types.h>
#include <sys/stat.h>

static jclass gFileStatusClass;
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
 * cts/tests/src/android/os/cts/FileUtils.java
 *
 * Copied from hidden API: frameworks/base/core/jni/android_os_FileUtils.cpp
 */

jboolean android_os_cts_FileUtils_getFileStatus(JNIEnv* env, jobject thiz,
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

jstring android_os_cts_FileUtils_getUserName(JNIEnv* env, jobject thiz,
        jint uid)
{
    struct passwd *pwd = getpwuid(uid);
    return env->NewStringUTF(pwd->pw_name);
}

jstring android_os_cts_FileUtils_getGroupName(JNIEnv* env, jobject thiz,
        jint gid)
{
    struct group *grp = getgrgid(gid);
    return env->NewStringUTF(grp->gr_name);
}

jint android_os_cts_FileUtils_setPermissions(JNIEnv* env, jobject clazz,
        jstring file, jint mode)
{
    const char *fileStr = env->GetStringUTFChars(file, NULL);
    if (fileStr == NULL) {
        return -1;
    }

    if (strlen(fileStr) <= 0) {
        env->ReleaseStringUTFChars(file, fileStr);
        return ENOENT;
    } 

    jint returnValue = chmod(fileStr, mode) == 0 ? 0 : errno;
    env->ReleaseStringUTFChars(file, fileStr);
    return returnValue;
}

static JNINativeMethod gMethods[] = {
    {  "getFileStatus", "(Ljava/lang/String;Landroid/os/cts/FileUtils$FileStatus;Z)Z",
            (void *) android_os_cts_FileUtils_getFileStatus  },
    {  "getUserName", "(I)Ljava/lang/String;",
            (void *) android_os_cts_FileUtils_getUserName  },
    {  "getGroupName", "(I)Ljava/lang/String;",
            (void *) android_os_cts_FileUtils_getGroupName  },
    {  "setPermissions", "(Ljava/lang/String;I)I",
            (void *) android_os_cts_FileUtils_setPermissions },
};

int register_android_os_cts_FileUtils(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/os/cts/FileUtils");

    gFileStatusClass = env->FindClass("android/os/cts/FileUtils$FileStatus");
    gFileStatusDevFieldID = env->GetFieldID(gFileStatusClass, "dev", "I");
    gFileStatusInoFieldID = env->GetFieldID(gFileStatusClass, "ino", "I");
    gFileStatusModeFieldID = env->GetFieldID(gFileStatusClass, "mode", "I");
    gFileStatusNlinkFieldID = env->GetFieldID(gFileStatusClass, "nlink", "I");
    gFileStatusUidFieldID = env->GetFieldID(gFileStatusClass, "uid", "I");
    gFileStatusGidFieldID = env->GetFieldID(gFileStatusClass, "gid", "I");
    gFileStatusSizeFieldID = env->GetFieldID(gFileStatusClass, "size", "J");
    gFileStatusBlksizeFieldID = env->GetFieldID(gFileStatusClass, "blksize", "I");
    gFileStatusBlocksFieldID = env->GetFieldID(gFileStatusClass, "blocks", "J");
    gFileStatusAtimeFieldID = env->GetFieldID(gFileStatusClass, "atime", "J");
    gFileStatusMtimeFieldID = env->GetFieldID(gFileStatusClass, "mtime", "J");
    gFileStatusCtimeFieldID = env->GetFieldID(gFileStatusClass, "ctime", "J");

    return env->RegisterNatives(clazz, gMethods, 
            sizeof(gMethods) / sizeof(JNINativeMethod)); 
}
