#ifndef BDS_ELF_H
#define BDS_ELF_H

/* 
 * elf.h - A package for manipulating Elf binaries that
 *         have been mapped into memory by the loader.
 */

#ifdef __cplusplus
extern "C"
{
#endif

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <errno.h>
#include <fcntl.h>
#include <elf.h>

/* 
 * This is a handle that is created by elf_open and then used by every
 * other function in the elf package 
*/
typedef struct {
    void *maddr;            /* Start of mapped Elf binary segment in memory */
    int mlen;               /* Length in bytes of the mapped Elf segment */
    Elf32_Ehdr *ehdr;       /* Start of main Elf header (same as maddr) */
    Elf32_Sym *dsymtab;     /* Start of dynamic symbol table */
    Elf32_Sym *dsymtab_end; /* End of dynamic symbol table (dsymtab + size) */
    char *dstrtab;          /* Address of dynamic string table */ 
} Elf_obj;



// Forward Decls.
static inline Elf_obj * elf_register(void *mapped_addr, int len); 

/* 
 * elf_open - Map a binary into the address space and extract the
 * locations of the static and dynamic symbol tables and their string
 * tables. Return this information in a Elf object file handle that will
 * be passed to all of the other elf functions.
 */
static inline Elf_obj *
elf_open(char *filename) 
{
    int fd;
    struct stat sbuf;
    void * addr;
    int len;

    /* Do some consistency checks on the binary */
    fd = open(filename, O_RDONLY);
    if (fd == -1) {
	fprintf(stderr,	"Can't open \"%s\": %s\n", filename, strerror(errno));
        return NULL;
    }
    if (fstat(fd, &sbuf) == -1) {
	fprintf(stderr, "Can't stat \"%s\": %s\n", filename, strerror(errno));
        return NULL;
    }
    if (sbuf.st_size < sizeof(Elf32_Ehdr)) {
	fprintf(stderr, "\"%s\" is not an ELF binary object\n",	filename);
        return NULL;
    }

    /* It looks OK, so map the Elf binary into our address space */
    len = sbuf.st_size;
    addr = mmap(NULL, len, PROT_READ, MAP_SHARED, fd, 0);
    if (addr == (void *)-1) {
	fprintf(stderr,	"Can't mmap \"%s\": %s\n", filename, strerror(errno));
        return NULL;
    }
    close(fd);

    return elf_register(addr, len);
}


static inline void elf_unregister(Elf_obj *ep) { free(ep); }

/* 
 * elf_close - Free up the resources of an  elf object
 */
static inline void
elf_close(Elf_obj *ep) 
{
    if (munmap(ep->maddr, ep->mlen) < 0) {
	perror("munmap");
    }
    elf_unregister(ep);
}

/*
 * elf_dsymname - Return ASCII name of a dynamic symbol
 */ 
static inline char *
elf_dsymname(Elf_obj *ep, Elf32_Sym *sym)
{
    return &ep->dstrtab[sym->st_name];
}

/*
 * elf_firstdsym - Return ptr to first symbol in dynamic symbol table
 */
static inline Elf32_Sym *
elf_firstdsym(Elf_obj *ep)
{
    return ep->dsymtab;
}

/*
 * elf_nextdsym - Return ptr to next symbol in dynamic symbol table,
 * of NULL if no more symbols.
 */ 
static inline Elf32_Sym *
elf_nextdsym(Elf_obj *ep, Elf32_Sym *sym)
{
    sym++;
    if (sym < ep->dsymtab_end)
	return sym;
    else
	return NULL;
}

/*
 * elf_isdfunc - Return true if symbol is a dynamic function 
 */
static inline int
elf_isdfunc(Elf32_Sym *sym) 
{
    return ((ELF32_ST_TYPE(sym->st_info) == STT_FUNC));
}


/*
 * elf_firstphdr - Return ptr to first program header entry
 */
static inline Elf32_Phdr *
elf_firstphdr(Elf_obj *ep)
{
    return (Elf32_Phdr *)((uintptr_t)ep->maddr + ep->ehdr->e_phoff);
}

/*
 * elf_nextphdr - Return ptr to next program header entry,
 * or NULL if no more hdrs.
 */
static inline Elf32_Phdr *
elf_nextphdr(Elf_obj *ep, Elf32_Phdr *phdr)
{
    Elf32_Sym * phdr_end = (Elf32_Sym *)((uintptr_t)elf_firstphdr(ep) + 
                               (ep->ehdr->e_phentsize * ep->ehdr->e_phnum));

    phdr++;
    if ((unsigned)phdr < (unsigned)phdr_end)
	return phdr;
    else
	return NULL;
}


static inline Elf32_Dyn *
elf_firstdyn(Elf_obj *ep)
{
  Elf32_Phdr * dyn_hdr = elf_firstphdr(ep);
  while(dyn_hdr != NULL)
  {
    dyn_hdr = elf_nextphdr(ep, dyn_hdr);
    if(dyn_hdr->p_type == PT_DYNAMIC) 
      return (Elf32_Dyn *)((uintptr_t)ep->maddr + dyn_hdr->p_vaddr); // use vaddr when loaded 
                                            //into mem because it will point to the next page.
  }
  return NULL;
}

/*
 * elf_nextdyn - Return ptr to next dynamic section entry,
 * or NULL if no more.
 */
static inline Elf32_Dyn *
elf_nextdyn(Elf32_Dyn *dyn_entry)
{
    dyn_entry++;
    if (dyn_entry->d_tag != DT_NULL)
	return dyn_entry;
    else
	return NULL;
}

static inline Elf_obj *
elf_register(void *mapped_addr, int len)
{
    Elf_obj *ep;

    if ((ep = (Elf_obj *) malloc(sizeof(Elf_obj))) == NULL) {
	fprintf(stderr, "Malloc failed: %s\n", strerror(errno));
        return NULL;
    }

    /* The Elf binary begins with the Elf header */
    ep->mlen = len;
    ep->maddr = mapped_addr;
    ep->ehdr = (Elf32_Ehdr*)ep->maddr;

    /* Make sure that this is an Elf binary */ 
    if (strncmp((char*)ep->ehdr->e_ident, ELFMAG, SELFMAG)) {
	fprintf(stderr, "Not an ELF binary object!!\n");
        return NULL;
    }

    for(Elf32_Dyn * dyn_ent = elf_firstdyn(ep);
        dyn_ent != NULL; dyn_ent = elf_nextdyn(dyn_ent))
    {
      if(dyn_ent->d_tag == DT_SYMTAB)
      {
        ep->dsymtab = (Elf32_Sym *)((uintptr_t)ep->maddr + dyn_ent->d_un.d_val);
      }
      else if(dyn_ent->d_tag == DT_STRTAB)
      {
        ep->dstrtab = (char *)((uintptr_t)ep->maddr + dyn_ent->d_un.d_val);
        ep->dsymtab_end = (Elf32_Sym *)((uintptr_t)ep->dstrtab - sizeof(Elf32_Sym));
      }
    }
    return ep;
}

static inline Elf32_Sym *
elf_get_dysym_for_name(Elf_obj *elf_obj, const char * name)
{
  for(Elf32_Sym * sym = elf_firstdsym(elf_obj);
      sym != NULL; sym = elf_nextdsym(elf_obj, sym))
  {
    if(strcmp(elf_dsymname(elf_obj, sym), name) == 0) return sym;
  }
  return NULL;
}


#ifdef __cplusplus
}
#endif

#endif // BDS_ELF_H

