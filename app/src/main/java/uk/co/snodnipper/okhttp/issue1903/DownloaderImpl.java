package uk.co.snodnipper.okhttp.issue1903;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.ByteString;
import timber.log.Timber;

public class DownloaderImpl implements Downloader {
    protected final OkHttpClient mClient;

    private final CacheControl mCacheControl;

    private final Cache mCache;
    private final Context mContext;

    public DownloaderImpl(Context context) {
        mContext = context;

        // configure cache location
        String state = Environment.getExternalStorageState();
        File rootLocation;
        Timber.e("cache state: " + state);

        // Make sure it's available
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            rootLocation = context.getExternalFilesDir(null);
        } else {
            // Load another directory, probably local memory
            rootLocation = new File(System.getProperty("java.io.tmpdir"));
        }


        File cacheDirectory = new File(
                rootLocation +
                        File.separator + "esri-maps-cache" + File.separator + "streaming");
        long cacheSize = 1024 * 1024 * 250L * 1024L * 1024L;

        mCache = new Cache(cacheDirectory, cacheSize);

        mClient = new OkHttpClient.Builder()
                .cache(mCache)
                .build();

        // cache control
        mCacheControl = new CacheControl.Builder()
                .maxStale(90, TimeUnit.DAYS)
                .maxAge(3, TimeUnit.DAYS)
                .build();
    }

    // source: http://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-timeouts
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Closes {@code closeable}, ignoring any checked exceptions. Does nothing
     * if {@code closeable} is null.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public byte[] getData(String url) throws IOException {
        CacheControl cacheControl;

        if (isOnline()) {
            Timber.e("! ONLINE - fetch " + url + " from " + this.toString());
            cacheControl = mCacheControl;
        } else {
            Timber.e("! CACHE - fetch " + url + " from " + this.toString());
            cacheControl = CacheControl.FORCE_CACHE;
        }

        Request request = new Request.Builder()
                .cacheControl(cacheControl)
                .url(url)
                .build();

        byte[] result = null;
        Response resp = null;
        Call call = null;

        try {
            call = mClient.newCall(request);

            resp = call.execute();

            // ORIGINAL resp = mClient.newCall(request).execute();
            if (resp.body() == null) {
                throw new IllegalStateException("Null body!");
            }

            if (resp.body().contentType() != null) {
                result = resp.body().bytes();
                resp.body().close();
            } else {
                String message = resp.message();
                Timber.e("message: " + message);
            }
        } catch (Exception e) {
            if (call != null && call.isCanceled()) {
                call.cancel();
            }

            if (resp != null && resp.body() != null) {
                resp.body().close();
            }
        } finally {
            if (resp != null && resp.body() != null) {
                resp.body().close();
            }
        }

        if (result != null) {
            Timber.e("! WE HAVE DATA " + url);
        } else {
            Timber.e("! GOSH - NO DATA " + url + ".  Key: " + md5Hex(url));
        }
        return result;
    }

    /** Returns a 32 character string containing an MD5 hash of {@code s}. */
    public static String md5Hex(String s) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
            return ByteString.of(md5bytes).hex();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
