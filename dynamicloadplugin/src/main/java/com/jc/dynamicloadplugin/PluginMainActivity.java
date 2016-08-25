package com.jc.dynamicloadplugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import com.ryg.dynamicload.DLBasePluginActivity;
import com.ryg.dynamicload.internal.DLIntent;

public class PluginMainActivity extends DLBasePluginActivity implements View.OnClickListener{

    private Button mBtnSecondActivity = null;
    private Button mBtnBindService = null;
    private Button mBtnUnbindService = null;
    private ServiceConnection mConnection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);
        mBtnSecondActivity = (Button) findViewById(R.id.btn_second_activity);
        mBtnSecondActivity.setOnClickListener(this);
        mBtnBindService = (Button) findViewById(R.id.btn_bind_service);
        mBtnBindService.setOnClickListener(this);
        mBtnUnbindService = (Button) findViewById(R.id.btn_unbind_service);
        mBtnUnbindService.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        if(view == mBtnSecondActivity){
//            Intent intent = new Intent(PluginMainActivity.this, SecondActivity.class);
//            that.startActivity(intent);
            DLIntent intent = new DLIntent(getPackageName(), SecondActivity.class);
            startPluginActivity(intent);
        }
        else if(view == mBtnBindService){
            if(null == mConnection){
                mConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                        ((IServiceAction)iBinder).result("onServiceConnected");
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {

                    }
                };
            }
            DLIntent intent = new DLIntent(getPackageName(), PluginService.class);
            bindPluginService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        else if(view == mBtnUnbindService) {
            DLIntent intent = new DLIntent(getPackageName(), PluginService.class);
            unBindPluginService(intent, mConnection);
        }
    }
}
