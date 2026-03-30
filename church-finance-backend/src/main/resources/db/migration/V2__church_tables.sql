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
    CONSTRAINT fk_church_membership_user FOREIGN KEY (user_id) REFERENCES church_finance.users(id),
    CONSTRAINT church_membership_church_user_unique UNIQUE (church_id, user_id)
);

CREATE TABLE church_finance.church_membership_roles(
    church_membership_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (church_membership_id, role),
    CONSTRAINT fk_church_membership_roles_membership FOREIGN KEY (church_membership_id) REFERENCES church_finance.church_membership(id) ON DELETE CASCADE
);

CREATE TABLE church_finance.transaction_category(
    id UUID PRIMARY KEY,
    church_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    added_by UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_transaction_category_church FOREIGN KEY (church_id) REFERENCES church_finance.church(id),
    CONSTRAINT transaction_category_church_name_unique UNIQUE (church_id, name)
);

CREATE INDEX idx_church_membership_church_id ON church_finance.church_membership(church_id);
CREATE INDEX idx_church_membership_user_id ON church_finance.church_membership(user_id);
CREATE INDEX idx_transaction_category_church_id ON church_finance.transaction_category(church_id);
