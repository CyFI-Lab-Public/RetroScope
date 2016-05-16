/******************************************************************************
 *
 *  Copyright (C) 2009-2013 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
#include "bt_target.h"

#if (defined BLE_INCLUDED && BLE_INCLUDED == TRUE)

#include <string.h>
#include "gap_int.h"
#include "gap_api.h"
#include "gattdefs.h"
#include "gatt_api.h"
#include "gatt_int.h"
#include "btm_int.h"
#include "hcimsgs.h"

#define GAP_CHAR_ICON_SIZE          2
#define GAP_CHAR_DEV_NAME_SIZE      248
#define GAP_BLE_PRIVACY_FLAG_SIZE    1

#define GAP_MAX_NUM_INC_SVR       0
#define GAP_MAX_ATTR_NUM          (2 * GAP_MAX_CHAR_NUM + GAP_MAX_NUM_INC_SVR + 1)
#define GAP_MAX_CHAR_VALUE_SIZE   (30 + GAP_CHAR_DEV_NAME_SIZE)


#ifndef GAP_ATTR_DB_SIZE
#define GAP_ATTR_DB_SIZE      GATT_DB_MEM_SIZE(GAP_MAX_NUM_INC_SVR, GAP_MAX_CHAR_NUM, GAP_MAX_CHAR_VALUE_SIZE)
#endif

/* privacy flag readable and writable with encryption on */
#ifndef GAP_BLE_PRIVACY_FLAG_PERM
#define GAP_BLE_PRIVACY_FLAG_PERM       (GATT_PERM_READ|GATT_PERM_WRITE)
#endif

#define GATT_READ_GAP_PRIVACY_FLAG      1
#define GATT_SET_GAP_PRIVACY_FLAG       2
#define GATT_READ_GAP_REMOTE_NAME       3
#define GATT_UPDATE_RECONN_ADDR         4

#define GAP_BLE_PRIVACY_UNKNOWN         0xff

static void gap_ble_s_attr_request_cback (UINT16 conn_id, UINT32 trans_id, tGATTS_REQ_TYPE op_code, tGATTS_DATA *p_data);

/* client connection callback */
static void  gap_ble_c_connect_cback (tGATT_IF gatt_if, BD_ADDR bda, UINT16 conn_id, BOOLEAN connected, tGATT_DISCONN_REASON reason);
static void  gap_ble_c_cmpl_cback (UINT16 conn_id, tGATTC_OPTYPE op, tGATT_STATUS status, tGATT_CL_COMPLETE *p_data);

static tGATT_CBACK gap_cback =
{
    gap_ble_c_connect_cback,
    gap_ble_c_cmpl_cback,
    NULL,
    NULL,
    gap_ble_s_attr_request_cback
};



/*******************************************************************************
**
** Function         gap_find_clcb_by_bd_addr
**
** Description      The function searches all LCB with macthing bd address
**
** Returns          total number of clcb found.
**
*******************************************************************************/
tGAP_CLCB *gap_find_clcb_by_bd_addr(BD_ADDR bda)
{
    UINT8 i_clcb;
    tGAP_CLCB    *p_clcb = NULL;

    for (i_clcb = 0, p_clcb= gap_cb.clcb; i_clcb < GAP_MAX_CL; i_clcb++, p_clcb++)
    {
        if (p_clcb->in_use && !memcmp(p_clcb->bda, bda, BD_ADDR_LEN))
        {
            return p_clcb;
        }
    }

    return NULL;
}

/*******************************************************************************
**
** Function         gap_ble_find_clcb_by_conn_id
**
** Description      The function searches all LCB with macthing connection ID
**
** Returns          total number of clcb found.
**
*******************************************************************************/
tGAP_CLCB *gap_ble_find_clcb_by_conn_id(UINT16 conn_id)
{
    UINT8 i_clcb;
    tGAP_CLCB    *p_clcb = NULL;

    for (i_clcb = 0, p_clcb= gap_cb.clcb; i_clcb < GAP_MAX_CL; i_clcb++, p_clcb++)
    {
        if (p_clcb->in_use && p_clcb->connected && p_clcb->conn_id == conn_id)
        {
            return p_clcb;
        }
    }

    return p_clcb;
}

/*******************************************************************************
**
** Function         gap_clcb_alloc
**
** Description      The function allocates a GAP  connection link control block
**
** Returns           NULL if not found. Otherwise pointer to the connection link block.
**
*******************************************************************************/
tGAP_CLCB *gap_clcb_alloc (UINT16 conn_id, BD_ADDR bda)
{
    UINT8         i_clcb = 0;
    tGAP_CLCB    *p_clcb = NULL;

    for (i_clcb = 0, p_clcb= gap_cb.clcb; i_clcb < GAP_MAX_CL; i_clcb++, p_clcb++)
    {
        if (!p_clcb->in_use)
        {
            p_clcb->in_use      = TRUE;
            p_clcb->conn_id     = conn_id;
            p_clcb->connected   = TRUE;
            memcpy (p_clcb->bda, bda, BD_ADDR_LEN);
            break;
        }
    }
    return p_clcb;
}

/*******************************************************************************
**
** Function         gap_find_alloc_clcb
**
** Description      The function find or allocates a GAP  connection link control block
**
** Returns           NULL if not found. Otherwise pointer to the connection link block.
**
*******************************************************************************/
tGAP_CLCB *gap_find_alloc_clcb (UINT16 conn_id, BD_ADDR bda)
{
    UINT8         i_clcb = 0;
    tGAP_CLCB    *p_clcb = NULL;

    for (i_clcb = 0, p_clcb= gap_cb.clcb; i_clcb < GAP_MAX_CL; i_clcb++, p_clcb++)
    {
        if (!p_clcb->in_use)
        {
            p_clcb->in_use      = TRUE;
            p_clcb->conn_id     = conn_id;
            p_clcb->connected   = TRUE;
            memcpy (p_clcb->bda, bda, BD_ADDR_LEN);
            break;
        }
    }
    return p_clcb;
}

/*******************************************************************************
**
** Function         gap_get_conn_id_if_connected
**
** Description      This function returns a connecttion handle to a ATT server
**                  if the server is already connected
**
** Parameters       client_if: client interface.
**                  bd_addr: peer device address.
**
** Returns          Connection handle or invalid handle value
**
*******************************************************************************/
UINT16 gap_get_conn_id_if_connected (BD_ADDR bd_addr)
{
    tGAP_CLCB       *p_clcb;
    UINT16          i;

    GAP_TRACE_EVENT2 ("gap_get_conn_id_if_connected() - BDA: %08x%04x ",
                      (bd_addr[0]<<24)+(bd_addr[1]<<16)+(bd_addr[2]<<8)+bd_addr[3],
                      (bd_addr[4]<<8)+bd_addr[5]);

    for (i = 0, p_clcb = gap_cb.clcb; i < GAP_MAX_CL; i++, p_clcb++)
    {
        if (p_clcb->in_use && p_clcb->connected && !memcmp(p_clcb->bda, bd_addr,  BD_ADDR_LEN) )
        {
            return(p_clcb->conn_id);
        }
    }

    /* If here, failed to allocate a client control block */
    GATT_TRACE_DEBUG0 ("gap_get_conn_id_if_connected: not connected");
    return(GATT_INVALID_CONN_ID);
}

/*******************************************************************************
**
** Function         gap_ble_enqueue_op
**
** Description      enqueue a GAP operation when GAP client is busy
**
** Returns          void
**
*******************************************************************************/
void gap_ble_enqueue_op( tGAP_CLCB * p_clcb, UINT8 op, BD_ADDR reconn_addr, UINT8 privacy_flag, void *p_cback)
{
    tGAP_BLE_PENDING_OP  *p_op = (tGAP_BLE_PENDING_OP *)GKI_getbuf(sizeof(tGAP_BLE_PENDING_OP));

    if (p_op != NULL)
    {
        p_op->op = op;
        p_op->p_pending_cback = p_cback;

        if (op == GATT_SET_GAP_PRIVACY_FLAG)
            p_op->pending_data.privacy_flag = privacy_flag;
        else if (op == GATT_UPDATE_RECONN_ADDR)
            memcpy(p_op->pending_data.reconn_addr, reconn_addr, BD_ADDR_LEN);

        GKI_enqueue(&p_clcb->pending_op_q, p_op);
    }
}

/*******************************************************************************
**
** Function         gap_ble_process_pending_op
**
** Description      get next pending operation and process it
**
** Returns          void
**
*******************************************************************************/
static BOOLEAN gap_ble_process_pending_op(tGAP_CLCB *p_clcb)
{
    tGAP_BLE_PENDING_OP *p_pending_op = (tGAP_BLE_PENDING_OP *)GKI_dequeue(&p_clcb->pending_op_q);
    BOOLEAN         started = FALSE;

    if (p_pending_op != NULL)
    {
        if (p_pending_op->op == GATT_UPDATE_RECONN_ADDR)
        {
            GAP_BleUpdateReconnectAddr( p_clcb->bda,
                                        p_pending_op->pending_data.reconn_addr,
                                        (tGAP_BLE_RECONN_ADDR_CBACK *)p_pending_op->p_pending_cback);
            started = TRUE;
        }
        GKI_freebuf(p_pending_op);
    }
    else
    {
        GAP_TRACE_EVENT0("No pending operation");
    }

    return started;
}

/*******************************************************************************
**   GAP Attributes Database Request callback
*******************************************************************************/
tGATT_STATUS gap_read_attr_value (UINT16 handle, tGATT_VALUE *p_value, BOOLEAN is_long)
{
    tGAP_ATTR   *p_db_attr = gap_cb.gatt_attr;
    UINT8       *p = p_value->value, i;
    UINT16      offset = p_value->offset;
    UINT8       *p_dev_name = NULL;

    for (i = 0; i < GAP_MAX_CHAR_NUM; i ++, p_db_attr ++)
    {
        if (handle == p_db_attr->handle)
        {
            if (p_db_attr->uuid != GATT_UUID_GAP_DEVICE_NAME &&
                is_long == TRUE)
                return GATT_NOT_LONG;

            switch (p_db_attr->uuid)
            {
                case GATT_UUID_GAP_DEVICE_NAME:
                    BTM_ReadLocalDeviceName((char **)&p_dev_name);
                    if (strlen ((char *)p_dev_name) > GATT_MAX_ATTR_LEN)
                        p_value->len = GATT_MAX_ATTR_LEN;
                    else
                        p_value->len = (UINT16)strlen ((char *)p_dev_name);

                    if (offset > p_value->len)
                        return GATT_INVALID_OFFSET;
                    else
                    {
                        p_value->len -= offset;
                        p_dev_name += offset;
                        ARRAY_TO_STREAM(p, p_dev_name, p_value->len);
                        GAP_TRACE_EVENT1("GATT_UUID_GAP_DEVICE_NAME len=0x%04x", p_value->len);
                    }
                    break;

                case GATT_UUID_GAP_ICON:
                    UINT16_TO_STREAM(p, p_db_attr->attr_value.icon);
                    p_value->len = 2;
                    break;

                case GATT_UUID_GAP_PRIVACY_FLAG:
                    UINT8_TO_STREAM(p, p_db_attr->attr_value.privacy);
                    p_value->len = 1;
                    break;

                case GATT_UUID_GAP_RECONN_ADDR:
                    p_value->len = BD_ADDR_LEN;
                    BDADDR_TO_STREAM(p, p_db_attr->attr_value.reconn_bda);
                    break;

                case GATT_UUID_GAP_PREF_CONN_PARAM:
                    UINT16_TO_STREAM(p, p_db_attr->attr_value.conn_param.int_min); /* int_min */
                    UINT16_TO_STREAM(p, p_db_attr->attr_value.conn_param.int_max); /* int_max */
                    UINT16_TO_STREAM(p, p_db_attr->attr_value.conn_param.latency); /* latency */
                    UINT16_TO_STREAM(p, p_db_attr->attr_value.conn_param.sp_tout);  /* sp_tout */
                    p_value->len =8;
                    break;
            }
            return GATT_SUCCESS;
        }
    }
    return GATT_NOT_FOUND;
}

/*******************************************************************************
**   GAP Attributes Database Read/Read Blob Request process
*******************************************************************************/
tGATT_STATUS gap_proc_read (tGATTS_REQ_TYPE type, tGATT_READ_REQ *p_data, tGATTS_RSP *p_rsp)
{
    tGATT_STATUS    status = GATT_NO_RESOURCES;

    if (p_data->is_long)
        p_rsp->attr_value.offset = p_data->offset;

    p_rsp->attr_value.handle = p_data->handle;

    status = gap_read_attr_value(p_data->handle, &p_rsp->attr_value, p_data->is_long);

    return status;
}
BOOLEAN gap_read_local_reconn_addr(BD_ADDR_PTR reconn_bda)
{
    BD_ADDR dummy_bda = {0};

    if (memcmp(gap_cb.reconn_bda, dummy_bda, BD_ADDR_LEN) != 0)
    {
        memcpy(reconn_bda, gap_cb.reconn_bda, BD_ADDR_LEN);
        return TRUE;
    }
    else
        return FALSE;
}

/******************************************************************************
**
** Function         gap_proc_write_req
**
** Description      GAP ATT server process a write request.
**
** Returns          void.
**
*******************************************************************************/
UINT8 gap_proc_write_req( tGATTS_REQ_TYPE type, tGATT_WRITE_REQ *p_data)
{
    tGAP_ATTR   *p_db_attr = gap_cb.gatt_attr;
    UINT8   i;

    for (i = 0; i < GAP_MAX_CHAR_NUM; i ++, p_db_attr ++)
    {
        if (p_data-> handle == p_db_attr->handle)
        {
            if (p_data->offset != 0) return GATT_NOT_LONG;
            if (p_data->is_prep) return GATT_REQ_NOT_SUPPORTED;

/* DO NOT SUPPORT RECONNECTION ADDRESS FOR NOW

            if (p_db_attr->uuid == GATT_UUID_GAP_RECONN_ADDR)
            {
                if (!btm_cb.ble_ctr_cb.privacy)
                    return GATT_WRITE_NOT_PERMIT;
                if (p_data->len != BD_ADDR_LEN) return GATT_INVALID_ATTR_LEN;

                STREAM_TO_BDADDR(p_db_attr->attr_value.reconn_bda, p);
                // write direct connection address
                memcpy(&gap_cb.reconn_bda, p_db_attr->attr_value.reconn_bda, BD_ADDR_LEN);

                return GATT_SUCCESS;
            }
            else
*/
            return GATT_WRITE_NOT_PERMIT;
        }
    }
    return GATT_NOT_FOUND;

}

/******************************************************************************
**
** Function         gap_ble_s_attr_request_cback
**
** Description      GAP ATT server attribute access request callback.
**
** Returns          void.
**
*******************************************************************************/
void gap_ble_s_attr_request_cback (UINT16 conn_id, UINT32 trans_id,
                                   tGATTS_REQ_TYPE type, tGATTS_DATA *p_data)
{
    UINT8       status = GATT_INVALID_PDU;
    tGATTS_RSP  rsp_msg;
    BOOLEAN     ignore = FALSE;

    GAP_TRACE_EVENT1("gap_ble_s_attr_request_cback : recv type (0x%02x)", type);

    memset(&rsp_msg, 0, sizeof(tGATTS_RSP));

    switch (type)
    {
        case GATTS_REQ_TYPE_READ:
            status = gap_proc_read(type, &p_data->read_req, &rsp_msg);
            break;

        case GATTS_REQ_TYPE_WRITE:
            if (!p_data->write_req.need_rsp)
                ignore = TRUE;

            status = gap_proc_write_req(type, &p_data->write_req);
            break;

        case GATTS_REQ_TYPE_WRITE_EXEC:
            ignore = TRUE;
            GAP_TRACE_EVENT0("Ignore GATTS_REQ_TYPE_WRITE_EXEC"  );
            break;

        case GATTS_REQ_TYPE_MTU:
            GAP_TRACE_EVENT1("Get MTU exchange new mtu size: %d", p_data->mtu);
            ignore = TRUE;
            break;

        default:
            GAP_TRACE_EVENT1("Unknown/unexpected LE GAP ATT request: 0x%02x", type);
            break;
    }

    if (!ignore)
        GATTS_SendRsp (conn_id, trans_id, status, &rsp_msg);
}

/*******************************************************************************
**
** Function         btm_ble_att_db_init
**
** Description      GAP ATT database initalization.
**
** Returns          void.
**
*******************************************************************************/
void gap_attr_db_init(void)
{
    tBT_UUID        app_uuid = {LEN_UUID_128,{0}};
    tBT_UUID        uuid     = {LEN_UUID_16,{UUID_SERVCLASS_GAP_SERVER}};
    UINT16          service_handle;
    tGAP_ATTR       *p_db_attr = &gap_cb.gatt_attr[0];
    tGATT_STATUS    status;

    /* Fill our internal UUID with a fixed pattern 0x82 */
    memset (&app_uuid.uu.uuid128, 0x82, LEN_UUID_128);
    memset(gap_cb.gatt_attr, 0, sizeof(tGAP_ATTR) *GAP_MAX_CHAR_NUM);

    gap_cb.gatt_if = GATT_Register(&app_uuid, &gap_cback);

    GATT_StartIf(gap_cb.gatt_if);

    /* Create a GAP service */
    service_handle = GATTS_CreateService (gap_cb.gatt_if, &uuid, 0, GAP_MAX_ATTR_NUM, TRUE);

    GAP_TRACE_EVENT1 ("gap_attr_db_init service_handle = %d", service_handle);

    /* add Device Name Characteristic
    */
    uuid.len = LEN_UUID_16;
    uuid.uu.uuid16 = p_db_attr->uuid = GATT_UUID_GAP_DEVICE_NAME;
    p_db_attr->handle = GATTS_AddCharacteristic(service_handle, &uuid, GATT_PERM_READ, GATT_CHAR_PROP_BIT_READ);
    p_db_attr ++;

    /* add Icon characteristic
    */
    uuid.uu.uuid16   = p_db_attr->uuid = GATT_UUID_GAP_ICON;
    p_db_attr->handle = GATTS_AddCharacteristic(service_handle,
                                                &uuid,
                                                GATT_PERM_READ,
                                                GATT_CHAR_PROP_BIT_READ);
    p_db_attr ++;

    /* start service now */
    memset (&app_uuid.uu.uuid128, 0x81, LEN_UUID_128);

    status = GATTS_StartService(gap_cb.gatt_if, service_handle, GAP_TRANSPORT_SUPPORTED );

    GAP_TRACE_EVENT3 ("GAP App gatt_if: %d  s_hdl = %d start_status=%d",
                      gap_cb.gatt_if, service_handle, status);



}

/*******************************************************************************
**
** Function         GAP_BleAttrDBUpdate
**
** Description      GAP ATT database update.
**
** Returns          void.
**
*******************************************************************************/
void GAP_BleAttrDBUpdate(UINT16 attr_uuid, tGAP_BLE_ATTR_VALUE *p_value)
{
    tGAP_ATTR  *p_db_attr = gap_cb.gatt_attr;
    UINT8       i = 0;

    GAP_TRACE_EVENT1("GAP_BleAttrDBUpdate attr_uuid=0x%04x", attr_uuid);

    for (i = 0; i < GAP_MAX_CHAR_NUM; i ++, p_db_attr ++)
    {
        if (p_db_attr->uuid == attr_uuid)
        {
            GAP_TRACE_EVENT1("Found attr_uuid=0x%04x", attr_uuid);

            switch (attr_uuid)
            {
            case GATT_UUID_GAP_ICON:
                p_db_attr->attr_value.icon  =  p_value->icon;
                break;

            case GATT_UUID_GAP_PREF_CONN_PARAM:
                memcpy((void *)&p_db_attr->attr_value.conn_param, (const void *)&p_value->conn_param, sizeof(tGAP_BLE_PREF_PARAM));
                break;

            case GATT_UUID_GAP_DEVICE_NAME:
                BTM_SetLocalDeviceName((char *)p_value->p_dev_name);
                break;

            }
            break;
        }
    }

    return;
}

/*******************************************************************************
**
** Function         gap_ble_cl_op_cmpl
**
** Description      GAP client operation complete callback
**
** Returns          void
**
*******************************************************************************/
void gap_ble_cl_op_cmpl(tGAP_CLCB *p_clcb, BOOLEAN status, UINT16 len, UINT8 *p_name)
{
    tGAP_BLE_DEV_NAME_CBACK *p_dev_name_cback = (tGAP_BLE_DEV_NAME_CBACK *)(p_clcb->p_cback);
    UINT16                  op = p_clcb->cl_op_uuid;

    GAP_TRACE_EVENT1("gap_ble_cl_op_cmpl status: %d", status);

    p_clcb->cl_op_uuid = 0;
    p_clcb->p_cback=NULL;

    if (p_dev_name_cback)
    {
        GAP_TRACE_EVENT0("calling gap_ble_cl_op_cmpl");

        if (op == GATT_UUID_GAP_DEVICE_NAME)
            (* p_dev_name_cback)(status, p_clcb->bda, len, (char *)p_name);
    }

    if (!gap_ble_process_pending_op(p_clcb) &&
        p_clcb->cl_op_uuid == 0)
        GATT_Disconnect(p_clcb->conn_id);

}

/*******************************************************************************
**
** Function         gap_ble_c_connect_cback
**
** Description      Client connection callback.
**
** Returns          void
**
*******************************************************************************/
static void gap_ble_c_connect_cback (tGATT_IF gatt_if, BD_ADDR bda, UINT16 conn_id,
                                     BOOLEAN connected, tGATT_DISCONN_REASON reason)
{
    tGAP_CLCB   *p_clcb = gap_find_clcb_by_bd_addr (bda);
    UINT16      cl_op_uuid;

    GAP_TRACE_EVENT5 ("gap_ble_c_connect_cback: from %08x%04x connected:%d conn_id=%d reason = 0x%04x",
                      (bda[0]<<24)+(bda[1]<<16)+(bda[2]<<8)+bda[3],
                      (bda[4]<<8)+bda[5], connected, conn_id, reason);


    if (connected)
    {
        if (p_clcb == NULL)
        {
            if ((p_clcb = gap_clcb_alloc(conn_id, bda))== NULL)
            {
                GAP_TRACE_ERROR0 ("gap_ble_c_connect_cback: no_resource");
                return;
            }
        }
        p_clcb->conn_id = conn_id;
        p_clcb->connected = TRUE;

        /* Do not use reconnection address for now -->
          check privacy enabled? set reconnect address
        btm_ble_update_reconnect_address(bda);*/
    }
    else
    {
        if (p_clcb != NULL)
            p_clcb->connected = FALSE;
    }

    if (p_clcb)
    {
        cl_op_uuid = p_clcb->cl_op_uuid;

        GAP_TRACE_EVENT1 ("cl_op_uuid=0x%04x", cl_op_uuid  );

        if (p_clcb->connected)
        {
            p_clcb->cl_op_uuid = 0;
            if (cl_op_uuid == GATT_UUID_GAP_DEVICE_NAME)
            {
                GAP_BleReadPeerDevName (bda, (tGAP_BLE_DEV_NAME_CBACK *)p_clcb->p_cback);
            }
        }
        /* current link disconnect */
        else
        {
            gap_ble_cl_op_cmpl(p_clcb, FALSE, 0, NULL);
            memset(p_clcb, 0, sizeof(tGAP_CLCB));
        }
    }

}

/*******************************************************************************
**
** Function         gap_ble_c_cmpl_cback
**
** Description      Client operation complete callback.
**
** Returns          void
**
*******************************************************************************/
static void gap_ble_c_cmpl_cback (UINT16 conn_id, tGATTC_OPTYPE op, tGATT_STATUS status, tGATT_CL_COMPLETE *p_data)

{
    tGAP_CLCB   *p_clcb = gap_ble_find_clcb_by_conn_id(conn_id);
    UINT16      op_type;
    UINT16      min, max, latency, tout;
    UINT16      len;
    UINT8       *pp;

    if (p_clcb == NULL)
        return;

    op_type = p_clcb->cl_op_uuid;

    GAP_TRACE_EVENT3 ("gap_ble_c_cmpl_cback() - op_code: 0x%02x  status: 0x%02x  read_type: 0x%04x", op, status, op_type);
    /* Currently we only issue read commands */
    if (op != GATTC_OPTYPE_READ && op != GATTC_OPTYPE_WRITE)
        return;

    if (status != GATT_SUCCESS)
    {
        gap_ble_cl_op_cmpl(p_clcb, FALSE, 0, NULL);
        return;
    }

    pp = p_data->att_value.value;

    switch (op_type)
    {
        case GATT_UUID_GAP_PREF_CONN_PARAM:
            GAP_TRACE_EVENT0 ("GATT_UUID_GAP_PREF_CONN_PARAM");
            /* Extract the peripheral preferred connection parameters and save them */

            STREAM_TO_UINT16 (min, pp);
            STREAM_TO_UINT16 (max, pp);
            STREAM_TO_UINT16 (latency, pp);
            STREAM_TO_UINT16 (tout, pp);

            BTM_BleSetPrefConnParams (p_clcb->bda, min, max, latency, tout);
            /* release the connection here */
            gap_ble_cl_op_cmpl(p_clcb, TRUE, 0, NULL);
            break;

        case GATT_UUID_GAP_DEVICE_NAME:
            GAP_TRACE_EVENT0 ("GATT_UUID_GAP_DEVICE_NAME");
            len = (UINT16)strlen((char *)pp);
            if (len > GAP_CHAR_DEV_NAME_SIZE)
                len = GAP_CHAR_DEV_NAME_SIZE;
            gap_ble_cl_op_cmpl(p_clcb, TRUE, len, pp);
            break;
        case GATT_UUID_GAP_ICON:
            break;

    }
}

/*******************************************************************************
**
** Function         gap_ble_cl_read_request
**
** Description      utility function to start a read request for a GAP charactersitic
**
** Returns          TRUE if read started, else FALSE if GAP is busy
**
*******************************************************************************/
BOOLEAN gap_ble_cl_read_request(tGAP_CLCB *p_clcb, UINT16 uuid, void * p_cback)
{
    tGATT_READ_PARAM   param;

    memset(&param, 0, sizeof(tGATT_READ_PARAM));

    param.service.uuid.len       = LEN_UUID_16;
    param.service.uuid.uu.uuid16 = uuid;
    param.service.s_handle       = 1;
    param.service.e_handle       = 0xFFFF;
    param.service.auth_req       = 0;

    if (GATTC_Read(p_clcb->conn_id, GATT_READ_BY_TYPE, &param) != GATT_SUCCESS)
    {
        GAP_TRACE_ERROR0 ("GAP_BleReadPeerPrefConnParams: GATT_Read Failed");
        /* release the link here */
        GATT_Disconnect(p_clcb->conn_id);
        return(FALSE);
    }
    else
    {
        p_clcb->p_cback = p_cback;
        p_clcb->cl_op_uuid = uuid;
        return TRUE;
    }

}

/*******************************************************************************
**
** Function         GAP_BleReadPeerPrefConnParams
**
** Description      Start a process to read a connected peripheral's preferred
**                  connection parameters
**
** Returns          TRUE if read started, else FALSE if GAP is busy
**
*******************************************************************************/
BOOLEAN GAP_BleReadPeerPrefConnParams (BD_ADDR peer_bda)
{

    tGAP_CLCB   *p_clcb = gap_find_clcb_by_bd_addr (peer_bda);

    if (p_clcb == NULL)
    {
        if ((p_clcb = gap_clcb_alloc(0, peer_bda)) == NULL)
        {
            GAP_TRACE_ERROR0("GAP_BleReadPeerPrefConnParams max connection reached");
            return FALSE;
        }
        p_clcb->connected = FALSE;
    }

    GAP_TRACE_API3 ("GAP_BleReadPeerPrefConnParams() - BDA: %08x%04x  cl_op_uuid: 0x%04x",
                    (peer_bda[0]<<24)+(peer_bda[1]<<16)+(peer_bda[2]<<8)+peer_bda[3],
                    (peer_bda[4]<<8)+peer_bda[5], p_clcb->cl_op_uuid);

    /* For now we only handle one at a time */
    if (p_clcb->cl_op_uuid != 0)
        return(FALSE);

    /* hold the link here */
    GATT_Connect(gap_cb.gatt_if, p_clcb->bda, TRUE);

    if (p_clcb->connected)
    {
    return gap_ble_cl_read_request(p_clcb, GATT_UUID_GAP_PREF_CONN_PARAM, NULL);
    }
    /* Mark currently active operation */
    p_clcb->cl_op_uuid = GATT_UUID_GAP_PREF_CONN_PARAM;

    return(TRUE);


}

/*******************************************************************************
**
** Function         GAP_BleReadPeerDevName
**
** Description      Start a process to read a connected peripheral's device name.
**
** Returns          TRUE if request accepted
**
*******************************************************************************/
BOOLEAN GAP_BleReadPeerDevName (BD_ADDR peer_bda, tGAP_BLE_DEV_NAME_CBACK *p_cback)
{
    tGAP_CLCB   *p_clcb = NULL;

    if (p_cback == NULL)
        return(FALSE);

    if ((p_clcb = gap_find_clcb_by_bd_addr (peer_bda)) == NULL)
    {
        if ((p_clcb = gap_clcb_alloc(0, peer_bda)) == NULL)
    {
        GAP_TRACE_ERROR0("GAP_BleReadPeerDevName max connection reached");
            return FALSE;
    }
        p_clcb->connected = FALSE;
    }

    GAP_TRACE_EVENT3 ("GAP_BleReadPeerDevName() - BDA: %08x%04x  cl_op_uuid: 0x%04x",
                      (peer_bda[0]<<24)+(peer_bda[1]<<16)+(peer_bda[2]<<8)+peer_bda[3],
                      (peer_bda[4]<<8)+peer_bda[5], p_clcb->cl_op_uuid);

    /* For now we only handle one at a time */
    if (p_clcb->cl_op_uuid != 0)
        return(FALSE);

    /* hold the link here */
    GATT_Connect(gap_cb.gatt_if, p_clcb->bda, TRUE);

    if (p_clcb->connected)
    {
        return gap_ble_cl_read_request(p_clcb, GATT_UUID_GAP_DEVICE_NAME, (void *)p_cback);
    }

    p_clcb->p_cback = (void *)p_cback;
    /* Mark currently active operation */
    p_clcb->cl_op_uuid = GATT_UUID_GAP_DEVICE_NAME;


    return(TRUE);
}

/*******************************************************************************
**
** Function         GAP_BleCancelReadPeerDevName
**
** Description      Cancel reading a peripheral's device name.
**
** Returns          TRUE if request accepted
**
*******************************************************************************/
BOOLEAN GAP_BleCancelReadPeerDevName (BD_ADDR peer_bda)
{
    tGAP_CLCB *p_clcb = gap_find_clcb_by_bd_addr (peer_bda);

    GAP_TRACE_EVENT3 ("GAP_BleCancelReadPeerDevName() - BDA: %08x%04x  cl_op_uuid: 0x%04x",
                      (peer_bda[0]<<24)+(peer_bda[1]<<16)+(peer_bda[2]<<8)+peer_bda[3],
                      (peer_bda[4]<<8)+peer_bda[5], (p_clcb == NULL)? 0 : p_clcb->cl_op_uuid);

    if (p_clcb == NULL || p_clcb->cl_op_uuid != GATT_UUID_GAP_DEVICE_NAME)
    {
        GAP_TRACE_ERROR0 ("Cannot cancel current op is not get dev name");
        return FALSE;
    }

    if (!p_clcb->connected)
    {
        if (!GATT_CancelConnect(gap_cb.gatt_if, peer_bda, TRUE))
        {
            GAP_TRACE_ERROR0 ("Cannot cancel where No connection id");
            return FALSE;
        }
    }

    gap_ble_cl_op_cmpl(p_clcb, FALSE, 0, NULL);

    return(TRUE);
}

/*******************************************************************************
**
** Function         GAP_BleUpdateReconnectAddr
**
** Description      Start a process to udpate the reconnect address if remote devive
**                  has privacy enabled.
**
** Returns          TRUE if read started, else FALSE if GAP is busy
**
*******************************************************************************/
BOOLEAN GAP_BleUpdateReconnectAddr (BD_ADDR peer_bda, BD_ADDR reconn_addr,
                                    tGAP_BLE_RECONN_ADDR_CBACK *p_cback)
{
    tGAP_CLCB         *p_clcb;
    tGATT_DISC_PARAM   param;

    if (p_cback == NULL)
        return(FALSE);

    /* This function should only be called if there is a connection to  */
    /* the peer. Get a client handle for that connection.               */
    if ((p_clcb = gap_find_clcb_by_bd_addr (peer_bda)) == NULL ||
        !p_clcb->connected)
    {
        GAP_TRACE_ERROR0("No connection, can not update reconnect address");
        return(FALSE);
    }

    GAP_TRACE_API3 ("GAP_BleUpdateReconnectAddr() - BDA: %08x%04x  cl_op_uuid: 0x%04x",
                    (peer_bda[0]<<24)+(peer_bda[1]<<16)+(peer_bda[2]<<8)+peer_bda[3],
                    (peer_bda[4]<<8)+peer_bda[5], p_clcb->cl_op_uuid);

    /* For now we only handle one at a time */
    if (p_clcb->cl_op_uuid != 0)
    {
        gap_ble_enqueue_op(p_clcb, GATT_UPDATE_RECONN_ADDR, reconn_addr, 0, (void *)p_cback);
        return(FALSE);
    }

    /* hold the link here */
    GATT_Connect(gap_cb.gatt_if, p_clcb->bda, TRUE);

    memset(&param, 0, sizeof(tGATT_DISC_PARAM));

    param.service.len       = LEN_UUID_16;
    param.service.uu.uuid16 = GATT_UUID_GAP_RECONN_ADDR;
    param.s_handle          = 1;
    param.e_handle          = 0xFFFF;

    if (GATTC_Discover(p_clcb->conn_id, GATT_DISC_CHAR, &param) != GATT_SUCCESS)
    {
        GAP_TRACE_ERROR0 ("GAP_BleReadPeerPrefConnParams: GATT_Read Failed");
        /* release the link here */
        GATT_Disconnect(p_clcb->conn_id);
        return(FALSE);
    }
    else
    {
        p_clcb->p_cback     = (void *)p_cback;
        memcpy(p_clcb->reconn_addr, reconn_addr, BD_ADDR_LEN);
        p_clcb->cl_op_uuid  = GATT_UUID_GAP_RECONN_ADDR;
    }

    return TRUE;

}

#endif  /* BLE_INCLUDED */





