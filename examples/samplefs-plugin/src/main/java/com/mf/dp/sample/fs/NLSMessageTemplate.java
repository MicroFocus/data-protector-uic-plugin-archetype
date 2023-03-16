/*
 * Copyright (c) 2023 Micro Focus or one of its affiliates.
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mf.dp.sample.fs;

public class NLSMessageTemplate {

	private NLSMessageTemplate() {
		throw new IllegalStateException("Constant class");
	}

	public static final String NLS_START_FULL_BACKUP					= "Starting full backup.";
	public static final String NLS_START_INCREMENTAL_BACKUP 			= "Starting incremental backup.";
	public static final String NLS_GET_LAST_BACKUP_DETAILS 				= "Retrieving last backup details.";
	public static final String NLS_FAILED_TO_OBTAIN_LAST_BACKUP_DETAIL 	= "Could not obtain last backup details:\n\t\t%s";
	public static final String NLS_INCREMENTAL_REQUIRES_PREVIOUS_BACKUP = "Incremental backup requires previous backup.";
	public static final String NLS_FAILED_TO_PREPARE_FOR_BACKUP 		= "Could not prepare for backup:\n\t\t%s";
	public static final String NLS_LOG_DIR_REMOVED 						= "The log directory %s is removed.";
	public static final String NLS_FAILED_TO_REMOVE_LOG_DIR 			= "Could not remove the log directory %s:\n\t\t%s";
	public static final String NLS_TRANSFER_TO_MA 						= "Transferring backup data to Media Agent. This may take a while.";
	public static final String NLS_TRANSFER_TO_MA_COMPLETED 			= "Transfer of backup data to Media Agent is complete.";
	public static final String NLS_TRANSFER_TO_MA_FAILED 				= "Could not transfer backup data to Media Agent:\n\t\t%s";
	public static final String NLS_INPUT_VALIDATION_FAILED 				= "Input validation failed:\n\t\t%s";
	public static final String NLS_START_RESTORE 						= "Starting restore.";
	public static final String NLS_TRANSFER_FROM_MA 					= "Transferring backup data from Media Agent. This may take a while.";
	public static final String NLS_TRANSFER_FROM_MA_COMPLETED 			= "Transfer of backup data from Media Agent is complete.";
	public static final String NLS_TRANSFER_FROM_MA_FAILED 				= "Could not transfer backup data from Media Agent:\n\t\t%s";
	public static final String NLS_UNEXPECTED_ERROR 					= "Unexpected error occurred:\n\t\t%s";
	public static final String NLS_APPLYING_INCREMENTAL 				= "There are %s incremental backups to apply.";
	public static final String NLS_APPLYING_INCREMENTAL_COMPLETED 		= "All incremental backups have been applied.";
	public static final String NLS_FAILED_TO_APPLY_INCREMENTAL 			= "Could not apply incremental backups:\n\t\t%s";
	public static final String NLS_FAILED_TO_PREPARE_FOR_RESTORE 		= "Could not prepare for restore:\n\t\t%s";
}
