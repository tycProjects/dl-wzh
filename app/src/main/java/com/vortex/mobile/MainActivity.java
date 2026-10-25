package com.vortex.mobile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(32,48,32,32); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setBackgroundColor(Color.rgb(10,10,16));
    TextView title = new TextView(this); title.setText("VORTEX"); title.setTextSize(32); title.setTextColor(Color.rgb(190,120,255)); title.setGravity(Gravity.CENTER); root.addView(title);
    TextView sub = new TextView(this); sub.setText("Floating companion menu"); sub.setTextColor(Color.WHITE); sub.setTextSize(16); sub.setPadding(0,12,0,32); root.addView(sub);
    Button roblox = new Button(this); roblox.setText("Open Roblox"); roblox.setOnClickListener(v -> { Intent i=getPackageManager().getLaunchIntentForPackage("com.roblox.client"); if(i!=null) startActivity(i); else { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.roblox.client"))); } catch(Exception ignored){} } }); root.addView(roblox);
    Button overlay = new Button(this); overlay.setText("Enable floating V button"); overlay.setOnClickListener(v -> { if(!Settings.canDrawOverlays(this)) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); else startService(new Intent(this, OverlayService.class)); }); root.addView(overlay);
    TextView note = new TextView(this); note.setText("This companion provides a local menu and notes. It does not inject scripts into Roblox or bypass its protections."); note.setTextColor(Color.LTGRAY); note.setPadding(0,32,0,0); root.addView(note); setContentView(root);
  }
}
