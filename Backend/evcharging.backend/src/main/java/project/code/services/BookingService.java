package project.code.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import project.code.model.*;
import project.code.model.enums.BookingStatus;
import project.code.repository.*;
import project.code.dto.booking.CreateBookingRequest;
import project.code.dto.booking.BookingDto;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingPointRepository pointRepository;
    private final EVDriverRepository driverRepository;
    private final CSStaffRepository csStaffRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingDto createBooking(User currentUser, CreateBookingRequest request) {
        EVDriver driver = findDriverProfileByUser(currentUser);

        ChargingPoint point = pointRepository.findByIdWithLock(request.chargingPointId())
                .orElseThrow(() -> new EntityNotFoundException("Charging Point not found"));

        try {
            System.out.println("User " + currentUser.getEmail() + " đang giữ khóa và xử lý...");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long currentCount = bookingRepository.count();
        String bookingCode = String.format("BK%03d", currentCount + 1);
        System.out.println("User " + currentUser.getEmail() + " định dùng mã: " + bookingCode);
        if (request.startTime().isAfter(request.endTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        boolean isOccupied = bookingRepository.existsOverlappingBooking(point, request.startTime(), request.endTime());
        if (isOccupied) {
            System.out.println("User " + currentUser.getEmail() + " thất bại: Đã bị trùng!");
            throw new IllegalStateException("Cổng sạc này đã được đặt trong khung giờ bạn chọn.");
        }

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .driver(driver)
                .chargingPoint(point)
                .station(point.getStation())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("User " + currentUser.getEmail() + " đặt thành công với mã: " + bookingCode);

        return mapToDto(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(User currentUser) {
        EVDriver driver = findDriverProfileByUser(currentUser);
        return bookingRepository.findByDriver(driver).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto cancelBooking(User currentUser, Long bookingId) {
        EVDriver driver = findDriverProfileByUser(currentUser);
        Booking booking = findBookingByIdOrThrow(bookingId);

        if (!booking.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy lịch đặt chỗ này.");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Không thể hủy lịch đặt chỗ đã " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return mapToDto(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getAllBookingsForStation(User currentUser) {
        CSStaff staff = findStaffProfileByUser(currentUser);
        ChargingStation station = staff.getStationAssigned();

        return bookingRepository.findByChargingPoint_Station(station).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto updateBookingStatus(User currentUser, Long bookingId, BookingStatus newStatus) {
        CSStaff staff = findStaffProfileByUser(currentUser);
        Booking booking = findBookingByIdOrThrow(bookingId);

        checkStaffPermission(staff, booking);

        BookingStatus oldStatus = booking.getStatus();
        if (newStatus == BookingStatus.CONFIRMED && oldStatus != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể CONFIRM các booking đang PENDING.");
        }
        if (newStatus == BookingStatus.COMPLETED && oldStatus != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể COMPLETE các booking đang CONFIRMED.");
        }
        if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.PENDING) {
            throw new AccessDeniedException("Nhân viên không có quyền Hủy hoặc Đặt lại booking.");
        }

        booking.setStatus(newStatus);
        return mapToDto(bookingRepository.save(booking));
    }

    private EVDriver findDriverProfileByUser(User user) {
        return driverRepository.findByUserAccount(user)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ EVDriver cho người dùng: " + user.getEmail()));
    }

    private CSStaff findStaffProfileByUser(User user) {
        return csStaffRepository.findByUserAccount(user)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ CSStaff cho người dùng: " + user.getEmail()));
    }

    private Booking findBookingByIdOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch đặt chỗ ID: " + bookingId));
    }

    private void checkStaffPermission(CSStaff staff, Booking booking) {
        if (booking.getChargingPoint().getStation() == null ||
                staff.getStationAssigned() == null) {
            throw new IllegalStateException("Dữ liệu trạm sạc bị lỗi (null).");
        }

        if (!booking.getChargingPoint().getStation().getStationId().equals(
                staff.getStationAssigned().getStationId()
        )) {
            throw new AccessDeniedException("Bạn không có quyền trên booking tại trạm này.");
        }
    }

    private BookingDto mapToDto(Booking b) {
        return new BookingDto(
                b.getId(),
                b.getDriver().getId(),
                b.getChargingPoint().getChargingPointId(),
                b.getStation().getName(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus()
        );
    }
}

//package project.code.services;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import project.code.model.*;
//import project.code.model.enums.BookingStatus;
//import project.code.repository.*;
//import project.code.dto.booking.CreateBookingRequest;
//import project.code.dto.booking.BookingDto;
//
//import jakarta.persistence.EntityNotFoundException;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final ChargingPointRepository pointRepository;
//    private final EVDriverRepository driverRepository;
//    private final CSStaffRepository csStaffRepository;
//
//    // Không dùng Isolation.SERIALIZABLE -> Dùng mặc định (READ_COMMITTED)
//    @Transactional
//    public BookingDto createBooking(User currentUser, CreateBookingRequest request) {
//        EVDriver driver = findDriverProfileByUser(currentUser);
//
//        ChargingPoint point = pointRepository.findById(request.chargingPointId())
//                .orElseThrow(() -> new EntityNotFoundException("Charging Point not found"));
//
//        long currentCount = bookingRepository.count();
//        String bookingCode = String.format("BK%03d", currentCount + 1);
//
//        System.out.println("User " + currentUser.getEmail() + " đã tính được mã: " + bookingCode);
//
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        if (request.startTime().isAfter(request.endTime())) {
//            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
//        }
//
//        boolean isOccupied = bookingRepository.existsOverlappingBooking(point, request.startTime(), request.endTime());
//        if (isOccupied) {
//            throw new IllegalStateException("Cổng sạc này đã được đặt trong khung giờ bạn chọn.");
//        }
//
//        Booking booking = Booking.builder()
//                .bookingCode(bookingCode)
//                .driver(driver)
//                .chargingPoint(point)
//                .station(point.getStation())
//                .startTime(request.startTime())
//                .endTime(request.endTime())
//                .status(BookingStatus.PENDING)
//                .build();
//
//        Booking savedBooking = bookingRepository.save(booking);
//
//        System.out.println("User " + currentUser.getEmail() + " lưu thành công mã: " + bookingCode);
//
//        return mapToDto(savedBooking);
//    }
//
//    @Transactional(readOnly = true)
//    public List<BookingDto> getMyBookings(User currentUser) {
//        EVDriver driver = findDriverProfileByUser(currentUser);
//        return bookingRepository.findByDriver(driver).stream()
//                .map(this::mapToDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public BookingDto cancelBooking(User currentUser, Long bookingId) {
//        EVDriver driver = findDriverProfileByUser(currentUser);
//        Booking booking = findBookingByIdOrThrow(bookingId);
//
//        if (!booking.getDriver().getId().equals(driver.getId())) {
//            throw new AccessDeniedException("Bạn không có quyền hủy lịch đặt chỗ này.");
//        }
//
//        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
//            throw new IllegalStateException("Không thể hủy lịch đặt chỗ đã " + booking.getStatus());
//        }
//
//        booking.setStatus(BookingStatus.CANCELLED);
//        return mapToDto(bookingRepository.save(booking));
//    }
//
//    @Transactional(readOnly = true)
//    public List<BookingDto> getAllBookingsForStation(User currentUser) {
//        CSStaff staff = findStaffProfileByUser(currentUser);
//        ChargingStation station = staff.getStationAssigned();
//
//        return bookingRepository.findByChargingPoint_Station(station).stream()
//                .map(this::mapToDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public BookingDto updateBookingStatus(User currentUser, Long bookingId, BookingStatus newStatus) {
//        CSStaff staff = findStaffProfileByUser(currentUser);
//        Booking booking = findBookingByIdOrThrow(bookingId);
//
//        checkStaffPermission(staff, booking);
//
//        BookingStatus oldStatus = booking.getStatus();
//        if (newStatus == BookingStatus.CONFIRMED && oldStatus != BookingStatus.PENDING) {
//            throw new IllegalStateException("Chỉ có thể CONFIRM các booking đang PENDING.");
//        }
//        if (newStatus == BookingStatus.COMPLETED && oldStatus != BookingStatus.CONFIRMED) {
//            throw new IllegalStateException("Chỉ có thể COMPLETE các booking đang CONFIRMED.");
//        }
//        if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.PENDING) {
//            throw new AccessDeniedException("Nhân viên không có quyền Hủy hoặc Đặt lại booking.");
//        }
//
//        booking.setStatus(newStatus);
//        return mapToDto(bookingRepository.save(booking));
//    }
//
//    private EVDriver findDriverProfileByUser(User user) {
//        return driverRepository.findByUserAccount(user)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ EVDriver cho người dùng: " + user.getEmail()));
//    }
//
//    private CSStaff findStaffProfileByUser(User user) {
//        return csStaffRepository.findByUserAccount(user)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ CSStaff cho người dùng: " + user.getEmail()));
//    }
//
//    private Booking findBookingByIdOrThrow(Long bookingId) {
//        return bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch đặt chỗ ID: " + bookingId));
//    }
//
//    private void checkStaffPermission(CSStaff staff, Booking booking) {
//        if (booking.getChargingPoint().getStation() == null ||
//                staff.getStationAssigned() == null) {
//            throw new IllegalStateException("Dữ liệu trạm sạc bị lỗi (null).");
//        }
//
//        if (!booking.getChargingPoint().getStation().getStationId().equals(
//                staff.getStationAssigned().getStationId()
//        )) {
//            throw new AccessDeniedException("Bạn không có quyền trên booking tại trạm này.");
//        }
//    }
//
//    private BookingDto mapToDto(Booking b) {
//        return new BookingDto(
//                b.getId(),
//                b.getDriver().getId(),
//                b.getChargingPoint().getChargingPointId(),
//                b.getStation().getName(),
//                b.getStartTime(),
//                b.getEndTime(),
//                b.getStatus()
//        );
//    }
//}