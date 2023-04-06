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

public class RestoreContext {

	private long time;
	private String dataDirPath;
	private boolean deleteDataDirAfterUse = false;
	private String logDirPath;
	private boolean deleteLogDirAfterUse = false;
	
	public RestoreContext() {
		this.time = System.currentTimeMillis();
	}
	public long getTime() {
		return time;
	}
	public String getDataDirPath() {
		return dataDirPath;
	}
	public void setDataDirPath(String dataDirPath) {
		this.dataDirPath = dataDirPath;
	}
	public boolean isDeleteDataDirAfterUse() {
		return deleteDataDirAfterUse;
	}
	public void setDeleteDataDirAfterUse(boolean deleteDataDirAfterUse) {
		this.deleteDataDirAfterUse = deleteDataDirAfterUse;
	}
	public String getLogDirPath() {
		return logDirPath;
	}
	public void setLogDirPath(String logDirPath) {
		this.logDirPath = logDirPath;
	}
	public boolean isDeleteLogDirAfterUse() {
		return deleteLogDirAfterUse;
	}
	public void setDeleteLogDirAfterUse(boolean deleteLogDirAfterUse) {
		this.deleteLogDirAfterUse = deleteLogDirAfterUse;
	}

}
