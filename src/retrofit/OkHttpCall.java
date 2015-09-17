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
package retrofit;

import static retrofit.Utils.closeQuietly;

import java.io.IOException;

import retrofit.converter.KGsonResponseBodyConverter;
import android.os.SystemClock;
import android.text.TextUtils;

import com.example.retrefit2.volleycache.Cache.Entry;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;

public final class OkHttpCall<T> implements Call<T> {
	private final OkHttpClient client;
	private final RequestFactory requestFactory;
	private final Converter<ResponseBody, T> responseConverter;
	private final Object[] args;

	private volatile com.squareup.okhttp.Call rawCall;
	private boolean executed; // Guarded by this.
	private volatile boolean canceled;

	OkHttpCall(OkHttpClient client, RequestFactory requestFactory, Converter<ResponseBody, T> responseConverter, Object[] args) {
		this.client = client;
		this.requestFactory = requestFactory;
		this.responseConverter = responseConverter;
		this.args = args;
	}

	// We are a final type & this saves clearing state.
	@Override
	public OkHttpCall<T> clone() {
		return new OkHttpCall<>(client, requestFactory, responseConverter, args);
	}

	/**
	 * 请求策越
	 * 
	 * @author Administrator
	 *
	 */
	interface RequestMode {
		String LOAD_DEFAULT = "LOAD_DEFAULT";// 默认不处理
		String LOAD_NETWORK_ONLY = "LOAD_NETWORK_ONLY";// 只从网络获取
		String LOAD_NETWORK_ELSE_CACHE = "LOAD_NETWORK_ELSE_CACHE";// 先从网络获取，网络没有取本地
		String LOAD_CACHE_ELSE_NETWORK = "LOAD_CACHE_ELSE_NETWORK";// 先从本地获取，本地没有取网络
	}

	public interface CacheMode {
		String LOAD_DEFAULT = "RequestMode: " + RequestMode.LOAD_DEFAULT;
		String LOAD_NETWORK_ONLY = "RequestMode: " + RequestMode.LOAD_NETWORK_ONLY;
		String LOAD_NETWORK_ELSE_CACHE = "RequestMode: " + RequestMode.LOAD_NETWORK_ELSE_CACHE;
		String LOAD_CACHE_ELSE_NETWORK = "RequestMode: " + RequestMode.LOAD_CACHE_ELSE_NETWORK;
	}

	@Override
	public void enqueue(final Callback<T> callback) {
		synchronized (this) {
			if (executed)
				throw new IllegalStateException("Already executed");
			executed = true;
		}

		com.squareup.okhttp.Call rawCall;
		final com.squareup.okhttp.Request request;
		try {
			request = requestFactory.create(args);
			// ----------------------------------------------------------------------cgp
			String headerValue = request.header("RequestMode");
			if (!TextUtils.isEmpty(headerValue)) {
				switch (headerValue) {
					case RequestMode.LOAD_NETWORK_ELSE_CACHE:// 先网络然后再缓存
						rawCall = client.newCall(request);
						if (canceled) {
							rawCall.cancel();
						}
						this.rawCall = rawCall;
						exeRequest(callback, rawCall, request, true);
						return;
					case RequestMode.LOAD_CACHE_ELSE_NETWORK:// 先缓存再网络
						// ---------------------充缓存中取
						Response<T> response = execCacheRequest(request);
						if (response != null) {
							callback.onResponse(response);
							return;
						}
						// ---------------------充缓存中取
						// 如果缓存没有就跳出，执行网络请求
					case RequestMode.LOAD_DEFAULT:
					case RequestMode.LOAD_NETWORK_ONLY:
					default:
						break;// 直接跳出
				}
			}
			// ----------------------------------------------------------------------cgp
			rawCall = client.newCall(request);
			if (canceled) {
				rawCall.cancel();
			}
			this.rawCall = rawCall;

			exeRequest(callback, rawCall, request, false);
		} catch (Throwable t) {
			callback.onFailure(t);
			return;
		}

	}

	private void exeRequest(final Callback<T> callback, com.squareup.okhttp.Call rawCall, final com.squareup.okhttp.Request request, final boolean ifNetworkFailsExeCache) {
		rawCall.enqueue(new com.squareup.okhttp.Callback() {
			@Override
			public void onFailure(Request request, IOException e) {
				callback.onFailure(e);
			}

			@Override
			public void onResponse(com.squareup.okhttp.Response rawResponse) {
				Response<T> response;
				try {
					response = parseResponse(rawResponse, request);
				} catch (Throwable e) {
					if (ifNetworkFailsExeCache) {
						response = execCacheRequest(request);
						if (response == null) {
							callback.onFailure(e);
							return;
						}
					} else {
						callback.onFailure(e);
						return;
					}
				}
				callback.onResponse(response);
			}
		});
	}

	private Response<T> execCacheRequest(Request request) {
		SystemClock.sleep(10000);
		if (responseConverter instanceof KGsonResponseBodyConverter) {
			KGsonResponseBodyConverter<T> converter = (KGsonResponseBodyConverter<T>) responseConverter;
			Entry entry = converter.getkOkhttpCache().get(request.urlString());// 充缓存中获取entry
			if (entry != null && entry.data != null) {// 如果有数据就使用缓存
				MediaType contentType = MediaType.parse(entry.mimeType);
				byte[] bytes = entry.data;
				try {
					com.squareup.okhttp.Response rawResponse = new com.squareup.okhttp.Response.Builder()//
							.code(200).request(request).protocol(Protocol.HTTP_1_1).body(ResponseBody.create(contentType, bytes)).build();
					return parseResponse(rawResponse, request);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	public Response<T> execute() throws IOException {// 这里方法没有做缓存处理，不要使用
		synchronized (this) {
			if (executed)
				throw new IllegalStateException("Already executed");
			executed = true;
		}
		Request request = requestFactory.create(args);
		com.squareup.okhttp.Call rawCall = client.newCall(request);
		if (canceled) {
			rawCall.cancel();
		}
		this.rawCall = rawCall;
		return parseResponse(rawCall.execute(), request);
	}

	// TODO 这里讲request也传给了返回结果
	// private com.squareup.okhttp.Call createRawCall() {
	// Request request=requestFactory.create(args);
	// return client.newCall(requestFactory.create(args));
	// }

	private Response<T> parseResponse(com.squareup.okhttp.Response rawResponse, Request request) throws IOException {

		ResponseBody rawBody = rawResponse.body();
		// Remove the body's source (the only stateful object) so we can pass the response along.
		rawResponse = rawResponse.newBuilder().body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength())).build();
		// rawResponse.cacheResponse()
		int code = rawResponse.code();
		if (code < 200 || code >= 300) {
			try {
				// Buffer the entire body to avoid future I/O.
				ResponseBody bufferedBody = Utils.readBodyToBytesIfNecessary(rawBody);
				return Response.error(bufferedBody, rawResponse);
			} finally {
				closeQuietly(rawBody);
			}
		}

		if (code == 204 || code == 205) {
			return Response.success(null, rawResponse);
		}

		ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
		try {
			T body;
			if (responseConverter instanceof KGsonResponseBodyConverter) {
				KGsonResponseBodyConverter<T> converter = (KGsonResponseBodyConverter<T>) responseConverter;
				body = converter.convert(catchingBody, request);
			} else {
				body = responseConverter.convert(catchingBody);
			}
			return Response.success(body, rawResponse);
		} catch (RuntimeException e) {
			// If the underlying source threw an exception, propagate that rather than indicating it was
			// a runtime exception.
			catchingBody.throwIfCaught();
			throw e;
		}
	}

	public void cancel() {
		canceled = true;
		com.squareup.okhttp.Call rawCall = this.rawCall;
		if (rawCall != null) {
			rawCall.cancel();
		}
	}
}
