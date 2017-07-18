package com.modules.service;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;

/**
 * Created by 0048104325 on 2017/7/18.
 */

public interface IDownloadApi {
    @GET("iop-zparking-0.0.1.20170718092339.apk")
    Observable<ResponseBody> loadFile();
}
