ALTER TABLE church_finance.transaction_category
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE church_finance.transaction_category
    DROP CONSTRAINT transaction_category_church_name_unique;

ALTER TABLE church_finance.transaction_category
    ADD CONSTRAINT transaction_category_church_name_type_unique
        UNIQUE (church_id, name, transaction_type);
