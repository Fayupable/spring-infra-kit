-- ============================================================================
-- PostgreSQL Integration Demo - Database Initialization
-- ============================================================================

-- Create database if not exists
SELECT 'CREATE DATABASE postgres_integration_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'postgres_integration_db')\gexec

-- Connect to database \c postgres_integration_db

-- Enable UUID extension
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema
CREATE SCHEMA IF NOT EXISTS app_schema;

-- Set search path
SET
search_path TO app_schema;

-- ============================================================================
-- USERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS users
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    username
    VARCHAR
(
    50
) UNIQUE NOT NULL,
    email VARCHAR
(
    100
) UNIQUE NOT NULL,
    full_name VARCHAR
(
    100
),
    age INTEGER CHECK
(
    age
    >=
    0
    AND
    age
    <=
    150
),
    status VARCHAR
(
    20
) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
    );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- ============================================================================
-- PRODUCTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS products
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    200
) NOT NULL,
    description TEXT,
    price DECIMAL
(
    10,
    2
) NOT NULL CHECK
(
    price
    >=
    0
),
    stock_quantity INTEGER DEFAULT 0 CHECK
(
    stock_quantity
    >=
    0
),
    category VARCHAR
(
    50
),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);

-- ============================================================================
-- ORDERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS orders
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL
    REFERENCES
    users
(
    id
) ON DELETE CASCADE,
    order_number VARCHAR
(
    50
) UNIQUE NOT NULL,
    total_amount DECIMAL
(
    10,
    2
) NOT NULL CHECK
(
    total_amount
    >=
    0
),
    status VARCHAR
(
    20
) DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
  WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);

-- ============================================================================
-- ORDER ITEMS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS order_items
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    order_id
    BIGINT
    NOT
    NULL
    REFERENCES
    orders
(
    id
) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products
(
    id
)
  ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    unit_price DECIMAL
(
    10,
    2
) NOT NULL CHECK
(
    unit_price
    >=
    0
),
    total_price DECIMAL
(
    10,
    2
) NOT NULL CHECK
(
    total_price
    >=
    0
),
    created_at TIMESTAMP
  WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Function for updating updated_at timestamp
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- Apply triggers
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_products_updated_at ON products;
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE
    ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_orders_updated_at ON orders;
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE
    ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- SAMPLE DATA
-- ============================================================================

-- Insert users
INSERT INTO users (username, email, full_name, age, status)
VALUES ('john_doe', 'john.doe@example.com', 'John Doe', 30, 'ACTIVE'),
       ('jane_smith', 'jane.smith@example.com', 'Jane Smith', 25, 'ACTIVE'),
       ('bob_wilson', 'bob.wilson@example.com', 'Bob Wilson', 35, 'ACTIVE'),
       ('alice_brown', 'alice.brown@example.com', 'Alice Brown', 28, 'INACTIVE'),
       ('charlie_davis', 'charlie.davis@example.com', 'Charlie Davis', 32, 'ACTIVE') ON CONFLICT (username) DO NOTHING;

-- Insert products
INSERT INTO products (name, description, price, stock_quantity, category, is_active)
VALUES ('Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 50, 'Electronics', true),
       ('Wireless Mouse', 'Ergonomic wireless mouse', 29.99, 200, 'Accessories', true),
       ('USB-C Cable', '2m USB-C charging cable', 19.99, 500, 'Accessories', true),
       ('Mechanical Keyboard', 'RGB mechanical keyboard', 149.99, 75, 'Accessories', true),
       ('4K Monitor', '27-inch 4K IPS monitor', 449.99, 30, 'Electronics', true),
       ('Webcam HD', '1080p webcam', 79.99, 100, 'Electronics', true),
       ('Laptop Stand', 'Adjustable aluminum stand', 39.99, 150, 'Accessories', true),
       ('External SSD 1TB', 'Portable SSD', 129.99, 80, 'Storage', true),
       ('Headphones Pro', 'Noise-cancelling headphones', 299.99, 45, 'Audio', true),
       ('Phone Case', 'Protective phone case', 24.99, 300, 'Accessories', true) ON CONFLICT DO NOTHING;

-- Insert orders
INSERT INTO orders (user_id, order_number, total_amount, status, notes)
VALUES (1, 'ORD-2024-001', 1329.98, 'DELIVERED', 'First order'),
       (1, 'ORD-2024-002', 449.99, 'SHIPPED', 'Monitor order'),
       (2, 'ORD-2024-003', 179.98, 'PROCESSING', 'Accessories bundle'),
       (3, 'ORD-2024-004', 1299.99, 'PENDING', 'Laptop order'),
       (2, 'ORD-2024-005', 79.99, 'CANCELLED', 'Changed mind') ON CONFLICT (order_number) DO NOTHING;

-- Insert order items
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price)
VALUES (1, 1, 1, 1299.99, 1299.99),
       (1, 2, 1, 29.99, 29.99),
       (2, 5, 1, 449.99, 449.99),
       (3, 4, 1, 149.99, 149.99),
       (3, 2, 1, 29.99, 29.99),
       (4, 1, 1, 1299.99, 1299.99),
       (5, 6, 1, 79.99, 79.99) ON CONFLICT DO NOTHING;

\echo
'Database postgres_integration_db initialized successfully!';