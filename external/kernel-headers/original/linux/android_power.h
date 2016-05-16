/* linux/android_power.h
** 
** Copyright 2005-2006, The Android Open Source Project
** Author: Arve Hjønnevåg
**
** This file is dual licensed.  It may be redistributed and/or modified
** under the terms of the Apache 2.0 License OR version 2 of the GNU
** General Public License.
*/

#ifndef _LINUX_ANDROID_POWER_H
#define _LINUX_ANDROID_POWER_H

#include <linux/list.h>

typedef struct
{
	struct list_head    link;
	int                 lock_count;
	int                 flags;
	const char         *name;
	int                 expires;
} android_suspend_lock_t;

#define ANDROID_SUSPEND_LOCK_FLAG_COUNTED (1U << 0)
#define ANDROID_SUSPEND_LOCK_FLAG_USER_READABLE (1U << 1)
#define ANDROID_SUSPEND_LOCK_FLAG_USER_SET (1U << 2)
#define ANDROID_SUSPEND_LOCK_FLAG_USER_CLEAR (1U << 3)
#define ANDROID_SUSPEND_LOCK_FLAG_USER_INC (1U << 4)
#define ANDROID_SUSPEND_LOCK_FLAG_USER_DEC (1U << 5)
#define ANDROID_SUSPEND_LOCK_FLAG_USER_VISIBLE_MASK (0x1fU << 1)
#define ANDROID_SUSPEND_LOCK_AUTO_EXPIRE (1U << 6)


typedef struct android_early_suspend android_early_suspend_t;
struct android_early_suspend
{
	struct list_head link;
	int level;
	void (*suspend)(android_early_suspend_t *h);
	void (*resume)(android_early_suspend_t *h);
};

typedef enum {
	ANDROID_CHARGING_STATE_UNKNOWN,
	ANDROID_CHARGING_STATE_DISCHARGE,
	ANDROID_CHARGING_STATE_MAINTAIN, // or trickle
	ANDROID_CHARGING_STATE_SLOW,
	ANDROID_CHARGING_STATE_NORMAL,
	ANDROID_CHARGING_STATE_FAST,
	ANDROID_CHARGING_STATE_OVERHEAT
} android_charging_state_t;

//android_suspend_lock_t *android_allocate_suspend_lock(const char *debug_name);
//void android_free_suspend_lock(android_suspend_lock_t *lock);
int android_init_suspend_lock(android_suspend_lock_t *lock);
void android_uninit_suspend_lock(android_suspend_lock_t *lock);
void android_lock_suspend(android_suspend_lock_t *lock);
void android_lock_suspend_auto_expire(android_suspend_lock_t *lock, int timeout);
void android_unlock_suspend(android_suspend_lock_t *lock);
void android_power_wakeup(int notification); /* notification = 0: normal wakeup, notification = 1: temporary wakeup */

int android_power_is_driver_suspended(void);

void android_register_early_suspend(android_early_suspend_t *handler);
void android_unregister_early_suspend(android_early_suspend_t *handler);

void android_power_set_battery_level(int level); // level 0-100
void android_power_set_charging_state(android_charging_state_t state);

#endif

