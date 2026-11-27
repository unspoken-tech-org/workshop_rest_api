package com.tproject.workshop.repository;

import com.tproject.workshop.model.CustomerDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerDeviceRepository extends JpaRepository<CustomerDevice, Integer> {}
