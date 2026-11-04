package com.camstream.mobile;
import android.net.Uri;import android.os.Bundle;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import java.io.File;
public class PlayerActivity extends AppCompatActivity {
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_player);VideoView v=findViewById(R.id.video);String p=getIntent().getStringExtra("path");if(p==null||!new File(p).exists()){Toast.makeText(this,"Video file is unavailable.",Toast.LENGTH_LONG).show();finish();return;}v.setVideoURI(Uri.fromFile(new File(p)));v.setMediaController(new MediaController(this));v.setOnErrorListener((mp,w,e)->{Toast.makeText(this,"This video format could not be played.",Toast.LENGTH_LONG).show();return true;});v.setOnPreparedListener(mp->v.start());findViewById(R.id.back).setOnClickListener(x->finish());}
}
