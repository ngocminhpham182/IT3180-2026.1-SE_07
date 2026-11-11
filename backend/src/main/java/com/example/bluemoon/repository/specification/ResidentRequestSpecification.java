package com.example.bluemoon.repository.specification;

import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.ResidentRequest;
import com.example.bluemoon.model.enums.ResidentRequestStatus;
import com.example.bluemoon.model.enums.ResidentRequestType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public class ResidentRequestSpecification {

    private ResidentRequestSpecification() {
    }

    public static Specification<ResidentRequest> submittedByUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("submittedBy").get("id"), userId);
        };
    }

    public static Specification<ResidentRequest> hasStatusIn(Collection<ResidentRequestStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<ResidentRequest> hasType(ResidentRequestType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<ResidentRequest> hasHouseholdId(Long householdId) {
        return (root, query, cb) -> {
            if (householdId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("household").get("id"), householdId);
        };
    }

    public static Specification<ResidentRequest> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String escaped = keyword.trim().toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            Join<ResidentRequest, Household> household = root.join("household");

            return cb.or(
                    cb.like(cb.lower(household.get("householdCode")), pattern, '\\'),
                    cb.like(cb.lower(household.get("apartmentNo")), pattern, '\\')
            );
        };
    }
}
