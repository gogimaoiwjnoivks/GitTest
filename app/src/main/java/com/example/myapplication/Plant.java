package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 로컬 SQLite(Room) 데이터베이스 및 원격 Firestore NoSQL 클라우드 데이터 구조를 동시 충족하는
 * 하이브리드 단일 식물 도메인 데이터 모델 엔티티 클래스입니다.
 */
@Entity(tableName = "my_plants")
public class Plant {

    /** * 데이터베이스의 고유 기본키(PrimaryKey) 필드입니다.
     * 분산 클라우드 환경과의 무결성 동기화를 위해 원격 파이어스토어 문서 고유 ID(Document ID) 규격을 공유합니다.
     */
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

    /** * Firebase Cloud Firestore 역직렬화(Deserialization) 연동을 위한 필수 규격인 인자 없는 생성자(No-argument Constructor)입니다.
     * 데이터 로드 시 리플렉션 API 모델에 의해 내부적으로 자동 호출됩니다.
     */
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

    /**
     * 날짜별 일지 메모 맵 객체를 유출 위험 없이 보장 및 영속 변환하기 위한 방어적 Getter 인터페이스입니다.
     * 인스턴스가 널 상태일 경우 빈 해시맵으로 초기화 캐싱을 대행합니다.
     */
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