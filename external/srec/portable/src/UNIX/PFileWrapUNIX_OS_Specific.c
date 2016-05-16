/*---------------------------------------------------------------------------*
 *  PFileWrapUNIX_OS_Specific.c  *
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

#include <sys/types.h>
#include <sys/stat.h>

#include "errno.h"
#include "PFileSystemImpl.h"
#include "PANSIFileSystem.h"
#include "PANSIFileSystemImpl.h"
#include "phashtable.h"
#include "LCHAR.h"
#include "plog.h"

ESR_ReturnCode pf_make_dir ( const LCHAR* path )
    {
    ESR_ReturnCode rc;

    passert(path!=NULL);

    if ( mkdir ( path, S_IRWXU|S_IRWXG|S_IRWXO ) == 0)
	{
	rc = ESR_SUCCESS;
	}
    else
        {
        switch (errno)
            {
            case EEXIST:
                rc = ESR_IDENTIFIER_COLLISION;
		break;

            case ENOENT:
                rc = ESR_NO_MATCH_ERROR;
		break;

            default:
                PLogError ( L("ESR_INVALID_STATE") );
                rc = ESR_INVALID_STATE;
		break;
            }
        }
    return ( rc );
    }



ESR_ReturnCode pf_get_cwd ( LCHAR* path, size_t *len )
    {
    ESR_ReturnCode rc;

    if ( path != NULL )
	{
        if ( getcwd ( path, *len ) != NULL)
	    {
	    rc = ESR_SUCCESS;
	    }
	else
	    {
            switch ( errno )
                {
                case ERANGE:
                    rc =  ESR_BUFFER_OVERFLOW;
		    break;

                case ENOMEM:
                    rc =  ESR_OUT_OF_MEMORY;
		    break;

                default:
                    PLogError(L("ESR_INVALID_STATE"));
                    rc = ESR_INVALID_STATE;
		    break;
                }
	    }
	}
    else
	{
	rc = ESR_INVALID_ARGUMENT;
	PLogError(ESR_rc2str(rc));
	}

    return ( rc );
    }



ESR_ReturnCode pf_change_dir ( const LCHAR* path )
    {
    ESR_ReturnCode rc;

    passert ( path != NULL );
    passert ( *path != '\0' );

    if ( chdir ( path ) == 0 )
	rc = ESR_SUCCESS;
    else
	rc = ESR_NO_MATCH_ERROR;
    return ( rc );
    }
