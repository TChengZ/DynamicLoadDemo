package com.jc.dynamicloaddemo;

import android.app.Application;

/**
 * Created by Administrator on 2016/8/24.
 */
public class BaseApplicaiton extends Application{

    private static BaseApplicaiton mInstance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static BaseApplicaiton getInstance() {
        return mInstance;
    }
}
