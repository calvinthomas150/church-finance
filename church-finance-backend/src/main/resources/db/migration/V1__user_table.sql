CREATE TABLE church_finance.users(
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    added_by UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    user_status VARCHAR(20) NOT NULL,
    is_system_admin BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT users_email_unique UNIQUE (email)
);
