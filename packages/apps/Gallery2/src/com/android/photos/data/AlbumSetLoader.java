package com.android.photos.data;

import android.database.MatrixCursor;


public class AlbumSetLoader {
    public static final int INDEX_ID = 0;
    public static final int INDEX_TITLE = 1;
    public static final int INDEX_TIMESTAMP = 2;
    public static final int INDEX_THUMBNAIL_URI = 3;
    public static final int INDEX_THUMBNAIL_WIDTH = 4;
    public static final int INDEX_THUMBNAIL_HEIGHT = 5;
    public static final int INDEX_COUNT_PENDING_UPLOAD = 6;
    public static final int INDEX_COUNT = 7;
    public static final int INDEX_SUPPORTED_OPERATIONS = 8;

    public static final String[] PROJECTION = {
        "_id",
        "title",
        "timestamp",
        "thumb_uri",
        "thumb_width",
        "thumb_height",
        "count_pending_upload",
        "_count",
        "supported_operations"
    };
    public static final MatrixCursor MOCK = createRandomCursor(30);

    private static MatrixCursor createRandomCursor(int count) {
        MatrixCursor c = new MatrixCursor(PROJECTION, count);
        for (int i = 0; i < count; i++) {
            c.addRow(createRandomRow());
        }
        return c;
    }

    private static Object[] createRandomRow() {
        double random = Math.random();
        int id = (int) (500 * random);
        Object[] row = {
            id,
            "Fun times " + id,
            (long) (System.currentTimeMillis() * random),
            null,
            0,
            0,
            (random < .3 ? 1 : 0),
            1,
            0
        };
        return row;
    }
}