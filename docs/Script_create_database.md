```
CREATE TABLE household (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_code VARCHAR(30)  NOT NULL,               
    apartment_no   VARCHAR(20)  NOT NULL,                 
    floor          SMALLINT,                             
    area           NUMERIC(10, 2) not null CHECK (area > 0),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'INACTIVE')),   
    deleted_at     TIMESTAMPTZ,                      
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resident (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id         BIGINT NOT NULL REFERENCES household (id) ON DELETE RESTRICT,
    full_name            VARCHAR(150) NOT NULL,
    date_of_birth        DATE NOT NULL,                   
    gender               VARCHAR(10)
                         CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    identity_number      VARCHAR(30),
    phone                VARCHAR(20),
    relationship_to_head VARCHAR(50),
    is_head              BOOLEAN NOT NULL DEFAULT FALSE,
    residency_status     VARCHAR(30) NOT NULL DEFAULT 'RESIDING'
                         CHECK (residency_status IN (
                             'RESIDING', 'TEMPORARILY_ABSENT', 'TEMPORARY_RESIDENCE',
                             'MOVED_OUT', 'DECEASED')),  
    status_until         DATE,
    move_in_date         DATE,
    move_out_date        DATE,
    deleted_at           TIMESTAMPTZ,                     
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (move_out_date IS NULL OR move_in_date IS NULL OR move_out_date >= move_in_date)
);

CREATE TABLE app_role (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE app_user (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username           VARCHAR(100) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    full_name          VARCHAR(150) NOT NULL,             =
    email              VARCHAR(255) UNIQUE,
    phone              VARCHAR(20),
    resident_id        BIGINT UNIQUE REFERENCES resident (id) ON DELETE RESTRICT,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at      TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES app_role (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE resident_movement (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resident_id      BIGINT NOT NULL REFERENCES resident (id) ON DELETE RESTRICT,
    movement_type    VARCHAR(30) NOT NULL
                     CHECK (movement_type IN (
                         'MOVE_IN', 'MOVE_OUT', 'TRANSFER', 'BIRTH', 'DEATH',  
                         'TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE',
                         'RETURN')),  
    old_household_id BIGINT REFERENCES household (id) ON DELETE RESTRICT,
    new_household_id BIGINT REFERENCES household (id) ON DELETE RESTRICT,
    effective_date   DATE NOT NULL,
    start_date       DATE,                      -
    end_date         DATE,                      
    note             TEXT,                      
    recorded_by      BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),  
    CHECK (movement_type NOT IN ('TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE')
           OR start_date IS NOT NULL)
);


CREATE TABLE fee_category (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code               VARCHAR(30)  NOT NULL UNIQUE,
    name               VARCHAR(150) NOT NULL,
    description        TEXT,
    fee_kind           VARCHAR(20) NOT NULL
                       CHECK (fee_kind IN ('MANDATORY', 'VOLUNTARY')),
    calculation_method VARCHAR(20) NOT NULL
                       CHECK (calculation_method IN ('PER_AREA', 'FIXED', 'MANUAL')),
    unit_price         NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    billing_cycle      VARCHAR(20) NOT NULL DEFAULT 'MONTHLY'         
                       CHECK (billing_cycle IN ('MONTHLY', 'ONE_TIME')),
    due_day            SMALLINT CHECK (due_day BETWEEN 1 AND 28),      
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (calculation_method = 'MANUAL' OR unit_price > 0),
    CHECK (fee_kind = 'VOLUNTARY' OR due_day IS NOT NULL)
);

CREATE TABLE billing_period (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(30)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                 CHECK (status IN ('OPEN', 'CLOSED')),   
    generated_at TIMESTAMPTZ,
    generated_by BIGINT REFERENCES app_user (id) ON DELETE RESTRICT,
    closed_at    TIMESTAMPTZ,
    closed_by    BIGINT REFERENCES app_user (id) ON DELETE RESTRICT,
    CHECK (end_date >= start_date)
);

CREATE TABLE receivable (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id       BIGINT NOT NULL REFERENCES household (id)      ON DELETE RESTRICT,
    fee_category_id    BIGINT NOT NULL REFERENCES fee_category (id)   ON DELETE RESTRICT,
    billing_period_id  BIGINT NOT NULL REFERENCES billing_period (id) ON DELETE RESTRICT,
    quantity           NUMERIC(10, 2),                  
    unit_price_applied NUMERIC(14, 2) NOT NULL CHECK (unit_price_applied >= 0),
    amount_due         NUMERIC(14, 2) NOT NULL CHECK (amount_due >= 0),
    due_date           DATE NOT NULL,                  
    description        TEXT,
    created_by         BIGINT REFERENCES app_user (id) ON DELETE RESTRICT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT NOT NULL DEFAULT 0,     
    CONSTRAINT uq_receivable_period
        UNIQUE (household_id, fee_category_id, billing_period_id)   
);

CREATE TABLE resident_request (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_type    VARCHAR(30) NOT NULL
                    CHECK (request_type IN ('PROFILE_UPDATE', 'TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    household_id    BIGINT NOT NULL REFERENCES household (id) ON DELETE RESTRICT,
    resident_id     BIGINT REFERENCES resident (id) ON DELETE RESTRICT,
    submitted_by    BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note            TEXT CHECK (note IS NULL OR char_length(note) <= 500),
    proposed        JSONB NOT NULL,
    before_values   JSONB,
    decided_by      BIGINT REFERENCES app_user (id) ON DELETE RESTRICT,
    decided_at      TIMESTAMPTZ,
    decision_reason TEXT CHECK (decision_reason IS NULL OR char_length(decision_reason) <= 500),
    decision_note   TEXT CHECK (decision_note   IS NULL OR char_length(decision_note)   <= 500),
    result_resident_id BIGINT REFERENCES resident (id) ON DELETE RESTRICT,
    result_movement_id BIGINT REFERENCES resident_movement (id) ON DELETE RESTRICT,
    CONSTRAINT ck_rr_decided_consistent
        CHECK ((status = 'PENDING') = (decided_at IS NULL)),
    CONSTRAINT ck_rr_decider_required
        CHECK (status = 'PENDING' OR decided_by IS NOT NULL),
    CONSTRAINT ck_rr_reject_reason
        CHECK (status <> 'REJECTED' OR (decision_reason IS NOT NULL AND btrim(decision_reason) <> '')),
    CONSTRAINT ck_rr_result_only_approved
        CHECK (status = 'APPROVED'
               OR (result_resident_id IS NULL AND result_movement_id IS NULL)),
    CONSTRAINT ck_rr_resident_required
        CHECK (request_type = 'TEMPORARY_RESIDENCE' OR resident_id IS NOT NULL),
    CONSTRAINT ck_rr_guest_has_no_resident
        CHECK (request_type <> 'TEMPORARY_RESIDENCE' OR resident_id IS NULL),
    CONSTRAINT ck_rr_before_values
        CHECK ((request_type = 'PROFILE_UPDATE') = (before_values IS NOT NULL))
);

```