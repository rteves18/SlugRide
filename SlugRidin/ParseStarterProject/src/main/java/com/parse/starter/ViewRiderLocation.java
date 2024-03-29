package com.parse.starter;


import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
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
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

//view rider location as driver

public class ViewRiderLocation extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Intent i;
    String Requests;

    //sets up our back button so the user can exit if they don't like the request.
    public void back(View view){
        Intent intent = new Intent(getApplicationContext(), ViewRequests.class);
        startActivity(intent);
    }

    //if the user likes the request, they can hit accept which starts this code
    public void acceptRequest(View view){

        //search parse for the reuqest that was accepted. Once the reuqest is found,
        // add the drivers username to parse and save so that the request is marked as taken.
        // Now we can open up a new google maps intent so the driver can navigate to the rider.
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Requests);
        query.whereEqualTo("requesterUsername", i.getStringExtra("username"));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if ( e == null){
                    if(objects.size() > 0){
                        for (ParseObject object : objects){
                            object.put("driverUsername", ParseUser.getCurrentUser().getUsername());
                            object.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("http://maps.google.com/maps?daddr=" + i.getDoubleExtra("latitude", 0) + "," + i.getDoubleExtra("longitude", 0)));
                                        startActivity(intent);
                                    }
                                }
                            });


                        }
                    }
                }
            }
        });

    }

    public String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_rider_location);
        TextView status = (TextView) findViewById(R.id.status);

        i = getIntent();
        Requests = i.getStringExtra("request");
        String setCampus = i.getStringExtra("campus");
        String setBusRoute = i.getStringExtra("busRoute");
        status.setText("Heading " + capitalizeFirstLetter(setCampus) + " Campus " + "Route " + setBusRoute);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        mMap.clear();

        ArrayList<Marker> markers = new ArrayList<Marker>();

        //Taking Rider LATLNG and creating a marker
        LatLng RiderLatLng = new LatLng(i.getDoubleExtra("latitude", 0), i.getDoubleExtra("longitude", 0));
        markers.add(mMap.addMarker(new MarkerOptions().position(RiderLatLng).title("Rider Location")));

        //Taking Driver LATLNG and creating a marker
        LatLng DriverLatLng = new LatLng(i.getDoubleExtra("userLatitude", 0), i.getDoubleExtra("userLongitude", 0));
        markers.add(mMap.addMarker(new MarkerOptions()
                        .position(DriverLatLng)
                        .title("Your Location")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.driversmarkertrimmed))));

        //Looping through Markers and adding to builder.
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(Marker marker : markers) {
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

    }
}
