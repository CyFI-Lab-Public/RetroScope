/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


#ifndef CTSAUDIO_FILEUTIL_H
#define CTSAUDIO_FILEUTIL_H

#include <stdarg.h>

#include <utils/String8.h>
#include <utils/threads.h>
#include <iostream>
#include <fstream>

/**
 * Class to write to file and stdout at the same time.
 *
 */
class FileUtil {
public:
    /**
     * create log / report dir
     * @param dirPath returns path of created dir
     */
    static bool prepare(android::String8& dirPath);

protected:
    FileUtil();
    virtual ~FileUtil();

    /**
     * if fileName is NULL, only stdout output will be supproted
     */
    virtual bool init(const char* fileName);

    virtual bool doPrintf(const char* fmt, ...);
    /// fileOnly log only to file
    /// loglevel 0 .., -1 means no log level.
    virtual bool doVprintf(bool fileOnly, int loglevel, const char *fmt, va_list ap);

private:
    // store dirPath to prevent creating multiple times
    static android::String8 mDirPath;
    std::ofstream mFile;
    static const int DEFAULT_BUFFER_SIZE = 1024;
    // buffer for printf. one line longer than this will be truncated.
    char* mBuffer;
    int mBufferSize;
    android::Mutex mWriteLock;
};


#endif // CTSAUDIO_FILEUTIL_H
