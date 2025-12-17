package project.code.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.code.model.ChargingPoint;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingPointRepository extends JpaRepository<ChargingPoint, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ChargingPoint p WHERE p.chargingPointId = :id")
    Optional<ChargingPoint> findByIdWithLock(@Param("id") Long id);

    List<ChargingPoint> findByStation_StationId(Long stationId);
}