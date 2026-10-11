package com.tproject.workshop.repository;

import java.util.List;

import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer>, DeviceRepositoryJdbc {
    List<Device> findByCustomerIdCustomerOrderByEntryDateDesc(int customerId);
}
