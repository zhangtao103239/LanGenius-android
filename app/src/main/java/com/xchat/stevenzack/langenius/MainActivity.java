package com.xchat.stevenzack.langenius;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.zxing.WriterException;
import com.google.zxing.activity.CaptureActivity;
import com.google.zxing.common.BitmapUtils;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;
import com.yanzhenjie.andserver.website.AssetsWebsite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import LanGenius.JavaHandler;
import LanGenius.LanGenius;


public class MainActivity extends AppCompatActivity {
    private ClipboardManager clipboardManager;
    private String TAG="Main";
    public  Handler handler=new Handler(){
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.arg1){
                case 0://onClipboard received
                    final String txt=msg.obj.toString();
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied text",txt));
                    if (txt.startsWith("http://")||txt.startsWith("https://"))
                        Snackbar.make((CoordinatorLayout)findViewById(R.id.main_container),R.string.text_received_is_link,Snackbar.LENGTH_LONG)
                                .setAction(MainActivity.this.getString(R.string.open), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(txt));
                                        startActivity(browserIntent);
                                    }
                                })
                                .show();
                    else
                        Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.newClipboard),Toast.LENGTH_SHORT).show();
                    break;
                case 1://on File received
                    Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.newFile)+msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    Message newmsg=new Message();
                    newmsg.arg1=2;
                    String str=sp_settings.getString(MainActivity.this.getString(R.string.sp_sub_frcv_path),Environment.getExternalStorageDirectory().toString()+"/");
                    newmsg.obj=str+msg.obj.toString();
                    Log.d(TAG, "handleMessage: newmsg.obj="+newmsg.obj.toString());
                    handler.sendMessage(newmsg);
                    break;
                case 2://add file
                    if (msg.obj!=null) {
                        String path = msg.obj.toString();
                        try {
                            Log.d(TAG, "onActivityResult: PATH ==" + path);
                            String[] strs = path.split("/");
                            HashMap<String,String> hashMap=new HashMap<>();
                            hashMap.put("FileName",strs[strs.length-1]);
                            hashMap.put("Path",path);
                            strings.add(hashMap);
                            simpleAdapter.notifyDataSetChanged();
                            LanGenius.addFile(path);
                        } catch (Exception e) {
                            Log.d(TAG, "onActivityResult: Exception  == " + e.toString());
                            Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.addFileFailed), Toast.LENGTH_SHORT).show();
                        }
                        if (findViewById(R.id.main_introFrame).getVisibility()==View.VISIBLE){
//                            fab.setVisibility(View.GONE);
                            TextView tv=(TextView)findViewById(R.id.main_introInfo);
                            tv.setText(MainActivity.this.getString(R.string.now_browse_link)+"\n\n"+txt_ip.getText().toString());
                        }
                    }else {
                        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.addFileFailed), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 3://open uri in browser
                    final String url=msg.obj.toString();
                    TextView textView=new TextView(MainActivity.this);
                    textView.setText(MainActivity.this.getString(R.string.click_to_copy)+":\n"+url);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied text",url));
                            Toast.makeText(MainActivity.this,url,Toast.LENGTH_SHORT).show();
                        }
                    });
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(MainActivity.this.getString(R.string.open)+"?")
                            .setView(textView)
                            .setPositiveButton(MainActivity.this.getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(browserIntent);
                                }
                            })
                            .setNegativeButton(MainActivity.this.getString(R.string.str_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                    break;
            }
        }
    };
    private TextView txt_ip;
    private ListView listView;
    private List<HashMap<String,String>> strings=new ArrayList<>();
    private SimpleAdapter simpleAdapter;
    private com.getbase.floatingactionbutton.FloatingActionButton fab_files,fab_apps;
    private FloatingActionsMenu fab;
    private SharedPreferences sp_settings;
    private Server uploadServer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clipboardManager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                LanGenius.setClipboard(clipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
            }
        });
        HandlerConverter.MainActivity_handler=handler;
        if (clipboardManager.getPrimaryClip()!=null&&clipboardManager.getPrimaryClip().getItemAt(0)!=null&&
                clipboardManager.getPrimaryClip().getItemAt(0).getText()!=null) {
            LanGenius.setClipboard(clipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
        }
        txt_ip=(TextView)findViewById(R.id.txt_hostname);
        String str_IP=LanGenius.getIP();
        sp_settings=getSharedPreferences(MainActivity.this.getString(R.string.sp_settings),MODE_PRIVATE);
        final String default_port=sp_settings.getString(MainActivity.this.getString(R.string.sp_sub_port),MainActivity.this.getString(R.string.default_port));
        txt_ip.setText("http://"+(str_IP=="127.0.0.1"?"localhost":str_IP)+default_port);
        txt_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied text",txt_ip.getText().toString()));
                Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.address_copied),Toast.LENGTH_SHORT).show();
            }
        });
        ((ImageButton)findViewById(R.id.main_optionMenu)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu=new PopupMenu(MainActivity.this,v);
                popupMenu.getMenuInflater().inflate(R.menu.main_menu,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.main_menu_kc:
                                Intent intent=new Intent(MainActivity.this,KCActivity.class);
                                startActivity(intent);
                                break;
                            case R.id.main_menu_settings:
                                Intent intent1=new Intent(MainActivity.this,SettingsActivity.class);
                                startActivity(intent1);
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
        String lang=MainActivity.this.getString(R.string.language);
        File file=new File("/data/data/"+getPackageName()+"/showtime/");
        if (!file.exists()){
            file.mkdirs();
            copyFileOrDir("showtime");
        }
        LanGenius.start(lang,new MyJavaHandler(),default_port,file.getAbsolutePath());
        AndServer andServer=new AndServer.Build()
                .port(Integer.parseInt(default_port.substring(1))+1)
                .registerHandler("upload",new RequestUploadHandler())
                .website(new AssetsWebsite(getAssets(),""))
                .build();
        uploadServer=andServer.createServer();
        uploadServer.start();
        ((ImageButton)findViewById(R.id.bt_openbrowser)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tempstr=LanGenius.getIP();
                if (tempstr=="127.0.0.1")
                    tempstr="localhost";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+tempstr+default_port));
                startActivity(browserIntent);
            }
        });
        listView=(ListView)findViewById(R.id.main_listview);
        simpleAdapter=new SimpleAdapter(this,strings,R.layout.listview_item,new String[]{"FileName"},new int[]{R.id.listview_txt_filename});
        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i=new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getApplicationContext().getPackageName() + ".provider", new File(strings.get(position).get("Path"))));
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(i);
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.openFileFailed), Toast.LENGTH_SHORT).show();
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int lsIndex=i;
                TextView textView=new TextView(MainActivity.this);
                textView.setText(FileChooserMultiFiles.simplifyPath(MainActivity.this,strings.get(lsIndex).get("Path")));
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(MainActivity.this.getString(R.string.cancel_sharing)+"?")
                        .setPositiveButton(MainActivity.this.getString(R.string.str_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                LanGenius.deleteFile(lsIndex);
                                strings.remove(lsIndex);
                                simpleAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(MainActivity.this.getString(R.string.str_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setView(textView)
                        .show();
                return true;
            }
        });
        fab=(FloatingActionsMenu)findViewById(R.id.main_fab);
        fab_files=(com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.main_fab_file);
        fab_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,FileChooserMultiFiles.class);
                startActivity(intent);
                fab.collapse();
            }
        });
        fab_apps=(com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.main_fab_apps);
        fab_apps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,AppsChooserActivity.class);
                startActivity(intent);
                fab.collapse();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this,R.color.colorPrimaryDark));
        }
        String str=sp_settings.getString(this.getString(R.string.sp_sub_frcv_path), Environment.getExternalStorageDirectory().toString()+"/");
        RequestUploadHandler.saveDirectory=new File(str);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is=null;
                OutputStream os=null;
                try {
                    File file=new File("/data/data/com.xchat.stevenzack.langenius/kc_linux_x64");
                    is=getResources().getAssets().open("kc_linux_x64");

                    if (!file.exists()) {
                        file.createNewFile();
                        os = new FileOutputStream(file);
                        byte[] buf = new byte[10240];
                        int l;
                        while ((l = is.read(buf)) > 0) {
                            os.write(buf, 0, l);
                        }
                        is.close();
                        os.close();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "run: " + e.toString());
                }
                try {
                    File file=new File("/data/data/com.xchat.stevenzack.langenius/kc_windows_x64.exe");
                    is=getResources().getAssets().open("kc_windows_x64.exe");
                    if (!file.exists()) {
                        file.createNewFile();
                        os = new FileOutputStream(file);
                        byte[] buf = new byte[10240];
                        int l;
                        while ((l = is.read(buf)) > 0) {
                            os.write(buf, 0, l);
                        }
                        is.close();
                        os.close();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "run: " + e.toString());
                }
            }
        }).start();
        SharedPreferences sp_intro=getSharedPreferences("intro",MODE_PRIVATE);
        if (!sp_intro.getBoolean("passed",false)){
            showIntro();
        }
//        handleSendAction(getIntent().getExtras(),getIntent().getAction());
        Intent intent=getIntent();
        String addFilePath=intent.getStringExtra("file");
        if (addFilePath!=null) {
            Message message = new Message();
            message.arg1 = 2;
            message.obj = addFilePath;
            handler.sendMessage(message);
        }
        ((ImageView)findViewById(R.id.main_qr_code)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Bitmap bitmap = BitmapUtils.create2DCode(txt_ip.getText().toString());
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    ImageView imageView=new ImageView(MainActivity.this);
                    imageView.setImageBitmap(bitmap);
                    builder.setView(imageView);
                    builder.setTitle(MainActivity.this.getString(R.string.scan_qr_code));
                    builder.setNegativeButton(MainActivity.this.getString(R.string.str_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    builder.show();
                } catch (WriterException e) {
                    Toast.makeText(MainActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        ((ImageView)findViewById(R.id.main_scan)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED) {
                        startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class),1001);
                    } else {
                        Log.v(TAG,"Permission is revoked");
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 38);
                    }
                }else {
                    startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class),1001);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1001&&data!=null){
            final String result = data.getStringExtra(CaptureActivity.SCAN_QRCODE_RESULT);
            Log.d(TAG, "onActivityResult: QR scan result = "+result);
//            Toast.makeText(MainActivity.this,result,Toast.LENGTH_SHORT).show();
            Message msg=new Message();msg.arg1=3;msg.obj=result;handler.sendMessage(msg);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==38){
            startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class),1001);
        }else if (requestCode == 2038) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // continue here - permission was granted
                    createFloatView("OK");
                }
            }
        }
    }

    private void showIntro() {
        findViewById(R.id.main_quitGuide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean b=((CheckBox)findViewById(R.id.main_neverShowUpAgain)).isChecked();
                findViewById(R.id.main_headFrame).setVisibility(View.VISIBLE);
                findViewById(R.id.main_secondText).setVisibility(View.VISIBLE);
                findViewById(R.id.main_listview).setVisibility(View.VISIBLE);
                fab.setVisibility(View.VISIBLE);
                findViewById(R.id.main_introFrame).setVisibility(View.GONE);
                if (b){
                    getSharedPreferences("intro",MODE_PRIVATE).edit().putBoolean("passed",b).commit();
                }
            }
        });
        findViewById(R.id.main_headFrame).setVisibility(View.GONE);
        findViewById(R.id.main_secondText).setVisibility(View.GONE);
        findViewById(R.id.main_listview).setVisibility(View.GONE);
        findViewById(R.id.main_introFrame).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        LanGenius.stop();
        uploadServer.stop();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStoragePermissionGranted();
        final SharedPreferences sp_settings=getSharedPreferences(MainActivity.this.getString(R.string.sp_settings),MODE_PRIVATE);
        String str=sp_settings.getString(this.getString(R.string.sp_sub_frcv_path),Environment.getExternalStorageDirectory().toString()+"/");
        Switch switchCompat=(Switch)findViewById(R.id.main_switch1);
        boolean bo=sp_settings.getBoolean(MainActivity.this.getString(R.string.sp_sub_clipshare),false);
        switchCompat.setChecked(bo);
        if (bo){
            ((TextView)findViewById(R.id.main_txt_clipshare)).setText(MainActivity.this.getString(R.string.cbhasbeenshared));
        }else{
            ((TextView)findViewById(R.id.main_txt_clipshare)).setText(MainActivity.this.getString(R.string.cbsharedisabled));
        }
        LanGenius.setCBEnabled(bo);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    ((TextView)findViewById(R.id.main_txt_clipshare)).setText(MainActivity.this.getString(R.string.cbhasbeenshared));
//                    checkDrawOverlayPermission();
                }else{
                    ((TextView)findViewById(R.id.main_txt_clipshare)).setText(MainActivity.this.getString(R.string.cbsharedisabled));
                }
                LanGenius.setCBEnabled(isChecked);
                sp_settings.edit().putBoolean(MainActivity.this.getString(R.string.sp_sub_clipshare),isChecked).commit();
            }
        });

    }
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED&&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }
    class MyJavaHandler implements JavaHandler{
        @Override
        public void onClipboardReceived(String s) {
            Message msg=new Message();
            msg.arg1=0;
            msg.obj=s;
            handler.sendMessage(msg);
        }

        @Override
        public void onFileReceived(String s) {
            if (isStoragePermissionGranted()){
            }
        }
    }
    private void copyFileOrDir(String path) {
        AssetManager assetManager = getResources().getAssets();
        String assets[] = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath = "/data/data/" + this.getPackageName() + "/" + path;
                File dir = new File(fullPath);
                if (!dir.exists())
                    dir.mkdir();
                for (int i = 0; i < assets.length; ++i) {
                    copyFileOrDir(path + "/" + assets[i]);
                }
            }
        } catch (IOException ex) {
            Log.e("tag2", "I/O Exception", ex);
        }
    }
    private void copyFile(String filename) {
        AssetManager assetManager = getResources().getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            String newFileName = "/data/data/" + this.getPackageName() + "/" + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("tag1", e.getMessage());
        }

    }
    public void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                /** request permission via start activity for result */
                startActivityForResult(intent, 2038);
            }else {
                createFloatView("OK");
            }
        }
    }
    private void createFloatView(String txt){
        final TextView button=new TextView(getApplicationContext());
        button.setText(txt);
        final WindowManager windowManager=(WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type=WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format= PixelFormat.RGBA_8888;
        params.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.width=100;
        params.height=100;
        windowManager.addView(button,params);
    }

}
