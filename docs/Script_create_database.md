```sql

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Hàm bất biến phục vụ index tìm kiếm tiếng Việt không dấu (ResidentRepository)
CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

-- Hàm trigger chặn UPDATE/DELETE cho bảng append-only (resident_movement)
CREATE OR REPLACE FUNCTION forbid_modify() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% on % is not allowed (append-only table)', TG_OP, TG_TABLE_NAME;
END $$;

-- Hàm trigger tự động cập nhật updated_at
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END $$;


-- =====================================================================
-- 1. HỘ GIA ĐÌNH & NHÂN KHẨU (US-01, 02, 03, 04, 05, 06, 13, 14)
-- =====================================================================

CREATE TABLE household (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_code VARCHAR(30)  NOT NULL,
    apartment_no   VARCHAR(20)  NOT NULL,
    floor          SMALLINT     CHECK (floor IS NULL OR (floor >= 1 AND floor <= 60)),
    area           NUMERIC(10, 2) CHECK (area IS NULL OR area > 0),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'INACTIVE')),
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_household_code
    ON household (household_code) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_household_apartment
    ON household (apartment_no)   WHERE deleted_at IS NULL;
CREATE INDEX idx_household_code_trgm
    ON household USING gin (lower(household_code) gin_trgm_ops);
CREATE INDEX idx_household_apartment_trgm
    ON household USING gin (lower(apartment_no) gin_trgm_ops);


CREATE TABLE resident (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id         BIGINT       NOT NULL REFERENCES household (id) ON DELETE RESTRICT,
    full_name            VARCHAR(150) NOT NULL,
    date_of_birth        DATE         NOT NULL,
    gender               VARCHAR(10)  CHECK (gender IN ('MALE', 'FEMALE')),
    identity_number      VARCHAR(30),
    phone                VARCHAR(20),
    relationship_to_head VARCHAR(50),
    is_head              BOOLEAN      NOT NULL DEFAULT FALSE,
    residency_status     VARCHAR(30)  NOT NULL DEFAULT 'RESIDING'
                         CHECK (residency_status IN (
                             'RESIDING', 'TEMPORARILY_ABSENT', 'TEMPORARY_RESIDENCE',
                             'MOVED_OUT', 'DECEASED')),
    status_until         DATE,
    move_in_date         DATE,
    move_out_date        DATE,
    deleted_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (move_out_date IS NULL OR move_in_date IS NULL OR move_out_date >= move_in_date)
);

CREATE UNIQUE INDEX uq_resident_identity
    ON resident (identity_number)
    WHERE identity_number IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_resident_household_head
    ON resident (household_id) WHERE is_head AND deleted_at IS NULL;
CREATE INDEX idx_resident_household ON resident (household_id);
CREATE INDEX idx_resident_name_trgm
    ON resident USING gin (f_unaccent(lower(full_name)) gin_trgm_ops);


-- =====================================================================
-- 2. TÀI KHOẢN & PHÂN QUYỀN (US-08, 09, 10)
-- =====================================================================

CREATE TABLE app_role (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO app_role (code, name) VALUES
    ('BQT',        'Ban quản trị'),
    ('ACCOUNTANT', 'Kế toán'),
    ('RESIDENT',   'Cư dân'),
    ('AUTHORITY',  'Cơ quan chức năng');


CREATE TABLE app_user (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username             VARCHAR(100) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    full_name            VARCHAR(150) NOT NULL,
    email                VARCHAR(255) UNIQUE,
    phone                VARCHAR(20),
    resident_id          BIGINT UNIQUE REFERENCES resident (id) ON DELETE RESTRICT,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_count   INT          NOT NULL DEFAULT 0 CHECK (failed_login_count >= 0),
    locked_until         TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    password_changed_at  TIMESTAMPTZ,
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_app_user_username ON app_user (lower(username));


CREATE TABLE user_role (
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES app_role (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_role_role ON user_role (role_id);


-- =====================================================================
-- 3. BIẾN ĐỘNG NHÂN KHẨU & ĐỀ NGHỊ CƯ DÂN (US-01, 02, 03, 05, 06)
-- =====================================================================

CREATE TABLE resident_movement (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resident_id      BIGINT      NOT NULL REFERENCES resident (id) ON DELETE RESTRICT,
    movement_type    VARCHAR(30) NOT NULL
                     CHECK (movement_type IN (
                         'MOVE_IN', 'MOVE_OUT', 'TRANSFER', 'BIRTH', 'DEATH',
                         'TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE', 'RETURN')),
    old_household_id BIGINT      REFERENCES household (id) ON DELETE RESTRICT,
    new_household_id BIGINT      REFERENCES household (id) ON DELETE RESTRICT,
    effective_date   DATE        NOT NULL,
    start_date       DATE,
    end_date         DATE,
    note             TEXT,
    recorded_by      BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_initial       BOOLEAN     NOT NULL DEFAULT FALSE,
    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CHECK (movement_type NOT IN ('TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE')
           OR start_date IS NOT NULL)
);

CREATE INDEX idx_movement_resident_date
    ON resident_movement (resident_id, effective_date DESC);

CREATE TRIGGER trg_movement_append_only
    BEFORE UPDATE OR DELETE ON resident_movement
    FOR EACH ROW EXECUTE FUNCTION forbid_modify();


CREATE TABLE resident_request (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_type       VARCHAR(30) NOT NULL
                       CHECK (request_type IN ('PROFILE_UPDATE', 'TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE')),
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    household_id       BIGINT      NOT NULL REFERENCES household (id) ON DELETE RESTRICT,
    resident_id        BIGINT      REFERENCES resident (id) ON DELETE RESTRICT,
    submitted_by       BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    submitted_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note               TEXT        CHECK (note IS NULL OR char_length(note) <= 500),
    proposed           JSONB       NOT NULL,
    before_values      JSONB,
    decided_by         BIGINT      REFERENCES app_user (id) ON DELETE RESTRICT,
    decided_at         TIMESTAMPTZ,
    decision_reason    TEXT        CHECK (decision_reason IS NULL OR char_length(decision_reason) <= 500),
    decision_note      TEXT        CHECK (decision_note   IS NULL OR char_length(decision_note)   <= 500),
    result_resident_id BIGINT      REFERENCES resident (id) ON DELETE RESTRICT,
    result_movement_id BIGINT      REFERENCES resident_movement (id) ON DELETE RESTRICT,

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

CREATE UNIQUE INDEX uq_rr_pending_profile
    ON resident_request (resident_id)
    WHERE status = 'PENDING' AND request_type = 'PROFILE_UPDATE';

CREATE UNIQUE INDEX uq_rr_pending_absence
    ON resident_request (resident_id)
    WHERE status = 'PENDING' AND request_type = 'TEMPORARY_ABSENCE';

CREATE UNIQUE INDEX uq_rr_pending_guest_identity
    ON resident_request ((proposed #>> '{person,identityNumber}'))
    WHERE status = 'PENDING'
      AND request_type = 'TEMPORARY_RESIDENCE'
      AND (proposed #>> '{person,identityNumber}') IS NOT NULL;

CREATE INDEX idx_rr_status_submitted ON resident_request (status, submitted_at);
CREATE INDEX idx_rr_submitter        ON resident_request (submitted_by, submitted_at DESC);
CREATE INDEX idx_rr_household        ON resident_request (household_id, status);


-- =====================================================================
-- 4. KHOẢN THU, KỲ THU & CÔNG NỢ (US-12, 13, 14)
-- =====================================================================

CREATE TABLE fee_category (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code               VARCHAR(30)    NOT NULL,
    name               VARCHAR(150)   NOT NULL,
    description        VARCHAR(1000),
    fee_kind           VARCHAR(20)    NOT NULL
                       CHECK (fee_kind IN ('MANDATORY', 'VOLUNTARY')),
    calculation_method VARCHAR(20)    NOT NULL
                       CHECK (calculation_method IN ('PER_AREA', 'FIXED', 'MANUAL')),
    unit_price         NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    billing_cycle      VARCHAR(20)    NOT NULL DEFAULT 'MONTHLY'
                       CHECK (billing_cycle IN ('MONTHLY', 'ONE_TIME')),
    due_day            INTEGER        CHECK (due_day BETWEEN 1 AND 28),
    is_active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_fee_category_code UNIQUE (code),
    CHECK (calculation_method = 'MANUAL' OR unit_price > 0),
    CHECK (fee_kind = 'VOLUNTARY' OR due_day IS NOT NULL)
);


CREATE TABLE billing_period (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(30)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                 CHECK (status IN ('OPEN', 'CLOSED')),
    generated_at TIMESTAMPTZ,
    generated_by BIGINT,
    closed_at    TIMESTAMPTZ,
    closed_by    BIGINT,
    CONSTRAINT billing_period_generated_by_fkey
        FOREIGN KEY (generated_by) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT billing_period_closed_by_fkey
        FOREIGN KEY (closed_by)    REFERENCES app_user (id) ON DELETE RESTRICT,
    CHECK (end_date >= start_date)
);


CREATE TABLE receivable (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id       BIGINT         NOT NULL REFERENCES household (id)      ON DELETE RESTRICT,
    fee_category_id    BIGINT         NOT NULL REFERENCES fee_category (id)   ON DELETE RESTRICT,
    billing_period_id  BIGINT         NOT NULL REFERENCES billing_period (id) ON DELETE RESTRICT,
    quantity           NUMERIC(10, 2),
    unit_price_applied NUMERIC(14, 2) NOT NULL CHECK (unit_price_applied >= 0),
    amount_due         NUMERIC(14, 2) NOT NULL CHECK (amount_due >= 0),
    due_date           DATE           NOT NULL,
    description        TEXT,
    created_by         BIGINT         REFERENCES app_user (id) ON DELETE RESTRICT,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_receivable_period
        UNIQUE (household_id, fee_category_id, billing_period_id)
);

CREATE INDEX idx_receivable_household ON receivable (household_id);
CREATE INDEX idx_receivable_period    ON receivable (billing_period_id);


-- =====================================================================
-- 5. TRIGGER CẬP NHẬT updated_at
-- =====================================================================

CREATE TRIGGER trg_household_upd    BEFORE UPDATE ON household
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_resident_upd     BEFORE UPDATE ON resident
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_app_user_upd     BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_fee_category_upd BEFORE UPDATE ON fee_category
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```
