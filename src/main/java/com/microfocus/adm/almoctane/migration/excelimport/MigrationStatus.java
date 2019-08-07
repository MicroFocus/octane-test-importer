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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class exposes a set of messages which can be used in order to provide an explicit status at the end of the
 * migration.
 *
 * The methods provided in this class can help compute and decide on the final status.
 */
final class MigrationStatus {
    /**
     * Status messages
     */
    public enum Status {
        EMPTY_FILE,
        INCORRECT_FILE,
        NOT_ALL_TESTS_WERE_MIGRATED,
        NOT_ALL_STEPS_WERE_UPLOADED,
        NOT_ALL_TESTS_AND_STEPS_WERE_MIGRATED,
        INIT_SCRIPT_UPLOAD_CLIENT_FAILED,
        INIT_SHEET_FAILED,
        INIT_OCTANE_FAILED,
        INIT_ENTITIES_FAILED,
        INIT_PHASES_FAILED,
        INIT_USER_TAGS_FAILED,
        CANNOT_MIGRATE,
        INIT_SUCCESS,
        SUCCESS
    }

    private static int migratedTests = 0;
    private static int uploadedSteps = 0;

    private static int failedTests = 0;
    private static AtomicInteger failedSteps = new AtomicInteger(0);

    /**
     * Increments the migratedTests
     */
    public static void addMigratedTest() {
        migratedTests++;
    }

    /**
     * Increments the uploadedSteps
     */
    public static void addUploadedStep() {
        uploadedSteps++;
    }

    /**
     * Increments the failedTests
     */
    public static void addFailedTest() {
        failedTests++;
    }

    /**
     * Increments the failedSteps
     */
    public static void addFailedStep() {
        failedSteps.addAndGet(1);
    }

    /**
     * @return migratedTests
     */
    public static int getMigratedTests() {
        return migratedTests;
    }

    /**
     *
     * @return uploadedSteps
     */
    public static int getUploadedSteps() {
        return uploadedSteps;
    }

    /**
     *
     * @return failedTests
     */
    public static int getFailedTests() {
        return failedTests;
    }

    /**
     *
     * @return failedSteps number
     */
    public static int getFailedSteps() {
        return failedSteps.get();
    }

    /**
     * Resets the value of the parameters.
     * This should be used after a migration has finished.
     */
    public static void reset() {
        failedSteps = new AtomicInteger(0);
        failedTests = 0;

        uploadedSteps = 0;
        migratedTests = 0;
    }
}
