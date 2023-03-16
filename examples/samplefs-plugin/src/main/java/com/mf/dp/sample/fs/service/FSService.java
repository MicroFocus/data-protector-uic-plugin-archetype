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
package com.mf.dp.sample.fs.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mf.dp.sample.fs.model.FileEntry;
import com.mf.dp.uic.command.BaseCommand;
import com.mf.dp.uic.command.OSCommand;
import com.mf.dp.uic.exception.ServiceException;
import com.mf.dp.uic.process.ProcessConfigurator;
import com.mf.dp.uic.util.ExceptionUtil;

/**
 * This class provides limited set of file system related functionality needed by this plugin. 
 * 
 * There are a couple of key points to keep in mind:
 * 
 * <ol>
 * <li> {@code java.io} and {@code java.nio} packages provide rich set of file system
 *      related functionality built directly in the JDK that would have been a far superior 
 *      alternative to rolling our own using platform specific OS commands. However, 
 *      because this program is running under <em>dpuic</em> user, it may not have access
 *      to all files and directories necessary for backup and restore operations.
 *      For that reason, file system access method must be implemented using OS commands
 *      in conjunction with the use of <em>sudo</em> capability supported by the Unified
 *      Integration Controller environment. From the productivity point of view, this may
 *      impose some limitation that the plugin developers should be aware of.
 *      
 * <li> The primary purpose of this implementation is simply to illustrate via examples
 *      some of the important aspects of the SDK. The implementation of the functionality
 *      itself is inefficient and incomplete, and should not be considered to be anything
 *      more than an example.
 * </ol>
 *
 */
@Component
public class FSService {
	
	private static final String FIND 	= "/usr/bin/find";
	
	private static Logger logger = LoggerFactory.getLogger(FSService.class); 
	
	// Inject a property value from the configuration file (samplefs.properties).
	@Value("${samplefs.fsservice.script.copy-files}")
	private String copyFilesScript;

	@Value("${samplefs.fsservice.input-method:pipe}")
	private String inputMethod;

	public List<FileEntry> getDirectoryList(String dirPath, boolean followSymbolicLinks) throws ServiceException {	
		assertDirectory(dirPath, followSymbolicLinks, false);
		
		return getList(dirPath, followSymbolicLinks);
	}

	public void assertDirectory(String dirPath, boolean followSymbolicLinks, boolean shouldBeEmpty) throws ServiceException {
		try {
			if(!new OSCommand().queryWithFind(dirPath, followSymbolicLinks, "d", shouldBeEmpty)) {
				throw new ServiceException(HttpStatus.BAD_REQUEST, dirPath + " does not denote an existing and empty directory");
			}
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}
	}
	
	public void copyFilesModifiedSince(String srcDirPath, String dstDirPath, long since, boolean followSymbolicLinks) throws ServiceException {
		try {
			OSCommand osCommand = new OSCommand();
			
			StringBuilder command = new StringBuilder(FIND);
			if(followSymbolicLinks)
				command.append(" -L");
			command.append(" \"")
			.append(srcDirPath)
			.append("\" -type f -printf \"%T@ %p\\n\"");
	
			String out = osCommand.exec(command.toString());
			
			String filesModifiedSince = Arrays.stream(out.split(System.lineSeparator()))
			.map(line -> new FileItem(line.substring(0, 21), line.substring(22)))
			.filter(item -> item.getModTime() > since)
			.map(item -> item.filePath)
			.collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator();
			
			// This illustrates how to locate and execute custom script supplied by the plugin.
			if(inputMethod.equals("pipe")) {
				// This illustrates how to directly pipe a string value as input to the OS command to be invoked.
				command = new StringBuilder(copyFilesScript)
						.append(" \"")
						.append(srcDirPath)
						.append("\" \"")
						.append(dstDirPath)
						.append("\"");
				ProcessConfigurator configurator = ProcessConfigurator.create();
				configurator.pipedToStdin(filesModifiedSince);
				configurator.command(BaseCommand.withShell(command.toString()));
				out = osCommand.exec(configurator);
			} else if(inputMethod.equals("tempfile")) {
				// This alternate method illustrates how to use input redirection with a temporary file to the OS command to be invoked.
				File tempFile = writeToTempFile(filesModifiedSince);
				try {
					command = new StringBuilder(copyFilesScript)
							.append(" \"")
							.append(srcDirPath)
							.append("\" \"")
							.append(dstDirPath)
							.append("\" < \"")
							.append(tempFile)
							.append("\"");
					// To use shell features such as redirection, pipe, etc., must run with a shell
					out = osCommand.exec(BaseCommand.withShell(command.toString()));
					logger.info("{} files copied", out);
				} finally {
					FileUtils.deleteQuietly(tempFile);
				}							
			} else {
				throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Plugin is mis-configured");
			}
			
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}
	}
	
	public void copyContent(String sourceDirPath, String targetDirPath) throws ServiceException {
		try {
			// Make sure that the wildcard character (*) is outside of the quotes.
			// Otherwise, the shell will treat it as a file name.
			new OSCommand().copy("\"" + sourceDirPath + "\"/*", "\"" + targetDirPath + "\"", true, true, true);
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}
	}
	
	private List<FileEntry> getList(String dirPath, boolean followSymbolicLinks) throws ServiceException {
		try {
			StringBuilder command = new StringBuilder(FIND);
			if(followSymbolicLinks)
				command.append(" -L");
			command.append(" \"")
			.append(dirPath)
			.append("\" -maxdepth 1 -printf \"%y %p\\n\"");
	
			String out = new OSCommand().exec(command.toString());
			
			return Arrays.stream(out.split(System.lineSeparator()))
			.map(line -> new FileEntry(line.substring(2), "d".equals(line.substring(0,1))))
			.filter(entry -> !dirPath.equals(entry.getFilePath()))
			.collect(Collectors.toList());
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}
	}
	
	private File writeToTempFile(String value) throws IOException {
		File tempFile = File.createTempFile("samplefs", ".tmp");
		Files.write(tempFile.toPath(), value.getBytes(StandardCharsets.UTF_8));
		tempFile.deleteOnExit();
		return tempFile;
	}
	
	private static class FileItem {
		private long modTime;
		private String filePath;
		
		public FileItem(String linuxTimeWithFractionalPart, String filePath) {
			this.modTime = Long.parseLong(linuxTimeWithFractionalPart.substring(0, 10)) * 1000L + Long.parseLong(linuxTimeWithFractionalPart.substring(11, 14));
			this.filePath = filePath;
		}
		public long getModTime() {
			return modTime;
		}
	}
	
}
