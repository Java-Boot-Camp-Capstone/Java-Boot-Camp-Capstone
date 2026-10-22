INSERT INTO customer_interaction (
    interaction_id,
    customer_id,
    interaction_type,
    summary,
    correlation_id,
    created_at
)
SELECT
    gen_random_uuid(),
    customer_id,
    'CALL',
    'Discussed account needs',
    'lab-request-001',
    NOW()
FROM customer
WHERE public_id = 'CUS-1001';