/* Copyright 2015 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.co.snodnipper.okhttp.issue1903;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import timber.log.Timber;

import java.io.IOException;
import java.util.Set;


public class MainActivity extends Activity {

    private MapView mMapView;
    private Bundle mSavedInstanceState;
    private Downloader mDownloader;
    private Thread mThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // after the content of this activity is set
        // the map can be accessed from the layout
        mMapView = (MapView)findViewById(R.id.map);

        mDownloader = ((TestApplication) getApplication()).getDownloadLoader();
        MapboxLayer mMapboxLayer = new MapboxLayer("OpenStreetMap", mDownloader);
        mMapView.addLayer(mMapboxLayer);

        // Set the Esri logo to be visible, and enable map to wrap around date line.
        mMapView.setEsriLogoVisible(true);
        mMapView.enableWrapAround(true);

        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            public void onStatusChanged(Object source, STATUS status) {
                // Set the map extent once the map has been initialized, and the basemap is added
                // or changed; this will be indicated by the layer initialization of the basemap layer. As there is only
                // a single layer, there is no need to check the source object.

                if (status == OnStatusChangedListener.STATUS.INITIALIZED && source == mMapView) {
                    onMapReady();
                    return;
                }
            }
        });
        mSavedInstanceState = savedInstanceState;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Point center = getWgs84Point();

        boolean hasCenter = center != null;
        if (hasCenter) {
            outState.putDouble("x", center.getX());
            outState.putDouble("y", center.getY());
            outState.putDouble("scale", mMapView.getScale());
        }

        super.onSaveInstanceState(outState);
    }

    public void onDownloadRequest(View view) {
        if (mThread != null) {
            Toast.makeText(this, "Thread already running", Toast.LENGTH_SHORT).show();
            return;
        }

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Polygon p = mMapView.getExtent();
                SpatialReference in = mMapView.getSpatialReference();
                SpatialReference out = SpatialReference.create(SpatialReference.WKID_WGS84);
                Polygon wgs84p = (Polygon) GeometryEngine.project(p, in, out);

                Set<String> urls = UrlProvider.getUrls(wgs84p, 1, 18);
                for (String url : urls) {
                    try {
                        if (MainActivity.this.isDestroyed()) {
                            break;
                        }
                        mDownloader.getData(url);
                    } catch (IOException e) {
                        Timber.e(e, "problem downloading");
                    }
                }
                Timber.d("finished download");
                mThread = null;
            }
        });
        mThread.start();
    }

    /**
     * NOTE: USED TO GET THE CENTER OF AN EXISTING BASEMAP
     * @return a WGS84 point or null if undefined center
     */
    private Point getWgs84Point() {
        if (mMapView == null) {
            return null;
        }
        Point existingCenter = mMapView.getCenter();
        SpatialReference spatialReference = mMapView.getSpatialReference();

        boolean hasSpatialReference = spatialReference != null;
        if (hasSpatialReference) {
            if (!spatialReference.isWGS84()) {
                Point g = existingCenter;
                SpatialReference in = spatialReference;
                SpatialReference out = SpatialReference.create(SpatialReference.WKID_WGS84);
                return (Point) GeometryEngine.project(g, in, out);
            } else {
                return existingCenter;
            }
        }
        return null;
    }

    private void onMapReady() {
        if (mSavedInstanceState != null) {
            double x = mSavedInstanceState.getDouble("x");
            double y = mSavedInstanceState.getDouble("y");
            double scale = mSavedInstanceState.getDouble("scale");
            mMapView.setScale(scale, false);
            mMapView.centerAt(y, x, false);
        }
    }
}
