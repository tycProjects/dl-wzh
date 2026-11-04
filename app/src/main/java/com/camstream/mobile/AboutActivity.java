package com.camstream.mobile;
import android.os.Bundle;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;
public class AboutActivity extends AppCompatActivity {
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);l.setPadding(28,40,28,20);l.setOrientation(1);l.setBackgroundColor(0xFF090D18);TextView t=new TextView(this);t.setText("CAM STREAM MOBILE\n\nULTIMATE PROFESSIONAL GAMING SCREEN RECORDER\n\nVersion 1.0.0\n\nPrivacy: recordings are saved locally in this app's private Movies folder. Screen capture starts only after the Android system consent screen.\n\nCamera and microphone permissions are requested only when their features are used.\n\nThis is an independent project and is not affiliated with any game publisher.");t.setTextColor(-1);t.setTextSize(16);l.addView(t);setContentView(l);}
}
