package com.ali.chatapplicationbasics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import java.util.Locale;

public class ExtraActivity extends AppCompatActivity {

    private LinearProgressIndicator progressIndicator;
    private TextView textView;
    private EditText editText;
    private AppCompatButton btn;
    private TextInputLayout inputLayout;
    private FirebaseAuth mAuth;
    private boolean re_sign_in;
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra);

        mAuth = FirebaseAuth.getInstance();

        progressIndicator = findViewById(R.id.extra_progress_line);
        inputLayout = findViewById(R.id.extra_placeholder);
        textView = findViewById(R.id.extra_text);
        editText = findViewById(R.id.extra_field);
        btn = findViewById(R.id.confirm_btn);

        progressIndicator.setIndeterminate(true);
        Intent intent = getIntent();
        bundle = intent.getExtras();
        re_sign_in = bundle.getBoolean("re_sign_in");
        textView.setText(bundle.getString("main_text"));
        String hint = bundle.getString("sec_text");
        String s = hint.substring(0, 1).toUpperCase(Locale.ROOT) + hint.substring(1);
        editText.setHint(s);
        inputLayout.setHint(s);
        if (s.equals("Password")) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            inputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressIndicator.setVisibility(View.VISIBLE);
                btn.setClickable(false);
                new Thread(() -> eventDispatch()).start();
            }
        });
    }

    private void eventDispatch() {
        String reason = bundle.getString("reason");
        if (reason.equals("password")) {
            updatePassword();
        } else if (reason.equals("email")) {
            updateEmail();
        }
    }

    private void updatePassword() {
        String txt_pass = editText.getText().toString();
        if (txt_pass.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ExtraActivity.this, "Empty password!", Toast.LENGTH_SHORT).show();
                    editText.requestFocus();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    btn.setClickable(true);
                }
            });
            return;
        }
        mAuth.getCurrentUser().updatePassword(txt_pass).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressIndicator.setVisibility(View.GONE);
                            Toast.makeText(ExtraActivity.this, "Updated password successfully!", Toast.LENGTH_SHORT).show();
                            if (re_sign_in) {
                                mAuth.signOut();
                                Toast.makeText(ExtraActivity.this, "Re:sign-in needed!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ExtraActivity.this, SignInActivity.class)
                                        .putExtra("re_authenticate", false)
                                        .putExtra("pass_to_extra", false)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                            startActivity(new Intent(ExtraActivity.this, MainActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                    });

                } else {
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException weakPasswordException) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ExtraActivity.this, "Weak password!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (FirebaseAuthInvalidUserException userException) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ExtraActivity.this, "Account is no longer available!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (FirebaseAuthRecentLoginRequiredException requiredException) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ExtraActivity.this, "Re-authentication is required!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        SharedPreferences.Editor editor = getSharedPreferences("shared_pref_log", MODE_PRIVATE).edit();
                        if (editor == null) {
                            startActivity(new Intent(ExtraActivity.this, SignInActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra("re_authenticate", false));
                        }
                        editor.putBoolean("hasLogin", false);
                        editor.apply();
                        startActivity(new Intent(ExtraActivity.this, SignInActivity.class)
                                .putExtra("re_authenticate", true)
                                .putExtra("pass_to_extra", true));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressIndicator.setVisibility(View.INVISIBLE);
                            btn.setClickable(true);
                        }
                    });
                }

            }
        });
    }

    private void updateEmail() {
        String txt_email = editText.getText().toString();
        if (txt_email.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ExtraActivity.this, "Empty email!", Toast.LENGTH_SHORT).show();
                    editText.requestFocus();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    btn.setClickable(true);
                }
            });
        } else if (!Patterns.EMAIL_ADDRESS.matcher(txt_email).matches()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ExtraActivity.this, "Invalid email!", Toast.LENGTH_SHORT).show();
                    editText.requestFocus();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    btn.setClickable(true);
                }
            });
        } else {
            mAuth.getCurrentUser().updateEmail(txt_email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(ExtraActivity.this, "Updated email successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ExtraActivity.this, ExtraActivity.class)
                                        .putExtra("main_text", "Set password")
                                        .putExtra("reason", "password")
                                        .putExtra("sec_text", "password")
                                        .putExtra("re_sign_in", true)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                            }
                        });

                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException userException) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ExtraActivity.this, "Account is no longer available!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (FirebaseAuthRecentLoginRequiredException requiredException) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ExtraActivity.this, "Re-authentication is required!", Toast.LENGTH_SHORT).show();
                                }
                            });
                            SharedPreferences.Editor editor = getSharedPreferences("shared_pref_log", MODE_PRIVATE).edit();
                            if (editor == null) {
                                startActivity(new Intent(ExtraActivity.this, SignInActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .putExtra("re_authenticate", false));
                                return;
                            }
                            editor.putBoolean("hasLogin", false);
                            editor.apply();
                            startActivity(new Intent(ExtraActivity.this, SignInActivity.class)
                                    .putExtra("re_authenticate", true)
                                    .putExtra("pass_to_extra", true));
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressIndicator.setVisibility(View.INVISIBLE);
                                btn.setClickable(true);
                            }
                        });
                    }

                }
            });
        }
    }

}