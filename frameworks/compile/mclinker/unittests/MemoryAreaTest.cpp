//===- MemoryAreaTest.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/FileSystem.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MemoryAreaFactory.h>
#include <mcld/Support/Path.h>

#include "MemoryAreaTest.h"
#include <fcntl.h>
#include <cstdio>

using namespace mcld;
using namespace mcld::sys::fs;
using namespace mcldtest;


// Constructor can do set-up work for all test here.
MemoryAreaTest::MemoryAreaTest()
{
}

// Destructor can do clean-up work that doesn't throw exceptions here.
MemoryAreaTest::~MemoryAreaTest()
{
}

// SetUp() will be called immediately before each test.
void MemoryAreaTest::SetUp()
{
}

// TearDown() will be called immediately after each test.
void MemoryAreaTest::TearDown()
{
}

//==========================================================================//
// Testcases
//
TEST_F( MemoryAreaTest, read_by_malloc )
{
	Path path(TOPDIR);
	path.append("unittests/test3.txt");

	MemoryAreaFactory *AreaFactory = new MemoryAreaFactory(1);
	MemoryArea* area = AreaFactory->produce(path, FileHandle::ReadOnly);
	MemoryRegion* region = area->request(3, 2);
	ASSERT_EQ('L', region->getBuffer()[0]);
	ASSERT_EQ('O', region->getBuffer()[1]);
	area->release(region);
	AreaFactory->destruct(area);
	//delete AreaFactory;;
}

TEST_F( MemoryAreaTest, write_by_malloc )
{
	Path path(TOPDIR);
	path.append("unittests/test2.txt");
	MemoryAreaFactory *AreaFactory = new MemoryAreaFactory(1);
	MemoryArea* area = AreaFactory->produce(path, FileHandle::ReadOnly);
        ASSERT_TRUE(area->handler()->isOpened());
        ASSERT_TRUE(area->handler()->isGood());
	MemoryRegion* region = area->request(3, 4);
	region->getBuffer()[0] = 'L';
	region->getBuffer()[1] = 'i';
	region->getBuffer()[2] = 'n';
	region->getBuffer()[3] = 'k';
	area->release(region);
	area->clear();
	area->handler()->close();

        area->handler()->open(path, FileHandle::ReadOnly);
        ASSERT_TRUE(area->handler()->isOpened());
        ASSERT_TRUE(area->handler()->isGood());
        region = area->request(5, 2);
        ASSERT_EQ('n', region->getBuffer()[0]);
        ASSERT_EQ('k', region->getBuffer()[1]);
	area->release(region);
	AreaFactory->destruct(area);
	//delete AreaFactory;;
}

TEST_F( MemoryAreaTest, read_one_page )
{
        Path path(TOPDIR) ;
	path.append("unittests/test3.txt") ;
	MemoryAreaFactory *AreaFactory = new MemoryAreaFactory(1) ;
	MemoryArea* area = AreaFactory->produce(path, FileHandle::ReadOnly) ;
	ASSERT_TRUE(area->handler()->isOpened()) ;
	ASSERT_TRUE(area->handler()->isGood()) ;
	MemoryRegion* region = area->request(0, 4096);
	ASSERT_EQ('H', region->getBuffer()[0]);
	ASSERT_EQ('E', region->getBuffer()[1]);
	area->release(region);
	AreaFactory->destruct(area);
	//delete AreaFactory; ;
}

TEST_F( MemoryAreaTest, write_one_page )
{
        Path path(TOPDIR) ;
	path.append("unittests/test2.txt") ;
        MemoryAreaFactory *AreaFactory = new MemoryAreaFactory(1) ;
	MemoryArea* area = AreaFactory->produce(path, FileHandle::ReadWrite);
	ASSERT_TRUE(area->handler()->isOpened()) ;
	ASSERT_TRUE(area->handler()->isGood()) ;
	MemoryRegion* region = area->request(0, 4096) ;
	region->getBuffer()[4000] = 'K' ;
	region->getBuffer()[4001] = 'R' ;
	ASSERT_EQ('K', region->getBuffer()[4000]);
	ASSERT_EQ('R', region->getBuffer()[4001]);
	area->release(region);
	area->clear() ;
	area->handler()->close();

	area->handler()->open(path, FileHandle::ReadOnly);
	region = area->request(4000, 4);
	ASSERT_EQ('K', region->getBuffer()[0]);
	ASSERT_EQ('R', region->getBuffer()[1]);
	region->getBuffer()[0] = 'O' ;
	region->getBuffer()[1] = 'H' ;
        area->clear() ;
	AreaFactory->destruct(area);
	//delete AreaFactory; ;
}

TEST_F( MemoryAreaTest, write_sync )
{
        Path path(TOPDIR) ;
	path.append("unittests/test2.txt") ;
	MemoryAreaFactory *AreaFactory = new MemoryAreaFactory(1) ;
	MemoryArea* area = AreaFactory->produce(path, FileHandle::ReadWrite) ;
	ASSERT_TRUE(area->handler()->isOpened()) ;
	ASSERT_TRUE(area->handler()->isGood()) ;
	MemoryRegion* region1 = area->request(0, 4096) ;
	MemoryRegion* region2 = area->request(512, 1024) ;
	region1->getBuffer()[1000] = 'L' ;
	region1->getBuffer()[1001] = 'L' ;
	region2->getBuffer()[488] = 'V' ;
	region2->getBuffer()[489] = 'M' ;
	area->release(region1);
	area->release(region2);
	area->clear();
	area->handler()->close();
	area->handler()->open(path, FileHandle::ReadWrite);
	region1 = area->request(0, 1024) ;
	EXPECT_EQ('V', region1->getBuffer()[1000]) ;
	EXPECT_EQ('M', region1->getBuffer()[1001]) ;
	region1->getBuffer()[1000] = '@' ;
	region1->getBuffer()[1001] = '@' ;
	area->release(region1);
	area->clear();
	AreaFactory->destruct(area);
	//delete AreaFactory; ;
}


