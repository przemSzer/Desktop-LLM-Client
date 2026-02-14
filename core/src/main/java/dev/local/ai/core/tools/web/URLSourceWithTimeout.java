package dev.local.ai.core.tools.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import dev.langchain4j.data.document.source.UrlSource;

public class URLSourceWithTimeout extends UrlSource {

    private final URL url;
    private final int readTimeout;
    private final int connectTimeout;
    public URLSourceWithTimeout(URL url, int readTimeout, int connectTimeout) {
        super(url);
        this.url = url;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
    }

    @Override
    public InputStream inputStream() throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return connection.getInputStream();
    }
}
