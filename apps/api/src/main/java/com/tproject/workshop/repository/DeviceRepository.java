package com.tproject.workshop.repository;

import java.sql.Timestamp;
import java.util.List;

import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer>, DeviceRepositoryJdbc {
    List<Device> findByCustomerIdCustomerOrderByEntryDateDesc(int customerId);

    @Modifying
    @Query("UPDATE devices d SET d.lastViewedAt = :lastViewedAt WHERE d.id = :deviceId")
    int updateLastViewedAt(@Param("deviceId") int deviceId, @Param("lastViewedAt") Timestamp lastViewedAt);
}
