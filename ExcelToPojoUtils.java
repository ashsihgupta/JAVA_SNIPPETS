package com.jio.crm.l2o.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.reflections.Reflections;

import com.jio.crm.l2o.beans.Lead;

public class ExcelToPojoUtils {
	public static final String BOOLEAN_TRUE = "1";
	public static final String LIST_SEPARATOR = ",";
	private static Map<String, Class<?>> ENTITY_LIST = new HashMap<>();
	private static String PACKAGE_NAME = "com.jio";

	@SuppressWarnings("unused")
	private static String strToFieldName(String str) {
		str = str.replaceAll("[^a-zA-Z0-9]", "");
		return str.length() > 0 ? str.substring(0, 1).toLowerCase() + str.substring(1) : null;
	}

	public static <T> List<T> toPojo(Class<?> type, InputStream inputStream)
			throws IOException, Exception, InvalidFormatException {
		List<T> results = new ArrayList<>();
		Workbook workbook = null;
		T clsInstance = null;

		try {
			workbook = WorkbookFactory.create(inputStream);
			Sheet sheet = workbook.getSheetAt(0);

			// header column names
			List<String> colNames = new ArrayList<>();
			Row headerRow = sheet.getRow(0);
			for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
				org.apache.poi.ss.usermodel.Cell headerCell = headerRow.getCell(i,
						org.apache.poi.ss.usermodel.Row.RETURN_BLANK_AS_NULL);
				colNames.add(headerCell != null
						? strToFieldName(((org.apache.poi.ss.usermodel.Cell) headerCell).getStringCellValue())
						: null);
			}

			for (int j = 1; j < sheet.getPhysicalNumberOfRows(); j++) {
				Row row = sheet.getRow(j);

				Object result = type.getDeclaredConstructor().newInstance();
				for (int k = 0; k < row.getPhysicalNumberOfCells(); k++) {
					if (colNames.get(k) != null) {
						org.apache.poi.ss.usermodel.Cell cell = row.getCell(k, Row.RETURN_BLANK_AS_NULL);
						if (cell != null) {
							DataFormatter formatter = new DataFormatter();
							String strValue = formatter.formatCellValue((org.apache.poi.ss.usermodel.Cell) cell);
							Field field = type.getDeclaredField(colNames.get(k));
							field.setAccessible(true);
							if (field != null) {
								Object value = null;
								if (field.getType().equals(Long.class)) {
									value = Long.valueOf(strValue);
								} else if (field.getType().equals(String.class)) {

									value = ((org.apache.poi.ss.usermodel.Cell) cell).getStringCellValue();
									if (value.equals("")) {
										value = null;
									}
								} else if (field.getType().equals(Integer.class)) {
									if (strValue.isEmpty()) {
										value = null;
									} else {
										value = Integer.valueOf(strValue);
									}
								} else if (field.getType().equals(LocalDate.class)) {
									value = LocalDate.parse(strValue);
								} else if (field.getType().equals(LocalDateTime.class)) {
									value = LocalDateTime.parse(strValue);
								} else if (field.getType().equals(boolean.class)) {
									value = BOOLEAN_TRUE.equals(strValue);
								} else if (field.getType().equals(BigDecimal.class)) {
									value = new BigDecimal(strValue);
								} else if (field.getType().equals(Object.class)) {
									value = strValue;
								} else if (field.getType().equals(float.class)) {
									value = Float.valueOf(strValue);
								} else if (field.getType().equals(List.class)) {

									value = mapListFromExcel(clsInstance, field, strValue);

								}
								field.set(result, value);
							}
						}
					}
				}
				results.add((T) result);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		finally {
			if (workbook != null) {
				workbook.close();
			}
		}

		return results;
	}
	
	public static <T> List<T> convertSetToList(Set<T> set)
	{

		List<T> list = new ArrayList<>(set);

		return list;
	}
	
	public static <T> Object mapListFromExcel(T clsInstance,Field field,String strValue)
	{
		Object value = null;
		String clsName = field.getName();

		try {

			Class cls = getClassName(clsName);
			clsInstance = (T) cls.newInstance();

			List<String> list = Arrays.asList(strValue.trim().split("\\s*" + LIST_SEPARATOR + "\\s*"));
			Field[] declaredFields = clsInstance.getClass().getDeclaredFields();

			Set<Lead> lead = new HashSet<>();

			for (int i = 0; i < declaredFields.length; i++) {
				declaredFields[i].setAccessible(true);

				for (int z = 0; z < list.size(); z++) {
					if (i == z) {

						declaredFields[i].set(clsInstance, list.get(i));
						lead.add((Lead) clsInstance);

					}

				}
				List<Lead> data = convertSetToList(lead);
				value = data;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;

	}
	
	public static Class<?> getClassName(String name)
	{
	
		
		String entityName=name.substring(0, 1).toUpperCase() +name.substring(1);
		
		Class<?> clsName = null;
		if (ENTITY_LIST.isEmpty()) {

			Reflections reflections = new Reflections(PACKAGE_NAME);
			Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(L20.class);
			for (Class<?> cls : annotated) {
				ENTITY_LIST.put(cls.getSimpleName().toLowerCase(), cls);
				if (cls.getSimpleName().equalsIgnoreCase(entityName.toLowerCase())) {
					clsName = cls;
				}
			}
		} else {
			clsName = ENTITY_LIST.get(entityName.toLowerCase());
		}
		
		return clsName;
	}
	
}
