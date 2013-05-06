package com.FSScanner;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class FourSquareRestClient {

    private static final String BASE_URL = "https://api.foursquare.com/v2/venues/";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
