package com.ijustyce.weekly1601;

import com.facebook.stetho.Stetho;
import com.ijustyce.fastandroiddev3.IApplication;
import com.ijustyce.fastandroiddev3.http.HttpManager;

/**
 * Created by yangchun on 2016/11/10.
 */

public class AppApplication extends IApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        HttpManager.setBaseUrl("http://192.168.31.176:9090/gen/");
        HttpManager.trustAllSll();
        Stetho.initializeWithDefaults(this);
     //   HttpManager.setHttpsCer("ssl.crt");
    }
}
