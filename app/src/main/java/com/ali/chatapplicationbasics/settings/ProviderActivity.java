package com.ali.chatapplicationbasics.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.R;
import com.ali.chatapplicationbasics.RegisterActivity;
import com.ali.chatapplicationbasics.SignInActivity;
import com.ali.chatapplicationbasics.utils.GoogleSignInHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;


public class ProviderActivity extends AppCompatActivity {
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private RecyclerView providerView;
    private GoogleSignInHandler signInHandler;
    ActivityResultLauncher<Intent> signInActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        new Thread(() -> handleSignInResult(result)).start();
                    }
                }
            }
    );
    private final ProviderHolder.OnItemClickListener listener = new ProviderHolder.OnItemClickListener() {
        @Override
        public void onItemClick(String actionId, String provider, ImageView view) {
            if (actionId.equals("true")) {
                if (provider.equals("Google")) {
                    if (getUserProvider().size() > 1) {
                        new Thread(() -> removeProvider(GoogleAuthProvider.PROVIDER_ID)).start();
                        view.setContentDescription("false");
                        view.setImageResource(R.drawable.add_icon);
//                        onBackPressed();
                    } else {
                        Toast.makeText(ProviderActivity.this, "There must be at least one provider!", Toast.LENGTH_SHORT).show();
                    }
                } else if (provider.equals("Email-Password")) {
                    if (getUserProvider().size() > 1) {
                        new Thread(() -> removeProvider(EmailAuthProvider.PROVIDER_ID)).start();
                        view.setContentDescription("false");
                        view.setImageResource(R.drawable.add_icon);
//                        onBackPressed();
                    } else {
                        Toast.makeText(ProviderActivity.this, "There must be at least one provider!", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (provider.equals("Google")) {
                    Intent i = signInHandler.googleSignInIntent();
                    signInActivityResult.launch(i);
                    view.setContentDescription("true");
                    view.setImageResource(R.drawable.check_mark_icon);
                } else if (provider.equals("Email-Password")) {
                    view.setContentDescription("true");
                    view.setImageResource(R.drawable.check_mark_icon);
                    Toast.makeText(ProviderActivity.this, "Re-authentication is needed!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProviderActivity.this, SignInActivity.class)
                            .putExtra("pass_to_extra", true)
                            .putExtra("re_authenticate", true)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
            }
        }
    };
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider);
        signInHandler = new GoogleSignInHandler(this, getResources().getString(R.string.default_web_client_id));
        providerView = findViewById(R.id.provider_view);
        toolbar = findViewById(R.id.provider_toolbar);
        providerView.setHasFixedSize(true);
        providerView.setLayoutManager(new LinearLayoutManager(null));
        toolbar.setTitle("Providers");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        reloadUser();
    }

    private void addProviders() {
        providerView.setAdapter(new ProviderHolder(getUserProvider(), this, listener));
    }


    private void reloadUser() {
        user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException userException) {
                        runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ProviderActivity.this, "This account is no longer available!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ProviderActivity.this, RegisterActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    addProviders();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void handleSignInResult(ActivityResult result) {
        AuthCredential authCredential = signInHandler.googleAuthCredential(result);
        mAuth.signInWithCredential(authCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ProviderActivity.this, "Registered Google Sign-in!", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException userException) {
                                Toast.makeText(ProviderActivity.this, "This account isn't available anymore!", Toast.LENGTH_SHORT).show();
                            } catch (FirebaseAuthInvalidCredentialsException
                                    credentialsException) {
                                Toast.makeText(ProviderActivity.this, "Invalid credentials!", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
    }

    private void removeProvider(String providerId) {
        user.unlink(providerId)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ProviderActivity.this, "Unregistered!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException userException) {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ProviderActivity.this, "Account does not exists!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(ProviderActivity.this, RegisterActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                    }
                                });
                            } catch (Exception e) {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ProviderActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                });
    }


    private List<String> getUserProvider() {
        List<String> providerList = new ArrayList<>();
        user.getProviderData().forEach(userInfo -> {
            String provider = userInfo.getProviderId();
            if (!provider.equals("firebase")) {
                providerList.add(provider);
            }
        });
        return providerList;
    }

    private void runOnMainThread(Runnable runnable) {
        ContextCompat.getMainExecutor(this).execute(runnable);
    }
}