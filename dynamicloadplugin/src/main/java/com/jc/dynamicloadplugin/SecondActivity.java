package com.jc.dynamicloadplugin;

import android.os.Bundle;

import com.ryg.dynamicload.DLBasePluginFragmentActivity;

/**
 * Created by Administrator on 2016/8/24.
 */
public class SecondActivity extends DLBasePluginFragmentActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }
}
