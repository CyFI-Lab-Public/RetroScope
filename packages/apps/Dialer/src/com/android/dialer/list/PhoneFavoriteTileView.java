/*

 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.dialer.list;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.list.ContactEntry;
import com.android.contacts.common.list.ContactTileView;
import com.android.dialer.R;
import com.android.dialer.list.PhoneFavoritesTileAdapter.ContactTileRow;
import com.android.dialer.list.PhoneFavoritesTileAdapter.ViewTypes;

/**
 * A light version of the {@link com.android.contacts.common.list.ContactTileView} that is used in
 * Dialtacts for frequently called contacts. Slightly different behavior from superclass when you
 * tap it, you want to call the frequently-called number for the contact, even if that is not the
 * default number for that contact. This abstract class is the super class to both the row and tile
 * view.
 */
public abstract class PhoneFavoriteTileView extends ContactTileView {

    private static final String TAG = PhoneFavoriteTileView.class.getSimpleName();
    private static final boolean DEBUG = false;

    /** Length of all animations in miniseconds. */
    private int mAnimationDuration;

    /** The view that holds the front layer of the favorite contact card. */
    private View mFavoriteContactCard;
    /** The view that holds the background layer of the removal dialogue. */
    private View mRemovalDialogue;
    /** Undo button for undoing favorite removal. */
    private View mUndoRemovalButton;
    /** The view that holds the list view row. */
    protected ContactTileRow mParentRow;
    /** The view that indicates whether the contact is a favorate. */
    protected ImageView mStarView;

    /** Users' most frequent phone number. */
    private String mPhoneNumberString;

    /** Custom gesture detector.*/
    protected GestureDetector mGestureDetector;

    // Dummy clip data object that is attached to drag shadows so that text views
    // don't crash with an NPE if the drag shadow is released in their bounds
    private static final ClipData EMPTY_CLIP_DATA = ClipData.newPlainText("", "");

    public PhoneFavoriteTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAnimationDuration = context.getResources().getInteger(R.integer.fade_duration);
    }

    public ContactTileRow getParentRow() {
        return mParentRow;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFavoriteContactCard = findViewById(com.android.dialer.R.id.contact_favorite_card);
        mRemovalDialogue = findViewById(com.android.dialer.R.id.favorite_remove_dialogue);
        mUndoRemovalButton = findViewById(com.android.dialer.R.id
                .favorite_remove_undo_button);
        mStarView = (ImageView) findViewById(com.android.dialer.R.id.contact_favorite_star);

        mUndoRemovalButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                undoRemove();
            }
        });

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final PhoneFavoriteTileView view = (PhoneFavoriteTileView) v;
                // NOTE The drag shadow is handled in the ListView.
                if (view instanceof PhoneFavoriteRegularRowView) {
                    final ContactTileRow parent = view.getParentRow();
                    // If the view is regular row, start drag the row view.
                    // Drag is not available for the item exceeds the PIN_LIMIT.
                    if (parent.getRegularRowItemIndex() < PhoneFavoritesTileAdapter.PIN_LIMIT) {
                        parent.startDrag(EMPTY_CLIP_DATA, new View.DragShadowBuilder(), null, 0);
                    }
                } else {
                    // If the view is a tile view, start drag the tile.
                    view.startDrag(EMPTY_CLIP_DATA, new View.DragShadowBuilder(), null, 0);
                }
                return true;
            }
        });
    }

    @Override
    public void loadFromContact(ContactEntry entry) {
        super.loadFromContact(entry);
        mPhoneNumberString = null; // ... in case we're reusing the view
        if (entry != null) {
            // Grab the phone-number to call directly... see {@link onClick()}
            mPhoneNumberString = entry.phoneNumber;

            mStarView.setVisibility(entry.isFavorite ? VISIBLE : GONE);
            // If this is a blank entry, don't show anything.
            // TODO krelease:Just hide the view for now. For this to truly look like an empty row
            // the entire ContactTileRow needs to be hidden.
            if (entry == ContactEntry.BLANK_ENTRY) {
                setVisibility(View.INVISIBLE);
            } else {
                setVisibility(View.VISIBLE);
            }
        }
    }

    public void displayRemovalDialog() {
        mRemovalDialogue.setVisibility(VISIBLE);
        mRemovalDialogue.setAlpha(0f);
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mRemovalDialogue, "alpha",
                1.f).setDuration(mAnimationDuration);

        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mParentRow.setHasTransientState(true);
            };

            @Override
            public void onAnimationEnd(Animator animation) {
                mParentRow.setHasTransientState(false);
            }
        });
        fadeIn.start();
    }

    /**
     * Signals the user wants to undo removing the favorite contact.
     */
    public void undoRemove() {
        // Makes the removal dialogue invisible.
        mRemovalDialogue.setAlpha(0.0f);
        mRemovalDialogue.setVisibility(GONE);

        // Animates back the favorite contact card.
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mFavoriteContactCard, "alpha", 1.f).
                setDuration(mAnimationDuration);
        final ObjectAnimator moveBack = ObjectAnimator.ofFloat(mFavoriteContactCard, "translationX",
                0.f).setDuration(mAnimationDuration);

        final AnimatorSet animSet = new AnimatorSet();

        animSet.playTogether(fadeIn, moveBack);

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mParentRow.setHasTransientState(true);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mParentRow.getItemViewType() == ViewTypes.FREQUENT) {
                    SwipeHelper.setSwipeable(mParentRow, true);
                } else {
                    SwipeHelper.setSwipeable(PhoneFavoriteTileView.this, true);
                }
                mParentRow.setHasTransientState(false);
            }
        });
        animSet.start();
        // Signals the PhoneFavoritesTileAdapter to undo the potential delete.
        mParentRow.getTileAdapter().undoPotentialRemoveEntryIndex();
    }

    /**
     * Sets up the favorite contact card.
     */
    public void setupFavoriteContactCard() {
        if (mRemovalDialogue != null) {
            mRemovalDialogue.setVisibility(GONE);
            mRemovalDialogue.setAlpha(0.f);
        }
        mFavoriteContactCard.setAlpha(1.0f);
        mFavoriteContactCard.setTranslationX(0.f);
    }

    @Override
    protected void onAttachedToWindow() {
        mParentRow = (ContactTileRow) getParent();
    }

    @Override
    protected boolean isDarkTheme() {
        return false;
    }

    @Override
    protected OnClickListener createClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                // When the removal dialog is present, don't allow a click to call
                if (mListener == null || mRemovalDialogue.isShown()) return;
                if (TextUtils.isEmpty(mPhoneNumberString)) {
                    // Copy "superclass" implementation
                    mListener.onContactSelected(getLookupUri(), MoreContactUtils
                            .getTargetRectFromView(
                                    mContext, PhoneFavoriteTileView.this));
                } else {
                    // When you tap a frequently-called contact, you want to
                    // call them at the number that you usually talk to them
                    // at (i.e. the one displayed in the UI), regardless of
                    // whether that's their default number.
                    mListener.onCallNumberDirectly(mPhoneNumberString);
                }
            }
        };
    }
}
