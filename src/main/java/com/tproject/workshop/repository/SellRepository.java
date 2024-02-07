package com.tproject.workshop.repository;

import com.tproject.workshop.model.Sell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellRepository extends JpaRepository <Sell, Integer> {
}
