# DownloadApp
下载更新app的一个示例，Retrofit2 + OkHttp + Rxjava2+ Notification + Service实现后台自动更新，喜欢请点个 star 哦！



### Init retrofit、okhttp、rxjava2 

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
                                        Logger.i(TAG + " %s"+" %s", "onProgress: " + "total ----> " + total
                                                + " done ---->" + progress,progress * 100 / total, Looper.myLooper());
                                        updateNotification(progress * 100 / total);
                                    }
                                }))
                                .build();
                    }
                })
                .build();
        return okHttpClient;
    }
    
    
### Notification view

        /**
         * 初始化Notification通知
         */
        public void initNotification() {
            builder = new NotificationCompat.Builder(mContext)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText("0%")
                    .setContentTitle("App更新")
                    .setProgress(100, 0, false);
            notificationManager = (NotificationManager) mContext
                    .getSystemService(Context.NOTIFICATION_SERVICE);
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
    
### Download app file and install

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


## Thanks 

[DownLoadManager](https://github.com/shanyao0/DownLoadManager)


## License

    This is free and unencumbered software released into the public domain.

    Anyone is free to copy, modify, publish, use, compile, sell, or
    distribute this software, either in source code form or as a compiled
    binary, for any purpose, commercial or non-commercial, and by any
    means.

    In jurisdictions that recognize copyright laws, the author or authors
    of this software dedicate any and all copyright interest in the
    software to the public domain. We make this dedication for the benefit
    of the public at large and to the detriment of our heirs and
    successors. We intend this dedication to be an overt act of
    relinquishment in perpetuity of all present and future rights to this
    software under copyright law.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
    IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
    OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
    ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
    OTHER DEALINGS IN THE SOFTWARE.

    For more information, please refer to <http://unlicense.org>
