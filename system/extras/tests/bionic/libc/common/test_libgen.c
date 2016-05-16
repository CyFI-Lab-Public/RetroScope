// test the basename, dirname, basename_r and dirname_r
#include <libgen.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

static int  fail = 0;

static void
test_basename(char*  _input, const char*  _expected, int  _errno)
{
    char   temp[256], *input = _input;
    char*  ret;
#if HOST
    /* GLibc does modify the input string. bummer */
    if (_input) {
        strcpy(temp, _input);
        input = temp;
    }
#endif
    errno = 0;
    ret   = basename(input);
    if (_expected == NULL) {
        if (ret != NULL) {
            fprintf(stderr,
                    "KO: basename(\"%s\") returned \"%s\", NULL expected)\n",
                    _input, ret);
            fail += 1;
        } else if (errno != _errno) {
            fprintf(stderr,
                    "KO: basename(\"%s\") returned NULL with error: %d (%d expected)\n",
                    _input, errno, _errno);
            fail += 1;
        } else {
            printf( "OK: basename(\"%s\") returned NULL with error %d\n",
                    _input, _errno );
        }
    } else {
        if (ret == NULL) {
            fprintf(stderr, "KO: basename(\"%s\") returned NULL with error %d\n",
                    _input, errno);
            fail += 1;
        }
        else if (strcmp(ret, _expected)) {
            fprintf(stderr, "KO: basename(\"%s\") returned \"%s\", instead of \"%s\"\n",
                    _input, ret, _expected);
        }
        else {
            printf( "OK: basename(\"%s\") returned \"%s\"\n",
                    _input, ret );
        }
    }
}


#if !HOST
static void
test_basename_r(char*  _input, const char*  _expected_content, int  _expected, char*  _buff, size_t  _bufflen, int  _errno)
{
    int   ret;
    errno = 0;
    ret   = basename_r(_input, _buff, _bufflen );
    if (ret != _expected) {
        fprintf(stderr,
                "KO: basename_r(\"%s\", <buff>, %d) returned %d (expected %d)\n",
                _input, _bufflen, ret, _expected);
        fail += 1;
        return;
    }
    if (ret == -1) {
        if (errno != _errno) {
            fprintf(stderr,
                    "KO: basename_r(\"%s\", <buff>, %d) returned -1 with errno=%d (expected %d)\n",
                    _input, _bufflen, errno, _errno);
            fail += 1;
            return;
        }
    }
    else if (_buff != NULL && memcmp( _buff, _expected_content, ret ) ) {
        fprintf(stderr,
                "KO: basename_r(\"%s\", <buff>, %d) returned \"%s\", expected \"%s\"\n",
                _input, _bufflen, _buff, _expected_content );
        fail += 1;
        return;
    }
    printf("OK: basename_r(\"%s\", <buff>, %d) returned \"%s\"\n",
            _input, _bufflen, _expected_content );
}

static void
test_dirname_r(char*  _input, const char*  _expected_content, int  _expected, char*  _buff, size_t  _bufflen, int  _errno)
{
    int   ret;
    errno = 0;
    ret   = dirname_r(_input, _buff, _bufflen );
    if (ret != _expected) {
        fprintf(stderr,
                "KO: dirname_r(\"%s\", <buff>, %d) returned %d (expected %d)\n",
                _input, _bufflen, ret, _expected);
        fail += 1;
        return;
    }
    if (ret == -1) {
        if (errno != _errno) {
            fprintf(stderr,
                    "KO: dirname_r(\"%s\", <buff>, %d) returned -1 with errno=%d (expected %d)\n",
                    _input, _bufflen, errno, _errno);
            fail += 1;
            return;
        }
    }
    else if (_buff != NULL &&  memcmp( _buff, _expected_content, ret ) ) {
        fprintf(stderr,
                "KO: dirname_r(\"%s\", <buff>, %d) returned \"%s\", expected \"%s\"\n",
                _input, _bufflen, _buff, _expected_content );
        fail += 1;
        return;
    }
    printf("OK: dirname_r(\"%s\", <buff>, %d) returned \"%s\"\n",
            _input, _bufflen, _expected_content );
}
#endif


static void
test_dirname(char*  _input, const char*  _expected, int  _errno)
{
    char   temp[256], *input = _input;
    char*  ret;
#if HOST
    /* GLibc does modify the input string. bummer */
    if (_input) {
        strcpy(temp, _input);
        input = temp;
    }
#endif
    errno = 0;
    ret   = dirname(input);
    if (_expected == NULL) {
        if (ret != NULL) {
            fprintf(stderr,
                    "KO: dirname(\"%s\") returned \"%s\", NULL expected)\n",
                    _input, ret);
            fail += 1;
        } else if (errno != _errno) {
            fprintf(stderr,
                    "KO: dirname(\"%s\") returned NULL with error: %d (%d expected)\n",
                    _input, errno, _errno);
            fail += 1;
        } else {
            printf( "OK: dirname(\"%s\") returned NULL with error %d\n",
                    _input, _errno );
        }
    } else {
        if (ret == NULL) {
            fprintf(stderr, "KO: dirname(\"%s\") returned NULL with error %d\n",
                    _input, errno);
            fail += 1;
        }
        else if (strcmp(ret, _expected)) {
            fprintf(stderr, "KO: dirname(\"%s\") returned \"%s\", instead of \"%s\"\n",
                    _input, ret, _expected);
        }
        else {
            printf( "OK: dirname(\"%s\") returned \"%s\"\n",
                    _input, ret );
        }
    }
}




int  main( void )
{
    char  buff[256];

    test_basename( "", ".", 0 );
    test_basename( "/usr/lib", "lib", 0 );
    test_basename( "/usr/", "usr", 0 );
    test_basename( "usr", "usr", 0 );
    test_basename( "/", "/", 0 );
    test_basename( ".", ".", 0 );
    test_basename( "..", "..", 0 );

#if !HOST
    test_basename_r( "", ".",  1, NULL, 0, 0 );
    test_basename_r( "", ".", -1, buff, 0, ERANGE );
    test_basename_r( "", ".", -1, buff, 1, ERANGE );
    test_basename_r( "", ".", 1, buff, 2, 0 );
    test_basename_r( "", ".", 1, buff, sizeof(buff), 0 );
    test_basename_r( "/usr/lib", "lib", 3, buff, sizeof(buff), 0 );
    test_basename_r( "/usr/", "usr", 3, buff, sizeof(buff), 0 );
    test_basename_r( "usr", "usr", 3, buff, sizeof(buff), 0 );
    test_basename_r( "/", "/", 1, buff, sizeof(buff), 0 );
    test_basename_r( ".", ".", 1, buff, sizeof(buff), 0 );
    test_basename_r( "..", "..", 2, buff, sizeof(buff), 0 );
#endif

    test_dirname( "", ".", 0 );
    test_dirname( "/usr/lib", "/usr", 0 );
    test_dirname( "/usr/", "/", 0 );
    test_dirname( "usr", ".", 0 );
    test_dirname( ".", ".", 0 );
    test_dirname( "..", ".", 0 );

#if !HOST
    test_dirname_r( "", ".",  1, NULL, 0, 0 );
    test_dirname_r( "", ".", -1, buff, 0, ERANGE );
    test_dirname_r( "", ".", -1, buff, 1, ERANGE );
    test_dirname_r( "", ".", 1, buff, 2, 0 );
    test_dirname_r( "/usr/lib", "/usr", 4, buff, sizeof(buff), 0 );
    test_dirname_r( "/usr/", "/", 1, buff, sizeof(buff), 0 );
    test_dirname_r( "usr", ".", 1, buff, sizeof(buff), 0 );
    test_dirname_r( ".", ".", 1, buff, sizeof(buff), 0 );
    test_dirname_r( "..", ".", 1, buff, sizeof(buff), 0 );
#endif

    return (fail > 0);
}

