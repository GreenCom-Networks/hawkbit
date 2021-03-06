/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup_;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.model.Target_;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * RolloutGroupManagement to control rollout groups. This service secures all
 * the functionality based on the {@link PreAuthorize} annotation on methods.
 */
@Validated
@Service
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public class RolloutGroupManagement {

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Retrieves a single {@link RolloutGroup} by its ID.
     * 
     * @param rolloutGroupId
     *            the ID of the rollout group to find
     * @return the found {@link RolloutGroup} by its ID or {@code null} if it
     *         does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public RolloutGroup findRolloutGroupById(final Long rolloutGroupId) {
        return rolloutGroupRepository.findOne(rolloutGroupId);
    }

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout}.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public Page<RolloutGroup> findRolloutGroupsByRolloutId(final Long rolloutId, final Pageable page) {
        return rolloutGroupRepository.findByRolloutId(rolloutId, page);
    }

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout} and the given {@link Specification}.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param specification
     *            the specification to filter the result set based on attributes
     *            of the {@link RolloutGroup}
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public Page<RolloutGroup> findRolloutGroupsByPredicate(final Rollout rollout,
            final Specification<RolloutGroup> specification, final Pageable page) {
        return rolloutGroupRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(RolloutGroup_.rollout), rollout),
                specification.toPredicate(root, query, criteriaBuilder)), page);
    }

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout} with the detailed status.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public Page<RolloutGroup> findAllRolloutGroupsWithDetailedStatus(final Long rolloutId, final Pageable page) {
        final Page<RolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId, page);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(rollout -> rollout.getId())
                .collect(Collectors.toList());
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(
                rolloutGroupIds);

        for (final RolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), rolloutGroup.getTotalTargets());
            rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return rolloutGroups;
    }

    /**
     * Get count of targets in different status in rollout group.
     *
     * @param rolloutGroupId
     *            rollout group id
     * @return rolloutGroup with details of targets count for different statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public RolloutGroup findRolloutGroupWithDetailedStatus(final Long rolloutGroupId) {
        final RolloutGroup rolloutGroup = findRolloutGroupById(rolloutGroupId);
        final List<TotalTargetCountActionStatus> rolloutStatusCountItems = actionRepository
                .getStatusCountByRolloutGroupId(rolloutGroupId);

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                rolloutGroup.getTotalTargets());
        rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        return rolloutGroup;

    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRolloutGroup(
            final List<Long> rolloutGroupIds) {
        final List<TotalTargetCountActionStatus> resultList = actionRepository
                .getStatusCountByRolloutGroupId(rolloutGroupIds);
        return resultList.stream().collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));
    }

    /**
     * Get targets of specified rollout group.
     * 
     * @param rolloutGroup
     *            rollout group
     * @param specification
     *            the specification for filtering the targets of a rollout group
     * @param page
     *            the page request to sort and limit the result
     * 
     * @return Page<Target> list of targets of a rollout group
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public Page<Target> findRolloutGroupTargets(final RolloutGroup rolloutGroup,
            final Specification<Target> specification, final Pageable page) {
        return targetRepository.findAll((root, query, criteriaBuilder) -> {
            final ListJoin<Target, RolloutTargetGroup> rolloutTargetJoin = root.join(Target_.rolloutTargetGroup);
            return criteriaBuilder.and(specification.toPredicate(root, query, criteriaBuilder),
                    criteriaBuilder.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
        }, page);
    }

    /**
     * Get targets of specified rollout group.
     * 
     * @param rolloutGroup
     *            rollout group
     * @param page
     *            the page request to sort and limit the result
     * 
     * @return Page<Target> list of targets of a rollout group
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public Page<Target> findRolloutGroupTargets(@NotNull final RolloutGroup rolloutGroup, final Pageable page) {
        if (isRolloutStatusReady(rolloutGroup)) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return targetRepository.findByRolloutTargetGroupRolloutGroupId(rolloutGroup.getId(), page);
        }
        return targetRepository.findByActionsRolloutGroup(rolloutGroup, page);
    }

    private static boolean isRolloutStatusReady(final RolloutGroup rolloutGroup) {
        return rolloutGroup != null && RolloutStatus.READY.equals(rolloutGroup.getRollout().getStatus());
    }

    /**
     * 
     * Find all targets with action status by rollout group id. The action
     * status might be {@code null} if for the target within the rollout no
     * actions as been created, e.g. the target already had assigned the same
     * distribution set we do not create an action for it but the target is in
     * the result list of the rollout-group.
     * 
     * @param pageRequest
     *            the page request to sort and limit the result
     * @param rolloutGroup
     *            rollout group
     * @return {@link TargetWithActionStatus} target with action status
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    public Page<TargetWithActionStatus> findAllTargetsWithActionStatus(final PageRequest pageRequest,
            @NotNull final RolloutGroup rolloutGroup) {

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);

        final Root<RolloutTargetGroup> targetRoot = query.distinct(true).from(RolloutTargetGroup.class);
        final Join<RolloutTargetGroup, Target> targetJoin = targetRoot.join(RolloutTargetGroup_.target);
        final ListJoin<RolloutTargetGroup, Action> actionJoin = targetRoot.join(RolloutTargetGroup_.actions,
                JoinType.LEFT);

        final Root<RolloutTargetGroup> countQueryFrom = countQuery.distinct(true).from(RolloutTargetGroup.class);
        countQueryFrom.join(RolloutTargetGroup_.target);
        countQueryFrom.join(RolloutTargetGroup_.actions, JoinType.LEFT);
        countQuery.select(cb.count(countQueryFrom))
                .where(cb.equal(countQueryFrom.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
        final Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetJoin, actionJoin.get(Action_.status))
                .where(cb.equal(targetRoot.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
        final List<TargetWithActionStatus> targetWithActionStatus = entityManager.createQuery(multiselect)
                .setFirstResult(pageRequest.getOffset()).setMaxResults(pageRequest.getPageSize()).getResultList()
                .stream().map(o -> new TargetWithActionStatus((Target) o[0], (Action.Status) o[1]))
                .collect(Collectors.toList());
        return new PageImpl<>(targetWithActionStatus, pageRequest, totalCount);
    }
}
