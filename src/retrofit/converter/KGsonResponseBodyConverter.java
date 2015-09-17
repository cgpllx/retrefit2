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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import retrofit.Converter;
import retrofit.Utils;

import com.example.retrefit2.KResult;
import com.example.retrefit2.volleycache.Cache.Entry;
import com.example.retrefit2.volleycache.DiskBasedCache;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Util;

public final class KGsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
	private final TypeAdapter<T> adapter;
	private final DiskBasedCache kOkhttpCache;

	KGsonResponseBodyConverter(TypeAdapter<T> adapter, DiskBasedCache kOkhttpCache) {
		this.adapter = adapter;
		this.kOkhttpCache = kOkhttpCache;
	}

	public DiskBasedCache getkOkhttpCache() {
		return kOkhttpCache;
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

//		Reader reader = value.charStream();
		String string = value.string();
		 Reader reader = new InputStreamReader((new ByteArrayInputStream(string.getBytes())), Util.UTF_8);
		System.out.println();
		// JsonReader rr=new JsonReader(reader);
		// rr.setLenient(true);
		// Reader reader=new InputStreamReader(new ByteArrayInputStream(string.getBytes()),"UTF-8");
		try {
			T t = adapter.fromJson(reader);
			if (t instanceof KResult) {
				KResult kResult = (KResult) t;
				if (kResult != null && kResult.isSuccess()) {
					// 这里进行保存操作
					System.out.println("可以缓存的");
					Entry entry = new Entry(); 
//					String string = value.string();
					System.out.println("string=" + string);
					entry.data = string.getBytes("UTF-8");
					// entry.
					// entry.responseHeaders = request.headers();
					// value.contentType().
					entry.mimeType = value.contentType().toString();
//					value
					System.out.println("entry.mimeType=" + entry.mimeType);
					System.out.println("entry.data=" + entry.data);
					System.out.println("entry.data lenth=" + entry.data.length);
					kOkhttpCache.put(request.urlString(), entry);
					System.out.println("缓存成功");
				}
			}
			return t;
		} finally {
			Utils.closeQuietly(reader);
		}
	}
}
