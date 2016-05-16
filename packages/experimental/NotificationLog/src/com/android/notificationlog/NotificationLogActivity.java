package com.android.notificationlog;

import java.io.IOException;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.Date;

public class NotificationLogActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        ListAdapter adapter = new NotificationLogAdapter();
        setListAdapter(adapter);
        getListView().setTextFilterEnabled(true);
    }

    
    class NotificationLogAdapter extends BaseAdapter {
        private ArrayList<EventLog.Event> mNotificationEvents;
        private final PackageManager mPM;
        private final LayoutInflater mInflater;

        public NotificationLogAdapter() {
            mPM = getPackageManager();
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mNotificationEvents = new ArrayList<EventLog.Event>();
            
            int[] tags = new int[] { EventLog.getTagCode("notification_enqueue") };
            try {
                EventLog.readEvents(tags, mNotificationEvents);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            android.util.Log.d("NotificationLogActivity", "loaded " + getCount() + " entries");
        }
        
        public int getCount() {
            return mNotificationEvents != null ? mNotificationEvents.size() : 0;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.row, parent, false);
            } else {
                view = convertView;
            }
            bindView(view, mNotificationEvents.get(position));
            return view;
        }
        

        private final void bindView(View view, EventLog.Event evt) {
            TextView title = (TextView)view.findViewById(R.id.title);
            TextView more = (TextView)view.findViewById(R.id.text);
            TextView time = (TextView)view.findViewById(R.id.time);
            ImageView icon = (ImageView)view.findViewById(R.id.icon);

            Object[] data = (Object[]) evt.getData();
            // EventLog.writeEvent(EventLogTags.NOTIFICATION_ENQUEUE, pkg, id, tag,
            // notification.toString());
            String pkg = (String) data[0];
            int id = (Integer) data[1];
            String tag = (String) data[2];
            String text = (String) data[3];

            ApplicationInfo appInfo;
            Drawable appIcon = null;
            try {
                appInfo = mPM.getApplicationInfo(pkg, 0);
                pkg =  mPM.getApplicationLabel(appInfo) + " (" + pkg + ")";
                appIcon = mPM.getApplicationIcon(appInfo);
            } catch (NameNotFoundException e) {
            }
            title.setText(pkg);
            more.setText(text);
            time.setText(new Date(evt.getTimeNanos()/1000000).toString());
            icon.setImageDrawable(appIcon);
        }
    }
}
