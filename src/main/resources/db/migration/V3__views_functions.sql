-- V3: Views, PL/pgSQL functions and reporting SQL objects (Intermediate/Advanced SQL)
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
    u.first_name || ' ' || u.last_name AS customer_name,
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
    u.first_name || ' ' || u.last_name AS customer_name
FROM claim cl
JOIN policy pol ON pol.id = cl.policy_id
JOIN customer c ON c.id = cl.customer_id
JOIN app_user u ON u.id = c.user_id;

-- ---------------------------------------------------------------------------
-- VIEW: payment_rollup_view - monthly premium collection rollup
-- ---------------------------------------------------------------------------
CREATE VIEW payment_rollup_view AS
SELECT
    date_trunc('month', pay.payment_date) AS revenue_month,
    COUNT(*)                               AS payment_count,
    SUM(pay.amount)                        AS total_amount
FROM payment pay
WHERE pay.status = 'SUCCESS'
GROUP BY date_trunc('month', pay.payment_date)
ORDER BY revenue_month;

-- ---------------------------------------------------------------------------
-- FUNCTION: fn_generate_policy_number - sequence-friendly policy number generator
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_generate_policy_number()
RETURNS VARCHAR AS $$
DECLARE
    next_val BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1 INTO next_val FROM policy;
    RETURN 'POL-' || to_char(now(), 'YYYY') || '-' || LPAD(next_val::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- FUNCTION: fn_claims_settlement_ratio - claims settled / claims filed ratio
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_claims_settlement_ratio()
RETURNS NUMERIC AS $$
DECLARE
    total_claims BIGINT;
    settled_claims BIGINT;
BEGIN
    SELECT COUNT(*) INTO total_claims FROM claim;
    SELECT COUNT(*) INTO settled_claims FROM claim WHERE status = 'SETTLED';
    IF total_claims = 0 THEN
        RETURN 0;
    END IF;
    RETURN ROUND((settled_claims::NUMERIC / total_claims::NUMERIC) * 100, 2);
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- FUNCTION: fn_top_customers_by_premium - top N customers ranked by total premium paid
-- Demonstrates CTE + window function (RANK) usage inside a callable function.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_top_customers_by_premium(limit_count INTEGER DEFAULT 10)
RETURNS TABLE (
    customer_id BIGINT,
    customer_name TEXT,
    total_premium NUMERIC,
    customer_rank BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH premium_totals AS (
        SELECT
            c.id AS customer_id,
            u.first_name || ' ' || u.last_name AS customer_name,
            SUM(pay.amount) AS total_premium
        FROM payment pay
        JOIN customer c ON c.id = pay.customer_id
        JOIN app_user u ON u.id = c.user_id
        WHERE pay.status = 'SUCCESS'
        GROUP BY c.id, u.first_name, u.last_name
    ),
    ranked AS (
        SELECT
            pt.*,
            RANK() OVER (ORDER BY pt.total_premium DESC) AS customer_rank
        FROM premium_totals pt
    )
    SELECT ranked.customer_id, ranked.customer_name, ranked.total_premium, ranked.customer_rank
    FROM ranked
    ORDER BY ranked.customer_rank
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Partitioning concept demonstration.
-- The `payment` table (V1) is created as a regular table for cross-DB/test
-- portability (H2 test profile does not support native partitioning).
-- In a production PostgreSQL deployment the table would instead be declared as:
--
--   CREATE TABLE payment (... ) PARTITION BY RANGE (payment_date);
--   CREATE TABLE payment_2024 PARTITION OF payment
--       FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
--   CREATE TABLE payment_2025 PARTITION OF payment
--       FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
--
-- This range-partitioning-by-date strategy keeps historical payment/claim data
-- query-efficient (partition pruning) as the dataset grows year over year.
-- ---------------------------------------------------------------------------
