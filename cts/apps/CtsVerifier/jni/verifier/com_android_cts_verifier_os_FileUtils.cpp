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
#include <sys/types.h>
#include <sys/stat.h>
#include <grp.h>
#include <pwd.h>
#include <unistd.h>

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
static jfieldID gFileStatusExecutableID;

/* Copied from hidden API: frameworks/base/core/jni/android_os_FileUtils.cpp */
jboolean com_android_cts_verifier_os_FileUtils_getFileStatus(JNIEnv* env, jobject thiz,
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
        if (access(pathStr, X_OK) == 0) {
            env->SetBooleanField(fileStatus, gFileStatusExecutableID, JNI_TRUE);
        } else {
            env->SetBooleanField(fileStatus, gFileStatusExecutableID, JNI_FALSE);
        }
    }

    env->ReleaseStringUTFChars(path, pathStr);

    return ret;
}

jstring com_android_cts_verifier_os_FileUtils_getUserName(JNIEnv* env, jobject thiz,
        jint uid)
{
    struct passwd *pwd = getpwuid(uid);
    return env->NewStringUTF(pwd->pw_name);
}

jstring com_android_cts_verifier_os_FileUtils_getGroupName(JNIEnv* env, jobject thiz,
        jint gid)
{
    struct group *grp = getgrgid(gid);
    return env->NewStringUTF(grp->gr_name);
}

static JNINativeMethod gMethods[] = {
    {  "getFileStatus", "(Ljava/lang/String;Lcom/android/cts/verifier/os/FileUtils$FileStatus;Z)Z",
            (void *) com_android_cts_verifier_os_FileUtils_getFileStatus  },
    {  "getUserName", "(I)Ljava/lang/String;",
            (void *) com_android_cts_verifier_os_FileUtils_getUserName  },
    {  "getGroupName", "(I)Ljava/lang/String;",
            (void *) com_android_cts_verifier_os_FileUtils_getGroupName  },
};

int register_com_android_cts_verifier_os_FileUtils(JNIEnv* env)
{
    jclass clazz = env->FindClass("com/android/cts/verifier/os/FileUtils");

    jclass fileStatusClass = env->FindClass("com/android/cts/verifier/os/FileUtils$FileStatus");
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
    gFileStatusExecutableID = env->GetFieldID(fileStatusClass, "executable", "Z");

    return env->RegisterNatives(clazz, gMethods, 
            sizeof(gMethods) / sizeof(JNINativeMethod)); 
}
