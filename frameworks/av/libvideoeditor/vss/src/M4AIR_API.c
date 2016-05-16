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
 *************************************************************************
 * @file   M4AIR_API.c
 * @brief  Area of Interest Resizer  API
 *************************************************************************
 */

#define M4AIR_YUV420_FORMAT_SUPPORTED
#define M4AIR_YUV420A_FORMAT_SUPPORTED

/************************* COMPILATION CHECKS ***************************/
#ifndef M4AIR_YUV420_FORMAT_SUPPORTED
#ifndef M4AIR_BGR565_FORMAT_SUPPORTED
#ifndef M4AIR_RGB565_FORMAT_SUPPORTED
#ifndef M4AIR_BGR888_FORMAT_SUPPORTED
#ifndef M4AIR_RGB888_FORMAT_SUPPORTED
#ifndef M4AIR_JPG_FORMAT_SUPPORTED

#error "Please define at least one input format for the AIR component"

#endif
#endif
#endif
#endif
#endif
#endif

/******************************* INCLUDES *******************************/
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_CoreID.h"
#include "M4OSA_Mutex.h"
#include "M4OSA_Memory.h"
#include "M4VIFI_FiltersAPI.h"
#include "M4AIR_API.h"

/************************ M4AIR INTERNAL TYPES DEFINITIONS ***********************/

/**
 ******************************************************************************
 * enum         M4AIR_States
 * @brief       The following enumeration defines the internal states of the AIR.
 ******************************************************************************
 */
typedef enum
{
    M4AIR_kCreated,        /**< State after M4AIR_create has been called */
    M4AIR_kConfigured      /**< State after M4AIR_configure has been called */
}M4AIR_States;


/**
 ******************************************************************************
 * struct         M4AIR_InternalContext
 * @brief         The following structure is the internal context of the AIR.
 ******************************************************************************
 */
typedef struct
{
    M4AIR_States            m_state;        /**< Internal state */
    M4AIR_InputFormatType   m_inputFormat;  /**< Input format like YUV420Planar,
                                                 RGB565, JPG, etc ... */
    M4AIR_Params            m_params;       /**< Current input Parameter of  the processing */
    M4OSA_UInt32            u32_x_inc[4];   /**< ratio between input and ouput width for YUV */
    M4OSA_UInt32            u32_y_inc[4];   /**< ratio between input and ouput height for YUV */
    M4OSA_UInt32            u32_x_accum_start[4];    /**< horizontal initial accumulator value */
    M4OSA_UInt32            u32_y_accum_start[4];    /**< Vertical initial accumulator value */
    M4OSA_UInt32            u32_x_accum[4]; /**< save of horizontal accumulator value */
    M4OSA_UInt32            u32_y_accum[4]; /**< save of vertical accumulator value */
    M4OSA_UInt8*            pu8_data_in[4]; /**< Save of input plane pointers
                                                             in case of stripe mode */
    M4OSA_UInt32            m_procRows;     /**< Number of processed rows,
                                                     used in stripe mode only */
    M4OSA_Bool                m_bOnlyCopy;  /**< Flag to know if we just perform a copy
                                                        or a bilinear interpolation */
    M4OSA_Bool                m_bFlipX;     /**< Depend on output orientation, used during
                                                processing to revert processing order in X
                                                coordinates */
    M4OSA_Bool                m_bFlipY;     /**< Depend on output orientation, used during
                                                processing to revert processing order in Y
                                                coordinates */
    M4OSA_Bool                m_bRevertXY;  /**< Depend on output orientation, used during
                                                processing to revert X and Y processing order
                                                 (+-90° rotation) */
}M4AIR_InternalContext;

/********************************* MACROS *******************************/
#define M4ERR_CHECK_NULL_RETURN_VALUE(retval, pointer)\
     if ((pointer) == M4OSA_NULL) return ((M4OSA_ERR)(retval));


/********************** M4AIR PUBLIC API IMPLEMENTATION ********************/
/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_create(M4OSA_Context* pContext,M4AIR_InputFormatType inputFormat)
 * @brief    This function initialize an instance of the AIR.
 * @param    pContext:      (IN/OUT) Address of the context to create
 * @param    inputFormat:   (IN) input format type.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only). Invalid formatType
 * @return    M4ERR_ALLOC: No more memory is available
 ******************************************************************************
 */
M4OSA_ERR M4AIR_create(M4OSA_Context* pContext,M4AIR_InputFormatType inputFormat)
{
    M4OSA_ERR err = M4NO_ERROR ;
    M4AIR_InternalContext* pC = M4OSA_NULL ;

    /* Check that the address on the context is not NULL */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    *pContext = M4OSA_NULL ;

    /* Internal Context creation */
    pC = (M4AIR_InternalContext*)M4OSA_32bitAlignedMalloc(sizeof(M4AIR_InternalContext),
         M4AIR,(M4OSA_Char *)"AIR internal context") ;
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_ALLOC, pC) ;


    /* Check if the input format is supported */
    switch(inputFormat)
    {
#ifdef M4AIR_YUV420_FORMAT_SUPPORTED
        case M4AIR_kYUV420P:
        break ;
#endif
#ifdef M4AIR_YUV420A_FORMAT_SUPPORTED
        case M4AIR_kYUV420AP:
        break ;
#endif
        default:
            err = M4ERR_AIR_FORMAT_NOT_SUPPORTED;
            goto M4AIR_create_cleanup ;
    }

    /**< Save input format and update state */
    pC->m_inputFormat = inputFormat;
    pC->m_state = M4AIR_kCreated;

    /* Return the context to the caller */
    *pContext = pC ;

    return M4NO_ERROR ;

M4AIR_create_cleanup:
    /* Error management : we destroy the context if needed */
    if(M4OSA_NULL != pC)
    {
        free(pC) ;
    }

    *pContext = M4OSA_NULL ;

    return err ;
}



/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_cleanUp(M4OSA_Context pContext)
 * @brief    This function destroys an instance of the AIR component
 * @param    pContext:    (IN) Context identifying the instance to destroy
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only).
 * @return    M4ERR_STATE: Internal state is incompatible with this function call.
 ******************************************************************************
 */
M4OSA_ERR M4AIR_cleanUp(M4OSA_Context pContext)
{
    M4AIR_InternalContext* pC = (M4AIR_InternalContext*)pContext ;

    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    /**< Check state */
    if((M4AIR_kCreated != pC->m_state)&&(M4AIR_kConfigured != pC->m_state))
    {
        return M4ERR_STATE;
    }
    free(pC) ;

    return M4NO_ERROR ;

}


/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_configure(M4OSA_Context pContext, M4AIR_Params* pParams)
 * @brief   This function will configure the AIR.
 * @note    It will set the input and output coordinates and sizes,
 *          and indicates if we will proceed in stripe or not.
 *          In case a M4AIR_get in stripe mode was on going, it will cancel this previous
 *          processing and reset the get process.
 * @param    pContext:                (IN) Context identifying the instance
 * @param    pParams->m_bOutputStripe:(IN) Stripe mode.
 * @param    pParams->m_inputCoord:    (IN) X,Y coordinates of the first valid pixel in input.
 * @param    pParams->m_inputSize:    (IN) input ROI size.
 * @param    pParams->m_outputSize:    (IN) output size.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_ALLOC: No more memory space to add a new effect.
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only).
 * @return    M4ERR_AIR_FORMAT_NOT_SUPPORTED: the requested input format is not supported.
 ******************************************************************************
 */
M4OSA_ERR M4AIR_configure(M4OSA_Context pContext, M4AIR_Params* pParams)
{
    M4AIR_InternalContext* pC = (M4AIR_InternalContext*)pContext ;
    M4OSA_UInt32    i,u32_width_in, u32_width_out, u32_height_in, u32_height_out;
    M4OSA_UInt32    nb_planes;

    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    if(M4AIR_kYUV420AP == pC->m_inputFormat)
    {
        nb_planes = 4;
    }
    else
    {
        nb_planes = 3;
    }

    /**< Check state */
    if((M4AIR_kCreated != pC->m_state)&&(M4AIR_kConfigured != pC->m_state))
    {
        return M4ERR_STATE;
    }

    /** Save parameters */
    pC->m_params = *pParams;

    /* Check for the input&output width and height are even */
        if( ((pC->m_params.m_inputSize.m_height)&0x1)    ||
            ((pC->m_params.m_inputSize.m_height)&0x1))
        {
            return M4ERR_AIR_ILLEGAL_FRAME_SIZE;
        }

     if( ((pC->m_params.m_inputSize.m_width)&0x1)    ||
            ((pC->m_params.m_inputSize.m_width)&0x1))
        {
            return M4ERR_AIR_ILLEGAL_FRAME_SIZE;
        }
    if(((pC->m_params.m_inputSize.m_width) == (pC->m_params.m_outputSize.m_width))
        &&((pC->m_params.m_inputSize.m_height) == (pC->m_params.m_outputSize.m_height)))
    {
        /**< No resize in this case, we will just copy input in output */
        pC->m_bOnlyCopy = M4OSA_TRUE;
    }
    else
    {
        pC->m_bOnlyCopy = M4OSA_FALSE;

        /**< Initialize internal variables used for resize filter */
        for(i=0;i<nb_planes;i++)
        {

            u32_width_in = ((i==0)||(i==3))?pC->m_params.m_inputSize.m_width:\
                (pC->m_params.m_inputSize.m_width+1)>>1;
            u32_height_in = ((i==0)||(i==3))?pC->m_params.m_inputSize.m_height:\
                (pC->m_params.m_inputSize.m_height+1)>>1;
            u32_width_out = ((i==0)||(i==3))?pC->m_params.m_outputSize.m_width:\
                (pC->m_params.m_outputSize.m_width+1)>>1;
            u32_height_out = ((i==0)||(i==3))?pC->m_params.m_outputSize.m_height:\
                (pC->m_params.m_outputSize.m_height+1)>>1;

                /* Compute horizontal ratio between src and destination width.*/
                if (u32_width_out >= u32_width_in)
                {
                    pC->u32_x_inc[i]   = ((u32_width_in-1) * 0x10000) / (u32_width_out-1);
                }
                else
                {
                    pC->u32_x_inc[i]   = (u32_width_in * 0x10000) / (u32_width_out);
                }

                /* Compute vertical ratio between src and destination height.*/
                if (u32_height_out >= u32_height_in)
                {
                    pC->u32_y_inc[i]   = ((u32_height_in - 1) * 0x10000) / (u32_height_out-1);
                }
                else
                {
                    pC->u32_y_inc[i] = (u32_height_in * 0x10000) / (u32_height_out);
                }

                /*
                Calculate initial accumulator value : u32_y_accum_start.
                u32_y_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
                */
                if (pC->u32_y_inc[i] >= 0x10000)
                {
                    /*
                        Keep the fractionnal part, assimung that integer  part is coded
                        on the 16 high bits and the fractionnal on the 15 low bits
                    */
                    pC->u32_y_accum_start[i] = pC->u32_y_inc[i] & 0xffff;

                    if (!pC->u32_y_accum_start[i])
                    {
                        pC->u32_y_accum_start[i] = 0x10000;
                    }

                    pC->u32_y_accum_start[i] >>= 1;
                }
                else
                {
                    pC->u32_y_accum_start[i] = 0;
                }
                /**< Take into account that Y coordinate can be odd
                    in this case we have to put a 0.5 offset
                    for U and V plane as there a 2 times sub-sampled vs Y*/
                if((pC->m_params.m_inputCoord.m_y&0x1)&&((i==1)||(i==2)))
                {
                    pC->u32_y_accum_start[i] += 0x8000;
                }

                /*
                    Calculate initial accumulator value : u32_x_accum_start.
                    u32_x_accum_start is coded on 15 bits, and represents a value between
                    0 and 0.5
                */

                if (pC->u32_x_inc[i] >= 0x10000)
                {
                    pC->u32_x_accum_start[i] = pC->u32_x_inc[i] & 0xffff;

                    if (!pC->u32_x_accum_start[i])
                    {
                        pC->u32_x_accum_start[i] = 0x10000;
                    }

                    pC->u32_x_accum_start[i] >>= 1;
                }
                else
                {
                    pC->u32_x_accum_start[i] = 0;
                }
                /**< Take into account that X coordinate can be odd
                    in this case we have to put a 0.5 offset
                    for U and V plane as there a 2 times sub-sampled vs Y*/
                if((pC->m_params.m_inputCoord.m_x&0x1)&&((i==1)||(i==2)))
                {
                    pC->u32_x_accum_start[i] += 0x8000;
                }
        }
    }

    /**< Reset variable used for stripe mode */
    pC->m_procRows = 0;

    /**< Initialize var for X/Y processing order according to orientation */
    pC->m_bFlipX = M4OSA_FALSE;
    pC->m_bFlipY = M4OSA_FALSE;
    pC->m_bRevertXY = M4OSA_FALSE;
    switch(pParams->m_outputOrientation)
    {
        case M4COMMON_kOrientationTopLeft:
            break;
        case M4COMMON_kOrientationTopRight:
            pC->m_bFlipX = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationBottomRight:
            pC->m_bFlipX = M4OSA_TRUE;
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationBottomLeft:
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationLeftTop:
            pC->m_bRevertXY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationRightTop:
            pC->m_bRevertXY = M4OSA_TRUE;
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationRightBottom:
            pC->m_bRevertXY = M4OSA_TRUE;
            pC->m_bFlipX = M4OSA_TRUE;
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationLeftBottom:
            pC->m_bRevertXY = M4OSA_TRUE;
            pC->m_bFlipX = M4OSA_TRUE;
            break;
        default:
        return M4ERR_PARAMETER;
    }
    /**< Update state */
    pC->m_state = M4AIR_kConfigured;

    return M4NO_ERROR ;
}


/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_get(M4OSA_Context pContext, M4VIFI_ImagePlane* pIn, M4VIFI_ImagePlane* pOut)
 * @brief   This function will provide the requested resized area of interest according to
 *          settings  provided in M4AIR_configure.
 * @note    In case the input format type is JPEG, input plane(s)
 *          in pIn is not used. In normal mode, dimension specified in output plane(s) structure
 *          must be the same than the one specified in M4AIR_configure. In stripe mode, only the
 *          width will be the same, height will be taken as the stripe height (typically 16).
 *          In normal mode, this function is call once to get the full output picture.
 *          In stripe mode, it is called for each stripe till the whole picture has been
 *          retrieved,and  the position of the output stripe in the output picture
 *          is internally incremented at each step.
 *          Any call to M4AIR_configure during stripe process will reset this one to the
 *          beginning of the output picture.
 * @param    pContext:    (IN) Context identifying the instance
 * @param    pIn:            (IN) Plane structure containing input Plane(s).
 * @param    pOut:        (IN/OUT)  Plane structure containing output Plane(s).
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_ALLOC: No more memory space to add a new effect.
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only).
 ******************************************************************************
 */
M4OSA_ERR M4AIR_get(M4OSA_Context pContext, M4VIFI_ImagePlane* pIn, M4VIFI_ImagePlane* pOut)
{
    M4AIR_InternalContext* pC = (M4AIR_InternalContext*)pContext ;
    M4OSA_UInt32 i,j,k,u32_x_frac,u32_y_frac,u32_x_accum,u32_y_accum,u32_shift;
        M4OSA_UInt8    *pu8_data_in, *pu8_data_in_org, *pu8_data_in_tmp, *pu8_data_out;
        M4OSA_UInt8    *pu8_src_top;
        M4OSA_UInt8    *pu8_src_bottom;
    M4OSA_UInt32    u32_temp_value;
    M4OSA_Int32    i32_tmp_offset;
    M4OSA_UInt32    nb_planes;



    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    /**< Check state */
    if(M4AIR_kConfigured != pC->m_state)
    {
        return M4ERR_STATE;
    }

    if(M4AIR_kYUV420AP == pC->m_inputFormat)
    {
        nb_planes = 4;
    }
    else
    {
        nb_planes = 3;
    }

    /**< Loop on each Plane */
    for(i=0;i<nb_planes;i++)
    {

         /* Set the working pointers at the beginning of the input/output data field */

        u32_shift = ((i==0)||(i==3))?0:1; /**< Depend on Luma or Chroma */

        if((M4OSA_FALSE == pC->m_params.m_bOutputStripe)\
            ||((M4OSA_TRUE == pC->m_params.m_bOutputStripe)&&(0 == pC->m_procRows)))
        {
            /**< For input, take care about ROI */
            pu8_data_in     = pIn[i].pac_data + pIn[i].u_topleft \
                + (pC->m_params.m_inputCoord.m_x>>u32_shift)
                        + (pC->m_params.m_inputCoord.m_y >> u32_shift) * pIn[i].u_stride;

            /** Go at end of line/column in case X/Y scanning is flipped */
            if(M4OSA_TRUE == pC->m_bFlipX)
            {
                pu8_data_in += ((pC->m_params.m_inputSize.m_width)>>u32_shift) -1 ;
            }
            if(M4OSA_TRUE == pC->m_bFlipY)
            {
                pu8_data_in += ((pC->m_params.m_inputSize.m_height>>u32_shift) -1)\
                     * pIn[i].u_stride;
            }

            /**< Initialize accumulators in case we are using it (bilinear interpolation) */
            if( M4OSA_FALSE == pC->m_bOnlyCopy)
            {
                pC->u32_x_accum[i] = pC->u32_x_accum_start[i];
                pC->u32_y_accum[i] = pC->u32_y_accum_start[i];
            }

        }
        else
        {
            /**< In case of stripe mode for other than first stripe, we need to recover input
                 pointer from internal context */
            pu8_data_in = pC->pu8_data_in[i];
        }

        /**< In every mode, output data are at the beginning of the output plane */
        pu8_data_out    = pOut[i].pac_data + pOut[i].u_topleft;

        /**< Initialize input offset applied after each pixel */
        if(M4OSA_FALSE == pC->m_bFlipY)
        {
            i32_tmp_offset = pIn[i].u_stride;
        }
        else
        {
            i32_tmp_offset = -pIn[i].u_stride;
        }

        /**< In this case, no bilinear interpolation is needed as input and output dimensions
            are the same */
        if( M4OSA_TRUE == pC->m_bOnlyCopy)
        {
            /**< No +-90° rotation */
            if(M4OSA_FALSE == pC->m_bRevertXY)
            {
                /**< No flip on X abscissa */
                if(M4OSA_FALSE == pC->m_bFlipX)
                {
                    /**< Loop on each row */
                    for(j=0;j<pOut[i].u_height;j++)
                    {
                        /**< Copy one whole line */
                        memcpy((void *)pu8_data_out, (void *)pu8_data_in,
                             pOut[i].u_width);

                        /**< Update pointers */
                        pu8_data_out += pOut[i].u_stride;
                        if(M4OSA_FALSE == pC->m_bFlipY)
                        {
                            pu8_data_in += pIn[i].u_stride;
                        }
                        else
                        {
                            pu8_data_in -= pIn[i].u_stride;
                        }
                    }
                }
                else
                {
                    /**< Loop on each row */
                    for(j=0;j<pOut[i].u_height;j++)
                    {
                        /**< Loop on each pixel of 1 row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {
                            *pu8_data_out++ = *pu8_data_in--;
                        }

                        /**< Update pointers */
                        pu8_data_out += (pOut[i].u_stride - pOut[i].u_width);

                        pu8_data_in += pOut[i].u_width + i32_tmp_offset;

                    }
                }
            }
            /**< Here we have a +-90° rotation */
            else
            {

                /**< Loop on each row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    pu8_data_in_tmp = pu8_data_in;

                    /**< Loop on each pixel of 1 row */
                    for(k=0;k<pOut[i].u_width;k++)
                    {
                        *pu8_data_out++ = *pu8_data_in_tmp;

                        /**< Update input pointer in order to go to next/past line */
                        pu8_data_in_tmp += i32_tmp_offset;
                    }

                    /**< Update pointers */
                    pu8_data_out += (pOut[i].u_stride - pOut[i].u_width);
                    if(M4OSA_FALSE == pC->m_bFlipX)
                    {
                        pu8_data_in ++;
                    }
                    else
                    {
                        pu8_data_in --;
                    }
                }
            }
        }
        /**< Bilinear interpolation */
        else
        {

        if(3 != i)    /**< other than alpha plane */
        {
            /**No +-90° rotation */
            if(M4OSA_FALSE == pC->m_bRevertXY)
            {

                /**< Loop on each row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* Vertical weight factor */
                    u32_y_frac = (pC->u32_y_accum[i]>>12)&15;

                    /* Reinit horizontal weight factor */
                    u32_x_accum = pC->u32_x_accum_start[i];



                        if(M4OSA_TRUE ==  pC->m_bFlipX)
                        {

                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {

                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal
                                                                        weight factor */

                                pu8_src_top = (pu8_data_in - (u32_x_accum >> 16)) -1 ;

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                   pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                   (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                   pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }
                        }

                        else
                        {
                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {
                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal
                                                                        weight factor */

                                pu8_src_top = pu8_data_in + (u32_x_accum >> 16);

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                   pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                   (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                   pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                                    *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }

                        }

                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update vertical accumulator */
                    pC->u32_y_accum[i] += pC->u32_y_inc[i];
                      if (pC->u32_y_accum[i]>>16)
                    {
                        pu8_data_in = pu8_data_in + (pC->u32_y_accum[i] >> 16) * i32_tmp_offset;
                          pC->u32_y_accum[i] &= 0xffff;
                       }
                }
        }
            /** +-90° rotation */
            else
            {
                pu8_data_in_org = pu8_data_in;

                /**< Loop on each output row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* horizontal weight factor */
                    u32_x_frac = (pC->u32_x_accum[i]>>12)&15;

                    /* Reinit accumulator */
                    u32_y_accum = pC->u32_y_accum_start[i];

                    if(M4OSA_TRUE ==  pC->m_bFlipX)
                    {

                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */


                            pu8_src_top = (pu8_data_in - (pC->u32_x_accum[i] >> 16)) - 1;

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                 pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                 pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                              if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                  u32_y_accum &= 0xffff;
                               }

                        }
                    }
                    else
                    {
                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */

                            pu8_src_top = pu8_data_in + (pC->u32_x_accum[i] >> 16);

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                              if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                  u32_y_accum &= 0xffff;
                               }
                        }
                    }
                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update horizontal accumulator */
                    pC->u32_x_accum[i] += pC->u32_x_inc[i];

                    pu8_data_in = pu8_data_in_org;
                }

            }
            }/** 3 != i */
            else
            {
            /**No +-90° rotation */
            if(M4OSA_FALSE == pC->m_bRevertXY)
            {

                /**< Loop on each row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* Vertical weight factor */
                    u32_y_frac = (pC->u32_y_accum[i]>>12)&15;

                    /* Reinit horizontal weight factor */
                    u32_x_accum = pC->u32_x_accum_start[i];



                        if(M4OSA_TRUE ==  pC->m_bFlipX)
                        {

                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {

                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal
                                                                         weight factor */

                                pu8_src_top = (pu8_data_in - (u32_x_accum >> 16)) -1 ;

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                   pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                  (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                   pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                                u32_temp_value= (u32_temp_value >> 7)*0xff;

                                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }
                        }

                        else
                        {
                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {
                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal
                                                                        weight factor */

                                pu8_src_top = pu8_data_in + (u32_x_accum >> 16);

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                   pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                   (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                   pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                                u32_temp_value= (u32_temp_value >> 7)*0xff;

                                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }

                        }

                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update vertical accumulator */
                    pC->u32_y_accum[i] += pC->u32_y_inc[i];
                      if (pC->u32_y_accum[i]>>16)
                    {
                        pu8_data_in = pu8_data_in + (pC->u32_y_accum[i] >> 16) * i32_tmp_offset;
                          pC->u32_y_accum[i] &= 0xffff;
                       }
                }

            } /**< M4OSA_FALSE == pC->m_bRevertXY */
            /** +-90° rotation */
            else
            {
                pu8_data_in_org = pu8_data_in;

                /**< Loop on each output row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* horizontal weight factor */
                    u32_x_frac = (pC->u32_x_accum[i]>>12)&15;

                    /* Reinit accumulator */
                    u32_y_accum = pC->u32_y_accum_start[i];

                    if(M4OSA_TRUE ==  pC->m_bFlipX)
                    {

                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */


                            pu8_src_top = (pu8_data_in - (pC->u32_x_accum[i] >> 16)) - 1;

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                 pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                 pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                            u32_temp_value= (u32_temp_value >> 7)*0xff;

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                              if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                  u32_y_accum &= 0xffff;
                               }

                        }
                    }
                    else
                    {
                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */

                            pu8_src_top = pu8_data_in + (pC->u32_x_accum[i] >> 16);

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                            u32_temp_value= (u32_temp_value >> 7)*0xff;

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                              if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                  u32_y_accum &= 0xffff;
                               }
                        }
                    }
                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update horizontal accumulator */
                    pC->u32_x_accum[i] += pC->u32_x_inc[i];

                    pu8_data_in = pu8_data_in_org;

                }
                } /**< M4OSA_TRUE == pC->m_bRevertXY */
        }/** 3 == i */
            }
        /**< In case of stripe mode, save current input pointer */
        if(M4OSA_TRUE == pC->m_params.m_bOutputStripe)
        {
            pC->pu8_data_in[i] = pu8_data_in;
        }
    }

    /**< Update number of processed rows, reset it if we have finished
         with the whole processing */
    pC->m_procRows += pOut[0].u_height;
    if(M4OSA_FALSE == pC->m_bRevertXY)
    {
        if(pC->m_params.m_outputSize.m_height <= pC->m_procRows)    pC->m_procRows = 0;
    }
    else
    {
        if(pC->m_params.m_outputSize.m_width <= pC->m_procRows)    pC->m_procRows = 0;
    }

    return M4NO_ERROR ;

}



