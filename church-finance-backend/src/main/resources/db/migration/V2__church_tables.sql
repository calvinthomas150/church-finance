CREATE TABLE church_finance.church(
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    added_by UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE church_finance.church_membership(
    id UUID PRIMARY KEY,
    church_id UUID NOT NULL,
    user_id UUID NOT NULL,
    added_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_church_membership_church FOREIGN KEY (church_id) REFERENCES church_finance.church(id),
    CONSTRAINT fk_church_membership_user FOREIGN KEY (user_id) REFERENCES church_finance.users(id)
);

CREATE TABLE church_finance.church_membership_roles(
    church_membership_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (church_membership_id, role),
    CONSTRAINT fk_church_membership_roles_membership FOREIGN KEY (church_membership_id) REFERENCES church_finance.church_membership(id)
);

CREATE TABLE church_finance.transaction_category(
    id UUID PRIMARY KEY,
    church_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    added_by UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_transaction_category_church FOREIGN KEY (church_id) REFERENCES church_finance.church(id)
);
