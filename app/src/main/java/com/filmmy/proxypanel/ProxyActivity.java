package com.filmmy.proxypanel;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import it.beppi.tristatetogglebutton_library.*;
import it.beppi.tristatetogglebutton_library.TriStateToggleButton;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import rikka.shizuku.Shizuku;


public class ProxyActivity extends AppCompatActivity {
	
	private double S1 = 0;
	private double S2 = 0;
	private double S3 = 0;
	private double S4 = 0;
	
	private LinearLayout linear1;
	private LinearLayout linear2;
	private LinearLayout linear4;
	private ImageView imageview1;
	private TextView textview1;
	private TextView textview2;
	private TextView textview3;
	private ScrollView vscroll1;
	private LinearLayout linear5;
	private LinearLayout linear6;
	private LinearLayout linear19;
	private LinearLayout linear7;
	private LinearLayout linear8;
	private LinearLayout linear10;
	private LinearLayout linear11;
	private LinearLayout linear13;
	private LinearLayout linear14;
	private LinearLayout linear16;
	private LinearLayout linear17;
	private LinearLayout line1;
	private TextView textview4;
	private LinearLayout linear9;
	private TriStateToggleButton switch1;
	private TextView textview5;
	private TextView textview6;
	private LinearLayout linear12;
	private TriStateToggleButton switch2;
	private TextView textview7;
	private TextView textview8;
	private LinearLayout linear15;
	private TriStateToggleButton switch3;
	private TextView textview9;
	private TextView textview10;
	private LinearLayout linear18;
	private TriStateToggleButton switch4;
	private TextView textview11;
	private TextView textview12;
	private LinearLayout linear20;
	private LinearLayout linear29;
	private LinearLayout linear31;
	private LinearLayout line2;
	private TextView textview13;
	private TextView textview14;
	private LinearLayout linear30;
	private TextView textview15;
	private TextView textview16;
	private LinearLayout linear32;
	private TextView textview17;
	
	private ShizukuMaster SHIZUKU;
	private Intent i = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.proxy);
		initialize(_savedInstanceState);
		
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
		|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
		} else {
			initializeLogic();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		linear2 = findViewById(R.id.linear2);
		linear4 = findViewById(R.id.linear4);
		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		textview3 = findViewById(R.id.textview3);
		vscroll1 = findViewById(R.id.vscroll1);
		linear5 = findViewById(R.id.linear5);
		linear6 = findViewById(R.id.linear6);
		linear19 = findViewById(R.id.linear19);
		linear7 = findViewById(R.id.linear7);
		linear8 = findViewById(R.id.linear8);
		linear10 = findViewById(R.id.linear10);
		linear11 = findViewById(R.id.linear11);
		linear13 = findViewById(R.id.linear13);
		linear14 = findViewById(R.id.linear14);
		linear16 = findViewById(R.id.linear16);
		linear17 = findViewById(R.id.linear17);
		line1 = findViewById(R.id.line1);
		textview4 = findViewById(R.id.textview4);
		linear9 = findViewById(R.id.linear9);
		switch1 = findViewById(R.id.switch1);
		textview5 = findViewById(R.id.textview5);
		textview6 = findViewById(R.id.textview6);
		linear12 = findViewById(R.id.linear12);
		switch2 = findViewById(R.id.switch2);
		textview7 = findViewById(R.id.textview7);
		textview8 = findViewById(R.id.textview8);
		linear15 = findViewById(R.id.linear15);
		switch3 = findViewById(R.id.switch3);
		textview9 = findViewById(R.id.textview9);
		textview10 = findViewById(R.id.textview10);
		linear18 = findViewById(R.id.linear18);
		switch4 = findViewById(R.id.switch4);
		textview11 = findViewById(R.id.textview11);
		textview12 = findViewById(R.id.textview12);
		linear20 = findViewById(R.id.linear20);
		linear29 = findViewById(R.id.linear29);
		linear31 = findViewById(R.id.linear31);
		line2 = findViewById(R.id.line2);
		textview13 = findViewById(R.id.textview13);
		textview14 = findViewById(R.id.textview14);
		linear30 = findViewById(R.id.linear30);
		textview15 = findViewById(R.id.textview15);
		textview16 = findViewById(R.id.textview16);
		linear32 = findViewById(R.id.linear32);
		textview17 = findViewById(R.id.textview17);
		
		//OnTouch
		textview9.setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event){
				int ev = event.getAction();
				switch (ev) {
					case MotionEvent.ACTION_DOWN:
					
					
					
					break;
					case MotionEvent.ACTION_UP:
					
					
					
					break;
				} return true;
			}
		});
		
		textview9.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
			}
		});
		
		linear30.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				i.setAction(Intent.ACTION_VIEW);
				i.setData(Uri.parse("https://t.me/Filmmy_mod"));
				startActivity(i);
			}
		});
		
		textview15.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				
				return true;
			}
		});
		
		//OnTouch
		textview15.setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event){
				int ev = event.getAction();
				switch (ev) {
					case MotionEvent.ACTION_DOWN:
					
					
					
					break;
					case MotionEvent.ACTION_UP:
					
					
					
					break;
				} return true;
			}
		});
		
		textview15.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
			}
		});
		
		linear32.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				SketchwareUtil.showMessage(getApplicationContext(), "𝗠𝗔𝗜𝗡 𝗜𝗗 𝗦𝗔𝗙𝗘 𝗦𝗨𝗖𝗖𝗘𝗦𝗦𝗙𝗨𝗟𝗟𝗬");
			}
		});
	}
	
	private void initializeLogic() {
		linear6.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xC3121416));
		line1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFFFF0000));
		linear19.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xC3121416));
		line2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFFFF0000));
		linear30.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFFF0000));
		linear32.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFFF0000));
		_fixamods_proxy_panel("", "", "", "", "", "", "", "");
		FileUtil.writeFile("FIXAMODS", "JOIN");
	}
	
	public void _Coppy_Aseat(final String _AseatFile, final String _Path, final String _Realname) {
		ShizukuMaster SHIZUKU = new ShizukuMaster(ProxyActivity.this);
		CopyAssetShizuku(SHIZUKU,_AseatFile,_Path,_Realname);
	}
	
	
	public void _IJ() {
	}
	
	public void CopyAssetShizuku(ShizukuMaster shizuku,String _asset,String _path,String _file){
		Context context = this.getApplicationContext();
		shizuku = new ShizukuMaster(context);
		if (shizuku.ShizukuRunCheck()) {
			if (!_asset.equals("")) {
				if (!_file.equals("")) {
					try{
						//Context context = this.getApplicationContext();
						int count;
						java.io.InputStream input= context.getAssets().open(_asset);
						java.io.OutputStream output = new  java.io.FileOutputStream("/storage/emulated/0/"+_file);
						byte data[] = new byte[8192];
						while ((count = input.read(data))>0) {
							output.write(data, 0, count);
						}
						output.flush();
						output.close();
						input.close();
						if (_file.endsWith(".zip")) {
							
							
							if (shizuku != null) {
								shizuku.ShizukuShellCmd("unzip -o ".concat("/storage/emulated/0/".concat(_file.concat(" -d ".concat(_path)))));
								if (shizuku.isSucessByAndroidSketchwareMaster()) {
									SketchwareUtil.showMessage(getApplicationContext(), " SUCCESS ✔️");
									if (FileUtil.isExistFile("/storage/emulated/0/".concat(_file))) {
										FileUtil.deleteFile("/storage/emulated/0/".concat(_file));
									}
									else {
										
									}
									
								}
								else {
									SketchwareUtil.showMessage(getApplicationContext(), " FAILD ❌");
								}
							}
							else {
								
							}
						}
						else {
							
							
							if (shizuku != null) {
								shizuku.ShizukuShellCmd("mv ".concat("/storage/emulated/0/".concat(_file.concat(" ".concat(_path)))));
								if (shizuku.isSucessByAndroidSketchwareMaster()) {
									SketchwareUtil.showMessage(getApplicationContext(), " SUCCESS ✔️");
									
								}
								else {
									SketchwareUtil.showMessage(getApplicationContext(), " FAILD ❌");
									
								}
							}
							else {
								
							}
						}
					}catch(Exception e){
						
						SketchwareUtil.showMessage(getApplicationContext(), " FAILD ❌");
					}
				}
				else {
					SketchwareUtil.showMessage(getApplicationContext(), " FAILD ❌");
				}
			}
			else {
				SketchwareUtil.showMessage(getApplicationContext(), " FAILD ❌");
			}
		}
		else {
			SketchwareUtil.showMessage(getApplicationContext(), "shizuku not run !");
		}
	}
	/*
public boolean openPackage(String packageName) {
    try {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);  // অ্যাপটি ওপেন হবে
            return true;  // সফলভাবে অ্যাপটি ওপেন হয়েছে
        } else {
            return false;  // প্যাকেজটি ইনস্টল করা নেই
        }
    } catch (Exception e) {
        // যদি কোনো ত্রুটি ঘটে
        return false;  // প্যাকেজ ওপেন করা সম্ভব হয়নি
    }
}
public boolean openURL(String url) {
    try {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);  // ব্রাউজারে URL ওপেন হবে
        return true; // সফলভাবে URL ওপেন হয়েছে
    } catch (Exception e) {
        // যদি কোনো ত্রুটি ঘটে
        return false; // URL ওপেন করা সম্ভব হয়নি
    }
}

public boolean isPackageInstalled(String packageName) {
    PackageManager pm = getPackageManager();
    try {
        pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        return true;  // প্যাকেজটি ইনস্টল করা আছে
    } catch (PackageManager.NameNotFoundException e) {
        return false; // প্যাকেজটি ইনস্টল করা নেই
    }
}
*/
	
	{
	}
	
	
	public void _fixamods_proxy_panel(final String _linear8, final String _linear11, final String _linear14, final String _linear17, final String _S1, final String _S2, final String _S3, final String _S4) {
		linear8.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View _view){
				if (S1 == 0) {
					S1++;
					switch1.setToggleStatus(TriStateToggleButton.ToggleStatus.on);
					_Coppy_Aseat("l", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				} else {
					S1--;
					switch1.setToggleStatus(TriStateToggleButton.ToggleStatus.off);
					_Coppy_Aseat("l", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				}
			}
		});
		linear11.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View _view){
				if (S2 == 0) {
					S2++;
					switch2.setToggleStatus(TriStateToggleButton.ToggleStatus.on);
					_Coppy_Aseat("l", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				} else {
					S2--;
					switch2.setToggleStatus(TriStateToggleButton.ToggleStatus.off);
					_Coppy_Aseat("l", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				}
			}
		});
		linear14.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View _view){
				if (S3 == 0) {
					S3++;
					switch3.setToggleStatus(TriStateToggleButton.ToggleStatus.on);
					_Coppy_Aseat("l", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				} else {
					S3--;
					switch3.setToggleStatus(TriStateToggleButton.ToggleStatus.off);
					_Coppy_Aseat("l", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				}
			}
		});
		linear17.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View _view){
				if (S4 == 0) {
					S4++;
					switch4.setToggleStatus(TriStateToggleButton.ToggleStatus.on);
					_Coppy_Aseat("b", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				} else {
					S4--;
					switch4.setToggleStatus(TriStateToggleButton.ToggleStatus.off);
					_Coppy_Aseat("b", "storage/emulated/0/Android/data/com.dts.freefiremax/files/contentcache/Optional/android/gameassetbundles/", "shaders.~2Fff~2B4kWOY1cUAwrleOq4IaYUrRE~3D");
				}
			}
		});
	}
	
}