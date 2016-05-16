//===- MemoryAreaTest.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MEMORYAREA_TEST_H
#define MEMORYAREA_TEST_H

#include <gtest.h>

namespace mcld
{
class MemoryArea;

} // namespace for mcld

namespace mcldtest
{

/** \class MemoryAreaTest
 *  \brief 
 *
 *  \see MemoryArea 
 */
class MemoryAreaTest : public ::testing::Test
{
public:
	// Constructor can do set-up work for all test here.
	MemoryAreaTest();

	// Destructor can do clean-up work that doesn't throw exceptions here.
	virtual ~MemoryAreaTest();

	// SetUp() will be called immediately before each test.
	virtual void SetUp();

	// TearDown() will be called immediately after each test.
	virtual void TearDown();

protected:
	mcld::MemoryArea* m_pTestee;
};

} // namespace of mcldtest

#endif

