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
#ifndef MARKER_H
#define MARKER_H

#define ADD_CODE_MARKER_FUN(m_condition)                    \
    if ( !(m_condition) )                                   \
    {                                                       \
        __asm__ volatile (                                  \
            ".word     0x21614062\n\t"      /* '!a@b' */    \
            ".word     0x47712543\n\t"      /* 'Gq%C' */    \
            ".word     0x5F5F5F43\n\t"      /* '___C' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x245F5F5F"          /* '$___' */    \
        );                                                  \
    }

#define ADD_TEXT_MARKER_FUN(m_condition)                    \
    if ( !(m_condition) )                                   \
    {                                                       \
        __asm__ volatile (                                  \
            ".word     0x21614062\n\t"      /* '!a@b' */    \
            ".word     0x47712543\n\t"      /* 'Gq%C' */    \
            ".word     0x5F5F5F54\n\t"      /* '___T' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x5F5F5F5F\n\t"      /* '____' */    \
            ".word     0x245F5F5F"          /* '$___' */    \
        );                                                  \
    }

#endif
