package com.microfocus.adm.almoctane.migration.excelimport;

import com.hpe.adm.nga.sdk.authentication.Authentication;
import com.hpe.adm.nga.sdk.authentication.SimpleClientAuthentication;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

public class ImporterTests {
    //change the server you want to migrate to
    private final String server = "";

    //change the shared space
    private final int sharedSpace = 0;
    //change the workspace
    private final int workspace = 0;

    //change the user
    private static final String user = "";
    //change the password
    private static final String password = "";
    //change the proxy server
    private static final String proxyServer = "";
    //change the port
    private static final String proxyPort = "";

    private final Authentication authentication = new SimpleClientAuthentication(user, password);

    @BeforeClass
    public static void initProxy() {
        //HTTP
        System.setProperty("http.proxyHost", proxyServer);
        System.setProperty("http.proxyPort", proxyPort);

        //HTTPS
        System.setProperty("https.proxyHost", proxyServer);
        System.setProperty("https.proxyPort", proxyPort);
    }

    @Test
    public void JustHeaderWithAllRequiredFields() throws Exception {
        performMigration("testFiles/1 - JustHeaderWithAllRequiredFields.xlsx");
    }

    @Test
    public void JustHeaderWithSomeFieldsMissing() throws Exception {
        performMigration("testFiles/2 - JustHeaderWithSomeFieldsMissing.xlsx");
    }

    @Test
    public void JustHeaderWithInvertedColumnsAndBlankColumn() throws Exception {
        performMigration("testFiles/3 - JustHeaderWithInvertedColumnsAndBlankColumn.xlsx");
    }

    @Test
    public void JustHeaderWithInvertedColumnsAndBlankColumnDuplicatedColumns() throws Exception {
        performMigration("testFiles/4 - JustHeaderWithInvertedColumnsAndBlankColumnDuplicatedColumns.xlsx");
    }

    @Test
    public void JustHeaderWithUDFs() throws Exception {
        performMigration("testFiles/5 - JustHeaderWithUdfs.xlsx");
    }

    @Test
    public void AllRequiredFields() throws Exception {
        performMigration("testFiles/6 - AllRequiredFields.xlsx");
    }

    @Test
    public void SomeFieldsMissingWithUDFs() throws Exception {
        performMigration("testFiles/7 - SomeFieldsMissingWithUDFs.xlsx");
    }

    @Test
    public void InvertedColumnsAndBlankColumn() throws Exception {
        performMigration("testFiles/8 - InvertedColumnsAndBlankColumn.xlsx");
    }

    @Test
    public void InvertedColumnsAndBlankColumnDuplicatedColumns() throws Exception {
        performMigration("testFiles/9 - InvertedColumnsAndBlankColumnDuplicatedColumns.xlsx");
    }

    @Test
    public void WithUDFs() throws Exception {
        performMigration("testFiles/10 - WithUdf.xlsx");
    }

    @Test
    public void TestsWithSteps() throws Exception {
        performMigration("testFiles/11 - TestsWithSteps.xlsx");
    }

    private void performMigration(String fileName) throws URISyntaxException {
        // HTTP
        System.setProperty("http.proxyHost", proxyServer);
        System.setProperty("http.proxyPort", proxyPort);

        // HTTPS
        System.setProperty("https.proxyHost", proxyServer);
        System.setProperty("https.proxyPort", proxyPort);

        final File excelFile = new File(this.getClass().getClassLoader().getResource(fileName).toURI());

        ExcelImporter excelImporter = new ExcelImporter(
                server,
                sharedSpace,
                workspace,
                excelFile,
                user
        );

        excelImporter.init(authentication);

        excelImporter.migrate();
    }
}
