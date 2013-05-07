package com.FSScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.location.Criteria;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;

import android.widget.*;
import com.actionbarsherlock.view.Window;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.slidingmenu.lib.SlidingMenu;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;

/**
 * FSScaner Version 1.1
 * (C) 2013 Jakob Hoyer
 *
 *
 * This is a simple application that queries FourSquare API for venues and
 * presents them as a list. Also some additional information is displayed
 * on each venue (distance, count of checkins, address if present, category icon).
 * The application also features Google Maps API - when a venue is tapped,
 * it is displayed on a map.
 *
 *
 * Featuring:
 *
 * FourSquare API
 * Google Maps Android API v2
 * Caching icons on SD card
 *
 *
 * The application is including following 3rd party libraries:
 *
 * SlidingMenu - https://github.com/jfeinstein10/SlidingMenu
 * ActionBarSherlock - http://actionbarsherlock.com
 * PulltoRefresh - https://github.com/chrisbanes/Android-PullToRefresh
 * LazyList - https://github.com/thest1/LazyList
 * android-async-http - https://github.com/loopj/android-async-http
 *
 */





public class MainActivity extends SherlockActivity {

	// logcat logging
    private static final boolean LOG_ENABLED = true;
    private static final String LOG_TAG = "MainActivity";

    // FourSquare API specific contants
    private static final String API_VERSION = "20130501";
    private static final String CLIENT_ID = "GD55T2ZOVEHDYE3T4IDA22HLSS12JAJSWUUQLUFEM4CPKLU0";
	private static final String CLIENT_SECRET = "IVDKCQVGOSXP2LRD3I12NULFIJUYL51H1MPLVOX30NGWGAUO";
	private static final String FOOD_CATEGORY = "4d4b7105d754a06374d81259";
	private static final String BARS_CATEGORY = "4bf58dd8d48988d116941735";
	private static final String SHOPS_CATEGORY = "4d4b7105d754a06378d81259";
    private static final String NO_CATEGORY_ICON_URL = "https://foursquare.com/img/categories_v2/none_bg_44.png";

    // keys for Intent extras
    protected static final String VENUE_NAME = "venue_name";
    protected static final String VENUE_ADDRESS = "venue_address";
    protected static final String VENUE_DISTANCE = "venue_distance";
    protected static final String MARKER_LAT = "marker_lat";
    protected static final String MARKER_LNG = "marker_lng";

    // keys for data in savedInstanceState
    private static final String LOCATION = "loc";
    private static final String VENUES_LIST = "ven_list";

    // keys for preferences
    private static final String PREFS_KEYWORD = "keyword";
    protected static final String PREFS_RADIUS = "radius";
    private static final String PREFS_FOOD = "food";
    private static final String PREFS_BARS = "bars";
    private static final String PREFS_SHOPS = "shops";

	// fields
    private Location location = null;
	private List<Parcelable> venuesList = null;
    private CustomAdapter adapter;
	private SharedPreferences preferences;
    private PullToRefreshListView pullToRefreshListView;
	private ListView listView;
    private SlidingMenu menu;

    // RefreshVenues icon (action item on actionbar) visibility.
    // Used in onCreateOptionsMenu()
    private boolean refreshActionItemVisibility = true;


    /**
     * Applications initial setups, initial refresh, etc.
     * @param savedInstanceState If the parameter is not null, list of venues is
     *                           extracted and listView is updated. Otherwise
     *                           refreshVenues() is called to get list of venues.
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // setup indeterminate progress bar (circle) in actionbar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // set layout
        setContentView(R.layout.main_layout);

        // configure SlidingMenu
        setupSlidingMenu();

		// make app icon clickable
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

		// initialize preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // initialize views from main_layout, register listView clickListeners etc.
        setupViews();

		// In case of screen rotation get persisted data from bundle.
        // Otherwise need to get new data from Foursquare.com
        if (savedInstanceState != null) {
			// stop refreshing that was initiated before screen rotation
            stopRefreshMakeToast(false, null);

			// get data from bundle
            location = savedInstanceState.getParcelable(LOCATION);
            venuesList.addAll( savedInstanceState.getParcelableArrayList(VENUES_LIST) );

			// update listView
            updateListView();
		} else {
            refreshVenues();
		}
	}


    /**
     * Setup slidingMenu
     */
    private void setupSlidingMenu() {

        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindWidthRes(R.dimen.slidingmenu_width);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
        menu.setMenu(R.layout.sliding_layout);
    }


    /**
     * Setup pullTorefresh utility, initialize ListView,
     * its adapter and onClick listener.
     */
    private void setupViews() {
        // setup pullToRefresh refreshing
        pullToRefreshListView = (PullToRefreshListView)findViewById(R.id.listview);
        pullToRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                refreshVenues();
            }
        });
        listView = pullToRefreshListView.getRefreshableView();

        // initialize and attach ListView adapter
        venuesList = new ArrayList<Parcelable>();
        adapter = new CustomAdapter(this, venuesList);
        listView.setAdapter(adapter);

        // set listview click handler
        // Intent is used to start MapActivity. Extras for marker and info on the map
        OnItemClickListener mClickedHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Venue venue = (Venue)parent.getAdapter().getItem(position);
                Intent i = new Intent(MainActivity.this, MapActivity.class);
                i.putExtra(VENUE_NAME, venue.getName());
                i.putExtra(VENUE_ADDRESS, venue.getAddress());
                i.putExtra(VENUE_DISTANCE, venue.getDistance());
                i.putExtra(MARKER_LAT, venue.getLatitude());
                i.putExtra(MARKER_LNG, venue.getLongitude());
                startActivity(i);
            }
        };
        listView.setOnItemClickListener(mClickedHandler);
    }


    /**
     * This method starts the chain of methods to update users location, to get
     * new data from FourSquare.com, parse this data from JSON format to ArraysList
     * and finally display the data.
     */
    private void refreshVenues() {
        if (LOG_ENABLED) Log.d(LOG_TAG, "refreshVenues()");

        showProgressBar(true);

        // Check internet connection
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if ( networkInfo != null && networkInfo.isConnected() ) {
            locationSetup();
        } else {
            stopRefreshMakeToast(true, "No internet connection!");
        }
    }


    /**
     * Location detection source depends on available location providers that
     * the user can enable in System settings. Emphasis is on accuracy, so if GPS
     * is enabled, then it is preferred source.
     * Single location update is requested on application startup (location == null)
     * or when best available provider has changed (user has changed System settings
     * while application is on the background)
     */
    private void locationSetup() {
        // log
        if (LOG_ENABLED) Log.d(LOG_TAG, "locationSetup()");

        // get location provider, prefer accurate provider
        // getBestProvider() returns only enabled provider
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setCostAllowed(true);
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        final String locationProvider = locationManager.getBestProvider(criteria, true);

        // if all location providers are disabled
        if ( locationProvider == null ) {
            stopRefreshMakeToast(true, "Location detection disabled!");
            return;
        }

        // if provider has not changed since last location request,
        // we dont need to update location.
        if ( location != null && locationProvider.equals(location.getProvider()) ) {
            getFourSquareVenues();
            return;
        }

        // log
        if (LOG_ENABLED) Log.d(LOG_TAG, "Location updates requested from: " + locationProvider);

        // inform user, takes some additional time
        makeToast("Detecting location, please wait!");

        // create locationlistener. When loacation detection is finished,
        // global location is updated and getFourSquareVenues() is called.
        final LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location loc) {
                // log
                if (LOG_ENABLED) Log.d(LOG_TAG, "onLocationChanged(): " + loc.toString());
                // update location and go on with query to FourSquare
                location = loc;
                getFourSquareVenues();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {
                // log
                if (LOG_ENABLED) Log.d(LOG_TAG, "onProviderDisabled(): " + provider);

                // cancel update if provider is disabled
                locationManager.removeUpdates(this);

                // stop refreshing, inform user
                stopRefreshMakeToast(true, provider + " location disabled!");
            }
        };

        //register location updates with locationlistener
        locationManager.requestSingleUpdate( locationProvider, locationListener, null);
    }


    /**
     * The method builds the query URL that is used to get venues from FourSquare.com.
     * URL is built according to user settings in options menu: categories, radius,
     * search keyword. Query URL is passed to HTTP client that runs asynchronously.
     */
	private void getFourSquareVenues() {
        // log
        if (LOG_ENABLED) Log.d(LOG_TAG, "getFourSquareVenues()");

		// build query url
		String url = "search?ll=" + location.getLatitude() +
	    		"," + location.getLongitude() + "&intent=browse";

		final String keyword = preferences.getString(PREFS_KEYWORD, "");
		if ( !keyword.isEmpty() ) {
			url += "&query=" + keyword;
		}

        final String radius = preferences.getString(PREFS_RADIUS, "100");
        url += "&radius=" + radius;

		url += "&categoryId=";

		if ( preferences.getBoolean(PREFS_FOOD, false) ) {
			url += FOOD_CATEGORY + ",";
		}
		if ( preferences.getBoolean(PREFS_BARS, false) ) {
			url += BARS_CATEGORY + ",";
		}
		if ( preferences.getBoolean(PREFS_SHOPS, false) ) {
			url += SHOPS_CATEGORY + ",";
		}
		url += "&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&v=" + API_VERSION;

		// remove all whitespaces from url
		url = url.replaceAll("\\s", "");

        // log
        if (LOG_ENABLED) Log.d(LOG_TAG, "query URL: " + url);

		// ask FourSquare.com for venues, response is in JSON format
        FourSquareRestClient.get(url, new JsonHttpResponseHandler(){

            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (LOG_ENABLED) Log.d(LOG_TAG, "JsonHttpResponseHandler: onSuccess()");
                parseJson(jsonObject);
            }

            @Override
            public void onFailure(Throwable throwable, JSONObject jsonObject) {
                // log
                if (LOG_ENABLED) Log.d(LOG_TAG, "JsonHttpResponseHandler: onFailure()");

                // hide refreshing animations
                stopRefreshMakeToast(true, "Error retrieving data, try again!");
            }
        });
	}


    /**
     * JSON parsing and loading venues to ArraysList when HTTP client was
     * successfully comlpeted. venuesList has to be reused because it is registered
     * with listView adapter and the reference must not change.
     * Finally listView is updated and loading animations hidden.
     * @param jsonObject JSON object from HTTP client containing venues
     */
    private void parseJson(JSONObject jsonObject) {
        // log
        if (LOG_ENABLED) Log.d(LOG_TAG, "parseJson()");

        if ( jsonObject == null) {
            // log
            if (LOG_ENABLED) Log.d(LOG_TAG, "jsonObject == null, refreshing!");

            // hide refreshing animation, inform user
            stopRefreshMakeToast(true, "Error, try refresh!");
            return;
        }

        // clear old data
        venuesList.clear();

        // parse JSON data
        try {
            final JSONObject response = jsonObject.getJSONObject("response");
            final JSONArray venues = response.getJSONArray("venues");

            for (int i = 0; i < venues.length(); i++) {
                Venue venue = new Venue();
                JSONObject venueJSON = venues.optJSONObject(i);
                JSONObject venueLocationJSON = venueJSON.optJSONObject("location");
                JSONObject venueCategoriesJSON = venueJSON.optJSONArray("categories").optJSONObject(0);
                JSONObject venueStatsJSON = venueJSON.optJSONObject("stats");

                venue.setName(venueJSON.getString("name"));
                venue.setAddress(venueLocationJSON.optString("address"));
                venue.setDistance(venueLocationJSON.optInt("distance"));
                // category is optional
                if (!(venueCategoriesJSON == null)) {
                    venue.setIconUrl(venueCategoriesJSON.optJSONObject("icon").optString("prefix")
                            + "bg_44.png");
                } else {
                    venue.setIconUrl(NO_CATEGORY_ICON_URL);
                }
                venue.setLatitude(venueLocationJSON.optDouble("lat"));
                venue.setLongitude(venueLocationJSON.optDouble("lng"));
                venue.setCheckinsCount(venueStatsJSON.optInt("checkinsCount", 0));

                venuesList.add(venue);
            }
        } catch (JSONException e) {
            // log
            if (LOG_ENABLED) Log.d(LOG_TAG, "parseJson(): " + e.toString() );

            // hide refreshing animation, inform user
            stopRefreshMakeToast(true, "Error, try refresh!");
        }

        //refreshVenues listview
        updateListView();

        //refreshing is complete, hide loading animations
        stopRefreshMakeToast(false, null);
    }


    /**
     * Notifies the CustomAdapter that the underlaying data(venuesList) has changed
     * Also if the list id empty, toast message is shown
     */
    private void updateListView() {
        // log
        if (LOG_ENABLED) Log.d(LOG_TAG, "updateListView()");

        // if no venues was received from FourSquare
        if (venuesList.isEmpty()) makeToast("No venues found!");

        // update ListView
        adapter.notifyDataSetChanged();
    }


    /**
     * Stop refreshing and show Toast message.
     * @param makeToast makes Toast message if true
     * @param message   message to show on Toast
     */
    private void stopRefreshMakeToast(boolean makeToast, String message) {
        // inform user
        if ( makeToast ) makeToast( message );

        // hide refreshing animations
        pullToRefreshListView.onRefreshComplete();
        showProgressBar(false);
    }


    /**
     * Replaces refresh icon (action item on actionbar) with indeterminate progressBar
     * and vice versa.
     * @param visible If true, refresh icon is made invisible and progressBar is shown.
     */
    private void showProgressBar(boolean visible) {
        if (LOG_ENABLED) Log.d(LOG_TAG, "showProgressBar(" + visible + ")");

        //toggle action item
        refreshActionItemVisibility = !visible;
        supportInvalidateOptionsMenu();
        //toggle progress bar
        setSupportProgressBarIndeterminateVisibility(visible);
    }


    /**
     * Display toast message box
     * @param message Message to be displayed in toast,
     */
    private void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    /**
     * The application is using ActionBarSherlock library therefore this method places menu
     * items on actionBar. Global boolean refreshActionItemVisibility determines refresh icon
     * (refresh action item) visibility.
     * @param menu
     * @return
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.menu, menu);

        //test if refreshVenues action item needs to be hidden
        if (!refreshActionItemVisibility) {
            menu.findItem(R.id.refresh).setVisible(false);
        }
        return true;
	}


    /**
     * Connects action items with their actions.
     * @param item Item on actionBar that was tapped.
     * @return
     */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    switch (item.getItemId()) {
            case android.R.id.home:
                // toggle SlidingMenu
                menu.toggle();
                return true;
            case R.id.refresh:
	        	refreshVenues();
	            return true;
            case R.id.sort_by_distance:
                Collections.sort( venuesList, new VenueComparator(VenueComparator.CompareBy.DISTANCE));
                updateListView();
                return true;
            case R.id.sort_by_popularity:
                Collections.sort( venuesList,
                        Collections.reverseOrder(new VenueComparator(VenueComparator.CompareBy.POPULARITY)));
                updateListView();
                return true;
            case R.id.settings:
	        	Intent j = new Intent(MainActivity.this, SettingsActivity.class);
	    	    startActivity(j);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}


    /**
     * Saves venuesList to bundle so incase of screen rotation we can keep the venue data,
     * don't have to requery FourSquare.com.
     * @param outState
     */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable( LOCATION, location );
        outState.putParcelableArrayList( VENUES_LIST, (ArrayList<Parcelable>)venuesList );
	}

}
