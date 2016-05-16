


/*-----------------------------------------------------------------------------------
                                   DEBUG CORNER
------------------------------------------------------------------------------------*/
#ifdef DAL_TRACE
#include <stdio.h>

#define MAX_TRACE_BUFFER    150

#define DAL_PRINT( str )  phOsalNfc_DbgString(str)
#define DAL_DEBUG(str, arg)     \
{                                       \
    char        trace[MAX_TRACE_BUFFER];                    \
    snprintf(trace,MAX_TRACE_BUFFER,str,arg);   \
    phOsalNfc_DbgString(trace);                 \
}

#define DAL_PRINT_BUFFER(msg,buf,len)       \
{                                       \
    uint16_t    i = 0;                  \
    char        trace[MAX_TRACE_BUFFER];                    \
    snprintf(trace,MAX_TRACE_BUFFER,"\n\t %s:",msg);    \
    phOsalNfc_DbgString(trace);                 \
    phOsalNfc_DbgTrace(buf,len);            \
    phOsalNfc_DbgString("\r");              \
}

#define DAL_ASSERT_STR(x, str)   { if (!(x)) { phOsalNfc_DbgString(str); while(1); } }

#else
#define DAL_PRINT( str )
#define DAL_DEBUG(str, arg)
#define DAL_PRINT_BUFFER(msg,buf,len)
#define DAL_ASSERT_STR(x, str)

#endif

