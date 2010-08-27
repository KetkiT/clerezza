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

package org.apache.clerezza.rdf.storage.externalizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.AbstractMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;

/**
 *
 * @author reto
 */
/*
 * This could be optimized by not using the notification mechanism provided by
 * AbstractMGraph but intercept the notifictification of the basegraph
 */
class ExternalizingMGraph extends AbstractMGraph {
	private final MGraph baseGraph;
	private final File dataDir;

	private static final UriRef base64Uri =
			new UriRef("http://www.w3.org/2001/XMLSchema#base64Binary");
	//not using a nown uri-scheme (such as urn:hash) to avoid collission with Uris in the graph
	private static final String UriHashPrefix = "urn:x-litrep:";
	private static final Charset UTF8 = Charset.forName("utf-8");
	private static final byte[] DELIMITER = "^^".getBytes(UTF8);

	public ExternalizingMGraph(MGraph baseGraph, File dataDir) {
		this.baseGraph = baseGraph;
		this.dataDir = dataDir;
		System.out.println("Created externalizing mgraph with dir: "+dataDir);
	}

	@Override
	protected Iterator<Triple> performFilter(NonLiteral subject, UriRef predicate, Resource object) {
		if (object != null) {
			if (needsReplacing(object)) {
				return replaceReferences(baseGraph.filter(subject, predicate, replace((TypedLiteral)object)));
			} else {
				return baseGraph.filter(subject, predicate, object);
			}
		} else {
			return replaceReferences(baseGraph.filter(subject, predicate, object));
		}
	}

	@Override
	public int size() {
		return baseGraph.size();
	}

	@Override
	public boolean performAdd(Triple triple) {
		return baseGraph.add(replaceWithReference(triple));
	}

	@Override
	public boolean performRemove(Triple triple) {
		return baseGraph.remove(replaceWithReference(triple));
	}

	private Triple replaceWithReference(Triple triple) {
		Resource object = triple.getObject();
		if (needsReplacing(object)) {
			return new TripleImpl(triple.getSubject(), triple.getPredicate(),
					replace((TypedLiteral)object));
		} else {
			return triple;
		}
	}

	/**
	 * this method defines which resources are too be replaced, the rest of the
	 * code only assumes the resource to be replaced to be a typed literal.
	 * 
	 * @param object
	 * @return
	 */
	private boolean needsReplacing(Resource object) {
		if (object instanceof TypedLiteral) {
			if (((TypedLiteral)object).getDataType().equals(base64Uri)) {
				return true;
			}
		}
		return false;
	}

	private UriRef replace(TypedLiteral literal) {
		FileOutputStream out = null;
		try {
			final byte[] serializedLiteral = serializeLiteral(literal);
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(serializedLiteral);
			byte[] hash = digest.digest();
			String base16Hash = toBase16(hash);
			File storingFile = getStoringFile(base16Hash);
			out = new FileOutputStream(storingFile);
			out.write(serializedLiteral);
			return new UriRef(UriHashPrefix + base16Hash);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		} finally {
			try {
				out.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	 * the first bytes are the datatype-uri followed by trailing "^^", the rest
	 * the lexical form. everything encoded as utf-8
	 * @return
	 */
	private byte[] serializeLiteral(TypedLiteral literal) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] uriBytes = literal.getDataType().getUnicodeString().getBytes(UTF8);
			out.write(uriBytes);
			out.write(DELIMITER);
			out.write(literal.getLexicalForm().getBytes(UTF8));
			return out.toByteArray();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private TypedLiteral parseLiteral(File file) {

		try {
			InputStream in = new FileInputStream(file);
			try {
				ByteArrayOutputStream typeWriter = new ByteArrayOutputStream();
				int posInDelimiter = 0;
				for (int ch = in.read(); ch != -1; ch = in.read()) {
					if (ch != DELIMITER[posInDelimiter]) {
						posInDelimiter++;
						if (DELIMITER.length == posInDelimiter) {
							break;
						}
					} else {
						if (posInDelimiter > 0) {
							typeWriter.write(DELIMITER, 0, posInDelimiter);
							posInDelimiter = 0;
						}
						typeWriter.write(ch);
					}
				}
				UriRef type = new UriRef(new String(typeWriter.toByteArray(), UTF8));
				typeWriter = null;
				ByteArrayOutputStream dataWriter = new ByteArrayOutputStream((int) file.length());
				for (int ch = in.read(); ch != -1; ch = in.read()) {
					dataWriter.write(ch);
				}
				String lexicalForm = new String(dataWriter.toByteArray(), UTF8);
				return new TypedLiteralImpl(lexicalForm, type);
			} finally {
				in.close();
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

	}

	private String toBase16(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			if (b < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(b));
		}
		return sb.toString();
	}

	private File getStoringFile(String base16Hash) {
		File dir1 = new File(dataDir, base16Hash.substring(0,4));
		File dir2 = new File(dir1, base16Hash.substring(4,8));
		dir2.mkdirs();
		return new File(dir2, base16Hash.substring(8));
	}

	private Iterator<Triple> replaceReferences(final Iterator<Triple> base) {
		return new Iterator<Triple>() {

			@Override
			public boolean hasNext() {
				return base.hasNext();
			}

			@Override
			public Triple next() {
				return replaceReference(base.next());
			}

			@Override
			public void remove() {
				base.remove();
			}

			private Triple replaceReference(Triple triple) {
				Resource object = triple.getObject();
				if (object instanceof UriRef) {
					String uriString = ((UriRef)object).getUnicodeString();
					if (uriString.startsWith(UriHashPrefix)) {
						File file = getStoringFile(uriString.substring(UriHashPrefix.length()));
						return new TripleImpl(triple.getSubject(), triple.getPredicate(),
								parseLiteral(file));
					}
				}
				return triple;
			}
		};
	}
}
