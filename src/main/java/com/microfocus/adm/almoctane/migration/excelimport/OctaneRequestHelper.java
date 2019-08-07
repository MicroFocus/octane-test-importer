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

import com.hpe.adm.nga.sdk.Octane;
import com.hpe.adm.nga.sdk.entities.OctaneCollection;
import com.hpe.adm.nga.sdk.exception.OctanePartialException;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.network.OctaneHttpRequest;
import com.hpe.adm.nga.sdk.network.google.GoogleHttpClient;
import com.hpe.adm.nga.sdk.query.Query;
import com.hpe.adm.nga.sdk.query.QueryMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * This class helps perform Octane requests.
 */
class OctaneRequestHelper {
    private static final Logger logger = LoggerFactory.getLogger(ExcelImporter.class);

    private static String url;
    private static Octane octane;
    private static EntityModel defaultUser;
    private static GoogleHttpClient scriptUploadClient;

    public OctaneRequestHelper(String octaneUrl, Octane octane, GoogleHttpClient scriptUploadClient, String defaultUserEmail) {
        OctaneRequestHelper.url = octaneUrl.concat("/tests/id/script");

        OctaneRequestHelper.octane = octane;
        OctaneRequestHelper.scriptUploadClient = scriptUploadClient;
        OctaneRequestHelper.defaultUser = OctaneRequestHelper.getUserByEmail(defaultUserEmail);
    }

    /**
     * @return - The default user.
     */
    public static EntityModel getDefaultUser() {
        return defaultUser;
    }

    /**
     * Executes a request in order to create an entity in Octane.
     *
     * @param entityModel - The entity model for the manual test.
     * @param entityType  - The type of the entity which will be created.
     * @return - Created test.
     * @throws OctanePartialException - In case the request fails.
     */
    public static EntityModel createEntity(EntityModel entityModel, String entityType) {
        OctaneCollection<EntityModel> createdEntity = octane
                .entityList(entityType)
                .create()
                .entities(Collections.singletonList(entityModel))
                .execute();

        return createdEntity.stream().findFirst().orElseThrow(() ->
                new RuntimeException(String.format("Unable to create entity of type %s!", entityType)));
    }

    /**
     * Executes a request in order to get the root of the application modules.
     *
     * @return - Root of the application modules.
     * @throws RuntimeException       - In case the root application module was not found or the request failed.
     * @throws OctanePartialException - In case the request fails.
     */
    public EntityModel getApplicationModulesRoot() {
        return EntityModelHelper.getEssentialFields(octane.entityList(EntityModelHelper.PRODUCT_AREAS).get()
                .addFields(EntityModelHelper.Fields.NAME.field())
                .query(
                        Query.statement(
                                EntityModelHelper.Fields.PARENT.field(),
                                QueryMethod.EqualTo,
                                null).build()
                ).execute().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to get parent application module")));
    }

    /**
     * Executes a request in order to get a list's root.
     *
     * @param listName - The name of the list for which we search the root.
     * @return - The entity model of the list root.
     * @throws RuntimeException       - In case  the list is not found or the request failed.
     * @throws OctanePartialException - In case the request fails.
     */
    public static EntityModel getListRoot(String listName) {
        return octane.entityList(EntityModelHelper.LIST_NODES).get()
                .addFields(EntityModelHelper.Fields.NAME.field(), EntityModelHelper.Fields.LOGICAL_NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.NAME.field(), QueryMethod.EqualTo, listName)
                        .build())
                .execute()
                .stream().findFirst()
                .orElseThrow(() ->
                        new RuntimeException(String.format("Unable to get parent list root for list with name %s", listName)));
    }

    /**
     * Returns an entity model of a list item. The search is done by item's name.
     *
     * @param listRootId   - The id of list's root.
     * @param listItemName - Searched list item's name.
     * @return - An entity model with the list item.
     * - null if there was no list item found.
     * @throws OctanePartialException - In case the request fails.
     */
    public static EntityModel getListItem(String listRootId, String listItemName) {
        return octane.entityList(EntityModelHelper.LIST_NODES).get()
                .addFields(EntityModelHelper.Fields.NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.LIST_ROOT.field(), QueryMethod.EqualTo,
                        Query.statement(EntityModelHelper.Fields.ID.field(), QueryMethod.EqualTo, listRootId))
                        .and(Query.statement(EntityModelHelper.Fields.NAME.field(), QueryMethod.EqualTo, listItemName))
                        .build())
                .execute().stream().findFirst().orElse(null);
    }

    /**
     * Returns a collection with entity models, which are list's items.
     *
     * @param listRootId - The id of list's root.
     * @return - A collection with entity models, which are list's items.
     * @throws OctanePartialException - In case the request fails.
     */
    private static OctaneCollection<EntityModel> getListItems(String listRootId) {
        return octane.entityList(EntityModelHelper.LIST_NODES).get()
                .addFields(EntityModelHelper.Fields.NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.LIST_ROOT.field(), QueryMethod.EqualTo,
                        Query.statement(EntityModelHelper.Fields.ID.field(), QueryMethod.EqualTo, listRootId))
                        .build())
                .execute();
    }

    /**
     * Execute a request to search an entity by name.
     *
     * @param itemName   - Searched list item's name.
     * @param entityType - The type of the entity.
     * @return - An entity model with the list item.
     * - null if there was no list item found.
     * @throws OctanePartialException - In case the request fails.
     */
    public static EntityModel getEntityByName(String entityType, String itemName) {
        return octane.entityList(entityType).get()
                .addFields(EntityModelHelper.Fields.NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.NAME.field(), QueryMethod.EqualTo, itemName).build())
                .execute().stream().findFirst().orElse(null);
    }


    /**
     * Uploads steps to a test with the test id given as input. This is done on a different thread.
     *
     * @param stepsJson       - The JSON with the steps.
     * @param testId          - Id of the test where the steps are uploaded.
     * @param executorService - The executor service where we submit the task of uploading test's steps.
     */
    public static void uploadSteps(String stepsJson, String testId, ExecutorService executorService) {
        executorService.submit(() -> {
            try {
                OctaneHttpRequest.PutOctaneHttpRequest putOctaneHttpRequest = new OctaneHttpRequest.PutOctaneHttpRequest(
                        url.replace(EntityModelHelper.Fields.ID.field(), testId),
                        OctaneHttpRequest.JSON_CONTENT_TYPE,
                        stepsJson
                );

                scriptUploadClient.execute(putOctaneHttpRequest);
            } catch (OctanePartialException e) {
                MigrationStatus.addFailedStep();
                logger.error("Error creating script for manual test with id");
            }
        });
    }

    /**
     * Returns a collection with all the existent users.
     *
     * @return -  A collection with all the existing users.
     * @throws OctanePartialException - In case the request fails.
     */
    public OctaneCollection<EntityModel> getUsers() {
        return octane.entityList(EntityModelHelper.USERS).get()
                .addFields(EntityModelHelper.Fields.EMAIL.field(), EntityModelHelper.Fields.NAME.field()).execute();
    }

    /**
     * Returns an entity model for the user with the given email.
     *
     * @param email - User's email.
     * @return -  An entity model for the searched user.
     * @throws OctanePartialException - In case the request fails.
     */
    public static EntityModel getUserByEmail(String email) {
        return octane.entityList(EntityModelHelper.USERS).get()
                .addFields(EntityModelHelper.Fields.EMAIL.field(), EntityModelHelper.Fields.NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.EMAIL.field(), QueryMethod.EqualTo, email).build())
                .execute().stream().findFirst().orElse(null);
    }

    /**
     * Returns a list having the input list name.
     *
     * @param listName - The name of the list.
     * @return - A collection with the list items entity models.
     * @throws OctanePartialException - In case the request fails.
     */
    public OctaneCollection<EntityModel> getList(String listName) {
        EntityModel listRoot = getListRoot(listName);
        return getListItems(listRoot.getId());
    }

    /**
     * Executes a request in order to get an entity by id.
     *
     * @param entityType - The entity type which is used in the request.
     * @param entityId   - The id of the entity.
     * @return - The entity with the id given as input.
     * - null in case the entity was not found.
     * @throws OctanePartialException - In case the request fails.
     */
    public EntityModel getEntityWithEssentialFields(String entityType, String entityId) {
        OctaneCollection<EntityModel> entityModels = octane.entityList(entityType).get()
                .addFields(EntityModelHelper.Fields.NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.ID.field(), QueryMethod.EqualTo, entityId).build())
                .execute();
        if (entityModels.stream().findFirst().isPresent()) {
            return EntityModelHelper.getEssentialFields(entityModels.stream().findFirst().get());
        }
        return null;
    }

    /**
     * Executes a request in order to get all the application modules.
     *
     * @return - A collection containing all the application modules.
     * @throws OctanePartialException - In case the request fails.
     */
    public OctaneCollection<EntityModel> getApplicationModules() {
        return octane.entityList(EntityModelHelper.PRODUCT_AREAS).get()
                .addFields(EntityModelHelper.Fields.NAME.field(), EntityModelHelper.Fields.PARENT.field())
                .execute();
    }

    /**
     * Executes a request in order to get all the phases.
     *
     * @return - A collection containing all the phases.
     * @throws OctanePartialException - In case the request fails.
     */
    public OctaneCollection<EntityModel> getPhases() {
        return octane.entityList(EntityModelHelper.PHASES).get()
                .addFields(EntityModelHelper.Fields.NAME.field())
                .query(Query.statement(EntityModelHelper.Fields.ENTITY.field(), QueryMethod.EqualTo, "test_manual")
                        .build()).execute();
    }

    /**
     * Executes a request in order to get all the user tags.
     *
     * @return - A collection with all the user tags.
     * @throws OctanePartialException - In case the request fails.
     */
    public OctaneCollection<EntityModel> getUserTags() {
        return octane.entityList(EntityModelHelper.USER_TAGS).get().addFields(EntityModelHelper.Fields.NAME.field()).execute();
    }
}
