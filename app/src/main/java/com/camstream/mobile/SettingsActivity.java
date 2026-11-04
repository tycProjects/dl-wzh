package com.camstream.mobile;
import android.os.Bundle;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;
public class SettingsActivity extends AppCompatActivity {
 String[] resolutions={"Auto","480p","720p","1080p","1440p"},fps={"30 FPS","60 FPS","90 FPS","120 FPS"},quality={"Low","Medium","High","Ultra"},count={"No Countdown","3 Seconds","5 Seconds","10 Seconds"},audio={"Audio Disabled","Microphone","Internal Audio (Android supported apps only)"},themes={"Gaming","Nature","Theme 3","Theme 4"},modes={"Dark Mode","Light Mode"},langs={"English","Arabic","French"};
 android.content.SharedPreferences p;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_settings);p=getSharedPreferences("cam",0);
  bind(R.id.resolution,resolutions,"resolution","Auto");bind(R.id.fps,fps,"fps","30 FPS");bind(R.id.quality,quality,"quality","High");bind(R.id.countdown,count,"countdown","No Countdown");bind(R.id.audio,audio,"audio","Audio Disabled");bind(R.id.theme,themes,"theme","Gaming");bind(R.id.mode,modes,"mode","Dark Mode");bind(R.id.language,langs,"language","English");
  findViewById(R.id.save).setOnClickListener(v->{save();Toast.makeText(this,"Settings saved on this device.",Toast.LENGTH_SHORT).show();finish();});
 }
 void bind(int id,String[] values,String key,String def){Spinner s=findViewById(id);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));String val=p.getString(key,def);for(int i=0;i<values.length;i++)if(values[i].equals(val)){s.setSelection(i);break;}}
 void save(){saveSpinner(R.id.resolution,"resolution");saveSpinner(R.id.fps,"fps");saveSpinner(R.id.quality,"quality");saveSpinner(R.id.countdown,"countdown");saveSpinner(R.id.audio,"audio");saveSpinner(R.id.theme,"theme");saveSpinner(R.id.mode,"mode");saveSpinner(R.id.language,"language");}
 void saveSpinner(int id,String key){p.edit().putString(key,((Spinner)findViewById(id)).getSelectedItem().toString()).apply();}
}
