/*---------------------------------------------------------------------------*
 *  CircularBuffer.h  *
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

#ifndef CIRCULARBUFFER_H
#define CIRCULARBUFFER_H



/*
 * This is implemented as a set of macros rather than functions with proper
 * checking.  The reasons for doing so is that this is a non-public API that is
 * used in the audio delivery component and we want it to be as fast as
 * possible.
 */

#include "ESR_SharedPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup CircularBufferModule CircularBuffer API functions
 * Generic Circular Buffer implementation.
 *
 * @{
 */

/**
 * A circular buffer.
 *
 * @see list of functions used to operate on @ref CircularBufferModule "CircularBuffer" objects
 */
typedef struct CircularBuffer_t
{
  /**
   * Total buffer capacity.
   */
  size_t capacity;
  
  /**
   * Amount of data in buffer.
   */
  size_t size;
  
  /**
   * Write index.
   */
  size_t writeIdx;
  
  /**
   * Read index.
   */
  size_t readIdx;
  
}
CircularBuffer;

/**
 * Creates a circular buffer of the specified capacity.
 *
 * @param capacity the capacity in number of bytes of the data buffer.
 * @param mtag MALLOC allocation tag
 * @param buffer The circular buffer to initialize.
 */
ESR_SHARED_API ESR_ReturnCode CircularBufferCreate(size_t capacity, const LCHAR* mtag, CircularBuffer** buffer);

/**
 * Returns the capacity of the buffer.
 */
#define CircularBufferGetCapacity(buffer) ((buffer)->capacity + 0)

/**
 * Returns the current size (number of bytes) in the buffer.
 */
#define CircularBufferGetSize(buffer) ((buffer)->size + 0)

/**
 * Returns whether buffer is empty or not.
 */
#define CircularBufferIsEmpty(buffer) ((buffer)->size == 0)

/**
 * Returns whether buffer is full or not.
 **/
#define CircularBufferIsFull(buffer) ((buffer)->size == (buffer)->capacity)

/**
 * Resets the buffer to the empty state.
 */
#define CircularBufferReset(buffer) ((buffer)->size = \
                                     (buffer)->readIdx = \
                                                         (buffer)->writeIdx = 0)

/**
 * Determines the residual capacity of the circular buffer.
 */
#define CircularBufferGetAvailable(buffer) ((buffer)->capacity - (buffer)->size)

/**
 * Reads requested number of bytes from the circular buffer.
 *
 * @param buffer The circular buffer to read from.
 * @param data  Pointer to where to store read bytes.
 * @param bufSize The number of bytes to read from the circular buffer.
 *
 * @return the number of bytes that were read.  A negative value indicates an
 * error, while a value less than bufSize indicates that end-of-buffer is
 * reached.
 */
ESR_SHARED_API int CircularBufferRead(CircularBuffer* buffer, void* data, size_t bufSize);

/**
 * Skips requested number of bytes from the circular buffer.
 *
 * @param buffer The circular buffer to skip from.
 * @param bufSize The number of bytes to skip from the circular buffer.
 *
 * @return the number of bytes that were skipped.  A negative value indicates an
 * error, while a value less than bufSize indicates that end-of-buffer is
 * reached.
 **/
ESR_SHARED_API int CircularBufferSkip(CircularBuffer* buffer, size_t bufSize);

/**
 * Writes requested number of bytes from the circular buffer.
 *
 * @param buffer The circular buffer to write to
 * @param data  Pointer to data to write.
 * @param bufSize The number of bytes to write into the circular buffer.
 *
 * @return the number of bytes that were written.  A negative value indicates
 * an error, while a value less than bufSize indicates that buffer capacity is
 * reached.
 */
ESR_SHARED_API int CircularBufferWrite(CircularBuffer* buffer, const void* data, size_t bufSize);

/**
 * Removes the requested number of bytes from the end of the circular buffer.
 *
 * @param buffer The circular buffer to write to
 * @param amoun tThe number of bytes to remove from end of circular buffer.
 *
 * @return the number of bytes that were unwritten. A negative value indicates
 *         an error.
 */
ESR_SHARED_API int CircularBufferUnwrite(CircularBuffer* buffer, size_t amount);

/**
 * @}
 */


#endif 
