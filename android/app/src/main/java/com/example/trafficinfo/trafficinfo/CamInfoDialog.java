package com.example.trafficinfo.trafficinfo;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeoutException;

public class CamInfoDialog extends DialogFragment {
    AlertDialog.Builder mBuilder;
    View mView;
    FragmentActivity mContext;
    Dialog mReturn;
    Bundle mBundle;
    JSONObject mPrediction;
    int mHour;
    int mDayOfWeek;
    TextView mHint;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mBundle = this.getArguments();
        mBuilder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.cam_info, null);
        int circleType = mBundle.getInt("circleListType");
        try {
            this.mPrediction = new JSONObject(mBundle.getString("prediction"));
        }
        catch (Exception e) {
            Log.e("JSON", e.toString());
        }
        if (circleType == CircleListType.CAM_INFO.ordinal()) {
            setPopupInfo();
        }
        Calendar now = new GregorianCalendar();
        this.mDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        mBuilder.setView(mView);
        mReturn = mBuilder.create();

        return mReturn;
    }

    private void setPopupInfo() {
        String name = mBundle.getString("camName");
        String trafficLoad = TrafficLoad.values()[mBundle.getInt("trafficLoad")].toString().replace('_', ' ');
        int trafficIndicator = Math.min(mBundle.getInt("trafficIndicator"), 100);
        TextView textView;
        textView = (TextView) mView.findViewById(R.id.name);
        textView.setText(name);
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.traffic_load);
        textView.setText("Traffic Load:");
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.traffic_load_value);
        textView.setText(trafficIndicator + "/100 - " + trafficLoad);
        textView.setVisibility(View.VISIBLE);

        // Setup date change listener
        DatePicker datePicker = (DatePicker) mView.findViewById(R.id.datePicker);
        Calendar now = new GregorianCalendar();
        datePicker.init(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
                new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar tmp = new GregorianCalendar();
                        tmp.set(Calendar.YEAR, year);
                        tmp.set(Calendar.MONTH, monthOfYear);
                        tmp.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        CamInfoDialog.this.mDayOfWeek = tmp.get(Calendar.DAY_OF_WEEK);
                        CamInfoDialog.this.updatePrediction();
                    }
                });

        CheckBox button = (CheckBox) mView.findViewById(R.id.show_more);
        button.setVisibility(View.VISIBLE);

        ImageView closeIcon = (ImageView) mView.findViewById(R.id.icon_close);
        closeIcon.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View v) {
                CamInfoDialog.this.dismiss();
            }
        });

        SeekBar hourPicker = (SeekBar) mView.findViewById(R.id.hourPicker);
        hourPicker.setMax(23);

        // Setup hour change listener
        hourPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            TextView mHint = (TextView) mView.findViewById(R.id.seekBarHint);

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = (progress * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
                mHint.setText(progress + ":00");
                mHint.setX(seekBar.getX() + val + seekBar.getThumbOffset() / 2 - mHint.getWidth()/3);
                CamInfoDialog.this.mHour = progress;
                CamInfoDialog.this.updatePrediction();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                mHint.setY((float) (seekBar.getY() - 0.8 * seekBar.getHeight()));
                mHint.setVisibility(View.VISIBLE);
                CamInfoDialog.this.mHour = seekBar.getProgress();
                CamInfoDialog.this.updatePrediction();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHint.setVisibility(View.INVISIBLE);
                    }
                }, 150);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeekBar hourPicker = (SeekBar) mView.findViewById(R.id.hourPicker);
                Time now = new Time();
                now.setToNow();
                hourPicker.setProgress(now.hour);
                DatePicker datePicker = (DatePicker) mView.findViewById(R.id.datePicker);
                mHint = (TextView) mView.findViewById(R.id.seekBarHint);
                Button today = (Button) mView.findViewById(R.id.today);
                CamInfoDialog.this.mHour = hourPicker.getProgress();
                CamInfoDialog.this.updatePrediction();

                CheckBox button = (CheckBox) v;
                if (button.isChecked()) {
                    TextView textView = (TextView) mView.findViewById(R.id.traffic_load);
                    textView.setText("Average Traffic Load:");
                    mHint.setVisibility(View.INVISIBLE);
                    hourPicker.setVisibility(View.VISIBLE);
                    datePicker.setVisibility(View.VISIBLE);
                    today.setVisibility(View.VISIBLE);
                }
                else {
                    TextView textView = (TextView) mView.findViewById(R.id.traffic_load);
                    textView.setText("Traffic Load:");
                    int trafficIndicator = Math.min(mBundle.getInt("trafficIndicator"), 100);
                    String trafficLoad = TrafficLoad.values()[mBundle.getInt("trafficLoad")].toString().replace('_', ' ');
                    textView = (TextView) mView.findViewById(R.id.traffic_load_value);
                    textView.setText(trafficIndicator + "/100 - " + trafficLoad);
                    hourPicker.setVisibility(View.GONE);
                    mHint.setVisibility(View.GONE);
                    datePicker.setVisibility(View.GONE);
                    today.setVisibility(View.GONE);
                }
            }
        });

        Button today = (Button) mView.findViewById(R.id.today);
        today.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeekBar hourPicker = (SeekBar) mView.findViewById(R.id.hourPicker);
                Time now = new Time();
                now.setToNow();
                hourPicker.setProgress(now.hour);
                DatePicker datePicker = (DatePicker) mView.findViewById(R.id.datePicker);
                datePicker.updateDate(now.year, now.month, now.monthDay);
            }
        });
    }

    public void updatePrediction() {
        // Display traffic prediction based on date and time selection
        TextView traffic_load = (TextView) mView.findViewById(R.id.traffic_load_value);

        // 1 == Sunday
        if (this.mDayOfWeek == 1) {
            this.mDayOfWeek = 7;
        }
        else {
            this.mDayOfWeek -= 1;
        }
        int trafficIndicator = -1;
        try {
            trafficIndicator = this.mPrediction
                    .getJSONObject(Integer.toString(this.mDayOfWeek))
                    .getJSONArray(Integer.toString(this.mHour))
                    .getInt(0);
            trafficIndicator = (int) ((float) trafficIndicator / TrafficCamInfo.MAX_TRAFFIC_INDICATOR_VALUE * 100.0f);
            trafficIndicator = Math.min(trafficIndicator, 100);
        }
        catch (Exception e) {
            Log.e("TrafficLoad", e.toString());
        }
        String trafficLoad = TrafficLoad.values()
                [TrafficCamInfo.computeTrafficLoad(trafficIndicator).ordinal()].toString().replace('_', ' ');

        traffic_load.setText(Integer.toString(trafficIndicator) +
                "/100 - " + trafficLoad + " at " + String.format("%02d", this.mHour)+":00");
    }

    public void setContext(FragmentActivity context) {
        mContext = context;
    }
}
