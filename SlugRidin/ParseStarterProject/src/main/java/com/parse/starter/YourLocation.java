package com.parse.starter;



import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static android.support.design.widget.Snackbar.LENGTH_INDEFINITE;

//view rider location and request ride from driver

public class YourLocation extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    String provider;
    TextView infoTextView;
    Button requestUberButton;
    boolean buttonMyLocationClicked = false;
    Boolean requestActive = false;
    android.os.Handler handler = new android.os.Handler();
    boolean driverActive = false;
    boolean turnoff = false;
    Intent ir;
    String setBusRoute;
    String setCampus;
    String Requests;
    boolean alreadyExecuted = false;

    public String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public void showNotification(String message){
        View parentLayout = getWindow().getDecorView().findViewById(android.R.id.content);
        int length;
        if (message.contains("Searching")) {
            length =  Snackbar.LENGTH_INDEFINITE;
        } else {
            length = Snackbar.LENGTH_LONG;
        }
        final Snackbar snackBar = Snackbar.make(parentLayout, message, length);
        snackBar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.show();
//        View parentLayout = getWindow().getDecorView().findViewById(android.R.id.content);
//        Snackbar.make(parentLayout, message, LENGTH_INDEFINITE)
//                .setAction("Action", null).show();
    }


    public void checkForUpdates(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Requests);
        query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0){
                    driverActive = true;
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username", objects.get(0).getString("driverUsername"));
                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if (e == null && objects.size() > 0) {
                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");
                                Location location = locationManager.getLastKnownLocation(provider);
                                if (location != null){
                                        mMap.clear();
                                        ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                                        Double distanceInMiles = driverLocation.distanceInMilesTo(userLocation);
                                        Double distaneOneDP = (double) Math.round(distanceInMiles * 10) / 10;
                                        Log.d("distance", distanceInMiles.toString());
                                        if (distanceInMiles < 0.01){
                                            showNotification("Your driver is here");
                                            requestUberButton.setText("End Search");
//                                            requestActive = false;
                                            driverActive = false;
                                            ParseQuery<ParseObject> query = ParseQuery.getQuery(Requests);
                                            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null){
                                                        for (ParseObject object : objects){
                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });

                                        } else {

                                            if(!alreadyExecuted && requestActive == true){
                                                showNotification("Driver found!");
                                                alreadyExecuted = true;
                                            }

                                            infoTextView.setText("Your driver is " + distaneOneDP.toString() + " miles away!");

                                            ArrayList<Marker> markers = new ArrayList<Marker>();

                                            //Taking Rider LATLNG and creating a marker
                                            LatLng RiderLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                                            markers.add(mMap.addMarker(new MarkerOptions().position(RiderLatLng).title("Your Location")));

                                            //Taking Driver LATLNG and creating a marker
                                            LatLng DriverLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                            markers.add(mMap.addMarker(new MarkerOptions()
                                                    .position(DriverLatLng)
                                                    .title("Driver Location")
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.driversmarkertrimmed))));
                                            //Looping through Markers and adding to builder.
                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            //Building our boundaries
                                            LatLngBounds bounds = builder.build();

                                            //offset from edge of map to our markers
                                            int padding = 350;

                                            //putting it together now
                                            Point displaySizePx = new Point();
                                            Display display = getWindowManager().getDefaultDisplay();
                                            display.getSize(displaySizePx);
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, displaySizePx.x, displaySizePx.y, padding);

                                            //animate and show!
                                            mMap.animateCamera(cu);
                                            if (requestActive == false || turnoff == true) {
                                                turnoff = false;
                                                mMap.clear();
                                                infoTextView.setText("Heading " + capitalizeFirstLetter(setCampus) + " Campus Route " + setBusRoute);
                                            }
                                        }
                                }
                            }
                        }
                    });
//                    if(requestActive) {
//                        infoTextView.setText("Your driver is on the way!");
////                    requestUberButton.setVisibility(View.INVISIBLE);
//                    }
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForUpdates();
                        Log.d("check 1", Boolean.toString(alreadyExecuted));
                    }
                }, 2000);
            }
        });
    }
    //Runs when a user clicks "REQUEST UBER"
    public void requestUber(View view){

        //Allows the user to create a request
        if (requestActive == false) {

            //create a new parse object with public read/write access that passes along our request to the database
            ParseObject request = new ParseObject(Requests);
            request.put("requesterUsername", ParseUser.getCurrentUser().getUsername());//ParseUser.getCurrentUser().getUsername());
            ParseACL parseACL = new ParseACL();
            parseACL.setPublicWriteAccess(true);
            parseACL.setPublicReadAccess(true);
            request.setACL(parseACL);
            request.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        showNotification("Searching for driver...");
//                        infoTextView.setText("Finding Uber driver...");
                        requestUberButton.setText("Cancel");
                        requestActive = true;
                        Location location = locationManager.getLastKnownLocation(provider);
                        updateLocation(location);
                        Log.d("check 2", Boolean.toString(alreadyExecuted));
                        checkForUpdates();
                        alreadyExecuted = false;
//                        Log.d("check 3", Boolean.toString(alreadyExecuted));

                    }
                }
            });

            Log.i("MyApp", "request successful");

        //Allows the user to cancel an already submitted request
        } else {
            Log.i("blah", "deleting request");
            showNotification("Search Cancelled");
            //search Parse for all objects with our username. Then deletes them from the database
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(Requests);
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                        }
                    }
                }
            });
            if (driverActive == false) {
                infoTextView.setText("");
            } else {
                showNotification("Hitch Cancelled");

            }

            requestUberButton.setText("Hitch Ride");
            infoTextView.setText("Heading " + capitalizeFirstLetter(setCampus) + " Campus Route " + setBusRoute);
            requestActive = false;
            turnoff = true;
            Log.d("check 3", Boolean.toString(alreadyExecuted));
            checkForUpdates();
            alreadyExecuted = false;

        }
    }

    public void updateMap(Location location){
        if(driverActive == false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ir = getIntent();
        setCampus = ir.getStringExtra("campus");
        setBusRoute = ir.getStringExtra("busRoute");
        Requests = "Requests" + setCampus + setBusRoute;

        infoTextView = (TextView) findViewById(R.id.infoTextView);
        requestUberButton = (Button) findViewById(R.id.requestUber);
        infoTextView.setText("Heading " + capitalizeFirstLetter(setCampus) + " Campus Route " + setBusRoute);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(Requests);
        query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if( e == null){
                    if (objects.size() > 0){

                        requestActive = true;
                        requestUberButton.setText("Cancel");
                        Log.d("check 4", Boolean.toString(alreadyExecuted));
                        checkForUpdates();
                    }
                }
            }
        });


        //Use the location manager and provider to determine the users location accurate to 1 meter, updated 400 miliseconds
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
        locationManager.requestLocationUpdates(provider, 400, 1, this);
//        if (driverActive){
//            showNotification("Driver found!");
//        }


    }

    //restarts the location search when the user opens the app up
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }
    //pauses location search when the user closes the app to save on battery
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enables the My Location button
        if(driverActive == false) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    buttonMyLocationClicked = true;
                    return false;
                }
            });
        } else {
            mMap.setMyLocationEnabled(false);
        }



//        // Customize the google maps layout style
//        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this,R.raw.google_maps_custom_style);
//        mMap.setMapStyle(style);

        //When our map is ready we use our location to animate the camera and create a marker showing where we are.
        Location location = locationManager.getLastKnownLocation(provider);
        if ( location != null){
            updateMap(location);
            updateLocation(location);

        }
        //Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onLocationChanged(Location location) {

        //Every time we change location we clear the map and update our zoom and marker location.
        updateMap(location);
        updateLocation(location);

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

    //Creates a geopoint for the rider and saves this geopoint in parse.
    //This will allow us to later compare the rider geopoint with the driver geopoint.
    public void updateLocation(Location location){
        if (requestActive) {
            Log.i("blah", "putting location to request");
            final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(Requests);
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.put("requesterLocation", userLocation);
                                object.saveInBackground();
                            }
                        }
                    }
                }
            });
        }
    }
}
