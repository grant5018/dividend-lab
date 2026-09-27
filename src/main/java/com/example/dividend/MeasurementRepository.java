package com.example.dividend;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MeasurementRepository extends JpaRepository<Measurement,Long> { boolean existsByAssetId(Long assetId); }
