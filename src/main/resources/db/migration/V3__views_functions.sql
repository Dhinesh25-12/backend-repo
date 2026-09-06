-- V3: Views and SQL objects (MySQL compatible)
-- ======================================================================================

-- ---------------------------------------------------------------------------
-- VIEW: policy_summary_view - joins policy/customer/product for reporting
-- ---------------------------------------------------------------------------
CREATE VIEW policy_summary_view AS
SELECT
    pol.id                AS policy_id,
    pol.policy_number,
    pol.status             AS policy_status,
    pol.start_date,
    pol.end_date,
    pol.premium_amount,
    pol.coverage_amount,
    c.id                    AS customer_id,
    u.username              AS customer_username,
    CONCAT(u.first_name, ' ', u.last_name) AS customer_name,
    p.id                    AS product_id,
    p.name                  AS product_name,
    p.category              AS product_category
FROM policy pol
JOIN customer c ON c.id = pol.customer_id
JOIN app_user u ON u.id = c.user_id
JOIN product p ON p.id = pol.product_id;

-- ---------------------------------------------------------------------------
-- VIEW: claim_summary_view - claims joined with policy/customer for reporting
-- ---------------------------------------------------------------------------
CREATE VIEW claim_summary_view AS
SELECT
    cl.id            AS claim_id,
    cl.claim_number,
    cl.status         AS claim_status,
    cl.incident_date,
    cl.claim_amount,
    pol.id            AS policy_id,
    pol.policy_number,
    c.id              AS customer_id,
    CONCAT(u.first_name, ' ', u.last_name) AS customer_name
FROM claim cl
JOIN policy pol ON pol.id = cl.policy_id
JOIN customer c ON c.id = cl.customer_id
JOIN app_user u ON u.id = c.user_id;

-- ---------------------------------------------------------------------------
-- VIEW: payment_rollup_view - monthly premium collection rollup
-- MySQL: Use DATE_FORMAT instead of date_trunc for month-level grouping
-- ---------------------------------------------------------------------------
CREATE VIEW payment_rollup_view AS
SELECT
    DATE_FORMAT(pay.payment_date, '%Y-%m-01') AS revenue_month,
    COUNT(*) AS payment_count,
    SUM(pay.amount) AS total_amount
FROM payment pay
WHERE pay.status = 'SUCCESS'
GROUP BY DATE_FORMAT(pay.payment_date, '%Y-%m-01')
ORDER BY revenue_month;

-- ---------------------------------------------------------------------------
-- NOTE on Functions and Stored Procedures:
-- The original PostgreSQL functions used PL/pgSQL which is not available in MySQL.
-- For MySQL, equivalent functionality can be implemented using:
-- 1. MySQL UDFs (User Defined Functions) with SQL syntax
-- 2. Stored Procedures
-- 3. Application-layer logic (recommended for complex operations)
--
-- These views provide the core reporting functionality needed for the insurance portal.
-- Policy number and claim number generation is handled at the application layer
-- to avoid race conditions and to support UUID-based identifiers.
-- ---------------------------------------------------------------------------
