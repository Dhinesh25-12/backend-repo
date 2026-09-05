-- V2: Seed data - roles, sample users, products, and domain sample data
-- Default password for all seeded users is: Password123!  (BCrypt hash below)

INSERT INTO role (name, description) VALUES
    ('ADMIN', 'Platform administrator'),
    ('AGENT', 'Insurance agent'),
    ('CLAIMS_OFFICER', 'Claims processing officer'),
    ('CUSTOMER', 'Insurance customer');

-- Password hash corresponds to plaintext: Password123!
INSERT INTO app_user (username, email, password_hash, first_name, last_name, phone, active) VALUES
    ('admin', 'admin@insurance-portal.com', '$2b$10$b2bkA/4aO9HfLJLjydDatelKICBISY9xyPltNKTcCFwPGTBnLjZtq', 'System', 'Admin', '+1-555-0100', TRUE),
    ('agent1', 'agent1@insurance-portal.com', '$2b$10$b2bkA/4aO9HfLJLjydDatelKICBISY9xyPltNKTcCFwPGTBnLjZtq', 'Alice', 'Agent', '+1-555-0101', TRUE),
    ('claims1', 'claims1@insurance-portal.com', '$2b$10$b2bkA/4aO9HfLJLjydDatelKICBISY9xyPltNKTcCFwPGTBnLjZtq', 'Chris', 'Officer', '+1-555-0102', TRUE),
    ('customer1', 'customer1@insurance-portal.com', '$2b$10$b2bkA/4aO9HfLJLjydDatelKICBISY9xyPltNKTcCFwPGTBnLjZtq', 'John', 'Doe', '+1-555-0103', TRUE),
    ('customer2', 'customer2@insurance-portal.com', '$2b$10$b2bkA/4aO9HfLJLjydDatelKICBISY9xyPltNKTcCFwPGTBnLjZtq', 'Jane', 'Smith', '+1-555-0104', TRUE);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u, role r WHERE u.username = 'admin' AND r.name = 'ADMIN';
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u, role r WHERE u.username = 'agent1' AND r.name = 'AGENT';
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u, role r WHERE u.username = 'claims1' AND r.name = 'CLAIMS_OFFICER';
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u, role r WHERE u.username = 'customer1' AND r.name = 'CUSTOMER';
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u, role r WHERE u.username = 'customer2' AND r.name = 'CUSTOMER';

INSERT INTO customer (user_id, date_of_birth, address, city, state, postal_code, kyc_id_type, kyc_id_number)
SELECT u.id, DATE '1988-04-12', '123 Main St', 'Springfield', 'IL', '62701', 'PASSPORT', 'P1234567'
FROM app_user u WHERE u.username = 'customer1';
INSERT INTO customer (user_id, date_of_birth, address, city, state, postal_code, kyc_id_type, kyc_id_number)
SELECT u.id, DATE '1992-09-23', '456 Oak Ave', 'Metropolis', 'NY', '10001', 'DRIVER_LICENSE', 'D7654321'
FROM app_user u WHERE u.username = 'customer2';

INSERT INTO product (name, category, description, coverage_amount, premium_amount, tenure_months, active) VALUES
    ('SecureHealth Basic', 'HEALTH', 'Basic individual health coverage', 500000, 8000, 12, TRUE),
    ('SecureHealth Family', 'HEALTH', 'Family floater health coverage', 1000000, 15000, 12, TRUE),
    ('AutoShield Standard', 'MOTOR', 'Comprehensive motor insurance', 300000, 6000, 12, TRUE),
    ('AutoShield Premium', 'MOTOR', 'Premium motor insurance with zero-dep', 500000, 9500, 12, TRUE),
    ('LifeSecure Term', 'LIFE', 'Term life insurance plan', 5000000, 12000, 240, TRUE),
    ('LifeSecure Endowment', 'LIFE', 'Endowment life insurance plan', 2000000, 20000, 180, TRUE),
    ('TravelSafe Domestic', 'TRAVEL', 'Domestic travel insurance', 100000, 1200, 1, TRUE),
    ('TravelSafe International', 'TRAVEL', 'International travel insurance', 300000, 3500, 1, TRUE);

-- Sample policy for customer1
INSERT INTO policy (policy_number, customer_id, product_id, agent_id, nominee_name, nominee_relationship, nominee_contact,
                     coverage_amount, premium_amount, start_date, end_date, status)
SELECT 'POL-2024-000001', c.id, p.id, a.id, 'Mary Doe', 'Spouse', '+1-555-0199',
       p.coverage_amount, p.premium_amount, DATE '2024-01-01', DATE '2024-12-31', 'ACTIVE'
FROM customer c
JOIN app_user u ON u.id = c.user_id AND u.username = 'customer1'
JOIN product p ON p.name = 'SecureHealth Basic'
JOIN app_user a ON a.username = 'agent1';

INSERT INTO payment (policy_id, customer_id, amount, payment_method, status, invoice_number, receipt_number)
SELECT pol.id, pol.customer_id, pol.premium_amount, 'CARD', 'SUCCESS', 'INV-2024-000001', 'RCPT-2024-000001'
FROM policy pol WHERE pol.policy_number = 'POL-2024-000001';

INSERT INTO claim (claim_number, policy_id, customer_id, incident_date, description, claim_amount, status)
SELECT 'CLM-2024-000001', pol.id, pol.customer_id, DATE '2024-03-15', 'Hospitalization for minor surgery', 45000, 'SUBMITTED'
FROM policy pol WHERE pol.policy_number = 'POL-2024-000001';
