package uk.co.snodnipper.okhttp.issue1903;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;
import timber.log.Timber;

public class TestApplication extends Application {

    private Downloader mDownloader;

    @Override
    public void onCreate() {
        super.onCreate();
        mDownloader = new DownloaderImpl(getApplicationContext());
        Log.d(TestApplication.class.getSimpleName(), "Created downloader");
        configureStrictMode();
        Timber.plant(new Timber.DebugTree());
    }

    public Downloader getDownloadLoader() {
        return mDownloader;
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
