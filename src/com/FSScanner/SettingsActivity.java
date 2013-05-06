package com.FSScanner;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.slidingmenu.lib.app.SlidingPreferenceActivity;

import java.math.BigInteger;


public class SettingsActivity extends PreferenceActivity {


    /**
     * Radius field in settings must be validated. Radius cannot be unspecified, zero or more than 100000.
     * If user is trying to input invalid radius value, popup alert dialog is shown and radius value is not
     * changed.
     * @param savedInstanceState
     */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        EditTextPreference radiusEditTextPreference = (EditTextPreference) findPreference(MainActivity.PREFS_RADIUS);
        radiusEditTextPreference.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                final String tmp = newValue.toString();

                // test if user tries to input nothing
                if (tmp.isEmpty()) {
                    alert("Please specify radius!");
                   return false;
                }

                // throws exception if tmp is too large to be int
                int tmpInt;
                try {
                    tmpInt = Integer.parseInt(tmp);
                } catch (NumberFormatException e) {
                    alert("Radius too large, max 100 000!");
                    return false;
                }

                // test if input is 0
                if ( tmpInt == 0 ) {
                    alert("Radius cannot be zero!");
                    return false;
                }
                return true;
            }
        });
    }


    /**
    * Display popup message!
    * @param message
    */
    private void alert(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bad radius!");
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }
}