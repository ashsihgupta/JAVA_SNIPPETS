package com.jio.subscriptionengine.usersubscription.kafka;

import java.util.Map;

import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.subscriptionengine.usersubscription.utils.ObjectMapperHelper;

public class ObjectSerialize implements Serializer {

	@Override
	public byte[] serialize(String topic, Object data) {
		byte[] retVal = null;
		ObjectMapper objectMapper = ObjectMapperHelper.getInstance().getEntityObjectMapper();
		try {
			retVal = objectMapper.writeValueAsString(data).getBytes();
		} catch (Exception exception) {
			System.out.println("Error in serializing object" + data);
		}
		return retVal;
	}

	@Override
	public void close() {

	}

	@Override
	public void configure(Map arg0, boolean arg1) {
		// TODO Auto-generated method stub

	}

}
