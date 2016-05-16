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
 ************************************************************************
 * @file    M4OSA_Export.h
 * @brief    Data access export types for Android
 * @note    This file defines types which must be
 *          used to import or export any function.
 ************************************************************************
*/

#ifndef M4OSA_EXPORT_H
#define M4OSA_EXPORT_H

#ifdef __cplusplus
extern "C" {
#endif /*__cplusplus*/

/************************************/
/*  OSAL EXPORTS                    */
/************************************/

#define M4OSAL_CHARSTAR_EXPORT_TYPE            /**< OSAL CHAR_STAR        */
#define M4OSAL_CLOCK_EXPORT_TYPE            /**< OSAL CLOCK            */
#define M4OSAL_DATE_EXPORT_TYPE                /**< OSAL DATE            */
#define M4OSAL_FILE_EXPORT_TYPE                /**< OSAL FILE            */
#define M4OSAL_REALTIME_EXPORT_TYPE            /**< OSAL REAL TIME        */
#define M4OSAL_SOCKET_EXPORT_TYPE            /**< SOCKET                */
#define M4OSAL_STRING_EXPORT_TYPE            /**< OSAL STRING        */
#define M4OSAL_URI_EXPORT_TYPE                /**< OSAL URI            */
#define M4OSAL_MEMORY_EXPORT_TYPE            /**< OSAL MEMORY        */
#define M4OSAL_TRACE_EXPORT_TYPE            /**< OSAL TRACE            */
#define M4OSAL_TOOL_TIMER_EXPORT_TYPE        /**< OSAL TOOL TIMER    */
#define M4OSAL_SYSTEM_CM_EXPORT_TYPE        /**< SYSTEM COMMON API    */
#define M4OSAL_LINKED_LIST_EXPORT_TYPE        /**< TOOL LINKED LIST    */
#define M4OSAL_MEMORY_MANAGER_EXPORT_TYPE    /**< MEMORY MANAGER        */
#define M4OSAL_TRACE_MANAGER_EXPORT_TYPE    /**< TOOL TRACE MANAGER */
#define M4VPS_EXPORT_TYPE                    /**< VPS API            */
#define M4AP_EXPORT_TYPE                    /**< AUDIO PRESENTERS    */
#define M4VP_EXPORT_TYPE                    /**< VIDEO PRESENTERS    */
#define M4CB_EXPORT_TYPE                    /**< Call back            */

#ifdef __cplusplus
}
#endif /*__cplusplus*/

#endif /*M4OSA_EXPORT_H*/

