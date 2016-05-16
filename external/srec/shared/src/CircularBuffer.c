/*---------------------------------------------------------------------------*
 *  CircularBuffer.c  *
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



#ifdef _WIN32
#if _MSC_VER >= 1100    // Visual C++ 5.x
#pragma warning( disable : 4786 4503 )
#endif
#endif

#include "CircularBuffer.h"
#include "pmemory.h"
#ifndef __vxworks
#include <memory.h>
#endif

ESR_ReturnCode CircularBufferCreate(size_t capacity, const LCHAR* mtag, CircularBuffer** buffer)
{
  CircularBuffer* Interface;
  if (buffer == NULL || capacity <= 0)
    return ESR_INVALID_ARGUMENT;
    
  Interface = (CircularBuffer *) MALLOC(sizeof(CircularBuffer) + capacity, mtag);
  if (Interface == NULL)
    return ESR_OUT_OF_MEMORY;
  Interface->capacity = capacity;
  CircularBufferReset(Interface);
  *buffer = Interface;
  return ESR_SUCCESS;
}


int CircularBufferRead(CircularBuffer* buffer, void* data, size_t bufSize)
{
  size_t nbRead = 0;
  unsigned char *bufferData = NULL;
  
  if (buffer == NULL || (data == NULL && bufSize > 0))
    return -1;
    
  if (buffer->size < bufSize)
    bufSize = buffer->size;
    
  if (bufSize == 0)
    return 0;
    
  bufferData = ((unsigned char *) buffer) + sizeof(CircularBuffer);
  
  if (buffer->readIdx >= buffer->writeIdx)
  {
    nbRead = buffer->capacity - buffer-> readIdx;
    if (nbRead > bufSize) nbRead = bufSize;
    
    memcpy(data, bufferData + buffer->readIdx, nbRead);
    buffer->size -= nbRead;
    buffer->readIdx += nbRead;
    if (buffer->readIdx == buffer->capacity)
      buffer->readIdx = 0;
  }
  
  if (nbRead < bufSize)
  {
    int toRead = bufSize - nbRead;
    memcpy(((unsigned char *) data) + nbRead, bufferData + buffer->readIdx, toRead);
    buffer->size -= toRead;
    buffer->readIdx += toRead;
  }
  
  return bufSize;
}

int CircularBufferSkip(CircularBuffer* buffer, size_t bufSize)
{
  if ( buffer == NULL )
    return -1;
    
  if (buffer->size < bufSize)
    bufSize = buffer->size;
    
  if (bufSize == 0)
    return 0;
    
  buffer->readIdx += bufSize;
  if (buffer->readIdx >= buffer->capacity)
    buffer->readIdx -= buffer->capacity;
    
  buffer->size -= bufSize;
  
  return bufSize;
}

int CircularBufferWrite(CircularBuffer* buffer, const void *data, size_t bufSize)
{
  size_t nbWritten = 0;
  unsigned char *bufferData;
  size_t available = buffer->capacity - buffer->size;
  
  if (data == NULL && bufSize > 0)
    return -1;
    
  if (available < bufSize)	/* We need to force an error to be logged here */
    return -1;
/*    bufSize = available;	Throwing data on the floor with no notice is asking for trouble */
    
  if (bufSize == 0)
    return 0;
    
  bufferData = ((unsigned char*) buffer) + sizeof(CircularBuffer);
  
  if (buffer->writeIdx >= buffer->readIdx)
  {
    nbWritten = buffer->capacity - buffer->writeIdx;
    if (nbWritten > bufSize) nbWritten = bufSize;
    memcpy(bufferData + buffer->writeIdx, data, nbWritten);
    buffer->size += nbWritten;
    buffer->writeIdx += nbWritten;
    if (buffer->writeIdx == buffer->capacity)
      buffer->writeIdx = 0;
  }
  
  if (nbWritten < bufSize)
  {
    size_t toWrite = bufSize - nbWritten;
    memcpy(bufferData + buffer->writeIdx, ((unsigned char*) data) + nbWritten, toWrite);
    buffer->size += toWrite;
    buffer->writeIdx += toWrite;
  }
  
  return bufSize;
}

int CircularBufferUnwrite(CircularBuffer* buffer, size_t amount)
{
  size_t available = buffer->capacity - buffer->size;
  
  if (available < amount)
    amount = available;
  buffer->size -= amount;
  return amount;
}
