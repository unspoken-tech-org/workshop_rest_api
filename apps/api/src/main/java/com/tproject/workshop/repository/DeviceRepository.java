package com.tproject.workshop.repository;

import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.model.ServiceOrder;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<ServiceOrder, Integer> {
    List<ServiceOrder> findByCustomerDeviceCustomerIdCustomerOrderByEntryDateDesc(int customerId);

    /** Whether any order of the physical device has the status (decision 3: disposed device). */
    boolean existsByCustomerDeviceIdAndDeviceStatus(int customerDeviceId, DeviceStatusEnum status);

    @Modifying
    @Query("UPDATE ServiceOrder d SET d.lastViewedAt = :lastViewedAt WHERE d.id = :deviceId")
    int updateLastViewedAt(
            @Param("deviceId") int deviceId, @Param("lastViewedAt") Timestamp lastViewedAt);
}
