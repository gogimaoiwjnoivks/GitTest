package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "my_plants")
public class Plant {

    @PrimaryKey
    @NonNull
    private String id = "";

    private String name;
    private String imageUrl;
    private String userUid = "";

    private int waterCycleDays = 0;
    private List<String> wateredDatesStrList = new ArrayList<>();
    private List<String> nutrientDatesStrList = new ArrayList<>();
    private Map<String, String> dateMemoMap = new HashMap<>();

    public Plant() {}

    public Plant(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public int getWaterCycleDays() {
        return waterCycleDays;
    }

    public void setWaterCycleDays(int waterCycleDays) {
        this.waterCycleDays = waterCycleDays;
    }

    public List<String> getWateredDatesStrList() {
        return wateredDatesStrList;
    }

    public void setWateredDatesStrList(List<String> wateredDatesStrList) {
        this.wateredDatesStrList = wateredDatesStrList;
    }

    public List<String> getNutrientDatesStrList() {
        return nutrientDatesStrList;
    }

    public void setNutrientDatesStrList(List<String> nutrientDatesStrList) {
        this.nutrientDatesStrList = nutrientDatesStrList;
    }

    public Map<String, String> getDateMemoMap() {
        if (this.dateMemoMap == null) {
            this.dateMemoMap = new HashMap<>();
        }
        return this.dateMemoMap;
    }

    public void setDateMemoMap(Map<String, String> dateMemoMap) {
        this.dateMemoMap = dateMemoMap;
    }
}