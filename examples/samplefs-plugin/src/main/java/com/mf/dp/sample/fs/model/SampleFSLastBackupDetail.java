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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mf.dp.uic.model.LastBackupDetail;
import com.mf.dp.uic.util.JsonUtil;

public class SampleFSLastBackupDetail {

	private SampleFSObjectVerOptions objectVerOptions;
	private String backupType;
	
	public SampleFSLastBackupDetail (LastBackupDetail detail) throws JsonProcessingException {
		if(!StringUtils.isBlank(detail.getObjectVerOptionsAsJsonStr()))	
			this.objectVerOptions = JsonUtil.deserializeFromJson(detail.getObjectVerOptionsAsJsonStr(), SampleFSObjectVerOptions.class);
		this.backupType = detail.getBackupType();
	}
	
	public SampleFSObjectVerOptions getObjectVerOptions() {
		return objectVerOptions;
	}
	public void setObjectVerOptions(SampleFSObjectVerOptions objectVerOptions) {
		this.objectVerOptions = objectVerOptions;
	}
	public String getBackupType() {
		return backupType;
	}
	public void setBackupType(String backupType) {
		this.backupType = backupType;
	}	
	
}
