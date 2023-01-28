package com.ali.chatapplicationbasics.settings;

import static android.content.Context.MODE_PRIVATE;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.ali.chatapplicationbasics.BuildConfig;
import com.ali.chatapplicationbasics.R;
import com.ali.chatapplicationbasics.RegisterActivity;
import com.ali.chatapplicationbasics.SignInActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class AccountFragment extends PreferenceFragmentCompat {


    private final OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
        }
    };
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/");
    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    private FirebaseUser user;
    private LinearProgressIndicator progressIndicator;
    private final ActivityResultLauncher<Intent> profilePicActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    try {
                        requireActivity().getOnBackPressedDispatcher()
                                .addCallback((LifecycleOwner) requireContext(), callback);
                        runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                progressIndicator.setVisibility(View.VISIBLE);
                            }
                        });
                        Uri selectedImage = result.getData().getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();
                        Thread t = new Thread(() -> uploadFromFile(picturePath));
                        t.setPriority(Thread.MAX_PRIORITY);
                        t.start();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        Thread t = new Thread(() -> uploadFromFile("none"));
                        t.setPriority(Thread.MAX_PRIORITY);
                        t.start();
                    }
                }
            });
    private FirebaseAuth mAuth;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        progressIndicator = ((AppCompatActivity) requireActivity()).findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
        setPreferencesFromResource(R.xml.account_preferences, rootKey);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        accountOpt();

    }

    private void uploadFromFile(String file) {
        DatabaseReference userRef = databaseReference.child("users").child(user.getUid());
        if (file.equals("none")) {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    progressIndicator.setVisibility(View.GONE);
                    requireActivity().
                            getOnBackPressedDispatcher()
                            .addCallback(new OnBackPressedCallback(true) {
                                @Override
                                public void handleOnBackPressed() {
                                    requireActivity().onBackPressed();
                                }
                            });
                    Toast.makeText(requireContext(), "Updated profile!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(requireContext(), "Please wait, don't press back button!", Toast.LENGTH_SHORT).show();
            }
        });
        Uri fileUri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", new File(file));
        StorageReference profileRef = storageRef.child("users/" + user.getUid() +
                "/profile.png");
        UploadTask uploadTask = profileRef.putFile(fileUri);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Uploaded profile picture!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    profileRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                userRef.child("profile_pic").setValue(task.getResult().toString());
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(requireContext(), "Updating profile!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                Thread n = new Thread(() -> updateProfile(task.getResult()));
                                n.setPriority(Thread.MAX_PRIORITY);
                                n.start();
                            } else {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(requireContext(), "Error: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });

                } else {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Couldn't upload profile picture!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void updateProfile(Uri uri) {
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        user.updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressIndicator.setVisibility(View.GONE);
                                    requireActivity().
                                            getOnBackPressedDispatcher()
                                            .addCallback(new OnBackPressedCallback(true) {
                                                @Override
                                                public void handleOnBackPressed() {
                                                    requireActivity().onBackPressed();
                                                }
                                            });
                                    Toast.makeText(requireContext(), "Updated profile!", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            try {
                                throw task.getException();
                            } catch (Exception e) {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
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
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(requireContext(), "Re-authentication required!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(requireContext(), SignInActivity.class)
                                                .putExtra("re_authenticate", false)
                                                .putExtra("pass_to_extra", false)
                                                .putExtra("delete_need", true)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    }
                                });
                            } catch (FirebaseAuthInvalidUserException userException) {
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(requireContext(), "User isn't available!", Toast.LENGTH_SHORT).show();
                                        SharedPreferences prefs = requireActivity().getSharedPreferences("shared_pref_log",
                                                MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.clear();
                                        editor.apply();
                                        mAuth.signOut();
                                        startActivity(new Intent(requireContext(), SignInActivity.class)
                                                .putExtra("re_authenticate", false)
                                                .putExtra("pass_to_extra", false)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                                        requireActivity()
                                                .overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                    }
                                });
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressIndicator.setVisibility(View.GONE);
                                    Toast.makeText(requireContext(), "Deleted account successfully!", Toast.LENGTH_SHORT).show();
                                    SharedPreferences prefs = requireActivity().getSharedPreferences("shared_pref_log",
                                            MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.clear();
                                    editor.apply();
                                    startActivity(new Intent(requireContext(), RegisterActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    requireActivity()
                                            .overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                }
                            });
                        }
                    }
                });
    }

    private void deleteData() {
        final boolean[] finished1 = {false};
        final boolean[] finished2 = {false};
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.VISIBLE);
            }
        });
        String uid = user.getUid();
        storageRef.child("users/" + uid + "/").delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(requireContext(), "Error: " + task
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
                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        finished1[0] = true;
                    }
                });
        while (!finished1[0] || !finished2[0]) ;
        new Thread(this::deleteAccount).start();
    }

    private void accountOpt() {
        Preference.OnPreferenceClickListener pass_listen = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {

                if (user.getEmail() == null) {
                    Toast.makeText(requireContext(), "User has no Email-Password provider!", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.sendPasswordResetEmail(user.getEmail())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (!task.isSuccessful()) {
                                        try {
                                            throw task.getException();
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                        startActivity(new Intent(requireContext(), SignInActivity.class)
                                                .putExtra("re_authenticate", false)
                                                .putExtra("pass_to_extra", false)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                        requireActivity().overridePendingTransition(R.anim.fade_in
                                                , R.anim.fade_out);
                                    }
                                }
                            });
                }
                return true;
            }
        };
        Preference.OnPreferenceClickListener profile_listen = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                profilePicActivity.launch(new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                return true;
            }
        };
        Preference.OnPreferenceClickListener provider_listen = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(requireContext(), ProviderActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
        };
        Preference.OnPreferenceClickListener log_listen = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                mAuth.signOut();
                SharedPreferences prefs = requireActivity().getSharedPreferences("shared_pref_log",
                        MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
                requireContext().startActivity(new Intent(requireContext(), SignInActivity.class)
                        .putExtra("re_authenticate", false)
                        .putExtra("pass_to_extra", false)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
        };
        Preference.OnPreferenceClickListener delete_listen = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setMessage("Do you want to delete the existing account?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Thread(AccountFragment.this::deleteData).start();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                builder.create().show();
                return true;
            }
        };

        Preference pass_pref = findPreference("change_password");
        Preference profile_pref = findPreference("change_profile");
        Preference provider_pref = findPreference("change_provider");
        Preference log_pref = findPreference("log_out");
        Preference delete_perf = findPreference("account_delete");

        pass_pref.setOnPreferenceClickListener(pass_listen);
        profile_pref.setOnPreferenceClickListener(profile_listen);
        provider_pref.setOnPreferenceClickListener(provider_listen);
        log_pref.setOnPreferenceClickListener(log_listen);
        delete_perf.setOnPreferenceClickListener(delete_listen);
    }

    private void runOnMainThread(Runnable runnable) {
        ContextCompat.getMainExecutor(requireContext()).execute(runnable);
    }
}