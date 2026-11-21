package com.tproject.workshop.repository;

import com.tproject.workshop.model.Payment;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT COALESCE(SUM(p.paymentValue), 0) FROM payments p WHERE p.device.id = :deviceId")
    BigDecimal sumPaymentValueByDeviceId(@Param("deviceId") Integer deviceId);

    @Query(
            value =
                    "SELECT COALESCE(SUM(payment_value), 0) FROM payments "
                            + "WHERE id_device = :deviceId AND LOWER(category) = LOWER(:category)",
            nativeQuery = true)
    BigDecimal sumPaymentValueByDeviceIdAndCategory(
            @Param("deviceId") Integer deviceId, @Param("category") String category);
}
