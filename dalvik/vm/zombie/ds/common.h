#ifndef __BDS_DSCOMMON_H
#define __BDS_DSCOMMON_H

#ifdef __cplusplus
extern "C"
{
#endif

/**
 * container_of - get the struct for this entry
 * @ptr:	the pointer to a data struct entry in a container struct.
 * @type:	the type of the struct this is embedded in.
 * @member:	the name of the data struct entry within the container.
 */
#define container_of(ptr, type, member) \
    ((type *)((char *)(ptr)-(unsigned long)(&((type *)0)->member)))

static inline void * __safe_save_check_and_subtr (char * ptr, unsigned long off) {
  return ( ptr == NULL ? NULL : ptr - off );
}

#define container_of_safe(ptr, type, member) \
    ((type *)(__safe_save_check_and_subtr ((char *)ptr, (unsigned long)&((type *)0)->member)))

#ifdef __cplusplus
}
#endif

#endif // __BDS_DSCOMMON_H
