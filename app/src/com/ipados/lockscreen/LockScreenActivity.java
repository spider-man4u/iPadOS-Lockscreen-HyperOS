package com.ipados.lockscreen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LockScreenActivity extends Activity {

    private SettingsManager settings;
    private Handler timeHandler;
    private Runnable timeRunnable;

    // Views
    private View rootLayout;
    private ImageView wallpaperView;
    private View filterOverlay;
    private LinearLayout widgetColumn;
    private View btnAddWidgets;
    private LinearLayout widgetBattery;
    private TextView tvBatteryPct;
    private LinearLayout widgetWeather;
    private TextView tvWeatherTemp;
    private TextView tvWeatherDesc;
    private LinearLayout widgetCalendar;
    private TextView tvEventTitle;
    private View dateBox;
    private TextView tvDate;
    private FrameLayout clockBox;
    private TextView tvClock;
    private ImageView depthForegroundView;

    // Top Bar
    private View topBar;
    private TextView btnCancel;
    private TextView btnAdd;

    // Bottom Bars
    private View lockscreenBottomBar;
    private View btnFlashlight;
    private View btnCamera;
    private View customizerBottomBar;
    private TextView tvFilterName;
    private View[] filterDots = new View[5];
    private View btnChooseWallpaper;
    private View btnToggleDepth;
    private ImageView ivDepthIcon;
    private View btnMoreOptions;

    // Font Color Sheet
    private LinearLayout fontColorSheet;
    private LinearLayout fontListContainer;
    private LinearLayout colorPaletteContainer;
    private Button btnSheetDone;

    // States
    private boolean isCustomizerMode = false;
    private boolean isFlashlightOn = false;
    private int currentFilterIndex = 0;
    private int currentClockColor = SettingsManager.DEFAULT_COLOR;
    private boolean currentDepthEnabled = true;
    private String currentWallpaper = "ipad_clean.png";

    // Gesture detection
    private float touchDownY = 0;
    private float touchDownX = 0;
    private static final int SWIPE_THRESHOLD = 150;
    private static final int REQUEST_IMAGE_PICK = 2001;

    private static final String[] FILTER_NAMES = {
            "Natural", "Studio", "Black & White", "Color Wash", "Duotone"
    };

    private static final int[] PRESET_COLORS = {
            0xFFA3E7FC, // Light Cyan (iPad Classic)
            0xFFFFFFFF, // Pure White
            0xFFFFD2A8, // Peach
            0xFFFFA6C9, // Rose Pink
            0xFFD2B3FF, // Lavender Purple
            0xFFA8FFD8, // Mint Green
            0xFFFFF3A8, // Warm Gold
            0xFF3A3A3C  // Space Gray
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureWindowSecurity();
        setContentView(R.layout.activity_lockscreen);
        settings = new SettingsManager(this);

        initViews();
        loadTypography();
        loadSettingsState();
        setupClockUpdater();
        setupBatteryMonitor();
        setupGesturesAndClicks();
        applyFilter(currentFilterIndex);
        updateModeUI();
    }

    private void configureWindowSecurity() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        wallpaperView = findViewById(R.id.wallpaper_view);
        filterOverlay = findViewById(R.id.filter_overlay);
        widgetColumn = findViewById(R.id.widget_column);
        btnAddWidgets = findViewById(R.id.btn_add_widgets);
        widgetBattery = findViewById(R.id.widget_battery);
        tvBatteryPct = findViewById(R.id.tv_battery_pct);
        widgetWeather = findViewById(R.id.widget_weather);
        tvWeatherTemp = findViewById(R.id.tv_weather_temp);
        tvWeatherDesc = findViewById(R.id.tv_weather_desc);
        widgetCalendar = findViewById(R.id.widget_calendar);
        tvEventTitle = findViewById(R.id.tv_event_title);
        dateBox = findViewById(R.id.date_box);
        tvDate = findViewById(R.id.tv_date);
        clockBox = findViewById(R.id.clock_box);
        tvClock = findViewById(R.id.tv_clock);
        depthForegroundView = findViewById(R.id.depth_foreground_view);

        topBar = findViewById(R.id.top_bar);
        btnCancel = findViewById(R.id.btn_cancel);
        btnAdd = findViewById(R.id.btn_add);

        lockscreenBottomBar = findViewById(R.id.lockscreen_bottom_bar);
        btnFlashlight = findViewById(R.id.btn_flashlight);
        btnCamera = findViewById(R.id.btn_camera);

        customizerBottomBar = findViewById(R.id.customizer_bottom_bar);
        tvFilterName = findViewById(R.id.tv_filter_name);
        filterDots[0] = findViewById(R.id.dot_0);
        filterDots[1] = findViewById(R.id.dot_1);
        filterDots[2] = findViewById(R.id.dot_2);
        filterDots[3] = findViewById(R.id.dot_3);
        filterDots[4] = findViewById(R.id.dot_4);

        btnChooseWallpaper = findViewById(R.id.btn_choose_wallpaper);
        btnToggleDepth = findViewById(R.id.btn_toggle_depth);
        ivDepthIcon = findViewById(R.id.iv_depth_icon);
        btnMoreOptions = findViewById(R.id.btn_more_options);

        fontColorSheet = findViewById(R.id.font_color_sheet);
        fontListContainer = findViewById(R.id.font_list_container);
        colorPaletteContainer = findViewById(R.id.color_palette_container);
        btnSheetDone = findViewById(R.id.btn_sheet_done);
    }

    private void loadTypography() {
        try {
            Typeface sfRounded = Typeface.createFromAsset(getAssets(), SettingsManager.FONT_SF_ROUNDED);
            tvClock.setTypeface(sfRounded);
            tvDate.setTypeface(sfRounded);
            tvFilterName.setTypeface(sfRounded);
            btnCancel.setTypeface(sfRounded);
            btnAdd.setTypeface(sfRounded);
            if (tvBatteryPct != null) tvBatteryPct.setTypeface(sfRounded);
            if (tvWeatherTemp != null) tvWeatherTemp.setTypeface(sfRounded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSettingsState() {
        currentClockColor = settings.getClockColor();
        tvClock.setTextColor(currentClockColor);

        currentDepthEnabled = settings.isDepthEnabled();
        currentFilterIndex = settings.getFilterIndex();
        currentWallpaper = settings.getWallpaperType();

        String customUri = settings.getCustomWallpaperUri();
        if (customUri != null && !customUri.isEmpty()) {
            loadWallpaperFromUri(Uri.parse(customUri));
        } else {
            loadWallpaperImage(currentWallpaper);
        }
    }

    private void loadWallpaperImage(String filename) {
        try {
            InputStream is = getAssets().open("wallpapers/" + filename);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            wallpaperView.setImageBitmap(bitmap);
            is.close();
        } catch (Exception e) {
            wallpaperView.setBackgroundColor(Color.BLACK);
        }
    }

    private void loadWallpaperFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            wallpaperView.setImageBitmap(bitmap);
            if (is != null) is.close();
        } catch (Exception e) {
            loadWallpaperImage("ipad_clean.png");
        }
    }

    private void setupClockUpdater() {
        timeHandler = new Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeAndDate();
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void updateTimeAndDate() {
        Date now = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d", Locale.getDefault());

        tvClock.setText(timeFormat.format(now));
        tvDate.setText(dateFormat.format(now));
    }

    private void setupBatteryMonitor() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale > 0) {
                int pct = (int) ((level / (float) scale) * 100);
                if (tvBatteryPct != null) tvBatteryPct.setText(pct + "%");
            }
        }
    }

    private void setupGesturesAndClicks() {
        // Long click anywhere to enter customizer mode
        rootLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isCustomizerMode) {
                    isCustomizerMode = true;
                    updateModeUI();
                    return true;
                }
                return false;
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCustomizerMode = false;
                fontColorSheet.setVisibility(View.GONE);
                loadSettingsState();
                updateModeUI();
            }
        });

        // Add / Done button
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCustomizerMode = false;
                fontColorSheet.setVisibility(View.GONE);
                settings.setClockColor(currentClockColor);
                settings.setDepthEnabled(currentDepthEnabled);
                settings.setFilterIndex(currentFilterIndex);
                settings.setWallpaperType(currentWallpaper);
                updateModeUI();
                Toast.makeText(LockScreenActivity.this, "iPadOS Lockscreen saved", Toast.LENGTH_SHORT).show();
            }
        });

        // Clock tapped in edit mode -> Open font/color picker
        clockBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCustomizerMode) {
                    toggleFontColorSheet();
                }
            }
        });

        // Add widgets button
        btnAddWidgets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LockScreenActivity.this, "Widgets: Battery, Weather, Calendar", Toast.LENGTH_SHORT).show();
            }
        });

        // Flashlight toggle
        btnFlashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        // Camera launcher
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });

        // Wallpaper switcher (tap to cycle presets, long press for gallery)
        btnChooseWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.setCustomWallpaperUri("");
                if ("ipad_clean.png".equals(currentWallpaper)) {
                    currentWallpaper = "hyperos_lock.jpg";
                    Toast.makeText(LockScreenActivity.this, "Wallpaper: Blue Flow", Toast.LENGTH_SHORT).show();
                } else {
                    currentWallpaper = "ipad_clean.png";
                    Toast.makeText(LockScreenActivity.this, "Wallpaper: iPadOS Stock", Toast.LENGTH_SHORT).show();
                }
                loadWallpaperImage(currentWallpaper);
            }
        });

        btnChooseWallpaper.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.setType("image/*");
                    startActivityForResult(pick, REQUEST_IMAGE_PICK);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        // Depth toggle
        btnToggleDepth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDepthEnabled = !currentDepthEnabled;
                ivDepthIcon.setAlpha(currentDepthEnabled ? 1.0f : 0.4f);
                Toast.makeText(LockScreenActivity.this,
                        currentDepthEnabled ? "Depth Effect On" : "Depth Effect Off",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // More options
        btnMoreOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LockScreenActivity.this, "iPadOS 18 Tablet Edition", Toast.LENGTH_SHORT).show();
            }
        });

        // Filter carousel tap
        tvFilterName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilterIndex = (currentFilterIndex + 1) % FILTER_NAMES.length;
                applyFilter(currentFilterIndex);
            }
        });

        // Sheet Done
        btnSheetDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fontColorSheet.setVisibility(View.GONE);
            }
        });

        buildColorPalette();
    }

    private void updateModeUI() {
        if (isCustomizerMode) {
            topBar.setVisibility(View.VISIBLE);
            customizerBottomBar.setVisibility(View.VISIBLE);
            lockscreenBottomBar.setVisibility(View.GONE);
            btnAddWidgets.setVisibility(View.VISIBLE);

            dateBox.setBackgroundResource(R.drawable.bg_date_box);
            clockBox.setBackgroundResource(R.drawable.bg_clock_box);
            widgetColumn.setBackgroundResource(R.drawable.bg_widget_box);
        } else {
            topBar.setVisibility(View.GONE);
            customizerBottomBar.setVisibility(View.GONE);
            lockscreenBottomBar.setVisibility(View.VISIBLE);
            btnAddWidgets.setVisibility(View.GONE);
            fontColorSheet.setVisibility(View.GONE);

            dateBox.setBackground(null);
            clockBox.setBackground(null);
            widgetColumn.setBackground(null);
        }
    }

    private void applyFilter(int index) {
        tvFilterName.setText(FILTER_NAMES[index]);
        for (int i = 0; i < filterDots.length; i++) {
            filterDots[i].setBackgroundResource(i == index ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        }

        switch (index) {
            case 0: // Natural
                wallpaperView.setColorFilter(null);
                filterOverlay.setVisibility(View.GONE);
                break;
            case 1: // Studio
                ColorMatrix cmStudio = new ColorMatrix();
                cmStudio.setSaturation(1.4f);
                wallpaperView.setColorFilter(new ColorMatrixColorFilter(cmStudio));
                filterOverlay.setVisibility(View.GONE);
                break;
            case 2: // Black & White
                ColorMatrix cmBW = new ColorMatrix();
                cmBW.setSaturation(0.0f);
                wallpaperView.setColorFilter(new ColorMatrixColorFilter(cmBW));
                filterOverlay.setVisibility(View.GONE);
                break;
            case 3: // Color Wash
                wallpaperView.setColorFilter(null);
                filterOverlay.setBackgroundColor(0x33A3E7FC);
                filterOverlay.setVisibility(View.VISIBLE);
                break;
            case 4: // Duotone
                wallpaperView.setColorFilter(null);
                filterOverlay.setBackgroundColor(0x33FFA6C9);
                filterOverlay.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void toggleFontColorSheet() {
        if (fontColorSheet.getVisibility() == View.VISIBLE) {
            fontColorSheet.setVisibility(View.GONE);
        } else {
            fontColorSheet.setVisibility(View.VISIBLE);
        }
    }

    private void buildColorPalette() {
        colorPaletteContainer.removeAllViews();
        for (final int color : PRESET_COLORS) {
            FrameLayout circle = new FrameLayout(this);
            int size = (int) (44 * getResources().getDisplayMetrics().density);
            int margin = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            circle.setLayoutParams(params);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            gd.setStroke((int) (2 * getResources().getDisplayMetrics().density), Color.WHITE);
            circle.setBackground(gd);

            circle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentClockColor = color;
                    tvClock.setTextColor(color);
                }
            });

            colorPaletteContainer.addView(circle);
        }
    }

    private void toggleFlashlight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                if (camManager != null) {
                    String cameraId = camManager.getCameraIdList()[0];
                    isFlashlightOn = !isFlashlightOn;
                    camManager.setTorchMode(cameraId, isFlashlightOn);
                    btnFlashlight.setAlpha(isFlashlightOn ? 1.0f : 0.6f);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Flashlight: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            startActivity(cameraIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void unlockAndDismiss() {
        rootLayout.animate()
                .translationY(-rootLayout.getHeight())
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                        if (km != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            km.requestDismissKeyguard(LockScreenActivity.this, null);
                        }
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isCustomizerMode) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownY = event.getY();
                touchDownX = event.getX();
                return true;

            case MotionEvent.ACTION_UP:
                float deltaY = touchDownY - event.getY();
                float deltaX = Math.abs(touchDownX - event.getX());
                if (deltaY > SWIPE_THRESHOLD && deltaY > deltaX) {
                    unlockAndDismiss();
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                settings.setCustomWallpaperUri(uri.toString());
                loadWallpaperFromUri(uri);
                Toast.makeText(this, "Custom wallpaper selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (isCustomizerMode) {
            isCustomizerMode = false;
            updateModeUI();
        }
    }
}
