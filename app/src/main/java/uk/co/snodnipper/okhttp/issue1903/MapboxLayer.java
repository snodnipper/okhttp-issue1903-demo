package uk.co.snodnipper.okhttp.issue1903;

import com.esri.android.map.TiledServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;

import java.util.concurrent.RejectedExecutionException;

import timber.log.Timber;

/**
 * See:
 * http://help.arcgis.com/en/webapi/silverlight/apiref/ESRI.ArcGIS.Client~ESRI.ArcGIS.Client.TiledMapServiceLayer.html
 */
public class MapboxLayer extends TiledServiceLayer {
    private int mMinZoomLevel;
    private int mMaxZoomLevel;
    private static final int TILE_DPI = 96;
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final double[] RESOLUTIONS = new double[]{156543.0339279998D, 78271.5169639999D, 39135.7584820001D,
            19567.8792409999D, 9783.93962049996D, 4891.96981024998D, 2445.98490512499D, 1222.99245256249D,
            611.49622628138D, 305.748113140558D, 152.874056570411D, 76.4370282850732D, 38.2185141425366D,
            19.1092570712683D, 9.55462853563415D, 4.77731426794937D, 2.38865713397468D, 1.19432856685505D,
            0.597164283559817D, 0.298582141647617D, 0.149291070823808D, 0.074645535411904D,
            0.037322767705952D, 0.018661383985268D};
    private static final double[] SCALES = new double[]{5.91657527591555E8D, 2.95828763795777E8D, 1.47914381897889E8D,
            7.3957190948944E7D, 3.6978595474472E7D, 1.8489297737236E7D, 9244648.868618D, 4622324.434309D,
            2311162.217155D, 1155581.108577D, 577790.554289D, 288895.277144D, 144447.638572D, 72223.819286D,
            36111.909643D, 18055.954822D, 9027.977411D, 4513.988705D, 2256.994353D, 1128.497176D, 564.248588D,
            282.124294D, 141.062147D, 70.531074D};

    private static String SERVER = "http://a.tile.openstreetmap.org";

    private final static int MIN_ZOOM_LEVEL = 0;
    private final static int MAX_ZOOM_LEVEL = 18;

    private final Downloader mDownloader;

    public MapboxLayer(String name, Downloader downloader) {
        super(true);
        setName(name);
        mDownloader = downloader;
        boolean initLayer = true;
        configure(MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL, initLayer);
    }

    private void configure(int minZoomLevel, int maxZoomLevel, boolean initLayer) {
        if (minZoomLevel >= 0 && minZoomLevel <= maxZoomLevel && maxZoomLevel <= RESOLUTIONS.length) {
            mMinZoomLevel = minZoomLevel;
            mMaxZoomLevel = maxZoomLevel;
            if (initLayer) {
                try {
                    getServiceExecutor().submit(new Runnable() {
                        public void run() {
                            MapboxLayer.this.initLayer();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    Timber.e(e, "cannot init layer");
                }
            }

        } else {
            throw new IllegalArgumentException("minimum or maximum zoom levels are not set correctly");
        }
    }

    protected void initLayer() {
        if (getID() == 0L) {
            nativeHandle = create();
        }

        if (getID() == 0L) {
            changeStatus(OnStatusChangedListener.STATUS.fromInt(-1000));
            Timber.e("borked layer " + getName());
        } else {
            try {
                double[] resolutions = new double[mMaxZoomLevel];
                double[] scales = new double[mMaxZoomLevel];
                System.arraycopy(RESOLUTIONS, 0, resolutions, 0, mMaxZoomLevel);
                System.arraycopy(SCALES, 0, scales, 0, mMaxZoomLevel);
                setDefaultSpatialReference(SpatialReference.create(3857));
                setFullExtent(new Envelope(-2.003750834278E7D, -2.003750834278E7D, 2.003750834278E7D, 2.003750834278E7D));
                Point origin = new Point(-2.003750834278E7D, 2.003750834278E7D);
                int levels = scales.length;
                setTileInfo(new TileInfo(origin, scales, resolutions, levels, TILE_DPI,
                        TILE_WIDTH, TILE_HEIGHT));
                super.initLayer();
            } catch (Exception exception) {
                this.changeStatus(OnStatusChangedListener.STATUS.fromInt(-1007));
                Timber.e(exception, "borked layer " + getName());
            }
        }

    }

    protected byte[] getTile(int lev, int col, int row) throws Exception {
        if (mDownloader == null) {
            Timber.e("TILE ZERO null downloader");
            return new byte[0];
        }
        boolean isCorrectZoomLevel = lev >= mMinZoomLevel && lev <= mMaxZoomLevel;
        if (isCorrectZoomLevel) {
            String url = new StringBuilder()
                    .append(SERVER)
                    .append("/")
                    .append(lev)
                    .append("/")
                    .append(col)
                    .append("/")
                    .append(row)
                    .append(".png")
                    .toString();
            return mDownloader.getData(url);
        } else {
            Timber.e("TILE ZERO isCorrectZoomLevel " + isCorrectZoomLevel + " return zero.");
            return new byte[0];
        }
    }
}
