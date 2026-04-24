-- ============================================================
--  ReNu Tech — MySQL Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS renutech;
USE renutech;

-- ── Users (customers + staff + admin) ───────────────────────
CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(100)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    role       ENUM('customer','staff','admin') DEFAULT 'customer',
    status     ENUM('active','suspended')       DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Products ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    category        ENUM('iPhone','MacBook','Apple Watch') NOT NULL,
    condition_grade ENUM('Like New','Good Condition','Fair Condition') NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    stock           INT DEFAULT 0,
    description     TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Orders ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    order_ref        VARCHAR(20)  NOT NULL UNIQUE,
    user_id          INT,
    customer_name    VARCHAR(100),
    customer_email   VARCHAR(100),
    address          TEXT,
    total            DECIMAL(10,2),
    status           ENUM('pending','shipped','delivered','cancelled') DEFAULT 'pending',
    tracking_number  VARCHAR(100),
    order_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ── Order Items ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT NOT NULL,
    product_id   INT,
    product_name VARCHAR(200),
    quantity     INT DEFAULT 1,
    price        DECIMAL(10,2),
    FOREIGN KEY (order_id)   REFERENCES orders(id)   ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

-- ── Restock / Purchase Orders ────────────────────────────────
CREATE TABLE IF NOT EXISTS restock_orders (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    po_ref        VARCHAR(20)  NOT NULL UNIQUE,
    supplier      VARCHAR(200),
    product_name  VARCHAR(200),
    product_id    INT,
    quantity      INT,
    unit_cost     DECIMAL(10,2),
    expected_date DATE,
    status        ENUM('ordered','received','cancelled') DEFAULT 'ordered',
    notes         TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

-- ── Contact Messages ────────────────────────────────
CREATE TABLE contact_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    subject VARCHAR(255),
    message TEXT,
    is_read TINYINT(1) DEFAULT 0,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
--  Seed Data
-- ============================================================

-- Admin + staff accounts (password stored as plaintext for demo)
INSERT INTO users (name, email, password, role) VALUES
    ('Admin',       'admin@renutech.com',  'admin123',  'admin'),
    ('Alex Nguyen',  'alex@renutech.com',   'staff123',  'staff'),
    ('Jordan Lee',   'jordan@renutech.com', 'staff123',  'staff');

-- Customer accounts
INSERT INTO users (name, email, password, role) VALUES
    ('Sarah Kim',    'sarah@email.com',  'pass123', 'customer'),
    ('Marcus Webb',  'marcus@email.com', 'pass123', 'customer'),
    ('Priya Nair',   'priya@email.com',  'pass123', 'customer'),
    ('James Okoye',  'james@email.com',  'pass123', 'customer'),
    ('Leila Hassan', 'leila@email.com',  'pass123', 'customer'),
    ('Tom Bradley',  'tom@email.com',    'pass123', 'customer');

-- Products
INSERT INTO products (name, category, condition_grade, price, stock, description) VALUES
    ('iPhone 16 Pro — Neutral Titanium',    'iPhone',      'Like New',       699.00, 4,  'Pristine condition, all accessories included.'),
    ('iPhone 15 Pro — Black Titanium',      'iPhone',      'Good Condition', 489.00, 7,  'Minor surface scratches, battery at 92%.'),
    ('iPhone 17 — Blue',                    'iPhone',      'Fair Condition', 749.00, 2,  'Visible wear, fully functional.'),
    ('MacBook Pro M4 — Space Black 14"',    'MacBook',     'Like New',       1299.00, 3, 'Factory reset, no marks.'),
    ('MacBook Air M4 — Midnight 13"',       'MacBook',     'Good Condition', 749.00, 1,  'Light use, keyboard in great shape.'),
    ('MacBook Pro M3 — Silver 14"',         'MacBook',     'Like New',       999.00, 5,  'Excellent condition, includes charger.'),
    ('Apple Watch Ultra 3 — Natural',       'Apple Watch', 'Like New',       399.00, 0,  'Out of stock — restock expected soon.'),
    ('Apple Watch SE 2nd Gen — Midnight',   'Apple Watch', 'Good Condition', 169.00, 6,  'Minimal wear, band included.'),
    ('Apple Watch S11 — Midnight',          'Apple Watch', 'Good Condition', 269.00, 3,  'Good condition, ceramic back intact.');

-- Sample orders
INSERT INTO orders (order_ref, user_id, customer_name, customer_email, address, total, status, tracking_number) VALUES
    ('#RT-1041', 4, 'Sarah Kim',    'sarah@email.com',  '12 Oak St, Austin TX',       699.00,  'pending',   ''),
    ('#RT-1040', 5, 'Marcus Webb',  'marcus@email.com', '88 Pine Ave, Denver CO',     1299.00, 'shipped',   'UPS-928374651'),
    ('#RT-1039', 6, 'Priya Nair',   'priya@email.com',  '9 Elm Rd, Seattle WA',       399.00,  'pending',   ''),
    ('#RT-1038', 7, 'James Okoye',  'james@email.com',  '45 Maple Dr, NYC NY',        749.00,  'delivered', 'FEDEX-11293847'),
    ('#RT-1037', 8, 'Leila Hassan', 'leila@email.com',  '7 Cedar Ln, Miami FL',       489.00,  'pending',   ''),
    ('#RT-1036', 9, 'Tom Bradley',  'tom@email.com',    '33 Birch St, Chicago IL',    999.00,  'shipped',   'UPS-761923847');

-- Order items (one item per order for simplicity)
INSERT INTO order_items (order_id, product_id, product_name, quantity, price) VALUES
    (1, 1, 'iPhone 16 Pro — Neutral Titanium',  1, 699.00),
    (2, 4, 'MacBook Pro M4 — Space Black 14"',  1, 1299.00),
    (3, 7, 'Apple Watch Ultra 3 — Natural',     1, 399.00),
    (4, 5, 'MacBook Air M4 — Midnight 13"',     1, 749.00),
    (5, 2, 'iPhone 15 Pro — Black Titanium',    1, 489.00),
    (6, 6, 'MacBook Pro M3 — Silver 14"',       1, 999.00);

-- Restock orders
INSERT INTO restock_orders (po_ref, supplier, product_name, quantity, unit_cost, expected_date, status, notes) VALUES
    ('#PO-001', 'Apple Resellers Inc.', 'iPhone 16 Pro — Neutral Titanium',  10, 550.00, '2026-04-15', 'ordered',   ''),
    ('#PO-002', 'MacSource Co.',        'MacBook Pro M4 — Space Black 14"',  5,  980.00, '2026-04-12', 'received',  'All units in good condition'),
    ('#PO-003', 'WatchWorld Ltd.',      'Apple Watch Ultra 3 — Natural',     8,  310.00, '2026-04-20', 'ordered',   ''),
    ('#PO-004', 'Apple Resellers Inc.', 'iPhone 15 Pro — Black Titanium',    6,  380.00, '2026-04-08', 'cancelled', 'Supplier unavailable');
