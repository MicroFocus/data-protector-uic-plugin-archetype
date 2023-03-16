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

	private long restoreTime;
	private String dataDirPath;
	private String logDirPath;
	
	public long getRestoreTime() {
		return restoreTime;
	}
	public void setRestoreTime(long restoreTime) {
		this.restoreTime = restoreTime;
	}
	public String getDataDirPath() {
		return dataDirPath;
	}
	public void setDataDirPath(String dataDirPath) {
		this.dataDirPath = dataDirPath;
	}
	public String getLogDirPath() {
		return logDirPath;
	}
	public void setLogDirPath(String logDirPath) {
		this.logDirPath = logDirPath;
	}

}
