package com.example.trafficinfo.trafficinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class IncidentDialog extends DialogFragment {
    AlertDialog.Builder mBuilder;
    View mView;
    FragmentActivity mContext;
    Dialog mReturn;
    Bundle mBundle;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mBundle = this.getArguments();
        mBuilder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.incident_info, null);
        int circleType = mBundle.getInt("circleListType");
        if (circleType == CircleListType.INCIDENT.ordinal()) {
            setPopupInfo();
        }
        mBuilder.setView(mView);
        mReturn = mBuilder.create();

        return mReturn;
    }

    private void setPopupInfo() {
        String name = mBundle.getString("name");
        int numVehicles = mBundle.getInt("num_vehicles");
        int estimatedDuration = mBundle.getInt("estimated_duration");
        String lastReportTime = mBundle.getString("last_report_time");
        TextView textView;
        textView = (TextView) mView.findViewById(R.id.name);
        textView.setText(name);
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.num_vehicles);
        textView.setText("Number of vehicles:");
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.num_vehicles_value);
        textView.setText(((Integer) numVehicles).toString());
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.estimated_duration);
        textView.setText("Estimated duration:");
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.estimated_duration_value);
        if (estimatedDuration / 60 != 0) {
            if (estimatedDuration % 60 != 0) {
                textView.setText(estimatedDuration / 60 + " hours and " +
                        estimatedDuration % 60 + " minutes");
            } else {
                textView.setText(estimatedDuration / 60 + " hours");
            }
        } else {
            if (estimatedDuration % 60 != 0) {
                textView.setText(estimatedDuration % 60 + " minutes");
            } else {
                return;
            }
        }
        textView.setVisibility(View.VISIBLE);

        textView = (TextView) mView.findViewById(R.id.last_report_time);
        textView.setText("Last reported at:");
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.last_report_time_value);
        textView.setText(lastReportTime);
        textView.setVisibility(View.VISIBLE);

        ImageView image = (ImageView) mView.findViewById(R.id.image);
        switch (name.toLowerCase()) {
            case "accident":
                image.setImageResource(R.drawable.accident_icon);
                break;
            case "traffic jam":
                image.setImageResource(R.drawable.traffic_jam_icon);
                break;
            case "road under construction":
                image.setImageResource(R.drawable.construction_icon);
                textView = (TextView) mView.findViewById(R.id.name);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                break;
            case "flood":
                image.setImageResource(R.drawable.flood_icon);
                break;
            case "blizzard":
                image.setImageResource(R.drawable.blizzard_icon);
                break;
            case "other":
                image.setImageResource(R.drawable.other_icon);
                break;
            default:
                break;
        }
        image.setVisibility(View.VISIBLE);
    }

    public void setContext(FragmentActivity context) {
        mContext = context;
    }
}
