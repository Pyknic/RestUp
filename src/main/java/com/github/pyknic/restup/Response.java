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

import com.google.gson.Gson;
import java.util.Optional;
import java.util.stream.Stream;
import static java.util.Objects.requireNonNull;

/**
 * A response from the server than can be parsed into json, or just read to get
 * the status code.
 * 
 * @author  Emil Forslund
 * @since   1.0.0
 */
public final class Response {
    
    private final int status;
    private final String text;

    public Response(int status, String text) {
        this.status = status;
        this.text   = requireNonNull(text);
    }

    public int getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }
    
    public boolean success() {
        return status == 200;
    }
    
    public <T> Optional<T> decodeJson(Class<T> type) {
        if (text == null || !success()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(GSON.fromJson(text, type));
        }
    }
    
    public <T> Stream<T> decodeJsonArray(Class<T[]> arrayType) {
        return Stream.of(GSON.fromJson(text, arrayType));
    }
    
    private final static Gson GSON = new Gson();
}