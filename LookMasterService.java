package com.jio.subscriptionengine.epc.modules.customer.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.elastic.search.bean.SearchResult;
import com.elastic.search.enums.Levels;
import com.elastic.search.launcher.SessionFactory;
import com.elastic.search.service.Session;
import com.jio.subscriptionengine.epc.modules.bean.ReferenceValueMaster;

/**
 * @author Ashish14.Gupta
 *
 */
public class LookUpTableMasterService {

	final LookUpTableMasterRepository lookUpTableMasterRepository = new LookUpTableMasterRepository();

	/**
	 * @param enityName
	 * @param attrName
	 * @return
	 * @throws Exception
	 */
	public SearchResult<ReferenceValueMaster> lookUpData(final String enityName, final String attrName)
			throws Exception {

		final SessionFactory factory = SessionFactory.getSessionFactory();
		final Session session = factory.getSession();
		session.setSearchLevel(Levels.NONE);

		return lookUpTableMasterRepository.lookUpData(session, enityName, attrName);

		// for getting mapping for the values from reference_master_table
		

	}

	public Map<String, Map<String, List<Object>>> getLookUpDataResponse(SearchResult<ReferenceValueMaster> lookUpData) {
		//@formatter:off
		final  Map<String, Map<String, List<Object>>> data = lookUpData.getResult()
				.stream()
				.collect(
						Collectors.groupingBy(ReferenceValueMaster::getEntityName, 
												Collectors.groupingBy(ReferenceValueMaster::getAttributeName,
														Collectors.mapping(lm -> {
															Map<String,Object> objectNode = new HashMap<>();
															objectNode.put("value", lm.getValue());
															objectNode.put("text", lm.getDescription());
															return objectNode;
														}, Collectors.toList())

                )));
		
		//@formatter:on

		return data;
	}

}

