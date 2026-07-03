package com.mineralwater.repository;

import com.mineralwater.model.ServiceablePincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, Long> {
    Optional<ServiceablePincode> findByPincode(String pincode);
    boolean existsByPincode(String pincode);
    void deleteByPincode(String pincode);
}
