package com.ali.chatapplicationbasics.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleSignInHandler {

    private final Context context;
    private final String idToken;
    private GoogleSignInClient signInClient;


    public GoogleSignInHandler(Context context, String idToken) {
        this.context = context;
        this.idToken = idToken;
    }

    public Intent googleSignInIntent() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(idToken)
                .requestEmail()
                .build();

        signInClient = GoogleSignIn.getClient(context, googleSignInOptions);

        return signInClient.getSignInIntent();
    }


    public AuthCredential googleAuthCredential(ActivityResult result) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        if (!task.isSuccessful()) {
            ContextCompat.getMainExecutor(context).execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Couldn't sign-in using google account!", Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
        try {
            GoogleSignInAccount googleSignInAccount = task.getResult(ApiException.class);
            if (googleSignInAccount != null) {
                return GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);
            }

        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}