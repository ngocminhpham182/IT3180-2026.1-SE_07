-- =====================================================================
-- BlueMoon - V2__resident_request.sql   (chạy SAU V1__init_schema.sql)
-- US-05: cư dân gửi đề nghị chỉnh sửa thông tin nhân khẩu
-- US-06: cư dân gửi tờ khai tạm vắng, tạm trú để Ban quản trị duyệt
--
-- Đây là thay đổi DB DUY NHẤT của phần US-05/06: thêm một bảng, không
-- sửa bảng nào đã có. Hibernate (ddl-auto=update) chỉ tạo được bảng và
-- cột, KHÔNG tạo CHECK, partial index, expression index. Vì vậy phải chạy
-- file này bằng tay (hoặc qua Flyway) thay vì chỉ trông chờ vào entity.
-- =====================================================================

CREATE TABLE resident_request (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_type    VARCHAR(30) NOT NULL
                    CHECK (request_type IN ('PROFILE_UPDATE', 'TEMPORARY_ABSENCE', 'TEMPORARY_RESIDENCE')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),

    -- Hộ của người gửi tại thời điểm gửi
    household_id    BIGINT NOT NULL REFERENCES household (id) ON DELETE RESTRICT,
    -- Nhân khẩu liên quan. NULL với TEMPORARY_RESIDENCE (người khách chưa có hồ sơ)
    resident_id     BIGINT REFERENCES resident (id) ON DELETE RESTRICT,

    submitted_by    BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note            TEXT CHECK (note IS NULL OR char_length(note) <= 500),

    -- Nội dung đề nghị:
    --   PROFILE_UPDATE      {fullName, dateOfBirth, gender, identityNumber, phone, relationshipToHead}
    --   TEMPORARY_ABSENCE   {startDate, endDate}
    --   TEMPORARY_RESIDENCE {startDate, endDate, person: {...6 field như trên}}
    proposed        JSONB NOT NULL,
    -- Chỉ PROFILE_UPDATE: 6 field của nhân khẩu lúc gửi. "changes" được suy ra
    -- bằng cách so before_values với proposed, không lưu thành cột riêng.
    before_values   JSONB,

    -- Quyết định (NULL khi PENDING)
    decided_by      BIGINT REFERENCES app_user (id) ON DELETE RESTRICT,
    decided_at      TIMESTAMPTZ,
    decision_reason TEXT CHECK (decision_reason IS NULL OR char_length(decision_reason) <= 500),
    decision_note   TEXT CHECK (decision_note   IS NULL OR char_length(decision_note)   <= 500),

    -- Kết quả khi APPROVED
    result_resident_id BIGINT REFERENCES resident (id) ON DELETE RESTRICT,
    result_movement_id BIGINT REFERENCES resident_movement (id) ON DELETE RESTRICT,

    -- PENDING <=> chưa có thời điểm quyết định
    CONSTRAINT ck_rr_decided_consistent
        CHECK ((status = 'PENDING') = (decided_at IS NULL)),
    CONSTRAINT ck_rr_decider_required
        CHECK (status = 'PENDING' OR decided_by IS NOT NULL),
    -- Từ chối phải kèm lý do [US-05 AC4]
    CONSTRAINT ck_rr_reject_reason
        CHECK (status <> 'REJECTED' OR (decision_reason IS NOT NULL AND btrim(decision_reason) <> '')),
    -- Chỉ đề nghị đã duyệt mới có kết quả
    CONSTRAINT ck_rr_result_only_approved
        CHECK (status = 'APPROVED'
               OR (result_resident_id IS NULL AND result_movement_id IS NULL)),
    -- Sửa thông tin và tạm vắng bắt buộc nêu rõ nhân khẩu [US-06 AC3]
    CONSTRAINT ck_rr_resident_required
        CHECK (request_type = 'TEMPORARY_RESIDENCE' OR resident_id IS NOT NULL),
    CONSTRAINT ck_rr_guest_has_no_resident
        CHECK (request_type <> 'TEMPORARY_RESIDENCE' OR resident_id IS NULL),
    CONSTRAINT ck_rr_before_values
        CHECK ((request_type = 'PROFILE_UPDATE') = (before_values IS NOT NULL))
);

-- Mỗi nhân khẩu tối đa MỘT đề nghị sửa thông tin đang chờ
CREATE UNIQUE INDEX uq_rr_pending_profile
    ON resident_request (resident_id)
    WHERE status = 'PENDING' AND request_type = 'PROFILE_UPDATE';

-- Mỗi nhân khẩu tối đa MỘT tờ khai tạm vắng đang chờ
CREATE UNIQUE INDEX uq_rr_pending_absence
    ON resident_request (resident_id)
    WHERE status = 'PENDING' AND request_type = 'TEMPORARY_ABSENCE';

-- Một người khách (theo CCCD/CMND) tối đa MỘT tờ khai tạm trú đang chờ
CREATE UNIQUE INDEX uq_rr_pending_guest_identity
    ON resident_request ((proposed #>> '{person,identityNumber}'))
    WHERE status = 'PENDING'
      AND request_type = 'TEMPORARY_RESIDENCE'
      AND (proposed #>> '{person,identityNumber}') IS NOT NULL;

CREATE INDEX idx_rr_status_submitted ON resident_request (status, submitted_at);
CREATE INDEX idx_rr_submitter        ON resident_request (submitted_by, submitted_at DESC);
CREATE INDEX idx_rr_household        ON resident_request (household_id, status);
