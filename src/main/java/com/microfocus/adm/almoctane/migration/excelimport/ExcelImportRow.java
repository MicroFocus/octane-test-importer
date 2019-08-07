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

import com.hpe.adm.nga.sdk.model.EntityModel;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class helps extracting the data from the excel sheet.
 */
class ExcelImportRow {
    private static final Logger logger = LoggerFactory.getLogger(ExcelImportRow.class);

    private final Row row;
    private static HashMap<String, Integer> columnIndexes;
    private static final List<String> missingMandatoryFields = MandatoryFields.getMandatoryFieldsList();

    /**
     * Initializes the columnIndexes map. The map will contain the name of the mandatory fields mapped to their
     * corresponding index in the excel sheet.
     *
     * @param firstRow - The header of the excel sheet
     **/
    public static void initRowIndices(Row firstRow) {
        columnIndexes = new HashMap<>();

        firstRow.forEach(
                (column) -> {
                    if (missingMandatoryFields.contains(column.toString())) {
                        columnIndexes.put(column.getStringCellValue(), column.getColumnIndex());
                        missingMandatoryFields.remove(column.toString());
                    }
                });

        if (missingMandatoryFields.size() > 0) {
            throw new RuntimeException(String.format(
                    "There are fields missing!!\n\t\tThe following fields are missing: %s", missingMandatoryFields.toString()));
        }
    }

    /**
     * This method validates the unique id column for duplicate ids.
     *
     * @param rowIterator - The iterator of the excel sheet.
     */
    public static void validateUniqueId(Iterator<Row> rowIterator) {
        ExcelImportRow row = null;

        //skip the header
        if (rowIterator.hasNext()) {
            row = new ExcelImportRow(rowIterator.next());
        }

        Set<String> ids = new HashSet<>();

        while (rowIterator.hasNext()) {
            if (ids.contains(row.getUniqueId())) {
                throw new RuntimeException(
                        String.format("The unique_id column is not valid! Row with index %s has unique id %s, which is duplicated.",
                                row.getRow().getRowNum(), row.getUniqueId()));
            }

            ids.add(row.getUniqueId());
            row = new ExcelImportRow(rowIterator.next());
        }
    }

    public ExcelImportRow(Row row) {
        this.row = row;
    }

    /**
     * @return - The excel row.
     */
    public Row getRow() {
        return row;
    }

    /**
     * @return - The unique id cell value.
     */
    public String getUniqueId() {
        return getField(MandatoryFields.UNIQUE_ID);
    }

    /**
     * @return - The type cell value.
     */
    public String getType() {
        return getField(MandatoryFields.TYPE);
    }

    /**
     * @return - The name cell value.
     */
    public String getName() {
        return getField(MandatoryFields.NAME);
    }

    /**
     * @return - The step type cell value.
     */
    public String getStepType() {
        return getField(MandatoryFields.STEP_TYPE);
    }

    /**
     * @return - The step cell value.
     */
    public String getStep() {
        return getField(MandatoryFields.STEP_DESCRIPTION);
    }

    /**
     * @return - The test type cell value.
     */
    public String getTestType() {
        return getField(MandatoryFields.TEST_TYPE);
    }

    /**
     * @return - The application module cell value.
     */
    public String getApplicationModule() {
        return getField(MandatoryFields.PRODUCT_AREAS);
    }

    /**
     * @return - The covered content cell value.
     */
    public String getCoveredContent() {
        return getField(MandatoryFields.COVERED_CONTENT);
    }

    /**
     * @return - The designer cell value.
     */
    public String getDesigner() {
        return getField(MandatoryFields.DESIGNER);
    }

    /**
     * @return - The description cell value.
     */
    public String getDescription() {
        return getField(MandatoryFields.DESCRIPTION);
    }

    /**
     * @return - The estimated duration cell value.
     */
    public Long getEstimatedDuration() {
        String fieldValue = getField(MandatoryFields.ESTIMATED_DURATION);
        if (fieldValue != null) {
            try {
                String field = getField(MandatoryFields.ESTIMATED_DURATION);
                if (field != null) {
                    return Double.valueOf(field).longValue();
                }
                return null;
            } catch (NumberFormatException e) {
                logger.warn(String.format("Error converting cell value to number! At row unique id: \"%s\", Exception detailed message: \"%s\". The Estimated Duration field will be left blank by default.",
                        new ExcelImportRow(row).getUniqueId(),
                        e.getMessage()));
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * @return - The owner cell value.
     */
    public String getOwner() {
        return getField(MandatoryFields.OWNER);
    }

    /**
     * @return - The phase cell value.
     */
    public String getPhase() {
        return getField(MandatoryFields.PHASE);
    }

    /**
     * @return - The user tag cell value.
     */
    public String getUserTags() {
        return getField(MandatoryFields.USER_TAG);
    }

    /**
     * @return - true if the row is a manual test.
     * - false if the row is a step.
     */
    public boolean isManualTestRootRow() {
        String type = getType();
        if (type != null) {
            return type.equals("test_manual");
        }
        return false;
    }

    /**
     * @param entityMap - The map with test type entity models. The key is the name of the test type and the value is
     *                  the entity model.
     * @return - A list with the test type entity models. The name of the test types is taken from the test types cell.
     */
    public List<EntityModel> extractTestTypeEntityModelFromStringList(Map<String, EntityModel> entityMap) {
        if (this.getTestType() != null) {
            String[] entities = this.getTestType().split(",");
            return getEntityModels(entityMap, entities);
        }
        return null;
    }

    /**
     * @param entityMap - The map with user tag entity models. The key is the name of the user tag and the value is
     *                  the entity model.
     * @return - A list with the user tag entity models. The name of the user tags is taken from the test types cell.
     */
    public List<EntityModel> extractUserTagEntityModelFromStringList(Map<String, EntityModel> entityMap) {
        String[] entities = this.getUserTags().split(",");
        return getEntityModels(entityMap, entities);
    }

    /**
     * @param fieldName - Column header.
     * @return - The value of the cell on the current row and the column with the fieldName column header.
     */
    private String getField(MandatoryFields fieldName) {
        int index = columnIndexes.get(fieldName.fieldName());
        if (row.getCell(index) != null) {
            return row.getCell(index).toString();
        }
        return null;
    }

    /**
     * The names of the entities in the cells are translated to a list with their corresponding entity models.
     *
     * @param entityMap - Map with entity models as values mapped to a string field (i.e. email, name) as key.
     * @param entities  - The entities found in the cells.
     * @return - A list with the entity models.
     */
    private List<EntityModel> getEntityModels(Map<String, EntityModel> entityMap, String[] entities) {
        List<EntityModel> entityModelList = new ArrayList<>();

        for (String entity : entities) {
            EntityModel entityModel = entityMap.get(entity.trim());
            if (entityModel != null) {
                entityModelList.add(entityModel);
            }
        }

        return entityModelList;
    }
}
