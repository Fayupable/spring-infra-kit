package io.fayupable.specification.init;

import io.fayupable.specification.entity.Customer;
import io.fayupable.specification.entity.Order;
import io.fayupable.specification.entity.OrderItem;
import io.fayupable.specification.enums.OrderStatus;
import io.fayupable.specification.repository.CustomerRepository;
import io.fayupable.specification.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    private static final String[] FIRST_NAMES = {
            "Ahmet", "Mehmet", "Ayşe", "Fatma", "Ali", "Veli", "Zeynep", "Elif",
            "Mustafa", "Hüseyin", "Emine", "Hatice", "Can", "Cem", "Deniz", "Ece",
            "Burak", "Emre", "Selin", "Gizem", "Onur", "Kerem", "Merve", "Nazlı",
            "John", "Michael", "Sarah", "Emily", "David", "James", "Emma", "Olivia"
    };

    private static final String[] LAST_NAMES = {
            "Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Öztürk", "Aydın", "Arslan",
            "Koç", "Kurt", "Özdemir", "Aksoy", "Polat", "Şimşek", "Erdoğan", "Güneş",
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"
    };

    private static final String[] CITIES = {
            "Istanbul", "Ankara", "Izmir", "Antalya", "Bursa", "Adana", "Gaziantep",
            "Konya", "Mersin", "Kayseri", "Eskişehir", "Diyarbakır", "Samsun", "Denizli"
    };

    private static final String[] STREETS = {
            "Atatürk Caddesi", "İstiklal Caddesi", "Cumhuriyet Bulvarı", "Gazi Mustafa Kemal Bulvarı",
            "Fevzi Çakmak Caddesi", "Zübeyde Hanım Caddesi", "Millet Caddesi", "Barbaros Bulvarı"
    };

    private static final String[] PRODUCTS = {
            "iPhone 15 Pro", "Samsung Galaxy S24", "MacBook Pro M3", "Dell XPS 15",
            "iPad Pro", "AirPods Pro", "Sony WH-1000XM5", "Bose QuietComfort",
            "Nike Air Max", "Adidas Ultraboost", "Levi's Jeans", "North Face Jacket",
            "Dyson V15", "iRobot Roomba", "Philips Air Fryer", "Nespresso Machine",
            "LG OLED TV", "Samsung QLED TV", "PlayStation 5", "Xbox Series X",
            "Canon EOS R6", "Sony A7 IV", "GoPro Hero 12", "DJI Mini 4 Pro"
    };

    private static final String[] PAYMENT_METHODS = {
            "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "CASH_ON_DELIVERY", "PAYPAL"
    };

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0 || orderRepository.count() > 0) {
            log.info("Database already contains data. Skipping initialization.");
            log.info("Customers: {}, Orders: {}", customerRepository.count(), orderRepository.count());
            return;
        }

        log.info("Starting data initialization...");
        long startTime = System.currentTimeMillis();

        List<Customer> customers = createCustomers(500);
        customerRepository.saveAll(customers);
        log.info("Created {} customers", customers.size());

        List<Order> orders = createOrders(customers, 1200);
        orderRepository.saveAll(orders);
        log.info("Created {} orders", orders.size());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Data initialization completed in {}ms", duration);
        log.info("Total: {} customers, {} orders", customers.size(), orders.size());
    }

    private List<Customer> createCustomers(int count) {
        List<Customer> customers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String city = CITIES[random.nextInt(CITIES.length)];
            String street = STREETS[random.nextInt(STREETS.length)];

            Customer customer = Customer.builder()
                    .name(firstName + " " + lastName)
                    .email(generateEmail(firstName, lastName, i))
                    .phone(generatePhone())
                    .city(city)
                    .address(street + " No: " + (random.nextInt(100) + 1) + ", " + city)
                    .registeredAt(generatePastDate(365))
                    .build();

            customers.add(customer);
        }

        return customers;
    }

    private List<Order> createOrders(List<Customer> customers, int count) {
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            OrderStatus status = generateOrderStatus();
            LocalDateTime createdAt = generatePastDate(180);
            LocalDateTime deliveryDate = generateDeliveryDate(createdAt, status);

            Order order = Order.builder()
                    .orderNumber(generateOrderNumber(i))
                    .customer(customer)
                    .status(status)
                    .totalAmount(BigDecimal.ZERO)
                    .createdAt(createdAt)
                    .deliveryDate(deliveryDate)
                    .shippingAddress(customer.getAddress())
                    .shippingCity(customer.getCity())
                    .paymentMethod(PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)])
                    .trackingNumber(status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED ?
                            generateTrackingNumber() : null)
                    .notes(generateNotes())
                    .updatedAt(createdAt)
                    .build();

            int itemCount = random.nextInt(4) + 1;
            for (int j = 0; j < itemCount; j++) {
                OrderItem item = createOrderItem(order);
                order.addItem(item);
            }

            order.calculateTotal();

            orders.add(order);
        }

        return orders;
    }

    private OrderItem createOrderItem(Order order) {
        String productName = PRODUCTS[random.nextInt(PRODUCTS.length)];
        int quantity = random.nextInt(3) + 1;
        BigDecimal unitPrice = BigDecimal.valueOf(50 + random.nextInt(2000))
                .setScale(2, RoundingMode.HALF_UP);

        return OrderItem.builder()
                .order(order)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
    }

    private OrderStatus generateOrderStatus() {
        int rand = random.nextInt(100);
        if (rand < 30) return OrderStatus.DELIVERED;
        if (rand < 50) return OrderStatus.SHIPPED;
        if (rand < 65) return OrderStatus.PROCESSING;
        if (rand < 85) return OrderStatus.PENDING;
        return OrderStatus.CANCELLED;
    }

    private LocalDateTime generatePastDate(int maxDaysAgo) {
        int daysAgo = random.nextInt(maxDaysAgo);
        int hoursAgo = random.nextInt(24);
        int minutesAgo = random.nextInt(60);

        return LocalDateTime.now()
                .minusDays(daysAgo)
                .minusHours(hoursAgo)
                .minusMinutes(minutesAgo);
    }

    private LocalDateTime generateDeliveryDate(LocalDateTime createdAt, OrderStatus status) {
        if (status == OrderStatus.CANCELLED || status == OrderStatus.PENDING) {
            return null;
        }

        if (status == OrderStatus.DELIVERED) {
            int daysToDeliver = random.nextInt(7) + 1;
            return createdAt.plusDays(daysToDeliver);
        }

        if (status == OrderStatus.SHIPPED) {
            int daysToDeliver = random.nextInt(3) + 2;
            return createdAt.plusDays(daysToDeliver);
        }

        int daysToDeliver = random.nextInt(5) + 3;
        return createdAt.plusDays(daysToDeliver);
    }

    private String generateEmail(String firstName, String lastName, int index) {
        String domain = random.nextBoolean() ? "example.com" : "demo.com";
        return firstName.toLowerCase() + "." + lastName.toLowerCase() + index + "@" + domain;
    }

    private String generatePhone() {
        return String.format("+90 5%02d %03d %02d %02d",
                random.nextInt(100),
                random.nextInt(1000),
                random.nextInt(100),
                random.nextInt(100));
    }

    private String generateOrderNumber(int index) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("ORD-%04d%02d%02d-%04d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                index + 1);
    }

    private String generateTrackingNumber() {
        return String.format("TRK%010d", random.nextInt(1000000000));
    }

    private String generateNotes() {
        if (random.nextInt(100) < 30) {
            String[] notes = {
                    "Please deliver before 5 PM",
                    "Leave at the door",
                    "Call before delivery",
                    "Gift wrapping requested",
                    "Urgent delivery",
                    "Handle with care",
                    null
            };
            return notes[random.nextInt(notes.length)];
        }
        return null;
    }
}
