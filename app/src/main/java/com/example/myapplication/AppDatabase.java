package com.example.myapplication;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션의 로컬 데이터 저장을 담당하는 Room 데이터베이스 클래스입니다.
 * 객체 관계형 매핑(ORM)을 통해 SQLite 라이브러리를 추상화하여 제공합니다.
 */
@Database(entities = {Plant.class}, version = 5, exportSchema = false)
@TypeConverters({AppDatabase.Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract PlantDao plantDao();
    private static AppDatabase instance;

    /**
     * 데이터베이스 인스턴스를 반환하는 싱글톤 메서드입니다.
     * 여러 스레드에서 동시에 접근할 때 인스턴스가 중복 생성되는 것을 방지합니다.
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "plant_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    /**
     * Room 데이터베이스에서 지원하지 않는 복잡한 데이터 타입을
     * SQLite가 인식할 수 있는 기본 데이터 타입(String)으로 상호 변환해 주는 변환기 클래스입니다.
     */
    public static class Converters {
        private static final Gson gson = new Gson();

        /**
         * List 객체를 데이터베이스 저장을 위한 JSON 문자열로 변환합니다.
         */
        @TypeConverter
        public static String fromList(List<String> list) {
            if (list == null) return gson.toJson(new ArrayList<String>());
            return gson.toJson(list);
        }

        /**
         * JSON 문자열을 원래의 List 객체 구조로 복원합니다.
         */
        @TypeConverter
        public static List<String> toList(String value) {
            if (value == null) return new ArrayList<>();
            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(value, listType);
        }

        /**
         * Map 객체(날짜별 기록 데이터)를 데이터베이스 저장을 위한 JSON 문자열로 변환합니다.
         */
        @TypeConverter
        public static String fromMap(Map<String, String> map) {
            if (map == null) return gson.toJson(new HashMap<String, String>());
            return gson.toJson(map);
        }

        /**
         * JSON 문자열을 원래의 Map 객체 구조로 복원합니다.
         */
        @TypeConverter
        public static Map<String, String> toMap(String value) {
            if (value == null) return new HashMap<>();
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            return gson.fromJson(value, mapType);
        }
    }
}