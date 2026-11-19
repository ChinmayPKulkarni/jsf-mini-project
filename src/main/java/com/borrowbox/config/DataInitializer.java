package com.borrowbox.config;

import com.borrowbox.model.*;
import com.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.repository.ItemRepository;
import com.borrowbox.repository.RatingRepository;
import com.borrowbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final RatingRepository ratingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data population.");
            return;
        }

        log.info("Seeding initial demo data for BorrowBox...");

        // 1. Create Demo Users
        User u1 = User.builder()
                .usn("NNM24CS307")
                .fullName("Arjun Narayan")
                .email("arjun@college.edu")
                .password(passwordEncoder.encode("password123"))
                .phone("9876543210")
                .averageRating(5.0)
                .ratingCount(1)
                .createdAt(LocalDateTime.now())
                .build();

        User u2 = User.builder()
                .usn("NNM24CS315")
                .fullName("Chinmay Kulkarni")
                .email("chinmay@college.edu")
                .password(passwordEncoder.encode("password123"))
                .phone("9876543211")
                .averageRating(4.5)
                .ratingCount(2)
                .createdAt(LocalDateTime.now())
                .build();

        User u3 = User.builder()
                .usn("NNM24CS319")
                .fullName("Deon Steve")
                .email("deon@college.edu")
                .password(passwordEncoder.encode("password123"))
                .phone("9876543212")
                .averageRating(0.0)
                .ratingCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(u1);
        userRepository.save(u2);
        userRepository.save(u3);

        // 2. Create Demo Items
        Item item1 = Item.builder()
                .owner(u1)
                .title("Casio FX-991EX ClassWiz Calculator")
                .description("Advanced scientific calculator with high-resolution spreadsheet capabilities. Ideal for Engineering Mathematics and Physics labs.")
                .category(Category.ELECTRONICS)
                .itemCondition(ItemCondition.NEW)
                .status(ItemStatus.AVAILABLE)
                .imageUrl("https://images.unsplash.com/photo-1594980596870-8aa52a78d8cd?w=600&auto=format&fit=crop&q=80")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        Item item2 = Item.builder()
                .owner(u2)
                .title("Operating System Concepts (10th Edition - Silberschatz)")
                .description("Classic Dinosaur Book for OS course. Clean pages with minimal pencil highlights.")
                .category(Category.TEXTBOOKS)
                .itemCondition(ItemCondition.GOOD)
                .status(ItemStatus.AVAILABLE)
                .imageUrl("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        Item item3 = Item.builder()
                .owner(u1)
                .title("Digital Multimeter & Solderless Breadboard Kit")
                .description("Includes test probes, 9V battery, and standard breadboard jumper wires. Great for Digital Electronics & Microcontroller labs.")
                .category(Category.LAB_EQUIPMENT)
                .itemCondition(ItemCondition.GOOD)
                .status(ItemStatus.AVAILABLE)
                .imageUrl("https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=600&auto=format&fit=crop&q=80")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        Item item4 = Item.builder()
                .owner(u3)
                .title("65W Anker USB-C GaN Fast Charger with 2m Cable")
                .description("Fast charges Dell, HP, MacBook and Type-C phones. Dual ports (1x Type-C, 1x USB-A).")
                .category(Category.CHARGERS)
                .itemCondition(ItemCondition.NEW)
                .status(ItemStatus.AVAILABLE)
                .imageUrl("https://images.unsplash.com/photo-1622445262464-84b1456045b6?w=600&auto=format&fit=crop&q=80")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Item item5 = Item.builder()
                .owner(u2)
                .title("Yonex Muscle Power 29 Badminton Racket")
                .description("Even balance racket strung at 24lbs tension. Comes with full head cover. Suitable for campus indoor court games.")
                .category(Category.SPORTS)
                .itemCondition(ItemCondition.FAIR)
                .status(ItemStatus.AVAILABLE)
                .imageUrl("https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600&auto=format&fit=crop&q=80")
                .createdAt(LocalDateTime.now().minusDays(4))
                .build();

        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);
        itemRepository.save(item4);
        itemRepository.save(item5);

        // 3. Create Sample Borrow Request & Rating
        BorrowRequest pastReq = BorrowRequest.builder()
                .item(item1)
                .borrower(u2)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(7))
                .status(RequestStatus.RETURNED)
                .createdAt(LocalDateTime.now().minusDays(11))
                .build();
        borrowRequestRepository.save(pastReq);

        Rating r1 = Rating.builder()
                .borrowRequest(pastReq)
                .reviewer(u2)
                .reviewee(u1)
                .score(5)
                .comment("Calculator was in pristine condition, friendly handover!")
                .createdAt(LocalDateTime.now().minusDays(7))
                .build();
        ratingRepository.save(r1);

        log.info("BorrowBox sample data successfully seeded.");
    }
}

