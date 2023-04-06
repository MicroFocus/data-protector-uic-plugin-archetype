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

public class Constant {

	private Constant() {
		throw new IllegalStateException("Utility class");
	}

	public static final String PLUGIN_NAME = "SampleFS";

	public static final String BACKUP_TYPE_FULL = "full";
	
	public static final String BACKUP_TYPE_INCR = "incr";
}
