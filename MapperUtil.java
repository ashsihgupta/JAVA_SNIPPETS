package com.jio.crm.l2o.utils;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapperUtil<E, M> {

	private static final MapperUtil mapperUtil = new MapperUtil();

	private MapperUtil() {
	}

	public static final MapperUtil getInstance() {
		return mapperUtil;
	}

	public List<M> mapObjects(final List<E> entities, final Class<M> clazz) throws Exception {

		M model = null;

		List<M> po = new ArrayList<>();

		for (final E entity : entities) {

			final List<String> planFields = Arrays.asList(clazz.getDeclaredFields()).stream().map(Field::getName)
					.collect(Collectors.toList());

			model = clazz.newInstance();
			for (final Field f : model.getClass().getDeclaredFields()) {
				if (planFields.contains(f.getName())) {
					try {
						new PropertyDescriptor(f.getName(), clazz).getWriteMethod().invoke(model,
								new PropertyDescriptor(f.getName(), clazz).getReadMethod().invoke(entity));
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
			}

			po.add(model);

		}
		return po;

	}

	public List<M> excelToPojo(final Class<M> clazz, final String fileName) throws Exception {

		InputStream is = null;

		FileInputStream fileInputStream2 = null;

		String path = "/configuration/";

		List<M> entites = null;

		try {

			String profile = System.getProperty("profile");
			if (profile != null && profile.equals("dev")) {
				fileInputStream2 = new FileInputStream(new File("." + path + fileName + ".xlsx"));

			} else {
				fileInputStream2 = new FileInputStream(new File(".." + path + fileName + ".xlsx"));

			}

			is = fileInputStream2;

		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}
		try {
			entites = ExcelToPojoUtils.toPojo(clazz, is);

		} catch (Exception e) {

			e.printStackTrace();
		}

		return entites;

	}
}
