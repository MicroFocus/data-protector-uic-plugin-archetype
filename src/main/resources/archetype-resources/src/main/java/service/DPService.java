package ${package}.service;

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

import ${package}.Constant;
import ${package}.model.BackupContext;
import ${package}.model.RestoreContext;
import ${package}.model.${pluginName}BackupRequest;
import ${package}.model.${pluginName}LastBackupDetail;
import ${package}.model.${pluginName}RestoreRequest;

@Component
public class DPService {

	@Autowired
	private UDMService udmService;

	public void sendFullBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) throws ServiceException {
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

	public void sendIncrBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) throws ServiceException {
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
	
	public void receiveBackupFromDP(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext context) throws ServiceException {
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

	public ${pluginName}LastBackupDetail getLastBackupDetail(${pluginName}BackupRequest request) throws ServiceException {
		try {
			LastBackupDetail detail = udmService.getLastBackupDetail(
					Constant.PLUGIN_NAME,
					request.getAppOptions().getAppName(),
					request.getAppOptions().getAppId(),
					request.getAppHost());
			return new ${pluginName}LastBackupDetail(detail);
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}
	}
}
