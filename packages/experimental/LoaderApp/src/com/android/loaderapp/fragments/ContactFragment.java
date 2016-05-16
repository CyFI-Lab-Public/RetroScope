/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.loaderapp.fragments;

import com.android.loaderapp.ContactHeaderWidget;
import com.android.loaderapp.R;
import com.android.loaderapp.model.Collapser;
import com.android.loaderapp.model.ContactLoader;
import com.android.loaderapp.model.ContactsSource;
import com.android.loaderapp.model.Sources;
import com.android.loaderapp.model.TypePrecedence;
import com.android.loaderapp.model.Collapser.Collapsible;
import com.android.loaderapp.model.ContactLoader.ContactData;
import com.android.loaderapp.model.ContactsSource.DataKind;
import com.android.loaderapp.util.Constants;
import com.android.loaderapp.util.ContactPresenceIconUtil;
import com.android.loaderapp.util.ContactsUtils;
import com.android.loaderapp.util.DataStatus;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;

import android.app.LoaderManagingFragment;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.Intent;
import android.content.Loader;
import android.content.Entity.NamedContentValues;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ContactFragment extends LoaderManagingFragment<ContactData>
        implements OnClickListener, OnItemClickListener {
    private static final String TAG = "ContactCoupler";

    static final String ARG_URI = "uri";
    static final int LOADER_DETAILS = 1;

    Uri mUri;

    private static final boolean SHOW_SEPARATORS = false;

    protected Uri mLookupUri;
    private ViewAdapter mAdapter;
    private int mNumPhoneNumbers = 0;
    private Controller mController;

    /**
     * A list of distinct contact IDs included in the current contact.
     */
    private ArrayList<Long> mRawContactIds = new ArrayList<Long>();

    /* package */ ArrayList<ViewEntry> mPhoneEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mSmsEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mEmailEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mPostalEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mImEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mNicknameEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mOrganizationEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mGroupEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mOtherEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ArrayList<ViewEntry>> mSections = new ArrayList<ArrayList<ViewEntry>>();

    protected ContactHeaderWidget mContactHeaderWidget;

    protected LayoutInflater mInflater;

    protected int mReadOnlySourcesCnt;
    protected int mWritableSourcesCnt;
    protected boolean mAllRestricted;

    protected Uri mPrimaryPhoneUri = null;

    protected ArrayList<Long> mWritableRawContactIds = new ArrayList<Long>();

    private long mNameRawContactId = -1;
    private int mDisplayNameSource = DisplayNameSources.UNDEFINED;

    private ArrayList<Entity> mEntities = Lists.newArrayList();
    private HashMap<Long, DataStatus> mStatuses = Maps.newHashMap();

    /**
     * The view shown if the detail list is empty.
     * We set this to the list view when first bind the adapter, so that it won't be shown while
     * we're loading data.
     */
    private View mEmptyView;

    private ListView mListView;
    private boolean mShowSmsLinksForAllPhones;

    public ContactFragment() {
    }

    public ContactFragment(Uri uri, ContactFragment.Controller controller) {
        mUri = uri;
        mController = controller;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.contact_details, container, false);

        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mContactHeaderWidget = (ContactHeaderWidget) view.findViewById(R.id.contact_header_widget);
        mContactHeaderWidget.setExcludeMimes(new String[] { Contacts.CONTENT_ITEM_TYPE });

        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        mListView.setOnItemClickListener(this);
        // Don't set it to mListView yet.  We do so later when we bind the adapter.
        mEmptyView = view.findViewById(android.R.id.empty);

        // Build the list of sections. The order they're added to mSections dictates the
        // order they are displayed in the list.
        mSections.add(mPhoneEntries);
        mSections.add(mSmsEntries);
        mSections.add(mEmailEntries);
        mSections.add(mImEntries);
        mSections.add(mPostalEntries);
        mSections.add(mNicknameEntries);
        mSections.add(mOrganizationEntries);
        mSections.add(mGroupEntries);
        mSections.add(mOtherEntries);

        //TODO Read this value from a preference
        mShowSmsLinksForAllPhones = true;

        return view;
    }

    @Override
    public void onInitializeLoaders() {
        if (mUri != null) {
            loadContact(mUri);
        }
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_DETAILS: {
                Uri uri = args.getParcelable(ARG_URI);
                return new ContactLoader(getActivity(), uri);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<ContactData> loader, ContactData data) {
        switch (loader.getId()) {
            case LOADER_DETAILS: {
                setData(data);
                break;
            }
        }
    }

    public void loadContact(Uri uri) {
        mUri = uri;
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        startLoading(LOADER_DETAILS, args);
    }

    public void setData(ContactData data) {
        mEntities = data.entities;
        mStatuses = data.statuses;

        mNameRawContactId = data.nameRawContactId;
        mDisplayNameSource = data.displayNameSource;

        mContactHeaderWidget.bindFromContactLookupUri(data.uri);
        bindData();
    }

    public interface Controller {
        public void onPrimaryAction(ViewEntry entry);
        public void onSecondaryAction(ViewEntry entry);
    }

    public static final class DefaultController implements Controller {
        private Context mContext;

        public DefaultController(Context context) {
            mContext = context;
        }

        public void onPrimaryAction(ViewEntry entry) {
            Intent intent = entry.intent;
            if (intent != null) {
                try {
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No activity found for intent: " + intent);
                }
            }
        }

        public void onSecondaryAction(ViewEntry entry) {
            Intent intent = entry.secondaryIntent;
            if (intent != null) {
                try {
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No activity found for intent: " + intent);
                }
            }
        }
    }

    public void setController(Controller controller) {
        mController = controller;
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        if (mController != null) {
            ViewEntry entry = ViewAdapter.getEntry(mSections, position, SHOW_SEPARATORS);
            if (entry != null) {
                mController.onPrimaryAction(entry);
            }
        }
    }

    public void onClick(View v) {
        if (mController != null) {
            mController.onSecondaryAction((ViewEntry) v.getTag());
        }
    }

    private void bindData() {

        // Build up the contact entries
        buildEntries();

        // Collapse similar data items in select sections.
        Collapser.collapseList(mPhoneEntries);
        Collapser.collapseList(mSmsEntries);
        Collapser.collapseList(mEmailEntries);
        Collapser.collapseList(mPostalEntries);
        Collapser.collapseList(mImEntries);

        if (mAdapter == null) {
            mAdapter = new ViewAdapter(getActivity(), mSections);
            mListView.setAdapter(mAdapter);
        } else {
            mAdapter.setSections(mSections, SHOW_SEPARATORS);
        }
        mListView.setEmptyView(mEmptyView);
    }

    /**
     * Build up the entries to display on the screen.
     *
     * @param personCursor the URI for the contact being displayed
     */
    private final void buildEntries() {
        // Clear out the old entries
        final int numSections = mSections.size();
        for (int i = 0; i < numSections; i++) {
            mSections.get(i).clear();
        }

        mRawContactIds.clear();

        mReadOnlySourcesCnt = 0;
        mWritableSourcesCnt = 0;
        mAllRestricted = true;
        mPrimaryPhoneUri = null;

        mWritableRawContactIds.clear();

        if (mEntities == null || mStatuses == null) {
            return;
        }
        
        final Context context = getActivity();
        final Sources sources = Sources.getInstance(context);

        // Build up method entries
        for (Entity entity: mEntities) {
            final ContentValues entValues = entity.getEntityValues();
            final String accountType = entValues.getAsString(RawContacts.ACCOUNT_TYPE);
            final long rawContactId = entValues.getAsLong(RawContacts._ID);

            // Mark when this contact has any unrestricted components
            final boolean isRestricted = entValues.getAsInteger(RawContacts.IS_RESTRICTED) != 0;
            if (!isRestricted) mAllRestricted = false;

            if (!mRawContactIds.contains(rawContactId)) {
                mRawContactIds.add(rawContactId);
            }
            ContactsSource contactsSource = sources.getInflatedSource(accountType,
                    ContactsSource.LEVEL_SUMMARY);
            if (contactsSource != null && contactsSource.readOnly) {
                mReadOnlySourcesCnt += 1;
            } else {
                mWritableSourcesCnt += 1;
                mWritableRawContactIds.add(rawContactId);
            }


            for (NamedContentValues subValue : entity.getSubValues()) {
                final ContentValues entryValues = subValue.values;
                entryValues.put(Data.RAW_CONTACT_ID, rawContactId);

                final long dataId = entryValues.getAsLong(Data._ID);
                final String mimeType = entryValues.getAsString(Data.MIMETYPE);
                if (mimeType == null) continue;

                final DataKind kind = sources.getKindOrFallback(accountType, mimeType,
                        context, ContactsSource.LEVEL_MIMETYPES);
                if (kind == null) continue;

                final ViewEntry entry = ViewEntry.fromValues(context, mimeType, kind,
                        rawContactId, dataId, entryValues);

                final boolean hasData = !TextUtils.isEmpty(entry.data);
                final boolean isSuperPrimary = entryValues.getAsInteger(
                        Data.IS_SUPER_PRIMARY) != 0;

                if (Phone.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build phone entries
                    mNumPhoneNumbers++;

                    entry.intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts(Constants.SCHEME_TEL, entry.data, null));
                    entry.secondaryIntent = new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(Constants.SCHEME_SMSTO, entry.data, null));

                    // Remember super-primary phone
                    if (isSuperPrimary) mPrimaryPhoneUri = entry.uri;

                    entry.isPrimary = isSuperPrimary;
                    mPhoneEntries.add(entry);

                    if (entry.type == CommonDataKinds.Phone.TYPE_MOBILE
                            || mShowSmsLinksForAllPhones) {
                        // Add an SMS entry
                        if (kind.iconAltRes > 0) {
                            entry.secondaryActionIcon = kind.iconAltRes;
                        }
                    }
                } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build email entries
                    entry.intent = new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(Constants.SCHEME_MAILTO, entry.data, null));
                    entry.isPrimary = isSuperPrimary;
                    mEmailEntries.add(entry);

                    // When Email rows have status, create additional Im row
                    final DataStatus status = mStatuses.get(entry.id);
                    if (status != null) {
                        final String imMime = Im.CONTENT_ITEM_TYPE;
                        final DataKind imKind = sources.getKindOrFallback(accountType,
                                imMime, context, ContactsSource.LEVEL_MIMETYPES);
                        final ViewEntry imEntry = ViewEntry.fromValues(context,
                                imMime, imKind, rawContactId, dataId, entryValues);
                        imEntry.intent = ContactsUtils.buildImIntent(entryValues);
                        imEntry.applyStatus(status, false);
                        mImEntries.add(imEntry);
                    }
                } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build postal entries
                    entry.maxLines = 4;
                    entry.intent = new Intent(Intent.ACTION_VIEW, entry.uri);
                    mPostalEntries.add(entry);
                } else if (Im.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build IM entries
                    entry.intent = ContactsUtils.buildImIntent(entryValues);
                    if (TextUtils.isEmpty(entry.label)) {
                        entry.label = context.getString(R.string.chat).toLowerCase();
                    }

                    // Apply presence and status details when available
                    final DataStatus status = mStatuses.get(entry.id);
                    if (status != null) {
                        entry.applyStatus(status, false);
                    }
                    mImEntries.add(entry);
                } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType) &&
                        (hasData || !TextUtils.isEmpty(entry.label))) {
                    // Build organization entries
                    final boolean isNameRawContact = (mNameRawContactId == rawContactId);

                    final boolean duplicatesTitle =
                        isNameRawContact
                        && mDisplayNameSource == DisplayNameSources.ORGANIZATION
                        && (!hasData || TextUtils.isEmpty(entry.label));

                    if (!duplicatesTitle) {
                        entry.uri = null;

                        if (TextUtils.isEmpty(entry.label)) {
                            entry.label = entry.data;
                            entry.data = "";
                        }

                        mOrganizationEntries.add(entry);
                    }
                } else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build nickname entries
                    final boolean isNameRawContact = (mNameRawContactId == rawContactId);

                    final boolean duplicatesTitle =
                        isNameRawContact
                        && mDisplayNameSource == DisplayNameSources.NICKNAME;

                    if (!duplicatesTitle) {
                        entry.uri = null;
                        mNicknameEntries.add(entry);
                    }
                } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build note entries
                    entry.uri = null;
                    entry.maxLines = 100;
                    mOtherEntries.add(entry);
                } else if (Website.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build note entries
                    entry.uri = null;
                    entry.maxLines = 10;
                    try {
                        WebAddress webAddress = new WebAddress(entry.data);
                        entry.intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(webAddress.toString()));
                    } catch (ParseException e) {
                        Log.e(TAG, "Couldn't parse website: " + entry.data);
                    }
                    mOtherEntries.add(entry);
                } else {
                    // Handle showing custom rows
                    entry.intent = new Intent(Intent.ACTION_VIEW, entry.uri);

                    // Use social summary when requested by external source
                    final DataStatus status = mStatuses.get(entry.id);
                    final boolean hasSocial = kind.actionBodySocial && status != null;
                    if (hasSocial) {
                        entry.applyStatus(status, true);
                    }

                    if (hasSocial || hasData) {
                        mOtherEntries.add(entry);
                    }
                }
            }
        }
    }

    static String buildActionString(DataKind kind, ContentValues values, boolean lowerCase,
            Context context) {
        if (kind.actionHeader == null) {
            return null;
        }
        CharSequence actionHeader = kind.actionHeader.inflateUsing(context, values);
        if (actionHeader == null) {
            return null;
        }
        return lowerCase ? actionHeader.toString().toLowerCase() : actionHeader.toString();
    }

    static String buildDataString(DataKind kind, ContentValues values, Context context) {
        if (kind.actionBody == null) {
            return null;
        }
        CharSequence actionBody = kind.actionBody.inflateUsing(context, values);
        return actionBody == null ? null : actionBody.toString();
    }

    /**
     * A basic structure with the data for a contact entry in the list.
     */
   public static class ViewEntry extends ContactEntryAdapter.Entry implements Collapsible<ViewEntry> {
        public Context context = null;
        public String resPackageName = null;
        public int actionIcon = -1;
        public boolean isPrimary = false;
        public int secondaryActionIcon = -1;
        public Intent intent;
        public Intent secondaryIntent = null;
        public int maxLabelLines = 1;
        public ArrayList<Long> ids = new ArrayList<Long>();
        public int collapseCount = 0;

        public int presence = -1;

        public CharSequence footerLine = null;

        private ViewEntry() {
        }

        /**
         * Build new {@link ViewEntry} and populate from the given values.
         */
        public static ViewEntry fromValues(Context context, String mimeType, DataKind kind,
                long rawContactId, long dataId, ContentValues values) {
            final ViewEntry entry = new ViewEntry();
            entry.context = context;
            entry.contactId = rawContactId;
            entry.id = dataId;
            entry.uri = ContentUris.withAppendedId(Data.CONTENT_URI, entry.id);
            entry.mimetype = mimeType;
            entry.label = buildActionString(kind, values, false, context);
            entry.data = buildDataString(kind, values, context);

            if (kind.typeColumn != null && values.containsKey(kind.typeColumn)) {
                entry.type = values.getAsInteger(kind.typeColumn);
            }
            if (kind.iconRes > 0) {
                entry.resPackageName = kind.resPackageName;
                entry.actionIcon = kind.iconRes;
            }

            return entry;
        }

        /**
         * Apply given {@link DataStatus} values over this {@link ViewEntry}
         *
         * @param fillData When true, the given status replaces {@link #data}
         *            and {@link #footerLine}. Otherwise only {@link #presence}
         *            is updated.
         */
        public ViewEntry applyStatus(DataStatus status, boolean fillData) {
            presence = status.getPresence();
            if (fillData && status.isValid()) {
                this.data = status.getStatus().toString();
                this.footerLine = status.getTimestampLabel(context);
            }

            return this;
        }

        public boolean collapseWith(ViewEntry entry) {
            // assert equal collapse keys
            if (!shouldCollapseWith(entry)) {
                return false;
            }

            // Choose the label associated with the highest type precedence.
            if (TypePrecedence.getTypePrecedence(mimetype, type)
                    > TypePrecedence.getTypePrecedence(entry.mimetype, entry.type)) {
                type = entry.type;
                label = entry.label;
            }

            // Choose the max of the maxLines and maxLabelLines values.
            maxLines = Math.max(maxLines, entry.maxLines);
            maxLabelLines = Math.max(maxLabelLines, entry.maxLabelLines);

            // Choose the presence with the highest precedence.
            if (StatusUpdates.getPresencePrecedence(presence)
                    < StatusUpdates.getPresencePrecedence(entry.presence)) {
                presence = entry.presence;
            }

            // If any of the collapsed entries are primary make the whole thing primary.
            isPrimary = entry.isPrimary ? true : isPrimary;

            // uri, and contactdId, shouldn't make a difference. Just keep the original.

            // Keep track of all the ids that have been collapsed with this one.
            ids.add(entry.id);
            collapseCount++;
            return true;
        }

        public boolean shouldCollapseWith(ViewEntry entry) {
            if (entry == null) {
                return false;
            }

            if (!ContactsUtils.areDataEqual(context, mimetype, data, entry.mimetype, entry.data)) {
                return false;
            }

            if (!TextUtils.equals(mimetype, entry.mimetype)
                    || !ContactsUtils.areIntentActionEqual(intent, entry.intent)
                    || !ContactsUtils.areIntentActionEqual(secondaryIntent, entry.secondaryIntent)
                    || actionIcon != entry.actionIcon) {
                return false;
            }

            return true;
        }
    }

    /** Cache of the children views of a row */
    static class ViewCache {
        public TextView label;
        public TextView data;
        public TextView footer;
        public ImageView actionIcon;
        public ImageView presenceIcon;
        public ImageView primaryIcon;
        public ImageView secondaryActionButton;
        public View secondaryActionDivider;

        // Need to keep track of this too
        ViewEntry entry;
    }

    private final class ViewAdapter extends ContactEntryAdapter<ViewEntry> {
        ViewAdapter(Context context, ArrayList<ArrayList<ViewEntry>> sections) {
            super(context, sections, SHOW_SEPARATORS);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewEntry entry = getEntry(mSections, position, false);
            View v;

            ViewCache views;

            // Check to see if we can reuse convertView
            if (convertView != null) {
                v = convertView;
                views = (ViewCache) v.getTag();
            } else {
                // Create a new view if needed
                v = mInflater.inflate(R.layout.list_item_text_icons, parent, false);

                // Cache the children
                views = new ViewCache();
                views.label = (TextView) v.findViewById(android.R.id.text1);
                views.data = (TextView) v.findViewById(android.R.id.text2);
                views.footer = (TextView) v.findViewById(R.id.footer);
                views.actionIcon = (ImageView) v.findViewById(R.id.action_icon);
                views.primaryIcon = (ImageView) v.findViewById(R.id.primary_icon);
                views.presenceIcon = (ImageView) v.findViewById(R.id.presence_icon);
                views.secondaryActionButton = (ImageView) v.findViewById(
                        R.id.secondary_action_button);
                views.secondaryActionButton.setOnClickListener(ContactFragment.this);
                views.secondaryActionDivider = v.findViewById(R.id.divider);
                v.setTag(views);
            }

            // Update the entry in the view cache
            views.entry = entry;

            // Bind the data to the view
            bindView(v, entry);
            return v;
        }

        @Override
        protected View newView(int position, ViewGroup parent) {
            // getView() handles this
            throw new UnsupportedOperationException();
        }

        @Override
        protected void bindView(View view, ViewEntry entry) {
            final Resources resources = mContext.getResources();
            ViewCache views = (ViewCache) view.getTag();

            // Set the label
            TextView label = views.label;
            setMaxLines(label, entry.maxLabelLines);
            label.setText(entry.label);

            // Set the data
            TextView data = views.data;
            if (data != null) {
                if (entry.mimetype.equals(Phone.CONTENT_ITEM_TYPE)
                        || entry.mimetype.equals(Constants.MIME_SMS_ADDRESS)) {
                    data.setText(PhoneNumberUtils.formatNumber(entry.data));
                } else {
                    data.setText(entry.data);
                }
                setMaxLines(data, entry.maxLines);
            }

            // Set the footer
            if (!TextUtils.isEmpty(entry.footerLine)) {
                views.footer.setText(entry.footerLine);
                views.footer.setVisibility(View.VISIBLE);
            } else {
                views.footer.setVisibility(View.GONE);
            }

            // Set the primary icon
            views.primaryIcon.setVisibility(entry.isPrimary ? View.VISIBLE : View.GONE);

            // Set the action icon
            ImageView action = views.actionIcon;
            if (entry.actionIcon != -1) {
                Drawable actionIcon;
                if (entry.resPackageName != null) {
                    // Load external resources through PackageManager
                    actionIcon = mContext.getPackageManager().getDrawable(entry.resPackageName,
                            entry.actionIcon, null);
                } else {
                    actionIcon = resources.getDrawable(entry.actionIcon);
                }
                action.setImageDrawable(actionIcon);
                action.setVisibility(View.VISIBLE);
            } else {
                // Things should still line up as if there was an icon, so make it invisible
                action.setVisibility(View.INVISIBLE);
            }

            // Set the presence icon
            Drawable presenceIcon = ContactPresenceIconUtil.getPresenceIcon(
                    mContext, entry.presence);
            ImageView presenceIconView = views.presenceIcon;
            if (presenceIcon != null) {
                presenceIconView.setImageDrawable(presenceIcon);
                presenceIconView.setVisibility(View.VISIBLE);
            } else {
                presenceIconView.setVisibility(View.GONE);
            }

            // Set the secondary action button
            ImageView secondaryActionView = views.secondaryActionButton;
            Drawable secondaryActionIcon = null;
            if (entry.secondaryActionIcon != -1) {
                secondaryActionIcon = resources.getDrawable(entry.secondaryActionIcon);
            }
            if (entry.secondaryIntent != null && secondaryActionIcon != null) {
                secondaryActionView.setImageDrawable(secondaryActionIcon);
                secondaryActionView.setTag(entry);
                secondaryActionView.setVisibility(View.VISIBLE);
                views.secondaryActionDivider.setVisibility(View.VISIBLE);
            } else {
                secondaryActionView.setVisibility(View.GONE);
                views.secondaryActionDivider.setVisibility(View.GONE);
            }
        }

        private void setMaxLines(TextView textView, int maxLines) {
            if (maxLines == 1) {
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                textView.setSingleLine(false);
                textView.setMaxLines(maxLines);
                textView.setEllipsize(null);
            }
        }
    }
}
