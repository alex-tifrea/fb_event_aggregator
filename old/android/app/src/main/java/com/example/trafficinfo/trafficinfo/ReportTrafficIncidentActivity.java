package com.example.trafficinfo.trafficinfo;

import android.content.Intent;
import android.location.Location;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportTrafficIncidentActivity extends FragmentActivity {
    private Location userLocation;
    private String incidentType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.incidentType = new String();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_traffic_incident);

        // Set new text in the text views that contain the red star.
        TextView textview = (TextView) findViewById(R.id.textView2);
        String text = "<font color=#000000>1) What kind of traffic incident are you dealing with? </font> <font color=#ff0000>*</font>";
        textview.setText(Html.fromHtml(text));
        textview = (TextView) findViewById(R.id.textView4);
        text = "<font color=#000000>2) Estimate the number of vehicles involved: </font> <font color=#ff0000>*</font>";
        textview.setText(Html.fromHtml(text));
        textview = (TextView) findViewById(R.id.textView5);
        text = "<font color=#000000>3) How long do you estimate it will take until the incident is solved: </font> <font color=#ff0000>*</font>";
        textview.setText(Html.fromHtml(text));

        setupUI(findViewById(R.id.parent));
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submit();
            }
        });
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public void setupUI(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if(!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(ReportTrafficIncidentActivity.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    public void onRadioButtonClicked(View view) {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        radioGroup.clearCheck();
        RadioButton button = (RadioButton) view;
        button.setChecked(true);
        this.incidentType = button.getText().toString();
    }

    private void submit() {
        EditText editText1 = (EditText) findViewById(R.id.editText);
        EditText editText2 = (EditText) findViewById(R.id.editText2);
        String numVehiclesStr = editText1.getText().toString();
        String durationStr = editText2.getText().toString();
        int numVehicles, duration;
        try {
            numVehicles= Integer.parseInt(numVehiclesStr);
        }
        catch (Exception e) {
            numVehicles = -1;
        }
        try {
            duration = Integer.parseInt(durationStr);
        }
        catch (Exception e) {
            duration = -1;
        }

        // Check if all the fields are completed.
        boolean isCompleted = true;
        if (duration < 0 || numVehicles < 0 || this.incidentType == "") {
            Toast.makeText(getApplicationContext(), "All required fields must be completed.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = getIntent();
        String lat = Double.toString(i.getDoubleExtra("lat", 0));
        String lng = Double.toString(i.getDoubleExtra("lng", 0));
        Calendar c = Calendar.getInstance();
        String reportTime = "";
        reportTime += c.get(Calendar.YEAR) + "-";
        reportTime += String.format("%02d", c.get(Calendar.MONTH)+ 1) + "-";
        reportTime += String.format("%02d", c.get(Calendar.DAY_OF_MONTH)) + "T";
        reportTime += String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":";
        reportTime += String.format("%02d", c.get(Calendar.MINUTE)) + ":";
        reportTime += String.format("%02d", c.get(Calendar.SECOND)) + "+0300";

        Log.e("DATA", reportTime);

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("report_time", reportTime);
            jsonObj.put("incident_type", this.incidentType);
            jsonObj.put("estimated_num_vehicles", numVehicles);
            jsonObj.put("estimated_duration", duration);
            jsonObj.put("lat", lat);
            jsonObj.put("lng", lng);
        }
        catch (Exception e) {
            Log.e("REPORT_INCIDENT", e.toString());
        }
        String URL = "http://optimal-aurora-92818.appspot.com/reportincident";
        new HttpPostAsyncTask().execute(URL, jsonObj.toString());
        finish();
    }
}