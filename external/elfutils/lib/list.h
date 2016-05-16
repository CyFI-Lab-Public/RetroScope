/* Copyright (C) 2001, 2002, 2003 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2001.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifndef LIST_H
#define LIST_H	1

/* Add element to the end of a circular, double-linked list.  */
#define CDBL_LIST_ADD_REAR(first, newp) \
  do {									      \
    __typeof (newp) _newp = (newp);					      \
    assert (_newp->next == NULL);					      \
    assert (_newp->previous == NULL);					      \
    if (unlikely ((first) == NULL))					      \
      (first) = _newp->next = _newp->previous = _newp;			      \
    else								      \
      {									      \
	_newp->next = (first);						      \
	_newp->previous = (first)->previous;				      \
	_newp->previous->next = _newp->next->previous = _newp;		      \
      }									      \
  } while (0)

/* Remove element from circular, double-linked list.  */
#define CDBL_LIST_DEL(first, elem) \
  do {									      \
    __typeof (elem) _elem = (elem);					      \
    /* Check whether the element is indeed on the list.  */		      \
    assert (first != NULL && _elem != NULL				      \
	    && (first != elem						      \
		|| ({ __typeof (elem) _runp = first->next;		      \
		      while (_runp != first)				      \
			if (_runp == _elem)				      \
			  break;					      \
			else						      \
		          _runp = _runp->next;				      \
		      _runp == _elem; })));				      \
    if (unlikely (_elem->next == _elem))				      \
      first = NULL;							      \
    else								      \
      {									      \
	_elem->next->previous = _elem->previous;			      \
	_elem->previous->next = _elem->next;				      \
	if (unlikely (first == _elem))					      \
	  first = _elem->next;						      \
      }									      \
     assert ((_elem->next = _elem->previous = NULL, 1));		      \
  } while (0)


/* Add element to the front of a single-linked list.  */
#define SNGL_LIST_PUSH(first, newp) \
  do {									      \
    __typeof (newp) _newp = (newp);					      \
    assert (_newp->next == NULL);					      \
    _newp->next = first;						      \
    first = _newp;							      \
  } while (0)


/* Add element to the rear of a circular single-linked list.  */
#define CSNGL_LIST_ADD_REAR(first, newp) \
  do {									      \
    __typeof (newp) _newp = (newp);					      \
    assert (_newp->next == NULL);					      \
    if (unlikely ((first) == NULL))					      \
      (first) = _newp->next = _newp;					      \
    else								      \
      {									      \
	_newp->next = (first)->next;					      \
	(first) = (first)->next = _newp;				      \
      }									      \
  } while (0)


#endif	/* list.h */
