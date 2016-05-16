package com.android.dialer.list;

import android.test.AndroidTestCase;

public class PhoneFavoritesTileAdapterTest extends AndroidTestCase {
    private PhoneFavoritesTileAdapter mAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAdapter = new PhoneFavoritesTileAdapter(getContext(), null, null, 3, 1);
    }

    /**
     * TODO: Add tests
     *
     * Test cases (various combinations of):
     * No pinned contacts
     * One pinned contact
     * Multiple pinned contacts with differing pinned positions
     * Multiple pinned contacts with conflicting pinned positions
     * Pinned contacts with pinned positions at the start, middle, end, and outside the list
     */
    public void testArrangeContactsByPinnedPosition() {

    }

    /**
     * TODO: Add tests
     *
     * This method assumes that contacts have already been reordered by
     * arrangeContactsByPinnedPosition, so we can test it with a less expansive set of test data.
     *
     * Test cases:
     * Pin a single contact at the start, middle and end of a completely unpinned list
     * Pin a single contact at the start, middle and end of a list with various numbers of
     * pinned contacts
     * Pin a single contact at the start, middle and end of a list where all contacts are pinned
     * such that contacts are forced to the left as necessary.
     */
    public void testGetReflowedPinnedPositions() {

    }


}
