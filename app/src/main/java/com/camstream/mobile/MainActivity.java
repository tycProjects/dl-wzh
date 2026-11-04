package com.camstream.mobile;

import android.app.*;
import android.content.*;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.Settings;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int CAPTURE_REQUEST=4101;
    private TextView status,timer;
    private boolean recording=false, paused=false;
    private long startedAt;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){ public void run(){
        if(recording){ long s=(System.currentTimeMillis()-startedAt)/1000;
            timer.setText(String.format(java.util.Locale.US,"%02d:%02d:%02d",s/3600,(s/60)%60,s%60));
            handler.postDelayed(this,500);
        }
    }};
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);
        status=findViewById(R.id.status);timer=findViewById(R.id.timer);
        findViewById(R.id.settings).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        findViewById(R.id.library).setOnClickListener(v->startActivity(new Intent(this,LibraryActivity.class)));
        findViewById(R.id.camera).setOnClickListener(v->startActivity(new Intent(this,CameraActivity.class)));
        findViewById(R.id.about).setOnClickListener(v->startActivity(new Intent(this,AboutActivity.class)));
        findViewById(R.id.floating).setOnClickListener(v->enableFloating());
        findViewById(R.id.record).setOnClickListener(v->{if(recording) stopRecording(); else requestCapture();});
        updateConfig();
    }
    private void updateConfig(){
        android.content.SharedPreferences p=getSharedPreferences("cam",0);
        String r=p.getString("resolution","Auto"), f=p.getString("fps","30 FPS"), q=p.getString("quality","High");
        ((TextView)findViewById(R.id.config)).setText(r+"  •  "+f+"  •  "+q+" quality");
    }
    private void requestCapture(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},77);
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(),CAPTURE_REQUEST);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);
        if(req==CAPTURE_REQUEST && res==RESULT_OK && data!=null){
            Intent i=new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_START);
            i.putExtra("resultCode",res); i.putExtra("data",data);
            if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
            recording=true;paused=false;startedAt=System.currentTimeMillis();
            status.setText("● RECORDING");handler.post(tick);
            Toast.makeText(this,"Screen capture service started",Toast.LENGTH_SHORT).show();
        } else if(req==CAPTURE_REQUEST) Toast.makeText(this,"Screen-capture permission was not granted.",Toast.LENGTH_LONG).show();
    }
    private void stopRecording(){
        startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_STOP));
        recording=false;handler.removeCallbacks(tick);status.setText("● READY TO CAPTURE");timer.setText("00:00:00");
    }
    private void enableFloating(){
        if(!Settings.canDrawOverlays(this)){
            new AlertDialog.Builder(this).setTitle("Floating controls permission")
                .setMessage("Allow display over other apps so recording controls can remain visible while you play.")
                .setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())))).show();
        } else { Intent i=new Intent(this,FloatingControlsService.class); if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i); }
    }
}
