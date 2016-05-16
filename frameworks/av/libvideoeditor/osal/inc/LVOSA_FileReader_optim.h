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

/**
 ******************************************************************************
 * @file         M4OSA_FileReader_optim.h
 * @brief        File reader for Symbian
 * @note         This file declares functions and types to read a file.
 ******************************************************************************
*/



#ifndef M4OSA_FILEREADER_OPTIM_H
#define M4OSA_FILEREADER_OPTIM_H

#define M4OSA_READER_OPTIM_USE_OSAL_IF

/**/
#ifndef M4OSA_READER_OPTIM_USE_OSAL_IF
    typedef struct
    {
        M4OSA_Void*        (*pFctPtr_Open)( M4OSA_Void* fd,
                                            M4OSA_UInt32 FileModeAccess,
                                            M4OSA_UInt16* errno );
        M4OSA_FilePosition (*pFctPtr_Read)( M4OSA_Void* fd,
                                            M4OSA_UInt8* data,
                                            M4OSA_FilePosition size,
                                            M4OSA_UInt16* errno );
        M4OSA_FilePosition (*pFctPtr_Seek)( M4OSA_Void* fd,
                                            M4OSA_FilePosition pos,
                                            M4OSA_FileSeekAccessMode mode,
                                            M4OSA_UInt16* errno );
        M4OSA_FilePosition (*pFctPtr_Tell)( M4OSA_Void* fd,
                                            M4OSA_UInt16* errno );
        M4OSA_Int32        (*pFctPtr_Close)( M4OSA_Void* fd,
                                             M4OSA_UInt16* errno );
        M4OSA_Void         (*pFctPtr_AccessType)( M4OSA_UInt32 FileModeAccess_In,
                                                  M4OSA_Void* FileModeAccess_Out );

    } M4OSA_FileSystem_FctPtr;
#endif
/**/


/* Reader API : bufferized functions */
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_ERR M4OSA_fileReadOpen_optim( M4OSA_Context* context,
                                        M4OSA_Void* fileDescriptor,
                                        M4OSA_UInt32 fileModeAccess);
#else
    M4OSA_ERR M4OSA_fileReadOpen_optim( M4OSA_Context* context,
                                        M4OSA_Void* fileDescriptor,
                                        M4OSA_UInt32 fileModeAccess,
                                        M4OSA_FileSystem_FctPtr *FS);
#endif

M4OSA_ERR M4OSA_fileReadData_optim( M4OSA_Context context,
                                    M4OSA_MemAddr8 buffer,
                                    M4OSA_UInt32* size );
M4OSA_ERR M4OSA_fileReadSeek_optim( M4OSA_Context context,
                                    M4OSA_FileSeekAccessMode seekMode,
                                    M4OSA_FilePosition* position );
M4OSA_ERR M4OSA_fileReadClose_optim( M4OSA_Context context );
M4OSA_ERR M4OSA_fileReadGetOption_optim( M4OSA_Context context,
                                         M4OSA_FileReadOptionID optionID,
                                         M4OSA_DataOption *optionValue );
M4OSA_ERR M4OSA_fileReadSetOption_optim( M4OSA_Context context,
                                         M4OSA_FileReadOptionID optionID,
                                         M4OSA_DataOption optionValue );

#endif /* M4OSA_FILEREADER_OPTIM_H */
