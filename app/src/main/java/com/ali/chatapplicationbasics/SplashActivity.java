package com.ali.chatapplicationbasics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private ImageView splashIcon;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        long cache_size = Integer.parseInt(sharedPreferences.getString("cache_size", "20")) * 1000000L;
        boolean persistence = sharedPreferences.getBoolean("app_perst", true);
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(cache_size);
            FirebaseDatabase.getInstance().setPersistenceEnabled(persistence);
            FirebaseDatabase.getInstance()
                    .getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/")
                    .keepSynced(true);
        }
        AppCompatDelegate.setDefaultNightMode(
                Integer.parseInt(sharedPreferences.getString("pref_theme", "-1"))
        );
        splashIcon = findViewById(R.id.splash_icon);
        animate();
    }

    private void animate() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        splashIcon.setAnimation(animation);
        Dexter.withContext(this)
                .withPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            }
                        }, 3000);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();


    }
}