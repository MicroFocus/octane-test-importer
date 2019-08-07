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

import com.hpe.adm.nga.sdk.model.*;

import java.util.List;

/**
 * This class is used to build EntityModel objects.
 */
class EntityModelBuilder {
    private LongFieldModel estimatedDuration;

    private StringFieldModel id;
    private StringFieldModel type;
    private StringFieldModel email;
    private StringFieldModel nameField;
    private StringFieldModel descriptionField;

    private ReferenceFieldModel owner;
    private ReferenceFieldModel parent;
    private ReferenceFieldModel phase;
    private ReferenceFieldModel designer;

    private MultiReferenceFieldModel testType;
    private MultiReferenceFieldModel userTags;
    private MultiReferenceFieldModel productAreas;
    private MultiReferenceFieldModel coveredContent;

    private EntityModel finalEntityModel;

    /**
     * Creates a new string field model for the id field.
     *
     * @param id - The value of the id field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder id(String id) {
        this.id = new StringFieldModel(EntityModelHelper.Fields.ID.field(), id);
        return this;
    }

    /**
     * Creates a new string field model for the name field.
     *
     * @param name - The value of the name field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder name(String name) {
        this.nameField = new StringFieldModel(EntityModelHelper.Fields.NAME.field(), name);
        return this;
    }

    /**
     * Creates a new string field model for the description field.
     *
     * @param description - The value of the description field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder description(String description) {
        this.descriptionField = new StringFieldModel(EntityModelHelper.Fields.DESCRIPTION.field(), description);
        return this;
    }

    /**
     * Creates a new string field model for the email field.
     *
     * @param email - The value of the email field.
     * @return - The current EntityModelBuilder
     */
    @SuppressWarnings("UnusedReturnValue")
    public EntityModelBuilder email(String email) {
        this.email = new StringFieldModel(EntityModelHelper.Fields.EMAIL.field(), email);
        return this;
    }

    /**
     * Creates a new multi-reference field model for the test types field.
     *
     * @param testTypes - The value of the test types field.
     * @return - The current EntityModelBuilder
     */
    @SuppressWarnings("UnusedReturnValue")
    public EntityModelBuilder testType(List<EntityModel> testTypes) {
        this.testType = new MultiReferenceFieldModel(EntityModelHelper.Fields.TEST_TYPE.field(), testTypes);
        return this;
    }

    /**
     * Creates a new multi-reference field model for the user tags field.
     *
     * @param userTags - The value of the user tags field.
     * @return - The current EntityModelBuilder
     */
    @SuppressWarnings("UnusedReturnValue")
    public EntityModelBuilder userTags(List<EntityModel> userTags) {
        this.userTags = new MultiReferenceFieldModel(EntityModelHelper.Fields.USER_TAGS.field(), userTags);
        return this;
    }

    /**
     * Creates a new string field model for the type field.
     *
     * @param type - The value of the type field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder type(String type) {
        this.type = new StringFieldModel(EntityModelHelper.Fields.TYPE.field(), type);
        return this;
    }

    /**
     * Creates a new multi-reference field model for the product areas field.
     *
     * @param productAreas - The value of the product areas field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder productAreas(List<EntityModel> productAreas) {
        this.productAreas = new MultiReferenceFieldModel(EntityModelHelper.PRODUCT_AREAS, productAreas);
        return this;
    }

    /**
     * Creates a new long field model for the estimated duration field.
     *
     * @param estimatedDuration - The value of the estimated duration field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder estimatedDuration(Long estimatedDuration) {
        this.estimatedDuration = new LongFieldModel(EntityModelHelper.ESTIMATED_DURATION, estimatedDuration);
        return this;
    }

    /**
     * Creates a new reference field model for the parent field.
     *
     * @param parent - The value of the parent field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder parent(EntityModel parent) {
        this.parent = new ReferenceFieldModel(EntityModelHelper.Fields.PARENT.field(), parent);
        return this;
    }

    /**
     * Creates a new reference field model for the owner field.
     *
     * @param owner - The value of the owner field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder owner(EntityModel owner) {
        this.owner = new ReferenceFieldModel(EntityModelHelper.Fields.OWNER.field(), owner);
        return this;
    }

    /**
     * Creates a new reference field model for the phase field.
     *
     * @param phase - The value of the phase field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder phase(EntityModel phase) {
        this.phase = new ReferenceFieldModel(EntityModelHelper.Fields.PHASE.field(), phase);
        return this;
    }

    /**
     * Creates a new reference field model for the designer field.
     *
     * @param designer - The value of the designer field.
     * @return - The current EntityModelBuilder
     */
    @SuppressWarnings("UnusedReturnValue")
    public EntityModelBuilder designer(EntityModel designer) {
        this.designer = new ReferenceFieldModel(EntityModelHelper.Fields.DESIGNER.field(), designer);
        return this;
    }

    /**
     * Creates a new multi-reference field model for the covered content field.
     *
     * @param coveredContent - The value of the covered content field.
     * @return - The current EntityModelBuilder
     */
    public EntityModelBuilder coveredContent(List<EntityModel> coveredContent) {
        this.coveredContent = new MultiReferenceFieldModel(EntityModelHelper.Fields.COVERED_CONTENT.field(), coveredContent);
        return this;
    }

    /**
     * This method creates a new entity model and adds to it all the fields which are not null.
     *
     * @return - The entity model with all the fields.
     */
    public EntityModel build() {
        this.finalEntityModel = new EntityModel();

        addField(id);
        addField(nameField);
        addField(descriptionField);
        addField(testType);
        addField(productAreas);
        addField(estimatedDuration);
        addField(parent);
        addField(owner);
        addField(type);
        addField(email);
        addField(phase);
        addField(designer);
        addField(userTags);
        addField(coveredContent);

        return finalEntityModel;
    }

    /**
     * Adds a field to the entity model if it is not null.
     *
     * @param fieldModel - The field which needs to be added.
     */
    private void addField(FieldModel fieldModel) {
        if (fieldModel != null) {
            finalEntityModel.setValue(fieldModel);
        }
    }
}
