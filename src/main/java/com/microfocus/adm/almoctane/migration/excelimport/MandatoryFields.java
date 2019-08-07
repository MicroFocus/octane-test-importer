/*
 * Copyright 2019 EntIT Software LLC, a Micro Focus company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microfocus.adm.almoctane.migration.excelimport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Here are all the mandatory fields necessary for migration.
 * These fields must be in the excel file.
 */
public enum MandatoryFields {
    NAME("name"),
    TYPE("type"),
    OWNER("owner"),
    PHASE("phase"),
    USER_TAG("user_tags"),
    DESIGNER("designer"),
    STEP_TYPE("step_type"),
    UNIQUE_ID("unique_id"),
    TEST_TYPE("test_type"),
    DESCRIPTION("description"),
    PRODUCT_AREAS("product_areas"),
    COVERED_CONTENT("covered_content"),
    STEP_DESCRIPTION("step_description"),
    ESTIMATED_DURATION("estimated_duration");

    private final String mandatoryFieldsName;

    MandatoryFields(String mandatoryFieldsName) {
        this.mandatoryFieldsName = mandatoryFieldsName;
    }

    /**
     * @return the name of the mandatory field
     */
    String fieldName() {
        return mandatoryFieldsName;
    }

    /**
     * @return a list with all the mandatory fields
     */
    public static List<String> getMandatoryFieldsList() {
        return new ArrayList<>(Arrays.asList(
                MandatoryFields.NAME.fieldName(),
                MandatoryFields.TYPE.fieldName(),
                MandatoryFields.PHASE.fieldName(),
                MandatoryFields.OWNER.fieldName(),
                MandatoryFields.DESIGNER.fieldName(),
                MandatoryFields.USER_TAG.fieldName(),
                MandatoryFields.STEP_TYPE.fieldName(),
                MandatoryFields.UNIQUE_ID.fieldName(),
                MandatoryFields.TEST_TYPE.fieldName(),
                MandatoryFields.DESCRIPTION.fieldName(),
                MandatoryFields.PRODUCT_AREAS.fieldName(),
                MandatoryFields.COVERED_CONTENT.fieldName(),
                MandatoryFields.STEP_DESCRIPTION.fieldName(),
                MandatoryFields.ESTIMATED_DURATION.fieldName()
        ));
    }
}
