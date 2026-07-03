package com.mineralwater.controller;

import com.mineralwater.model.ServiceablePincode;
import com.mineralwater.repository.ServiceablePincodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PincodeController {

    private final ServiceablePincodeRepository serviceablePincodeRepository;

    // Public endpoint: check if pincode is serviceable
    @GetMapping("/pincodes/check/{pincode}")
    public ResponseEntity<Map<String, Boolean>> checkPincode(@PathVariable String pincode) {
        long count = serviceablePincodeRepository.count();
        // If no pincodes are configured, all pincodes are serviceable (fallback)
        if (count == 0) {
            return ResponseEntity.ok(Map.of("serviceable", true));
        }
        boolean exists = serviceablePincodeRepository.existsByPincode(pincode);
        return ResponseEntity.ok(Map.of("serviceable", exists));
    }

    // Admin endpoints
    @GetMapping("/admin/pincodes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceablePincode>> getAllPincodes() {
        return ResponseEntity.ok(serviceablePincodeRepository.findAll());
    }

    @PostMapping("/admin/pincodes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceablePincode> addPincode(@RequestBody Map<String, String> body) {
        String code = body.get("pincode");
        if (code == null || code.trim().length() != 6 || !code.trim().matches("^[1-9][0-9]{5}$")) {
            throw new RuntimeException("Invalid pincode format. Must be 6 digits.");
        }
        
        String cleanedCode = code.trim();
        if (serviceablePincodeRepository.existsByPincode(cleanedCode)) {
            throw new RuntimeException("Pincode " + cleanedCode + " is already in the serviceable list.");
        }

        ServiceablePincode pincode = ServiceablePincode.builder()
                .pincode(cleanedCode)
                .build();
        return ResponseEntity.ok(serviceablePincodeRepository.save(pincode));
    }

    @DeleteMapping("/admin/pincodes/{pincode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deletePincode(@PathVariable String pincode) {
        if (!serviceablePincodeRepository.existsByPincode(pincode)) {
            throw new RuntimeException("Pincode not found: " + pincode);
        }
        serviceablePincodeRepository.deleteByPincode(pincode);
        return ResponseEntity.noContent().build();
    }
}
