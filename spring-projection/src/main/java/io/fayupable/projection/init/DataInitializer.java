package io.fayupable.projection.init;

import io.fayupable.projection.entity.Product;
import io.fayupable.projection.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Database already contains {} products. Skipping initialization.", productRepository.count());
            return;
        }

        log.info("Initializing database with sample products...");

        List<Product> products = new ArrayList<>();

        products.addAll(createElectronicsProducts());
        products.addAll(createClothingProducts());
        products.addAll(createBooksProducts());
        products.addAll(createHomeProducts());
        products.addAll(createSportsProducts());

        productRepository.saveAll(products);

        log.info("Successfully initialized database with {} products", products.size());
    }

    private List<Product> createElectronicsProducts() {
        List<Product> products = new ArrayList<>();

        String[] brands = {"Apple", "Samsung", "Sony", "Dell", "HP", "Lenovo", "LG", "Microsoft"};
        String[] products1 = {"iPhone", "MacBook", "iPad", "AirPods", "Apple Watch"};
        String[] products2 = {"Galaxy", "Galaxy Tab", "Galaxy Watch", "Galaxy Buds"};
        String[] products3 = {"XPS", "Inspiron", "Latitude", "Alienware"};
        String[] products4 = {"Surface", "Surface Pro", "Surface Laptop", "Xbox"};

        for (int i = 0; i < 50; i++) {
            String brand = brands[random.nextInt(brands.length)];
            String productName = generateElectronicsName(brand);

            products.add(Product.builder()
                    .name(productName)
                    .description(generateDescription(productName, "Electronics"))
                    .brand(brand)
                    .category("Electronics")
                    .price(BigDecimal.valueOf(299 + random.nextInt(2701)))
                    .stock(random.nextInt(100))
                    .rating(BigDecimal.valueOf(3.0 + random.nextDouble() * 2.0))
                    .imageUrl("https://cdn.example.com/products/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .thumbnailUrl("https://cdn.example.com/products/thumb/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .specifications(generateSpecifications("Electronics"))
                    .viewCount(random.nextInt(10000))
                    .salesCount(random.nextInt(1000))
                    .build());
        }

        return products;
    }

    private List<Product> createClothingProducts() {
        List<Product> products = new ArrayList<>();

        String[] brands = {"Nike", "Adidas", "Puma", "Under Armour", "Zara", "H&M", "Gap", "Levi's"};
        String[] types = {"T-Shirt", "Jeans", "Jacket", "Sneakers", "Hoodie", "Shorts", "Dress", "Sweater"};

        for (int i = 0; i < 60; i++) {
            String brand = brands[random.nextInt(brands.length)];
            String type = types[random.nextInt(types.length)];
            String productName = brand + " " + type + " " + generateColor();

            products.add(Product.builder()
                    .name(productName)
                    .description(generateDescription(productName, "Clothing"))
                    .brand(brand)
                    .category("Clothing")
                    .price(BigDecimal.valueOf(19 + random.nextInt(181)))
                    .stock(random.nextInt(200))
                    .rating(BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5))
                    .imageUrl("https://cdn.example.com/products/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .thumbnailUrl("https://cdn.example.com/products/thumb/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .specifications(generateSpecifications("Clothing"))
                    .viewCount(random.nextInt(5000))
                    .salesCount(random.nextInt(500))
                    .build());
        }

        return products;
    }

    private List<Product> createBooksProducts() {
        List<Product> products = new ArrayList<>();

        String[] publishers = {"Penguin", "HarperCollins", "Simon & Schuster", "Macmillan", "Hachette"};
        String[] genres = {"Fiction", "Mystery", "Thriller", "Romance", "Science Fiction", "Biography"};

        for (int i = 0; i < 40; i++) {
            String genre = genres[random.nextInt(genres.length)];
            String productName = "The " + generateBookTitle() + " - " + genre;
            String publisher = publishers[random.nextInt(publishers.length)];

            products.add(Product.builder()
                    .name(productName)
                    .description(generateDescription(productName, "Books"))
                    .brand(publisher)
                    .category("Books")
                    .price(BigDecimal.valueOf(9 + random.nextInt(41)))
                    .stock(random.nextInt(500))
                    .rating(BigDecimal.valueOf(3.8 + random.nextDouble() * 1.2))
                    .imageUrl("https://cdn.example.com/products/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .thumbnailUrl("https://cdn.example.com/products/thumb/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .specifications(generateSpecifications("Books"))
                    .viewCount(random.nextInt(3000))
                    .salesCount(random.nextInt(300))
                    .build());
        }

        return products;
    }

    private List<Product> createHomeProducts() {
        List<Product> products = new ArrayList<>();

        String[] brands = {"IKEA", "Wayfair", "Ashley", "West Elm", "Crate & Barrel"};
        String[] types = {"Chair", "Table", "Sofa", "Bed", "Lamp", "Cabinet", "Desk", "Shelf"};

        for (int i = 0; i < 30; i++) {
            String brand = brands[random.nextInt(brands.length)];
            String type = types[random.nextInt(types.length)];
            String productName = brand + " " + type + " " + generateStyle();

            products.add(Product.builder()
                    .name(productName)
                    .description(generateDescription(productName, "Home"))
                    .brand(brand)
                    .category("Home")
                    .price(BigDecimal.valueOf(49 + random.nextInt(951)))
                    .stock(random.nextInt(50))
                    .rating(BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5))
                    .imageUrl("https://cdn.example.com/products/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .thumbnailUrl("https://cdn.example.com/products/thumb/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .specifications(generateSpecifications("Home"))
                    .viewCount(random.nextInt(2000))
                    .salesCount(random.nextInt(200))
                    .build());
        }

        return products;
    }

    private List<Product> createSportsProducts() {
        List<Product> products = new ArrayList<>();

        String[] brands = {"Nike", "Adidas", "Under Armour", "Reebok", "Puma", "New Balance"};
        String[] types = {"Running Shoes", "Basketball", "Yoga Mat", "Dumbbell Set", "Tennis Racket", "Soccer Ball"};

        for (int i = 0; i < 20; i++) {
            String brand = brands[random.nextInt(brands.length)];
            String type = types[random.nextInt(types.length)];
            String productName = brand + " " + type + " Pro";

            products.add(Product.builder()
                    .name(productName)
                    .description(generateDescription(productName, "Sports"))
                    .brand(brand)
                    .category("Sports")
                    .price(BigDecimal.valueOf(29 + random.nextInt(271)))
                    .stock(random.nextInt(150))
                    .rating(BigDecimal.valueOf(4.0 + random.nextDouble() * 1.0))
                    .imageUrl("https://cdn.example.com/products/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .thumbnailUrl("https://cdn.example.com/products/thumb/" + productName.toLowerCase().replace(" ", "-") + ".jpg")
                    .specifications(generateSpecifications("Sports"))
                    .viewCount(random.nextInt(4000))
                    .salesCount(random.nextInt(400))
                    .build());
        }

        return products;
    }

    private String generateElectronicsName(String brand) {
        if (brand.equals("Apple")) {
            String[] products = {"iPhone 15 Pro", "MacBook Air M3", "iPad Pro", "AirPods Max", "Apple Watch Ultra"};
            return products[random.nextInt(products.length)];
        } else if (brand.equals("Samsung")) {
            String[] products = {"Galaxy S24", "Galaxy Z Fold 5", "Galaxy Tab S9", "Galaxy Watch 6"};
            return products[random.nextInt(products.length)];
        } else if (brand.equals("Dell")) {
            String[] products = {"XPS 15", "Inspiron 14", "Alienware m15", "Latitude 7420"};
            return products[random.nextInt(products.length)];
        } else {
            return brand + " " + (random.nextBoolean() ? "Laptop" : "Smartphone") + " " + (2020 + random.nextInt(5));
        }
    }

    private String generateDescription(String productName, String category) {
        String[] adjectives = {"premium", "high-quality", "durable", "comfortable", "stylish", "innovative", "reliable"};
        String[] features = {"perfect for everyday use", "designed for professionals", "ideal for enthusiasts", "great value"};

        return String.format("Experience the %s %s. %s with cutting-edge features. %s. " +
                        "Built with attention to detail and quality craftsmanship. " +
                        "Perfect addition to your %s collection. Limited stock available.",
                adjectives[random.nextInt(adjectives.length)],
                productName,
                category,
                features[random.nextInt(features.length)],
                category.toLowerCase());
    }

    private String generateSpecifications(String category) {
        switch (category) {
            case "Electronics":
                return String.format("Display: %d inch, RAM: %dGB, Storage: %dGB, Battery: %d mAh, Processor: Latest Gen",
                        5 + random.nextInt(12), 4 + random.nextInt(29), 128 + random.nextInt(897), 3000 + random.nextInt(2001));
            case "Clothing":
                return String.format("Material: %s, Size: %s, Color: %s, Care: Machine washable",
                        random.nextBoolean() ? "Cotton" : "Polyester",
                        random.nextBoolean() ? "M" : "L",
                        generateColor());
            case "Books":
                return String.format("Pages: %d, Format: %s, Language: English, ISBN: %d",
                        200 + random.nextInt(601), random.nextBoolean() ? "Hardcover" : "Paperback",
                        1000000000 + random.nextInt(900000000));
            case "Home":
                return String.format("Dimensions: %dx%dx%d cm, Material: %s, Color: %s, Assembly: Required",
                        50 + random.nextInt(151), 50 + random.nextInt(151), 50 + random.nextInt(151),
                        random.nextBoolean() ? "Wood" : "Metal", generateColor());
            case "Sports":
                return String.format("Weight: %d kg, Material: %s, Size: %s, Warranty: 1 year",
                        1 + random.nextInt(10), random.nextBoolean() ? "Synthetic" : "Leather",
                        random.nextBoolean() ? "Standard" : "Professional");
            default:
                return "High-quality product with excellent features.";
        }
    }

    private String generateColor() {
        String[] colors = {"Black", "White", "Blue", "Red", "Green", "Gray", "Navy", "Brown"};
        return colors[random.nextInt(colors.length)];
    }

    private String generateStyle() {
        String[] styles = {"Modern", "Classic", "Vintage", "Contemporary", "Minimalist"};
        return styles[random.nextInt(styles.length)];
    }

    private String generateBookTitle() {
        String[] titles = {"Secret Garden", "Lost City", "Silent Night", "Golden Dawn", "Dark Mystery",
                "Hidden Truth", "Final Chapter", "Broken Code", "Rising Sun", "Endless Road"};
        return titles[random.nextInt(titles.length)];
    }
}