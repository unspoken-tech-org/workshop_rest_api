package com.tproject.workshop.repository;

import com.tproject.workshop.model.ServiceOrder;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository
        extends JpaRepository<ServiceOrder, Integer>, DeviceRepositoryJdbc {
    List<ServiceOrder> findByCustomerIdCustomerOrderByEntryDateDesc(int customerId);

    @Modifying
    @Query("UPDATE ServiceOrder d SET d.lastViewedAt = :lastViewedAt WHERE d.id = :deviceId")
    int updateLastViewedAt(
            @Param("deviceId") int deviceId, @Param("lastViewedAt") Timestamp lastViewedAt);
}
