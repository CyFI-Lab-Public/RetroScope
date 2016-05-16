# -*- rpm-spec-*-
Summary: A collection of utilities and DSOs to handle compiled objects
Name: elfutils
Version: 0.138
Release: 1
License: GPLv2 with exceptions
Group: Development/Tools
Source: elfutils-%{version}.tar.bz2
Obsoletes: libelf libelf-devel
Requires: elfutils-libelf = %{version}-%{release}
Requires: glibc >= 2.7

# ExcludeArch: xxx

BuildRoot: %{_tmppath}/%{name}-root
BuildRequires: gcc >= 4.1.2-33
BuildRequires: glibc >= 2.7
BuildRequires: bison >= 1.875
BuildRequires: flex >= 2.5.4a
BuildRequires: bzip2
BuildRequires: m4

%define _gnu %{nil}
%define _programprefix eu-

%description
Elfutils is a collection of utilities, including ld (a linker),
nm (for listing symbols from object files), size (for listing the
section sizes of an object or archive file), strip (for discarding
symbols), readelf (to see the raw ELF file structures), and elflint
(to check for well-formed ELF files).  Also included are numerous
helper libraries which implement DWARF, ELF, and machine-specific ELF
handling.

%package devel
Summary: Development libraries to handle compiled objects.
Group: Development/Tools
Requires: elfutils = %{version}-%{release}
Requires: elfutils-libelf-devel = %{version}-%{release}

%description devel
The elfutils-devel package contains the libraries to create
applications for handling compiled objects.  libebl provides some
higher-level ELF access functionality.  libdw provides access to
the DWARF debugging information.  libasm provides a programmable
assembler interface.

%package devel-static
Summary: Static archives to handle compiled objects.
Group: Development/Tools
Requires: elfutils-devel = %{version}-%{release}

%description devel-static
The elfutils-devel-static archive contains the static archives
with the code the handle compiled objects.

%package libelf
Summary: Library to read and write ELF files.
Group: Development/Tools

%description libelf
The elfutils-libelf package provides a DSO which allows reading and
writing ELF files on a high level.  Third party programs depend on
this package to read internals of ELF files.  The programs of the
elfutils package use it also to generate new ELF files.

%package libelf-devel
Summary: Development support for libelf
Group: Development/Tools
Requires: elfutils-libelf = %{version}-%{release}
Conflicts: libelf-devel

%description libelf-devel
The elfutils-libelf-devel package contains the libraries to create
applications for handling compiled objects.  libelf allows you to
access the internals of the ELF object file format, so you can see the
different sections of an ELF file.

%package libelf-devel-static
Summary: Static archive of libelf
Group: Development/Tools
Requires: elfutils-libelf-devel = %{version}-%{release}
Conflicts: libelf-devel

%description libelf-devel-static
The elfutils-libelf-static package contains the static archive
for libelf.

%prep
%setup -q

%build
%configure --program-prefix=%{_programprefix}
make

%install
rm -rf ${RPM_BUILD_ROOT}
mkdir -p ${RPM_BUILD_ROOT}%{_prefix}

%makeinstall

chmod +x ${RPM_BUILD_ROOT}%{_prefix}/%{_lib}/lib*.so*
chmod +x ${RPM_BUILD_ROOT}%{_prefix}/%{_lib}/elfutils/lib*.so*

# XXX Nuke unpackaged files
{ cd ${RPM_BUILD_ROOT}
  rm -f .%{_bindir}/eu-ld
  rm -f .%{_includedir}/elfutils/libasm.h
  rm -f .%{_libdir}/libasm.so
  rm -f .%{_libdir}/libasm.a
}

%check
make check

%clean
rm -rf ${RPM_BUILD_ROOT}

%post -p /sbin/ldconfig

%postun -p /sbin/ldconfig

%post libelf -p /sbin/ldconfig

%postun libelf -p /sbin/ldconfig

%files
%defattr(-,root,root)
%doc README TODO
%{_bindir}/eu-elflint
%{_bindir}/eu-nm
%{_bindir}/eu-readelf
%{_bindir}/eu-size
%{_bindir}/eu-strip
%{_bindir}/eu-findtextrel
%{_bindir}/eu-addr2line
%{_bindir}/eu-elfcmp
%{_bindir}/eu-ranlib
%{_bindir}/eu-strings
%{_bindir}/eu-objdump
%{_bindir}/eu-ar
%{_bindir}/eu-unstrip
%{_bindir}/eu-make-debug-archive
#%{_bindir}/eu-ld
%{_libdir}/libasm-%{version}.so
%{_libdir}/libdw-%{version}.so
%{_libdir}/libasm.so.*
%{_libdir}/libdw.so.*
%dir %{_libdir}/elfutils
%{_libdir}/elfutils/lib*.so

%files devel
%defattr(-,root,root)
%{_includedir}/dwarf.h
%dir %{_includedir}/elfutils
%{_includedir}/elfutils/elf-knowledge.h
#%{_includedir}/elfutils/libasm.h
%{_includedir}/elfutils/libebl.h
%{_includedir}/elfutils/libdw.h
%{_includedir}/elfutils/libdwfl.h
%{_libdir}/libebl.a
#%{_libdir}/libasm.so
%{_libdir}/libdw.so

%files devel-static
%{_libdir}/libdw.a
#%{_libdir}/libasm.a

%files libelf
%defattr(-,root,root)
%{_libdir}/libelf-%{version}.so
%{_libdir}/libelf.so.*

%files libelf-devel
%defattr(-,root,root)
%{_includedir}/libelf.h
%{_includedir}/gelf.h
%{_includedir}/nlist.h
%{_libdir}/libelf.so

%files libelf-devel-static
%{_libdir}/libelf.a

%changelog
* Wed Dec 31 2008 Roland McGrath <roland@redhat.com> 0.138-1
- Install <elfutils/version.h> header file for applications to use in
  source version compatibility checks.
- libebl: backend fixes for i386 TLS relocs; backend support for
  NT_386_IOPERM
- libcpu: disassembler fixes
- libdwfl: bug fixes
- libelf: bug fixes
- nm: bug fixes for handling corrupt input files

* Tue Aug 26 2008 Ulrich Drepper <drepper@redhat.com> 0.137-1
- Minor fixes for unreleased 0.136 release.

* Mon Aug 25 2008 Ulrich Drepper <drepper@redhat.com> 0.136-1
- libdwfl: bug fixes; new segment interfaces;	 all the libdwfl-based
 tools now support --core=COREFILE option

* Mon May 12 2008 Ulrich Drepper <drepper@redhat.com> 0.135-1
- libdwfl: bug fixes
- strip: changed handling of ET_REL files wrt symbol tables and relocs

* Tue Apr  8 2008 Ulrich Drepper <drepper@redhat.com> 0.134-1
- elflint: backend improvements for sparc, alpha
- libdwfl, libelf: bug fixes

* Sat Mar  1 2008 Ulrich Drepper <drepper@redhat.com> 0.133-1
- readelf, elflint, libebl: SHT_GNU_ATTRIBUTE section handling (readelf -A)
- readelf: core note handling for NT_386_TLS, NT_PPC_SPE, Alpha NT_AUXV
- libdwfl: bug fixes and optimization in relocation handling
- elfcmp: bug fix for non-allocated section handling
- ld: implement newer features of binutils linker.

* Mon Jan 21 2008 Ulrich Drepper <drepper@redhat.com> 0.132-1
- libcpu: Implement x86 and x86-64 disassembler.
- libasm: Add interface for disassembler.
- all programs: add debugging of branch prediction.
- libelf: new function elf_scnshndx.

* Sun Nov 11 2007 Ulrich Drepper <drepper@redhat.com> 0.131-1
- libdw: DW_FORM_ref_addr support; dwarf_formref entry point now depreca
ted;       bug fixes for oddly-formatted DWARF
- libdwfl: bug fixes in offline archive support, symbol table handling;
	 apply partial relocations for dwfl_module_address_section on
ET_REL
- libebl: powerpc backend support for Altivec registers

* Mon Oct 15 2007 Ulrich Drepper <drepper@redhat.com> 0.130-1
- readelf: -p option can take an argument like -x for one section,
	 or no argument (as before) for all SHF_STRINGS sections;
	 new option --archive-index (or -c);	 improved -n output fo
r core files, on many machines
- libelf: new function elf_getdata_rawchunk, replaces gelf_rawchunk;
	new functions gelf_getnote, gelf_getauxv, gelf_update_auxv
- readelf, elflint: handle SHT_NOTE sections without requiring phdrs
- elflint: stricter checks on debug sections
- libdwfl: new functions dwfl_build_id_find_elf, dwfl_build_id_find_debu
ginfo,	 dwfl_module_build_id, dwfl_module_report_build_id;	 suppo
rt dynamic symbol tables found via phdrs;	 dwfl_standard_find_de
buginfo now uses build IDs when available
- unstrip: new option --list (or -n)
- libebl: backend improvements for sparc, alpha, powerpc

* Tue Aug 14 2007 Ulrich Drepper <drepper@redhat.com> 0.129-1
- readelf: new options --hex-dump (or -x), --strings (or -p)
- addr2line: new option --symbols (or -S)

* Wed Apr 18 2007 Ulrich Drepper <drepper@redhat.com> 0.127-1
- libdw: new function dwarf_getsrcdirs
- libdwfl: new functions dwfl_module_addrsym, dwfl_report_begin_add,
	 dwfl_module_address_section

* Mon Feb  5 2007 Ulrich Drepper <drepper@redhat.com> 0.126-1
- new program: ar

* Mon Dec 18 2006 Ulrich Drepper <drepper@redhat.com> 0.125-1
- elflint: Compare DT_GNU_HASH tests.
- move archives into -static RPMs
- libelf, elflint: better support for core file handling

* Tue Oct 10 2006 Ulrich Drepper <drepper@redhat.com> 0.124-1
- libebl: sparc backend support for return value location
- libebl, libdwfl: backend register name support extended with more info
- libelf, libdw: bug fixes for unaligned accesses on machines that care
- readelf, elflint: trivial bugs fixed

* Mon Aug 14 2006 Roland McGrath <roland@redhat.com> 0.123-1
- libebl: Backend build fixes, thanks to Stepan Kasal.
- libebl: ia64 backend support for register names, return value location
- libdwfl: Handle truncated linux kernel module section names.
- libdwfl: Look for linux kernel vmlinux files with .debug suffix.
- elflint: Fix checks to permit --hash-style=gnu format.

* Wed Jul 12 2006 Ulrich Drepper <drepper@redhat.com> 0.122-1
- libebl: add function to test for relative relocation
- elflint: fix and extend DT_RELCOUNT/DT_RELACOUNT checks
- elflint, readelf: add support for DT_GNU_HASHlibelf: add elf_gnu_hash
- elflint, readelf: add support for 64-bit SysV-style hash tables
- libdwfl: new functions dwfl_module_getsymtab, dwfl_module_getsym.

* Wed Jun 14 2006  <drepper@redhat.com> 0.121-1
- libelf: bug fixes for rewriting existing files when using mmap.
- make all installed headers usable in C++ code.
- readelf: better output format.
- elflint: fix tests of dynamic section content.
- ld: Implement --as-needed, --execstack, PT_GNU_STACK.  Many small patc
hes.
- libdw, libdwfl: handle files without aranges info.

* Tue Apr  4 2006 Ulrich Drepper <drepper@redhat.com> 0.120-1
- Bug fixes.
- dwarf.h updated for DWARF 3.0 final specification.
- libdwfl: New function dwfl_version.
- The license is now GPL for most files.  The libelf, libebl, libdw,and
libdwfl libraries have additional exceptions.  Add reference toOIN.

* Thu Jan 12 2006 Roland McGrath <roland@redhat.com> 0.119-1
- elflint: more tests.
- libdwfl: New function dwfl_module_register_names.
- libebl: New backend hook for register names.

* Tue Dec  6 2005 Ulrich Drepper <drepper@redhat.com> 0.118-1
- elflint: more tests.
- libdwfl: New function dwfl_module_register_names.
- libebl: New backend hook for register names.

* Thu Nov 17 2005 Ulrich Drepper <drepper@redhat.com> 0.117-1
- libdwfl: New function dwfl_module_return_value_location.
- libebl: Backend improvements for several CPUs.

* Mon Oct 31 2005 Ulrich Drepper <drepper@redhat.com> 0.116-1
- libdw: New functions dwarf_ranges, dwarf_entrypc, dwarf_diecu,       d
warf_entry_breakpoints.  Removed Dwarf_Func type and functions       d
warf_func_name, dwarf_func_lowpc, dwarf_func_highpc,       dwarf_func_
entrypc, dwarf_func_die; dwarf_getfuncs callback now uses       Dwarf_
Die, and dwarf_func_file, dwarf_func_line, dwarf_func_col       replac
ed by dwarf_decl_file, dwarf_decl_line, dwarf_decl_column;       dwarf
_func_inline, dwarf_func_inline_instances now take Dwarf_Die.       Ty
pe Dwarf_Loc renamed to Dwarf_Op; dwarf_getloclist,       dwarf_addrlo
clists renamed dwarf_getlocation, dwarf_getlocation_addr.

* Fri Sep  2 2005 Ulrich Drepper <drepper@redhat.com> 0.115-1
- libelf: speed-ups of non-mmap reading.
- strings: New program.
- Implement --enable-gcov option for configure.
- libdw: New function dwarf_getscopes_die.

* Wed Aug 24 2005 Ulrich Drepper <drepper@redhat.com> 0.114-1
- libelf: new function elf_getaroff
- libdw: Added dwarf_func_die, dwarf_func_inline, dwarf_func_inline_inst
ances.
- libdwfl: New functions dwfl_report_offline, dwfl_offline_section_addre
ss,	 dwfl_linux_kernel_report_offline.
- ranlib: new program

* Mon Aug 15 2005 Ulrich Drepper <drepper@redhat.com> 0.114-1
- libelf: new function elf_getaroff
- ranlib: new program

* Wed Aug 10 2005 Ulrich Drepper <@redhat.com> 0.113-1
- elflint: relax a bit. Allow version definitions for defined symbols ag
ainstDSO versions also for symbols in nobits sections.  Allow .rodata
sectionto have STRINGS and MERGE flag set.
- strip: add some more compatibility with binutils.

* Sat Aug  6 2005 Ulrich Drepper <@redhat.com> 0.113-1
- elflint: relax a bit. Allow version definitions for defined symbols ag
ainstDSO versions also for symbols in nobits sections.  Allow .rodata
sectionto have STRINGS and MERGE flag set.

* Sat Aug  6 2005 Ulrich Drepper <@redhat.com> 0.113-1
- elflint: relax a bit. Allow version definitions for defined symbols ag
ainstDSO versions also for symbols in nobits sections.

* Fri Aug  5 2005 Ulrich Drepper <@redhat.com> 0.112-1
- elfcmp: some more relaxation.
- elflint: many more tests, especially regarding to symbol versioning.
- libelf: Add elfXX_offscn and gelf_offscn.
- libasm: asm_begin interface changes.
- libebl: Add three new interfaces to directly access machine, class, an
ddata encoding information.
- objdump: New program.  Just the beginning.

* Thu Jul 28 2005 Ulrich Drepper <@redhat.com> 0.111-1
- libdw: now contains all of libdwfl.  The latter is not installed anymore.
- elfcmp: little usability tweak, name and index of differing section is
 printed.

* Sun Jul 24 2005 Ulrich Drepper <@redhat.com> 0.110-1
- libelf: fix a numbe rof problems with elf_update
- elfcmp: fix a few bugs.  Compare gaps.
- Fix a few PLT problems and mudflap build issues.
- libebl: Don't expose Ebl structure definition in libebl.h.  It's now p
rivate.

* Thu Jul 21 2005 Ulrich Drepper <@redhat.com> 0.109-1
- libebl: Check for matching modules.
- elflint: Check that copy relocations only happen for OBJECT or NOTYPE
symbols.
- elfcmp: New program.
- libdwfl: New library.

* Mon May  9 2005 Ulrich Drepper <@redhat.com> 0.108-1
- strip: fix bug introduced in last change
- libdw: records returned by dwarf_getsrclines are now sorted by address

* Sun May  8 2005 Ulrich Drepper <@redhat.com> 0.108-1
- strip: fix bug introduced in last change

* Sun May  8 2005 Ulrich Drepper <@redhat.com> 0.107-1
- readelf: improve DWARF output format
- strip: support Linux kernel modules

* Fri Apr 29 2005 Ulrich Drepper <drepper@redhat.com> 0.107-1
- readelf: improve DWARF output format

* Mon Apr  4 2005 Ulrich Drepper <drepper@redhat.com> 0.106-1
- libdw: Updated dwarf.h from DWARF3 speclibdw: add new funtions dwarf_f
unc_entrypc, dwarf_func_file, dwarf_func_line,dwarf_func_col, dwarf_ge
tsrc_file

* Fri Apr  1 2005 Ulrich Drepper <drepper@redhat.com> 0.105-1
- addr2line: New program
- libdw: add new functions: dwarf_addrdie, dwarf_macro_*, dwarf_getfuncs
,dwarf_func_*.
- findtextrel: use dwarf_addrdie

* Mon Mar 28 2005 Ulrich Drepper <drepper@redhat.com> 0.104-1
- findtextrel: New program.

* Mon Mar 21 2005 Ulrich Drepper <drepper@redhat.com> 0.103-1
- libdw: Fix using libdw.h with gcc < 4 and C++ code.  Compiler bug.

* Tue Feb 22 2005 Ulrich Drepper <drepper@redhat.com> 0.102-1
- More Makefile and spec file cleanups.

* Fri Jan 16 2004 Jakub Jelinek <jakub@redhat.com> 0.94-1
- upgrade to 0.94

* Fri Jan 16 2004 Jakub Jelinek <jakub@redhat.com> 0.93-1
- upgrade to 0.93

* Thu Jan  8 2004 Jakub Jelinek <jakub@redhat.com> 0.92-1
- full version
- macroized spec file for GPL or OSL builds
- include only libelf under GPL plus wrapper scripts

* Wed Jan  7 2004 Jakub Jelinek <jakub@redhat.com> 0.91-2
- macroized spec file for GPL or OSL builds

* Wed Jan  7 2004 Ulrich Drepper <drepper@redhat.com>
- split elfutils-devel into two packages.

* Wed Jan  7 2004 Jakub Jelinek <jakub@redhat.com> 0.91-1
- include only libelf under GPL plus wrapper scripts

* Tue Dec 23 2003 Jeff Johnson <jbj@redhat.com> 0.89-3
- readelf, not readline, in %%description (#111214).

* Fri Sep 26 2003 Bill Nottingham <notting@redhat.com> 0.89-1
- update to 0.89 (fix eu-strip)

* Tue Sep 23 2003 Jakub Jelinek <jakub@redhat.com> 0.86-3
- update to 0.86 (fix eu-strip on s390x/alpha)
- libebl is an archive now; remove references to DSO

* Mon Jul 14 2003 Jeff Johnson <jbj@redhat.com> 0.84-3
- upgrade to 0.84 (readelf/elflint improvements, rawhide bugs fixed).

* Fri Jul 11 2003 Jeff Johnson <jbj@redhat.com> 0.83-3
- upgrade to 0.83 (fix invalid ELf handle on *.so strip, more).

* Wed Jul  9 2003 Jeff Johnson <jbj@redhat.com> 0.82-3
- upgrade to 0.82 (strip tests fixed on big-endian).

* Tue Jul  8 2003 Jeff Johnson <jbj@redhat.com> 0.81-3
- upgrade to 0.81 (strip excludes unused symtable entries, test borked).

* Thu Jun 26 2003 Jeff Johnson <jbj@redhat.com> 0.80-3
- upgrade to 0.80 (debugedit changes for kernel in progress).

* Wed Jun 04 2003 Elliot Lee <sopwith@redhat.com>
- rebuilt

* Wed May 21 2003 Jeff Johnson <jbj@redhat.com> 0.79-2
- upgrade to 0.79 (correct formats for size_t, more of libdw "works").

* Mon May 19 2003 Jeff Johnson <jbj@redhat.com> 0.78-2
- upgrade to 0.78 (libdwarf bugfix, libdw additions).

* Mon Feb 24 2003 Elliot Lee <sopwith@redhat.com>
- debuginfo rebuild

* Thu Feb 20 2003 Jeff Johnson <jbj@redhat.com> 0.76-2
- use the correct way of identifying the section via the sh_info link.

* Sat Feb 15 2003 Jakub Jelinek <jakub@redhat.com> 0.75-2
- update to 0.75 (eu-strip -g fix)

* Tue Feb 11 2003 Jakub Jelinek <jakub@redhat.com> 0.74-2
- update to 0.74 (fix for writing with some non-dirty sections)

* Thu Feb  6 2003 Jeff Johnson <jbj@redhat.com> 0.73-3
- another -0.73 update (with sparc fixes).
- do "make check" in %%check, not %%install, section.

* Mon Jan 27 2003 Jeff Johnson <jbj@redhat.com> 0.73-2
- update to 0.73 (with s390 fixes).

* Wed Jan 22 2003 Tim Powers <timp@redhat.com>
- rebuilt

* Wed Jan 22 2003 Jakub Jelinek <jakub@redhat.com> 0.72-4
- fix arguments to gelf_getsymshndx and elf_getshstrndx
- fix other warnings
- reenable checks on s390x

* Sat Jan 11 2003 Karsten Hopp <karsten@redhat.de> 0.72-3
- temporarily disable checks on s390x, until someone has
  time to look at it

* Thu Dec 12 2002 Jakub Jelinek <jakub@redhat.com> 0.72-2
- update to 0.72

* Wed Dec 11 2002 Jakub Jelinek <jakub@redhat.com> 0.71-2
- update to 0.71

* Wed Dec 11 2002 Jeff Johnson <jbj@redhat.com> 0.69-4
- update to 0.69.
- add "make check" and segfault avoidance patch.
- elfutils-libelf needs to run ldconfig.

* Tue Dec 10 2002 Jeff Johnson <jbj@redhat.com> 0.68-2
- update to 0.68.

* Fri Dec  6 2002 Jeff Johnson <jbj@redhat.com> 0.67-2
- update to 0.67.

* Tue Dec  3 2002 Jeff Johnson <jbj@redhat.com> 0.65-2
- update to 0.65.

* Mon Dec  2 2002 Jeff Johnson <jbj@redhat.com> 0.64-2
- update to 0.64.

* Sun Dec 1 2002 Ulrich Drepper <drepper@redhat.com> 0.64
- split packages further into elfutils-libelf

* Sat Nov 30 2002 Jeff Johnson <jbj@redhat.com> 0.63-2
- update to 0.63.

* Fri Nov 29 2002 Ulrich Drepper <drepper@redhat.com> 0.62
- Adjust for dropping libtool

* Sun Nov 24 2002 Jeff Johnson <jbj@redhat.com> 0.59-2
- update to 0.59

* Thu Nov 14 2002 Jeff Johnson <jbj@redhat.com> 0.56-2
- update to 0.56

* Thu Nov  7 2002 Jeff Johnson <jbj@redhat.com> 0.54-2
- update to 0.54

* Sun Oct 27 2002 Jeff Johnson <jbj@redhat.com> 0.53-2
- update to 0.53
- drop x86_64 hack, ICE fixed in gcc-3.2-11.

* Sat Oct 26 2002 Jeff Johnson <jbj@redhat.com> 0.52-3
- get beehive to punch a rhpkg generated package.

* Wed Oct 23 2002 Jeff Johnson <jbj@redhat.com> 0.52-2
- build in 8.0.1.
- x86_64: avoid gcc-3.2 ICE on x86_64 for now.

* Tue Oct 22 2002 Ulrich Drepper <drepper@redhat.com> 0.52
- Add libelf-devel to conflicts for elfutils-devel

* Mon Oct 21 2002 Ulrich Drepper <drepper@redhat.com> 0.50
- Split into runtime and devel package

* Fri Oct 18 2002 Ulrich Drepper <drepper@redhat.com> 0.49
- integrate into official sources

* Wed Oct 16 2002 Jeff Johnson <jbj@redhat.com> 0.46-1
- Swaddle.
