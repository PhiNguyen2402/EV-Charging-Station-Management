
package project.code.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import project.code.dto.wallet.WalletBalanceApiResponse;
import project.code.dto.wallet.WalletTopUpRequest;
import project.code.model.User;
import project.code.services.EVDriverService;

// Import các DTO
import project.code.dto.evdriver.EVDriverProfileDto;
import project.code.dto.evdriver.UpdateEvDriverRequest;
import project.code.dto.vehicle.VehicleDto;
import project.code.dto.vehicle.CreateVehicleRequest;
import project.code.dto.vehicle.UpdateVehicleRequest;

import java.util.List;

@RestController
@RequestMapping("/api/evdrivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EVDRIVER')")
public class EVDriverController {

    private final EVDriverService evDriverService;

    @GetMapping("/me")
    public ResponseEntity<EVDriverProfileDto> getCurrentDriverProfile() {
        User currentUser = getCurrentUser();
        EVDriverProfileDto profile = evDriverService.getDriverProfile(currentUser);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<EVDriverProfileDto> updateCurrentDriverProfile(
            @Valid @RequestBody UpdateEvDriverRequest request) {
        User currentUser = getCurrentUser();
        EVDriverProfileDto updatedProfile = evDriverService.updateDriverProfile(currentUser, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/me/vehicles")
    public ResponseEntity<List<VehicleDto>> getCurrentDriverVehicles() {
        User currentUser = getCurrentUser();
        List<VehicleDto> vehicles = evDriverService.getVehicles(currentUser);
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping("/me/vehicles")
    public ResponseEntity<VehicleDto> addVehicleToCurrentDriver(
            @Valid @RequestBody CreateVehicleRequest request) {
        User currentUser = getCurrentUser();
        VehicleDto newVehicle = evDriverService.addVehicle(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newVehicle);
    }

    @PutMapping("/me/vehicles/{vehicleId}")
    public ResponseEntity<?> updateVehicleForCurrentDriver(
            @PathVariable Long vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {
        User currentUser = getCurrentUser();
        try {
            VehicleDto updatedVehicle = evDriverService.updateVehicle(currentUser, vehicleId, request);
            return ResponseEntity.ok(updatedVehicle);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/me/vehicles/{vehicleId}")
    public ResponseEntity<?> deleteVehicleForCurrentDriver(@PathVariable Long vehicleId) {
        User currentUser = getCurrentUser();
        try {
            evDriverService.deleteVehicle(currentUser, vehicleId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/me/wallet")
    public ResponseEntity<WalletBalanceApiResponse> getCurrentDriverWallet() {
        User currentUser = getCurrentUser();
        WalletBalanceApiResponse balance = evDriverService.getWalletBalance(currentUser);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/me/wallet/top-up")
    public ResponseEntity<WalletBalanceApiResponse> topUpWallet(
            @Valid @RequestBody WalletTopUpRequest request) {

        User currentUser = getCurrentUser();

        try {
            WalletBalanceApiResponse updatedBalance = evDriverService.topUpWallet(currentUser, request);

            System.out.println("Service 'topUpWallet' đã thực thi thành công.");

            return ResponseEntity.ok(updatedBalance);

        } catch (Exception e) {
            System.err.println("!!! LỖI TẠI EVDriverController: " + e.getMessage() + " !!!");
            throw e;
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new RuntimeException("Không thể xác định người dùng đang đăng nhập.");
        }
        return (User) authentication.getPrincipal();
    }
}