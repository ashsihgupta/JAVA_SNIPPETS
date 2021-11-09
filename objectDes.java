package com.jio.subscriptionengine.usersubscription.kafka;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.poi.ss.formula.functions.T;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.subscriptionengine.usersubscription.utils.ObjectMapperHelper;

public class ObjectDeserialize implements Deserializer<T> {

	@Override
	public void close() {
	}

	@Override
	public T deserialize(String arg0, byte[] arg1) {
		 ObjectMapper mapper = ObjectMapperHelper.getInstance().getEntityObjectMapper();
		T obj = null;
		try {
			obj = mapper.readValue(arg1, T.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

	@Override
	public void configure(Map arg0, boolean arg1) {

	}

}
