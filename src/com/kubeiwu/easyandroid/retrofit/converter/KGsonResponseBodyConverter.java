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
package com.kubeiwu.easyandroid.retrofit.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import retrofit.Converter;
import retrofit.Utils;

import com.example.retrefit2.KResult;
import com.google.gson.TypeAdapter;
import com.kubeiwu.easyandroid.cache.volleycache.Cache;
import com.kubeiwu.easyandroid.cache.volleycache.Cache.Entry;
import com.kubeiwu.easyandroid.cache.volleycache.DiskBasedCache;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Util;

public final class KGsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
	private final TypeAdapter<T> adapter;
	private final DiskBasedCache cache;

	KGsonResponseBodyConverter(TypeAdapter<T> adapter, DiskBasedCache kOkhttpCache) {
		this.adapter = adapter;
		this.cache = kOkhttpCache;
	}

	public Cache getCache() {
		return cache;
	}

	@Override
	public T convert(ResponseBody value) throws IOException {

		Reader reader = value.charStream();
		try {
			return adapter.fromJson(reader);
		} finally {
			Utils.closeQuietly(reader);
		}
	}

	public T convert(ResponseBody value, Request request) throws IOException {

		String string = value.string();
		Reader reader = new InputStreamReader((new ByteArrayInputStream(string.getBytes())), Util.UTF_8);
		try {
			T t = adapter.fromJson(reader);
			if (t instanceof KResult) {
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

}
