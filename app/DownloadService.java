package com.zte.fn.iop.zparking.modules.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import com.orhanobut.logger.Logger;
import com.zte.fn.iop.zparking.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by 0048104325 on 2017/7/18.
 */

public class DownloadService extends Service{
    private final static String TAG = "DownloadService %s";

    //目标文件存储的文件夹路径
    private String  fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File
            .separator + "M_DEFAULT_DIR";
    //目标文件存储的文件名
    private String fileName = "iop-zparking-0.0.1.20170718092339.apk";

    private String url = "http://10.5.64.20:8080/view/%E7%89%A9%E8%81%94%E7%BD%91IOP%E7%9C%8B%E6%9D%BF/job/IOP_ZPARKING_BUILD/lastSuccessfulBuild/artifact/app/build/outputs/apk/";

    private Context mContext;

    private Retrofit retrofit;

    private CompositeDisposable mCompositeDisposable;

    private int preProgress = 0;
    private int NOTIFY_ID = 10000;

    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.i(TAG,"onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i(TAG,"onCreate");
        mContext = this;
        mCompositeDisposable = new CompositeDisposable();
    }


    File file = null;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i(TAG,"onStartCommand");
        initNotification();
        IDownloadApi iDownloadApi = getRetrofit(IDownloadApi.class);
        iDownloadApi.loadFile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        Logger.i(TAG,"onSubscribe");
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull ResponseBody responseBody) {
                        Logger.i(TAG,"onNext");
                        File file = null;
                        try {
                            file = DownloadUtils.getInstance(fileDir,fileName).saveFile(responseBody);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        installApk(file);
                        cancelNotification();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Logger.e(TAG +" %s","onError "+e.getMessage(),e);
                        cancelNotification();
                    }

                    @Override
                    public void onComplete() {
                        Logger.i(TAG,"onComplete");
                    }
                });
        return super.onStartCommand(intent, flags, startId);
    }

    private <T> T getRetrofit(Class<T> T) {
        retrofit = new Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        return retrofit.create(T);
    }

    private OkHttpClient getOkHttpClient(){
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20 * 1000, TimeUnit.MILLISECONDS)
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        okhttp3.Response orginalResponse = chain.proceed(chain.request());
                        return orginalResponse.newBuilder()
                                .body(new ProgressResponseBody(orginalResponse.body(), new ProgressListener() {
                                    @Override
                                    public void onProgress(long progress, long total, boolean done) {
                                        Logger.i(TAG + " %s"+" %s", "onProgress: " + "total ----> " + total + " done ---->" + progress,progress * 100 / total, Looper.myLooper());
                                        updateNotification(progress * 100 / total);
                                    }
                                }))
                                .build();
                    }
                })
                .build();
        return okHttpClient;
    }

    /**
     * 初始化Notification通知
     */
    public void initNotification() {
        builder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("0%")
                .setContentTitle("园区停车")
                .setProgress(100, 0, false);
        notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, builder.build());
    }

    /**
     * 更新通知
     */
    public void updateNotification(long progress) {
        int currProgress = (int) progress;
        if (preProgress < currProgress) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, currProgress, false);
            notificationManager.notify(NOTIFY_ID, builder.build());
        }
        preProgress =  currProgress;
    }

    /**
     * 取消通知
     */
    public void cancelNotification() {
        notificationManager.cancel(NOTIFY_ID);
    }

    /**
     * 安装软件
     *
     * @param file
     */
    private void installApk(File file) {
        Uri uri = Uri.fromFile(file);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        // 执行意图进行安装
        mContext.startActivity(install);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.i(TAG,"onDestroy");
        mCompositeDisposable.clear();
    }
}
