/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.triaxrs.blackbox;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;
import org.apache.clerezza.triaxrs.JaxRsHandler;
import org.apache.clerezza.triaxrs.mock.ResponseImpl;
import org.apache.clerezza.triaxrs.testutils.HandlerCreator;
import org.wymiwyg.wrhapi.HeaderName;
import org.wymiwyg.wrhapi.Method;
import org.wymiwyg.wrhapi.Request;
import org.wymiwyg.wrhapi.RequestURI;

public class TestWriterDeterminedContentType {


	@Path("/")
	public static class MyResource {

		@GET
		public Person handleGet() {
			return new Person();
		}
	}
	
	public static class Person {

	}
	
	@Provider
	@Produces("text/html")
	public static class PersonWriter implements MessageBodyWriter<Person> {

		@Override
		public boolean isWriteable(Class<?> type, Type genericType,
				Annotation[] annotations, MediaType mediaType) {
			 return Person.class.isAssignableFrom(type);
		}


		@Override
		public long getSize(Person t, java.lang.Class<?> type,
				java.lang.reflect.Type genericType,
				java.lang.annotation.Annotation[] annotations, MediaType mediaType) {
			return -1;
		}

		@Override
		public void writeTo(Person t, Class<?> type, Type genericType,
				Annotation[] annotations, MediaType mediaType,
				MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
			entityStream.write("<html/>".getBytes());
		}
	}

	@Test
	public void testResponseObject() throws Exception {

		JaxRsHandler handler = HandlerCreator.getHandler(MyResource.class, PersonWriter.class);

		Request requestMock = EasyMock.createNiceMock(Request.class);
		ResponseImpl responseImpl = new ResponseImpl();
		expect(requestMock.getMethod()).andReturn(Method.GET).anyTimes();
		RequestURI requestURI = EasyMock.createNiceMock(RequestURI.class);
		expect(requestURI.getPath()).andReturn("/");
		expect(requestMock.getRequestURI()).andReturn(requestURI).anyTimes();
		expect(requestURI.getType()).andReturn(null).anyTimes();
		replay(requestMock);
		replay(requestURI);
		handler.handle(requestMock, responseImpl);
		responseImpl.consumeBody();
		String[] contentType = responseImpl.getHeaders().get(HeaderName.CONTENT_TYPE);
		Assert.assertTrue(contentType.length == 1);
		Assert.assertEquals("text/html", contentType[0]);
	}
}
