package com.ponie.delayx;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LoginActivity extends Activity {

    public static long key = 0;

    private EditText etKey;
    private Button btnLogin;
    private TextView tvStatus;
    private FrameLayout rootLayout;
    private FrameLayout loadingOverlay;
    private Handler handler = new Handler();
    private List<View> viewsToAnimate = new ArrayList<View>();
    private ObjectAnimator pulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        createModernCyberUI();
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String savedKey = prefs.getString("saved_key", "");
        if (!savedKey.isEmpty()) {
            etKey.setText(savedKey);
        } else {
            // Key mặc định - không cần server
            etKey.setText("muopdepzais1tg");
        }
        
        startEntranceAnimation();
    }

    private void createModernCyberUI() {
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.parseColor("#050505")); 
        
        AdvancedParticleView particleBackground = new AdvancedParticleView(this);
        particleBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootLayout.addView(particleBackground);
        
        LinearLayout formContainer = new LinearLayout(this);
        formContainer.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams formParams = new FrameLayout.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280, getResources().getDisplayMetrics()),
                FrameLayout.LayoutParams.WRAP_CONTENT);
        formParams.gravity = Gravity.CENTER;
        formContainer.setLayoutParams(formParams);
        
        GradientDrawable formBg = new GradientDrawable();
        formBg.setColor(Color.parseColor("#B30A0A0F")); 
        formBg.setCornerRadius(dp(24));
        formBg.setStroke(dp(1), Color.parseColor("#4DFFFFFF")); 
        formContainer.setBackground(formBg);
        formContainer.setPadding(dp(20), dp(28), dp(20), dp(28));
        formContainer.setElevation(dp(20));
        
        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(Gravity.CENTER);
        titleLayout.setPadding(0, 0, 0, dp(2));
        
        final TextView tvIcon = new TextView(this);
        tvIcon.setText("👑Mướp MOD TEAM👑");
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvIcon.setTypeface(null, Typeface.BOLD);
        tvIcon.setPadding(0, 0, dp(6), 0);
        titleLayout.addView(tvIcon);
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Authenticator");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tvTitle.setTypeface(null, Typeface.NORMAL);
        titleLayout.addView(tvTitle);    
        
        formContainer.addView(titleLayout);
        viewsToAnimate.add(titleLayout);
        
        // 7 màu chạy liên tục (Rainbow animation)
        final int[] rainbowColors = new int[]{
            Color.parseColor("#FF0000"), // Đỏ
            Color.parseColor("#FF7F00"), // Cam
            Color.parseColor("#FFFF00"), // Vàng
            Color.parseColor("#00FF00"), // Lục
            Color.parseColor("#0000FF"), // Lam
            Color.parseColor("#4B0082"), // Chàm
            Color.parseColor("#9400D3")  // Tím
        };
        final float[] rainbowPos = new float[]{0f, 0.16f, 0.33f, 0.5f, 0.66f, 0.83f, 1f};
        
        final Runnable rainbowRunnable = new Runnable() {
            float offset = 0f;
            @Override
            public void run() {
                if (tvIcon.getWidth() <= 0) {
                    tvIcon.postDelayed(this, 50);
                    return;
                }
                offset += 0.02f;
                if (offset > 1f) offset -= 1f;
                
                // Shift colors for continuous flow
                int[] shifted = new int[rainbowColors.length];
                for (int i = 0; i < rainbowColors.length; i++) {
                    int idx = (i + (int)(offset * rainbowColors.length)) % rainbowColors.length;
                    shifted[i] = rainbowColors[idx];
                }
                
                Shader textShader = new LinearGradient(0, 0, tvIcon.getWidth(), 0,
                        shifted, rainbowPos, Shader.TileMode.CLAMP);
                tvIcon.getPaint().setShader(textShader);
                tvIcon.invalidate();
                tvIcon.postDelayed(this, 40); // ~25fps
            }
        };
        tvIcon.post(rainbowRunnable);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Mướp MOD TEAM • License System");
        tvSubtitle.setTextColor(Color.parseColor("#8BA3B3")); 
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSubtitle.setGravity(Gravity.CENTER);
        tvSubtitle.setPadding(0, 0, 0, dp(24));
        tvSubtitle.setLetterSpacing(0.05f);     
        formContainer.addView(tvSubtitle);
        viewsToAnimate.add(tvSubtitle);
        
        FrameLayout inputLayout = new FrameLayout(this);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputLp.setMargins(0, 0, 0, dp(10));
        inputLayout.setLayoutParams(inputLp);

        final GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#66000000")); 
        inputBg.setCornerRadius(dp(14));
        inputBg.setStroke(dp(1), Color.parseColor("#33FFFFFF")); 
        
        etKey = new EditText(this);
        etKey.setHint("Key mặc định: muopdepzais1tg");
        etKey.setHintTextColor(Color.parseColor("#66FFFFFF"));
        etKey.setTextColor(Color.WHITE);
        etKey.setBackground(inputBg);
        etKey.setPadding(dp(16), dp(14), dp(50), dp(14));
        etKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etKey.setSingleLine(true);
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inputLayout.addView(etKey);
        
        etKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    inputBg.setStroke(dp(2), Color.parseColor("#00F2FE"));
                } else {
                    inputBg.setStroke(dp(1), Color.parseColor("#33FFFFFF"));
                }
            }
        });

        final TextView tvEye = new TextView(this);
        tvEye.setText("👁️");
        tvEye.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvEye.setTextColor(Color.parseColor("#66FFFFFF"));
        FrameLayout.LayoutParams eyeLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        eyeLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        eyeLp.rightMargin = dp(12);
        tvEye.setLayoutParams(eyeLp);
        
        tvEye.setOnClickListener(new View.OnClickListener() {
            boolean visible = false;
            public void onClick(View v) {
                visible = !visible;
                if (visible) {
                    etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                tvEye.setAlpha(visible ? 1.0f : 0.5f);
                etKey.setSelection(etKey.getText().length());
            }
        });
        inputLayout.addView(tvEye);
        
        formContainer.addView(inputLayout);
        viewsToAnimate.add(inputLayout);
        
        tvStatus = new TextView(this);
        tvStatus.setText("");
        tvStatus.setTextColor(Color.parseColor("#FF0055")); 
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, dp(2), 0, dp(12));
        formContainer.addView(tvStatus);
        viewsToAnimate.add(tvStatus);
        
        btnLogin = new Button(this);
        btnLogin.setText("ĐĂNG NHẬP");
        btnLogin.setTextColor(Color.BLACK);
        btnLogin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnLogin.setTypeface(null, Typeface.BOLD);
        btnLogin.setAllCaps(true);
        // Neon gold glow
        btnLogin.setShadowLayer(20, 0, 0, Color.parseColor("#FFD700"));
        
        GradientDrawable btnBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#FFE566"), Color.parseColor("#FFD700"), Color.parseColor("#FFAA00")}); 
        btnBg.setCornerRadius(dp(16));
        btnBg.setStroke(dp(1), Color.parseColor("#FFFFFF"));
        btnLogin.setBackground(btnBg);
        btnLogin.setElevation(dp(10));
        btnLogin.setPadding(0, dp(15), 0, dp(15));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 0, 0, dp(12));
        btnLogin.setLayoutParams(btnLp);
        
        formContainer.addView(btnLogin);
        viewsToAnimate.add(btnLogin);
        
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                btnLogin,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.03f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.03f, 1f));
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();
        
        Button btnGetKey = new Button(this);
        btnGetKey.setText("🔑 GET KEY");
        btnGetKey.setTextColor(Color.parseColor("#B3FFFFFF"));
        btnGetKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnGetKey.setTypeface(null, Typeface.BOLD);
        btnGetKey.setAllCaps(false);
        GradientDrawable gkBg = new GradientDrawable();
        gkBg.setColor(Color.parseColor("#33000000"));
        gkBg.setCornerRadius(dp(14));
        gkBg.setStroke(dp(1), Color.parseColor("#4DFFFFFF"));
        btnGetKey.setBackground(gkBg);
        btnGetKey.setPadding(0, dp(12), 0, dp(12));
        
        formContainer.addView(btnGetKey, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        viewsToAnimate.add(btnGetKey);
        
        TextView tvFooter = new TextView(this);
        tvFooter.setText("© Mướp VIP 2026");
        tvFooter.setTextColor(Color.parseColor("#4DFFFFFF"));
        tvFooter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvFooter.setGravity(Gravity.CENTER);
        tvFooter.setPadding(0, dp(18), 0, 0);        
        formContainer.addView(tvFooter);
        viewsToAnimate.add(tvFooter);
        
        rootLayout.addView(formContainer);
        createLoadingOverlay();
        setContentView(rootLayout);
        
        setClickAnimation(btnLogin);
        setClickAnimation(btnGetKey);
        
        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { attemptLogin(); }
        });

        btnGetKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://game-license-server-2dhq.onrender.com/")));
                } catch (Exception ignored) {}
            }
        });
    }
    
    private void startEntranceAnimation() {
        for (View view : viewsToAnimate) {
            view.setAlpha(0f);
            view.setTranslationY(dp(30));
            view.setScaleX(0.9f);
            view.setScaleY(0.9f);
        }
        
        for (int i = 0; i < viewsToAnimate.size(); i++) {
            final View view = viewsToAnimate.get(i);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.animate()
                        .alpha(1f)
                        .translationY(0)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .setInterpolator(new DecelerateInterpolator(1.5f))
                        .start();
                }
            }, 80 + (i * 70));
        }
    }

    private void createLoadingOverlay() {
        loadingOverlay = new FrameLayout(this);
        loadingOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loadingOverlay.setBackgroundColor(Color.parseColor("#E6050505"));
        loadingOverlay.setClickable(true);
        loadingOverlay.setVisibility(View.GONE);
        ProgressBar pb = new ProgressBar(this);
        FrameLayout.LayoutParams pbLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        pbLp.gravity = Gravity.CENTER;
        pb.setLayoutParams(pbLp);
        pb.getIndeterminateDrawable().setColorFilter(
                Color.parseColor("#00F2FE"), android.graphics.PorterDuff.Mode.SRC_IN);

        loadingOverlay.addView(pb);
        rootLayout.addView(loadingOverlay);
    }

    private void setClickAnimation(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.7f); 
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start(); 
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1f); 
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start(); 
                        break;
                }
                return false;
            }
        });
    }

    private void attemptLogin() {
        String key = etKey.getText().toString().trim();
        if (key.isEmpty()) { 
            tvStatus.setText("Vui lòng nhập key để tiếp tục!"); 
            shakeAnimation(etKey); 
            return; 
        }
        String hwid = new KeyActivity(this).getHWID();
        tvStatus.setText("");
        loadingOverlay.setAlpha(0f);
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingOverlay.animate().alpha(1f).setDuration(200).start();
        new LoginTask().execute(key, hwid);
    }
    
    private void shakeAnimation(final View view) {
        view.animate().translationX(dp(12)).setDuration(40).withEndAction(new Runnable() {
            @Override
            public void run() {
                view.animate().translationX(-dp(12)).setDuration(40).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.animate().translationX(0).setDuration(40).start();
                    }
                }).start();
            }
        }).start();
    }

    private class LoginTask extends AsyncTask<String, Void, KeyActivity.Result> {
        @Override
        protected KeyActivity.Result doInBackground(String... p) {
            return new KeyActivity(LoginActivity.this).verify(p[0]);
        }
        
        @Override
        protected void onPostExecute(KeyActivity.Result r) {
            loadingOverlay.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                @Override
                public void run() {
                    loadingOverlay.setVisibility(View.GONE);
                }
            }).start();
            
            if (r.killed) { 
                tvStatus.setText("Ứng dụng bị khóa tạm thời!"); 
                shakeAnimation(tvStatus);
                return; 
            }
            if (r.banned) { 
                tvStatus.setText("Tài khoản của bạn đã bị cấm!"); 
                shakeAnimation(tvStatus);
                return; 
            }
            if (r.success) {
                SharedPreferences.Editor editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit();
                editor.putString("saved_key", etKey.getText().toString().trim());
                if (r.expires > 0) {
                    editor.putLong("key_expires", r.expires);
                }
                editor.commit();
                
                key = System.currentTimeMillis();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("key_auth_check", key);
                startActivity(intent);
                finish();
            } else {
                tvStatus.setText(r.message.isEmpty() ? "Key không hợp lệ hoặc đã hết hạn!" : r.message);
                shakeAnimation(tvStatus);
            }
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }

    private class AdvancedParticleView extends View {
        private Paint paintParticle;
        private Path trianglePath, plusPath;
        private Particle[] particles;
        private Random random;

        class Particle {
            float x, y, speedX, speedY, radius;
            float rotation, rotationSpeed; 
            int color, shapeType; 
        }

        public AdvancedParticleView(Context context) {
            super(context);
            paintParticle = new Paint(Paint.ANTI_ALIAS_FLAG);
            trianglePath = new Path();
            plusPath = new Path();
            random = new Random();
            
            particles = new Particle[55]; 
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            final int[] particleColors = {
                    Color.parseColor("#00F2FE"), 
                    Color.parseColor("#4FACFE"), 
                    Color.parseColor("#FF0844"), 
                    Color.parseColor("#B066FE")  
            };

            for (int i = 0; i < particles.length; i++) {
                Particle p = new Particle();
                p.x = random.nextFloat() * w;
                p.y = random.nextFloat() * h;
                p.radius = random.nextFloat() * dp(3) + dp(2); 
                p.speedX = (random.nextFloat() - 0.5f) * 1.5f; 
                p.speedY = (random.nextFloat() - 0.5f) * 1.5f;
                p.rotation = random.nextFloat() * 360f; 
                p.rotationSpeed = (random.nextFloat() - 0.5f) * 3f; 
                p.color = particleColors[random.nextInt(particleColors.length)];
                p.shapeType = random.nextInt(3); 
                particles[i] = p;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int connectDist = dp(80); 
            for (int i = 0; i < particles.length; i++) {
                Particle p = particles[i];
                if (p == null) continue;

                p.x += p.speedX;
                p.y += p.speedY;
                p.rotation += p.rotationSpeed;

                if (p.x < 0 || p.x > w) p.speedX *= -1;
                if (p.y < 0 || p.y > h) p.speedY *= -1;

                paintParticle.setColor(p.color);
                paintParticle.setStyle(Paint.Style.FILL);
                
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.rotation);

                if (p.shapeType == 0) {
                    canvas.drawCircle(0, 0, p.radius, paintParticle);
                } else if (p.shapeType == 1) {
                    trianglePath.reset();
                    trianglePath.moveTo(0, -p.radius * 1.2f);
                    trianglePath.lineTo(p.radius * 1.04f, p.radius * 0.6f);
                    trianglePath.lineTo(-p.radius * 1.04f, p.radius * 0.6f);
                    trianglePath.close();
                    canvas.drawPath(trianglePath, paintParticle);
                } else {
                    plusPath.reset();
                    float thick = p.radius * 0.3f;
                    float len = p.radius * 1.2f;
                    plusPath.addRect(-thick, -len, thick, len, Path.Direction.CW);
                    plusPath.addRect(-len, -thick, len, thick, Path.Direction.CW);
                    canvas.drawPath(plusPath, paintParticle);
                }
                canvas.restore();
              
                for (int j = i + 1; j < particles.length; j++) {
                    Particle p2 = particles[j];
                    if (p2 == null) continue;
                    
                    float dx = p.x - p2.x;
                    float dy = p.y - p2.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (dist < connectDist) {
                        paintParticle.setColor(p.color); 
                        int lineAlpha = (int) (160 * (1 - dist / connectDist));
                        paintParticle.setAlpha(lineAlpha);
                        paintParticle.setStrokeWidth(dp(1));
                        canvas.drawLine(p.x, p.y, p2.x, p2.y, paintParticle);
                    }
                }
            }
            postInvalidateDelayed(16); 
        }
    }
}
