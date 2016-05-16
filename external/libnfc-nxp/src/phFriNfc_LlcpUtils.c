/*
 * Copyright (C) 2010 NXP Semiconductors
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
 * \file  phFriNfc_Llcp.c
 * \brief NFC LLCP core
 *
 * Project: NFC-FRI
 *
 */

/*include files*/
#include <phNfcTypes.h>
#include <phNfcHalTypes.h>
#include <phLibNfcStatus.h>
#include <phFriNfc_LlcpUtils.h>
#include <phFriNfc_Llcp.h>

NFCSTATUS phFriNfc_Llcp_DecodeTLV( phNfc_sData_t  *psRawData,
                                   uint32_t       *pOffset,
                                   uint8_t        *pType,
                                   phNfc_sData_t  *psValueBuffer )
{
   uint8_t type;
   uint8_t length;
   uint32_t offset = *pOffset;

   /* Check for NULL pointers */
   if ((psRawData == NULL) || (pOffset == NULL) || (pType == NULL) || (psValueBuffer == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check offset */
   if (offset > psRawData->length)
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check if enough room for Type and Length (with overflow check) */
   if ((offset+2 > psRawData->length) && (offset+2 > offset))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Get Type and Length from current TLV, and update offset */
   type = psRawData->buffer[offset];
   length = psRawData->buffer[offset+1];
   offset += 2;

   /* Check if enough room for Value with announced Length (with overflow check) */
   if ((offset+length > psRawData->length) && (offset+length > offset))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Save response, and update offset */
   *pType = type;
   psValueBuffer->buffer = psRawData->buffer + offset;
   psValueBuffer->length = length;
   offset += length;

   /* Save updated offset */
   *pOffset = offset;

   return NFCSTATUS_SUCCESS;
}

NFCSTATUS phFriNfc_Llcp_EncodeTLV( phNfc_sData_t  *psValueBuffer,
                                   uint32_t       *pOffset,
                                   uint8_t        type,
                                   uint8_t        length,
                                   uint8_t        *pValue)
{
   uint32_t offset = *pOffset;
   uint32_t finalOffset = offset + 2 + length; /* 2 stands for Type and Length fields size */
   uint8_t i;

   /* Check for NULL pointers */
   if ((psValueBuffer == NULL) || (pOffset == NULL) || (pValue == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check offset */
   if (offset > psValueBuffer->length)
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check if enough room for Type, Length and Value (with overflow check) */
   if ((finalOffset > psValueBuffer->length) || (finalOffset < offset))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Set the TYPE */
   psValueBuffer->buffer[offset] = type;
   offset += 1;

   /* Set the LENGTH */
   psValueBuffer->buffer[offset] = length;
   offset += 1;

   /* Set the VALUE */
   for(i=0;i<length;i++,offset++)
   {
      psValueBuffer->buffer[offset]  = pValue[i];
   }

   /* Save updated offset */
   *pOffset = offset;

   return NFCSTATUS_SUCCESS;
}

NFCSTATUS phFriNfc_Llcp_AppendTLV( phNfc_sData_t  *psValueBuffer,
                                   uint32_t       nTlvOffset,
                                   uint32_t       *pCurrentOffset,
                                   uint8_t        length,
                                   uint8_t        *pValue)
{
   uint32_t offset = *pCurrentOffset;
   uint32_t finalOffset = offset + length;

   /* Check for NULL pointers */
   if ((psValueBuffer == NULL) || (pCurrentOffset == NULL) || (pValue == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check offset */
   if (offset > psValueBuffer->length)
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check if enough room for Type and Length (with overflow check) */
   if ((finalOffset > psValueBuffer->length) || (finalOffset < offset))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Update the LENGTH */
   psValueBuffer->buffer[nTlvOffset+1] += length;

   /* Set the VALUE */
   memcpy(psValueBuffer->buffer + offset, pValue, length);
   offset += length;

   /* Save updated offset */
   *pCurrentOffset = offset;

   return NFCSTATUS_SUCCESS;
}


/* TODO: comment function EncodeMIUX */
void phFriNfc_Llcp_EncodeMIUX(uint16_t miux,
                              uint8_t* pMiuxEncoded)
{
   /* MASK */
   miux = miux & PHFRINFC_LLCP_TLV_MIUX_MASK;

   pMiuxEncoded[0] = miux >> 8;
   pMiuxEncoded[1] = miux & 0xff;
}

/* TODO: comment function EncodeRW */
void phFriNfc_Llcp_EncodeRW(uint8_t *pRw)
{
   /* MASK */
   *pRw = *pRw & PHFRINFC_LLCP_TLV_RW_MASK;
}

/**
 * Initializes a Fifo Cyclic Buffer to point to some allocated memory.
 */
void phFriNfc_Llcp_CyclicFifoInit(P_UTIL_FIFO_BUFFER   pUtilFifo,
                                  const uint8_t        *pBuffStart,
                                  uint32_t             buffLength)
{
   pUtilFifo->pBuffStart = (uint8_t *)pBuffStart;
   pUtilFifo->pBuffEnd   = (uint8_t *)pBuffStart + buffLength-1;
   pUtilFifo->pIn        = (uint8_t *)pBuffStart;
   pUtilFifo->pOut       = (uint8_t *)pBuffStart;
   pUtilFifo->bFull      = FALSE;
}

/**
 * Clears the Fifo Cyclic Buffer - loosing any data that was in it.
 */
void phFriNfc_Llcp_CyclicFifoClear(P_UTIL_FIFO_BUFFER pUtilFifo)
{
   pUtilFifo->pIn = pUtilFifo->pBuffStart;
   pUtilFifo->pOut = pUtilFifo->pBuffStart;
   pUtilFifo->bFull      = FALSE;
}

/**
 * Attempts to write dataLength bytes to the specified Fifo Cyclic Buffer.
 */
uint32_t phFriNfc_Llcp_CyclicFifoWrite(P_UTIL_FIFO_BUFFER   pUtilFifo,
                                       uint8_t              *pData,
                                       uint32_t             dataLength)
{
   uint32_t dataLengthWritten = 0;
   uint8_t * pNext;

   while(dataLengthWritten < dataLength)
   {
      pNext = (uint8_t*)pUtilFifo->pIn+1;

      if(pNext > pUtilFifo->pBuffEnd)
      {
         //Wrap around
         pNext = pUtilFifo->pBuffStart;
      }

      if(pUtilFifo->bFull)
      {
         //Full
         break;
      }

      if(pNext == pUtilFifo->pOut)
      {
         // Trigger Full flag
         pUtilFifo->bFull = TRUE;
      }

      dataLengthWritten++;
      *pNext = *pData++;
      pUtilFifo->pIn = pNext;
   }

   return dataLengthWritten;
}

/**
 * Attempts to read dataLength bytes from the specified  Fifo Cyclic Buffer.
 */
uint32_t phFriNfc_Llcp_CyclicFifoFifoRead(P_UTIL_FIFO_BUFFER   pUtilFifo,
                                          uint8_t              *pBuffer,
                                          uint32_t             dataLength)
{
   uint32_t  dataLengthRead = 0;
   uint8_t * pNext;

   while(dataLengthRead < dataLength)
   {
      if((pUtilFifo->pOut == pUtilFifo->pIn) && (pUtilFifo->bFull == FALSE))
      {
         //No more bytes in buffer
         break;
      }
      else
      {
         dataLengthRead++;

         if(pUtilFifo->pOut == pUtilFifo->pBuffEnd)
         {
            /* Wrap around */
            pNext = pUtilFifo->pBuffStart;
         }
         else
         {
            pNext = (uint8_t*)pUtilFifo->pOut + 1;
         }

         *pBuffer++ = *pNext;

         pUtilFifo->pOut = pNext;

         pUtilFifo->bFull = FALSE;
      }
   }

   return dataLengthRead;
}

/**
 * Returns the number of bytes currently stored in Fifo Cyclic Buffer.
 */
uint32_t phFriNfc_Llcp_CyclicFifoUsage(P_UTIL_FIFO_BUFFER pUtilFifo)
{
   uint32_t dataLength;
   uint8_t * pIn        = (uint8_t *)pUtilFifo->pIn;
   uint8_t * pOut       = (uint8_t *)pUtilFifo->pOut;

   if (pUtilFifo->bFull)
   {
      dataLength = pUtilFifo->pBuffEnd - pUtilFifo->pBuffStart + 1;
   }
   else
   {
      if(pIn >= pOut)
      {
         dataLength = pIn - pOut;
      }
      else
      {
         dataLength = pUtilFifo->pBuffEnd - pOut;
         dataLength += (pIn+1) - pUtilFifo->pBuffStart;
      }
   }

   return dataLength;
}


/**
 * Returns the available room for writing in Fifo Cyclic Buffer.
 */
uint32_t phFriNfc_Llcp_CyclicFifoAvailable(P_UTIL_FIFO_BUFFER pUtilFifo)
{
   uint32_t dataLength;
   uint32_t  size;
   uint8_t * pIn         = (uint8_t *)pUtilFifo->pIn;
   uint8_t * pOut        = (uint8_t *)pUtilFifo->pOut;

   if (pUtilFifo->bFull)
   {
      dataLength = 0;
   }
   else
   {
      if(pIn >= pOut)
      {
         size = pUtilFifo->pBuffEnd - pUtilFifo->pBuffStart + 1;
         dataLength = size - (pIn - pOut);
      }
      else
      {
         dataLength = pOut - pIn;
      }
   }

   return dataLength;
}



uint32_t phFriNfc_Llcp_Header2Buffer( phFriNfc_Llcp_sPacketHeader_t *psHeader, uint8_t *pBuffer, uint32_t nOffset )
{
   uint32_t nOriginalOffset = nOffset;
   pBuffer[nOffset++] = (uint8_t)((psHeader->dsap << 2)  | (psHeader->ptype >> 2));
   pBuffer[nOffset++] = (uint8_t)((psHeader->ptype << 6) | psHeader->ssap);
   return nOffset - nOriginalOffset;
}

uint32_t phFriNfc_Llcp_Sequence2Buffer( phFriNfc_Llcp_sPacketSequence_t *psSequence, uint8_t *pBuffer, uint32_t nOffset )
{
   uint32_t nOriginalOffset = nOffset;
   pBuffer[nOffset++] = (uint8_t)((psSequence->ns << 4) | (psSequence->nr));
   return nOffset - nOriginalOffset;
}

uint32_t phFriNfc_Llcp_Buffer2Header( uint8_t *pBuffer, uint32_t nOffset, phFriNfc_Llcp_sPacketHeader_t *psHeader )
{
   psHeader->dsap  = (pBuffer[nOffset] & 0xFC) >> 2;
   psHeader->ptype = ((pBuffer[nOffset]  & 0x03) << 2) | ((pBuffer[nOffset+1] & 0xC0) >> 6);
   psHeader->ssap  = pBuffer[nOffset+1] & 0x3F;
   return PHFRINFC_LLCP_PACKET_HEADER_SIZE;
}

uint32_t phFriNfc_Llcp_Buffer2Sequence( uint8_t *pBuffer, uint32_t nOffset, phFriNfc_Llcp_sPacketSequence_t *psSequence )
{
   psSequence->ns = pBuffer[nOffset] >> 4;
   psSequence->nr = pBuffer[nOffset] & 0x0F;
   return PHFRINFC_LLCP_PACKET_SEQUENCE_SIZE;
}


