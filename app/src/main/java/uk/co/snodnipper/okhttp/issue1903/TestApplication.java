package uk.co.snodnipper.okhttp.issue1903;

import android.app.Application;
import android.os.StrictMode;
import timber.log.Timber;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        configureStrictMode();
        Timber.plant(new Timber.DebugTree());
    }

    private void configureStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDialog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }
}
