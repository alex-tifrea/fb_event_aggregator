package com.example.trafficinfo.trafficinfo;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

enum IncidentType {
    NONE, ACCIDENT, TRAFFIC_JAM, ROAD_UNDER_CONSTRUCTION, FLOOD, BLIZZARD, OTHER
}

/*
 * Class that contains the information collected from a traffic CCTV camera.
 */
public class ReportedIncident {
    static final long NUM_MILLISECONDS_PER_MINUTE=60000;
    private SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private SimpleDateFormat dFormat_simple = new SimpleDateFormat("yyyy-MM-dd");
    private Date mLastReportTime;
    private IncidentType mIncidentType;
    private LatLng mLocation;
    private int mAverageDuration;
    private int mAverageNumVehicles;

    public ReportedIncident(JSONObject incident) throws Exception {
        String firstDateStr = incident.getString("first_report_time");
        String lastDateStr = incident.getString("last_report_time");
        this.parseDates(firstDateStr, lastDateStr);
        String incidentType = incident.getString("incident_type");
        switch (incidentType.toLowerCase()) {
            case "traffic accident":
                this.mIncidentType = IncidentType.ACCIDENT;
                break;
            case "traffic jam":
                this.mIncidentType = IncidentType.TRAFFIC_JAM;
                break;
            case "road under construction":
                this.mIncidentType = IncidentType.ROAD_UNDER_CONSTRUCTION;
                break;
            case "flash flood":
                this.mIncidentType = IncidentType.FLOOD;
                break;
            case "snow blizzard":
                this.mIncidentType = IncidentType.BLIZZARD;
                break;
            case "other":
                this.mIncidentType = IncidentType.OTHER;
                break;
            default:
                break;
        }
        float lat = Float.parseFloat(incident.getString("lat"));
        float lng = Float.parseFloat(incident.getString("lng"));
        this.mLocation = new LatLng(lat, lng);
        this.mAverageDuration = incident.getInt("average_duration_time");
        this.mAverageNumVehicles = incident.getInt("average_num_vehicles");
    }

    public int computeRadius() {
        int radius = 0;
        if (mAverageNumVehicles < 5) {
            radius = 25;
        } else if (mAverageNumVehicles < 15) {
            radius = 50;
        } else if (mAverageNumVehicles < 25) {
            radius = 100;
        } else if (mAverageNumVehicles < 50) {
            radius = 150;
        } else {
            radius = 200;
        }
        return radius;
    }

    public LatLng getLocation() {
        return this.mLocation;
    }

    private void parseDates(String firstDateStr, String lastDateStr) throws Exception {
        try {
            this.mLastReportTime = dFormat.parse(lastDateStr);
        }
        catch (Exception e) {
            this.mLastReportTime = dFormat_simple.parse(lastDateStr);
        }
    }

    public boolean isValid() {
        Date now = new Date();
        long startIncident = mLastReportTime.getTime();
        Date endIncident = new Date(startIncident + mAverageDuration * NUM_MILLISECONDS_PER_MINUTE);
        return now.before(endIncident);
    }

    public String getLastReportTime() {
        return DateFormat.getTimeInstance().format(mLastReportTime);
    }

    public String getName() {
        return mIncidentType.toString().replace('_', ' ');
    }

    public int getEstimatedDuration() { return mAverageDuration; }

    public int getNumberVehicles() { return mAverageNumVehicles; }
}
