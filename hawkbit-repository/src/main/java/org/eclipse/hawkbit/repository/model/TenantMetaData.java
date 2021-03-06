/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Tenant entity with meta data that is configured globally for the entire
 * tenant. This entity is not tenant aware to allow the system to access it
 * through the {@link EntityManager} even before the actual tenant exists.
 *
 * Entities owned by the tenant are based on {@link TenantAwareBaseEntity}.
 *
 */
@Table(name = "sp_tenant", indexes = {
        @Index(name = "sp_idx_tenant_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "tenant" }, name = "uk_tenantmd_tenant") })
@Entity
public class TenantMetaData extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "tenant", nullable = false, length = 40)
    private String tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_ds_type", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_tenant_md_default_ds_type"))
    private DistributionSetType defaultDsType;

    /**
     * Default constructor needed for JPA entities.
     */
    public TenantMetaData() {
        // Default constructor needed for JPA entities.
    }

    /**
     * Standard constructor.
     *
     * @param defaultDsType
     *            of this tenant
     * @param tenant
     */
    public TenantMetaData(final DistributionSetType defaultDsType, final String tenant) {
        super();
        this.defaultDsType = defaultDsType;
        this.tenant = tenant;
    }

    public DistributionSetType getDefaultDsType() {
        return defaultDsType;
    }

    public void setDefaultDsType(final DistributionSetType defaultDsType) {
        this.defaultDsType = defaultDsType;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof TenantMetaData)) {
            return false;
        }

        return true;
    }
}
