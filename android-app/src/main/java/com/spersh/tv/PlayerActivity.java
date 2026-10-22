package com.spersh.tv;
import android.os.Bundle;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import okhttp3.OkHttpClient;
import org.json.*;
import java.util.*;

@OptIn(markerClass=UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {
  ExoPlayer exo;
  protected void onCreate(Bundle b){
    super.onCreate(b); setContentView(R.layout.activity_player);
    PlayerView view=findViewById(R.id.player);
    try{
      JSONObject o=new JSONObject(getIntent().getStringExtra("payload"));
      String url=o.getString("url");
      Map<String,String> headers=new LinkedHashMap<>();
      JSONObject hs=o.optJSONObject("headers");
      if(hs!=null){Iterator<String> it=hs.keys();while(it.hasNext()){String k=it.next();headers.put(k,hs.optString(k));}}
      OkHttpDataSource.Factory http=new OkHttpDataSource.Factory(new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).retryOnConnectionFailure(true).build());
      if(!headers.isEmpty())http.setDefaultRequestProperties(headers);
      DefaultDataSource.Factory data=new DefaultDataSource.Factory(this,http);
      exo=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(data)).build();
      view.setPlayer(exo);

      MediaItem.Builder mb=new MediaItem.Builder().setUri(url);
      JSONArray subs=o.optJSONArray("subtitles");
      if(subs!=null){
        List<MediaItem.SubtitleConfiguration> list=new ArrayList<>();
        for(int n=0;n<subs.length();n++){
          JSONObject s=subs.getJSONObject(n);
          String u=s.optString("src"),lang=s.optString("lang","ar"),label=s.optString("label",lang);
          if(!u.isEmpty()) list.add(new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(u))
            .setMimeType(MimeTypes.TEXT_VTT).setLanguage(lang).setLabel(label).build());
        }
        mb.setSubtitleConfigurations(list);
      }
      exo.setMediaItem(mb.build()); exo.prepare(); exo.play();
    }catch(Exception e){ finish(); }
  }
  protected void onPause(){super.onPause();if(exo!=null)exo.pause();}
  protected void onDestroy(){if(exo!=null)exo.release();super.onDestroy();}
}
