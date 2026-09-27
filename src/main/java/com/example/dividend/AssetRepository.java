package com.example.dividend;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AssetRepository extends JpaRepository<Asset,Long> { boolean existsByCodeAndIdNot(String code,Long id); }
