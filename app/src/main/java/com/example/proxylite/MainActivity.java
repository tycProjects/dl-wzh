package com.example.proxylite;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText hostInput, portInput;
    private TextView status;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(16);
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        return e;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(10, 17, 32));
        scroll.addView(root);

        TextView brand = text("PROXY LITE", 14, Color.rgb(95, 220, 190));
        brand.setLetterSpacing(0.16f);
        root.addView(brand);

        TextView title = text("Proxy connection", 29, Color.WHITE);
        title.setTypeface(null, 1);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(12);
        root.addView(title, titleParams);

        TextView subtitle = text("No account required • Configure your own server", 14, Color.rgb(174, 186, 205));
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        root.addView(subtitle, subParams);

        TextView serverLabel = text("SERVER ADDRESS", 12, Color.rgb(174, 186, 205));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(30);
        root.addView(serverLabel, labelParams);

        hostInput = field("e.g. proxy.example.com", "");
        addField(root, hostInput);

        TextView portLabel = text("PORT", 12, Color.rgb(174, 186, 205));
        LinearLayout.LayoutParams portLabelParams = new LinearLayout.LayoutParams(-1, -2);
        portLabelParams.topMargin = dp(16);
        root.addView(portLabel, portLabelParams);

        portInput = field("e.g. 8080", "");
        portInput.setInputType(2);
        addField(root, portInput);

        Button connect = new Button(this);
        connect.setText("CONNECT");
        connect.setTextSize(15);
        connect.setAllCaps(false);
        connect.setTextColor(Color.rgb(10, 17, 32));
        connect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(95, 220, 190)));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(54));
        buttonParams.topMargin = dp(24);
        root.addView(connect, buttonParams);

        status = text("Status: Not connected", 15, Color.WHITE);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(20);
        root.addView(status, statusParams);

        TextView note = text(
            "Starter UI only. This build validates the server fields but does not create a proxy tunnel yet. A compatible proxy protocol/client core and a valid server are required for real traffic.",
            13, Color.rgb(174, 186, 205));
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(26);
        root.addView(note, noteParams);

        connect.setOnClickListener(v -> {
            String host = hostInput.getText().toString().trim();
            String portText = portInput.getText().toString().trim();
            int port;
            try { port = Integer.parseInt(portText); }
            catch (Exception ex) { port = -1; }
            if (host.isEmpty() || port < 1 || port > 65535) {
                Toast.makeText(this, "Enter a valid server address and port", Toast.LENGTH_SHORT).show();
                status.setText("Status: Check server details");
                status.setTextColor(Color.rgb(255, 190, 90));
                return;
            }
            status.setText("Status: Settings saved • tunnel not implemented");
            status.setTextColor(Color.rgb(255, 190, 90));
            Toast.makeText(this, "Server details validated", Toast.LENGTH_SHORT).show();
        });

        setContentView(scroll);
    }

    private void addField(LinearLayout root, EditText e) {
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(120, 134, 156));
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(95, 220, 190)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.topMargin = dp(8);
        root.addView(e, p);
    }
}
