package com.example.trafficinfo.trafficinfo;

import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.SearchView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

enum CircleListType {
    NONE, EVENT, CAM_INFO, INCIDENT
}

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private ArrayList<Pair<TrafficEvent,Circle> > mEventsCircles;
    private ArrayList<Pair<TrafficCamInfo,Circle> > mCamCircles;
    private ArrayList<Pair<ReportedIncident,Circle> > mIncidentsCircles;
    private Location mCurrentLocation;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Circle> mTodayEvents, mThreeDaysEvents, mWeekEvents, mMonthEvents;
    private Menu mMenu;
    public static final String TAG = MapsActivity.class.getSimpleName();

    // Color for reported incidents (red).
    private int mColorIncidents = 0x60FF3300;
    // Color for Facebook events (blue).
    private int mColorEvents = 0x80519BFC;
    // Color for traffic cams (purple).
    private int mColorCams = 0x809966FF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent curr_intent = getIntent();
        if(android.os.Build.VERSION.SDK_INT < 11) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);

        mEventsCircles = new ArrayList<Pair<TrafficEvent, Circle>>();
        mCamCircles = new ArrayList<Pair<TrafficCamInfo, Circle>>();
        mIncidentsCircles = new ArrayList<Pair<ReportedIncident, Circle>>();

        setContentView(R.layout.activity_main);

        setUpMapIfNeeded();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        handleIntent(curr_intent);

        if(!haveNetworkConnection()) {
            Toast.makeText(getApplicationContext(), "No connection available.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        this.mMenu = menu;
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(
                new ComponentName(getApplicationContext(), MapsActivity.class)));

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * On selecting action bar icons
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        boolean notChecked;
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
            case R.id.action_refresh:
                refresh();
                return true;
            case R.id.action_report_incident:
                ReportTrafficIncident();
                return true;
            case R.id.events_today:
                notChecked = item.isChecked();
                this.hideAllCircles();
                if (!notChecked && this.mTodayEvents != null) {
                    for (int i = 0; i < this.mTodayEvents.size(); i++) {
                        this.mTodayEvents.get(i).setVisible(true);
                    }
                    item.setChecked(true);
                }
                return true;
            case R.id.events_threedays:
                notChecked = item.isChecked();
                this.hideAllCircles();
                if (!notChecked && this.mThreeDaysEvents != null) {
                    for (int i = 0; i < this.mThreeDaysEvents.size(); i++) {
                        this.mThreeDaysEvents.get(i).setVisible(true);
                    }
                    item.setChecked(true);
                }
                return true;
            case R.id.events_week:
                notChecked = item.isChecked();
                this.hideAllCircles();
                if (!notChecked && this.mWeekEvents != null) {
                    for (int i = 0; i < this.mWeekEvents.size(); i++) {
                        this.mWeekEvents.get(i).setVisible(true);
                    }
                    item.setChecked(true);
                }
                return true;
            case R.id.events_month:
                notChecked = item.isChecked();
                this.hideAllCircles();
                if (!notChecked && this.mMonthEvents != null) {
                    for (int i = 0; i < this.mMonthEvents.size(); i++) {
                        this.mMonthEvents.get(i).setVisible(true);
                    }
                    item.setChecked(true);
                }
                return true;
            case R.id.action_about:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void hideAllCircles() {
        mMenu.findItem(R.id.events_today).setChecked(false);
        mMenu.findItem(R.id.events_threedays).setChecked(false);
        mMenu.findItem(R.id.events_week).setChecked(false);
        mMenu.findItem(R.id.events_month).setChecked(false);
        for (int i = 0; i < mEventsCircles.size(); i++) {
            mEventsCircles.get(i).second.setVisible(false);
        }
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if(!haveNetworkConnection()) {
                Toast.makeText(getApplicationContext(), "No connection available.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Geocoder geocoder = new Geocoder(getApplicationContext());
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocationName(query, 1);
                if (addresses.size() > 0) {
                    double latitude = addresses.get(0).getLatitude();
                    double longitude = addresses.get(0).getLongitude();
                    Location location = new Location("");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    handleNewLocation(location);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Launching new activity
     * */
    private void ReportTrafficIncident() {
        Intent i = new Intent(MapsActivity.this, ReportTrafficIncidentActivity.class);
        i.putExtra("lat", this.mCurrentLocation.getLatitude());
        i.putExtra("lng", this.mCurrentLocation.getLongitude());
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Set the onMapClick listener
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLgn) {
                    // Check if the click was inside a circle
                    Pair<Integer, CircleListType> circleIndex = isClickInsideCircle(latLgn);
                    if (circleIndex.first != -1) {
                        // If the circle index represents a Facebook event.
                        if (circleIndex.second.equals(CircleListType.EVENT)) {
                            TrafficEvent event = mEventsCircles.get(circleIndex.first).first;
                            DialogFragment newFragment = new EventInfoDialog();
                            ((EventInfoDialog) newFragment).setContext(MapsActivity.this);
                            Bundle bundle = new Bundle();
                            bundle.putInt("circleListType", CircleListType.EVENT.ordinal());
                            bundle.putString("coverPhotoURL", event.getCoverPhotoURL());
                            bundle.putString("name", event.getName());
                            bundle.putString("start_time", event.getStartDate().toString());
                            bundle.putInt("attending", event.getAttending());
                            newFragment.setArguments(bundle);
                            newFragment.show(getFragmentManager(), "eventInfo");
                        }
                        // If the circle index represents a traffic camera.
                        if (circleIndex.second.equals(CircleListType.CAM_INFO)) {
                            TrafficCamInfo camInfo = mCamCircles.get(circleIndex.first).first;
                            DialogFragment newFragment = new CamInfoDialog();
                            ((CamInfoDialog) newFragment).setContext(MapsActivity.this);
                            Bundle bundle = new Bundle();
                            bundle.putInt("circleListType", CircleListType.CAM_INFO.ordinal());
                            bundle.putInt("trafficIndicator", camInfo.getTrafficIndicator());
                            bundle.putInt("trafficLoad", camInfo.getTrafficLoad().ordinal());
                            bundle.putString("camName", camInfo.getCamName());
                            bundle.putString("reportTime", camInfo.getReportTime().toString());
                            bundle.putString("prediction", camInfo.getPrediction().toString());
                            newFragment.setArguments(bundle);
                            newFragment.show(getFragmentManager(), "camInfo");
                        }
                        // If the circle index represents a reported traffic incident.
                        if (circleIndex.second.equals(CircleListType.INCIDENT)) {
                            ReportedIncident incident = mIncidentsCircles.get(circleIndex.first).first;
                            DialogFragment newFragment = new IncidentDialog();
                            ((IncidentDialog) newFragment).setContext(MapsActivity.this);
                            Bundle bundle = new Bundle();
                            bundle.putInt("circleListType", CircleListType.INCIDENT.ordinal());
                            bundle.putString("name", incident.getName());
                            bundle.putInt("num_vehicles", incident.getNumberVehicles());
                            bundle.putInt("estimated_duration", incident.getEstimatedDuration());
                            bundle.putString("last_report_time", incident.getLastReportTime());
                            newFragment.setArguments(bundle);
                            newFragment.show(getFragmentManager(), "incidentInfo");
                        }
                    }
                }
            });
            // Set the onLocationChange listener
            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location arg0) {
                    mCurrentLocation = arg0;
                    Log.e("LOCATION", "My location is " + arg0.getLatitude() + " " + arg0.getLongitude());
                }
            });
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private Pair<Integer, CircleListType> isClickInsideCircle(LatLng clickLocation) {
        ArrayList<Pair<Integer, CircleListType> > candidates = new ArrayList<>();
        // Check for events circles.
        for (int i = 0; i < mEventsCircles.size(); i++) {
            float[] results = new float[1];
            Location.distanceBetween(clickLocation.latitude, clickLocation.longitude,
                    mEventsCircles.get(i).second.getCenter().latitude,
                    mEventsCircles.get(i).second.getCenter().longitude,
                    results);
            if (results[0] <= mEventsCircles.get(i).second.getRadius() &&
                    mEventsCircles.get(i).second.isVisible()) {
                candidates.add(new Pair(i, CircleListType.EVENT));
            }
        }

        // Check for traffic cams circles.
        for (int i = 0; i < mCamCircles.size(); i++) {
            float[] results = new float[1];
            Location.distanceBetween(clickLocation.latitude, clickLocation.longitude,
                    mCamCircles.get(i).second.getCenter().latitude,
                    mCamCircles.get(i).second.getCenter().longitude,
                    results);
            if (results[0] <= mCamCircles.get(i).second.getRadius() &&
                    mCamCircles.get(i).second.isVisible()) {
                candidates.add(new Pair(i, CircleListType.CAM_INFO));
            }
        }

        // Check for traffic incidents circles.
        for (int i = 0; i < mIncidentsCircles.size(); i++) {
            float[] results = new float[1];
            Location.distanceBetween(clickLocation.latitude, clickLocation.longitude,
                    mIncidentsCircles.get(i).second.getCenter().latitude,
                    mIncidentsCircles.get(i).second.getCenter().longitude,
                    results);
            if (results[0] <= mIncidentsCircles.get(i).second.getRadius() &&
                    mIncidentsCircles.get(i).second.isVisible()) {
                candidates.add(new Pair(i, CircleListType.INCIDENT));
            }
        }

        if (candidates.size() == 0) {
            return new Pair(-1, CircleListType.NONE);
        }

        class MyComparator implements Comparator<Pair<Integer, CircleListType>> {
            public int compare(Pair<Integer, CircleListType> ob1, Pair<Integer, CircleListType> ob2){
                Circle circle1 = null, circle2 = null;
                switch (ob1.second) {
                    case EVENT:
                        circle1 = mEventsCircles.get(ob1.first).second;
                        break;
                    case CAM_INFO:
                        circle1 = mCamCircles.get(ob1.first).second;
                        break;
                    case INCIDENT:
                        circle1 = mIncidentsCircles.get(ob1.first).second;
                        break;
                    default:
                        break;
                }

                switch (ob2.second) {
                    case EVENT:
                        circle2 = mEventsCircles.get(ob2.first).second;
                        break;
                    case CAM_INFO:
                        circle2 = mCamCircles.get(ob2.first).second;
                        break;
                    case INCIDENT:
                        circle2 = mIncidentsCircles.get(ob2.first).second;
                        break;
                    default:
                        break;
                }

                if (circle1 == null || circle2 == null) {
                    return Integer.MIN_VALUE;
                }
                return (int) (circle1.getRadius() - circle2.getRadius());
            }
        }

        Collections.sort(candidates, new MyComparator());
        // Return the best match
        return candidates.get(0);
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        new HttpGetAsyncTask(this, "FULL").execute("http://optimal-aurora-92818.appspot.com/events");
    }

    public void refresh() {
        new HttpGetAsyncTask(this, "FAST").execute("http://optimal-aurora-92818.appspot.com/events");
    }

    public void setDataFromServer(String jsonStr, String mode) {
        if (jsonStr == null || jsonStr == "") {
            Log.d("ERROR", "empty jsonStr");
            return;
        }

        JSONObject serverData = null;
        try {
            serverData = new JSONObject(jsonStr);
        }
        catch (Exception e) {
            Log.d("ERROR", e.toString());
            return;
        }

        if (mode == "FULL") {
            this.mTodayEvents = new ArrayList<>();
            this.mThreeDaysEvents = new ArrayList<>();
            this.mWeekEvents = new ArrayList<>();
            this.mMonthEvents = new ArrayList<>();
        }

        if (mIncidentsCircles != null) {
            for (int i = 0; i < mIncidentsCircles.size(); i++) {
                mIncidentsCircles.get(i).second.setVisible(false);
            }
            mIncidentsCircles.clear();
        }
        if (mCamCircles != null) {
            for (int i = 0; i < mCamCircles.size(); i++) {
                mCamCircles.get(i).second.setVisible(false);
            }
            mCamCircles.clear();
        }
        this.mIncidentsCircles = new ArrayList<>();
        this.mCamCircles = new ArrayList<>();

        Calendar tmp_date = new GregorianCalendar();
        tmp_date.set(Calendar.HOUR_OF_DAY, 0);
        tmp_date.set(Calendar.MINUTE, 0);
        tmp_date.set(Calendar.SECOND, 0);
        tmp_date.add(Calendar.DAY_OF_MONTH, 1);
        Date midnight = tmp_date.getTime();
        tmp_date.add(Calendar.DAY_OF_MONTH, 2);
        Date threeDays = tmp_date.getTime();
        tmp_date.add(Calendar.DAY_OF_MONTH, 5);
        Date week = tmp_date.getTime();
        tmp_date.add(Calendar.DAY_OF_MONTH, 23);
        Date month = tmp_date.getTime();

        try {
            JSONArray incidentsArray = serverData.getJSONArray("incidents");
            JSONArray camArray = serverData.getJSONArray("webcams");

            if (mode == "FULL") {
                JSONArray eventsArray = serverData.getJSONArray("data");

                // Parse FB events.
                for (int i = 0; i < eventsArray.length(); i++) {
                    try {
                        JSONObject jsonEvent = eventsArray.getJSONObject(i);
                        TrafficEvent trafficEvent = new TrafficEvent(jsonEvent);
                        Circle circle = mMap.addCircle(new CircleOptions()
                                .center(trafficEvent.getLocation())
                                .radius(trafficEvent.computeRadius())
                                .fillColor(mColorEvents)
                                .strokeWidth(0));
                        circle.setVisible(false);
                        mEventsCircles.add(new Pair(trafficEvent, circle));
                        if (trafficEvent.getStartDate().compareTo(midnight) < 0) {
                            mTodayEvents.add(circle);
                        }
                        if (trafficEvent.getStartDate().compareTo(threeDays) < 0) {
                            mThreeDaysEvents.add(circle);
                        }
                        if (trafficEvent.getStartDate().compareTo(week) < 0) {
                            mWeekEvents.add(circle);
                        }
                        if (trafficEvent.getStartDate().compareTo(month) < 0) {
                            mMonthEvents.add(circle);
                        }
                    } catch (Exception e) {
                        Log.d("JSON_FB_EVENT", e.toString());
                    }
                }
            }
            // Parse reported incidents.
            for (int i = 0; i < incidentsArray.length(); i++) {
                try {
                    JSONObject jsonIncident = incidentsArray.getJSONObject(i);
                    ReportedIncident incident = new ReportedIncident(jsonIncident);
                    if (incident.isValid() == false) {
                        continue;
                    }

                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(incident.getLocation())
                            .radius(incident.computeRadius())
                            .fillColor(mColorIncidents)
                            .strokeWidth(0));
                    mIncidentsCircles.add(new Pair(incident, circle));
                    circle.setVisible(true);
                }
                catch (Exception e) {
                    Log.e("JSON_INCIDENT", e.toString());
                }
            }
            // Parse information gathered from traffic cameras.
            for (int i = 0; i < camArray.length(); i++) {
                try {
                    JSONObject jsonCam = camArray.getJSONObject(i);
                    TrafficCamInfo camInfo = new TrafficCamInfo(jsonCam);
                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(camInfo.getLocation())
                            .radius(camInfo.computeRadius())
                            .fillColor(mColorCams)
                            .strokeWidth(0));
                    mCamCircles.add(new Pair(camInfo, circle));
                    circle.setVisible(true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e("JSON_CAM", e.toString());
                }
            }
        }
        catch (Exception e) {
            Log.d("ERROR", serverData.toString());
        }
    }

    private void handleNewLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            // Blank for a moment...
        }
        else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}