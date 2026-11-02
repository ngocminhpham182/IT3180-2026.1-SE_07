package com.example.bluemoon.repository.specification;

import com.example.bluemoon.model.AppRole;
import com.example.bluemoon.model.AppUser;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<AppUser> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {

            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("username")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("fullName")),
                            pattern
                    )
            );
        };
    }

    public static Specification<AppUser> hasRole(String roleCode) {
        return (root, query, criteriaBuilder) -> {

            if (roleCode == null || roleCode.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            Join<AppUser, AppRole> roles = root.join("roles");

            return criteriaBuilder.equal(
                    roles.get("code"),
                    roleCode
            );
        };
    }

    public static Specification<AppUser> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {

            if (active == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("active"),
                    active
            );
        };
    }
}
