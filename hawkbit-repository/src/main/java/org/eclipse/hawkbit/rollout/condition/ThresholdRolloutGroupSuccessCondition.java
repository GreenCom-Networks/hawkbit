/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rollout.condition;

import org.eclipse.hawkbit.repository.ActionRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component("thresholdRolloutGroupSuccessCondition")
public class ThresholdRolloutGroupSuccessCondition implements RolloutGroupConditionEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThresholdRolloutGroupSuccessCondition.class);

    @Autowired
    private ActionRepository actionRepository;

    @Override
    public boolean eval(final Rollout rollout, final RolloutGroup rolloutGroup, final String expression) {
        final Long totalGroup = rolloutGroup.getTotalTargets();
        final Long finished = this.actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(rollout.getId(),
                rolloutGroup.getId(), Action.Status.FINISHED);
        try {
            final Integer threshold = Integer.valueOf(expression);

            if (totalGroup == 0) {
                // in case e.g. targets has been deleted we don't have any
                // actions
                // left for this group, so the group is finished
                return true;
            }
            // calculate threshold
            return ((float) finished / (float) totalGroup) >= ((float) threshold / 100F);
        } catch (final NumberFormatException e) {
            LOGGER.error("Cannot evaluate condition expression " + expression, e);
            return false;
        }
    }

}
