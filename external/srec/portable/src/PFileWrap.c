/*---------------------------------------------------------------------------*
 *  PFileWrap.c  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdarg.h>
#include "LCHAR.h"
#include "pendian.h"
#include "PFile.h"
#include "plog.h"
#include "pstdio.h"
#include "ptypes.h"




ESR_ReturnCode PFileClose( PFile *self )
    {

    fclose ( (FILE *)self );
    return ( ESR_SUCCESS );
    }



ESR_ReturnCode PFileRead ( PFile *self, void *buffer, size_t size, size_t *count )
    {
    ESR_ReturnCode  read_status;
    size_t          items_read;
    int             ferror_status;

    items_read = fread ( buffer, size, *count, (FILE *)self );

    if ( items_read > 0 )
        {
        read_status = ESR_SUCCESS;
        *count = items_read;
        }
    else
        {
        ferror_status = ferror ( (FILE *)self );

        if ( ferror_status == 0 )
            {
            read_status = ESR_SUCCESS;
            *count = items_read;
            }
        else
            {
            read_status = ESR_READ_ERROR;
            }
        }
    return ( read_status );
    }



ESR_ReturnCode PFileWrite ( PFile *self, const void *buffer, size_t size, size_t *count )
    {
    ESR_ReturnCode  write_status;
    size_t          items_written;

    items_written = fwrite ( buffer, size, *count, (FILE *)self );

    if ( items_written == ( *count ) )
        {
        write_status = ESR_SUCCESS;
        *count = items_written;
        }
    else
        {
        write_status = ESR_READ_ERROR;
        }
    return ( write_status );
    }



ESR_ReturnCode PFileFlush ( PFile *self )
    {
    ESR_ReturnCode  flush_status;
    size_t          flush_ok;

    flush_ok = fflush ( (FILE *)self );

    if ( flush_ok == 0 )
        {
        flush_status = ESR_SUCCESS;
        }
    else
        {
        flush_status = ESR_FLUSH_ERROR;
        }
    return ( flush_status );
    }



ESR_ReturnCode PFileSeek ( PFile *self, long offset, int origin )
    {
    ESR_ReturnCode  seek_status;
    size_t          seek_ok;

    seek_ok = fseek ( (FILE *)self, offset, origin );

    if ( seek_ok == 0 )
        {
        seek_status = ESR_SUCCESS;
        }
    else
        {
        seek_status = ESR_SEEK_ERROR;
        }
    return ( seek_status );
    }



ESR_ReturnCode PFileGetPosition ( PFile *self, size_t *position )
    {
    ESR_ReturnCode  get_status;
    long            ftell_result;

    ftell_result = ftell ( (FILE *)self );

    if ( ftell_result >= 0 )
        {
	*position = (size_t)ftell_result;
        get_status = ESR_SUCCESS;
        }
    else
        {
        get_status = ESR_INVALID_STATE;
        }
    return ( get_status );
    }



ESR_ReturnCode PFileIsEOF ( PFile *self, ESR_BOOL *isEof )
    {
#ifdef NO_FEOF
    long            posCur;    /* remember current file position */
    long            posEnd;    /* end of file position */

    posCur = ftell ( self );
    fseek ( self, 0, SEEK_END );
    posEnd = ftell ( self );

    if ( posCur == posEnd )
        *isEof = ESR_TRUE;
    else
        *isEof = ESR_FALSE;
    fseek ( self, posCur, SEEK_SET );  /* restore position in file */
#else
    int             is_eof;

    is_eof = feof ( (FILE *)self );

    if ( is_eof != 0 )
        *isEof = ESR_TRUE;
    else
        *isEof = ESR_FALSE;
#endif
    return ( ESR_SUCCESS );
    }



ESR_ReturnCode PFileIsErrorSet ( PFile *self, ESR_BOOL *isError )
    {
    int is_error;

    is_error = ferror ( (FILE *)self );

    if ( is_error != 0 )
        *isError = ESR_TRUE;
    else
        *isError = ESR_FALSE;
    return ( ESR_SUCCESS );
    }



ESR_ReturnCode PFileClearError ( PFile *self )
    {

    clearerr ( (FILE *)self );
    return ( ESR_SUCCESS );
    }



ESR_ReturnCode PFileVfprintf ( PFile *self, int *result, const LCHAR *format, va_list args )
    {
    int bytes_printed;

    bytes_printed = vfprintf ( (FILE *)self, format, args );

    if ( result != NULL )
        *result = bytes_printed;
    return ( ESR_SUCCESS );
    }



ESR_ReturnCode PFileFgetc ( PFile *self, LINT *result )
    {
    ESR_ReturnCode  fgetc_status;
    int             error_status;

    *result = fgetc ( (FILE *)self );

    if ( ( *result ) != EOF )
        {
        fgetc_status = ESR_SUCCESS;
        }
    else
        {
        error_status = ferror ( (FILE *)self );

        if ( error_status == 0 )
            fgetc_status = ESR_SUCCESS;
        else
            fgetc_status = ESR_INVALID_STATE;
        }
    return ( fgetc_status );
    }



ESR_ReturnCode PFileFgets ( PFile *self, LCHAR *string, int n, LCHAR **result )
    {
    ESR_ReturnCode  fgets_status;
    int             error_status;
    LCHAR           *temp;

    temp = fgets ( string, n, (FILE *)self );

    if ( temp != NULL )
        {
        fgets_status = ESR_SUCCESS;

        if ( result != NULL )
            *result = temp;
        }
    else
        {
        error_status = ferror ( (FILE *)self );

        if ( error_status == 0 )
            {
            fgets_status = ESR_SUCCESS;

            if ( result != NULL )
                *result = NULL;
            }
        else
            fgets_status = ESR_INVALID_STATE;
        }
    return ( fgets_status );
    }



PFile *pfopen ( const LCHAR *filename, const LCHAR *mode )
    {
    PFile           *result;

    result = (PFile *)fopen ( filename, mode );
    return ( result );
    }



size_t pfread ( void *buffer, size_t size, size_t count, PFile *stream )
    {
    ESR_ReturnCode rc;

    rc = PFileRead ( stream, buffer, size, &count );

    if ( rc != ESR_SUCCESS )
        return ( 0 );
    return ( count );
    }



size_t pfwrite ( const void *buffer, size_t size, size_t count, PFile *stream )
    {
    ESR_ReturnCode rc;

    rc = PFileWrite ( stream, buffer, size, &count );
    if ( rc != ESR_SUCCESS )
        return ( 0 );
    return ( count );
    }



int pfclose ( PFile *stream )
    {

    fclose ( (FILE *)stream );

    return ( 0 );
    }



void prewind (PFile *stream)
    {

    PFileSeek ( stream, 0, SEEK_SET );
    }



int pfseek ( PFile *stream, long offset, int origin )
    {
    ESR_ReturnCode rc;

    rc = PFileSeek ( stream, offset, origin );

    if ( rc != ESR_SUCCESS )
        return ( 1 );
    return ( 0 );
    }



long pftell ( PFile *stream )
    {
    ESR_ReturnCode  rc;
    size_t          result;

    rc = PFileGetPosition ( stream, &result );

    if ( rc != ESR_SUCCESS )
        return ( -1 );
    return ( result );
    }



int pfeof ( PFile *stream )
    {
    ESR_BOOL eof;

    PFileIsEOF ( stream, &eof );

    if ( ! eof )
        return ( 0 );
        return ( 1 );
    }



int pferror ( PFile *stream )
    {
    ESR_BOOL error;

    PFileIsErrorSet ( stream, &error );

    if ( ! error )
        return ( 0 );
    return ( 1 );
    }



void pclearerr ( PFile *stream )
    {

    PFileClearError ( stream );
    }



int pfflush ( PFile *stream )
    {
    ESR_ReturnCode rc;

    rc = PFileFlush ( stream );

    if ( rc != ESR_SUCCESS )
        return ( PEOF );
    return ( 0 );
    }



LCHAR* pfgets ( LCHAR *string, int n, PFile *self )
    {
    LCHAR           *result;
    ESR_ReturnCode  rc;

    rc = PFileFgets ( self, string, n, &result );

    if ( rc != ESR_SUCCESS )
        return ( NULL );
    return ( result );
    }



LINT pfgetc ( PFile *self )
    {
    ESR_ReturnCode  rc;
    LINT            result;

    rc = PFileFgetc ( self, &result );

    if ( rc != ESR_SUCCESS )
        return ( PEOF );
    return ( result );
    }



int pfprintf ( PFile *stream, const LCHAR *format, ... )
    {
    ESR_ReturnCode  rc;
    int             result;
    va_list         args;

    va_start ( args, format );
    rc = PFileVfprintf ( stream, &result, format, args );
    va_end ( args );

    if ( rc != ESR_SUCCESS )
        return ( -1 );
    return ( result );
    }



int pvfprintf ( PFile *stream, const LCHAR *format, va_list argptr )
    {
    ESR_ReturnCode  rc;
    int             result;

    rc = PFileVfprintf ( stream, &result, format, argptr );

    if ( rc != ESR_SUCCESS )
        return ( -1 );
    return ( result );
    }


ESR_ReturnCode pf_convert_backslashes_to_forwardslashes ( LCHAR *string_to_convert )
    {
    ESR_ReturnCode  rc;
    int             string_status;

    if ( string_to_convert != NULL )
        {
	string_status = lstrreplace ( string_to_convert, L('\\'), L('/') );

	if ( string_status == 0 )
	    rc = ESR_SUCCESS;
	else
	    rc = ESR_INVALID_ARGUMENT;
	}
    else
	{
	rc = ESR_INVALID_ARGUMENT;
	}
    return ( rc );
    }



ESR_ReturnCode pf_is_path_absolute ( const LCHAR* input_path, ESR_BOOL* isAbsolute )
    {
    ESR_ReturnCode rc;
    LCHAR path [P_PATH_MAX];
	     
    if ( isAbsolute != NULL )
        {
	LSTRCPY ( path, input_path );
	rc = pf_convert_backslashes_to_forwardslashes ( path );

	if ( rc == ESR_SUCCESS )
	    {
	    if ( ( path [0] == '/' ) || ( ( LISALPHA ( path [0] ) ) && ( path [1] == ':' ) && ( path [2] == '/' ) ) )
		*isAbsolute = ESR_TRUE;
	    else
		*isAbsolute = ESR_FALSE;
	    }
	}
    else
	{
	rc = ESR_INVALID_ARGUMENT;
	}
    return ( rc );
    }    
 
