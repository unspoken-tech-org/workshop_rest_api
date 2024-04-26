package com.tproject.workshop.repository;

import com.tproject.workshop.model.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceStatusRepository extends JpaRepository<DeviceStatus, Integer> {

    DeviceStatus findByStatusIgnoreCase(String status);
}
