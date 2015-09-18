package com.example.retrefit2;

import java.util.List;

import retrofit.Call;
import retrofit.KOkHttpCall.CacheMode;
import retrofit.http.GET;
import retrofit.http.Headers;
import rx.Observable;

public interface Api {
	// http://192.168.0.241/xinfang-xpt/xpt/loginProcess?j_username=13530145721&j_password=1234567&appType=android

	// @Headers(CacheMode.LOAD_CACHE_ELSE_NETWORK)
	@GET("/xpt/loginProcess?j_username=15889797548&j_password=20090705&appType=Android")
	Call<String> login();

	// @Headers(CacheMode.LOAD_CACHE_ELSE_NETWORK)
	@GET("/xpt/area/city")
	Call<JsonResult<List<AreaInfo>>> getCity();

	// @Headers(CacheMode.LOAD_CACHE_ELSE_NETWORK)
	@GET("/xpt/area/city")
	Call<JsonResult<List<AreaInfo>>> getCity1();
	
	@Headers(CacheMode.LOAD_CACHE_ELSE_NETWORK)
	@GET("/xpt/area/city")
	Observable<JsonResult<List<AreaInfo>>> getCity2();
}
