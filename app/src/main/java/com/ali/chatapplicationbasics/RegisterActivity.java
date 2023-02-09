package com.ali.chatapplicationbasics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;

import com.ali.chatapplicationbasics.utils.GoogleSignInHandler;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;


public class RegisterActivity extends AppCompatActivity {

    LinearProgressIndicator progressIndicator;
    DatabaseReference databaseReference;
    private SignInButton googleBtn;
    private GoogleSignInHandler signInHandler;
    private AppCompatButton registerbtn;
    private TextView nameView, emailView, passwordView, signin_text;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    ActivityResultLauncher<Intent> profilePicActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    registerbtn.setClickable(false);
                    try {
                        Uri selectedImage = result.getData().getData();
                        progressIndicator.setVisibility(View.VISIBLE);
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();
                        new Thread(() -> uploadFromFile(picturePath)).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Thread(() -> uploadFromFile("none")).start();
                    }
                }
            });

    ActivityResultLauncher<Intent> signInActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    new Thread(() -> handleGoogleSignInResult(result)).start();
                }
            });

    private static byte[] getImageBytes(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (InputStream stream = url.openStream()) {
            byte[] buffer = new byte[4096];

            while (true) {
                int bytesRead = stream.read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                output.write(buffer, 0, bytesRead);
            }
        }

        return output.toByteArray();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        storageRef = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/");
        mAuth = FirebaseAuth.getInstance();

        signInHandler = new GoogleSignInHandler(this, getResources().getString(R.string.default_web_client_id));
        progressIndicator = findViewById(R.id.r_progress_line);
        registerbtn = findViewById(R.id.r_registerbtn);
        emailView = findViewById(R.id.r_email);
        passwordView = findViewById(R.id.r_password);
        nameView = findViewById(R.id.r_name);
        signin_text = findViewById(R.id.signin_text);
        googleBtn = findViewById(R.id.r_google_icon);
        progressIndicator.setIndeterminate(true);
        signin_text.setClickable(true);

        new Thread(this::start).start();

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void start() {
        if (mAuth.getCurrentUser() != null &&
                getSharedPreferences("shared_pref_log", MODE_PRIVATE)
                        .getBoolean("hasLogin", false)) {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();

        }
        signin_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, SignInActivity.class)
                        .putExtra("re_authenticate", false)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        registerbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressIndicator.setVisibility(View.VISIBLE);
                new Thread(() -> register()).start();
            }
        });

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressIndicator.setVisibility(View.VISIBLE);
                new Thread(() -> googleSignIn()).start();
            }
        });

    }

    private void googleSignIn() {
        Intent i = signInHandler.googleSignInIntent();
        signInActivityResult.launch(i);
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        AuthCredential authCredential = signInHandler.googleAuthCredential(result);
        if (authCredential != null) {
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
                                FirebaseUser user = mAuth.getCurrentUser();

                                DatabaseReference userRef = databaseReference.child("users").child(user.getUid());
                                userRef.child("email").setValue(user.getEmail());
                                userRef.child("name").setValue(user.getDisplayName());

                                userRef.child("f_list").setValue(new ArrayList<>());
                                new Thread(() -> setPassword()).start();
                                new Thread(() -> uploadFromUri(mAuth.getCurrentUser().getPhotoUrl().toString()))
                                        .start();
                                progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Success!", Toast.LENGTH_SHORT).show();
//                                startActivity(new Intent(RegisterActivity.this, MainActivity.class)
//                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                        }
                    });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RegisterActivity.this, "Google Sign-In Error!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setPassword() {
        startActivity(new Intent(RegisterActivity.this, ExtraActivity.class)
                .putExtra("main_text", "Set password")
                .putExtra("reason", "password")
                .putExtra("sec_text", "password")
                .putExtra("re_sign_in", false)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void register() {

        String txt_email = emailView.getText().toString();
        String txt_pass = passwordView.getText().toString();
        String txt_name = nameView.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(txt_email).matches()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RegisterActivity.this, "Invalid email address!", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    emailView.requestFocus();
                }
            });
        } else if (txt_pass.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RegisterActivity.this, "Empty password", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    passwordView.requestFocus();
                }
            });
        } else if (txt_name.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RegisterActivity.this, "Empty name", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    nameView.requestFocus();
                }
            });
        } else {
            mAuth.createUserWithEmailAndPassword(txt_email, txt_pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RegisterActivity.this, "Created account successfully!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                new Thread(() -> signIn(txt_name, txt_email, txt_pass)).start();
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressIndicator.setVisibility(View.GONE);
                                        try {
                                            throw task.getException();
                                        } catch (
                                                FirebaseAuthWeakPasswordException weakPasswordException) {
                                            Toast.makeText(RegisterActivity.this, "Weak Password!", Toast.LENGTH_SHORT).show();
                                        } catch (
                                                FirebaseAuthInvalidCredentialsException invalidCredentialsException) {
                                            Toast.makeText(RegisterActivity.this, "Malformed email", Toast.LENGTH_SHORT).show();
                                        } catch (
                                                FirebaseAuthUserCollisionException collisionException) {
                                            Toast.makeText(RegisterActivity.this, "Email already exists!", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                            }
                        }

                    });
        }
    }

    private void signIn(String username, String email, String password) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();
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
                            FirebaseUser user = mAuth.getCurrentUser();

                            DatabaseReference userRef = databaseReference.child("users").child(user.getUid());
                            userRef.child("email").setValue(user.getEmail());
                            userRef.child("name").setValue(user.getDisplayName());

                            userRef.child("f_list").setValue(new ArrayList<>());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressIndicator.setVisibility(View.GONE);
                                }
                            });
                            mAuth.getCurrentUser().updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (task.isSuccessful()) {
                                                        userRef.child("name").setValue(username);
                                                        Toast.makeText(RegisterActivity.this, "Select your profile picture", Toast.LENGTH_SHORT).show();
                                                        Intent i = new Intent(Intent.ACTION_PICK,
                                                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                                        new Thread(() -> profilePicActivity.launch(i)).start();
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Couldn't sign-in!", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        }
                                    });

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressIndicator.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                            try {
                                throw task.getException();
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RegisterActivity.this, "Error: "
                                                + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                    }
                });
    }

    private void uploadFromFile(String file) {
        FirebaseUser user = mAuth.getCurrentUser();
        DatabaseReference userRef = databaseReference.child("users").child(user.getUid());
        if (file.equals("none")) {
            new Thread(() -> updatePic(null)).start();
            userRef.child("profile_pic").setValue("");
            return;
        }
        Uri fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(file));
        StorageReference profileRef = storageRef.child("users/" + mAuth.getCurrentUser().getUid() +
                "/profile.png");
        UploadTask uploadTask = profileRef.putFile(fileUri);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Uploaded profile picture!", Toast.LENGTH_SHORT).show();
                    profileRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                userRef.child("profile_pic").setValue(task.getResult().toString());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RegisterActivity.this, "Updating profile!", Toast.LENGTH_SHORT).show();
                                        new Thread(() -> updatePic(task.getResult())).start();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "Couldn't upload profile picture!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

    }

    private void uploadFromUri(String uri) {
        StorageReference profileRef = storageRef.child("users/" + mAuth.getCurrentUser().getUid() +
                "/profile.png");
        FirebaseUser user = mAuth.getCurrentUser();
        DatabaseReference userRef = databaseReference.child("users").child(user.getUid());
        userRef.child("profile_pic").setValue(uri);
        new Thread(() -> {
            try {
                profileRef.putBytes(getImageBytes(uri));
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "Error retrieving profile picture!", Toast.LENGTH_SHORT).show();
                    }
                });
                e.printStackTrace();
            }
        }).start();
//        ContextCompat.getMainExecutor(this)
//                .execute(() -> Toast.makeText(this, "Uploaded profile picture!", Toast.LENGTH_SHORT).show());

    }

    private void updatePic(Uri uri) {
        if (uri == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressIndicator.setVisibility(View.GONE);
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
            });
            return;
        }
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        mAuth.getCurrentUser().updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressIndicator.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, "Updated profile!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                    finish();
                                }
                            });

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    registerbtn.setClickable(true);
                                    progressIndicator.setVisibility(View.INVISIBLE);
                                    mAuth.signOut();
                                    Toast.makeText(RegisterActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
    }

}