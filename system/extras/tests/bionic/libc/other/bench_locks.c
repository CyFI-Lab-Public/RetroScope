/* a small program to benchmark locking primitives with different implementations */

#include <pthread.h>
#include <sys/time.h>
#include <stdio.h>

static double  now(void)
{
    struct timeval   tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec + tv.tv_usec/1000000.0;
}

int  main( void )
{
    double             t0, t1;
    pthread_mutex_t    lock1 = PTHREAD_MUTEX_INITIALIZER;
    int volatile       lock2 = 0;
    long               count;
    const long         ITERATIONS = 1000000;

    /* pthread_mutex_lock */
    t0 = now();
    for (count = ITERATIONS; count > 0; count--) {
        pthread_mutex_lock(&lock1);
        pthread_mutex_unlock(&lock1);
    }
    t1 = now() - t0;
    printf( "pthread_mutex_lock/unlock:  %.5g us/op\n", (t1*1000000.0)/ITERATIONS );

    return 0;
}
