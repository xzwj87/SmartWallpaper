package com.samsung.app.smartwallpaper;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by samsung on 2018/3/21.
 * Author: my2013.wang@samsung.com
 */

public class AppInfoActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String SETTINGS_ACTION ="android.settings.APPLICATION_DETAILS_SETTINGS";
    private Button btn_appinfo;
    private Button btn_checkupdate;
    private TextView tv_version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = this.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.app_info);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.about_smartwallpaper);
        // 去掉logo图标
        getSupportActionBar().setDisplayShowHomeEnabled(false);
//        getSupportActionBar().setTitle("返回");
        if(Build.VERSION.SDK_INT >= 21){
            getSupportActionBar().setElevation(3);
        }
//        Drawable upArrow = getResources().getDrawable(R.drawable.go_back);
//        upArrow.setColorFilter(getResources().getColor(R.color.colorRed), PorterDuff.Mode.SRC_ATOP);
//        getSupportActionBar().setHomeAsUpIndicator(upArrow);
//        getSupportActionBar().setHomeAsUpIndicator(R.drawable.bt_shape);

        tv_version = (TextView) findViewById(R.id.tv_version);
        btn_appinfo = (Button) findViewById(R.id.btn_appinfo);
        btn_checkupdate = (Button) findViewById(R.id.btn_checkupdate);
        btn_appinfo.setOnClickListener(this);
        btn_checkupdate.setOnClickListener(this);
        String current_version = this.getResources().getString(R.string.current_version) + BuildConfig.VERSION_NAME;
        tv_version.setText(current_version);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()){
            case R.id.btn_appinfo:
                intent = new Intent(SETTINGS_ACTION);
                intent.setData(Uri.fromParts("package", this.getPackageName(), null));
                startActivity(intent);
                break;
            case R.id.btn_checkupdate:
                try {
                    Uri uri = Uri.parse("market://details?id=" + getPackageName());
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }catch (Exception e){

                }
                Toast.makeText(AppInfoActivity.this.getApplicationContext(), "已经是最新版本", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
