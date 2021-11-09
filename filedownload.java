package com.jio.subscriptionengine.crm.modules.upload.service;

/**
 * 
 * @author Ghajnafar.Shahid
 *
 */
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.eclipse.jetty.http.HttpStatus;

import com.jio.subscriptionengine.crm.clearCodes.ClearCodeLevel;
import com.jio.subscriptionengine.crm.clearCodes.ClearCodes;
import com.jio.subscriptionengine.crm.configurationManager.ConfigParamsEnum;
import com.jio.subscriptionengine.crm.core.BaseResponse;
import com.jio.subscriptionengine.crm.countermanager.CounterNameEnum;
import com.jio.subscriptionengine.crm.exceptions.BaseException;
import com.jio.subscriptionengine.crm.hdfs.HdfsConfigurationManager;
import com.jio.subscriptionengine.crm.hdfs.HdfsUtilsService;
import com.jio.subscriptionengine.crm.logger.DappLoggerService;
import com.jio.subscriptionengine.crm.modules.draft.service.DraftService;
import com.jio.subscriptionengine.crm.modules.upload.helper.UOLColumnEnum;
import com.jio.subscriptionengine.crm.node.startup.CrmBootStrapper;
import com.jio.subscriptionengine.crm.utils.MimeTypes;
import com.jio.subscriptions.modules.bean.Order;
import com.jio.telco.framework.clearcode.ClearCodeAsnPojo;
import com.jio.telco.framework.clearcode.ClearCodeAsnUtility;

import au.com.bytecode.opencsv.CSVReader;

public class FileUploadService {

	private static final String CONTENT_DISPOSIOTION = "Content-Disposition";
	private static final String CONTENT_ATTACHMENT = "inline; filename=\"";
	private static DraftService draftService = new DraftService();

	/**
	 * @param req
	 * @param uiFileName
	 * @return
	 * @throws IOException
	 * @throws BaseException
	 */
	public BaseResponse<Map<String, String>> uploadFile(HttpServletRequest req, String uiFileName)
			throws BaseException {
		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();
		final ClearCodeAsnPojo ccAsnPojo = CrmBootStrapper.getInstance().getClearCodeObj();

		String orderId = req.getParameter("order-id");
		if (orderId == null || orderId.isEmpty()) {
			orderId = "temp";
		}

		// get file extensions
		String extension = FilenameUtils.getExtension(uiFileName);

		// get base file Name
		String fileNameTemp = FilenameUtils.getBaseName(uiFileName);
		// generate UUID file name
		// String file = ElasticUUIDGenerator.getInstance().getUniqueUUID();
		Date date = new Date();
		String file = fileNameTemp + "_" + date.getTime();
		String fileName = file + "." + extension;

		// constructs path of the directory to save uploaded file
		String uploadFilePath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId;

		// write file in HDFS
		try {
			byte[] byteArray = IOUtils.toByteArray(req.getInputStream());

			Configuration conf = HdfsConfigurationManager.getConfiguration();

			FileSystem fs = FileSystem.get(conf);
			Path path = new Path(uploadFilePath + "/" + fileName);
			try (FSDataOutputStream out = fs.create(path)) {
				out.write(byteArray);
				out.close();
			}
		} catch (Exception e) {
			CounterNameEnum.CNTR_UPLOAD_FILE_FAILURE.increment();
			ccAsnPojo.addClearCode(ClearCodes.UPLOAD_FILE_FAILURE.getValue(), ClearCodeLevel.FAILURE);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "File writting Exception");
		}

		Map<String, String> fileObject = new HashMap<>();
		fileObject.put("file-name", fileName);
		fileObject.put("actual-file-name", uiFileName);

		final BaseResponse<Map<String, String>> baseResponse = new BaseResponse<>(fileObject);
		CounterNameEnum.CNTR_UPLOAD_FILE_SUCCESS.increment();
		ccAsnPojo.addClearCode(ClearCodes.UPLOAD_FILE_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
		ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);

		return baseResponse;
	}

	/**
	 * @param req
	 * @param uiFileName
	 * @return
	 * @throws IOException
	 * @throws BaseException
	 */
	public BaseResponse<Map<String, String>> uploadUolFile(HttpServletRequest req, String uiFileName)
			throws BaseException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();

		final ClearCodeAsnPojo ccAsnPojo = CrmBootStrapper.getInstance().getClearCodeObj();

		String orderId = req.getParameter("order-id");

		// get file extensions
		String extension = FilenameUtils.getExtension(uiFileName);
		String fileNameTemp = FilenameUtils.getBaseName(uiFileName);

		// generate UUID file name
		// String file = ElasticUUIDGenerator.getInstance().getUniqueUUID();
		Date date = new Date();
		String file = fileNameTemp + "_" + date.getTime();
		String fileName = file + "." + extension;

		// constructs path of the directory to save uploaded file
		String uploadFilePath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId;

		// write file in HDFS
		try {
			byte[] byteArray = IOUtils.toByteArray(req.getInputStream());

			Map<String, String> columns = Arrays.stream(UOLColumnEnum.values())
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

			// checking validation of UOL file
			if (!validateColumns(columns, byteArray)) {
				CounterNameEnum.CNTR_UPLOAD_UOL_FILE_FAILURE.increment();
				throw new BaseException(HttpStatus.BAD_REQUEST_400, "UOL File is not Valid");
			}
			Configuration conf = HdfsConfigurationManager.getConfiguration();

			FileSystem fs = FileSystem.get(conf);
			Path path = new Path(uploadFilePath + "/" + fileName);
			try (FSDataOutputStream out = fs.create(path)) {
				out.write(byteArray);
				out.close();
			}

		} catch (IOException e) {
			CounterNameEnum.CNTR_UPLOAD_UOL_FILE_FAILURE.increment();
			ccAsnPojo.addClearCode(ClearCodes.UPLOAD_UOL_FILE_FAILURE.getValue(), ClearCodeLevel.FAILURE);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "File writting Exception");
		}

		Map<String, String> fileObject = new HashMap<>();
		fileObject.put("file-name", fileName);
		fileObject.put("actual-file-name", uiFileName);

		final BaseResponse<Map<String, String>> baseResponse = new BaseResponse<>(fileObject);
		CounterNameEnum.CNTR_UPLOAD_UOL_FILE_SUCCESS.increment();
		ccAsnPojo.addClearCode(ClearCodes.UPLOAD_UOL_FILE_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
		ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);

		return baseResponse;
	}

	/**
	 * @param response
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws BaseException
	 */
	public void getFile(HttpServletResponse response, String fileName, String orderId, String fileDownloadName)
			throws FileNotFoundException, IOException, BaseException {

		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();

		final ClearCodeAsnPojo ccAsnPojo = CrmBootStrapper.getInstance().getClearCodeObj();

		// check for order id
		if (orderId == null || orderId.isEmpty()) {
			orderId = "temp";
		}
		String targetPath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId + "/" + fileName;

		// getting file from HDFS
		Configuration conf = HdfsConfigurationManager.getConfiguration();

		FileSystem fileSystem = FileSystem.get(conf);
		Path path = new Path(targetPath);
		if (fileSystem.exists(path)) {

			try (FSDataInputStream in = fileSystem.open(path)) {
				long length = fileSystem.getFileStatus(path).getLen();
				String extension = FilenameUtils.getExtension(fileName);
				String contentType = MimeTypes.getMimeType(extension);
				response.setContentType(contentType);

				if (fileDownloadName != null) {
					fileName = fileDownloadName + "." + extension;
				}
				response.setHeader(CONTENT_DISPOSIOTION, CONTENT_ATTACHMENT + fileName + "\"");
				response.setContentLength((int) length);

				OutputStream os = response.getOutputStream();
				try {

					byte[] buffer = new byte[1024];
					int numBytes = 0;
					while ((numBytes = in.read(buffer)) > 0) {
						os.write(buffer, 0, numBytes);
					}

				} finally {
					os.flush();
					os.close();
					in.close();
					fileSystem.close();
				}
				ccAsnPojo.addClearCode(ClearCodes.GET_COLUMN_VALUES_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
				ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
				CounterNameEnum.CNTR_GET_COLUMN_VALUES_SUCCESS.increment();
			}
		} else {
			CounterNameEnum.CNTR_GET_COLUMN_VALUES_FAILURE.increment();
			ccAsnPojo.addClearCode(ClearCodes.GET_COLUMN_VALUES_FAILURE.getValue(), ClearCodeLevel.FAILURE);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "File not found");

		}
	}

	/**
	 * @param orderId
	 * @param fileName
	 * @param columns
	 * @return
	 * @throws BaseException
	 * @throws IOException
	 */
	public BaseResponse<Map<String, Boolean>> validateColumns(String orderId, String fileName,
			Map<String, String> columns) throws BaseException, IOException {
		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();

		final ClearCodeAsnPojo ccAsnPojo = CrmBootStrapper.getInstance().getClearCodeObj();

		String targetPath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId + "/" + fileName;

		byte[] buffer = HdfsUtilsService.getInstance().readFile(targetPath);
		CSVReader csvReader = null;
		BaseResponse<Map<String, Boolean>> resp = null;
		try {

			// converting byte array to File Reader
			InputStream input = new ByteArrayInputStream(buffer);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
			csvReader = new CSVReader(bufferedReader);
			String[] nextRecord;
			List<String> headerList = new ArrayList<String>();
			Map<String, Boolean> response = new HashMap<String, Boolean>();
			// we are going to read data line by line

			if ((nextRecord = csvReader.readNext()) != null) {
				for (String cell : nextRecord) {
					System.out.println(cell);
					headerList.add(cell);
				}
				if (validateColumns(columns, headerList)) {
					response.put("validation", true);
				} else {
					response.put("validation", false);
				}
				;
			} else {
				response.put("validation", false);
			}

			resp = new BaseResponse<Map<String, Boolean>>(response);
			CounterNameEnum.CNTR_VALIDATE_COLUMN_SUCCESS.increment();
			ccAsnPojo.addClearCode(ClearCodes.VALIDATE_COLUMN_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
		} catch (IOException e) {
			CounterNameEnum.CNTR_VALIDATE_COLUMN_FAILURE.increment();
			ccAsnPojo.addClearCode(ClearCodes.VALIDATE_COLUMN_FAILURE.getValue(), ClearCodeLevel.FAILURE);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "File not found");
		} finally {
			if (csvReader != null) {
				csvReader.close();
			}
		}
		return resp;

	}

	/**
	 * @param orderId
	 * @param fileName
	 * @param columns
	 * @return
	 * @throws BaseException
	 * @throws IOException
	 */
	public List<Map<String, Object>> getColumnsValues(String orderId, String fileName, Map<String, String> columns)
			throws BaseException, IOException {
		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();

		final ClearCodeAsnPojo ccAsnPojo = CrmBootStrapper.getInstance().getClearCodeObj();

		String targetPath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId + "/" + fileName;

		byte[] buffer = HdfsUtilsService.getInstance().readFile(targetPath);
		CSVReader csvReader = null;

		// making response object
		List<Map<String, Object>> resp = new ArrayList<Map<String, Object>>();

		try {

			// converting byte array to File Reader
			InputStream input = new ByteArrayInputStream(buffer);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));

			// converting into CSVReader
			csvReader = new CSVReader(bufferedReader);

			// Reading csv File and getting required component values
			List<String[]> rows = csvReader.readAll();
			if (rows.size() > 1) {
				List<String> headerList = Arrays.asList(rows.get(0));
				if (validateColumns(columns, headerList)) {
					Map<String, Integer> columnIndexMapping = getColumnsIndexMapping(columns, headerList);
					rows.remove(0);
					for (String row[] : rows) {
						Map<String, Object> obj = new HashMap<String, Object>();
						columnIndexMapping.forEach((key, index) -> {
							obj.put(key, filterResult(row, index));
						});
						resp.add(obj);
					}
				} else {
					CounterNameEnum.CNTR_GET_COLUMN_VALUES_FAILURE.increment();
					ccAsnPojo.addClearCode(ClearCodes.GET_COLUMN_VALUES_FAILURE.getValue(), ClearCodeLevel.FAILURE);
					ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
					throw new BaseException(HttpStatus.BAD_REQUEST_400, "Column is missing");
				}
			} else {
				CounterNameEnum.CNTR_GET_COLUMN_VALUES_FAILURE.increment();
				ccAsnPojo.addClearCode(ClearCodes.GET_COLUMN_VALUES_FAILURE.getValue(), ClearCodeLevel.FAILURE);
				ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
				throw new BaseException(HttpStatus.BAD_REQUEST_400, "Data Rows Not Present");
			}
			CounterNameEnum.CNTR_GET_COLUMN_VALUES_SUCCESS.increment();
			ccAsnPojo.addClearCode(ClearCodes.GET_COLUMN_VALUES_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);

		} catch (IOException e) {
			CounterNameEnum.CNTR_GET_COLUMN_VALUES_FAILURE.increment();
			ccAsnPojo.addClearCode(ClearCodes.GET_COLUMN_VALUES_FAILURE.getValue(), ClearCodeLevel.FAILURE);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "File not found");
		} finally {
			if (csvReader != null) {
				csvReader.close();
			}
		}
		return resp;
	}

	private String filterResult(String[] row, Integer index) {

		String data = String.valueOf(row[index]);
		if (data.startsWith("`"))
			return data.substring(1, data.length());
		return data;
	}

	/**
	 * @param columns
	 * @param headerList
	 * @return
	 */
	public boolean validateColumns(Map<String, String> columns, List<String> headerList) {
		for (String column : columns.keySet()) {
			if (headerList.contains(columns.get(column))) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param columns
	 * @param headerList
	 * @return
	 */
	public Map<String, Integer> getColumnsIndexMapping(Map<String, String> columns, List<String> headerList) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (String column : columns.keySet()) {
			int index = headerList.indexOf(columns.get(column));
			map.put(column, index);
		}

		return map;
	}

	/**
	 * @param columns
	 * @param buffer
	 * @return
	 * @throws IOException
	 * @throws BaseException
	 */
	public boolean validateColumns(Map<String, String> columns, byte buffer[]) throws IOException, BaseException {
		boolean isValidated = false;
		CSVReader csvReader = null;
		try {
			// converting byte array to File Reader
			InputStream input = new ByteArrayInputStream(buffer);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));

			// converting into CSVReader
			csvReader = new CSVReader(bufferedReader);

			// Reading csv File and getting required column values
			List<String[]> rows = csvReader.readAll();
			if (rows.size() > 0) {
				List<String> headerList = Arrays.asList(rows.get(0));
				isValidated = validateColumns(columns, headerList);
			}
		} catch (Exception e) {
			CounterNameEnum.CNTR_UPLOAD_UOL_FILE_FAILURE.increment();
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "UOL File is not Valid");
		} finally {
			if (csvReader != null) {
				csvReader.close();
			}
		}

		return isValidated;
	}

	public boolean moveFileFromTemp(Order order) throws Exception {

		boolean isMoved = false;
		String orderId = order.getOrderId();

		try {
			Map<String, String> docList = this.draftService.getListOfOrderDocument(orderId);
			List<String> docIds = new ArrayList<String>(docList.values());
			for (String docId : docIds) {
				String srcPath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/temp/" + docId;
//				byte bytes[] = HdfsUtilsService.getInstance().readFile(srcPath);
				String destPath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId;
//				HdfsUtilsService.getInstance().writeByte(bytes, destPath, docId);
//				HdfsUtilsService.getInstance().deleteFile(srcPath);
				HdfsUtilsService.getInstance().renamePath(srcPath, destPath, docId);
				isMoved = true;
			}

		} catch (IOException e) {
			isMoved = false;
			throw new BaseException(HttpStatus.BAD_REQUEST_400, "File Not Found Exception");
		}
		return isMoved;
	}

	public BaseResponse<Map<String, String>> deleteFile(String orderId, String fileName) throws IOException, BaseException {
		// logger
		DappLoggerService.GENERAL_INFO_LOG
				.getLogBuilder(
						"Executing [ " + this.getClass().getName() + "."
								+ Thread.currentThread().getStackTrace()[1].getMethodName() + " ]",
						this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName())
				.writeLog();

		final ClearCodeAsnPojo ccAsnPojo = CrmBootStrapper.getInstance().getClearCodeObj();

		// check for order id
		if (orderId == null || orderId.isEmpty()) {
			orderId = "temp";
		}
		String targetPath = ConfigParamsEnum.FILE_UPLOAD_DIR.getStringValue() + "/" + orderId + "/" + fileName;
		try {
			HdfsUtilsService.getInstance().deleteFile(targetPath);
			CounterNameEnum.CNTR_DELETE_FILE_SUCCESS.increment();
			ccAsnPojo.addClearCode(ClearCodes.DELETE_FILE_SUCCESS.getValue(), ClearCodeLevel.SUCCESS);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
		} catch (BaseException e) {
			CounterNameEnum.CNTR_DELETE_FILE_FAILURE.increment();
			ccAsnPojo.addClearCode(ClearCodes.DELETE_FILE_FAILURE.getValue(), ClearCodeLevel.FAILURE);
			ClearCodeAsnUtility.getASNInstance().writeASNForClearCode(ccAsnPojo);
			throw e;
		}
		Map<String, String> fileObject = new HashMap<>();
		BaseResponse<Map<String, String>> response = new BaseResponse<Map<String,String>>(fileObject);
		return response;
	}

}
