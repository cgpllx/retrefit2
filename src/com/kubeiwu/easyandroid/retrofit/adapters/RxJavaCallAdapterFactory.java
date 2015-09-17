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
package com.kubeiwu.easyandroid.retrofit.adapters;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit.Call;
import retrofit.CallAdapter;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Utils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import com.example.retrefit2.KResult;
import com.example.retrefit2.MvpException;

/**
 * TODO docs
 */
public final class RxJavaCallAdapterFactory implements CallAdapter.Factory {
	/**
	 * TODO
	 */
	public static RxJavaCallAdapterFactory create() {
		return new RxJavaCallAdapterFactory();
	}

	private RxJavaCallAdapterFactory() {
	}

	@Override
	public CallAdapter<?> get(Type returnType, Annotation[] annotations) {
		Class<?> rawType = Utils.getRawType(returnType);
		boolean isSingle = "rx.Single".equals(rawType.getCanonicalName());
		if (rawType != Observable.class && !isSingle) {
			return null;
		}
		if (!(returnType instanceof ParameterizedType)) {
			String name = isSingle ? "Single" : "Observable";
			throw new IllegalStateException(name + " return type must be parameterized" + " as " + name + "<Foo> or " + name + "<? extends Foo>");
		}

		CallAdapter<Observable<?>> callAdapter = getCallAdapter(returnType);
		if (isSingle) {
			// Add Single-converter wrapper from a separate class. This defers classloading such that
			// regular Observable operation can be leveraged without relying on this unstable RxJava API.
			return SingleHelper.makeSingle(callAdapter);
		}
		return callAdapter;
	}

	private CallAdapter<Observable<?>> getCallAdapter(Type returnType) {
		Type observableType = Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
		Class<?> rawObservableType = Utils.getRawType(observableType);
		if (rawObservableType == Response.class) {
			if (!(observableType instanceof ParameterizedType)) {
				throw new IllegalStateException("Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
			}
			Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) observableType);
			return new ResponseCallAdapter(responseType);
		}

		if (rawObservableType == Result.class) {
			if (!(observableType instanceof ParameterizedType)) {
				throw new IllegalStateException("Result must be parameterized" + " as Result<Foo> or Result<? extends Foo>");
			}
			Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) observableType);
			return new ResultCallAdapter(responseType);
		}

		return new SimpleCallAdapter(observableType);
	}

	static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {
		private final Call<T> originalCall;

		private CallOnSubscribe(Call<T> originalCall) {
			this.originalCall = originalCall;
		}

		@Override
		public void call(final Subscriber<? super Response<T>> subscriber) {//这里的代码可以设置为在线程中运行
			// Since Call is a one-shot type, clone it for each new subscriber.
			final Call<T> call = originalCall.clone();

			// Attempt to cancel the call if it is still in-flight on unsubscription.
			subscriber.add(Subscriptions.create(new Action0() {
				@Override
				public void call() {
					call.cancel();
				}
			}));

			call.enqueue(new Callback<T>() {
				@Override
				public void onResponse(Response<T> response) {
					if (subscriber.isUnsubscribed()) {
						return;
					}
					try {
						T t = response.body();
						if (t != null && t instanceof KResult) {
							KResult kResult = (KResult) t;
							if (kResult == null || !kResult.isSuccess()) {
								subscriber.onError(new MvpException(kResult != null ? kResult.getFailureDesc() : "服务器或网络异常"));
								return;
							}
						}
						subscriber.onNext(response);
					} catch (Throwable t) {
						subscriber.onError(t);
						return;
					}
					subscriber.onCompleted();
				}

				@Override
				public void onFailure(Throwable t) {
					if (subscriber.isUnsubscribed()) {
						return;
					}
					subscriber.onError(t);
				}
			});
		}
	}

	static final class ResponseCallAdapter implements CallAdapter<Observable<?>> {
		private final Type responseType;

		ResponseCallAdapter(Type responseType) {
			this.responseType = responseType;
		}

		@Override
		public Type responseType() {
			return responseType;
		}

		@Override
		public <R> Observable<Response<R>> adapt(Call<R> call) {
			return Observable.create(new CallOnSubscribe<>(call));
		}
	}

	static final class SimpleCallAdapter implements CallAdapter<Observable<?>> {
		private final Type responseType;

		SimpleCallAdapter(Type responseType) {
			this.responseType = responseType;
		}

		@Override
		public Type responseType() {
			return responseType;
		}

		@Override
		public <R> Observable<R> adapt(Call<R> call) {
			return Observable.create(new CallOnSubscribe<>(call)) //
					.flatMap(new Func1<Response<R>, Observable<R>>() {
						@Override
						public Observable<R> call(Response<R> response) {
							if (response.isSuccess()) {
								return Observable.just(response.body());
							}
							return Observable.error(new HttpException(response));
						}
					});
		}
	}

	static final class ResultCallAdapter implements CallAdapter<Observable<?>> {
		private final Type responseType;

		ResultCallAdapter(Type responseType) {
			this.responseType = responseType;
		}

		@Override
		public Type responseType() {
			return responseType;
		}

		@Override
		public <R> Observable<Result<R>> adapt(Call<R> call) {
			return Observable.create(new CallOnSubscribe<>(call)) //
					.map(new Func1<Response<R>, Result<R>>() {
						@Override
						public Result<R> call(Response<R> response) {
							return Result.response(response);
						}
					}).onErrorReturn(new Func1<Throwable, Result<R>>() {
						@Override
						public Result<R> call(Throwable throwable) {
							return Result.error(throwable);
						}
					});
		}
	}
}
