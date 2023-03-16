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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mf.dp.uic.exception.ServiceException;
import com.mf.dp.uic.model.LastBackupDetail;
import com.mf.dp.uic.plugin.spi.IProgressStatus;
import com.mf.dp.uic.service.UDMService;
import com.mf.dp.uic.service.UDMService.ReceiveRequest;
import com.mf.dp.uic.service.UDMService.SendRequest;
import com.mf.dp.uic.service.UDMService.SendRequest.SendRequestBuilder;
import com.mf.dp.uic.util.Base64Util;
import com.mf.dp.uic.util.ExceptionUtil;
import com.mf.dp.uic.util.JsonUtil;

import com.mf.dp.sample.fs.Constant;
import com.mf.dp.sample.fs.model.BackupContext;
import com.mf.dp.sample.fs.model.RestoreContext;
import com.mf.dp.sample.fs.model.SampleFSBackupRequest;
import com.mf.dp.sample.fs.model.SampleFSLastBackupDetail;
import com.mf.dp.sample.fs.model.SampleFSRestoreRequest;

@Component
public class DPService {
	
	@Autowired
	private UDMService udmService;

	public void sendFullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) throws ServiceException {
		try {
			SendRequestBuilder requestBuilder = SendRequest.builder()
					.backupType(request.getBackupType())
					.dataDir(context.getDataDirPath())
					.logDir(context.getLogDirPath())
					.barlist(request.getBarlist())
					.appType(Constant.PLUGIN_NAME)
					.appName(request.getAppOptions().getAppName())
					.appId(request.getAppOptions().getAppId())
					.sourceClient(request.getAppHost())
					.base64EncodedObjectOptions(Base64Util.base64Encode(JsonUtil.serializeToJson(context.getObjectOptions())))
					.base64EncodedObjectVerOptions(Base64Util.base64Encode(JsonUtil.serializeToJson(context.getObjectVerOptions())));
			if(request.getConcurrency() != null)
				requestBuilder.concurrency(request.getConcurrency());
	
			udmService.sendBackupToDP(status, requestBuilder.build());
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}	
	}

	public void sendIncrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) throws ServiceException {
		try {
			SendRequestBuilder requestBuilder = SendRequest.builder()
					.backupType(request.getBackupType())
					.logDir(context.getLogDirPath())
					.barlist(request.getBarlist())
					.appType(Constant.PLUGIN_NAME)
					.appName(request.getAppOptions().getAppName())
					.appId(request.getAppOptions().getAppId())
					.sourceClient(request.getAppHost())
					.base64EncodedObjectOptions(Base64Util.base64Encode(JsonUtil.serializeToJson(context.getObjectOptions())))
					.base64EncodedObjectVerOptions(Base64Util.base64Encode(JsonUtil.serializeToJson(context.getObjectVerOptions())));
			if(request.getConcurrency() != null)
				requestBuilder.concurrency(request.getConcurrency());
	
			udmService.sendBackupToDP(status, requestBuilder.build());
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}	
	}
	
	public void receiveBackupFromDP(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext context) throws ServiceException {
		ReceiveRequest mockedRequest = ReceiveRequest.builder()
				.dataDir(context.getDataDirPath())
				.logDir(context.getLogDirPath())
				.barlist(request.getBarlist())
				.appType(Constant.PLUGIN_NAME)
				.appName(request.getAppOptions().getAppName())
				.appId(request.getAppOptions().getAppId())
				.sourceClient(request.getAppHost())
				// DO NOT use request.getSessionId() as it will be INCORRECT due to extra backslashes added by poco library during serialization
				.sessionId(request.getAppOptions().getSessionId())
				.build();
		
		udmService.receiveBackupFromDP(status, mockedRequest);
	}

	public SampleFSLastBackupDetail getLastBackupDetail(SampleFSBackupRequest request) throws ServiceException {
		try {
			LastBackupDetail detail = udmService.getLastBackupDetail(
					Constant.PLUGIN_NAME,
					request.getAppOptions().getAppName(),
					request.getAppOptions().getAppId(),
					request.getAppHost());
			return new SampleFSLastBackupDetail(detail);
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}
	}
}
