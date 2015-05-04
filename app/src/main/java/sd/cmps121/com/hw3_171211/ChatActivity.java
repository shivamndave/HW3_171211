package sd.cmps121.com.hw3_171211;


import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ChatActivity extends Activity {
    Location lastLocation;

    protected Double locationLat;
    protected Double locationLong;

    protected ProgressBar spinNtf;

    private static final String LOG_TAG = "MainActivity";

    // This is an id for my app, to keep the key space separate from other apps.

    private static final String SERVER_URL_PREFIX = "https://luca-teaching.appspot.com/store/default/";

    // To remember the post we received.
    public static final String PREF_POSTS = "pref_posts";

    // Uploader.
    private ServerCall uploader;

    private class ListElement {
        ListElement() {
        }

        public String textLabel;
        public String timeLabel;
    }


    private ArrayList<ListElement> aList;

    // Array Adapter used to show the list of incoming messages
    private class MyAdapter extends ArrayAdapter<ListElement> {

        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<ListElement> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
            this.context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout newView;

            ListElement w = getItem(position);

            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource, newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }

            // Fills in the view.
            TextView tv = (TextView) newView.findViewById(R.id.itemText);
            TextView ts = (TextView) newView.findViewById(R.id.timeStampText);

            tv.setText(w.textLabel);

            String parsedTs = parseTs(w.timeLabel);
            ts.setText(parsedTs);

            // Set a listener for the whole list item.
            newView.setTag(w.textLabel);


            return newView;
        }
    }
    private String parseTs(String ts) {
        DateFormat formatTs = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        formatTs.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date longFormatDate;
        String outputDate = "";

        try {
            longFormatDate = formatTs.parse(ts);
            DateFormat outputFormatter = new SimpleDateFormat("EE',' h:mm aa");
            outputDate = outputFormatter.format(longFormatDate);
        } catch (ParseException e) {
            return null;
        }
        return outputDate;
    }

    private MyAdapter aa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        //createAdapter();
    }


}
