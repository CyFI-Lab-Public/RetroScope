/*

Copyright (c) 2008, The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.
 * Neither the name of Google, Inc. nor the names of its contributors
   may be used to endorse or promote products derived from this
   software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

*/

#include <nativehelper/JNIHelp.h>
#include <nativehelper/jni.h>

#include <assert.h>
#include <ctype.h>
#include <dlfcn.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <utils/Log.h>

#include "jhead.h"

#ifndef NELEM
#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif

// Define the line below to turn on poor man's debugging output
#undef SUPERDEBUG

// Various tests
#undef REALLOCTEST
#undef OUTOFMEMORYTEST1

static void addExifAttibute(JNIEnv *env, jmethodID putMethod, jobject hashMap, char* key, char* value) {
    jstring jkey = (*env)->NewStringUTF(env, key);
    jstring jvalue = (*env)->NewStringUTF(env, value);

    jobject jobject_of_entryset = (*env)->CallObjectMethod(env, hashMap, putMethod, jkey, jvalue);

    (*env)->ReleaseStringUTFChars(env, jkey, key);
    (*env)->ReleaseStringUTFChars(env, jvalue, value);
}

extern void ResetJpgfile();

static int loadExifInfo(const char* FileName, int readJPG) {
#ifdef SUPERDEBUG
    ALOGE("loadExifInfo");
#endif
    int Modified = FALSE;
    ReadMode_t ReadMode = READ_METADATA;
    if (readJPG) {
        // Must add READ_IMAGE else we can't write the JPG back out.
        ReadMode |= READ_IMAGE;
    }

#ifdef SUPERDEBUG
    ALOGE("ResetJpgfile");
#endif
    ResetJpgfile();

    // Start with an empty image information structure.
    memset(&ImageInfo, 0, sizeof(ImageInfo));
    ImageInfo.FlashUsed = -1;
    ImageInfo.MeteringMode = -1;
    ImageInfo.Whitebalance = -1;

    // Store file date/time.
    {
        struct stat st;
        if (stat(FileName, &st) >= 0) {
            ImageInfo.FileDateTime = st.st_mtime;
            ImageInfo.FileSize = st.st_size;
        }
    }

    strncpy(ImageInfo.FileName, FileName, PATH_MAX);
#ifdef SUPERDEBUG
    ALOGE("ReadJpegFile");
#endif
    return ReadJpegFile(FileName, ReadMode);
}

static void saveJPGFile(const char* filename) {
    char backupName[400];
    struct stat buf;

#ifdef SUPERDEBUG
    ALOGE("Modified: %s\n", filename);
#endif

    strncpy(backupName, filename, 395);
    strcat(backupName, ".t");

    // Remove any .old file name that may pre-exist
#ifdef SUPERDEBUG
    ALOGE("removing backup %s", backupName);
#endif
    unlink(backupName);

    // Rename the old file.
#ifdef SUPERDEBUG
    ALOGE("rename %s to %s", filename, backupName);
#endif
    rename(filename, backupName);

    // Write the new file.
#ifdef SUPERDEBUG
    ALOGE("WriteJpegFile %s", filename);
#endif
    if (WriteJpegFile(filename)) {

        // Copy the access rights from original file
#ifdef SUPERDEBUG
        ALOGE("stating old file %s", backupName);
#endif
        if (stat(backupName, &buf) == 0){
            // set Unix access rights and time to new file
            struct utimbuf mtime;
            chmod(filename, buf.st_mode);

            mtime.actime = buf.st_mtime;
            mtime.modtime = buf.st_mtime;

            utime(filename, &mtime);
        }

        // Now that we are done, remove original file.
#ifdef SUPERDEBUG
        ALOGE("unlinking old file %s", backupName);
#endif
        unlink(backupName);
#ifdef SUPERDEBUG
        ALOGE("returning from saveJPGFile");
#endif
    } else {
#ifdef SUPERDEBUG
        ALOGE("WriteJpegFile failed, restoring from backup file");
#endif
        // move back the backup file
        rename(backupName, filename);
    }
}

void copyThumbnailData(uchar* thumbnailData, int thumbnailLen) {
#ifdef SUPERDEBUG
    ALOGE("******************************** copyThumbnailData\n");
#endif
    Section_t* ExifSection = FindSection(M_EXIF);
    if (ExifSection == NULL) {
        return;
    }
    int NewExifSize = ImageInfo.ThumbnailOffset+8+thumbnailLen;
    ExifSection->Data = (uchar *)realloc(ExifSection->Data, NewExifSize);
    if (ExifSection->Data == NULL) {
        return;
    }
    uchar* ThumbnailPointer = ExifSection->Data+ImageInfo.ThumbnailOffset+8;

    memcpy(ThumbnailPointer, thumbnailData, thumbnailLen);

    ImageInfo.ThumbnailSize = thumbnailLen;

    Put32u(ExifSection->Data+ImageInfo.ThumbnailSizeOffset+8, thumbnailLen);

    ExifSection->Data[0] = (uchar)(NewExifSize >> 8);
    ExifSection->Data[1] = (uchar)NewExifSize;
    ExifSection->Size = NewExifSize;
}

static void saveAttributes(JNIEnv *env, jobject jobj, jstring jfilename, jstring jattributes)
{
#ifdef SUPERDEBUG
    ALOGE("******************************** saveAttributes\n");
#endif
    // format of attributes string passed from java:
    // "attrCnt attr1=valueLen value1attr2=value2Len value2..."
    // example input: "4 ImageLength=4 1024Model=6 FooImageWidth=4 1280Make=3 FOO"
    ExifElement_t* exifElementTable = NULL;
    const char* filename = NULL;
    uchar* thumbnailData = NULL;
    int attrCnt = 0;
    const char* attributes = (*env)->GetStringUTFChars(env, jattributes, NULL);
    if (attributes == NULL) {
        goto exit;
    }
#ifdef SUPERDEBUG
    ALOGE("attributes %s\n", attributes);
#endif

    // Get the number of attributes - it's the first number in the string.
    attrCnt = atoi(attributes);
    char* attrPtr = strchr(attributes, ' ') + 1;
#ifdef SUPERDEBUG
    ALOGE("attribute count %d attrPtr %s\n", attrCnt, attrPtr);
#endif

    // Load all the hash exif elements into a more c-like structure
    exifElementTable = malloc(sizeof(ExifElement_t) * attrCnt);
    if (exifElementTable == NULL) {
        goto exit;
    }
#ifdef OUTOFMEMORYTEST1
    goto exit;
#endif

    int i;
    char tag[100];
    int hasDateTimeTag = FALSE;
    int gpsTagCount = 0;
    int exifTagCount = 0;

    for (i = 0; i < attrCnt; i++) {
        // get an element from the attribute string and add it to the c structure
        // first, extract the attribute name
        char* tagEnd = strchr(attrPtr, '=');
        if (tagEnd == 0) {
#ifdef SUPERDEBUG
            ALOGE("saveAttributes: couldn't find end of tag");
#endif
            goto exit;
        }
        if (tagEnd - attrPtr > 99) {
#ifdef SUPERDEBUG
            ALOGE("saveAttributes: attribute tag way too long");
#endif
            goto exit;
        }
        memcpy(tag, attrPtr, tagEnd - attrPtr);
        tag[tagEnd - attrPtr] = 0;

        if (IsGpsTag(tag)) {
            exifElementTable[i].GpsTag = TRUE;
            exifElementTable[i].Tag = GpsTagNameToValue(tag);
            ++gpsTagCount;
        } else {
            exifElementTable[i].GpsTag = FALSE;
            exifElementTable[i].Tag = TagNameToValue(tag);
            ++exifTagCount;
        }
        attrPtr = tagEnd + 1;

        if (IsDateTimeTag(exifElementTable[i].Tag)) {
            hasDateTimeTag = TRUE;
        }

        // next get the length of the attribute value
        int valueLen = atoi(attrPtr);
        attrPtr = strchr(attrPtr, ' ') + 1;
        if (attrPtr == 0) {
#ifdef SUPERDEBUG
            ALOGE("saveAttributes: couldn't find end of value len");
#endif
            goto exit;
        }
        exifElementTable[i].Value = malloc(valueLen + 1);
        if (exifElementTable[i].Value == NULL) {
            goto exit;
        }
        memcpy(exifElementTable[i].Value, attrPtr, valueLen);
        exifElementTable[i].Value[valueLen] = 0;
        exifElementTable[i].DataLength = valueLen;

        attrPtr += valueLen;

#ifdef SUPERDEBUG
        ALOGE("tag %s id %d value %s data length=%d isGps=%d", tag, exifElementTable[i].Tag,
            exifElementTable[i].Value, exifElementTable[i].DataLength, exifElementTable[i].GpsTag);
#endif
    }

    filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
#ifdef SUPERDEBUG
    ALOGE("Call loadAttributes() with filename is %s. Loading exif info\n", filename);
#endif
    loadExifInfo(filename, TRUE);

#ifdef SUPERDEBUG
//    DumpExifMap = TRUE;
    ShowTags = TRUE;
    ShowImageInfo(TRUE);
    ALOGE("create exif 2");
#endif

    // If the jpg file has a thumbnail, preserve it.
    int thumbnailLength = ImageInfo.ThumbnailSize;
    if (ImageInfo.ThumbnailOffset) {
        Section_t* ExifSection = FindSection(M_EXIF);
        if (ExifSection) {
            uchar* thumbnailPointer = ExifSection->Data + ImageInfo.ThumbnailOffset + 8;
            thumbnailData = (uchar*)malloc(ImageInfo.ThumbnailSize);
            // if the malloc fails, we just won't copy the thumbnail
            if (thumbnailData) {
                memcpy(thumbnailData, thumbnailPointer, thumbnailLength);
            }
        }
    }

    create_EXIF(exifElementTable, exifTagCount, gpsTagCount, hasDateTimeTag);

    if (thumbnailData) {
        copyThumbnailData(thumbnailData, thumbnailLength);
    }

exit:
#ifdef SUPERDEBUG
    ALOGE("cleaning up now in saveAttributes");
#endif
    // try to clean up resources
    if (attributes) {
        (*env)->ReleaseStringUTFChars(env, jattributes, attributes);
    }
    if (filename) {
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    }
    if (exifElementTable) {
        // free the table
        for (i = 0; i < attrCnt; i++) {
            free(exifElementTable[i].Value);
        }
        free(exifElementTable);
    }
    if (thumbnailData) {
        free(thumbnailData);
    }
#ifdef SUPERDEBUG
    ALOGE("returning from saveAttributes");
#endif

// Temporarily saving these commented out lines because they represent a lot of figuring out
// patterns for JNI.
//    // Get link to Method "entrySet"
//    jmethodID entrySetMethod = (*env)->GetMethodID(env, jclass_of_hashmap, "entrySet", "()Ljava/util/Set;");
//
//    // Invoke the "entrySet" method on the HashMap object
//    jobject jobject_of_entryset = (*env)->CallObjectMethod(env, hashMap, entrySetMethod);
//
//    // Get the Set Class
//    jclass jclass_of_set = (*env)->FindClass(env, "java/util/Set");
//
//    if (jclass_of_set == 0) {
//        printf("java/util/Set lookup failed\n");
//        return;
//    }
//
//    // Get link to Method "iterator"
//    jmethodID iteratorMethod = (*env)->GetMethodID(env, jclass_of_set, "iterator", "()Ljava/util/Iterator;");
//
//    // Invoke the "iterator" method on the jobject_of_entryset variable of type Set
//    jobject jobject_of_iterator = (*env)->CallObjectMethod(env, jobject_of_entryset, iteratorMethod);
//
//    // Get the "Iterator" class
//    jclass jclass_of_iterator = (*env)->FindClass(env, "java/util/Iterator");
//
//    // Get link to Method "hasNext"
//    jmethodID hasNextMethod = (*env)->GetMethodID(env, jclass_of_iterator, "hasNext", "()Z");
//
//    // Invoke - Get the value hasNextMethod
//    jboolean bHasNext = (*env)->CallBooleanMethod(env, jobject_of_iterator, hasNextMethod);

//    // Get link to Method "hasNext"
//    jmethodID nextMethod = (*env)->GetMethodID(env, jclass_of_iterator, "next", "()Ljava/util/Map/Entry;");
//
//    jclass jclass_of_mapentry = (*env)->FindClass(env, "java/util/Map/Entry");
//
//    jmethodID getKeyMethod = (*env)->GetMethodID(env, jclass_of_mapentry, "getKey", "()Ljava/lang/Object");
//
//    jmethodID getValueMethod = (*env)->GetMethodID(env, jclass_of_mapentry, "getValue", "()Ljava/lang/Object");
}

static jboolean appendThumbnail(JNIEnv *env, jobject jobj, jstring jfilename, jstring jthumbnailfilename)
{
#ifdef SUPERDEBUG
    ALOGE("******************************** appendThumbnail\n");
#endif

    const char* filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
    if (filename == NULL) {
        return JNI_FALSE;
    }
    const char* thumbnailfilename = (*env)->GetStringUTFChars(env, jthumbnailfilename, NULL);
    if (thumbnailfilename == NULL) {
        return JNI_FALSE;
    }
 #ifdef SUPERDEBUG
     ALOGE("*******before actual call to ReplaceThumbnail\n");
     ShowImageInfo(TRUE);
 #endif
    ReplaceThumbnail(thumbnailfilename);
 #ifdef SUPERDEBUG
     ShowImageInfo(TRUE);
 #endif
    (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    (*env)->ReleaseStringUTFChars(env, jthumbnailfilename, thumbnailfilename);

    DiscardData();
    return JNI_TRUE;
}

static void commitChanges(JNIEnv *env, jobject jobj, jstring jfilename)
{
#ifdef SUPERDEBUG
    ALOGE("******************************** commitChanges\n");
#endif
    const char* filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
    if (filename) {
        saveJPGFile(filename);
        DiscardData();
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    }
}

static jbyteArray getThumbnail(JNIEnv *env, jobject jobj, jstring jfilename)
{
#ifdef SUPERDEBUG
    ALOGE("******************************** getThumbnail\n");
#endif

    const char* filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
    if (filename) {
        loadExifInfo(filename, FALSE);
        Section_t* ExifSection = FindSection(M_EXIF);
        if (ExifSection == NULL ||  ImageInfo.ThumbnailSize == 0) {
#ifdef SUPERDEBUG
    ALOGE("no exif section or size == 0, so no thumbnail\n");
#endif
            goto noThumbnail;
        }
        uchar* thumbnailPointer = ExifSection->Data + ImageInfo.ThumbnailOffset + 8;

        jbyteArray byteArray = (*env)->NewByteArray(env, ImageInfo.ThumbnailSize);
        if (byteArray == NULL) {
#ifdef SUPERDEBUG
    ALOGE("couldn't allocate thumbnail memory, so no thumbnail\n");
#endif
            goto noThumbnail;
        }
        (*env)->SetByteArrayRegion(env, byteArray, 0, ImageInfo.ThumbnailSize, thumbnailPointer);
#ifdef SUPERDEBUG
    ALOGE("thumbnail size %d\n", ImageInfo.ThumbnailSize);
#endif
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        DiscardData();
        return byteArray;
    }
noThumbnail:
    if (filename) {
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    }
    DiscardData();
    return NULL;
}

static jlongArray getThumbnailRange(JNIEnv *env, jobject jobj, jstring jfilename) {
    jlongArray resultArray = NULL;
    const char* filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
    if (filename) {
        loadExifInfo(filename, FALSE);
        Section_t* ExifSection = FindSection(M_EXIF);
        if (ExifSection == NULL || ImageInfo.ThumbnailSize == 0) {
            goto done;
        }

        jlong result[2];
        result[0] = ExifSection->Offset + ImageInfo.ThumbnailOffset + 8;
        result[1] = ImageInfo.ThumbnailSize;

        resultArray = (*env)->NewLongArray(env, 2);
        if (resultArray == NULL) {
            goto done;
        }

        (*env)->SetLongArrayRegion(env, resultArray, 0, 2, result);
    }
done:
    if (filename) {
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    }
    DiscardData();
    return resultArray;
}

static int attributeCount;      // keep track of how many attributes we've added

// returns new buffer length
static int addKeyValueString(char** buf, int bufLen, const char* key, const char* value) {
    // Appends to buf like this: "ImageLength=4 1024"

    char valueLen[15];
    snprintf(valueLen, 15, "=%d ", (int)strlen(value));

    // check to see if buf has enough room to append
    int len = strlen(key) + strlen(valueLen) + strlen(value);
    int newLen = strlen(*buf) + len;
    if (newLen >= bufLen) {
#ifdef REALLOCTEST
        bufLen = newLen + 5;
        ALOGE("reallocing to %d", bufLen);
#else
        bufLen = newLen + 500;
#endif
        *buf = realloc(*buf, bufLen);
        if (*buf == NULL) {
            return 0;
        }
    }
    // append the new attribute and value
    snprintf(*buf + strlen(*buf), bufLen, "%s%s%s", key, valueLen, value);
#ifdef SUPERDEBUG
    ALOGE("buf %s", *buf);
#endif
    ++attributeCount;
    return bufLen;
}

// returns new buffer length
static int addKeyValueInt(char** buf, int bufLen, const char* key, int value) {
    char valueStr[20];
    snprintf(valueStr, 20, "%d", value);

    return addKeyValueString(buf, bufLen, key, valueStr);
}

// returns new buffer length
static int addKeyValueDouble(char** buf, int bufLen, const char* key, double value, const char* format) {
    char valueStr[30];
    snprintf(valueStr, 30, format, value);

    return addKeyValueString(buf, bufLen, key, valueStr);
}

// Returns new buffer length. Rational value will be appended as "numerator/denominator".
static int addKeyValueRational(char** buf, int bufLen, const char* key, rat_t value) {
    char valueStr[25];
    snprintf(valueStr, sizeof(valueStr), "%u/%u", value.num, value.denom);
    return addKeyValueString(buf, bufLen, key, valueStr);
}

static jstring getAttributes(JNIEnv *env, jobject jobj, jstring jfilename)
{
#ifdef SUPERDEBUG
    ALOGE("******************************** getAttributes\n");
#endif
    const char* filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
    loadExifInfo(filename, FALSE);
#ifdef SUPERDEBUG
    ShowImageInfo(TRUE);
#endif
    (*env)->ReleaseStringUTFChars(env, jfilename, filename);

    attributeCount = 0;
#ifdef REALLOCTEST
    int bufLen = 5;
#else
    int bufLen = 1000;
#endif
    char* buf = malloc(bufLen);
    if (buf == NULL) {
        return NULL;
    }
    *buf = 0;   // start the string out at zero length

    // save a fake "hasThumbnail" tag to pass to the java ExifInterface
    bufLen = addKeyValueString(&buf, bufLen, "hasThumbnail",
        ImageInfo.ThumbnailOffset == 0 || ImageInfo.ThumbnailAtEnd == FALSE || ImageInfo.ThumbnailSize == 0 ?
            "false" : "true");
    if (bufLen == 0) return NULL;

    if (ImageInfo.CameraMake[0]) {
        bufLen = addKeyValueString(&buf, bufLen, "Make", ImageInfo.CameraMake);
        if (bufLen == 0) return NULL;
    }
    if (ImageInfo.CameraModel[0]) {
        bufLen = addKeyValueString(&buf, bufLen, "Model", ImageInfo.CameraModel);
        if (bufLen == 0) return NULL;
    }
    if (ImageInfo.DateTime[0]) {
        bufLen = addKeyValueString(&buf, bufLen, "DateTime", ImageInfo.DateTime);
        if (bufLen == 0) return NULL;
    }
    bufLen = addKeyValueInt(&buf, bufLen, "ImageWidth", ImageInfo.Width);
    if (bufLen == 0) return NULL;

    bufLen = addKeyValueInt(&buf, bufLen, "ImageLength", ImageInfo.Height);
    if (bufLen == 0) return NULL;

    bufLen = addKeyValueInt(&buf, bufLen, "Orientation", ImageInfo.Orientation);
    if (bufLen == 0) return NULL;

    if (ImageInfo.FlashUsed >= 0) {
        bufLen = addKeyValueInt(&buf, bufLen, "Flash", ImageInfo.FlashUsed);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.FocalLength.num != 0 && ImageInfo.FocalLength.denom != 0) {
        bufLen = addKeyValueRational(&buf, bufLen, "FocalLength", ImageInfo.FocalLength);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.DigitalZoomRatio > 1.0){
        // Digital zoom used.  Shame on you!
        bufLen = addKeyValueDouble(&buf, bufLen, "DigitalZoomRatio", ImageInfo.DigitalZoomRatio, "%1.3f");
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.ExposureTime){
        const char* format;
        if (ImageInfo.ExposureTime < 0.010){
            format = "%6.4f";
        } else {
            format = "%5.3f";
        }

        bufLen = addKeyValueDouble(&buf, bufLen, "ExposureTime", (double)ImageInfo.ExposureTime, format);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.ApertureFNumber){
        bufLen = addKeyValueDouble(&buf, bufLen, "FNumber", (double)ImageInfo.ApertureFNumber, "%3.1f");
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.Distance){
        bufLen = addKeyValueDouble(&buf, bufLen, "SubjectDistance", (double)ImageInfo.Distance, "%4.2f");
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.ISOequivalent){
        bufLen = addKeyValueInt(&buf, bufLen, "ISOSpeedRatings", ImageInfo.ISOequivalent);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.ExposureBias){
        // If exposure bias was specified, but set to zero, presumably its no bias at all,
        // so only show it if its nonzero.
        bufLen = addKeyValueDouble(&buf, bufLen, "ExposureBiasValue", (double)ImageInfo.ExposureBias, "%4.2f");
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.Whitebalance >= 0) {
        bufLen = addKeyValueInt(&buf, bufLen, "WhiteBalance", ImageInfo.Whitebalance);
        if (bufLen == 0) return NULL;
    }

    bufLen = addKeyValueInt(&buf, bufLen, "LightSource", ImageInfo.LightSource);
    if (bufLen == 0) return NULL;


    if (ImageInfo.MeteringMode) {
        bufLen = addKeyValueInt(&buf, bufLen, "MeteringMode", ImageInfo.MeteringMode);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.ExposureProgram) {
        bufLen = addKeyValueInt(&buf, bufLen, "ExposureProgram", ImageInfo.ExposureProgram);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.ExposureMode) {
        bufLen = addKeyValueInt(&buf, bufLen, "ExposureMode", ImageInfo.ExposureMode);
        if (bufLen == 0) return NULL;
    }

    if (ImageInfo.GpsInfoPresent) {
        if (ImageInfo.GpsLatRaw[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSLatitude", ImageInfo.GpsLatRaw);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsLatRef[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSLatitudeRef", ImageInfo.GpsLatRef);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsLongRaw[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSLongitude", ImageInfo.GpsLongRaw);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsLongRef[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSLongitudeRef", ImageInfo.GpsLongRef);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsAlt[0]) {
            bufLen = addKeyValueRational(&buf, bufLen, "GPSAltitude", ImageInfo.GpsAltRaw);
            bufLen = addKeyValueInt(&buf, bufLen, "GPSAltitudeRef", ImageInfo.GpsAltRef);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsDateStamp[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSDateStamp", ImageInfo.GpsDateStamp);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsTimeStamp[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSTimeStamp", ImageInfo.GpsTimeStamp);
            if (bufLen == 0) return NULL;
        }
        if (ImageInfo.GpsProcessingMethod[0]) {
            bufLen = addKeyValueString(&buf, bufLen, "GPSProcessingMethod", ImageInfo.GpsProcessingMethod);
            if (bufLen == 0) return NULL;
        }
    }

    if (ImageInfo.Comments[0]) {
        bufLen = addKeyValueString(&buf, bufLen, "UserComment", ImageInfo.Comments);
        if (bufLen == 0) return NULL;
    }

    // put the attribute count at the beginnnig of the string
    int finalBufLen = strlen(buf) + 20;
    char* finalResult = malloc(finalBufLen);
    if (finalResult == NULL) {
        free(buf);
        return NULL;
    }
    snprintf(finalResult, finalBufLen, "%d %s", attributeCount, buf);
    int k;
    for (k = 0; k < finalBufLen; k++)
        if (!isascii(finalResult[k]))
            finalResult[k] = '?';
    free(buf);

#ifdef SUPERDEBUG
    ALOGE("*********Returning result \"%s\"", finalResult);
#endif
    jstring result = ((*env)->NewStringUTF(env, finalResult));
    free(finalResult);
    DiscardData();
    return result;
}

static const char *classPathName = "android/media/ExifInterface";

static JNINativeMethod methods[] = {
  {"saveAttributesNative", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)saveAttributes },
  {"getAttributesNative", "(Ljava/lang/String;)Ljava/lang/String;", (void*)getAttributes },
  {"appendThumbnailNative", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)appendThumbnail },
  {"commitChangesNative", "(Ljava/lang/String;)V", (void*)commitChanges },
  {"getThumbnailNative", "(Ljava/lang/String;)[B", (void*)getThumbnail },
  {"getThumbnailRangeNative", "(Ljava/lang/String;)[J", (void*)getThumbnailRange },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        fprintf(stderr,
            "Native registration unable to find class '%s'\n", className);
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        fprintf(stderr, "RegisterNatives failed for '%s'\n", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 */
static int registerNatives(JNIEnv* env)
{
    return jniRegisterNativeMethods(env, classPathName,
                                    methods, NELEM(methods));
}

/*
 * Set some test stuff up.
 *
 * Returns the JNI version on success, -1 on failure.
 */
__attribute__ ((visibility("default"))) jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        fprintf(stderr, "ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    printf("In mgmain JNI_OnLoad\n");

    if (registerNatives(env) < 0) {
        fprintf(stderr, "ERROR: Exif native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
