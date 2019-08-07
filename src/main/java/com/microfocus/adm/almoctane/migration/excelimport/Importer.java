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

import com.hpe.adm.nga.sdk.authentication.Authentication;
import com.hpe.adm.nga.sdk.authentication.SimpleClientAuthentication;

import java.io.File;
import java.net.URISyntaxException;

/**
 * This is the class from where the importer can be run.
 */
public class Importer {
    //change the server you want to migrate to
    private static final String server = "";

    //change the shared space
    private static final int sharedSpace = 0;
    //change the workspace
    private static final int workspace = 0;

    //change the user
    private static final String user = "";
    //change the password
    private static final String password = "";
    //change the proxy server
    private static final String proxyServer = "";
    //change the port
    private static final String proxyPort = "";

    //change the name of the file
    private static final String fileName = "";

    private static final Authentication authentication = new SimpleClientAuthentication(user, password);

    /**
     * After completing the fields from above, this method can be run to import the tests into Octane.
     *
     * @throws URISyntaxException - In case the path to the file is not correct.
     */
    public static void main(String[] args) throws URISyntaxException {
        // HTTP
        System.setProperty("http.proxyHost", proxyServer);
        System.setProperty("http.proxyPort", proxyPort);

        // HTTPS
        System.setProperty("https.proxyHost", proxyServer);
        System.setProperty("https.proxyPort", proxyPort);
        final File excelFile = new File(Importer.class.getClassLoader().getResource(fileName).toURI());

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
