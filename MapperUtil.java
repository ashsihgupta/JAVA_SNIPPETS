package com.jio.crm.l2o.utils;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;

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

	public List<M> excelToPojo(final Class<M> clazz, HttpServletRequest req) throws Exception {

		InputStream is = null;

		List<M> entites = null;

		try {

			byte[] byteArray = IOUtils.toByteArray(req.getInputStream());

			is = new ByteArrayInputStream(byteArray);

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

	public M mapObjects(final E entity, final Class<M> clazz, final Class<E> entityClass) throws Exception {

		M model = null;

		final List<String> planFields = Arrays.asList(entityClass.getDeclaredFields()).stream().map(Field::getName)
				.collect(Collectors.toList());

		model = clazz.getDeclaredConstructor().newInstance();
		for (final Field f : model.getClass().getDeclaredFields()) {
			if (planFields.contains(f.getName())) {
				try {
					new PropertyDescriptor(f.getName(), clazz).getWriteMethod().invoke(model,
							new PropertyDescriptor(f.getName(), entityClass).getReadMethod().invoke(entity));
				} catch (Exception e) {

					e.printStackTrace();
				}
			}
		}

		return model;

	}

	/**
	 * Method which maps a list of entities to respective models
	 * 
	 * @param entities
	 * @param clazz
	 * @param entityClass
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public List<M> mapObjects(final List<E> entities, final Class<M> clazz, final Class<E> entityClass)
			throws InstantiationException, IllegalAccessException {
		final List<M> models = new ArrayList<>();
		final List<String> planFields = Arrays.asList(entityClass.getDeclaredFields()).stream().map(Field::getName)
				.collect(Collectors.toList());
		for (final E entity : entities) {
			if (entity != null) {
				M model = null;
				try {
					model = clazz.getDeclaredConstructor().newInstance();
					for (final Field f : model.getClass().getDeclaredFields()) {
						if (planFields.contains(f.getName())) {
							new PropertyDescriptor(f.getName(), clazz).getWriteMethod().invoke(model,
									new PropertyDescriptor(f.getName(), entityClass).getReadMethod().invoke(entity));
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					models.add(model);
				}

			}
		}
		return models;
	}

}
