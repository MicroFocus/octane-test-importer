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
import com.hpe.adm.nga.sdk.authentication.Authentication;
import com.hpe.adm.nga.sdk.exception.OctaneException;
import com.hpe.adm.nga.sdk.exception.OctanePartialException;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.model.StringFieldModel;
import com.hpe.adm.nga.sdk.network.google.GoogleHttpClient;
import com.microfocus.adm.almoctane.migration.excelimport.EntityModelHelper.StepTypes;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the class where all the import logic is stored.
 */
class ExcelImporter {
    private static final Logger logger = LoggerFactory.getLogger(ExcelImporter.class);

    private static final String TEST_TYPE = "Test_Type";

    private static final AtomicInteger initErrors = new AtomicInteger();
    private static int currentTestSteps;

    private final int sharedSpace;
    private final int workspace;
    private final String server;
    private final File migrationFile;

    private String URL;
    private Octane octane;
    private final String defaultUserEmail;

    //Change the name of the default release here
    @SuppressWarnings("FieldCanBeLocal")
    private final String defaultReleaseName = "1";

    //Change the name of the default test type here
    private final String defaultTestTypeValueName = "End to End";

    private EntityModel defaultTestTypeValue;
    private OctaneRequestHelper requestHelper;
    private GoogleHttpClient scriptUploadClient;
    private EntityModel rootApplicationEntityModel;

    private Map<String, EntityModel> testTypeListValues;
    private Map<String, EntityModel> users;
    private Map<String, EntityModel> phases;
    private Map<String, EntityModel> userTags;

    private final Map<String, String> migratedTestsIdsMap;

    private XSSFSheet sheet;
    private Map<String, EntityModel> createdApplicationModules;
    private ExcelImportRow excelImportRow;
    private final UDFHandler udfHandler;

    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

    public ExcelImporter(
            final String server,
            final int sharedSpace,
            final int workspace,
            final File migrationFile,
            final String defaultUserEmail
    ) {
        logger.info("Init Migration Tool");
        this.server = server;
        this.workspace = workspace;
        this.sharedSpace = sharedSpace;
        this.migrationFile = migrationFile;
        this.defaultUserEmail = defaultUserEmail;

        currentTestSteps = 0;

        migratedTestsIdsMap = new HashMap<>();

        udfHandler = new UDFHandler();
    }

    /**
     * Initializes the necessary objects for the migration.
     *
     * @param authentication - The authentication used to initialize the script upload client.
     * @return - Status of the migration.
     */
    @SuppressWarnings("UnusedReturnValue")
    public MigrationStatus.Status init(Authentication authentication) {
        logger.info("Starting initializations");

        try {
            logger.info("Initializing sheet...");
            initSheet();
        } catch (IOException e) {
            StringUtils.logException(logger, "Error initializing sheet", e);
            initErrors.addAndGet(1);
            return MigrationStatus.Status.INIT_SHEET_FAILED;
        }

        try {
            logger.info("Initializing script upload client...");
            initScriptUploadClient(authentication);
        } catch (Exception e) {
            StringUtils.logException(logger, "Error initializing script upload client", e);
            initErrors.addAndGet(1);
            return MigrationStatus.Status.INIT_SCRIPT_UPLOAD_CLIENT_FAILED;
        }

        try {
            logger.info("Initializing Octane...");
            initOctane(authentication);
        } catch (Exception e) {
            StringUtils.logException(logger, "Error initializing Octane", e);
            initErrors.addAndGet(1);
            return MigrationStatus.Status.INIT_OCTANE_FAILED;
        }

        initURL();
        initHelper();

        try {
            logger.info("Getting necessary entities from Octane...");
            initEntities();
        } catch (Exception e) {
            StringUtils.logException(logger, "Error getting entities from Octane", e);
            initErrors.addAndGet(1);
            return MigrationStatus.Status.INIT_ENTITIES_FAILED;
        }

        try {
            logger.info("Getting existent phases from Octane...");
            initPhases();
        } catch (Exception e) {
            StringUtils.logException(logger, "Error getting phases from Octane", e);
            initErrors.addAndGet(1);
            return MigrationStatus.Status.INIT_PHASES_FAILED;
        }

        try {
            logger.info("Getting existent user tags from Octane...");
            initUserTags();
        } catch (Exception e) {
            StringUtils.logException(logger, "Error getting user tags from Octane", e);
            initErrors.addAndGet(1);
            return MigrationStatus.Status.INIT_USER_TAGS_FAILED;
        }

        logger.info("Initializing the default release..");
        UDFHandler.initDefaultRelease(defaultReleaseName);

        logger.info("Initialization done");
        return MigrationStatus.Status.INIT_SUCCESS;
    }

    /**
     * This method imports the tests from excel to Octane
     *
     * @return - The migration status.
     */
    @SuppressWarnings("UnusedReturnValue")
    public MigrationStatus.Status migrate() {
        if (initErrors.get() > 0) {
            logger.error(String.format("Cannot start migration! There are %s errors", initErrors));
            return MigrationStatus.Status.CANNOT_MIGRATE;
        }

        Iterator<Row> rowIterator = sheet.iterator();

        StringBuilder sb = new StringBuilder();
        int testsCount = 0;

        if (rowIterator.hasNext()) {
            try {
                Row headerRow = rowIterator.next();
                ExcelImportRow.initRowIndices(headerRow);
                UDFHandler.initRowIndexes(headerRow);

                Iterator<Row> columnIterator = sheet.rowIterator();
                ExcelImportRow.validateUniqueId(columnIterator);

            } catch (RuntimeException e) {
                logger.error(String.format("There are initialization failures!\n\t\t%s", e.getMessage()));
                return MigrationStatus.Status.INCORRECT_FILE;
            }
        } else {
            logger.error("The worksheet is empty. Please provide a correct worksheet!");
            return MigrationStatus.Status.EMPTY_FILE;
        }

        if (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            excelImportRow = new ExcelImportRow(currentRow);

            testsCount = createTestWithSteps(rowIterator, sb, testsCount);
        } else {
            logger.error("There are no tests in the given worksheet. Please provide a correct worksheet!");
            return MigrationStatus.Status.EMPTY_FILE;
        }

        while (rowIterator.hasNext()) {
            testsCount = createTestWithSteps(rowIterator, sb, testsCount);
        }

        if (excelImportRow.getType() != null && excelImportRow.isManualTestRootRow()) {
            testsCount = createTestWithSteps(rowIterator, sb, testsCount);
        }

        logger.info("TESTS CREATED: " + testsCount);

        executorService.shutdown();

        return computeStatus();
    }

    /**
     * Calculates the status of the migration based on number of tests imported and number of tests failed
     *
     * @return - The migration status
     */
    private MigrationStatus.Status computeStatus() {
        int failedTests = MigrationStatus.getFailedTests();
        int failedSteps = MigrationStatus.getFailedSteps();

        if (failedSteps > 0 && failedTests > 0) {
            return MigrationStatus.Status.NOT_ALL_TESTS_AND_STEPS_WERE_MIGRATED;
        }

        if (failedSteps > 0) {
            return MigrationStatus.Status.NOT_ALL_STEPS_WERE_UPLOADED;
        }

        if (failedTests > 0) {
            return MigrationStatus.Status.NOT_ALL_TESTS_WERE_MIGRATED;
        }

        return MigrationStatus.Status.SUCCESS;
    }

    /**
     * Creates a manual tests and uploads the steps for it.
     *
     * @param rowIterator - The iterator for the excel sheet.
     * @param sb          - The string builder for the test steps.
     * @param count       - The number of tests that are migrated
     * @return - The number of the tests that were migrated
     */
    private int createTestWithSteps(Iterator<Row> rowIterator, StringBuilder sb, int count) {
        if (excelImportRow.getType() != null && excelImportRow.isManualTestRootRow()) {
            try {
                EntityModel createdTestManualEntity = createManualTest(excelImportRow);

                migratedTestsIdsMap.put(excelImportRow.getUniqueId(), createdTestManualEntity.getId());

                MigrationStatus.addMigratedTest();

                excelImportRow = buildCurrentManualTestStepsAndAdvanceIterator(rowIterator, excelImportRow, sb);

                uploadStepsForTest(createdTestManualEntity, sb.toString());

                count++;
            } catch (OctanePartialException e) {
                String exceptionMessage = e.getErrorModels().iterator().next().getValue("description").getValue().toString();
                logger.error(String.format("Error creating manual test with unique_id \"%s\". Exception message: %s", excelImportRow.getUniqueId(), exceptionMessage));
                advanceIteratorAndChangeExcelImportRow(rowIterator);
                MigrationStatus.addFailedTest();
            } catch (OctaneException e) {
                logger.error(String.format("Error creating manual test with unique_id \"%s\". Exception message: %s", excelImportRow.getUniqueId(), e.getMessage()));
                advanceIteratorAndChangeExcelImportRow(rowIterator);
                MigrationStatus.addFailedTest();
            } catch (Exception e) {
                MigrationStatus.addFailedTest();
                StringUtils.logException(logger, "Error creating test", e);
                advanceIteratorAndChangeExcelImportRow(rowIterator);
            } finally {
                sb.setLength(0);
            }
        } else {
            if (excelImportRow.getType() == null) {
                MigrationStatus.addFailedStep();
                logger.error(String.format("The row with unique id \"%s\" does not have a type defined, therefore it will no be migrated!",
                        excelImportRow.getUniqueId()));
                advanceIteratorAndChangeExcelImportRow(rowIterator);
            } else if (!excelImportRow.getType().equals("test_manual") || !excelImportRow.getType().equals("step")) {
                logger.error(String.format("The row with unique id \"%s\" has a wrong type defined, therefore it will no be migrated! Row type \"%s\"",
                        excelImportRow.getUniqueId(), excelImportRow.getTestType()));
                advanceIteratorAndChangeExcelImportRow(rowIterator);
            } else {
                advanceIteratorAndChangeExcelImportRow(rowIterator);
                throw new RuntimeException("Something went wrong!");
            }
        }
        return count;
    }

    /**
     * This method advances iterator and updates the excelImportRow.
     *
     * @param rowIterator - The iterator for the excel sheet.
     */
    private void advanceIteratorAndChangeExcelImportRow(Iterator<Row> rowIterator) {
        if (rowIterator.hasNext()) {
            excelImportRow = new ExcelImportRow(rowIterator.next());
        }
    }

    /**
     * Initializes the Octane request helper.
     */
    private void initHelper() {
        requestHelper = new OctaneRequestHelper(URL, octane, scriptUploadClient, defaultUserEmail);
    }

    /**
     * Initializes the API url
     */
    private void initURL() {
        URL = server + "/api/shared_spaces/" + sharedSpace + "/workspaces/" + workspace;
    }

    /**
     * Opens the excel sheet.
     *
     * @throws IOException - In case the excel cannot be opened
     */
    private void initSheet() throws IOException {
        FileInputStream file = new FileInputStream(migrationFile);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        sheet = workbook.getSheetAt(0);
    }

    /**
     * Initializes Octane.
     *
     * @param authentication - The authentication with the default user.
     */
    private void initOctane(Authentication authentication) {
        octane = new Octane.Builder(authentication)
                .Server(server)
                .sharedSpace(sharedSpace)
                .workSpace(workspace)
                .build();
    }

    /**
     * Initializes the script upload client.
     *
     * @param authentication - The authentication with the default user.
     */
    private void initScriptUploadClient(Authentication authentication) {
        scriptUploadClient = new GoogleHttpClient(server);
        scriptUploadClient.authenticate(authentication);
    }

    /**
     * Initializes the user map, application modules map, and test type map.
     */
    private void initEntities() {
        initUserList();
        initProductAreas();
        initTestTypeListAndDefaultTestTypeValue();
    }

    /**
     * Initializes the phases map.
     */
    private void initPhases() {
        phases = EntityModelHelper.getMapFromList(requestHelper.getPhases(), EntityModelHelper::getName, EntityModelHelper::getEssentialFields);
    }

    /**
     * Initializes the user tags map.
     */
    private void initUserTags() {
        userTags = EntityModelHelper.getMapFromList(requestHelper.getUserTags(), EntityModelHelper::getName, EntityModelHelper::getEssentialFields);
    }

    /**
     * Initializes the user map.
     */
    private void initUserList() {
        users = EntityModelHelper.getMapFromList(requestHelper.getUsers(), EntityModelHelper::getEmail, EntityModelHelper::getUserFields);
    }

    /**
     * Initializes the root application module and the created application modules map.
     */
    private void initProductAreas() {
        rootApplicationEntityModel = requestHelper.getApplicationModulesRoot();

        createdApplicationModules = EntityModelHelper.getMapFromList(
                requestHelper.getApplicationModules(),
                EntityModelHelper::getName,
                EntityModelHelper::getApplicationModuleFields);
    }

    /**
     * Initializes the test types map with the test type list values from Octane.
     */
    private void initTestTypeListAndDefaultTestTypeValue() {
        testTypeListValues = EntityModelHelper.getMapFromList(requestHelper.getList(TEST_TYPE), EntityModelHelper::getName, EntityModelHelper::getEssentialFields);

        testTypeListValues.forEach((testTypeName, testTypeEntityModel) -> {
            if (testTypeName.equals(defaultTestTypeValueName)) {
                defaultTestTypeValue = testTypeEntityModel;
            }
        });
    }

    /**
     * Builds the steps for the current test.
     *
     * @param rowIterator           - The iterator for the excel sheet.
     * @param currentExcelImportRow - The row which contains a test step.
     * @param sb                    - The string builder for the test steps
     * @return - The next row after the test script, which represents a test.
     */
    private ExcelImportRow buildCurrentManualTestStepsAndAdvanceIterator(Iterator<Row> rowIterator,
                                                                         ExcelImportRow currentExcelImportRow,
                                                                         StringBuilder sb) {
        while (rowIterator.hasNext()) {
            currentExcelImportRow = new ExcelImportRow(rowIterator.next());

            if (currentExcelImportRow.getType() != null && currentExcelImportRow.getType().equals("step")) {
                buildSteps(currentExcelImportRow, sb);
                currentTestSteps++;
            } else if (currentExcelImportRow.isManualTestRootRow()) {
                break;
            }
        }

        return currentExcelImportRow;
    }

    /**
     * Uploads the steps for the migrated test.
     *
     * @param createdTestManualEntity - The manual test which was uploaded to Octane.
     * @param steps                   - The steps string that will be uploaded for the test.
     */
    private void uploadStepsForTest(EntityModel createdTestManualEntity, String steps) {
        if (createdTestManualEntity != null && !steps.equals("")) {
            OctaneRequestHelper.uploadSteps(buildStepsJson(steps), createdTestManualEntity.getId(), executorService);

            addUploadedSteps();

            logger.info(String.format("Uploaded steps for test with id: %s", createdTestManualEntity.getId()));
        }
    }

    /**
     * Adds to the current test steps.
     */
    private void addUploadedSteps() {
        while (currentTestSteps > 0) {
            MigrationStatus.addUploadedStep();
            currentTestSteps--;
        }
    }

    /**
     * Creates the entity model for the manual test and uploads it in Octane.
     *
     * @param row - The row with the manual test.
     * @return - The entity model of the manual tests which was uploaded in Octane.
     */
    private EntityModel createManualTest(ExcelImportRow row) {
        List<EntityModel> applicationModulesList = getApplicationModules(row);
        if (applicationModulesList != null && applicationModulesList.size() == 0) {
            applicationModulesList.add(rootApplicationEntityModel);
            logger.warn(String.format("For the entity with unique_id \"%s\" the default value for application modules \"%s\" was used instead of \"%s\"",
                    row.getUniqueId(), rootApplicationEntityModel.getValue(EntityModelHelper.Fields.NAME.field()), row.getOwner()));
        }

        String ownerEmail = row.getOwner();
        EntityModel owner = null;
        if (ownerEmail != null) {
            owner = users.get(row.getOwner().trim());
        }
        if (owner == null) {
            owner = OctaneRequestHelper.getDefaultUser();
            logger.warn(String.format("For the entity with unique_id \"%s\" the default value for owner \"%s\" was used instead of \"%s\"",
                    row.getUniqueId(), defaultUserEmail, row.getOwner()));
        }

        EntityModelBuilder testEntityBuilder = new EntityModelBuilder()
                .name(row.getName())
                .description(row.getDescription())
                .productAreas(applicationModulesList)
                .owner(owner);

        Long estimatedDuration = row.getEstimatedDuration();
        if (estimatedDuration != null) {
            if (estimatedDuration < 1 || estimatedDuration > 7000) {
                logger.warn(String.format("For the entity with unique_id \"%s\" an estimated duration was not used. Estimated duration field value: \"%s\"",
                        row.getUniqueId(), row.getEstimatedDuration()));
            } else {
                testEntityBuilder.estimatedDuration(estimatedDuration);
            }
        }

        List<EntityModel> testTypes = row.extractTestTypeEntityModelFromStringList(testTypeListValues);
        if (testTypes == null) {
            testEntityBuilder.testType(Collections.singletonList(defaultTestTypeValue));
            logger.warn(String.format("For the entity with unique_id \"%s\" the default value for test type \"%s\" was used instead of \"%s\"",
                    row.getUniqueId(), defaultTestTypeValueName, row.getTestType()));
        } else {
            testEntityBuilder.testType(testTypes);
        }
        setDesigner(row, testEntityBuilder);
        setPhase(row, testEntityBuilder);
        setCoveredContent(row, testEntityBuilder);
        setUserTags(row, testEntityBuilder);

        EntityModel testEntityModel = testEntityBuilder.build();
        udfHandler.addUDFsToEntityModel(excelImportRow.getRow(), testEntityModel);
        EntityModel createdTestManualEntity = OctaneRequestHelper.createEntity(testEntityModel, EntityModelHelper.MANUAL_TESTS);

        logger.info(String.format("Uploaded test with original id: %s => target id: %s", row.getUniqueId(), createdTestManualEntity.getId()));

        return createdTestManualEntity;
    }

    /**
     * Returns the application modules where the test should be assigned. If the application module does not exist, it
     * will be created.
     *
     * @param row - The row containing the manual test and the application modules where the test should be assigned.
     * @return - The list with the application modules where the test should be assigned.
     */
    private List<EntityModel> getApplicationModules(ExcelImportRow row) {
        String rawApplicationModule = row.getApplicationModule();

        String[] applicationModules;

        if (rawApplicationModule != null) {
            applicationModules = rawApplicationModule.split(",");
        } else {
            return new ArrayList<>();
        }

        EntityModel applicationModule;
        List<EntityModel> applicationModulesList = new ArrayList<>();

        if (applicationModules.length == 1) {
            applicationModule = getOrCreateApplicationModule(applicationModules[0].trim());
            if (applicationModule == null) {
                return null;
            }

            applicationModulesList.add(applicationModule);
        } else {
            for (String appModule : applicationModules) {
                applicationModule = getOrCreateApplicationModule(appModule.trim());
                if (applicationModule == null) {
                    return null;
                }

                applicationModulesList.add(applicationModule);
            }
        }
        return applicationModulesList;
    }

    /**
     * Sets the user tags for the entity model. If the user tags do not exist in Octane, they are created.
     *
     * @param row               - The row containing the manual test and the user tags for test.
     * @param testEntityBuilder - The test entity model builder where the user tags will be added.
     */
    private void setUserTags(ExcelImportRow row, EntityModelBuilder testEntityBuilder) {
        if (row.getUserTags() != null) {
            String[] userTagStrings = row.getUserTags().split(",");
            List<EntityModel> userTagsList = row.extractUserTagEntityModelFromStringList(userTags);

            if (userTagsList.size() < userTagStrings.length) {
                for (String userTag : userTagStrings) {
                    if (userTags.get(userTag.trim()) == null) {
                        EntityModel userTagEntityModel = new EntityModelBuilder().name(userTag.trim()).type("user_tag").build();

                        EntityModel createdUserTag = OctaneRequestHelper.createEntity(userTagEntityModel, EntityModelHelper.USER_TAGS);

                        userTagsList.add(createdUserTag);
                        userTags.put(userTag.trim(), createdUserTag);
                    }
                }
            }

            testEntityBuilder.userTags(userTagsList);
        }
    }

    /**
     * Sets the covered content for the entity model. Covered content should be a user story id or feature id which
     * already exists in Octane.
     *
     * @param row               - The row with the manual test containing the covered content for test.
     * @param testEntityBuilder - The test entity model builder where the covered content will be added.
     */
    private void setCoveredContent(ExcelImportRow row, EntityModelBuilder testEntityBuilder) {
        if (row.getCoveredContent() != null) {
            String[] coveredContent = row.getCoveredContent().split(",");
            List<EntityModel> coveredContentEntities = new ArrayList<>();

            if (!coveredContent[0].equals("")) {
                for (String id : coveredContent) {
                    int coveredContentSize = coveredContentEntities.size();
                    try {
                        long longIdValue = Double.valueOf(id.trim()).longValue();
                        getPossibleEntityForCoveredContent(coveredContentEntities, Long.toString(longIdValue), EntityModelHelper.USER_STORIES);
                        getPossibleEntityForCoveredContent(coveredContentEntities, Long.toString(longIdValue), EntityModelHelper.FEATURES);

                        if (coveredContentEntities.size() - 1 != coveredContentSize) {
                            logger.warn(String.format("For the entity with unique_id \"%s\". The covered content with the id \"%s\" was not found in Octane. ", row.getUniqueId(), id));
                        }
                    } catch (NumberFormatException e) {
                        logger.warn(String.format("For the entity with unique_id \"%s\" the covered content \"%s\" is not correct. This covered content will not be used for the test.",
                                row.getUniqueId(), row.getCoveredContent()));
                    }
                }
            }
            testEntityBuilder.coveredContent(coveredContentEntities);
        }
    }

    /**
     * Searches for the entity with the id given as input and adds it to the covered content entities list.
     *
     * @param coveredContentEntities - The list where entities are added.
     * @param id                     - The id of the searched entity.
     * @param entityName             - The name of the entity that is searched.
     */
    private void getPossibleEntityForCoveredContent(List<EntityModel> coveredContentEntities, String id, String entityName) {
        EntityModel coveredContentEntityModel = requestHelper.getEntityWithEssentialFields(entityName, id);
        if (coveredContentEntityModel != null) {
            coveredContentEntityModel.removeValue(EntityModelHelper.Fields.TYPE.field());
            coveredContentEntityModel.setValue(new StringFieldModel(EntityModelHelper.Fields.TYPE.field(), "work_item"));

            coveredContentEntities.add(coveredContentEntityModel);
        }
    }

    /**
     * Sets the phase for the entity model builder.
     *
     * @param row               - The row with the test containing the phase of the test.
     * @param testEntityBuilder - The entity model builder where the phase is added.
     */
    private void setPhase(ExcelImportRow row, EntityModelBuilder testEntityBuilder) {
        if (row.getPhase() != null) {
            EntityModel phase = phases.get(row.getPhase());

            if (phase != null) {
                testEntityBuilder.phase(phase);
            } else {
                logger.warn(String.format("The default phase New was assigned for row with unique id \"%s\".", row.getUniqueId()));
            }
        }
    }

    /**
     * Sets the designer for the entity model builder.
     *
     * @param row               - The row with the test containing the designer of the test.
     * @param testEntityBuilder - The entity model builder where the designer is added.
     */
    private void setDesigner(ExcelImportRow row, EntityModelBuilder testEntityBuilder) {
        if (row.getDesigner() != null) {
            EntityModel designer = users.get(row.getDesigner());

            if (designer != null) {
                testEntityBuilder.designer(designer);
            } else {
                testEntityBuilder.designer(OctaneRequestHelper.getDefaultUser());
                logger.warn(String.format("For the entity with unique_id \"%s\" the default value for designer \"%s\" was used instead of \"%s\"",
                        row.getUniqueId(), defaultUserEmail, row.getDesigner()));
            }
        }
    }

    /**
     * Returns the application module or creates it in case it does not exist in Octane.
     *
     * @param applicationModule - The name of the application module.
     * @return - The application module.
     */
    private EntityModel getOrCreateApplicationModule(String applicationModule) {
        if (applicationModule.equals("")) {
            return rootApplicationEntityModel;
        }

        EntityModel newApplicationModule = createdApplicationModules.get(applicationModule);

        if (newApplicationModule == null) {
            try {
                newApplicationModule = new EntityModelBuilder()
                        .name(applicationModule)
                        .parent(rootApplicationEntityModel)
                        .build();
            } catch (Exception e) {
                StringUtils.logException(logger, "Application module could not be created!", e);
                return null;
            }
            EntityModel returnedAppModule = OctaneRequestHelper.createEntity(newApplicationModule, EntityModelHelper.PRODUCT_AREAS);

            newApplicationModule.setValue(new StringFieldModel(EntityModelHelper.Fields.TYPE.field(), returnedAppModule.getType()));
            newApplicationModule.setValue(new StringFieldModel(EntityModelHelper.Fields.ID.field(), returnedAppModule.getId()));

            createdApplicationModules.put(applicationModule, newApplicationModule);
        }

        return newApplicationModule;
    }

    /**
     * Builds a string with all the test steps. The steps can be : Call step, Validation step or simple step. The Call step
     * must have ids from the unique_id column.
     *
     * @param row                - The row with the test step.
     * @param stepsStringBuilder - The string builder where the steps are built.
     */
    private void buildSteps(ExcelImportRow row, StringBuilder stepsStringBuilder) {
        final String step = row.getStep().replace("\t\n", "");

        if (row.getStepType().equals(StepTypes.SIMPLE.stepTypeName())) {
            stepsStringBuilder.append("- ").append(StringUtils.escapeMetaCharacters(step)).append("\\n");
        } else if (row.getStepType().equals(StepTypes.VALIDATION.stepTypeName())) {
            stepsStringBuilder.append("- ?").append(StringUtils.escapeMetaCharacters(step)).append("\\n");
        } else if (row.getStepType().equals(StepTypes.CALL.stepTypeName())) {
            String testId = migratedTestsIdsMap.get(row.getStep());
            if (testId != null) {
                stepsStringBuilder.append("- @")
                        .append(testId)
                        .append("\\n");
            } else {
                logger.warn(String.format("For the entry with unique id \"%s\" the call step for id \"%s\" could not be found and will be ignored.",
                        row.getUniqueId(),
                        row.getStep()));
            }
        } else {
            logger.warn(String.format("For the entry with unique_id \"%s\" the step type is not valid. Step type value: \"%s\"",
                    row.getUniqueId(),
                    row.getStepType()));
        }
    }

    /**
     * Builds the JSON with the test steps.
     *
     * @param manualSteps - The string containing all the steps for the test
     * @return - A new string containing the JSON for the manual test
     */
    private String buildStepsJson(final String manualSteps) {
        return String.format("{\"script\":\"%s\",\"comment\":\"\",\"revision_type\":\"Minor\"}", manualSteps);
    }
}