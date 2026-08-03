package com.example.hotelsmartbookingbackend.config;

import com.example.hotelsmartbookingbackend.entity.Service;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting hotel service data initialization...");
        
        List<Service> existingServices = serviceRepository.findAll();
        List<Service> servicesToSeed = new ArrayList<>();
        
        addServiceIfMissing("Coca-Cola (Minibar)", "Nước ngọt Coca-Cola mát lạnh", new BigDecimal("20000"), "can", existingServices, servicesToSeed);
        addServiceIfMissing("Pepsi (Minibar)", "Nước ngọt Pepsi mát lạnh", new BigDecimal("20000"), "can", existingServices, servicesToSeed);
        addServiceIfMissing("Heineken Beer (Minibar)", "Bia Heineken lon", new BigDecimal("35000"), "can", existingServices, servicesToSeed);
        addServiceIfMissing("Mỳ ly Hảo Hảo (Minibar)", "Mỳ tôm ăn liền Hảo Hảo hương vị tôm chua cay", new BigDecimal("15000"), "cup", existingServices, servicesToSeed);
        addServiceIfMissing("Nước suối Aquafina (Minibar)", "Nước uống tinh khiết Aquafina 500ml", new BigDecimal("15000"), "bottle", existingServices, servicesToSeed);
        addServiceIfMissing("Bao cao su Durex (Minibar)", "Bao cao su Durex kéo dài thời gian, an toàn chất lượng", new BigDecimal("60000"), "box", existingServices, servicesToSeed);
        addServiceIfMissing("Bim Bim Lay's (Minibar)", "Khoai tây chiên Lay's vị tự nhiên", new BigDecimal("20000"), "bag", existingServices, servicesToSeed);
        addServiceIfMissing("Giặt ủi (Thông thường)", "Dịch vụ giặt ủi quần áo thông thường", new BigDecimal("30000"), "kg", existingServices, servicesToSeed);
        addServiceIfMissing("Spa Massage Body", "Dịch vụ xông hơi massage toàn thân thư giãn chuyên nghiệp", new BigDecimal("300000"), "hour", existingServices, servicesToSeed);
        addServiceIfMissing("Đưa đón sân bay bằng Ô tô", "Dịch vụ đưa đón sân bay bằng ô tô đời mới", new BigDecimal("250000"), "trip", existingServices, servicesToSeed);
        addServiceIfMissing("Thuê xe máy tự lái", "Thuê xe máy tay ga/xe số tự lái 24h", new BigDecimal("150000"), "day", existingServices, servicesToSeed);
        
        if (!servicesToSeed.isEmpty()) {
            serviceRepository.saveAll(servicesToSeed);
            log.info("Successfully seeded {} new hotel services into the database.", servicesToSeed.size());
        }

        // Seed default test user accounts if missing
        seedUserIfMissing("kietluong1412@gmail.com", "123456", "Kiệt Lương", "0901234567", "079200012345", Role.customer);
        seedUserIfMissing("admin@elysian.com", "123456", "Quản Trị Viên Elysian", "0909999999", "079200099999", Role.manager);
    }

    private void seedUserIfMissing(String email, String rawPassword, String fullName, String phone, String idCard, Role role) {
        if (userRepository.existsByEmail(email)) {
            log.info("User {} already exists, skipping seed.", email);
            return;
        }
        try {
            User user = new User();
            user.setEmail(email);
            user.setCreatedAt(Instant.now());
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setPhoneNumber(phone);
            user.setIdCardNumber(idCard);
            user.setRole(role);
            user.setStatus("Active");
            userRepository.save(user);
            log.info("Successfully seeded default user account: {}", email);
        } catch (Exception e) {
            log.warn("Could not seed default user account {}: {}", email, e.getMessage());
        }
    }

    private void addServiceIfMissing(
            String name,
            String description,
            BigDecimal price,
            String unit,
            List<Service> existingServices,
            List<Service> servicesToSeed
    ) {
        boolean exists = existingServices.stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name));
        if (!exists) {
            Service service = new Service();
            service.setName(name);
            service.setDescription(description);
            service.setPrice(price);
            service.setUnit(unit);
            service.setIsActive(true);
            service.setCreatedAt(Instant.now());
            service.setUpdatedAt(Instant.now());
            servicesToSeed.add(service);
        }
    }
}
