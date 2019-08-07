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

import com.hpe.adm.nga.sdk.entities.OctaneCollection;
import com.hpe.adm.nga.sdk.model.EntityModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class contains methods and attributes which are useful when working with Octane EntityModels
 */
class EntityModelHelper {
    static final String PHASES = "phases";
    static final String RELEASES = "releases";
    static final String FEATURES = "features";
    static final String USER_TAGS = "user_tags";
    static final String USER_STORIES = "stories";
    static final String USERS = "workspace_users";
    static final String LIST_NODES = "list_nodes";
    static final String MANUAL_TESTS = "manual_tests";
    static final String PRODUCT_AREAS = "product_areas";
    static final String ESTIMATED_DURATION = "estimated_duration";

    /**
     * These fields can be used when working with list_nodes or manual_tests
     */
    public enum Fields {
        ID("id"),
        NAME("name"),
        TYPE("type"),
        EMAIL("email"),
        OWNER("owner"),
        PHASE("phase"),
        PARENT("parent"),
        ENTITY("entity"),
        DESIGNER("designer"),
        USER_TAGS("user_tags"),
        LIST_ROOT("list_root"),
        TEST_TYPE("test_type"),
        DESCRIPTION("description"),
        LOGICAL_NAME("logical_name"),
        COVERED_CONTENT("covered_content");

        private final String fieldName;

        Fields(String fieldName) {
            this.fieldName = fieldName;
        }

        /**
         * @return the field name
         */
        String field() {
            return fieldName;
        }

    }

    /**
     * Here are all the possible step types which can be found in an import file
     */
    public enum StepTypes {
        CALL("Call"),
        SIMPLE("simple"),
        VALIDATION("Validation");

        private final String stepName;

        StepTypes(String fieldName) {
            this.stepName = fieldName;
        }

        /**
         * @return - step type name
         */
        String stepTypeName() {
            return stepName;
        }
    }

    /**
     * Creates a new entity model containing only the basic fields (i.e id, name and type)
     *
     * @param entityModel - The fields will be taken from this entity model
     * @return - A new entity model with the basic fields mentioned above
     */
    static EntityModel getEssentialFields(EntityModel entityModel) {
        return new EntityModelBuilder()
                .id(entityModel.getId())
                .name(getName(entityModel))
                .type(getType(entityModel))
                .build();
    }

    /**
     * Creates a new entity model containing only the basic fields needed to reference a user
     * (i.e id, name, type and email)
     *
     * @param entityModel - The fields will be taken from this entity model
     * @return - A new entity model with the fields mentioned above
     */
    static EntityModel getUserFields(EntityModel entityModel) {
        return new EntityModelBuilder()
                .id(entityModel.getId())
                .name(getName(entityModel))
                .type(getType(entityModel))
                .email(getEmail(entityModel))
                .build();
    }

    /**
     * Creates a new entity model containing only the basic fields needed to reference an application module
     * (i.e id, name, type and parent)
     *
     * @param entityModel - The fields will be taken from this entity model
     * @return - A new entity model with the fields mentioned above
     */
    static EntityModel getApplicationModuleFields(EntityModel entityModel) {
        return new EntityModelBuilder()
                .id(entityModel.getId())
                .name(getName(entityModel))
                .type(getType(entityModel))
                .parent(getParent(entityModel))
                .build();
    }

    /**
     * This method creates a map with an entity model string attribute as key and the entity model's necessary fields
     * as value.
     *
     * @param items                      - Collection with entity models
     * @param getFieldCallback           - Callback which will return the key filed
     * @param getNecessaryFieldsCallback - Callback which returns an entity model with the basic fields
     * @return - A map containing the attributes of the entity models mapped to the entity models with the basic fields.
     */
    static Map<String, EntityModel> getMapFromList(
            OctaneCollection<EntityModel> items,
            Function<EntityModel, String> getFieldCallback,
            Function<EntityModel, EntityModel> getNecessaryFieldsCallback
    ) {
        Map<String, EntityModel> itemsMap = new HashMap<>();

        for (EntityModel item : items) {
            itemsMap.put(getFieldCallback.apply(item), getNecessaryFieldsCallback.apply(item));
        }

        return itemsMap;
    }

    /**
     * @param entityModel - The entity model is used to get the field
     * @return - The name field from the entity model
     */
    public static String getName(EntityModel entityModel) {
        return entityModel.getValue(Fields.NAME.field()).getValue().toString();
    }

    /**
     * @param entityModel - The entity model is used to get the field
     * @return - The type field from the entity model
     */
    private static String getType(EntityModel entityModel) {
        return entityModel.getValue(Fields.TYPE.field()).getValue().toString();
    }

    /**
     * @param entityModel - The entity model is used to get the field
     * @return - The email field from the entity model
     */
    static String getEmail(EntityModel entityModel) {
        return entityModel.getValue(Fields.EMAIL.field()).getValue().toString();
    }

    /**
     * @param entityModel - The entity model is used to get the field
     * @return - The parent field from the entity model
     */
    private static EntityModel getParent(EntityModel entityModel) {
        return (EntityModel) entityModel.getValue(Fields.PARENT.field()).getValue();
    }
}
