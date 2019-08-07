## Octane test importer

### Introduction
This is a tool for importing tests from a .xlsx file having a format specific to Octane.

The tool uses only the first sheet of an excel document.

***In case the tool runs more than one time in the same workspace where it was run before, the entities will get duplicated.
We suggest running the tool in a clean environment or backing up the Octane data before running.


### Prerequisites

The following objects which are also present in the excel sheet must be created before the migration:
* Users
* User defined fields (UDFs) and lists
* Releases/Features/User stories.
* All the phases present in the excel file must be defined in the Octane workflow

The mandatory fields which must be present in the excel document are the following:
* `unique_id` - The id of the test in the .xlsx file.
* `type` - Row's type. It can be a step or a test_manual.
* `name` - The name of the test. In case the row is a step, this field can be blank.
* `step_type` - This can have the following values: simple, Validation, Call. In case it is a call step, the step_description must contain a value from the unique_id column defined above it.
* `step_description` - The description of a step.
* `test_type` -  This field can have values defined in the Test_Type list (i.e. API, Acceptance, End to End or any other values). It supports multiple values separated by a comma.
* `product_areas` - The application modules that will be assign to the manual test. There can be multiple application modules separated by a comma.
* `covered_content` - The feature or user story where the manual test will be assigned. There can be multiple entity ids separated by a comma.
* `designer` - The email of the user who is the designer of the test.
* `description` - The description of the test and the steps.
* `estimated_duration` - The estimated duration of the test.
* `owner` - The email of the user who is the owner of the test
* `phase` - The phase of the test.
* `user_tags` - The user tags.

You can check the octane docs for more information on the fields.

Fill in the default release an default test type in the ExcelImporter class. These will be used in case the values listed in the excel document are missing(for mandatory fields like test type) or they are not correct.
```java
    private String defaultReleaseName = "1";

    private String defaultTestTypeValueName = "End to End";
```

### Limitations

In case the excel file contains numerical values in cells (i.e. `2345`) without string values in it, the following change should be made: at the beginning of the number insert a `'` symbol (i.e. `'2345`) to modify the old value into an integer. This is also applying to boolean values (i.e. instead of `TRUE` use `'true`). The value of the boolean fields must contain only the values `'true` or `'false`.

Covered content, estimated duration, integer UDFs and unique id columns must follow the rule mentioned above.

For the user fields (i.e. `designer`, `owner` or any other UDFs of type user) do not use only special characters to define the email (i.e. `"\!@` or any other special character sequence). An example of valid input for a user field is `user@domain.com`. This applies also to list item names in case of list UDFs.

If the importer contains in the UDFHandler class fields which are not present in the excel document, those will be ignored. This applies also if the fields are present in the excel document and missing from the UDFHandler class.

If there are duplicated columns only the values from the first encountered column will be considered. This is valid for UDF columns and mandatory columns too.

Call steps can only refer to manual tests that were already imported to Octane. The import is done from top to bottom. 

The cells after the last test/step must all be empty, otherwise some errors will be logged.

### Preparing the importer

##### UDF settings

The UDFHandler class should be changed according to the .xlsx file as follows:
* Add all the UDF names into the UDFNames class.
* Add the UDF names and their type in the udfTypes map.
* If there are UDFs which reference a list, the list name should be put in the ListNames class
* The udfSubtypes map should be changed. The reference fields and the multi-reference fields must be added here.

 
 This is an example of a possible configuration:
```java
    private class UDFNames {
        public static final String USR_UDF = "usr_udf";
        public static final String RELEASE_UDF = "rel_udf";
        public static final String DATE_UDF = "date_udf";
        public static final String LIST_UDF = "list_udf";
    }
    
    private static Map<String, String> udfTypes = new HashMap<String, String>() {{
            put(UDFNames.DATE_UDF, UDFTypes.DATE);
            put(UDFNames.USR_UDF, UDFTypes.REFERNCE);
            put(UDFNames.LIST_UDF, UDFTypes.MULTI_REFERENCE);
            put(UDFNames.RELEASE_UDF, UDFTypes.MULTI_REFERENCE);
    }};
    
    private static class ListNames {
        public static final String NEW_LIST = "new_list";
    }
    
    private static Map<String, String> udfSubtypes = new HashMap<String, String>() {{
            put(UDFNames.LIST_UDF, String.format("%s,%s", UDFSubtypes.LIST, ListNames.NEW_LIST));
            put(UDFNames.USR_UDF, UDFSubtypes.USER);
            put(UDFNames.RELEASE_UDF, UDFSubtypes.RELEASE);
   }};
``` 

In case you have date UDFs the `dateFormatter` in the UDFHandler class can be modified according to the .xlsx date format.
```java
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss z");
```

##### Server settings
The `Importer` class contains the server settings. One should change the values of `server`, `sharedspace`, `workspace`, `user`, `password`, `proxyServer` and `porxyPort` and only after to run the migration. 

Example of configuration:

```java
    private final String server = "http://octane-server.com";

    private final int sharedspace = 1001;
    private final int workspace = 1002;
    
    private final String user = "myUser@email.com";
    private final String password = "password";
    
    private final String proxyServer = "myproxy.com";
    private final String proxyPort = "8080";
```

##### Set the file name

The file should be located in the resource directory of the project. In the `Importer` class, use the name of the file for the `fileName` in this case.

```java
private static final String fileName = "file.xlsx";
```

##### Run the importer

Run the `main` method from the `Importer` class.










