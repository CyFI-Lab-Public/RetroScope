        if (key <= MAX_HASH_VALUE) {
            int MPH = hash_to_MPH[key];
            if (MPH >= 0) {
                assert(MPH < MPH_MAX);
                SLInterfaceID trial = &SL_IID_array[MPH];
                if (!memcmp(iid, trial, sizeof(struct SLInterfaceID_)))
                    return MPH;
            }
        }
    }
    return -1;
}
