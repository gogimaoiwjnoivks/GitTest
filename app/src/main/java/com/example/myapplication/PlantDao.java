package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * SQLite 스토리지 엔티티에 직접 액세스하는 데이터 접근 객체(DAO) 인터페이스 규격입니다.
 * 추상화된 추적 쿼리를 통해 로컬 캐시 트랜잭션을 전담합니다.
 */
@Dao
public interface PlantDao {

    /**
     * 식물 로컬 레코드를 영속 영역에 삽입합니다.
     * 동일 Primary Key 충족 데이터가 입수될 시 중복 예외 유발을 차단하고 기존 데이터를 무조건 덮어쓰는(REPLACE) 충돌 정책을 적용합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Plant plant);

    /**
     * 인증 수행 식별자(UID) 세션 조건을 검증하여 고유 매핑된 식물 컬렉션을 내림차순 질의합니다.
     */
    @Query("SELECT * FROM my_plants WHERE userUid = :uid")
    List<Plant> getPlantsByUser(String uid);

    @Update
    void update(Plant plant);

    @Delete
    void delete(Plant plant);
}