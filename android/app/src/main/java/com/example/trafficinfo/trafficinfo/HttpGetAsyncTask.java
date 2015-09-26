package com.example.trafficinfo.trafficinfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HttpGetAsyncTask extends AsyncTask<String, Void, String> {
    private MapsActivity mMap;
    private String mMode;

    public HttpGetAsyncTask(MapsActivity map, String mode) {
        this.mMode = mode;
        this.mMap = map;
    }

    protected String doInBackground(String... strUrl) {
        HttpURLConnection urlConnection = null;
        String response = null;
        try {
            URL url = new URL(strUrl[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
        }
        catch (Exception e) {
            Log.d("ERROR", e.toString());
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        this.mMap.setDataFromServer(result, mMode);
    }
}

class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
    View mView;
    Context mContext;
    Bundle mBundle;

    public DownloadImageTask(Context context, View view, Bundle bundle) {
        this.mContext = context;
        this.mView = view;
        this.mBundle = bundle;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        setPopupInfo(result);
    }

    private void setPopupInfo(Bitmap result) {
        String name = mBundle.getString("name");
        String start_time = mBundle.getString("start_time").split(":00 EEST")[0];
        String attending = Integer.toString(mBundle.getInt("attending"));
        TextView textView;
        textView = (TextView) mView.findViewById(R.id.name);
        textView.setText(name);
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.attending);
        textView.setText("Attending: " + attending);
        textView.setVisibility(View.VISIBLE);
        textView = (TextView) mView.findViewById(R.id.start_time);
        textView.setText("Start time: " + start_time);
        textView.setVisibility(View.VISIBLE);
        ImageView imageView = (ImageView) mView.findViewById(R.id.cover);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(result);
    }
}

class HttpPostAsyncTask extends AsyncTask<String, Void, String> {
    protected String doInBackground(String... urls) {
        String URL = urls[0];
        String data = urls[1];
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(URL);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("data_json", data));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            return httpclient.execute(httppost).toString();
        }
        catch (Exception e) {
            Log.e("REPORT_INCIDENT", e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
    }
}