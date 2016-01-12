package uk.co.snodnipper.okhttp.issue1903;

import java.io.IOException;

public interface Downloader {

    byte[] getData(String url) throws IOException;
}
