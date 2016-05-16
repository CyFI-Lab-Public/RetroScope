package com.android.mail.compose;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.mail.R;
import com.android.mail.ui.AttachmentTile;

public class ComposeAttachmentTile extends AttachmentTile implements AttachmentDeletionInterface {
    private ImageButton mDeleteButton;

    public ComposeAttachmentTile(Context context) {
        this(context, null);
    }

    public ComposeAttachmentTile(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static ComposeAttachmentTile inflate(LayoutInflater inflater, ViewGroup parent) {
        ComposeAttachmentTile view = (ComposeAttachmentTile) inflater.inflate(
                R.layout.compose_attachment_tile, parent, false);
        return view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mDeleteButton = (ImageButton) findViewById(R.id.attachment_tile_close_button);
    }

    @Override
    public void addDeleteListener(OnClickListener clickListener) {
        mDeleteButton.setOnClickListener(clickListener);
    }
}
