/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-------------------------------------------------------------------*/
#include "includes.h"
#include "scanmerge.h"
#include "shlist.h"

#define IS_HIDDEN_AP(a)	(((a)->ssid_len == 0) || ((a)->ssid[0] == '\0'))

scan_ssid_t *scan_get_ssid( scan_result_t *res_ptr )
{
    static scan_ssid_t ssid_temp;
#ifdef WPA_SUPPLICANT_VER_0_6_X
    const u8 *res_ie;

    res_ie = wpa_scan_get_ie(res_ptr, WLAN_EID_SSID);
    if (!res_ie)
        return NULL;
    ssid_temp.ssid_len = (size_t)res_ie[1];
    os_memcpy(ssid_temp.ssid, (res_ie + 2), ssid_temp.ssid_len);
#else
    ssid_temp.ssid_len = res_ptr->ssid_len;
    os_memcpy(ssid_temp.ssid, res_ptr->ssid, ssid_temp.ssid_len);
#endif
    return &ssid_temp;
}

/*-----------------------------------------------------------------------------
Routine Name: scan_init
Routine Description: Inits scan merge list
Arguments:
   mydrv   - pointer to private driver data structure
Return Value:
-----------------------------------------------------------------------------*/
void scan_init( struct wpa_driver_ti_data *mydrv )
{
    mydrv->last_scan = -1;
    shListInitList(&(mydrv->scan_merge_list));
}

/*-----------------------------------------------------------------------------
Routine Name: scan_free
Routine Description: Frees scan structure private data
Arguments:
   ptr - pointer to private data structure
Return Value:
-----------------------------------------------------------------------------*/
static void scan_free( void *ptr )
{
    os_free(ptr);
}

/*-----------------------------------------------------------------------------
Routine Name: scan_exit
Routine Description: Cleans scan merge list
Arguments:
   mydrv   - pointer to private driver data structure
Return Value:
-----------------------------------------------------------------------------*/
void scan_exit( struct wpa_driver_ti_data *mydrv )
{
    shListDelAllItems(&(mydrv->scan_merge_list), scan_free);
}

/*-----------------------------------------------------------------------------
Routine Name: scan_count
Routine Description: Gives number of list elements
Arguments:
   mydrv   - pointer to private driver data structure
Return Value: Number of elements in the list
-----------------------------------------------------------------------------*/
unsigned long scan_count( struct wpa_driver_ti_data *mydrv )
{
    return shListGetCount(&(mydrv->scan_merge_list));
}

/*-----------------------------------------------------------------------------
Routine Name: scan_equal
Routine Description: Compares bssid of scan result and scan merge structure
Arguments:
   val   - pointer to scan result structure
   idata - pointer to scan merge structure
Return Value: 1 - if equal, 0 - if not
-----------------------------------------------------------------------------*/
static int scan_equal( void *val,  void *idata )
{
    scan_ssid_t n_ssid, l_ssid, *p_ssid;
    scan_result_t *new_res = (scan_result_t *)val;
    scan_result_t *lst_res =
               (scan_result_t *)(&(((scan_merge_t *)idata)->scanres));
    int ret;
    size_t len;

    p_ssid = scan_get_ssid(new_res);
    if (!p_ssid)
        return 0;
    os_memcpy(&n_ssid, p_ssid, sizeof(scan_ssid_t));
    p_ssid = scan_get_ssid(lst_res);
    if (!p_ssid)
        return 0;
    os_memcpy(&l_ssid, p_ssid, sizeof(scan_ssid_t));

    len = (IS_HIDDEN_AP(&n_ssid) || IS_HIDDEN_AP(&l_ssid)) ?
          0 : n_ssid.ssid_len;
    ret = ((l_ssid.ssid_len != n_ssid.ssid_len) && (len != 0)) ||
          (os_memcmp(new_res->bssid, lst_res->bssid, ETH_ALEN) ||
           os_memcmp(n_ssid.ssid, l_ssid.ssid, len));
    return !ret;
}

/*-----------------------------------------------------------------------------
Routine Name: copy_scan_res
Routine Description: copies scan result structure to scan merge list item
Arguments:
   dst - pointer to scan result structure in the list
   src - source pointer to scan result structure
Return Value: NONE
-----------------------------------------------------------------------------*/
void copy_scan_res( scan_result_t *dst, scan_result_t *src )
{
#ifdef WPA_SUPPLICANT_VER_0_5_X
    if( IS_HIDDEN_AP(src) ) {
        os_memcpy(src->ssid, dst->ssid, dst->ssid_len);
        src->ssid_len = dst->ssid_len;
    }
#endif
    os_memcpy(dst, src, sizeof(scan_result_t));
}

/*-----------------------------------------------------------------------------
Routine Name: scan_add
Routine Description: adds scan result structure to scan merge list
Arguments:
   head    - pointer to scan merge list head
   res_ptr - pointer to scan result structure
Return Value: Pointer to scan merge item
-----------------------------------------------------------------------------*/
static scan_merge_t *scan_add( SHLIST *head, scan_result_t *res_ptr )
{
    scan_merge_t *scan_ptr;
    unsigned size = 0;

#ifdef WPA_SUPPLICANT_VER_0_6_X
    size += res_ptr->ie_len;
#endif
    scan_ptr = (scan_merge_t *)os_malloc(sizeof(scan_merge_t) + size);
    if( !scan_ptr )
        return( NULL );
    os_memcpy(&(scan_ptr->scanres), res_ptr, sizeof(scan_result_t) + size);
    scan_ptr->count = SCAN_MERGE_COUNT;
    shListInsLastItem(head, (void *)scan_ptr);
    return scan_ptr;
}

/*-----------------------------------------------------------------------------
Routine Name: scan_find
Routine Description: Looks for scan merge item in scan results array
Arguments:
   scan_ptr - pointer to scan merge item
   results - pointer to scan results array
   number_items - current number of items
Return Value: 1 - if item was found, 0 - otherwise
-----------------------------------------------------------------------------*/
static int scan_find( scan_merge_t *scan_ptr, scan_result_t *results,
                      unsigned int number_items )
{
    unsigned int i;

    for(i=0;( i < number_items );i++) {
        if( scan_equal(&(results[i]), scan_ptr) )
            return 1;
    }
    return 0;
}

#ifdef WPA_SUPPLICANT_VER_0_6_X
/*-----------------------------------------------------------------------------
Routine Name: scan_dup
Routine Description: Create copy of scan results entry
Arguments:
   res_ptr - pointer to scan result item
Return Value: pointer to new scan result item, or NULL
-----------------------------------------------------------------------------*/
static scan_result_t *scan_dup( scan_result_t *res_ptr )
{
    unsigned size;
    scan_result_t *new_ptr;

    if (!res_ptr)
        return NULL;

    size = sizeof(scan_result_t) + res_ptr->ie_len;
    new_ptr = os_malloc(size);
    if (!new_ptr)
        return NULL;
    if (res_ptr) {
        os_memcpy(new_ptr, res_ptr, size);
    }
    return new_ptr;
}
#endif

/*-----------------------------------------------------------------------------
Routine Name: scan_merge
Routine Description: Merges current scan results with previous
Arguments:
   mydrv   - pointer to private driver data structure
   results - pointer to scan results array
   number_items - current number of items
   max_size - maximum namber of items
Return Value: Merged number of items
-----------------------------------------------------------------------------*/
#ifdef WPA_SUPPLICANT_VER_0_6_X
unsigned int scan_merge( struct wpa_driver_ti_data *mydrv,
                         scan_result_t **results, int force_flag,
                         unsigned int number_items, unsigned int max_size )
#else
unsigned int scan_merge( struct wpa_driver_ti_data *mydrv,
                         scan_result_t *results, int force_flag,
                         unsigned int number_items, unsigned int max_size )
#endif
{
    SHLIST *head = &(mydrv->scan_merge_list);
    SHLIST *item, *del_item;
    scan_result_t *res_ptr;
    scan_merge_t *scan_ptr;
    unsigned int i;

    /* Prepare items for removal */
    item = shListGetFirstItem(head);
    while( item != NULL ) {
        scan_ptr = (scan_merge_t *)(item->data);
        if( scan_ptr->count != 0 )
            scan_ptr->count--;
        item = shListGetNextItem(head, item);
    }

    for(i=0;( i < number_items );i++) { /* Find/Add new items */
#ifdef WPA_SUPPLICANT_VER_0_6_X
        res_ptr = results[i];
#else
        res_ptr = &(results[i]);
#endif
        item = shListFindItem( head, res_ptr, scan_equal );
        if( item ) {
#ifdef WPA_SUPPLICANT_VER_0_6_X
            scan_ssid_t *p_ssid;
            scan_result_t *new_ptr;
#endif
            scan_ptr = (scan_merge_t *)(item->data);
            copy_scan_res(&(scan_ptr->scanres), res_ptr);
            scan_ptr->count = SCAN_MERGE_COUNT;
#ifdef WPA_SUPPLICANT_VER_0_6_X
	    p_ssid = scan_get_ssid(res_ptr);
            if (p_ssid && IS_HIDDEN_AP(p_ssid)) {
                new_ptr = scan_dup(res_ptr);
                if (new_ptr) {
                    results[i] = new_ptr;
                    os_free(res_ptr);
                }
            }
#endif
        }
        else {
            scan_add(head, res_ptr);
        }
    }

    item = shListGetFirstItem( head );  /* Add/Remove missing items */
    while( item != NULL ) {
        del_item = NULL;
        scan_ptr = (scan_merge_t *)(item->data);
        if( scan_ptr->count != SCAN_MERGE_COUNT ) {
            if( !force_flag && ((scan_ptr->count == 0) ||
                (mydrv->last_scan == SCAN_TYPE_NORMAL_ACTIVE)) ) {
                del_item = item;
            }
            else {
                if( number_items < max_size ) {
#ifdef WPA_SUPPLICANT_VER_0_6_X
                    res_ptr = scan_dup(&(scan_ptr->scanres));
                    if (res_ptr) {
                        results[number_items] = res_ptr;
                        number_items++;
                    }
#else
                    os_memcpy(&(results[number_items]),
                          &(scan_ptr->scanres), sizeof(scan_result_t));
                    number_items++;
#endif
                }
            }
        }
        item = shListGetNextItem(head, item);
        shListDelItem(head, del_item, scan_free);
    }

    return( number_items );
}

/*-----------------------------------------------------------------------------
Routine Name: scan_get_by_bssid
Routine Description: Gets scan_result pointer to item by bssid
Arguments:
   mydrv   - pointer to private driver data structure
   bssid   - pointer to bssid value
Return Value: pointer to scan_result item
-----------------------------------------------------------------------------*/
scan_result_t *scan_get_by_bssid( struct wpa_driver_ti_data *mydrv, u8 *bssid )
{
    SHLIST *head = &(mydrv->scan_merge_list);
    SHLIST *item;
    scan_result_t *cur_res;
    scan_ssid_t *p_ssid;

    item = shListGetFirstItem(head);
    if( item == NULL )
        return( NULL );
    do {
        cur_res = (scan_result_t *)&(((scan_merge_t *)(item->data))->scanres);
        p_ssid = scan_get_ssid(cur_res);
        if( (!os_memcmp(cur_res->bssid, bssid, ETH_ALEN)) &&
            (!IS_HIDDEN_AP(p_ssid)) ) {
            return( cur_res );
        }
        item = shListGetNextItem(head, item);
    } while( item != NULL );

    return( NULL );
}
