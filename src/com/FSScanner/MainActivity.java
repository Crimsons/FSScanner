package com.FSScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
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
import com.slidingmenu.lib.app.SlidingActivity;
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

public class MainActivity extends SherlockActivity {

	// logcat logging
    private static final boolean LOG_ENABLED = true;
    private static final String LOG_TAG = "MainActivity";

    // FourSquare specific contants
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
    private static final String LOC_PROVIDER = "loc_prov";
    private static final String LOCATION = "loc";
    private static final String VENUES_LIST = "ven_list";

    // keys for preferences
    private static final String PREFS_KEYWORD = "keyword";
    private static final String PREFS_PROVIDER = "provider";
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
        slidingMenuSetup();

		// make app icon clickable
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

		// initialize preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // initialize views from main_layout, register listView clickListeners etc.
        setupViews();

		// In case of screen rotation get persisted data from bundle.
        // Otherwise need to get new data from Foursquare.com
        if (savedInstanceState != null) {
			// get data from bundle
            setSupportProgressBarIndeterminateVisibility(false);
            location = savedInstanceState.getParcelable(LOCATION);
            venuesList.addAll( savedInstanceState.getParcelableArrayList(VENUES_LIST) );
			// compose listView
            drawListView();
		} else {
            refreshVenues();
		}
	}


    /**
     * Setup slidingMenu
     */
    private void slidingMenuSetup() {

        SlidingMenu menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindWidthRes(R.dimen.slidingmenu_width);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
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

            // get user location
            if ( !locationSetup() ) {
                makeToast("Enable location provider, then refresh!");
                pullToRefreshListView.onRefreshComplete();
                showProgressBar(false);
            }
        } else {
            makeToast("No internet connection!");
            pullToRefreshListView.onRefreshComplete();
            showProgressBar(false);
        }
    }


    /**
     * Logics to get users current location. Each time user does refresh, location update
     * is called. If location was fixed, applications global location is updated and the
     * value is used in next refresh query. If it is initial call to get location
     * (location == null), then getLastKnownLocation() is called.
     * @return  True if it was possible to register location listener. False if selected
     *          location provider was disabled or setting initial location failed.
     */
    private boolean locationSetup() {
        if (LOG_ENABLED) Log.d(LOG_TAG, "locationSetup()");

        // Test if user has changed location accuracy settings in preferences menu.
        // If we have location already and user has not changed settings, then
        // no need to get new location and method returns.
        String providerFromPrefs = preferences.getString(PREFS_PROVIDER, "");
        if ( location != null && location.getProvider().equals(providerFromPrefs)) {
            getFourSquareVenues();
            return true;
        }

        // inform user about location detection, may take some time.
        makeToast("Detecting your location, please wait!");

        // choose location provider according to preferences
        // and test if the provider is enabled
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider;
        if (providerFromPrefs.equals(LocationManager.GPS_PROVIDER)) {
            if ( locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
                locationProvider = LocationManager.GPS_PROVIDER;
            } else {
                makeToast("GPS disabled!");
                return false;
            }
        } else {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationProvider = LocationManager.NETWORK_PROVIDER;
            } else {
                makeToast("Network location disabled!");
                return false;
            }
        }

        // create locationlistener. When loacation detection is finished,
        // getFourSquareVenues() is called.
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location loc) {
                location = loc;
                getFourSquareVenues();
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };

        //register location updates with locationlistener
        if (LOG_ENABLED) Log.d(LOG_TAG, "Location updates requested from: " + locationProvider);
        locationManager.requestSingleUpdate( locationProvider, locationListener, null);

        return true;
    }


    /**
     * The method builds the query URL that is used to get venues from FourSquare.com.
     * URL is built according to user settings in options menu: categories, radius,
     * search keyword. Query URL is passed to HTTP client that runs asynchronously.
     */
	private void getFourSquareVenues() {
        if (LOG_ENABLED) Log.d(LOG_TAG, "getFourSquareVenues()");

		//build query url
		String url = "search?ll=" + location.getLatitude() +
	    		"," + location.getLongitude() + "&intent=browse";

		String keyword = preferences.getString(PREFS_KEYWORD, "");
		if ( !keyword.isEmpty() ) {
			url += "&query=" + keyword;
		}
        url += "&limit=50";

        String radius = preferences.getString(PREFS_RADIUS, "100");
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

		//remove all whitespaces from url
		url = url.replaceAll("\\s", "");
        if (LOG_ENABLED) Log.d(LOG_TAG, "keyword URL: " + url);

		//ask FourSquare.com for venues, response is in JSON format
        FourSquareRestClient.get(url, new JsonHttpResponseHandler(){

            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (LOG_ENABLED) Log.d(LOG_TAG, "JsonHttpResponseHandler: onSuccess()");
                parseJson(jsonObject);
            }

            @Override
            public void onFailure(Throwable throwable, JSONObject jsonObject) {
                if (LOG_ENABLED) Log.d(LOG_TAG, "JsonHttpResponseHandler: onFailure()");
                makeToast("Error retrieving data, try again!");

                // hide refreshing animations
                pullToRefreshListView.onRefreshComplete();
                showProgressBar(false);
            }
        });
	}


    /**
     * All the JSON parsing and loading venues to ArraysList when HTTP client was
     * successfully comlpeted. venuesList has to be reused because it is registered
     * with listView adapter and the reference must not change. Finally listView is
     * updated and loading animations hidden.
     * @param jsonObject JSON object from HTTP client containing venues
     */
    private void parseJson(JSONObject jsonObject) {
        if (LOG_ENABLED) Log.d(LOG_TAG, "parseJson()");

        if ( jsonObject == null) {
            if (LOG_ENABLED) Log.d(LOG_TAG, "jsonObject == null, refreshing!");
            refreshVenues();
            return;
        }

        // clear old data
        venuesList.clear();

        // parse JSON data
        try {
            JSONObject response = jsonObject.getJSONObject("response");
            JSONArray venues = response.getJSONArray("venues");

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
            if (LOG_ENABLED) Log.d(LOG_TAG, "parseJson(): " + e.toString() );
            makeToast("Invalid JSON data, try refresh!");
        }

        //refreshVenues listview
        drawListView();

        //refreshing is complete, hide loading animations
        pullToRefreshListView.onRefreshComplete();
        showProgressBar(false);
    }


    /**
     * Notifies the CustomAdapter that the underlaying data(venuesList) has changed
     * Also if the list id empty, toast is shown
     */
    private void drawListView() {
        if (LOG_ENABLED) Log.d(LOG_TAG, "drawListView()");
        adapter.notifyDataSetChanged();
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
     * The application is using ActionBarSherlock library therefore this method places menu
     * items on actionBar. Global boolean refreshActionItemVisibility determines refresh icon
     * (refresh action item) visibility.
     * @param menu
     * @return
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
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
                //show applications About page
                Intent i = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(i);
                return true;
            case R.id.refresh:
	        	refreshVenues();
	            return true;
            case R.id.sort_by_distance:
                Collections.sort( venuesList, new VenueComparator(VenueComparator.CompareBy.DISTANCE));
                drawListView();
                return true;
            case R.id.sort_by_popularity:
                Collections.sort( venuesList,
                        Collections.reverseOrder(new VenueComparator(VenueComparator.CompareBy.POPULARITY)));
                drawListView();
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
     * Display toast message box
     * @param message Message to be displayed in toast,
     */
    private void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
