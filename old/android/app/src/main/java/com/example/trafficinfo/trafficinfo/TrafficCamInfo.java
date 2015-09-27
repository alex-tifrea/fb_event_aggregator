package com.example.trafficinfo.trafficinfo;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

enum TrafficLoad {
    VERY_LIGHT, LIGHT, MODERATE, HEAVY, VERY_HEAVY
}

/*
 * Class that contains the information collected from a traffic CCTV camera.
 */
public class TrafficCamInfo {
    public static float MAX_TRAFFIC_INDICATOR_VALUE = 50000.0f;
    private SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private SimpleDateFormat dFormat_simple = new SimpleDateFormat("yyyy-MM-dd");
    private int mTrafficIndicator;
    private TrafficLoad mTrafficLoad;
    private LatLng mLocation;
    private String mCamName;
    private Date mReportTime;
    private JSONObject mPrediction;

    public TrafficCamInfo(JSONObject camInfo) throws Exception {
        String dateStr = camInfo.getString("report_time");
        this.parseDate(dateStr);
        this.mCamName = camInfo.getString("webcam_name");
        this.mTrafficIndicator = (int) (((float) camInfo.getInt("traffic_indicator")) / MAX_TRAFFIC_INDICATOR_VALUE * 100.0f);
        float lat = Float.parseFloat(camInfo.getString("lat"));
        float lng = Float.parseFloat(camInfo.getString("lng"));
        this.mLocation = new LatLng(lat, lng);
        this.mPrediction = camInfo.getJSONObject("prediction");

        this.mTrafficLoad = TrafficCamInfo.computeTrafficLoad(this.mTrafficIndicator);
    }

    private void parseDate(String dateStr) throws Exception {
        try {
            mReportTime = dFormat.parse(dateStr);
        }
        catch (Exception e) {
            mReportTime = dFormat_simple.parse(dateStr);
        }
    }

    public static TrafficLoad computeTrafficLoad(int trafficIndicator) {
        if (trafficIndicator < 20) {
            return TrafficLoad.VERY_LIGHT;
        } else if (trafficIndicator < 40) {
            return TrafficLoad.LIGHT;
        } else if (trafficIndicator < 60) {
            return TrafficLoad.MODERATE;
        } else if (trafficIndicator < 80) {
            return TrafficLoad.HEAVY;
        } else {
            return TrafficLoad.VERY_HEAVY;
        }
    }

    public int computeRadius() {
        int radius = 0;
        switch (mTrafficLoad) {
            case VERY_LIGHT:
                radius = 25;
                break;
            case LIGHT:
                radius = 50;
                break;
            case MODERATE:
                radius = 100;
                break;
            case HEAVY:
                radius = 150;
                break;
            case VERY_HEAVY:
                radius = 200;
                break;
            default: break;
        }
        return radius;
    }

    public Date getReportTime() { return mReportTime; }

    public TrafficLoad getTrafficLoad() { return mTrafficLoad; }

    public int getTrafficIndicator() { return mTrafficIndicator; }

    public String getCamName() { return mCamName; }

    public LatLng getLocation() { return mLocation; }

    public JSONObject getPrediction() { return this.mPrediction; }
}
