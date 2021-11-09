package com.jio.subscriptionengine.usersubscription.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class FtlUtilImpl implements FtlUtil {

	private static FtlUtilImpl ftlUtilImpl = new FtlUtilImpl();

	private FtlUtilImpl() {

	}

	public static FtlUtilImpl getInstance() {
		if (ftlUtilImpl == null) {
			ftlUtilImpl = new FtlUtilImpl();
		}
		return ftlUtilImpl;
	}

	@Override
	public String buildPojoObj(final String template, final Object data) throws Exception {
		final Configuration configuration = new Configuration();

		final StringWriter stringWriter = new StringWriter();

		final ObjectMapper obj = new ObjectMapper();

		final String jsonString = new com.google.gson.Gson().toJson(data);
		final JsonNode node = obj.readTree(jsonString);
		
		String newTemplate=buildDefaultTemplate(template);

		final Template templateFtl = new Template("templateName", new StringReader(newTemplate), configuration);

		templateFtl.process(node, stringWriter);
		return stringWriter.toString();
	}

	@Override
	public String buildPojoMap(final String label, final Map<String, Object> map) throws Exception {
		final Configuration configuration = new Configuration();
		String newLabel=buildDefaultTemplate(label);
		final Template template = new Template("templateName", new StringReader(newLabel), configuration);
		final StringWriter stringWriter = new StringWriter();

		template.process(map, stringWriter);
		return stringWriter.toString();
	}
	
	private static String buildDefaultTemplate(String template) {

		String prefix=template.replace("{", "{(");
		
		String postfix=prefix.replace("}", ")!'N.A'}");
		
		return postfix;
		
	}

}
