\c appdb

SET search_path TO app_schema;

-- Insert sample users
INSERT INTO users (username, email, password_hash, full_name, status) VALUES
                                                                          ('john_doe', 'john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John Doe', 'ACTIVE'),
                                                                          ('jane_smith', 'jane.smith@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Jane Smith', 'ACTIVE'),
                                                                          ('bob_wilson', 'bob.wilson@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob Wilson', 'ACTIVE'),
                                                                          ('alice_brown', 'alice.brown@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice Brown', 'INACTIVE'),
                                                                          ('charlie_davis', 'charlie.davis@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Charlie Davis', 'SUSPENDED');

-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity, category, is_active) VALUES
                                                                                         ('Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 50, 'Electronics', true),
                                                                                         ('Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 200, 'Accessories', true),
                                                                                         ('USB-C Cable', '2m USB-C to USB-C charging cable', 19.99, 500, 'Accessories', true),
                                                                                         ('Mechanical Keyboard', 'RGB mechanical keyboard with blue switches', 149.99, 75, 'Accessories', true),
                                                                                         ('4K Monitor', '27-inch 4K IPS monitor', 449.99, 30, 'Electronics', true),
                                                                                         ('Webcam HD', '1080p webcam with built-in microphone', 79.99, 100, 'Electronics', true),
                                                                                         ('Laptop Stand', 'Adjustable aluminum laptop stand', 39.99, 150, 'Accessories', true),
                                                                                         ('External SSD 1TB', 'Portable SSD with USB 3.2 Gen 2', 129.99, 80, 'Storage', true),
                                                                                         ('Headphones Pro', 'Noise-cancelling wireless headphones', 299.99, 45, 'Audio', true),
                                                                                         ('Phone Case', 'Protective phone case for iPhone 14', 24.99, 300, 'Accessories', false);

-- Insert sample orders
INSERT INTO orders (user_id, order_number, total_amount, status, shipping_address) VALUES
                                                                                       (1, 'ORD-2024-001', 1329.98, 'DELIVERED', '123 Main St, New York, NY 10001'),
                                                                                       (1, 'ORD-2024-002', 449.99, 'SHIPPED', '123 Main St, New York, NY 10001'),
                                                                                       (2, 'ORD-2024-003', 179.98, 'PROCESSING', '456 Oak Ave, Los Angeles, CA 90001'),
                                                                                       (3, 'ORD-2024-004', 1299.99, 'PENDING', '789 Pine Rd, Chicago, IL 60601'),
                                                                                       (2, 'ORD-2024-005', 79.99, 'CANCELLED', '456 Oak Ave, Los Angeles, CA 90001');

-- Insert order items
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) VALUES
-- Order 1 items
(1, 1, 1, 1299.99, 1299.99),
(1, 2, 1, 29.99, 29.99),
-- Order 2 items
(2, 5, 1, 449.99, 449.99),
-- Order 3 items
(3, 4, 1, 149.99, 149.99),
(3, 2, 1, 29.99, 29.99),
-- Order 4 items
(4, 1, 1, 1299.99, 1299.99),
-- Order 5 items (cancelled)
(5, 6, 1, 79.99, 79.99);

-- Update some timestamps for realistic data
UPDATE users SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '2 hours' WHERE id = 1;
UPDATE users SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '1 day' WHERE id = 2;
UPDATE users SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '3 days' WHERE id = 3;

UPDATE orders SET shipped_at = CURRENT_TIMESTAMP - INTERVAL '2 days' WHERE status = 'SHIPPED';
UPDATE orders SET delivered_at = CURRENT_TIMESTAMP - INTERVAL '5 days', shipped_at = CURRENT_TIMESTAMP - INTERVAL '7 days' WHERE status = 'DELIVERED';

-- Display summary
DO $$
DECLARE
user_count INTEGER;
    product_count INTEGER;
    order_count INTEGER;
BEGIN
SELECT COUNT(*) INTO user_count FROM users;
SELECT COUNT(*) INTO product_count FROM products;
SELECT COUNT(*) INTO order_count FROM orders;

RAISE NOTICE '========================================';
    RAISE NOTICE 'Database Initialization Complete!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Users created: %', user_count;
    RAISE NOTICE 'Products created: %', product_count;
    RAISE NOTICE 'Orders created: %', order_count;
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Default password for all users: password';
    RAISE NOTICE '========================================';
END $$;