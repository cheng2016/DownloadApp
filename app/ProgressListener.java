package com.modules.service;

/**
 * Created by 0048104325 on 2017/7/18.
 */

public interface ProgressListener {
    /**
     * @param progress     已经下载或上传字节数
     * @param total        总字节数
     * @param done         是否完成
     */
    void onProgress(long progress, long total, boolean done);
}
