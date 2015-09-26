package com.example.trafficinfo.trafficinfo;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Class that contains information about a Facebook event that can possibly affect the traffic.
 */
public class TrafficEvent {
    private SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private SimpleDateFormat dFormat_simple = new SimpleDateFormat("yyyy-MM-dd");
    private int attending;
    private Date startDate;
    private String name;
    private String coverPhotoURL;
    private LatLng location;

    public TrafficEvent(JSONObject event) throws Exception {
        String dateStr = event.getString("start_time");
        float lat = Float.parseFloat(event.getString("lat"));
        float lng = Float.parseFloat(event.getString("lng"));
        this.location = new LatLng(lat, lng);
        this.attending = event.getInt("attending");
        this.name = event.getString("name");
        this.coverPhotoURL = event.getString("cover");
        this.parseDate(dateStr);
    }

    private void parseDate(String dateStr) throws Exception {
        try {
            startDate = dFormat.parse(dateStr);
        }
        catch (Exception e) {
            startDate = dFormat_simple.parse(dateStr);
        }
    }

    public LatLng getLocation() {
       return this.location;
    }
    public Date getStartDate() {
        return startDate;
    }
    public int computeRadius() {
        if (this.attending < 100) {
            //return this.attending;
            return 0;
        }
        if (this.attending < 1000) {
            return 50 + this.attending / 10;
        }
        else {
            return 200 + this.attending / 100;
        }
    }
    public int getAttending() {
        return attending;
    }
    public String getName() {
        return name;
    }
    public String getCoverPhotoURL() {
        return coverPhotoURL;
    }
}
