package com.filmmy.proxypanel;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.*;
import com.AndroidSketchwareMaster.*;
import com.AndroidSketchwareMaster.CopyAssetTask;
import com.AndroidSketchwareMaster.ShizukuMaster;
import com.AndroidSketchwareMaster.ShizukuShell;
import com.AndroidSketchwareMaster.ShizukuShell.OnProcessCompleteListener;
import com.shizuku.permission15.*;
import de.hdodenhof.circleimageview.*;
import it.beppi.tristatetogglebutton_library.*;
import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import rikka.shizuku.Shizuku;
import androidX.Master12.AnimatedParticleView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.os.AsyncTask;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import android.os.Handler;
import android.os.Looper;

import rikka.shizuku.Shizuku;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent; 
import android.content.pm.PackageManager; import android.net.Uri; 
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
	
	private boolean estaPareado = false;
	
	private RelativeLayout linear1;
	private LinearLayout emoji;
	private LinearLayout linear100;
	private LinearLayout linear2;
	private LinearLayout linear3;
	private TextView textview2;
	private EditText edittext1;
	private TextView textview3;
	private EditText edittext2;
	private LinearLayout linear101;
	private Button button2;
	private Button button1;
	private TextView terminal;
	private CircleImageView circleimageview1;
	private TextView textview1;
	private Switch switch2;
	private LinearLayout linear103;
	private Switch switch3;
	
	private Intent i = new Intent();
	private ShizukuMaster SHIZUKU;
	private SharedPreferences data;
	private java.io.File ss;
	private java.io.File nikhil;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		emoji = findViewById(R.id.emoji);
		linear100 = findViewById(R.id.linear100);
		linear2 = findViewById(R.id.linear2);
		linear3 = findViewById(R.id.linear3);
		textview2 = findViewById(R.id.textview2);
		edittext1 = findViewById(R.id.edittext1);
		textview3 = findViewById(R.id.textview3);
		edittext2 = findViewById(R.id.edittext2);
		linear101 = findViewById(R.id.linear101);
		button2 = findViewById(R.id.button2);
		button1 = findViewById(R.id.button1);
		terminal = findViewById(R.id.terminal);
		circleimageview1 = findViewById(R.id.circleimageview1);
		textview1 = findViewById(R.id.textview1);
		switch2 = findViewById(R.id.switch2);
		linear103 = findViewById(R.id.linear103);
		switch3 = findViewById(R.id.switch3);
		data = getSharedPreferences("data", Activity.MODE_PRIVATE);
		
		button2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (edittext1.getText().toString().equals("FILMMY ") && edittext2.getText().toString().equals("MODS")) {
					i.setAction(Intent.ACTION_VIEW);
					i.setClass(getApplicationContext(), ProxyActivity.class);
					startActivity(i);
				} else {
					terminal.setText("Error Username ➪".concat(edittext1.getText().toString().concat("\nError Passsword ➪".concat(edittext2.getText().toString()))));
				}
			}
		});
		
		button1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				i.setAction(Intent.ACTION_VIEW);
				i.setData(Uri.parse("https://www.youtube.com/@filmmymods"));
				startActivity(i);
			}
		});
		
		switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton _param1, boolean _param2) {
				final boolean _isChecked = _param2;
				if (_isChecked) {
					data.edit().putString("edittext1", edittext1.getText().toString()).commit();
					data.edit().putString("edittext2", edittext2.getText().toString()).commit();
				} else {
					data.edit().remove("edittext1").commit();
					data.edit().remove("edittext2").commit();
				}
			}
		});
	}
	
	private void initializeLogic() {
		linear2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)50, (int)2, 0xFF313131, 0xFF1A1A1A));
		edittext1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)50, (int)2, 0xFF313131, 0xFF0F0F0F));
		edittext2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)50, (int)2, 0xFF313131, 0xFF0F0F0F));
		button1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)50, (int)2, 0xFF313131, 0xFFFF0000));
		button2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)50, (int)2, 0xFF313131, 0xFFFF0000));
		if (android.os.Build.VERSION.SDK_INT >= 30) {
			if (!android.os.Environment.isExternalStorageManager()) {
				try {
					Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					intent.setData(Uri.parse("package:" + getPackageName()));
					startActivity(intent);
				} catch (Exception e) {
					Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
					startActivity(intent);
				}
			}
		}
		ShizukuMaster SHIZUKU = new ShizukuMaster(MainActivity.this);
		
		
		if (SHIZUKU.ShizukuRunCheck()) {
			//AndroidSketchwareamaster
			if (Build.VERSION.SDK_INT >= 21) {
				if (checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
				|| checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
				|| checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
					
					requestPermissions(new String[]{
						Manifest.permission.MANAGE_EXTERNAL_STORAGE,
						Manifest.permission.READ_EXTERNAL_STORAGE,
						Manifest.permission.WRITE_EXTERNAL_STORAGE
					}, 5000);
				} else {
					// Permissions are already granted, proceed with your task
				}
			} else {
				// Handle cases for devices with SDK < 21
			}
			
			
			
			
			
			
			
			
			
			SHIZUKU.oncreatShizukuByAndroidSketchwareMaster();
			SketchwareUtil.showMessage(getApplicationContext(), "✔ 𝐒𝐇𝐈𝐙𝐔𝐊𝐔 𝐈𝐒 𝐑𝐔𝐍𝐍𝐈𝐍𝐆… 🔥");
		}
		else {
			SketchwareUtil.showMessage(getApplicationContext(), "✖ 𝐒𝐇𝐈𝐙𝐔𝐊𝐔 𝐈𝐒 𝐍𝐎𝐓 𝐑𝐔𝐍𝐍𝐈𝐍𝐆…");
		}
		AnimatedParticleView particleView = new AnimatedParticleView(MainActivity.this);
		particleView.setLineColor(0xFFF44336);
		particleView.setparticleCount(100);
		particleView.setParticleColor(0xFFF44336);
		particleView.setBackgroundColor(0xFF000000);
		particleView.setAnimationMode(AnimatedParticleView.MODE_BOUNCE); // বা MODE_BOUNCE, MODE_EXPLODE, MODE_WAVE,MODE_SWIRL
		particleView.setParticleRadiusRange(5.0f, 10.0f);
		
		// layout এর মধ্যে যোগ করুন
		//import androidX.Master12.AnimatedParticleView;
		emoji.addView(particleView);
		edittext1.setText(data.getString("edittext1", ""));
		edittext2.setText(data.getString("edittext2", ""));
	}
	
}