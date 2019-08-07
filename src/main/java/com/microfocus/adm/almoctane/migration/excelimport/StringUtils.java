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

import org.slf4j.Logger;

import java.util.Arrays;

/**
 * This class contains methods useful to perform operations with strings.
 */
class StringUtils {

    /**
     * Creates a new String with the special characters escaped.
     *
     * @param inputString - The string which contains special characters.
     * @return - A new String with the special characters escaped.
     */
    public static String escapeMetaCharacters(String inputString) {
        final String[] metaCharacters = {/*"\\", "^", "$", "{", "}", "[", "]", "(", ")", ".", "*", "+", "?", "|", "<", ">", "-", "&", "%",*/  "\"", "'"};

        for (String metaCharacter : metaCharacters) {
            if (inputString.contains(metaCharacter)) {
                inputString = inputString.replace(metaCharacter, "\\" + metaCharacter);
            }
        }
        return inputString.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n");
    }

    /**
     * Logs the exception message.
     *
     * @param logger  - The logger used for logging messages
     * @param message - The message which will be logged
     * @param e       - The exception that was thrown
     */
    public static void logException(Logger logger, String message, Exception e) {
        logger.error(String.format("%s\n\tCaused by: %s\n\tMessage: %s\n\t%s",
                message,
                e.getCause(),
                e.getMessage(),
                Arrays.toString(e.getStackTrace())
        ));
    }
}
