package com.jc.dynamicloaddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ryg.dynamicload.internal.DLIntent;
import com.ryg.dynamicload.internal.DLPluginManager;
import com.ryg.dynamicload.internal.DLPluginPackage;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener{

    private Button mBtnLaunchActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnLaunchActivity = (Button) findViewById(R.id.btn_launch_activity);
        mBtnLaunchActivity.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        if(view == mBtnLaunchActivity){
            String pluginApkPath = Environment.getExternalStorageDirectory() + File.separator + "plugin.apk";
            File file = new File(pluginApkPath);
            if(!file.exists()){
                Toast.makeText(MainActivity.this, "插件apk不存在，请在sd卡目录放plugin.apk作为插件", Toast.LENGTH_SHORT).show();
                return;
            }
            DLPluginManager pluginManager = DLPluginManager.getInstance(BaseApplicaiton.getInstance());
            DLPluginPackage dlPluginPackage = pluginManager.loadApk(pluginApkPath);
            pluginManager.startPluginActivity(this, new DLIntent(dlPluginPackage.packageName, dlPluginPackage.defaultActivity));
        }
    }
}
