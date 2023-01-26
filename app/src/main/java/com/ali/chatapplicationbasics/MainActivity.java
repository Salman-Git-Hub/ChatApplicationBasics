package com.ali.chatapplicationbasics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.Edits;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.chat.ChatActivity;
import com.ali.chatapplicationbasics.messages.Message;
import com.ali.chatapplicationbasics.messages.MessageList;
import com.ali.chatapplicationbasics.messages.MessagesAdapter;
import com.ali.chatapplicationbasics.search.SearchActivity;
import com.ali.chatapplicationbasics.settings.SettingsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private List<MessageList> messagesList = new ArrayList<>();
    private RecyclerView messagesRecyclerView;

    private MessagesAdapter messagesAdapter;
    private CircleImageView userProfilePic;
    private ImageView searchIcon;
    private LinearProgressIndicator progressIndicator;
    private boolean mainListenerState = false;
    private boolean loadingState = false;
    private boolean firstLoad = true;
    private TimerTask checkTask;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private MainUser mainUser;
    private StorageReference storageReference;
    private String lastMsg = "";
    private int unseenMsg = 0;

    private MessagesAdapter.OnItemClickListener listener;

    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        userProfilePic = findViewById(R.id.user_profile_pic);
        progressIndicator = findViewById(R.id.refresh_line);
        searchIcon = findViewById(R.id.search_icon);

        progressIndicator.setIndeterminate(true);
        messagesRecyclerView.setHasFixedSize(true);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        new Thread(this::setListener).start();


        if (user == null) {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, SignInActivity.class)
                    .putExtra("re_authenticate", false)
                    .putExtra("pass_to_extra", false)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

//        databaseReference.child("users").keepSynced(true);
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/");
        storageReference = FirebaseStorage.getInstance().getReference().child("users/" + user.getUid());
        progressIndicator.setVisibility(View.VISIBLE);
        new Thread(this::userData).start();

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().toString().isEmpty()){
            Picasso.get().load(user.getPhotoUrl()).into(userProfilePic);
        }
        databaseReference.child("users").child(user.getUid())
                .child("status").setValue("Online");
        databaseReference.child("users").child(user.getUid())
                        .child("profile_pic")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String url = snapshot.getValue(String.class);
                        if (url != null && !url.isEmpty()) {
                            Picasso.get().load(url).into(userProfilePic);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        userProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
        checkTask = new TimerTask() {
            @Override
            public void run() {
                new Thread(() -> checkUser()).start();
            }
        };



        new Thread(() -> new Timer().scheduleAtFixedRate(checkTask, 0L, 5*(60 * 1000))).start();
    }

    @Override
    protected void onStop() {
        databaseReference.child("users").child(user.getUid())
                .child("status").setValue("Offline");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        databaseReference.child("users").child(user.getUid())
                .child("status").setValue("Online");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        databaseReference.child("users").child(user.getUid())
                .child("status").setValue("Online");
        super.onResume();
    }

    private void setListener() {
        listener = new MessagesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MessageList message) {
                String name = message.getName();
                String last_Msg = message.getLastMessage();
                String profilePic = message.getProfilePic();
                String groupId = message.getGroupId();
                int unseen_Msg = message.getUnSeenMessages();
                new Thread() {
                    @Override
                    public void run() {
                        databaseReference.child("chat_groups")
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            loadSnap(task.getResult());
                                        } else {
                                            System.out.println(task.getException().getMessage());
                                        }
                                    }
                                });
                    }
                }.start();
                startActivity(new Intent(MainActivity.this, ChatActivity.class)
                        .putExtra("name", name)
                        .putExtra("last_msg", last_Msg)
                        .putExtra("profile", profilePic)
                        .putExtra("group", groupId)
                        .putExtra("unseen", unseen_Msg));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            }
        };
        messagesAdapter = new MessagesAdapter(messagesList, listener);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }


    private void userData() {
        DatabaseReference userRef = databaseReference.child("users").child(user.getUid()).getRef();
        userRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    mainUser = task.getResult().getValue(MainUser.class);
                    if (mainUser == null) {
                        startActivity(new Intent(MainActivity.this, RegisterActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                    else {
                        mainUser.setUid(task.getResult().getKey());
                        if (! mainListenerState) {
                            new Thread(() -> mainGroupListener()).start();
                            mainListenerState = true;
                        }
                    }
                }
                else {
                    try {
                        throw task.getException();
                    } catch (Exception e) {
                        runOnMainThread(MainActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }
    private void mainGroupListener() {
        DatabaseReference usersRef = databaseReference.child("chat_groups");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadingState = true;
                runOnMainThread(MainActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        progressIndicator.setVisibility(View.VISIBLE);
                    }
                });
                loadSnap(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ;
            }
        });
        new Thread(this::userGroupListener).start();
    }

    private void userGroupListener() {
        DatabaseReference userRef = databaseReference.child("users")
                .child(user.getUid()).child("g_list").getRef();
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, String> groupList = new HashMap<>();
                for (DataSnapshot snap: snapshot.getChildren()) {
                    if (snap.getKey() == null) {
                        return;
                    }
                    groupList.put(snap.getKey(), "");
                    DatabaseReference g_ref = databaseReference.child("chat_groups")
                            .child(snap.getKey()).child("members");
                    g_ref.child(mainUser.getUid()).setValue("");
                }
                if (groupList.isEmpty()) {
                    return;
                }
                mainUser.setG_list(groupList);
                if (loadingState) {
                    return;
                }

                databaseReference.child("chat_groups").get()
                        .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                if (task.isSuccessful()) {
                                    if (!loadingState) { // just to be extra careful
                                        new Thread(() -> loadSnap(task.getResult())).start();
                                    }
                                } else {
                                    System.out.println("Error occurred!");
                                    try {
                                        throw task.getException();
                                    } catch (Exception e) {
                                        runOnMainThread(MainActivity.this, new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnMainThread(MainActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Group Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }



    private void loadSnap(DataSnapshot snapshot) {
        messagesList.clear();
        for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
            if (mainUser.getG_list().isEmpty() || mainUser.getG_list() == null) {
                break;
            }
            if (mainUser.getG_list().keySet().contains(dataSnapshot.getKey())) {
                System.out.println(dataSnapshot.getKey());
                String groupId = dataSnapshot.getKey();
                String name = dataSnapshot.child("name").getValue(String.class);
                if (name == null) {
                    return;
                }
                if (name.contains("&&&")) {
                    name = name.replace("&&&", "").replace(user.getDisplayName(), "");
                }
                String profile_pic = dataSnapshot.child("profile_pic").getValue(String.class);
                if (profile_pic == null) {
                    profile_pic = "";
                }
                String finalProfile_pic = profile_pic;
                String finalName = name;
                databaseReference.child("chat_groups")
                        .child(groupId)
                        .child("messages")
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                boolean last = true;
                                List<DataSnapshot> newLi = new ArrayList<>();
                                snapshot.getChildren().forEach(newLi::add);
                                Collections.reverse(newLi);
                                String sender = "";
                                String id = "";
                                for (DataSnapshot snap: newLi) {
                                    if (!last) {
                                        break;
                                    }
                                    try {
                                        Message e = snap.getValue(Message.class);
                                        e.setMessageId(snap.getKey());
                                        if (e == null) {
                                            return;
                                        }
                                        sender = e.getName();
                                        lastMsg = e.getMessage();
                                        id = e.getMessageId();
                                        if (e.getSeenList() == null || e.getSeenList().isEmpty()) {
                                            unseenMsg = 0;
                                        } else if (! e.getSeenList().contains(user.getUid())) {
                                            unseenMsg ++;
                                        } else {
                                            last = false;
                                        }
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        e.printStackTrace();
                                        return;
                                    }

                                }
                                System.out.println(sender);
                                MessageList message = new MessageList(finalName, groupId, lastMsg, finalProfile_pic, unseenMsg, sender);
                                message.setMessageId(id);
                                List<MessageList> toRemove = new ArrayList<>();
                                messagesList.forEach(m -> {
                                    if (m.getName().equals(message.getName()) && m.getMessageId().equals(message.getMessageId())) {
                                        toRemove.add(m);
                                    }
                                });
                                if (! toRemove.isEmpty()) {
                                    messagesList.removeAll(toRemove);
                                }
                                messagesList.add(message);
                                unseenMsg = 0;
                                lastMsg = "";
                                loadingState = false;
                                runOnMainThread(MainActivity.this, new Runnable() {
                                    @Override
                                    public void run() {
                                        messagesAdapter.updateData(messagesList);
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }
            else {
                break;
            }

        }
        runOnMainThread(MainActivity.this, new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.GONE);
            }
        });

    }


    private void emailAuth() {
        if (! user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            runOnMainThread(MainActivity.this, new Runnable() {
                                @Override
                                public void run() {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Verification mail sent!", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(MainActivity.this, "Error sending verification mail!", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(MainActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });

        }
    }

    private void checkUser() {
        System.out.println("Reloading!");
        user.reload().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                try {
                    throw task.getException();
                } catch (FirebaseAuthInvalidUserException e) {
                    System.out.println(e.getMessage());
                    runOnMainThread(MainActivity.this, new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "User not available!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    checkTask.cancel();
                    mAuth.signOut();
                    SharedPreferences.Editor editor = getSharedPreferences("shared_pref_log", MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();
                    startActivity(new Intent(MainActivity.this, RegisterActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                } catch (Exception e) {
                    runOnMainThread(MainActivity.this, new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
            else {
                new Thread(this::emailAuth).start();
            }
        });
    }


    private void runOnMainThread(Context context, Runnable runnable) {
        ContextCompat.getMainExecutor(context).execute(runnable);
    }

}