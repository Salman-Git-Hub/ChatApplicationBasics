package com.ali.chatapplicationbasics.search;

import java.util.HashMap;
import java.util.List;

public class SearchList {

    private String profile;
    private String username;
    private List<String> friendList;
    private String userId;
    private HashMap<String, String> groupList;


    public SearchList(String profile, String username, List<String> friendList, String userId, HashMap<String, String> groupList) {
        this.profile = profile;
        this.username = username;
        this.friendList = friendList;
        this.userId = userId;
        this.groupList = groupList;
    }

    public String getProfile() {
        return profile;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getFriendList() {
        return friendList;
    }

    public String getUserId() {
        return userId;
    }

    public HashMap<String, String> getGroupList() {
        return groupList;
    }
}
