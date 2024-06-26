package com.tproject.workshop.repository;

import com.tproject.workshop.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellRepository extends JpaRepository <Sale, Integer> {
}
