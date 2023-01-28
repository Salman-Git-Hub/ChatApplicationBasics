package com.ali.chatapplicationbasics.search;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.MainUser;
import com.ali.chatapplicationbasics.R;
import com.ali.chatapplicationbasics.utils.IdGenerator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchActivity extends AppCompatActivity {

    private EditText searchText;
    private CircleImageView searchBtn;
    private RecyclerView searchRecyclerView;
    private LinearProgressIndicator progressIndicator;

    private final List<SearchList> searchLists = new ArrayList<>();

    private SearchAdapter searchAdapter;

    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private MainUser mainUser;

    private final DatabaseReference database = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplicationbasics-default-rtdb.firebaseio.com/");

    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchText = findViewById(R.id.search_bar);
        searchBtn = findViewById(R.id.search_btn);
        searchRecyclerView = findViewById(R.id.search_results);
        progressIndicator = findViewById(R.id.search_progress);
        progressIndicator.setIndeterminate(true);
        progressIndicator.setVisibility(View.VISIBLE);

        userRef = database.child("users").child(user.getUid());
        new Thread(this::getUserData).start();

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation anim = AnimationUtils.loadAnimation(SearchActivity.this, R.anim.bounce);
                view.startAnimation(anim);
                new Thread(() -> searchUser()).start();
            }
        });

    }

    @Override
    protected void onStop() {
        searchLists.clear();
        super.onStop();
    }

    private void searchUser() {
        String txt = searchText.getText().toString();
        if (txt.isEmpty()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.VISIBLE);
            }
        });
        database.child("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (task.isSuccessful()) {
                            parseSnapshot(txt, task.getResult());
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SearchActivity.this, "Error: " +
                                            task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
    }

    private void parseSnapshot(String txt, DataSnapshot snapshot) {
        List<SearchList> list = new ArrayList<>();
        searchLists.clear();
        for (DataSnapshot snap : snapshot.getChildren()) {
            MainUser u = snap.getValue(MainUser.class);
            u.setUid(snap.getKey());
            SearchList s = new SearchList(u.getProfile_pic(), u.getName(),
                    u.getF_list(), u.getUid(), u.getG_list());
            list.add(s);
        }
        list.forEach(user -> {
            if (user.getUsername().contains(txt)) {
                searchLists.add(user);
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchAdapter.updateData(searchLists);
                progressIndicator.setVisibility(View.GONE);
            }
        });
    }


    private void setListener() {
        SearchAdapter.OnItemClickListener listener = new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String userId, String name, List<String> friendList, HashMap<String, String> groupList, String reason,
                                    View view) {
                if (reason.equals("add")) {
                    progressIndicator.setVisibility(View.VISIBLE);
                    new Thread(() -> addUser(userId, name, friendList, groupList, view)).start();
                } else if (reason.equals("remove")) {
                    progressIndicator.setVisibility(View.VISIBLE);
                    new Thread(() -> removeUser(userId, name, friendList, groupList, view)).start();
                }
            }
        };

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.INVISIBLE);
                searchAdapter = new SearchAdapter(searchLists, SearchActivity.this, listener, user.getUid());
                searchRecyclerView.setHasFixedSize(true);
                searchRecyclerView.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
                searchRecyclerView.setAdapter(searchAdapter);
            }
        });
    }

    private void addUser(String userId, String name, List<String> fList, HashMap<String, String> gList,
                         View view) {

        if (userId.equals(user.getUid())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SearchActivity.this, "You can't add yourself!", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.INVISIBLE);

                }
            });
            return;
        }
        String newGroupId = new IdGenerator().getAlphaNumeric();

        if (gList == null) {
            gList = new HashMap<>();
        }
        gList.put(newGroupId, "");

        if (fList == null) {
            fList = new ArrayList<>();
            fList.add(user.getUid());
        } else if (!fList.contains(user.getUid())) {
            fList.add(user.getUid());
        }
        database.child("users").child(userId)
                .child("f_list").setValue(fList);
        database.child("users").child(userId)
                .child("g_list").setValue(gList);

        List<String> userFList = mainUser.getF_list();
        if (userFList == null) {
            userFList = new ArrayList<>();
            userFList.add(userId);
        } else if (!userFList.contains(userId)) {
            userFList.add(userId);
        }
        userRef.child("f_list").setValue(userFList);
        HashMap<String, String> userGList = mainUser.getG_list();
        if (userGList == null) {
            userGList = new HashMap<>();
        }
        userGList.put(newGroupId, "");
        userRef.child("g_list").setValue(userGList);


        DatabaseReference groupRef = database.child("chat_groups")
                .child(newGroupId);
        HashMap<String, String> groupMem = new HashMap<>();
        groupMem.put(userId, "");
        groupMem.put(mainUser.getUid(), "");
        groupRef.child("members").setValue(groupMem);
        String n = name + "&&&" + mainUser.getName();
        groupRef.child("name").setValue(n);
        groupRef.child("profile_pic")
                .setValue("https://1.bp.blogspot.com/-v1K9dUjFyVk/YPiEeR6cfaI/AAAAAAAAMKk/csmoxg1wAmUuZVBNaSQIxknECOsNLYZugCLcBGAsYHQ/s311/21.jpg");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.GONE);
                ((CircleImageView) view).setImageDrawable(SearchActivity.this.getDrawable(R.drawable.check_mark_icon));
                Toast.makeText(SearchActivity.this, "Added user!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void removeUser(String userId, String name, List<String> fList, HashMap<String, String> gList,
                            View view) {
        fList.remove(user.getUid());

        database.child("users").child(userId)
                .child("f_list").setValue(fList);

        List<String> userFlist = mainUser.getF_list();
        userFlist.remove(userId);
        userRef.child("f_list").setValue(userFlist);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressIndicator.setVisibility(View.GONE);
                ((CircleImageView) view).setImageDrawable(SearchActivity.this.getDrawable(R.drawable.search_action));

                Toast.makeText(SearchActivity.this, "Removed user!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getUserData() {
        userRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    mainUser = task.getResult().getValue(MainUser.class);
                    mainUser.setUid(task.getResult().getKey());
                    new Thread(() -> setListener()).start();
                } else {
                    System.out.println(task.getException().getMessage());
                }
            }
        });
    }

}