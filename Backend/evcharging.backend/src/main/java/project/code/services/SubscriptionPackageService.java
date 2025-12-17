package project.code.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.code.model.SubscriptionPackage;
import project.code.model.enums.SubscriptionType;
import project.code.repository.SubscriptionPackageRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;

    // Lấy tất cả subscription packages
    public List<SubscriptionPackage> getAllPackages() {
        return subscriptionPackageRepository.findAll();
    }

    // Lấy package theo ID
    public Optional<SubscriptionPackage> getPackageById(String packageId) {
        return subscriptionPackageRepository.findById(packageId);
    }

    // Tạo mới package
    public SubscriptionPackage createPackage(SubscriptionPackage subscriptionPackage) {
        return subscriptionPackageRepository.save(subscriptionPackage);
    }

    // Cập nhật package
    public SubscriptionPackage updatePackage(String packageId, SubscriptionPackage updatedPackage) {
        return subscriptionPackageRepository.findById(packageId)
                .map(pkg -> {
                    pkg.setName(updatedPackage.getName());
                    pkg.setDescription(updatedPackage.getDescription());
                    pkg.setPrice(updatedPackage.getPrice());
                    pkg.setDurationDays(updatedPackage.getDurationDays());
                    pkg.setBenefits(updatedPackage.getBenefits());
                    pkg.setType(updatedPackage.getType());
                    return subscriptionPackageRepository.save(pkg);
                })
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SubscriptionPackage ID: " + packageId));
    }

    // Xóa package
    public boolean deletePackage(String packageId) {
        if (subscriptionPackageRepository.existsById(packageId)) {
            subscriptionPackageRepository.deleteById(packageId);
            return true;
        }
        return false;
    }

    // Tìm package theo tên
    public Optional<SubscriptionPackage> getPackageByName(String name) {
        return subscriptionPackageRepository.findByName(name);
    }

    // Tìm packages theo từ khóa trong tên
    public List<SubscriptionPackage> searchPackagesByName(String keyword) {
        return subscriptionPackageRepository.findByNameContainingIgnoreCase(keyword);
    }

    public List<SubscriptionPackage> getPackagesByType(SubscriptionType type) {
        return subscriptionPackageRepository.findByType(type);
    }

    // Lấy packages theo khoảng giá
    public List<SubscriptionPackage> getPackagesByPriceRange(Double minPrice, Double maxPrice) {
        return subscriptionPackageRepository.findByPriceBetween(minPrice, maxPrice);
    }

    // Lấy packages có giá >= minPrice
    public List<SubscriptionPackage> getPackagesWithMinPrice(Double minPrice) {
        return subscriptionPackageRepository.findByPriceGreaterThanEqual(minPrice);
    }

    // Lấy packages có giá <= maxPrice
    public List<SubscriptionPackage> getPackagesWithMaxPrice(Double maxPrice) {
        return subscriptionPackageRepository.findByPriceLessThanEqual(maxPrice);
    }

    // Lấy packages theo số ngày hiệu lực
    public List<SubscriptionPackage> getPackagesByDuration(int durationDays) {
        return subscriptionPackageRepository.findByDurationDays(durationDays);
    }

    // Lấy packages có duration >= minDays
    public List<SubscriptionPackage> getPackagesWithMinDuration(int minDays) {
        return subscriptionPackageRepository.findByDurationDaysGreaterThanEqual(minDays);
    }

    // Tìm packages theo từ khóa trong mô tả
    public List<SubscriptionPackage> searchPackagesByDescription(String keyword) {
        return subscriptionPackageRepository.findByDescriptionContainingIgnoreCase(keyword);
    }

    // Lấy tất cả packages sắp xếp theo giá tăng dần
    public List<SubscriptionPackage> getAllPackagesSortedByPriceAsc() {
        return subscriptionPackageRepository.findAllByOrderByPriceAsc();
    }

    // Lấy tất cả packages sắp xếp theo giá giảm dần
    public List<SubscriptionPackage> getAllPackagesSortedByPriceDesc() {
        return subscriptionPackageRepository.findAllByOrderByPriceDesc();
    }

    // Lấy tất cả packages sắp xếp theo duration tăng dần
    public List<SubscriptionPackage> getAllPackagesSortedByDuration() {
        return subscriptionPackageRepository.findAllByOrderByDurationDaysAsc();
    }

    public List<SubscriptionPackage> getPackagesByTypeSorted(SubscriptionType type) {
        return subscriptionPackageRepository.findByTypeOrderByPriceAsc(type);
    }
}