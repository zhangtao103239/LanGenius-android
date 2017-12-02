package com.xchat.stevenzack.langenius;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import LanGenius.LanGenius;

public class DirChooserActivity extends AppCompatActivity {
    private ListView listView;
    private Button button;
    private String port="4444";
    private List<String> filenames=new ArrayList<>();
    private String orderStatus="name";
    private String currentPath;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1){
                case 0://reload listview   ( on currentPath changed )
                    String title_str=FileChooserMultiFiles.simplifyPath(DirChooserActivity.this,currentPath);
                    getSupportActionBar().setSubtitle(title_str);
//                    File[] listOfFiles=new File(currentPath).listFiles();
                    List<File> listOfFiles;
                    if (orderStatus.equals("length"))
                        listOfFiles=orderByLength(currentPath);
                    else if (orderStatus.equals("date"))
                        listOfFiles=orderByDate(currentPath);
                    else
                        listOfFiles=orderByName(currentPath);
                    filenames.clear();
                    if (listOfFiles!=null){
                        for (int i=0;i<listOfFiles.size();i++){
                            if (listOfFiles.get(i).getName().startsWith("."))
                                continue;
                            if (listOfFiles.get(i).isFile()){
                                filenames.add(listOfFiles.get(i).getName());
                            }else if (listOfFiles.get(i).isDirectory()){
                                filenames.add(listOfFiles.get(i).getName()+"/");
                            }
                        }
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(DirChooserActivity.this, android.R.layout.simple_list_item_1, filenames);
                    listView.setAdapter(arrayAdapter);
                    listView.setSelection((int)msg.obj);
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir_chooser);
        final Intent mi=getIntent();
        final String task=mi.getStringExtra("task");
        port=mi.getStringExtra("tmpPort");
        button=(Button)findViewById(R.id.dc_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (task.equals("staticSite")){
                    String port=mi.getStringExtra("tmpPort");
                    LanGenius.startStaticSite(":"+port,currentPath);
                    SharedPreferences sp_settings = getSharedPreferences(DirChooserActivity.this.getString(R.string.sp_settings), MODE_PRIVATE);
                    sp_settings.edit().putString("StaticSitePort", port).putString("staticSiteDir", currentPath).commit();
                }
                finish();
            }
        });
        listView=(ListView)findViewById(R.id.dc_listview);
        ((Button)findViewById(R.id.dc_button_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPath.length()>(Environment.getExternalStorageDirectory().toString()+"/").length()){
                    String[] strings=currentPath.split("/");
                    String newPath="/";
                    for (int i=1;i<strings.length-1;i++){
                        newPath=newPath+strings[i]+"/";
                    }
                    currentPath=newPath;
                    SharedPreferences sp=getSharedPreferences("cache",MODE_PRIVATE);
                    Message msg=new Message();msg.arg1=0;msg.obj=sp.getInt("dirlastVisit",0);handler.sendMessage(msg);//reload listview
                }
            }
        });
        SharedPreferences sp=getSharedPreferences(this.getString(R.string.sp_settings),MODE_PRIVATE);
        currentPath=sp.getString("dirChooserDir",Environment.getExternalStorageDirectory().toString()+"/");
        getSupportActionBar().setSubtitle(currentPath);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file=new File(currentPath+filenames.get(position));
                if (file.isDirectory()){
                    currentPath=currentPath+filenames.get(position);
                    SharedPreferences sp=getSharedPreferences("cache",MODE_PRIVATE);
                    sp.edit().putInt("dirlastVisit",position).commit();
                    Message msg=new Message();msg.arg1=0;msg.obj=0;handler.sendMessage(msg);//reload listview
                }
            }
        });
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filenames);
        listView.setAdapter(arrayAdapter);
        Message msg=new Message();msg.arg1=0;msg.obj=0;handler.sendMessage(msg);//reload listview
    }
    public void onBackPressed() {
        SharedPreferences sp_settings = getSharedPreferences(DirChooserActivity.this.getString(R.string.sp_settings), MODE_PRIVATE);
        sp_settings.edit().putString("StaticSitePort", port).putString("staticSiteDir", currentPath).commit();
        super.onBackPressed();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dirchooser_menu, menu);
        return true;
    }
    private void reloadListview(int i){
        Message msgr=new Message();msgr.arg1=0;msgr.obj=i;handler.sendMessage(msgr);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.dc_internal:
                currentPath=Environment.getExternalStorageDirectory().toString()+"/";
                reloadListview(0);
                return true;
            case R.id.dc_external:
//                String spath=System.getenv("SECONDARY_STORAGE");
                String spath=getStoragePath(DirChooserActivity.this,true);
                if (spath==null){
                    new AlertDialog.Builder(DirChooserActivity.this)
                            .setTitle(DirChooserActivity.this.getString(R.string.msg))
                            .setMessage(DirChooserActivity.this.getString(R.string.external_storage_not_found))
                            .setPositiveButton(DirChooserActivity.this.getString(R.string.str_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                    return true;
                }
                currentPath=spath+"/";
                reloadListview(0);
                return true;
            case R.id.dc_byname:
                orderStatus="name";
                reloadListview(0);
                return true;
            case R.id.dc_bydate:
                orderStatus="date";
                reloadListview(0);
                return true;
            case R.id.dc_bylength:
                orderStatus="length";
                reloadListview(0);
                return true;
            default:
                return true;
        }
    }
    private static String getStoragePath(Context mContext, boolean is_removale) {

        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static List<File> orderByName(String fliePath) {
        List<File> files = Arrays.asList(new File(fliePath).listFiles());
        Collections.sort(files, new Comparator< File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        return files;
    }
    public static List<File> orderByLength(String fliePath) {
        List<File> files = Arrays.asList(new File(fliePath).listFiles());
        Collections.sort(files, new Comparator< File>() {
            public int compare(File f1, File f2) {
                long diff = f1.length() - f2.length();
                if (diff > 0)
                    return 1;
                else if (diff == 0)
                    return 0;
                else
                    return -1;
            }
            public boolean equals(Object obj) {
                return true;
            }
        });
        return files;
    }
    public static List<File> orderByDate(String fliePath) {
        List<File> files = Arrays.asList(new File(fliePath).listFiles());
        Collections.sort(files, new Comparator< File>() {
            public int compare(File f1, File f2) {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0)
                    return 1;
                else if (diff == 0)
                    return 0;
                else
                    return -1;
            }
            public boolean equals(Object obj) {
                return true;
            }

        });
        return files;
    }
}
