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
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * This can be used in order to add UDFs to the entity models. In order to add UDFs add them in the udfTypes and
 * udfSubtypes.
 */
class UDFHandler {
    private static String defaultRelease;
    private static Map<String, Integer> rowIndexes;
    private static final Logger logger = LoggerFactory.getLogger(UDFHandler.class);

    //This formatter can be changed in case the file has any other format
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss z");

    private class UDFNames {
        static final String USR_UDF = "usr_udf";
        static final String INT_UDF = "int_udf";
        static final String DATE_UDF = "date_udf";
        static final String LONG_UDF = "long_udf";
        static final String MEMO_UDF = "memo_udf";
        static final String STRING_UDF = "str_udf";
        static final String RELEASE_UDF = "rel_udf";
        static final String BOOLEAN_UDF = "bool_udf";
        static final String DEF_LIST_UDF = "def_list_udf";
        static final String REL_REF_UDF = "rel_ref_udf";
        static final String USR_REF_UDF = "usr_ref_udf";
        static final String LIST_REF_UDF = "list_ref_udf";

    }

    /*****************************************************************************************************************
     * This map contains the UDFs that are defined in the excel and their type. These UDFs must be already
     * created in Octane. Possible values for udf subtypes: string, long(this is for long string UDFs), boolean, date,
     * reference, multiReference. Use the UDFTypes below in order to define your udf map.
     *****************************************************************************************************************
     */
    private static final Map<String, String> udfTypes = new HashMap<String, String>() {{
        put(UDFNames.INT_UDF, UDFTypes.FLOAT);
        put(UDFNames.DATE_UDF, UDFTypes.DATE);
        put(UDFNames.MEMO_UDF, UDFTypes.STRING);
        put(UDFNames.LONG_UDF, UDFTypes.STRING);
        put(UDFNames.STRING_UDF, UDFTypes.STRING);
        put(UDFNames.BOOLEAN_UDF, UDFTypes.BOOLEAN);
        put(UDFNames.USR_REF_UDF, UDFTypes.REFERENCE);
        put(UDFNames.REL_REF_UDF, UDFTypes.REFERENCE);
        put(UDFNames.LIST_REF_UDF, UDFTypes.REFERENCE);
        put(UDFNames.USR_UDF, UDFTypes.MULTI_REFERENCE);
        put(UDFNames.RELEASE_UDF, UDFTypes.MULTI_REFERENCE);
        put(UDFNames.DEF_LIST_UDF, UDFTypes.MULTI_REFERENCE);
    }};

    private class UDFTypes {
        static final String DATE = "date";
        //the long value is used for Integer fields
        static final String LONG = "long";
        static final String FLOAT = "float";
        static final String STRING = "string";
        static final String BOOLEAN = "boolean";
        static final String REFERENCE = "reference";
        static final String MULTI_REFERENCE = "multiReference";
    }

    private static class ListNames {
        static final String DEF_LIST = "def_list";
    }

    /*****************************************************************************************************************
     * This map contains the subtype of the UDF. Possible values:
     * - user
     * - release
     * - list - The list value must be followed by the list's name (i.e. ("name_udf", "list,list_name"))
     *****************************************************************************************************************
     */
    private static final Map<String, String> udfSubtypes = new HashMap<String, String>() {{
        put(UDFNames.DEF_LIST_UDF, String.format("%s,%s", UDFSubtypes.LIST, ListNames.DEF_LIST));
        put(UDFNames.USR_UDF, UDFSubtypes.USER);
        put(UDFNames.RELEASE_UDF, UDFSubtypes.RELEASE);
        put(UDFNames.LIST_REF_UDF, String.format("%s,%s", UDFSubtypes.LIST, ListNames.DEF_LIST));
        put(UDFNames.USR_REF_UDF, UDFSubtypes.USER);
        put(UDFNames.REL_REF_UDF, UDFSubtypes.RELEASE);
    }};

    private class UDFSubtypes {
        static final String LIST = "list";
        static final String USER = "user";
        static final String RELEASE = "release";
    }

    /**
     * This method sets the udf values of the manual test.
     *
     * @param row         - The excel row from where the data is taken
     * @param entityModel - The entity model of the manual test
     */
    public void addUDFsToEntityModel(Row row, EntityModel entityModel) {
        for (String udfName : udfTypes.keySet()) {
            if (rowIndexes.get(udfName) == null) {
                return;
            }
            if (row.getCell(rowIndexes.get(udfName)) == null) {
                continue;
            }

            FieldModel fm = null;

            switch (udfTypes.get(udfName)) {
                case UDFTypes.STRING:
                    fm = new StringFieldModel(udfName, getCellStringValue(row, udfName));
                    break;
                case UDFTypes.LONG:
                    fm = new LongFieldModel(udfName, getLongValue(row, udfName));
                    break;
                case UDFTypes.FLOAT:
                    fm = new FloatFieldModel(udfName, getFloatValue(row, udfName));
                    break;
                case UDFTypes.BOOLEAN:
                    fm = new BooleanFieldModel(udfName, getBooleanValue(row, udfName));
                    break;
                case UDFTypes.DATE:
                    ZonedDateTime date = getDateValue(row, udfName);
                    if (date != null) {
                        fm = new DateFieldModel(udfName, date);
                    }
                    break;
                case UDFTypes.REFERENCE: {
                    fm = getReferenceFieldModel(row, udfName);
                    break;
                }
                case UDFTypes.MULTI_REFERENCE: {
                    fm = getMultiReferenceFieldModel(row, udfName);
                    break;
                }
            }
            if (fm != null) {
                entityModel.setValue(fm);
            }
        }
    }

    /**
     * Return the date specified in the excel.
     *
     * @param row     - The excel row from where the data is taken.
     * @param udfName - Name of the udf.
     * @return - The date specified in the excel.
     */
    private ZonedDateTime getDateValue(Row row, String udfName) {
        try {
            return ZonedDateTime.parse(getCellStringValue(row, udfName), dateTimeFormatter);
        } catch (DateTimeParseException e) {
            logParseWarning(row, e, udfName, getCellStringValue(row, udfName));
            return null;
        }
    }

    /**
     * Return the float value specified in the excel.
     *
     * @param row     - The excel row from where the data is taken.
     * @param udfName - Name of the udf.
     * @return - The float value specified in the excel.
     */
    private Float getFloatValue(Row row, String udfName) {
        try {
            return Float.valueOf(getCellStringValue(row, udfName));
        } catch (NumberFormatException e) {
            logParseWarning(row, e, udfName, getCellStringValue(row, udfName));
            return null;
        }
    }

    /**
     * Return the string value specified in the excel.
     *
     * @param row     - The excel row from where the data is taken.
     * @param udfName - Name of the udf.
     * @return - The string value specified in the excel.
     */
    private String getCellStringValue(Row row, String udfName) {
        return row.getCell(rowIndexes.get(udfName)).toString();
    }

    /**
     * Return the boolean value specified in the excel.
     *
     * @param row     - The excel row from where the data is taken.
     * @param udfName - Name of the udf.
     * @return - The boolean value specified in the excel.
     */
    private Boolean getBooleanValue(Row row, String udfName) {
        try {
            return Boolean.valueOf(getCellStringValue(row, udfName));
        } catch (NumberFormatException e) {
            logParseWarning(row, e, udfName, getCellStringValue(row, udfName));
            return false;
        }
    }

    /**
     * Return the long value specified in the excel.
     *
     * @param row     - The excel row from where the data is taken.
     * @param udfName - Name of the udf.
     * @return - The long value specified in the excel.
     */
    private Long getLongValue(Row row, String udfName) {
        try {
            return Double.valueOf(getCellStringValue(row, udfName)).longValue();
        } catch (NumberFormatException e) {
            logParseWarning(row, e, udfName, getCellStringValue(row, udfName));
            return null;
        }
    }

    /**
     * Logs a warning in in case of a parsing exception.
     *
     * @param row       - The excel row from where the data is taken.
     * @param e         - The exception.
     * @param cellName  - The name of the UDF.
     * @param cellValue - The value of the UDF.
     */
    private void logParseWarning(Row row, Exception e, String cellName, String cellValue) {
        logger.warn(String.format("Error converting cell value! At row unique id: \"%s\". Exception detailed message: \"%s\". The field with name \"%s\" will be left blank by default. Field original content: \"%s\"",
                new ExcelImportRow(row).getUniqueId(),
                e.getMessage(),
                cellName,
                cellValue));
    }

    /**
     * Initializes the default release name.
     *
     * @param releaseName - The name of the release which will be used as default.
     */
    public static void initDefaultRelease(String releaseName) {
        defaultRelease = releaseName;
    }

    /**
     * The reference field model is build based on the type of the udf. The type can be: list, user, release.
     *
     * @param row     - The excel row from where the data is taken
     * @param udfName - The name of the UDF.
     * @return - The reference field model for the UDF.
     */
    private FieldModel getReferenceFieldModel(Row row, String udfName) {
        String type = udfSubtypes.get(udfName).split(",")[0];

        switch (type) {
            case UDFSubtypes.LIST:
                String entityName = udfSubtypes.get(udfName).split(",")[1];
                try {
                    EntityModel listItem = OctaneRequestHelper.getListItem(
                            OctaneRequestHelper.getListRoot(entityName).getId(),
                            getCellStringValue(row, udfName));
                    return new ReferenceFieldModel(udfName, listItem);
                } catch (RuntimeException e) {
                    logger.warn(String.format("For the entity with unique_id \"%s\" the list item \"%s\" for udf with name \"%s\" was not found.",
                            new ExcelImportRow(row).getUniqueId(), getCellStringValue(row, udfName), udfName));
                }
            case UDFSubtypes.USER:
                EntityModel user = OctaneRequestHelper.getUserByEmail(getCellStringValue(row, udfName));
                if (user != null) {
                    return new ReferenceFieldModel(udfName, user);
                } else {
                    return new ReferenceFieldModel(udfName, OctaneRequestHelper.getDefaultUser());
                }
            case UDFSubtypes.RELEASE:
                EntityModel release = OctaneRequestHelper.getEntityByName(EntityModelHelper.RELEASES,
                        getCellStringValue(row, udfName));
                if (release != null) {
                    return new ReferenceFieldModel(udfName, release);
                } else {
                    return new ReferenceFieldModel(udfName, OctaneRequestHelper.getEntityByName(EntityModelHelper.RELEASES, defaultRelease));
                }
            default:
                return null;
        }
    }

    /**
     * The multi-reference field model is build based on the type of the udf. The type can be: list, user, release.
     *
     * @param row     - The excel row from where the data is taken.
     * @param udfName - The name of the UDF.
     * @return - The multi-reference field model for the UDF.
     */
    private FieldModel getMultiReferenceFieldModel(Row row, String udfName) {
        String type = udfSubtypes.get(udfName).split(",")[0];
        String[] cellItems = getCellStringValue(row, udfName).split(",");
        List<EntityModel> entityModels = new ArrayList<>();

        switch (type) {
            case UDFSubtypes.LIST:
                return getMultiReferenceFieldModelForListField(udfName, cellItems, entityModels);
            case UDFSubtypes.USER:
                return getMultiReferenceFieldModelForUserField(row, udfName, cellItems, entityModels);
            case UDFSubtypes.RELEASE:
                return getMultiReferenceFieldModelForReleaseField(row, udfName, cellItems, entityModels);
            default:
                return null;
        }
    }

    /**
     * This method returns the multi-reference field for release fields.
     *
     * @param row          - The excel row from where the data is taken.
     * @param udfName      - The name of the release UDF.
     * @param cellItems    - The values of the UDF defined in excel.
     * @param entityModels - The list with the UDF entity model values.
     * @return - The multi-reference field for the user field.
     */
    private FieldModel getMultiReferenceFieldModelForReleaseField(Row row, String udfName, String[] cellItems, List<EntityModel> entityModels) {
        List<String> releases = new ArrayList<>();

        for (String item : cellItems) {
            EntityModel release = OctaneRequestHelper.getEntityByName(EntityModelHelper.RELEASES, item.trim());
            if (release != null && !releases.contains(EntityModelHelper.getName(release))) {
                entityModels.add(release);
                releases.add(EntityModelHelper.getName(release));
            } else {
                release = OctaneRequestHelper.getEntityByName(EntityModelHelper.RELEASES, defaultRelease);
                if (!releases.contains(EntityModelHelper.getName(release))) {
                    entityModels.add(OctaneRequestHelper.getEntityByName(EntityModelHelper.RELEASES, defaultRelease));
                    releases.add(EntityModelHelper.getName(release));
                    logger.warn(String.format("Releases listed in the \"%s\" field do not exist. Problem is located on row with unique_id \"%s\". Only one release will be set, and the rest will be ignored. Original content: \"%s\". Problematic item: \"%s\"", udfName, new ExcelImportRow(row).getUniqueId(), getCellStringValue(row, udfName), item.trim()));
                } else {
                    logger.warn(String.format("Releases listed in the \"%s\" field are duplicated. Problem is located on row with unique_id \"%s\". Only one release will be set, and the rest will be ignored. Original content: \"%s\". Problematic item: \"%s\"", udfName, new ExcelImportRow(row).getUniqueId(), getCellStringValue(row, udfName), item.trim()));
                }
            }
        }

        return new MultiReferenceFieldModel(udfName, entityModels);
    }

    /**
     * This method returns the multi-reference field for user fields.
     *
     * @param row          - The excel row from where the data is taken.
     * @param udfName      - The name of the user UDF.
     * @param cellItems    - The values of the UDF defined in excel.
     * @param entityModels - The list with the UDF entity model values.
     * @return - The multi-reference field for the user field.
     */
    private FieldModel getMultiReferenceFieldModelForUserField(Row row, String udfName, String[] cellItems, List<EntityModel> entityModels) {
        List<String> addedUsers = new ArrayList<>();

        for (String item : cellItems) {
            EntityModel user = OctaneRequestHelper.getUserByEmail(item.trim());
            if (user != null) {
                String userName = EntityModelHelper.getName(user);
                if (!addedUsers.contains(userName)) {
                    entityModels.add(user);
                    addedUsers.add(userName);
                    logger.warn(String.format("Users listed in the %s field are duplicated on row with unique_id %s. Only one user will be set, and the rest will be ignored. Original content: %s", udfName, new ExcelImportRow(row).getUniqueId(), getCellStringValue(row, udfName)));
                }
            } else {
                user = OctaneRequestHelper.getDefaultUser();
                String userName = EntityModelHelper.getName(user);
                if (!addedUsers.contains(userName)) {
                    entityModels.add(user);
                    addedUsers.add(userName);
                    logger.warn(String.format("Users listed in the %s field are duplicated or they do not exist on row with unique_id %s. Only one user will be set, and the rest will be ignored. Original content: %s", udfName, new ExcelImportRow(row).getUniqueId(), getCellStringValue(row, udfName)));
                }
            }
        }

        return new MultiReferenceFieldModel(udfName, entityModels);
    }

    /**
     * This method returns the multi-reference field for list fields.
     *
     * @param udfName      - The name of the list UDF.
     * @param cellItems    - The values of the UDF defined in excel.
     * @param entityModels - The list with the UDF entity model values.
     * @return - The multi-reference field for the user field.
     */
    private FieldModel getMultiReferenceFieldModelForListField(String udfName, String[] cellItems, List<EntityModel> entityModels) {
        for (String item : cellItems) {
            String entityName = udfSubtypes.get(udfName).split(",")[1];
            try {
                EntityModel listItem = OctaneRequestHelper.getListItem(
                        OctaneRequestHelper.getListRoot(entityName).getId(),
                        item.trim());
                if (listItem != null) {
                    entityModels.add(listItem);
                }
            } catch (RuntimeException e) {
                logger.warn(String.format("The list item \"%s\" for udf with name \"%s\" was not found.", item.trim(), udfName));
            }
        }

        return new MultiReferenceFieldModel(udfName, entityModels);
    }

    /**
     * The rowIndexes is set based on the header of the excel file. The map will contain the name of the udf as key and the
     * number of the column as value.
     *
     * @param firstRow - The header of the excel file.
     */
    public static void initRowIndexes(Row firstRow) {
        rowIndexes = new HashMap<>();
        List<String> mandatoryFields = MandatoryFields.getMandatoryFieldsList();
        Map<String, Integer> unusedFields = new HashMap<>();

        firstRow.forEach((column) -> {
            if (udfTypes.keySet().contains(column.toString())) {
                rowIndexes.put(column.getStringCellValue(), column.getColumnIndex());
            } else if (!mandatoryFields.contains(column.toString())) {
                unusedFields.put(column.toString(), column.getColumnIndex());
            }
        });

        logUnusedFields(unusedFields);
    }

    /**
     * This method logs the unused UDFs listed in the excel file.
     *
     * @param unusedFields - Map with UDF fields names mapped to their column indexes.
     */
    private static void logUnusedFields(Map<String, Integer> unusedFields) {
        if (unusedFields.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String field : unusedFields.keySet()) {
                sb.append(String.format("\t\t\tField name: %s, Field column: %s\n", field, unusedFields.get(field)));
            }

            logger.warn("You have unused fields in the excel sheet! They will be ignored for the import process.\n\t\tFields that are unused:\n" +
                    sb.toString());
        }
    }
}