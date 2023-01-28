package com.ali.chatapplicationbasics;

import java.util.HashMap;
import java.util.List;

public class MainUser {
    private String name;
    private String email;
    private String Uid;
    private String profile_pic;
    private String status;


    private HashMap<String, String> g_list;
    private List<String> f_list;

    public MainUser() {
    }

    public MainUser(String name, String email, String profile_pic, String status, HashMap<String, String> g_list, List<String> f_list) {
        this.name = name;
        this.email = email;
        this.profile_pic = profile_pic;
        this.status = status;
        this.g_list = g_list;
        this.f_list = f_list;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        this.Uid = uid;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public HashMap<String, String> getG_list() {
        return g_list;
    }

    public void setG_list(HashMap<String, String> g_list) {
        this.g_list = g_list;
    }

    public List<String> getF_list() {
        return f_list;
    }

    public void setF_list(List<String> f_list) {
        this.f_list = f_list;
    }

    public String getStatus() {
        return status;
    }
}
