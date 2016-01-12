# okhttp-issue1903-demo

The okhttp cache needs to rebuilt when URL requests are cancelled.  The ESRI Android SDK will cancel requests when panning / zooming etc., which will prevent new okhttp cache entries from being written.

The download button should download the tiles current in view (until the screen is rotated or all tiles are downloaded)

https://github.com/square/okhttp/issues/1903