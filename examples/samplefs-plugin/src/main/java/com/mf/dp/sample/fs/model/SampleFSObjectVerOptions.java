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
package com.mf.dp.sample.fs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mf.dp.uic.model.ObjectVerOptions;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleFSObjectVerOptions extends ObjectVerOptions {

    private String backupType;
	private long backupTime;
	
	public String getBackupType() {
		return backupType;
	}
	
	public void setBackupType(String backupType) {
		this.backupType = backupType;
	}
	
	public long getBackupTime() {
		return backupTime;
	}
	
	public void setBackupTime(long backupTime) {
		this.backupTime = backupTime;
	}
	
}
