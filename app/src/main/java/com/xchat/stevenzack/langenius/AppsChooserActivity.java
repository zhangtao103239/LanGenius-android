package com.xchat.stevenzack.langenius;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppsChooserActivity extends AppCompatActivity {
    private ListView listView;
    private SimpleAdapter simpleAdapter;
    private List<Map<String, Object>> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps_chooser);
        listView=(ListView)findViewById(R.id.apps_list);
        listView.setEmptyView(findViewById(R.id.apps_pro));
        new Thread(new Runnable() {
            @Override
            public void run() {
                data=getData();
                simpleAdapter=new SimpleAdapter(AppsChooserActivity.this,data,R.layout.applist_item,new String[]{"icon","title"},new int[]{R.id.applist_item_iv,R.id.applist_item_tv});
                simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                    @Override
                    public boolean setViewValue(View view, Object o, String s) {
                        if(view instanceof ImageView && o instanceof Drawable){
                            ImageView iv = (ImageView) view;
                            iv.setImageDrawable((Drawable)o);
                            return true;
                        }else
                            return false;
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(simpleAdapter);
                    }
                });
            }
        }).start();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int gi, long l) {
                if (HandlerConverter.MainActivity_handler!=null){
                    Message msg=new Message();msg.arg1=2;msg.obj=data.get(gi).get("path");
                    HandlerConverter.MainActivity_handler.sendMessage(msg);
                }
                finish();
            }
        });
    }
    private List<Map<String, Object>> getData() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        List<Map<String, Object>> maps=new ArrayList<Map<String, Object>>() ;
        for (int i=0;i<list.size();i++){
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("icon",pm.getApplicationIcon(list.get(i)).getCurrent());
            m.put("title",(String)pm.getApplicationLabel(list.get(i)));
            m.put("pkg",list.get(i).packageName);
            m.put("path",list.get(i).sourceDir);
            maps.add(m);
        }
        return maps;
    }
    public static String getApkPathByPackageName(String pkg){
        File file=new File("/data/app");
        if (file.isDirectory()){
            File[] l = file.listFiles();
            for (int i=0;i<l.length;i++){
                if (l[i].getName().contains(pkg)){
                    return "/data/app/"+l[i]+"/base.apk";
                }
            }
        }
        return null;
    }
}
