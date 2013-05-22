package com.FSScanner;


import android.app.Application;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;


/**
 * ACRA crash reporting in case of critical error.
 * Emails are sent with an ACTION_SEND intent. This means that the user has to
 * pick preferred email client (if no default app set), review and actually send
 * the email. ACRA can be easily reconfigured to send crash log to whatever backend.
 */

// ACRA configuration
@ReportsCrashes(
        formKey = "",
        mailTo = "funkytransistor@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.crash_title,
        resDialogText = R.string.crash_text,
        resDialogCommentPrompt = R.string.crash_comment_text)

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ACRA initialization
        ACRA.init(this);
    }
}
