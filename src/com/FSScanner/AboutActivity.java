package com.FSScanner;

import android.app.Activity;
import android.os.Bundle;


/**
 * Activity to display legal information, application version history etc
 */
public class AboutActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
    }
}