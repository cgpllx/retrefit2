/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit.Converter;
import retrofit.Retrofit;

import com.example.retrefit2.cache1.KOkhttpCache;
import com.example.retrefit2.volleycache.DiskBasedCache;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

/**
 * A {@linkplain Converter.Factory converter} which uses Gson for JSON.
 * <p>
 * Because Gson is so flexible in the types it supports, this converter assumes that it can handle all types. If you are mixing JSON serialization with something else (such as protocol buffers), you must {@linkplain Retrofit.Builder#addConverterFactory(Converter.Factory) add this instance} last to allow the other converters a chance to see their types.
 */
public final class KGsonConverterFactory extends Converter.Factory {
	/**
	 * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and decoding from JSON (when no charset is specified by a header) will use UTF-8.
	 */
	public static KGsonConverterFactory create() {
		return create(new Gson(), null);
	}

	public static KGsonConverterFactory create(DiskBasedCache kOkhttpCache) {
		return create(new Gson(), kOkhttpCache);
	}

	/**
	 * Create an instance using {@code gson} for conversion. Encoding to JSON and decoding from JSON (when no charset is specified by a header) will use UTF-8.
	 */
	public static KGsonConverterFactory create(Gson gson, DiskBasedCache kOkhttpCache) {
		return new KGsonConverterFactory(gson, kOkhttpCache);
	}

	private final Gson gson;
	private final DiskBasedCache kOkhttpCache;

	private KGsonConverterFactory(Gson gson, DiskBasedCache kOkhttpCache) {
		if (gson == null)
			throw new NullPointerException("gson == null");
		this.gson = gson;
		this.kOkhttpCache = kOkhttpCache;
	}

	@Override
	public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
		
		TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
		return new KGsonResponseBodyConverter<>(adapter, kOkhttpCache);
	}

	@Override
	public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
		TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
		return new GsonRequestBodyConverter<>(adapter);
	}
}
