#ifndef __NFC_OSAL_DEFERRED_CALL_H_
#define __NFC_OSAL_DEFERRED_CALL_H_

/**
 * \ingroup grp_osal_nfc
 *\brief Deferred call declaration.
 * This type of API is called from ClientApplication ( main thread) to notify 
 * specific callback.
 */
typedef  pphLibNfc_DeferredCallback_t nfc_osal_def_call_t;

/**
 * \ingroup grp_osal_nfc
 *\brief Deferred message specific info declaration.
 * This type information packed as WPARAM  when \ref PHOSALNFC_MESSAGE_BASE type 
 *windows message is posted to main thread.
 */
typedef phLibNfc_DeferredCall_t nfc_osal_def_call_msg_t;

/**
 * \ingroup grp_osal_nfc
 *\brief Deferred call declaration.
 * This Deferred call post message of type \ref PH_OSALNFC_TIMER_MSG along with
 * timer specific details.ain thread,which is responsible for timer callback notification
 * consumes of this message and notifies respctive timer callback. 
 *\note: This API packs upper timer specific callback notification  information and post 
 *ref\PHOSALNFC_MESSAGE_BASE to main thread via windows post messaging mechanism.
 */ 

void nfc_osal_deferred_call(nfc_osal_def_call_t func, void *param);

#endif