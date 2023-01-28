package com.ali.chatapplicationbasics;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.ali.chatapplicationbasics.utils.GoogleSignInHandler;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SignInActivity extends AppCompatActivity {

    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/");
    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    private SignInButton googleBtn;
    private EditText emailView, passView;
    private LinearProgressIndicator progressIndicator;
    private AppCompatButton loginBtn;
    private boolean re_auth = false;
    private boolean pass_to_extra = false;
    private boolean delete_need = false;
    private Bundle bundle;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
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
    private TextView forgotView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        googleBtn = findViewById(R.id.s_google_icon);
        loginBtn = findViewById(R.id.signin_btn);
        emailView = findViewById(R.id.s_email);
        passView = findViewById(R.id.s_password);
        forgotView = findViewById(R.id.forgot);
        progressIndicator = findViewById(R.id.l_progress_line);
        progressIndicator.setIndeterminate(true);


        bundle = getIntent().getExtras();
        re_auth = bundle.getBoolean("re_authenticate");
        delete_need = bundle.getBoolean("delete_need");
        pass_to_extra = bundle.getBoolean("pass_to_extra");

        if (!re_auth) {
            if (mAuth.getCurrentUser() != null &&
                    getSharedPreferences("shared_pref_log", MODE_PRIVATE)
                            .getBoolean("hasLogin", false)) {
                startActivity(new Intent(SignInActivity.this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            }
        }

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressIndicator.setVisibility(View.VISIBLE);
                new Thread(() -> googleSignIn()).start();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressIndicator.setVisibility(View.VISIBLE);
                new Thread(() -> emailSignIn()).start();
            }
        });

        forgotView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (emailView.getText().toString().isEmpty()) {
                    Toast.makeText(SignInActivity.this, "Empty email!", Toast.LENGTH_SHORT).show();
                    emailView.requestFocus();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(emailView.getText().toString()).matches()) {
                    Toast.makeText(SignInActivity.this, "Invalid email!", Toast.LENGTH_SHORT).show();
                    emailView.requestFocus();
                } else {
                    progressIndicator.setVisibility(View.VISIBLE);
                    new Thread(() -> resetPassword(emailView.getText().toString())).start();
                }
            }
        });

    }

    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            try {
                                throw task.getException();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressIndicator.setVisibility(View.INVISIBLE);
                                    Toast.makeText(SignInActivity.this, "Sent password reset email!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
    }


    private void deleteAccount() {
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            try {
                                throw task.getException();
                            } catch (
                                    FirebaseAuthRecentLoginRequiredException loginRequiredException) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        Toast.makeText(SignInActivity.this, "Re-authentication required!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } catch (FirebaseAuthInvalidUserException userException) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        Toast.makeText(SignInActivity.this, "User isn't available!", Toast.LENGTH_SHORT).show();
                                        SharedPreferences prefs = SignInActivity.this.getSharedPreferences("shared_pref_log",
                                                MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.clear();
                                        editor.apply();
                                        mAuth.signOut();
                                        startActivity(new Intent(SignInActivity.this, SignInActivity.class)
                                                .putExtra("re_authenticate", false)
                                                .putExtra("pass_to_extra", false)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                    }
                                });
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignInActivity.this, "Account deleted successfully!", Toast.LENGTH_SHORT).show();
                                    progressIndicator.setVisibility(View.GONE);
                                    startActivity(new Intent(SignInActivity.this, RegisterActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                }
                            });
                        }
                    }
                });
    }

    private void deleteData() {
        final boolean[] finished1 = {false};
        final boolean[] finished2 = {false};
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.VISIBLE);
            }
        });
        String uid = user.getUid();
        storageRef.child("users/" + uid).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignInActivity.this, "Error: " + task
                                            .getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        finished2[0] = true;
                    }
                });
        databaseReference.child("users").child(uid)
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignInActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        finished1[0] = true;
                    }
                });
        while (!finished1[0] || !finished2[0]) {
            continue;
        }
        new Thread(this::deleteAccount).start();
    }

    private void emailSignIn() {
        String email = emailView.getText().toString();
        String password = passView.getText().toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(SignInActivity.this, "Invalid email!", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty()) {
                    Toast.makeText(SignInActivity.this, "Empty email!", Toast.LENGTH_SHORT).show();
                    emailView.requestFocus();
                } else if (password.isEmpty()) {
                    Toast.makeText(SignInActivity.this, "Empty password!", Toast.LENGTH_SHORT).show();
                    passView.requestFocus();
                }
            }
        });

        if (re_auth) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SignInActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                                        if (delete_need) {
                                            new Thread(SignInActivity.this::deleteData).start();
                                            return;
                                        }
                                        if (!pass_to_extra) {
                                            SharedPreferences prefs = getSharedPreferences("shared_pref_log",
                                                    MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putBoolean("hasLogin", true);
                                            editor.apply();
                                            startActivity(new Intent(SignInActivity.this, MainActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                        } else {
                                            startActivity(new Intent(SignInActivity.this, ExtraActivity.class)
                                                    .putExtra("main_text", "Set email")
                                                    .putExtra("reason", "email")
                                                    .putExtra("sec_text", "email")
                                                    .putExtra("re_sign_in", false)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                        }
                                    }
                                });

                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthInvalidUserException userException) {
                                        Toast.makeText(SignInActivity.this, "This account isn't available anymore!", Toast.LENGTH_SHORT).show();
                                    } catch (
                                            FirebaseAuthInvalidCredentialsException credentialsException) {
                                        Toast.makeText(SignInActivity.this, "Invalid credentials!", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
        } else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                SharedPreferences prefs = getSharedPreferences("shared_pref_log",
                                        MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("name", mAuth.getCurrentUser().getDisplayName());
                                editor.putString("email", mAuth.getCurrentUser().getEmail());
                                editor.putBoolean("hasLogin", true);
                                editor.apply();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressIndicator.setVisibility(View.GONE);
                                        Toast.makeText(SignInActivity.this, "Sign-In success!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignInActivity.this, MainActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                    }
                                });

                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthInvalidUserException invalidUser) {
                                        Toast.makeText(SignInActivity.this, "Email does not exists!", Toast.LENGTH_SHORT).show();
                                    } catch (
                                            FirebaseAuthInvalidCredentialsException invalidCredentails) {
                                        Toast.makeText(SignInActivity.this, "Invalid password!", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    progressIndicator.setVisibility(View.INVISIBLE);
                                }
                            });
                        }
                    });
        }
    }


    private void googleSignIn() {
        signInHandler = new GoogleSignInHandler(this, getResources().getString(R.string.default_web_client_id));
        Intent i = signInHandler.googleSignInIntent();
        signInActivityResult.launch(i);
    }

    private void handleSignInResult(ActivityResult result) {
        AuthCredential authCredential = signInHandler.googleAuthCredential(result);
        if (authCredential != null) {
            if (re_auth) {
                user.reauthenticate(authCredential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(SignInActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                                            if (delete_need) {
                                                new Thread(SignInActivity.this::deleteData).start();
                                                return;
                                            }
                                            if (!pass_to_extra) {
                                                SharedPreferences.Editor editor = getSharedPreferences("shared_pref_log", MODE_PRIVATE).edit();
                                                editor.putBoolean("hasLogin", true);
                                                editor.apply();
                                                startActivity(new Intent(SignInActivity.this, MainActivity.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                            } else {
                                                startActivity(new Intent(SignInActivity.this, ExtraActivity.class)
                                                        .putExtra("main_text", "Set email")
                                                        .putExtra("reason", "email")
                                                        .putExtra("sec_text", "email")
                                                        .putExtra("re_sign_in", false)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                            }
                                        }
                                    });
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthInvalidUserException userException) {
                                            Toast.makeText(SignInActivity.this, "This account isn't available anymore!", Toast.LENGTH_SHORT).show();
                                        } catch (
                                                FirebaseAuthInvalidCredentialsException credentialsException) {
                                            Toast.makeText(SignInActivity.this, "Invalid credentials!", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                        }
                                    }
                                });
                            }
                        });
            } else {
                mAuth.signInWithCredential(authCredential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    SharedPreferences prefs = getSharedPreferences("shared_pref_log",
                                            MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("name", mAuth.getCurrentUser().getDisplayName());
                                    editor.putString("email", mAuth.getCurrentUser().getEmail());
                                    editor.putBoolean("hasLogin", true);
                                    editor.apply();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(SignInActivity.this, "Sign-In success!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignInActivity.this, MainActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                        }
                                    });
                                    return;
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthInvalidUserException userException) {
                                            Toast.makeText(SignInActivity.this, "This account isn't available anymore!", Toast.LENGTH_SHORT).show();
                                        } catch (
                                                FirebaseAuthInvalidCredentialsException credentialsException) {
                                            Toast.makeText(SignInActivity.this, "Invalid credentials!", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                            Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        });
            }

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SignInActivity.this, "Google Sign-in Error!", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }
}
