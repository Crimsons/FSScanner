package com.FSScanner;


import android.os.Bundle;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity extends SherlockFragmentActivity {

    private Bundle extras;
    private GoogleMap map;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);

        // enable apps icon as up button
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        extras = getIntent().getExtras();
    }


    /**
     * onResume() is selected because Android system may take the user to Google Play
     * store while setContentView() in onCreate(). When user comes back, onResume is invoked.
     * Read it from Google Maps Android API v2 guide :)
     */
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    /**
     * Read code comments
     */
    private void setUpMapIfNeeded() {
        // do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }


    /**
     * Configure the map. Some configuration is done in map_layout.xml.
     */
    private void setUpMap() {

        // set camera position
        double lat = extras.getDouble(MainActivity.MARKER_LAT);
        double lng = extras.getDouble(MainActivity.MARKER_LNG);
        map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));

        // build info-window text
        String name = extras.getString(MainActivity.VENUE_NAME);
        String address = extras.getString(MainActivity.VENUE_ADDRESS);
        int distance = extras.getInt(MainActivity.VENUE_DISTANCE);
        String snippet = address;
        if ( !snippet.isEmpty() ) {
            snippet += " - ";
        }
        snippet += distance + "m";

        //add marker
        map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(name)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                .showInfoWindow();

        //display my location layer
        map.setMyLocationEnabled(true);
    }


    /**
     * Application icon on actionBar behaves as up button
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}