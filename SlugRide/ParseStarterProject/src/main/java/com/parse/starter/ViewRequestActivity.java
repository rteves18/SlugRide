package com.parse.starter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ViewRequestActivity extends AppCompatActivity {

    ListView requestListView;
    ArrayList<String> requests = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    LocationManager locationManager;
    LocationListener locationListener;


    public void updateListView(Location location){
        Log.d("blah!", "1");
        if (location !=null) {
            Log.d("blah!", "2");
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");



        query.whereNear("location", geoPointLocation);
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){

                    Log.d("blah!", "3");
                    Log.d("object size", objects.toString());
                    if (objects.size() > 0) {
                        requests.clear();
                        for (ParseObject object : objects) {
                            Double distanceInMiles = geoPointLocation.distanceInMilesTo((ParseGeoPoint) object.get("requesterLocation"));
                            Double distaneOneDP = (double) Math.round(distanceInMiles *10) / 10;
                            requests.add(String.valueOf(distaneOneDP)+ " miles");
                        }
                    } else {
                        requests.add("No active requests nearby.");
                    }
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastKnownLocation);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);

        setTitle( "Nearby Requests");
        requestListView = (ListView) findViewById(R.id.requestListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requests.clear();
        requests.add("Getting nearby request.");
        requestListView.setAdapter(arrayAdapter);


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    updateListView(lastKnownLocation);
                }
            }
        }
    }
}


