package com.jc.dynamicloadplugin;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.ryg.dynamicload.DLBasePluginService;

/**
 * Created by Administrator on 2016/8/25.
 */
public class PluginService extends DLBasePluginService{

    private class PluginBinder extends Binder implements IServiceAction{

        @Override
        public void result(String s) {
            Toast.makeText(that, s, Toast.LENGTH_SHORT).show();
        }
    }

    private PluginBinder mPluginBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mPluginBinder = new PluginBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mPluginBinder;
    }

}
