package uk.co.snodnipper.okhttp.issue1903;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polygon;

import java.util.*;

public class UrlProvider {

    private final static float PI = 3.1415927f;

    /**
     * The default base endpoint of Mapbox services.
     */
    public static final String MAPBOX_BASE_URL_V4 = "http://a.tile.openstreetmap.org/";

    public static final String USER_AGENT = "Mapbox Android SDK/0.7.0";

    public static final Locale MAPBOX_LOCALE = Locale.US;

    public final static int UNLIMITED = -1;

    public enum RasterImageQuality {
        /** Full image quality. */
        MBXRasterImageQualityFull(0),
        /** 32 color indexed PNG. */
        MBXRasterImageQualityPNG32(1),
        /** 64 color indexed PNG. */
        MBXRasterImageQualityPNG64(2),
        /** 128 color indexed PNG. */
        MBXRasterImageQualityPNG128(3),
        /** 256 color indexed PNG. */
        MBXRasterImageQualityPNG256(4),
        /** 70% quality JPEG. */
        MBXRasterImageQualityJPEG70(5),
        /** 80% quality JPEG. */
        MBXRasterImageQualityJPEG80(6),
        /** 90% quality JPEG. */
        MBXRasterImageQualityJPEG90(7);

        private int value;

        RasterImageQuality(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RasterImageQuality getEnumForValue(int value) {
            switch (value) {
                case 0:
                    return MBXRasterImageQualityFull;
                case 1:
                    return MBXRasterImageQualityPNG32;
                case 2:
                    return MBXRasterImageQualityPNG64;
                case 3:
                    return MBXRasterImageQualityPNG128;
                case 4:
                    return MBXRasterImageQualityPNG256;
                case 5:
                    return MBXRasterImageQualityJPEG70;
                case 6:
                    return MBXRasterImageQualityJPEG80;
                case 7:
                    return MBXRasterImageQualityJPEG90;
                default:
                    return MBXRasterImageQualityFull;
            }
        }
    }

    public static long getUrlCount(Polygon polygon, int minZoom, int maxZoom) {
        Envelope envelope = new Envelope();
        polygon.queryEnvelope(envelope);

        double minLat = envelope.getYMin();
        double minLon = envelope.getXMin();
        double maxLat = envelope.getYMax();
        double maxLon = envelope.getXMax();

        // Loop through the zoom levels and count all tiles for the given zoom level.
        int minX;
        int maxX;
        int minY;
        int maxY;
        int tilesPerSide;
        int count = 0;
        outerloop:
        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            tilesPerSide = Double.valueOf(Math.pow(2.0, zoom)).intValue();
            minX = Double.valueOf(Math.floor(((minLon + 180.0) / 360.0) * tilesPerSide)).intValue();
            maxX = Double.valueOf(Math.floor(((maxLon + 180.0) / 360.0) * tilesPerSide)).intValue();
            minY = Double.valueOf(Math.floor((1.0 - (Math.log(Math.tan(maxLat * PI / 180.0) + 1.0 / Math.cos(maxLat * PI / 180.0)) / PI)) / 2.0 * tilesPerSide)).intValue();
            maxY = Double.valueOf(Math.floor((1.0 - (Math.log(Math.tan(minLat * PI / 180.0) + 1.0 / Math.cos(minLat * PI / 180.0)) / PI)) / 2.0 * tilesPerSide)).intValue();
            int x1 = maxX - minX + 1;
            int y1 = maxY - minY + 1;
            count += x1 * y1;
        }
        return count;
    }

    /**
     * Note: the polygon must comprising of four corners of map in map coordinates. The first vertex in polygon
     * represents top-left corner. The second vertex represents top-right corner. The third vertex represents
     * bottom-right corner. The fourth vertex represents bottom-left corner.
     * @param polygon containing WGS84 / EPSG:4326 coordinates aka latitude / longitude
     * @param minZoom restrict URLS to this zoom level and above.  See mapbox SDK, TileLayerConstants#ZOOM_LEVEL_MIN.
     * @param maxZoom restrict URLS to this zoom level and below.  See mapbox SDK, TileLayerConstants#ZOOM_LEVEL_MAX.
     * @return a set of URLs (inc. parameters) for the supplied {@code polygon}
     */
    public static Set<String> getUrls(Polygon polygon,
                                      int minZoom, int maxZoom) {
        return getUrls(polygon, minZoom, maxZoom, UNLIMITED);
    }

    private static Set<String> getUrls(Polygon polygon,
                                       int minZoom, int maxZoom, int limit) {
        boolean unlimited = limit == UNLIMITED;

        Envelope envelope = new Envelope();
        polygon.queryEnvelope(envelope);

        double minLat = envelope.getYMin();
        double minLon = envelope.getXMin();
        double maxLat = envelope.getYMax();
        double maxLon = envelope.getXMax();

        // Loop through the zoom levels and lat/lon bounds to generate a list of urls which should
        // be included in the offline map.
        Set<String> urls = new LinkedHashSet<>();
        UrlProvider.RasterImageQuality quality = UrlProvider.RasterImageQuality.MBXRasterImageQualityFull;

        int minX;
        int maxX;
        int minY;
        int maxY;
        int tilesPerSide;
        outerloop:
        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            tilesPerSide = Double.valueOf(Math.pow(2.0, zoom)).intValue();
            minX = Double.valueOf(Math.floor(((minLon + 180.0) / 360.0) * tilesPerSide)).intValue();
            maxX = Double.valueOf(Math.floor(((maxLon + 180.0) / 360.0) * tilesPerSide)).intValue();
            minY = Double.valueOf(Math.floor((1.0 - (Math.log(Math.tan(maxLat * PI / 180.0) + 1.0 / Math.cos(maxLat * PI / 180.0)) / PI)) / 2.0 * tilesPerSide)).intValue();
            maxY = Double.valueOf(Math.floor((1.0 - (Math.log(Math.tan(minLat * PI / 180.0) + 1.0 / Math.cos(minLat * PI / 180.0)) / PI)) / 2.0 * tilesPerSide)).intValue();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    urls.add(getMapTileURL(zoom, x, y));
                    if (!unlimited && urls.size() == limit) {
                        System.out.println("breaking with limit" + limit);
                        break outerloop;
                    }
                }
            }
        }
        return urls;
    }

    private static String getMapTileURL(int zoom, int x, int y) {
        return String.format(MAPBOX_LOCALE, MAPBOX_BASE_URL_V4 + "/%d/%d/%d.png", zoom, x, y);
    }

    public static Set<String> getUrls(List<Polygon> polygons,
                                      final int minZoom, final int maxZoom) {
        Set<String> toKeep = new HashSet<>();
        for (Polygon polygon : polygons) {
            Set<String> batch = UrlProvider.getUrls(polygon, minZoom, maxZoom);
            toKeep.addAll(batch);
        }
        return toKeep;
    }
}
