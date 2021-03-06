/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Central tenant configuration management operations of the SP server.
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class TenantConfigurationManagement implements EnvironmentAware {

    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private final ConfigurableConversionService conversionService = new DefaultConversionService();

    private Environment environment;

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten
     * configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     * 
     * @param <T>
     *            the type of the configuration value
     * @param configurationKey
     *            the key of the configuration
     * @param propertyType
     *            the type of the configuration value, e.g. {@code String.class}
     *            , {@code Integer.class}, etc
     * @return the converted configuration value either from the tenant specific
     *         configuration stored or from the fallback default values or
     *         {@code null} in case key has not been configured and not default
     *         value exists
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */

    @Cacheable(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public <T> TenantConfigurationValue<T> getConfigurationValue(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {
        validateTenantConfigurationDataType(configurationKey, propertyType);

        final TenantConfiguration tenantConfiguration = tenantConfigurationRepository
                .findByKey(configurationKey.getKeyName());

        return buildTenantConfigurationValueByKey(configurationKey, propertyType, tenantConfiguration);
    }

    /**
     * Validates the data type of the tenant configuration. If it is possible to
     * cast to the given data type.
     * 
     * @param configurationKey
     *            the key
     * @param propertyType
     *            the class
     */
    protected <T> void validateTenantConfigurationDataType(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {
        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.",
                            configurationKey.getDataType(), propertyType));
        }
    }

    /**
     * Build the tenant configuration by the given key
     * 
     * @param configurationKey
     *            the key
     * @param propertyType
     *            the property type
     * @param tenantConfiguration
     *            the configuration
     * @return <null> if no default value is set and no database value available
     *         or returns the tenant configuration value
     */
    protected <T> TenantConfigurationValue<T> buildTenantConfigurationValueByKey(
            final TenantConfigurationKey configurationKey, final Class<T> propertyType,
            final TenantConfiguration tenantConfiguration) {
        if (tenantConfiguration != null) {
            return TenantConfigurationValue.<T> builder().isGlobal(false).createdBy(tenantConfiguration.getCreatedBy())
                    .createdAt(tenantConfiguration.getCreatedAt())
                    .lastModifiedAt(tenantConfiguration.getLastModifiedAt())
                    .lastModifiedBy(tenantConfiguration.getLastModifiedBy())
                    .value(conversionService.convert(tenantConfiguration.getValue(), propertyType)).build();

        } else if (configurationKey.getDefaultKeyName() != null) {

            return TenantConfigurationValue.<T> builder().isGlobal(true).createdBy(null).createdAt(null)
                    .lastModifiedAt(null).lastModifiedBy(null)
                    .value(getGlobalConfigurationValue(configurationKey, propertyType)).build();
        }
        return null;
    }

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten
     * configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     * 
     * @param configurationKey
     *            the key of the configuration
     * @return the converted configuration value either from the tenant specific
     *         configuration stored or from the fall back default values or
     *         {@code null} in case key has not been configured and not default
     *         value exists
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public TenantConfigurationValue<?> getConfigurationValue(final TenantConfigurationKey configurationKey) {
        return getConfigurationValue(configurationKey, configurationKey.getDataType());
    }

    /**
     * returns the global configuration property either defined in the property
     * file or an default value otherwise.
     * 
     * @param <T>
     *            the type of the configuration value
     * @param configurationKey
     *            the key of the configuration
     * @param propertyType
     *            the type of the configuration value, e.g. {@code String.class}
     *            , {@code Integer.class}, etc
     * @return the global configured value
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in the property
     *             file or the default value does not match the expected type
     *             and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public <T> T getGlobalConfigurationValue(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {

        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.",
                            configurationKey.getDataType(), propertyType));
        }

        final T valueInProperties = environment.getProperty(configurationKey.getDefaultKeyName(), propertyType);

        if (valueInProperties == null) {
            return conversionService.convert(configurationKey.getDefaultValue(), propertyType);
        }

        return valueInProperties;
    }

    /**
     * Adds or updates a specific configuration for a specific tenant.
     * 
     * 
     * @param configurationKey
     *            the key of the configuration
     * @param value
     *            the configuration value which will be written into the
     *            database.
     * @return the configuration value which was just written into the database.
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     */
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION)
    public <T> TenantConfigurationValue<T> addOrUpdateConfiguration(final TenantConfigurationKey configurationKey,
            final T value) {

        if (!configurationKey.getDataType().isAssignableFrom(value.getClass())) {
            throw new TenantConfigurationValidatorException(String.format(
                    "Cannot parse the value %s of type %s into the type %s defined by the configuration key.", value,
                    value.getClass(), configurationKey.getDataType()));
        }

        configurationKey.validate(applicationContext, value);

        TenantConfiguration tenantConfiguration = tenantConfigurationRepository
                .findByKey(configurationKey.getKeyName());

        if (tenantConfiguration == null) {
            tenantConfiguration = new TenantConfiguration(configurationKey.getKeyName(), value.toString());
        } else {
            tenantConfiguration.setValue(value.toString());
        }

        final TenantConfiguration updatedTenantConfiguration = tenantConfigurationRepository.save(tenantConfiguration);

        final Class<T> clazzT = (Class<T>) value.getClass();

        return TenantConfigurationValue.<T> builder().isGlobal(false)
                .createdBy(updatedTenantConfiguration.getCreatedBy())
                .createdAt(updatedTenantConfiguration.getCreatedAt())
                .lastModifiedAt(updatedTenantConfiguration.getLastModifiedAt())
                .lastModifiedBy(updatedTenantConfiguration.getLastModifiedBy())
                .value(conversionService.convert(updatedTenantConfiguration.getValue(), clazzT)).build();
    }

    /**
     * Deletes a specific configuration for the current tenant. Does nothing in
     * case there is no tenant specific configuration value.
     *
     * @param configurationKey
     *            the configuration key to be deleted
     */
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION)
    public void deleteConfiguration(final TenantConfigurationKey configurationKey) {
        tenantConfigurationRepository.deleteByKey(configurationKey.getKeyName());
    }

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
}
