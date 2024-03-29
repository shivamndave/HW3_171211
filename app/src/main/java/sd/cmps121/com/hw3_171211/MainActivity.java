package sd.cmps121.com.hw3_171211;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends Activity {

    Location lastLocation;

    protected Double locationLat;
    protected Double locationLong;

    public ProgressBar spinNtf;

    private static final String LOG_TAG = "MainActivity";

    // This is an id for my app, to keep the key space separate from other apps.

    private static final String SERVER_URL_PREFIX = "https://hw3n-dot-luca-teaching.appspot.com/store/default/";

    // To remember the post we received.
    public static final String PREF_POSTS = "pref_posts";

    // Uploader.
    private ServerCall uploader;

    private class ListElement {
        ListElement() {
        }

        public String textLabel;
        public String timeLabel;
        public String userIdLabel;
        public boolean converseLabel;

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
            ImageView iv = (ImageView) newView.findViewById(R.id.convoIndic);

            tv.setText(w.textLabel);

            String parsedTs = parseTs(w.timeLabel);
            ts.setText(parsedTs);

            // HW3: For conversation posts, a notification is displayed showing you have
            // engaged in a private conversation with a person
            if (w.converseLabel) {
                iv.setVisibility(View.VISIBLE);
            } else {
                iv.setVisibility(View.INVISIBLE);
            }

            // Set a listener for the whole list item.

            // HW3: Adds a condition if this post is posted by another user, you can click to
            // engage in a prvate conversation with them. If you click your post, it tells you
            // that you posted it
            newView.setTag(w.textLabel);
            final String userConvId = w.userIdLabel;
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!(_appInfo.userid).equals(userConvId)) {
                        Intent chatActivity = new Intent(MainActivity.this, ChatActivity.class);
                        chatActivity.putExtra("dest", userConvId);
                        startActivity(chatActivity);
                    } else {
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(context, "You posted this message", duration);
                        toast.show();
                    }
                }
            });

            return newView;
        }
    }

    // citation: Piazza Post: "For anyone still having trouble the timestamp"
    // This presents the timestamp in a presentable way using the day and time
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
    private AppInfo _appInfo;


    // Also *note* the spinNtf is first called as gone
    // here, but it is visible when a click happens
    // on a button and then is invisible onResume/location change

    // On create function that is first called
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinNtf = (ProgressBar) findViewById(R.id.loadingNotif);
        spinNtf.setVisibility(View.GONE);
        _appInfo = AppInfo.getInstance(this);
        Log.e(LOG_TAG, _appInfo.userid);
        createAdapter();
    }

    // First initialization of the adapter when starting the app

    // HW3: Added a check here if its not null it will populate the fields
    // and added the userIdlabel and converse labels here to be able to track
    // conversations
    private void createAdapter() {
        Gson gson = new Gson();
        String result = getRecentMessages();
        MessageList ml = gson.fromJson(result, MessageList.class);
        aList = new ArrayList<ListElement>();
        if (result != null) {
            // Fills aList, so we can fill the listView.
            for (int i = 0; i < ml.messages.length; i++) {
                Log.e(LOG_TAG, ml.messages.toString());
                ListElement ael = new ListElement();
                ael.textLabel = ml.messages[i].msg;
                ael.timeLabel = ml.messages[i].ts;
                ael.userIdLabel = ml.messages[i].userid;
                ael.converseLabel = ml.messages[i].conversation;
                aList.add(ael);
            }
        }
        aa = new MyAdapter(this, R.layout.list_element, aList);
        ListView myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(aa);
    }


    // On starting the app's activities
    @Override
    protected void onStart() {
        super.onStart();
        String result = getRecentMessages();
        if (result != null) {
            displayResult(result);
        }
    }

    // When resuming the app between server calls
    @Override
    protected void onResume() {
        super.onResume();
        String result = getRecentMessages();
        if (result != null) {
            displayResult(result);
        }
    }

    // This function gets the location info, it is called whenever we want to
    // get location information and store the longitude/latitude information

    //HW3: Added a check if you have location services off, it wont crash,
    // but will just set you at (0,0) and tell you that you are there
    private void getLocationInfo() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if(lastLocation == null) {
            locationLat = 0.0;
            locationLong = 0.0;

            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, "Location services are not working or off.", duration);
            toast.show();
            Log.i(LOG_TAG, "Location services not working.");
        } else {

            lastLocation.getLatitude();
            lastLocation.getLongitude();

            locationLat = lastLocation.getLatitude();
            locationLong = lastLocation.getLongitude();
        }
    }

    // Function that gets recent messages, called in numerous functions to have calls to the server

    //HW3: userId is also gotten along with location
    private String getRecentMessages() {
        getLocationInfo();
        PostMessageSpec myCallSpec = new PostMessageSpec();

        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;

        //Let's add the parameters.
        HashMap<String, String> tempHash = new HashMap<String, String>();
        tempHash.put("lat", locationLat.toString());
        tempHash.put("lng", locationLong.toString());
        tempHash.put("userid", _appInfo.userid);

        myCallSpec.setParams(tempHash);
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        return settings.getString(PREF_POSTS, null);
    }


    // Whenever paused, we stop getting the location
    @Override
    protected void onPause() {
        String result = getRecentMessages();
        if (result != null) {
            displayResult(result);
        }
        // Stops the location updates.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);

        if (uploader != null) {
            uploader.cancel(true);
            uploader = null;
        }
        super.onPause();
    }

    // Location listener that updates the lat/lng at the bottom dynamically
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            TextView currentLocationTextView = (TextView) findViewById(R.id.currentlocview);

            lastLocation = location;
            locationLat = location.getLatitude();
            locationLong = location.getLongitude();

            String Text = "Latitude: " + locationLat + " Longitude: " +
                    locationLong;

            currentLocationTextView.setText(Text);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    // Post button that gets the text from the edittext (then clears it),
    // packages it into a PostMessageSpec with a HashMap, and then executes
    // it using the uploader

    // HW3: Added the message id and user id as things uploaded,
    // to allow for support for private messaging
    public void clickButton(View v) {
        spinNtf.setVisibility(View.VISIBLE);
        if (lastLocation == null) {
            return;
        }
        // Get the text we want to send.
        EditText et = (EditText) findViewById(R.id.editText);
        String msg = et.getText().toString();
        et.setText("");

        // Then, we start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();

        getLocationInfo();

        myCallSpec.url = SERVER_URL_PREFIX + "put_local";
        ;
        myCallSpec.context = MainActivity.this;
        //Let's add the parameters.
        HashMap<String, String> tempHash = new HashMap<String, String>();
        tempHash.put("msg", msg);
        tempHash.put("lat", locationLat.toString());
        tempHash.put("lng", locationLong.toString());
        tempHash.put("msgid", reallyComputeHash(msg));
        tempHash.put("userid", _appInfo.userid);


        myCallSpec.setParams(tempHash);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    // Refresh button simply gets the recent messages and then displays them
    public void clickRefreshButton(View v) {
        spinNtf.setVisibility(View.VISIBLE);
        String result = getRecentMessages();
        if (result != null) {
            displayResult(result);
        }
    }

    // Used to create the random msgid that we upload to post a message
    private String reallyComputeHash(String s) {
        // Computes the crypto hash of string s, in a web-safe format.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getBytes());
            digest.update("My secret key".getBytes());
            byte[] md = digest.digest();
            // Now we need to make it web safe.
            return Base64.encodeToString(md, Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    // This class is used to do the HTTP call, and it specifies how to use the result.
    // Also shows error message when server calls fail

    // HW3: Also finishes spinner progress bar here now, after server is called
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, "Server call failed", duration);
                toast.show();
                Log.i(LOG_TAG, "The server call failed.");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
            }
            spinNtf.setVisibility(View.GONE);
        }
    }

    // Used to get the message list, get the info needed (message & timestamp), then notifies
    // the adapter that this change has been made

    //HW3: Same items displayed, but now gets the userId and conversation boolean
    private void displayResult(String result) {
        Gson gson = new Gson();
        MessageList ml = gson.fromJson(result, MessageList.class);
        // Fills aList, so we can fill the listView.
        aList.clear();
        for (int i = 0; i < ml.messages.length; i++) {
            ListElement ael = new ListElement();
            ael.textLabel = ml.messages[i].msg;
            ael.timeLabel = ml.messages[i].ts;
            ael.userIdLabel = ml.messages[i].userid;
            ael.converseLabel = ml.messages[i].conversation;
            aList.add(ael);
        }
        aa.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
