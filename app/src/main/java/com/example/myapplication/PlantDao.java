package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PlantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Plant plant);


    @Query("SELECT * FROM my_plants WHERE userUid = :uid")
    List<Plant> getPlantsByUser(String uid);

    @Update
    void update(Plant plant);

    @Delete
    void delete(Plant plant);
}