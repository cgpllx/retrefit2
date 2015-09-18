/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import okio.Buffer;

import com.example.retrefit2.KResult;
import com.google.gson.TypeAdapter;
import com.kubeiwu.easyandroid.cache.volleycache.Cache;
import com.kubeiwu.easyandroid.cache.volleycache.Cache.Entry;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Util;

final class KGsonConverter<T> implements Converter<T> {
	private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private final TypeAdapter<T> typeAdapter;
	private final Cache cache;

	KGsonConverter(TypeAdapter<T> adapter, Cache cache) {
		this.typeAdapter = adapter;
		this.cache = cache;
	}
	
	public Cache getCache() {
		return cache;
	}

	@Override
	public T fromBody(ResponseBody body) throws IOException {
		Reader in = body.charStream();
		try {
			return typeAdapter.fromJson(in);
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {
			}
		}
	}

	public T fromBody(ResponseBody value, Request request) throws IOException {
		String string = value.string();
		Reader reader = new InputStreamReader((new ByteArrayInputStream(string.getBytes())), Util.UTF_8);
		try {
			T t = typeAdapter.fromJson(reader);
			if (t instanceof Result) {
				KResult kResult = (KResult) t;
				if (kResult != null && kResult.isSuccess()) {
					Entry entry = new Entry();

					entry.data = string.getBytes("UTF-8");
					entry.mimeType = value.contentType().toString();
					cache.put(request.urlString(), entry);
				}
			}
			return t;
		} finally {
			Utils.closeQuietly(reader);
		}
	}

	@Override
	public RequestBody toBody(T value) {
		Buffer buffer = new Buffer();
		Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
		try {
			typeAdapter.toJson(writer, value);
			writer.flush();
		} catch (IOException e) {
			throw new AssertionError(e); // Writing to Buffer does no I/O.
		}
		return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
	}
}
