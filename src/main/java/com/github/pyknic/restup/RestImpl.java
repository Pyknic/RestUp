/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.pyknic.restup;

import static com.github.pyknic.restup.Option.Type.HEADER;
import static com.github.pyknic.restup.Option.Type.PARAM;
import static com.github.pyknic.restup.Rest.encode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Default implementation of the {@link Rest}-interface.
 * 
 * @author  Emil Forslund
 * @since   1.0.0
 */
class RestImpl implements Rest {
    
    private final Protocol protocol;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    RestImpl(Protocol protocol, String host, int port, String username, String password) {
        this.protocol = requireNonNull(protocol);
        this.host     = requireNonNull(host);
        this.port     = port;
        this.username = username; // Can be null
        this.password = password; // Can be null
    }
    
    @Override
    public CompletableFuture<Response> get(String path, Option... option) {
        return send(Method.GET, path, option, NO_STREAM);
    }

    @Override
    public CompletableFuture<Response> post(String path, Option... option) {
        return send(Method.POST, path, option, NO_STREAM);
    }

    @Override
    public CompletableFuture<Response> delete(String path, Option... option) {
        return send(Method.DELETE, path, option, NO_STREAM);
    }

    @Override
    public CompletableFuture<Response> put(String path, Option... option) {
        return send(Method.PUT, path, option, NO_STREAM);
    }

    @Override
    public CompletableFuture<Response> options(String path, Option... option) {
        return send(Method.OPTIONS, path, option, NO_STREAM);
    }
    
    @Override
    public CompletableFuture<Response> get(String path, Iterator<String> uploader, Option... option) {
        return send(Method.GET, path, option, uploader);
    }

    @Override
    public CompletableFuture<Response> post(String path, Iterator<String> uploader, Option... option) {
        return send(Method.POST, path, option, uploader);
    }

    @Override
    public CompletableFuture<Response> delete(String path, Iterator<String> uploader, Option... option) {
        return send(Method.DELETE, path, option, uploader);
    }

    @Override
    public CompletableFuture<Response> put(String path, Iterator<String> uploader, Option... option) {
        return send(Method.PUT, path, option, uploader);
    }

    @Override
    public CompletableFuture<Response> options(String path, Iterator<String> uploader, Option... option) {
        return send(Method.OPTIONS, path, option, uploader);
    }
    
    @Override
    public CompletableFuture<Response> get(String path, String data, Option... option) {
        return send(Method.GET, path, option, new SingletonIterator<>(data));
    }

    @Override
    public CompletableFuture<Response> post(String path, String data, Option... option) {
        return send(Method.POST, path, option, new SingletonIterator<>(data));
    }

    @Override
    public CompletableFuture<Response> delete(String path, String data, Option... option) {
        return send(Method.DELETE, path, option, new SingletonIterator<>(data));
    }

    @Override
    public CompletableFuture<Response> put(String path, String data, Option... option) {
        return send(Method.PUT, path, option, new SingletonIterator<>(data));
    }

    @Override
    public CompletableFuture<Response> options(String path, String data, Option... option) {
        return send(Method.OPTIONS, path, option, new SingletonIterator<>(data));
    }
    
    protected String getProtocol() {
        switch (protocol) {
            case HTTP : return "http";
            case HTTPS : return "https";
            default : throw new UnsupportedOperationException(
                "Unknown enum constant '" + protocol + "'."
            );
        }
    }

    protected String getHost() {
        return host;
    }

    protected int getPort() {
        return port;
    }
    
    protected final URL getUrl(String path, Param... param) {
        try {
            final StringBuilder url = new StringBuilder()
                .append(getProtocol())
                .append("://")
                .append(host);
            
            if (port > 0) {
                url.append(":").append(port);
            }
            
            url.append("/").append(path);
            
            if (param.length > 0) {
                url.append(
                    Stream.of(param)
                        .map(p -> 
                            encode(p.getKey()) + 
                            "=" + 
                            encode(p.getValue())
                        )
                        .collect(joining("&", "?", ""))
                );
            }
            
            return new URL(url.toString());
        } catch (final MalformedURLException ex) {
            throw new RuntimeException("Error building URL.", ex);
        }
    }
    
    private CompletableFuture<Response> send(Method method, String path, Option[] options, Iterator<String> stream) {
        return CompletableFuture.supplyAsync(() -> {
            final Param[] params = Stream.of(options).filter(o -> o.getType() == PARAM).toArray(Param[]::new);
            final Header[] headers = Stream.of(options).filter(o -> o.getType() == HEADER).toArray(Header[]::new);
            
            final URL url = getUrl(path, params);
            
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                
                switch (method) {
                    case POST    : conn.setRequestMethod("POST"); break;
                    case GET     : conn.setRequestMethod("GET"); break;
                    case DELETE  : conn.setRequestMethod("DELETE"); break;
                    case OPTIONS : conn.setRequestMethod("OPTIONS"); break;
                    case PUT     : conn.setRequestMethod("PUT"); break;
                    default : throw new UnsupportedOperationException(
                        "Unknown enum constant '" + method + "'."
                    );
                }
                
                if (username != null && password != null) {
                    final byte[] authentication = (username + ":" + password).getBytes();
                    final String encoding = Base64.getEncoder().encodeToString(authentication);
                    conn.setRequestProperty("Authorization", "Basic " + encoding);
                }
                
                for (final Header header : headers) {
                    conn.setRequestProperty(
                        header.getKey(), 
                        header.getValue());
                }
                
                conn.setUseCaches(false);
                conn.setAllowUserInteraction(false);
                
                if (stream != NO_STREAM) {
                    conn.setDoOutput(true);
                }
                
                conn.connect();
                try (final BufferedWriter wr = new BufferedWriter(
                    new OutputStreamWriter(conn.getOutputStream()))) {
                    while (stream.hasNext()) {
                        final String data = stream.next();
                        wr.append(data);
                    }
                }

                final int status = conn.getResponseCode();
                final String text;
                
                try (final BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                    
                    final StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        sb.append(line);
                    }
                    
                    text = sb.toString();
                }
                
                return new Response(status, text);
            } catch (final IOException ex) {
                throw new RuntimeException("Could not send get-command.", ex);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
    
    private final static Iterator<String> NO_STREAM = new Iterator<String>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String next() {
            throw new IllegalStateException(
                "This method should never be called."
            );
        }
    };
    
    private final static class SingletonIterator<T> implements Iterator<T> {
        
        private final AtomicBoolean first = new AtomicBoolean(true);
        private final T instance;
        
        private SingletonIterator(T instance) {
            this.instance = instance;
        }

        @Override
        public boolean hasNext() {
            return first.compareAndSet(true, false);
        }

        @Override
        public T next() {
            return instance;
        }
    }
}