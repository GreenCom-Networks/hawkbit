/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Meta data for {@link DistributionSet}.
 *
 */
@IdClass(DsMetadataCompositeKey.class)
@Entity
@Table(name = "sp_ds_metadata")
public class DistributionSetMetadata extends MetaData {
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ds_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_metadata_ds"))
    private DistributionSet distributionSet;

    public DistributionSetMetadata() {
        // default public constructor for JPA
    }

    public DistributionSetMetadata(final String key, final DistributionSet distributionSet, final String value) {
        super(key, value);
        this.distributionSet = distributionSet;
    }

    public DsMetadataCompositeKey getId() {
        return new DsMetadataCompositeKey(distributionSet, getKey());
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = distributionSet;
    }

    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((distributionSet == null) ? 0 : distributionSet.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final DistributionSetMetadata other = (DistributionSetMetadata) obj;
        if (distributionSet == null) {
            if (other.distributionSet != null) {
                return false;
            }
        } else if (!distributionSet.equals(other.distributionSet)) {
            return false;
        }
        return true;
    }
}
