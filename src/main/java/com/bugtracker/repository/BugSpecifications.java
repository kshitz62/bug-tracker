package com.bugtracker.repository;

import com.bugtracker.dto.BugFilterRequest;
import com.bugtracker.entity.Bug;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the dynamic {@link Specification} used by bug search, filtering,
 * sorting and pagination.
 */
public final class BugSpecifications {

    private BugSpecifications() {
    }

    public static Specification<Bug> withFilter(BugFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("reporter", JoinType.LEFT);
                root.fetch("assignedDeveloper", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            String search = filter.getSearch();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("bugCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
                ));
            }
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getSeverity() != null) {
                predicates.add(criteriaBuilder.equal(root.get("severity"), filter.getSeverity()));
            }
            if (filter.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getAssigneeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedDeveloper").get("id"), filter.getAssigneeId()));
            }
            if (filter.getReporterId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("reporter").get("id"), filter.getReporterId()));
            }
            if (Boolean.TRUE.equals(filter.getUnassigned())) {
                predicates.add(criteriaBuilder.isNull(root.get("assignedDeveloper")));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
