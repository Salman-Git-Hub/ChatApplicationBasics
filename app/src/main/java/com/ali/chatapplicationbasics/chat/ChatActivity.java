package com.ali.chatapplicationbasics.chat;

import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.R;
import com.ali.chatapplicationbasics.messages.Message;
import com.ali.chatapplicationbasics.utils.RelativeTime;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private final RelativeTime relativeTime = new RelativeTime();
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/");
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private final List<ChatList> chatLists = new ArrayList<>();
    private final boolean loadingFirst = true;
    private long lastMessageTime = 0L;
    private ImageView backBtn;
    private CircleImageView profilePic, sendBtn;
    private TextView username;
    private EditText msgBox;
    private DatabaseReference chatRef;
    private Bundle bundle;
    private int unseen = 0;
    private boolean background = false;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bundle = getIntent().getExtras();
        unseen = bundle.getInt("unseen");
        chatRef = databaseReference.child("chat_groups")
                .child(bundle.getString("group"))
                .getRef();

        backBtn = findViewById(R.id.chat_back);
        profilePic = findViewById(R.id.chat_profile);
        sendBtn = findViewById(R.id.send_btn);
        username = findViewById(R.id.chat_username);
//        status = findViewById(R.id.user_status);
        msgBox = findViewById(R.id.message_text);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        if (!bundle.getString("profile").isEmpty() && bundle.getString("profile") != null) {
            Picasso.get().load(Uri.parse(bundle.getString("profile"))).into(profilePic);
        }


        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatLists, user.getUid());
        chatRecyclerView.setAdapter(chatAdapter);

        username.setText(bundle.getString("name"));
        new Thread(() -> messageListener(bundle.getString("group"))).start();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation anim = AnimationUtils.loadAnimation(ChatActivity.this, R.anim.bounce);
                view.startAnimation(anim);
                new Thread(() -> sendMsg()).start();
            }
        });

    }

    private void sendMsg() {
        String msg = msgBox.getText().toString();
        if (msg.isEmpty()) {
            return;
        }
        List<String> seenList = new ArrayList<>();
        seenList.add(user.getUid());
        Message newMsg = new Message(user.getUid(), user.getDisplayName(), msg, seenList);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.GMT_ZONE);
        long milliseconds = calendar.getTimeInMillis();
        if (milliseconds < lastMessageTime) {
            milliseconds = lastMessageTime + 1L;
        }
        String millis = String.valueOf(milliseconds);
        chatRef.child("messages")
                .child(millis)
                .setValue(newMsg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                msgBox.setText("");
            }
        });

    }

    @Override
    public void onBackPressed() {
        background = true;
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        background = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        background = false;
        super.onResume();
    }

    @Override
    protected void onRestart() {
        background = false;
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        chatRecyclerView.removeAllViews();
        super.onDestroy();
    }

    private void messageListener(String groupId) {
        databaseReference.child("chat_groups")
                .child(groupId)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getValue(Message.class) == null) {
                            return;
                        }
                        if (unseen != 0) {
                            new Thread(() -> markAsRead(snapshot, unseen)).start();
                            unseen = 0;
                        }
                        if (loadingFirst) {
                            chatLists.clear();
                        }
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Message e = snap.getValue(Message.class);
                            e.setMessageId(snap.getKey());
                            List<String> seenUser = e.getSeenList();
                            if (!seenUser.contains(user.getUid())) {
                                seenUser.add(user.getUid());
                                if (!background) {
                                    chatRef.child("messages")
                                            .child(e.getMessageId())
                                            .child("seenList")
                                            .setValue(seenUser);
                                }
                            }


                            String timeStamp = e.getMessageId();
                            String msg = e.getMessage();
                            String name = e.getName();
                            String time = relativeTime.getTimeAgo(Long.parseLong(timeStamp));
                            String sender = e.getSender();
                            lastMessageTime = Long.parseLong(timeStamp);
                            ChatList chat = new ChatList(name, msg, time, sender);
                            chatLists.add(chat);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chatAdapter.updateData(chatLists);
                                chatRecyclerView.smoothScrollToPosition(chatLists.size() - 1);
                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void markAsRead(DataSnapshot snapshot, int unseen) {
        for (DataSnapshot snap : snapshot.getChildren()) {
            if (unseen == 0) {
                break;
            }
            Message e = snap.getValue(Message.class);
            e.setMessageId(snap.getKey());
            List<String> seen = e.getSeenList();
            if (!seen.contains(user.getUid())) {
                seen.add(user.getUid());
                chatRef.child("messages")
                        .child(e.getMessageId())
                        .child("seenList")
                        .setValue(seen);
            }
            unseen--;
        }
    }
}