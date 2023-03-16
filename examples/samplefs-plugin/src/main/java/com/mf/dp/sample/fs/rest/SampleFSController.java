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
package com.mf.dp.sample.fs.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mf.dp.sample.fs.model.FileEntry;
import com.mf.dp.sample.fs.service.FSService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "SampleFS")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping(path = "/samplefs")
public class SampleFSController {
	
	@Autowired
	private FSService fsService;
	
	@Operation(summary = "Get directory listing")
	@GetMapping(path="/entries/{dirPath}", produces = "application/json")
	public List<FileEntry> list(@PathVariable("dirPath") String dirPath,
			@RequestParam(name="followSymbolicLinks", required = false, defaultValue="true") boolean followSymbolicLinks) {
		return fsService.getDirectoryList(dirPath, followSymbolicLinks);
	}
	
}
