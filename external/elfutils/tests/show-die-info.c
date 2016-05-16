/* Copyright (C) 1998, 1999, 2000, 2001, 2002, 2004, 2006 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 1998.

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

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#include <config.h>

#include <dwarf.h>
#include <inttypes.h>
#include <libelf.h>
#include ELFUTILS_HEADER(dw)
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>


static const char *tagnames[] =
{
  [DW_TAG_array_type] = "DW_TAG_array_type",
  [DW_TAG_class_type] = "DW_TAG_class_type",
  [DW_TAG_entry_point] = "DW_TAG_entry_point",
  [DW_TAG_enumeration_type] = "DW_TAG_enumeration_type",
  [DW_TAG_formal_parameter] = "DW_TAG_formal_parameter",
  [DW_TAG_imported_declaration] = "DW_TAG_imported_declaration",
  [DW_TAG_label] = "DW_TAG_label",
  [DW_TAG_lexical_block] = "DW_TAG_lexical_block",
  [DW_TAG_member] = "DW_TAG_member",
  [DW_TAG_pointer_type] = "DW_TAG_pointer_type",
  [DW_TAG_reference_type] = "DW_TAG_reference_type",
  [DW_TAG_compile_unit] = "DW_TAG_compile_unit",
  [DW_TAG_string_type] = "DW_TAG_string_type",
  [DW_TAG_structure_type] = "DW_TAG_structure_type",
  [DW_TAG_subroutine_type] = "DW_TAG_subroutine_type",
  [DW_TAG_typedef] = "DW_TAG_typedef",
  [DW_TAG_union_type] = "DW_TAG_union_type",
  [DW_TAG_unspecified_parameters] = "DW_TAG_unspecified_parameters",
  [DW_TAG_variant] = "DW_TAG_variant",
  [DW_TAG_common_block] = "DW_TAG_common_block",
  [DW_TAG_common_inclusion] = "DW_TAG_common_inclusion",
  [DW_TAG_inheritance] = "DW_TAG_inheritance",
  [DW_TAG_inlined_subroutine] = "DW_TAG_inlined_subroutine",
  [DW_TAG_module] = "DW_TAG_module",
  [DW_TAG_ptr_to_member_type] = "DW_TAG_ptr_to_member_type",
  [DW_TAG_set_type] = "DW_TAG_set_type",
  [DW_TAG_subrange_type] = "DW_TAG_subrange_type",
  [DW_TAG_with_stmt] = "DW_TAG_with_stmt",
  [DW_TAG_access_declaration] = "DW_TAG_access_declaration",
  [DW_TAG_base_type] = "DW_TAG_base_type",
  [DW_TAG_catch_block] = "DW_TAG_catch_block",
  [DW_TAG_const_type] = "DW_TAG_const_type",
  [DW_TAG_constant] = "DW_TAG_constant",
  [DW_TAG_enumerator] = "DW_TAG_enumerator",
  [DW_TAG_file_type] = "DW_TAG_file_type",
  [DW_TAG_friend] = "DW_TAG_friend",
  [DW_TAG_namelist] = "DW_TAG_namelist",
  [DW_TAG_namelist_item] = "DW_TAG_namelist_item",
  [DW_TAG_packed_type] = "DW_TAG_packed_type",
  [DW_TAG_subprogram] = "DW_TAG_subprogram",
  [DW_TAG_template_type_parameter] = "DW_TAG_template_type_parameter",
  [DW_TAG_template_value_parameter] = "DW_TAG_template_value_parameter",
  [DW_TAG_thrown_type] = "DW_TAG_thrown_type",
  [DW_TAG_try_block] = "DW_TAG_try_block",
  [DW_TAG_variant_part] = "DW_TAG_variant_part",
  [DW_TAG_variable] = "DW_TAG_variable",
  [DW_TAG_volatile_type] = "DW_TAG_volatile_type",
  [DW_TAG_dwarf_procedure] = "DW_TAG_dwarf_procedure",
  [DW_TAG_restrict_type] = "DW_TAG_restrict_type",
  [DW_TAG_interface_type] = "DW_TAG_interface_type",
  [DW_TAG_namespace] = "DW_TAG_namespace",
  [DW_TAG_imported_module] = "DW_TAG_imported_module",
  [DW_TAG_unspecified_type] = "DW_TAG_unspecified_type",
  [DW_TAG_partial_unit] = "DW_TAG_partial_unit",
  [DW_TAG_imported_unit] = "DW_TAG_imported_unit",
  [DW_TAG_mutable_type] = "DW_TAG_mutable_type",
  [DW_TAG_condition] = "DW_TAG_condition",
  [DW_TAG_shared_type] = "DW_TAG_shared_type",
};
#define ntagnames (sizeof (tagnames) / sizeof (tagnames[0]))


const struct
{
  int code;
  const char *name;
} attrs[] =
{
  { DW_AT_sibling, "sibling" },
  { DW_AT_location, "location" },
  { DW_AT_name, "name" },
  { DW_AT_ordering, "ordering" },
  { DW_AT_subscr_data, "subscr_data" },
  { DW_AT_byte_size, "byte_size" },
  { DW_AT_bit_offset, "bit_offset" },
  { DW_AT_bit_size, "bit_size" },
  { DW_AT_element_list, "element_list" },
  { DW_AT_stmt_list, "stmt_list" },
  { DW_AT_low_pc, "low_pc" },
  { DW_AT_high_pc, "high_pc" },
  { DW_AT_language, "language" },
  { DW_AT_member, "member" },
  { DW_AT_discr, "discr" },
  { DW_AT_discr_value, "discr_value" },
  { DW_AT_visibility, "visibility" },
  { DW_AT_import, "import" },
  { DW_AT_string_length, "string_length" },
  { DW_AT_common_reference, "common_reference" },
  { DW_AT_comp_dir, "comp_dir" },
  { DW_AT_const_value, "const_value" },
  { DW_AT_containing_type, "containing_type" },
  { DW_AT_default_value, "default_value" },
  { DW_AT_inline, "inline" },
  { DW_AT_is_optional, "is_optional" },
  { DW_AT_lower_bound, "lower_bound" },
  { DW_AT_producer, "producer" },
  { DW_AT_prototyped, "prototyped" },
  { DW_AT_return_addr, "return_addr" },
  { DW_AT_start_scope, "start_scope" },
  { DW_AT_bit_stride, "bit_stride" },
  { DW_AT_upper_bound, "upper_bound" },
  { DW_AT_abstract_origin, "abstract_origin" },
  { DW_AT_accessibility, "accessibility" },
  { DW_AT_address_class, "address_class" },
  { DW_AT_artificial, "artificial" },
  { DW_AT_base_types, "base_types" },
  { DW_AT_calling_convention, "calling_convention" },
  { DW_AT_count, "count" },
  { DW_AT_data_member_location, "data_member_location" },
  { DW_AT_decl_column, "decl_column" },
  { DW_AT_decl_file, "decl_file" },
  { DW_AT_decl_line, "decl_line" },
  { DW_AT_declaration, "declaration" },
  { DW_AT_discr_list, "discr_list" },
  { DW_AT_encoding, "encoding" },
  { DW_AT_external, "external" },
  { DW_AT_frame_base, "frame_base" },
  { DW_AT_friend, "friend" },
  { DW_AT_identifier_case, "identifier_case" },
  { DW_AT_macro_info, "macro_info" },
  { DW_AT_namelist_item, "namelist_item" },
  { DW_AT_priority, "priority" },
  { DW_AT_segment, "segment" },
  { DW_AT_specification, "specification" },
  { DW_AT_static_link, "static_link" },
  { DW_AT_type, "type" },
  { DW_AT_use_location, "use_location" },
  { DW_AT_variable_parameter, "variable_parameter" },
  { DW_AT_virtuality, "virtuality" },
  { DW_AT_vtable_elem_location, "vtable_elem_location" },
  { DW_AT_allocated, "allocated" },
  { DW_AT_associated, "associated" },
  { DW_AT_data_location, "data_location" },
  { DW_AT_byte_stride, "byte_stride" },
  { DW_AT_entry_pc, "entry_pc" },
  { DW_AT_use_UTF8, "use_UTF8" },
  { DW_AT_extension, "extension" },
  { DW_AT_ranges, "ranges" },
  { DW_AT_trampoline, "trampoline" },
  { DW_AT_call_column, "call_column" },
  { DW_AT_call_file, "call_file" },
  { DW_AT_call_line, "call_line" },
  { DW_AT_description, "description" },
  { DW_AT_binary_scale, "binary_scale" },
  { DW_AT_decimal_scale, "decimal_scale" },
  { DW_AT_small, "small" },
  { DW_AT_decimal_sign, "decimal_sign" },
  { DW_AT_digit_count, "digit_count" },
  { DW_AT_picture_string, "picture_string" },
  { DW_AT_mutable, "mutable" },
  { DW_AT_threads_scaled, "threads_scaled" },
  { DW_AT_explicit, "explicit" },
  { DW_AT_object_pointer, "object_pointer" },
  { DW_AT_endianity, "endianity" },
  { DW_AT_elemental, "elemental" },
  { DW_AT_pure, "pure" },
  { DW_AT_recursive, "recursive" },
  { DW_AT_MIPS_fde, "MIPS_fde" },
  { DW_AT_MIPS_loop_begin, "MIPS_loop_begin" },
  { DW_AT_MIPS_tail_loop_begin, "MIPS_tail_loop_begin" },
  { DW_AT_MIPS_epilog_begin, "MIPS_epilog_begin" },
  { DW_AT_MIPS_loop_unroll_factor, "MIPS_loop_unroll_factor" },
  { DW_AT_MIPS_software_pipeline_depth, "MIPS_software_pipeline_depth" },
  { DW_AT_MIPS_linkage_name, "MIPS_linkage_name" },
  { DW_AT_MIPS_stride, "MIPS_stride" },
  { DW_AT_MIPS_abstract_name, "MIPS_abstract_name" },
  { DW_AT_MIPS_clone_origin, "MIPS_clone_origin" },
  { DW_AT_MIPS_has_inlines, "MIPS_has_inlines" },
  { DW_AT_MIPS_stride_byte, "MIPS_stride_byte" },
  { DW_AT_MIPS_stride_elem, "MIPS_stride_elem" },
  { DW_AT_MIPS_ptr_dopetype, "MIPS_ptr_dopetype" },
  { DW_AT_MIPS_allocatable_dopetype, "MIPS_allocatable_dopetype" },
  { DW_AT_MIPS_assumed_shape_dopetype, "MIPS_assumed_shape_dopetype" },
  { DW_AT_MIPS_assumed_size, "MIPS_assumed_size" },
  { DW_AT_sf_names, "sf_names" },
  { DW_AT_src_info, "src_info" },
  { DW_AT_mac_info, "mac_info" },
  { DW_AT_src_coords, "src_coords" },
  { DW_AT_body_begin, "body_begin" },
  { DW_AT_body_end, "body_end" },
};
#define nattrs (sizeof (attrs) / sizeof (attrs[0]))


void
handle (Dwarf *dbg, Dwarf_Die *die, int n)
{
  Dwarf_Die child;
  unsigned int tag;
  const char *str;
  char buf[30];
  const char *name;
  Dwarf_Off off;
  Dwarf_Off cuoff;
  size_t cnt;
  Dwarf_Addr addr;
  int i;

  tag = dwarf_tag (die);
  if (tag != DW_TAG_invalid)
    {
      if (tag < ntagnames)
	str = tagnames[tag];
      else
	{
	  snprintf (buf, sizeof buf, "%#x", tag);
	  str = buf;
	}
    }
  else
    str = "* NO TAG *";

  name = dwarf_diename (die);
  if (name == 0)
    name = "* NO NAME *";

  off = dwarf_dieoffset (die);
  cuoff = dwarf_cuoffset (die);

  printf ("%*s%s\n", n * 5, "", str);
  printf ("%*s Name      : %s\n", n * 5, "", name);
  printf ("%*s Offset    : %lld\n", n * 5, "", (long long int) off);
  printf ("%*s CU offset : %lld\n", n * 5, "", (long long int) cuoff);

  printf ("%*s Attrs     :", n * 5, "");
  for (cnt = 0; cnt < nattrs; ++cnt)
    if (dwarf_hasattr (die, attrs[cnt].code))
      printf (" %s", attrs[cnt].name);
  puts ("");

  if (dwarf_hasattr (die, DW_AT_low_pc) && dwarf_lowpc (die, &addr) == 0)
    {
      Dwarf_Attribute attr;
      Dwarf_Addr addr2;
      printf ("%*s low PC    : %#llx\n",
	      n * 5, "", (unsigned long long int) addr);

      if (dwarf_attr (die, DW_AT_low_pc, &attr) == NULL
	  || dwarf_formaddr (&attr, &addr2) != 0
	  || addr != addr2)
	puts ("************* DW_AT_low_pc verify failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_addr))
	puts ("************* DW_AT_low_pc form failed ************");
      else if (dwarf_whatform (&attr) != DW_FORM_addr)
	puts ("************* DW_AT_low_pc form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_low_pc)
	puts ("************* DW_AT_low_pc attr failed ************");
    }
  if (dwarf_hasattr (die, DW_AT_high_pc) && dwarf_highpc (die, &addr) == 0)
    {
      Dwarf_Attribute attr;
      Dwarf_Addr addr2;
      printf ("%*s high PC   : %#llx\n",
	      n * 5, "", (unsigned long long int) addr);
      if (dwarf_attr (die, DW_AT_high_pc, &attr) == NULL
	  || dwarf_formaddr (&attr, &addr2) != 0
	  || addr != addr2)
	puts ("************* DW_AT_high_pc verify failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_addr))
	puts ("************* DW_AT_high_pc form failed ************");
      else if (dwarf_whatform (&attr) != DW_FORM_addr)
	puts ("************* DW_AT_high_pc form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_high_pc)
	puts ("************* DW_AT_high_pc attr failed ************");
    }

  if (dwarf_hasattr (die, DW_AT_byte_size) && (i = dwarf_bytesize (die)) != -1)
    {
      Dwarf_Attribute attr;
      Dwarf_Word u2;
      unsigned int u;
      printf ("%*s byte size : %d\n", n * 5, "", i);
      if (dwarf_attr (die, DW_AT_byte_size, &attr) == NULL
	  || dwarf_formudata (&attr, &u2) != 0
	  || i != (int) u2)
	puts ("************* DW_AT_byte_size verify failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_data1)
	       && ! dwarf_hasform (&attr, DW_FORM_data2)
	       && ! dwarf_hasform (&attr, DW_FORM_data4)
	       && ! dwarf_hasform (&attr, DW_FORM_data8)
	       && ! dwarf_hasform (&attr, DW_FORM_sdata)
	       && ! dwarf_hasform (&attr, DW_FORM_udata))
	puts ("************* DW_AT_byte_size form failed ************");
      else if ((u = dwarf_whatform (&attr)) == 0
	       || (u != DW_FORM_data1
		   && u != DW_FORM_data2
		   && u != DW_FORM_data4
		   && u != DW_FORM_data8
		   && u != DW_FORM_sdata
		   && u != DW_FORM_udata))
	puts ("************* DW_AT_byte_size form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_byte_size)
	puts ("************* DW_AT_byte_size attr failed ************");
    }
  if (dwarf_hasattr (die, DW_AT_bit_size) && (i = dwarf_bitsize (die)) != -1)
    {
      Dwarf_Attribute attr;
      Dwarf_Word u2;
      unsigned int u;
      printf ("%*s bit size  : %d\n", n * 5, "", i);
      if (dwarf_attr (die, DW_AT_bit_size, &attr) == NULL
	  || dwarf_formudata (&attr, &u2) != 0
	  || i != (int) u2)
	puts ("************* DW_AT_bit_size test failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_data1)
	       && ! dwarf_hasform (&attr, DW_FORM_data2)
	       && ! dwarf_hasform (&attr, DW_FORM_data4)
	       && ! dwarf_hasform (&attr, DW_FORM_data8)
	       && ! dwarf_hasform (&attr, DW_FORM_sdata)
	       && ! dwarf_hasform (&attr, DW_FORM_udata))
	puts ("************* DW_AT_bit_size form failed ************");
      else if ((u = dwarf_whatform (&attr)) == 0
	       || (u != DW_FORM_data1
		   && u != DW_FORM_data2
		   && u != DW_FORM_data4
		   && u != DW_FORM_data8
		   && u != DW_FORM_sdata
		   && u != DW_FORM_udata))
	puts ("************* DW_AT_bit_size form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_bit_size)
	puts ("************* DW_AT_bit_size attr failed ************");
    }
  if (dwarf_hasattr (die, DW_AT_bit_offset)
      && (i = dwarf_bitoffset (die)) != -1)
    {
      Dwarf_Attribute attr;
      Dwarf_Word u2;
      unsigned int u;
      printf ("%*s bit offset: %d\n", n * 5, "", i);
      if (dwarf_attr (die, DW_AT_bit_offset, &attr) == NULL
	  || dwarf_formudata (&attr, &u2) != 0
	  || i != (int) u2)
	puts ("************* DW_AT_bit_offset test failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_data1)
	       && ! dwarf_hasform (&attr, DW_FORM_data2)
	       && ! dwarf_hasform (&attr, DW_FORM_data4)
	       && ! dwarf_hasform (&attr, DW_FORM_data8)
	       && ! dwarf_hasform (&attr, DW_FORM_sdata)
	       && ! dwarf_hasform (&attr, DW_FORM_udata))
	puts ("************* DW_AT_bit_offset form failed ************");
      else if ((u = dwarf_whatform (&attr)) == 0
	       || (u != DW_FORM_data1
		   && u != DW_FORM_data2
		   && u != DW_FORM_data4
		   && u != DW_FORM_data8
		   && u != DW_FORM_sdata
		   && u != DW_FORM_udata))
	puts ("************* DW_AT_bit_offset form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_bit_offset)
	puts ("************* DW_AT_bit_offset attr failed ************");
    }

  if (dwarf_hasattr (die, DW_AT_language) && (i = dwarf_srclang (die)) != -1)
    {
      Dwarf_Attribute attr;
      Dwarf_Word u2;
      unsigned int u;
      printf ("%*s language  : %d\n", n * 5, "", i);
      if (dwarf_attr (die, DW_AT_language, &attr) == NULL
	  || dwarf_formudata (&attr, &u2) != 0
	  || i != (int) u2)
	puts ("************* DW_AT_language test failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_data1)
	       && ! dwarf_hasform (&attr, DW_FORM_data2)
	       && ! dwarf_hasform (&attr, DW_FORM_data4)
	       && ! dwarf_hasform (&attr, DW_FORM_data8)
	       && ! dwarf_hasform (&attr, DW_FORM_sdata)
	       && ! dwarf_hasform (&attr, DW_FORM_udata))
	puts ("************* DW_AT_language form failed ************");
      else if ((u = dwarf_whatform (&attr)) == 0
	       || (u != DW_FORM_data1
		   && u != DW_FORM_data2
		   && u != DW_FORM_data4
		   && u != DW_FORM_data8
		   && u != DW_FORM_sdata
		   && u != DW_FORM_udata))
	puts ("************* DW_AT_language form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_language)
	puts ("************* DW_AT_language attr failed ************");
    }

  if (dwarf_hasattr (die, DW_AT_ordering)
      && (i = dwarf_arrayorder (die)) != -1)
    {
      Dwarf_Attribute attr;
      Dwarf_Word u2;
      unsigned int u;
      printf ("%*s ordering  : %d\n", n * 5, "", i);
      if (dwarf_attr (die, DW_AT_ordering, &attr) == NULL
	  || dwarf_formudata (&attr, &u2) != 0
	  || i != (int) u2)
	puts ("************* DW_AT_ordering test failed ************");
      else if (! dwarf_hasform (&attr, DW_FORM_data1)
	       && ! dwarf_hasform (&attr, DW_FORM_data2)
	       && ! dwarf_hasform (&attr, DW_FORM_data4)
	       && ! dwarf_hasform (&attr, DW_FORM_data8)
	       && ! dwarf_hasform (&attr, DW_FORM_sdata)
	       && ! dwarf_hasform (&attr, DW_FORM_udata))
	puts ("************* DW_AT_ordering failed ************");
      else if ((u = dwarf_whatform (&attr)) == 0
	       || (u != DW_FORM_data1
		   && u != DW_FORM_data2
		   && u != DW_FORM_data4
		   && u != DW_FORM_data8
		   && u != DW_FORM_sdata
		   && u != DW_FORM_udata))
	puts ("************* DW_AT_ordering form (2) failed ************");
      else if (dwarf_whatattr (&attr) != DW_AT_ordering)
	puts ("************* DW_AT_ordering attr failed ************");
    }

  if (dwarf_hasattr (die, DW_AT_comp_dir))
    {
      Dwarf_Attribute attr;
      if (dwarf_attr (die, DW_AT_comp_dir, &attr) == NULL
	  || (name = dwarf_formstring (&attr)) == NULL)
	puts ("************* DW_AT_comp_dir attr failed ************");
      else
	printf ("%*s directory : %s\n", n * 5, "", name);
    }

  if (dwarf_hasattr (die, DW_AT_producer))
    {
      Dwarf_Attribute attr;
      if (dwarf_attr (die, DW_AT_producer, &attr) == NULL
	  || (name = dwarf_formstring (&attr)) == NULL)
	puts ("************* DW_AT_comp_dir attr failed ************");
      else
	printf ("%*s producer  : %s\n", n * 5, "", name);
    }

  if (dwarf_haschildren (die) != 0 && dwarf_child (die, &child) == 0)
    handle (dbg, &child, n + 1);
  if (dwarf_siblingof (die, die) == 0)
    handle (dbg, die, n);
}


int
main (int argc, char *argv[])
{
 int cnt;

  for (cnt = 1; cnt < argc; ++cnt)
    {
      int fd = open (argv[cnt], O_RDONLY);
      Dwarf *dbg;

      printf ("file: %s\n", basename (argv[cnt]));

      dbg = dwarf_begin (fd, DWARF_C_READ);
      if (dbg == NULL)
	{
	  printf ("%s not usable\n", argv[cnt]);
	  close (fd);
	  continue;
	}

      Dwarf_Off off = 0;
      Dwarf_Off old_off = 0;
      size_t hsize;
      Dwarf_Off abbrev;
      uint8_t addresssize;
      uint8_t offsetsize;
      while (dwarf_nextcu (dbg, off, &off, &hsize, &abbrev, &addresssize,
			   &offsetsize) == 0)
	{
	  printf ("New CU: off = %llu, hsize = %zu, ab = %llu, as = %" PRIu8
		  ", os = %" PRIu8 "\n",
		  (unsigned long long int) old_off, hsize,
		  (unsigned long long int) abbrev, addresssize,
		  offsetsize);

	  Dwarf_Die die;
	  if (dwarf_offdie (dbg, old_off + hsize, &die) != NULL)
	    handle (dbg, &die, 1);

	  old_off = off;
	}

      dwarf_end (dbg);
      close (fd);
    }

  return 0;
}
