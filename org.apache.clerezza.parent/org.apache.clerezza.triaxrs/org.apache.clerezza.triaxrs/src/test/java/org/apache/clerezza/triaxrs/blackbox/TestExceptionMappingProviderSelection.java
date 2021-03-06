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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.easymock.EasyMock;
import org.junit.Test;
import org.apache.clerezza.triaxrs.JaxRsHandler;
import org.apache.clerezza.triaxrs.mock.ResponseImpl;
import org.apache.clerezza.triaxrs.testutils.HandlerCreator;
import org.wymiwyg.wrhapi.Method;
import org.wymiwyg.wrhapi.Request;
import org.wymiwyg.wrhapi.RequestURI;

public class TestExceptionMappingProviderSelection {

	public static class MyException extends RuntimeException {

	}

	public static class MySubException extends MyException {

	}

	static String errMsgFoo = "missing foo";
	static String errMsgBar = "missing bar";

	@Path("/")
	public static class MyResource {

		@GET
		public void handleGet() throws Exception {
			throw new MySubException();
		}
	}

	@Provider
	public static class MyRuntimeExceptionMapper implements
			ExceptionMapper<RuntimeException> {

		@Override
		public Response toResponse(RuntimeException exception) {
			javax.ws.rs.core.Response r = javax.ws.rs.core.Response.status(
					Status.BAD_REQUEST).entity(errMsgFoo).type(
					MediaType.TEXT_PLAIN_TYPE).build();
			return r;
		}
	}

	@Provider
	public static class MyMyExceptionMapper implements
			ExceptionMapper<MyException> {

		@Override
		public Response toResponse(MyException exception) {
			javax.ws.rs.core.Response r = javax.ws.rs.core.Response.status(
					Status.BAD_REQUEST).entity(errMsgBar).type(
					MediaType.TEXT_PLAIN_TYPE).build();
			return r;
		}
	}

	@Test
	public void testExceptions() throws Exception {

		JaxRsHandler handler = HandlerCreator.getHandler(MyResource.class,
				MyRuntimeExceptionMapper.class, MyMyExceptionMapper.class);

		Request requestMock = EasyMock.createNiceMock(Request.class);
		expect(requestMock.getMethod()).andReturn(Method.GET).anyTimes();
		RequestURI requestURI = EasyMock.createNiceMock(RequestURI.class);
		expect(requestURI.getPath()).andReturn("/");
		expect(requestMock.getRequestURI()).andReturn(requestURI).anyTimes();
		replay(requestMock);
		replay(requestURI);

		ResponseImpl responseImpl = new ResponseImpl();
		handler.handle(requestMock, responseImpl);
		responseImpl.consumeBody();
		
		assertNotNull(responseImpl.getStatus());
		assertNotNull(responseImpl.getHeaders());
		assertArrayEquals(errMsgBar.getBytes(), responseImpl.getBodyBytes());
	}
}
