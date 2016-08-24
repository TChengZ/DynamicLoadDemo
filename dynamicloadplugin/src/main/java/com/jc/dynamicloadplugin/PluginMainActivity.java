package com.jc.dynamicloadplugin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ryg.dynamicload.DLBasePluginActivity;
import com.ryg.dynamicload.internal.DLIntent;

public class PluginMainActivity extends DLBasePluginActivity implements View.OnClickListener{

    private Button mBtnSecondActivity = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);
        mBtnSecondActivity = (Button) findViewById(R.id.btn_second_activity);
        mBtnSecondActivity.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        if(view == mBtnSecondActivity){
//            Intent intent = new Intent(PluginMainActivity.this, SecondActivity.class);
//            that.startActivity(intent);
            DLIntent intent = new DLIntent(getPackageName(), SecondActivity.class);
            startPluginActivity(intent);
        }
    }
}
