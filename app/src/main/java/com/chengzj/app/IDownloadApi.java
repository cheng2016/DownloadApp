package com.chengzj.app;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;

/**
 * Created by 0048104325 on 2017/7/18.
 */

public interface IDownloadApi {
    @GET("Wandoujia_141749_web_direct_binded.apk")
    Observable<ResponseBody> loadFile();
}
