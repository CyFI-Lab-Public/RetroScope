#include <string.h>
#include "blktrace.h"

#define DECLARE_MASK_MAP(mask)          { BLK_TC_##mask, #mask, "BLK_TC_"#mask }
#define COMPARE_MASK_MAP(mmp, str)                                      \
        (!strcasecmp((mmp)->short_form, (str)) ||                      \
         !strcasecmp((mmp)->long_form, (str)))

struct mask_map {
	int mask;
	char *short_form;
	char *long_form;
};

static struct mask_map mask_maps[] = {
	DECLARE_MASK_MAP(READ),
	DECLARE_MASK_MAP(WRITE),
	DECLARE_MASK_MAP(BARRIER),
	DECLARE_MASK_MAP(SYNC),
	DECLARE_MASK_MAP(QUEUE),
	DECLARE_MASK_MAP(REQUEUE),
	DECLARE_MASK_MAP(ISSUE),
	DECLARE_MASK_MAP(COMPLETE),
	DECLARE_MASK_MAP(FS),
	DECLARE_MASK_MAP(PC),
	DECLARE_MASK_MAP(NOTIFY),
	DECLARE_MASK_MAP(AHEAD),
	DECLARE_MASK_MAP(META),
	DECLARE_MASK_MAP(DISCARD),
	DECLARE_MASK_MAP(DRV_DATA),
};

int find_mask_map(char *string)
{
	unsigned int i;

	for (i = 0; i < sizeof(mask_maps)/sizeof(mask_maps[0]); i++)
		if (COMPARE_MASK_MAP(&mask_maps[i], string))
			return mask_maps[i].mask;

	return -1;
}

int valid_act_opt(int x)
{
	return (1 <= x) && (x < (1 << BLK_TC_SHIFT));
}
