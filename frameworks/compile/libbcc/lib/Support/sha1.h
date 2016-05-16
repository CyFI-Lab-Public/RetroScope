/*
 * See "sha1.c" for author info.
 */
#ifndef _DALVIK_SHA1
#define _DALVIK_SHA1

typedef struct {
    unsigned long state[5];
    unsigned long count[2];
    unsigned char buffer[64];
} SHA1_CTX;

#define HASHSIZE 20

#if defined(__cplusplus)
extern "C" {
#endif

void SHA1Init(SHA1_CTX* context);
void SHA1Update(SHA1_CTX* context, const unsigned char* data,
    unsigned long len);
void SHA1Final(unsigned char digest[HASHSIZE], SHA1_CTX* context);

#if defined(__cplusplus)
}
#endif

#endif /*_DALVIK_SHA1*/
