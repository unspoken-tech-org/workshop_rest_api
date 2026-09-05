package com.tproject.workshop.repository;

import com.tproject.workshop.model.CustomerPhone;
import com.tproject.workshop.model.CustomerPhoneId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPhoneRepository extends JpaRepository<CustomerPhone, CustomerPhoneId> {
    
    List<CustomerPhone> findByCustomerIdCustomer(int customerId);
    
    Optional<CustomerPhone> findByCustomerIdCustomerAndIsMainTrue(int customerId);
    
    @Modifying
    @Query("UPDATE customer_phones cp SET cp.isMain = false WHERE cp.customer.idCustomer = :customerId")
    void clearMainPhoneForCustomer(@Param("customerId") int customerId);
    
    @Query("SELECT cp FROM customer_phones cp WHERE cp.customer.idCustomer = :customerId AND cp.phone.number = :phoneNumber")
    Optional<CustomerPhone> findByCustomerIdAndPhoneNumber(@Param("customerId") int customerId, @Param("phoneNumber") String phoneNumber);
    
    void deleteByCustomerIdCustomer(int customerId);
    
    List<CustomerPhone> findByPhone_IdCellphone(int phoneId);
}
