package com.jio.subscriptionengine.crm.modules.upload.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.jio.subscriptionengine.crm.core.BaseResponse;
import com.jio.subscriptionengine.crm.core.DispatcherBaseController;
import com.jio.subscriptionengine.crm.core.HttpRequestMethod;
import com.jio.subscriptionengine.crm.core.annotations.Controller;
import com.jio.subscriptionengine.crm.core.annotations.EventName;
import com.jio.subscriptionengine.crm.core.annotations.RequestMapping;
import com.jio.subscriptionengine.crm.countermanager.CounterNameEnum;
import com.jio.subscriptionengine.crm.exceptions.BaseException;
import com.jio.subscriptionengine.crm.logger.DappLoggerService;
import com.jio.subscriptionengine.crm.modules.upload.service.FileUploadService;
import com.jio.subscriptionengine.crm.utils.ObjectMapperHelper;

/**
 * @author Alpesh.Sonar
 * @modified Ghajnafar.Shahid
 *
 */
@Controller
@RequestMapping(name = "/file-upload")
public class FileUploadController implements DispatcherBaseController {

	private static final long serialVersionUID = 1L;
	private FileUploadService fileUploadService = new FileUploadService();

	/**
	 * @param req
	 * @param resp
	 * @return
	 * @throws BaseException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@EventName("GET_FILE")
	@RequestMapping(name = "/get-file", type = HttpRequestMethod.GET)
	public BaseResponse<?> getCustomerList(HttpServletRequest req, HttpServletResponse resp)
			throws BaseException, FileNotFoundException, IOException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		CounterNameEnum.CNTR_GET_COLUMN_VALUES_REQUEST.increment();

		String fileName = req.getParameter("file-name");
		String orderId = req.getParameter("order-id");
		String fileDownloadName = req.getParameter("actual-file-name");
		this.fileUploadService.getFile(resp, fileName, orderId, fileDownloadName);
//		final BaseResponse<Object> baseResponse = new BaseResponse<>();
		return null;

	}

	/**
	 * @param req
	 * @param resp
	 * @return
	 * @throws BaseException
	 * @throws IOException
	 */
	@EventName("UPLOAD_FILE")
	@RequestMapping(name = "/upload", type = HttpRequestMethod.POST)
	public BaseResponse<?> uploadFile(HttpServletRequest req, HttpServletResponse resp)
			throws BaseException, IOException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		CounterNameEnum.CNTR_UPLOAD_FILE_REQUEST.increment();
		if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data")) {
			String fileName = req.getParameter("file-name");
			BaseResponse<Map<String, String>> response = this.fileUploadService.uploadFile(req, fileName);
			response.setMessage("File Uploaded Successfully");
			return response;
		}

		throw new BaseException(HttpStatus.BAD_REQUEST_400, "Use multipart/form-data for upload");
	}

	/**
	 * @param req
	 * @param resp
	 * @return
	 * @throws BaseException
	 * @throws IOException
	 */
	@EventName("UPLOAD_UOL_FILE")
	@RequestMapping(name = "/upload-uol", type = HttpRequestMethod.POST)
	public BaseResponse<?> uploadUolFile(HttpServletRequest req, HttpServletResponse resp)
			throws BaseException, IOException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		CounterNameEnum.CNTR_UPLOAD_UOL_FILE_REQUEST.increment();
		if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data")) {
			String fileName = req.getParameter("file-name");
			BaseResponse<Map<String, String>> response = this.fileUploadService.uploadUolFile(req, fileName);
			response.setMessage("File Uploaded Successfully");
			return response;
		}

		throw new BaseException(HttpStatus.BAD_REQUEST_400, "Use multipart/form-data for upload");
	}

	/**
	 * @param req
	 * @param resp
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws BaseException
	 */
	@EventName("VALIDATE_COLUMN")
	@RequestMapping(name = "/validate", type = HttpRequestMethod.POST)
	public BaseResponse<?> validateColumns(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, BaseException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		CounterNameEnum.CNTR_VALIDATE_COLUMN_REQUEST.increment();

		String fileName = req.getParameter("file-name");
		String orderId = req.getParameter("order-id");
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> reqJson = ObjectMapperHelper.getInstance().getEntityObjectMapper()
				.readValue(req.getReader(), Map.class);
		Map<String, String> columnList = reqJson.get("column");
		BaseResponse<Map<String, Boolean>> response = this.fileUploadService.validateColumns(orderId, fileName,
				columnList);
		response.setMessage("File Validation Successfully");

		return response;
	}
	/**
	 * @param req
	 * @param resp
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws BaseException
	 */
	@EventName("DELETE_FILE")
	@RequestMapping(name = "/delete", type = HttpRequestMethod.DELETE)
	public BaseResponse<?> deleteFile(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, BaseException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		CounterNameEnum.CNTR_DELETE_FILE_REQUEST.increment();

		String fileName = req.getParameter("file-name");
		String orderId = req.getParameter("order-id");
		BaseResponse<Map<String, String>> response=this.fileUploadService.deleteFile(orderId, fileName);
		response.setMessage(String.format("File: %s  Deleted Successfully", fileName));
		return response;
	}
}
