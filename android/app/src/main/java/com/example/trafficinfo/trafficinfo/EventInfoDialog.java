package com.example.trafficinfo.trafficinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class EventInfoDialog extends DialogFragment {
    AlertDialog.Builder mBuilder;
    View mView;
    FragmentActivity mContext;
    Dialog mReturn;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = this.getArguments();
        mBuilder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.event_info, null);
        int circleType = bundle.getInt("circleListType");
        if (circleType == CircleListType.EVENT.ordinal()) {
            String coverPhotoURL = bundle.getString("coverPhotoURL");
            if (coverPhotoURL != "none") {
                Log.e("COVER", coverPhotoURL);
                new DownloadImageTask(mContext, mView, bundle).execute(coverPhotoURL);
            }
        }
        mBuilder.setView(mView);
        mReturn = mBuilder.create();

        return mReturn;
    }

    public void setContext(FragmentActivity context) {
        mContext = context;
    }
}
