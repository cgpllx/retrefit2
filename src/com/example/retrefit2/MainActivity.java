package com.example.retrefit2;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.KGsonConverterFactory;
import retrofit.KRetrofit;
import retrofit.Response;
import retrofit.RxJavaCallAdapterFactory;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.kubeiwu.commontool.khttp.cache.disk.Utils;
import com.kubeiwu.easyandroid.cache.volleycache.DiskBasedCache;
import com.kubeiwu.easyandroid.manager.cookiesmanager.PersistentCookieStore;
import com.squareup.okhttp.OkHttpClient;

public class MainActivity extends Activity {
	TextView text;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.text);
		OkHttpClient client = new OkHttpClient();
		client.setConnectTimeout(15 * 1000, TimeUnit.MILLISECONDS);
		client.setReadTimeout(20 * 1000, TimeUnit.MILLISECONDS);
		client.setCookieHandler(new CookieManager(new PersistentCookieStore(getApplicationContext()), CookiePolicy.ACCEPT_ORIGINAL_SERVER));
		DiskBasedCache kOkhttpCache = new DiskBasedCache(Utils.getDiskCacheDir(getApplicationContext(), "volleycache1"));
		kOkhttpCache.initialize();
		KRetrofit retrofit = new KRetrofit.Builder()//
				.client(client)//
				.baseUrl("http://xf.qfang.com/")//
				.addConverterFactory(KGsonConverterFactory.create(kOkhttpCache))//
				.addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build();

		final Api service = retrofit.create(Api.class);
//		service.getCity().
		service.login().enqueue(new Callback<String>() {

			@Override
			public void onResponse(Response<String> response) {
				// System.out.println("response" + response);
				service.getCity().enqueue(new Callback<JsonResult<List<AreaInfo>>>() {

					@Override
					public void onResponse(Response<JsonResult<List<AreaInfo>>> response) {
						System.out.println("onResponse=" + response.body().getData().size());
					}

					@Override
					public void onFailure(Throwable t) {

					}
				});

			}

			@Override
			public void onFailure(Throwable t) {
				System.out.println("response" + t);
				service.getCity().enqueue(new Callback<JsonResult<List<AreaInfo>>>() {

					@Override
					public void onResponse(Response<JsonResult<List<AreaInfo>>> response) {
						System.out.println("onResponse=" + response.body().getData().size());
					}

					@Override
					public void onFailure(Throwable t) {

					}
				});

			}
		});

		service.getCity2()//
		.subscribeOn(Schedulers.io())//
		.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<JsonResult<List<AreaInfo>>>() {

					@Override
					public void onCompleted() {
						System.out.println("onCompleted");
					}

					@Override
					public void onError(Throwable arg0) {
						System.out.println("onError");
					}

					@Override
					public void onNext(JsonResult<List<AreaInfo>> arg0) {
						System.out.println("onNext");
						text.setText(arg0.getDesc());
					}
				})

		// service.getCity().enqueue(new Callback<JsonResult<List<AreaInfo>>>() {
		//
		// @Override
		// public void onResponse(Response<JsonResult<List<AreaInfo>>> response) {
		// System.out.println("onResponse="+response.body().getData().size());
		// }
		//
		// @Override
		// public void onFailure(Throwable t) {
		//
		// }
		// });
		;

	}

}
