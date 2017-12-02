package com.xchat.stevenzack.langenius;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class FileChooserMultiFiles extends AppCompatActivity {
    private Button button;
    private ListView listView;
    private List<String> filenames=new ArrayList<>();
    private String currentPath;
    private String orderStatus="name";
    private boolean selectAllStatus=false;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1){
                case 0://reload listview   ( on currentPath changed )
                    String title_str=simplifyPath(FileChooserMultiFiles.this,currentPath);
                    getSupportActionBar().setSubtitle(title_str);
                    selectAllStatus=false;
//                    File[] listOfFiles=new File(currentPath).listFiles();
                    List<File> listOfFiles;
                    if (orderStatus.equals("length"))
                        listOfFiles=DirChooserActivity.orderByLength(currentPath);
                    else if (orderStatus.equals("date"))
                        listOfFiles=DirChooserActivity.orderByDate(currentPath);
                    else
                        listOfFiles=DirChooserActivity.orderByName(currentPath);
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
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(FileChooserMultiFiles.this, android.R.layout.simple_list_item_multiple_choice, filenames);
                    listView.setAdapter(arrayAdapter);
                    listView.setSelection((int)msg.obj);
                    break;
                case 1://select all items
                    selectAllStatus=!selectAllStatus;
                    for (int i=0;i<listView.getAdapter().getCount();i++){
                        if (new File(currentPath+filenames.get(i)).isFile()){
                            listView.setItemChecked(i,selectAllStatus);
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fc_multifiles);
        Intent intent=getIntent();
        Bundle extras=intent.getExtras();
        String action=intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
//            Toast.makeText(FileChooserMultiFiles.this,.show();
            String path=getRealFilePath(this,uri);
            if (HandlerConverter.MainActivity_handler==null){
                Intent mi=new Intent(FileChooserMultiFiles.this,MainActivity.class);
                if (path==null||path.equals("")) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    finish();
                }
                mi.putExtra("file",path);
                startActivity(mi);
                finish();
            }else {
                Message msg=new Message();msg.arg1=2;msg.obj=path;
                HandlerConverter.MainActivity_handler.sendMessage(msg);
                Toast.makeText(this,FileChooserMultiFiles.this.getString(R.string.file_added),Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        getSupportActionBar().setTitle(FileChooserMultiFiles.this.getString(R.string.file_chooser));
        button=(Button)findViewById(R.id.filechooser_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray bs = listView.getCheckedItemPositions();
                for (int i=0;i<listView.getAdapter().getCount();i++){
                    if (bs.get(i)){
                        if (HandlerConverter.MainActivity_handler!=null){
                            Message msg=new Message();msg.arg1=2;msg.obj=currentPath+filenames.get(i);
                            HandlerConverter.MainActivity_handler.sendMessage(msg);
                        }
                    }
                }
                SharedPreferences sp=getSharedPreferences(FileChooserMultiFiles.this.getString(R.string.sp_settings),MODE_PRIVATE);
                sp.edit().putString("fileChooserDir",currentPath).commit();
                finish();
            }
        });
        ((Button)findViewById(R.id.filechooser_button_cancel)).setOnClickListener(new View.OnClickListener() {
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
                    Message msg=new Message();msg.arg1=0;msg.obj=sp.getInt("lastVisit",0);handler.sendMessage(msg);//reload listview
                }
            }
        });
        listView=(ListView)findViewById(R.id.filechooser_listview);
        SharedPreferences sp=getSharedPreferences(this.getString(R.string.sp_settings),MODE_PRIVATE);
        currentPath=sp.getString("fileChooserDir",Environment.getExternalStorageDirectory().toString()+"/");
        getSupportActionBar().setSubtitle(currentPath);

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file=new File(currentPath+filenames.get(position));
                if (file.isDirectory()){
                    currentPath=currentPath+filenames.get(position);
                    SharedPreferences sp=getSharedPreferences("cache",MODE_PRIVATE);
                    sp.edit().putInt("lastVisit",position).commit();
                    Message msg=new Message();msg.arg1=0;msg.obj=0;handler.sendMessage(msg);//reload listview
                }
            }
        });
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, filenames);
        listView.setAdapter(arrayAdapter);
        Message msg=new Message();msg.arg1=0;msg.obj=0;handler.sendMessage(msg);//reload listview
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filechooser_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        SharedPreferences sp=getSharedPreferences(FileChooserMultiFiles.this.getString(R.string.sp_settings),MODE_PRIVATE);
        sp.edit().putString("fileChooserDir",currentPath).commit();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.filechooser_menu_all:
                Message msg=new Message();msg.arg1=1;handler.sendMessage(msg);
                return true;
            case R.id.fc_internal:
                currentPath=Environment.getExternalStorageDirectory().toString()+"/";
                reloadListview(0);
                return true;
            case R.id.fc_external:
//                String spath=System.getenv("SECONDARY_STORAGE");
                String spath=getStoragePath(FileChooserMultiFiles.this,true);
                if (spath==null){
                    new AlertDialog.Builder(FileChooserMultiFiles.this)
                            .setTitle(FileChooserMultiFiles.this.getString(R.string.msg))
                            .setMessage(FileChooserMultiFiles.this.getString(R.string.external_storage_not_found))
                            .setPositiveButton(FileChooserMultiFiles.this.getString(R.string.str_ok), new DialogInterface.OnClickListener() {
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
            case R.id.fc_byname:
                orderStatus="name";
                reloadListview(0);
                return true;
            case R.id.fc_bydate:
                orderStatus="date";
                reloadListview(0);
                return true;
            case R.id.fc_bylength:
                orderStatus="length";
                reloadListview(0);
                return true;
            default:
                return true;
        }
    }
    private void reloadListview(int i){
        Message msgr=new Message();msgr.arg1=0;msgr.obj=i;handler.sendMessage(msgr);
    }
    private String getExternalPath(){
        File[] files=new File("/storage").listFiles();
        int j=0;
        if (files!=null){
            for (int i=0;i<files.length;i++){
                if (files[i].getName()!="emulated"){
                    if (j==1)
                        return files[i].getAbsolutePath();
                    j++;
                }
            }
        }
        return null;
    }
    private String getInternalPath(){
        File[] files=new File("/storage").listFiles();
        int j=0;
        if (files!=null){
            for (int i=0;i<files.length;i++){
                if (files[i].getName()!="emulated"){
                    if (j==0)
                        return files[i].getAbsolutePath();
                    j++;
                }
            }
        }
        return null;
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
    public static String getRealFilePath( final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }
    public static   String simplifyPath(Context context,String path){
        String title_str=path.replace(
                Environment.getExternalStorageDirectory().toString(),
                context.getString(R.string.internal_storage)
        );
        if (System.getenv("SECONDARY_STORAGE")!=null){
            title_str=title_str.replace(
                    System.getenv("SECONDARY_STORAGE"),
                    context.getString(R.string.external_storage)
            );
        }
        return title_str;
    }
}
