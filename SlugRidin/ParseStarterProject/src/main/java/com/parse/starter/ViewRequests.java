package com.parse.starter;

// This is the Activity for DRIVERS!!
// This activity allows drivers to see a listview showing the closest rides
// Drivers can click on the listview to see an individual ride which will take them to the ViewRiderLocation Activity

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

public class ViewRequests extends AppCompatActivity implements LocationListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;

    Location location;
    ListView listView;
    ArrayList<String> listViewContent;
    ArrayList<String> usernames;
    ArrayList<Double> latitudes;
    ArrayList<Double> longitudes;
    ArrayAdapter arrayAdapter;
    LocationManager locationManager;
    String provider;
    Intent ir;
    String setBusRoute;
    String setCampus;
    String Requests;
    android.os.Handler handler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ir = getIntent();
        setCampus = ir.getStringExtra("campus");
        setBusRoute = ir.getStringExtra("busRoute");
        Requests = "Requests" + setCampus + setBusRoute;
        setTitle("Heading " + setCampus.toUpperCase() + " Campus " + "Route " + setBusRoute);
        //Set up a location manager for the drivers location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        location = locationManager.getLastKnownLocation(provider);

        //If we find a location, update it!
        if ( location != null){
            updateLocation();
        }

        //Creating our ArrayLists to pass through to the ViewRiderLocation
        listView = (ListView) findViewById(R.id.listView);
        listViewContent = new ArrayList<String>();
        usernames = new ArrayList<String>();
        latitudes = new ArrayList<Double>();
        longitudes = new ArrayList<Double>();

        //Placeholder text until we have found requests
        listViewContent.add("Find nearby requests...");
//        Log.i("userLat", Double.toString(location.getLatitude()));
//        Log.i("userLong", Double.toString(location.getLongitude()));



        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listViewContent);
        listView.setAdapter(arrayAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.blue, R.color.purple);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(arrayAdapter);
                        updateLocation();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 2500);
            }
        });
        //Our onItemClickListener determines which listview item was clicked, then passes the pertinent information through.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean check = listViewContent.contains("No nearby request");
                if(!check){
                Intent i = new Intent(getApplicationContext(), ViewRiderLocation.class);
                i.putExtra("username", usernames.get(position));
                i.putExtra("latitude", latitudes.get(position));
                i.putExtra("longitude", longitudes.get(position));
                i.putExtra("userLatitude", location.getLatitude());
                i.putExtra("userLongitude", location.getLongitude());
                i.putExtra("request", Requests);
                startActivity(i);
                }

            }
        });





    }



    //Our Update location function creates a Parse GeoPoint for the driver and saves it in parse.
    //Then we search the parse database for all requests without a driver yet.
    //If we find any results, we loop through them and add the riders username, lat, and lng to our array.
    //Determine how far away the rider is from the driver and round it down to one decimal place.
    //Update the listview with the results.
    public void updateLocation(){
        final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Requests);
        query.whereDoesNotExist("driverUsername");
        query.whereNear("requesterLocation", userLocation);
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if ( e == null){
                    if(objects.size() > 0) {
                        listViewContent.clear();
                        usernames.clear();
                        latitudes.clear();
                        longitudes.clear();
                        for (ParseObject object : objects) {
                            Double distanceInMiles = userLocation.distanceInMilesTo((ParseGeoPoint) object.get("requesterLocation"));
                            Double distaneOneDP = (double) Math.round(distanceInMiles * 10) / 10;
                            listViewContent.add(String.valueOf(distaneOneDP) + " miles");
                            usernames.add(object.getString("requesterUsername"));
                            latitudes.add(object.getParseGeoPoint("requesterLocation").getLatitude());
                            longitudes.add(object.getParseGeoPoint("requesterLocation").getLongitude());
                            }
                    } else {
                        listViewContent.clear();
                        listViewContent.add("No nearby request");
                    }
                        arrayAdapter.notifyDataSetChanged();
                }

            }
        });
    }
    //restart the location updates on phone wake
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        onLocationChanged(location);
    }
    //save battery on phone sleep
    @Override
    protected void onPause() {
        super.onPause();
//        locationManager.removeUpdates(this);
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        onLocationChanged(location);

    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocation();
        ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
        ParseUser.getCurrentUser().saveInBackground();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
