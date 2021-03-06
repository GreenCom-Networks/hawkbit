/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter software module")
public class RSQLSoftwareModuleFieldTest extends AbstractIntegrationTest {

    @Before
    public void setupBeforeTest() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", "agent-hub", ""));
        softwareManagement.createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", "aa", ""));
        softwareManagement.createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", "aa", ""));

        final SoftwareModule ah2 = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub2", "1.0.1", "agent-hub2", ""));

        final SoftwareModuleMetadata softwareModuleMetadata = new SoftwareModuleMetadata("metaKey", ah, "metaValue");
        softwareManagement.createSoftwareModuleMetadata(softwareModuleMetadata);

        final SoftwareModuleMetadata softwareModuleMetadata2 = new SoftwareModuleMetadata("metaKey", ah2, "value");
        softwareManagement.createSoftwareModuleMetadata(softwareModuleMetadata2);
    }

    @Test
    @Description("Test filter software module by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(SoftwareModuleFields.ID.name() + "==*", 4);
    }

    @Test
    @Description("Test filter software module by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==agent-hub", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==agent-hub*", 2);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "!=agent-hub*", 2);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "=in=(agent-hub,notexist)", 1);
        assertRSQLQuery(SoftwareModuleFields.NAME.name() + "=out=(agent-hub,notexist)", 3);
    }

    @Test
    @Description("Test filter software module by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "==agent-hub", 1);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "=in=(agent-hub,notexist)", 1);
        assertRSQLQuery(SoftwareModuleFields.DESCRIPTION.name() + "=out=(agent-hub,notexist)", 3);
    }

    @Test
    @Description("Test filter software module by version")
    public void testFilterByParameterVersion() {
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "==1.0.1", 2);
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "!=v1.0", 4);
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "=in=(1.0.1,1.0.2)", 2);
        assertRSQLQuery(SoftwareModuleFields.VERSION.name() + "=out=(1.0.1)", 2);
    }

    @Test
    @Description("Test filter software module by type")
    public void testFilterByType() {
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "==application", 2);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "=in=(application)", 2);
        assertRSQLQuery(SoftwareModuleFields.TYPE.name() + "=out=(application)", 2);
    }

    @Test
    @Description("Test filter software module by metadata")
    public void testFilterByMetadata() {
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey==noExist*", 0);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey=in=(metaValue,value)", 2);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(SoftwareModuleFields.METADATA.name() + ".notExist==metaValue", 0);

    }

    private void assertRSQLQuery(final String rsqlParam, final long excpectedEntity) {
        final Page<SoftwareModule> find = softwareManagement.findSoftwareModulesByPredicate(
                RSQLUtility.parse(rsqlParam, SoftwareModuleFields.class), new PageRequest(0, 100));
        final long countAll = find.getTotalElements();
        assertThat(find).isNotNull();
        assertThat(countAll).isEqualTo(excpectedEntity);
    }
}
