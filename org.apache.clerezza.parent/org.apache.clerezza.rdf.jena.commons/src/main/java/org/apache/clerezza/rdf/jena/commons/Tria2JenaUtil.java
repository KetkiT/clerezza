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
package org.apache.clerezza.rdf.jena.commons;

import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import java.util.Map;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;

/**
 *
 * @author rbn
 */
public class Tria2JenaUtil {
	private Map<BNode, Node> tria2JenaBNodes;

	public Tria2JenaUtil(Map<BNode, Node> tria2JenaBNodes) {
		this.tria2JenaBNodes = tria2JenaBNodes;
	}

	public Node convert2JenaNode(NonLiteral nonLiteral) {
		if (nonLiteral instanceof UriRef) {
			return convert2JenaNode((UriRef)nonLiteral);
		} else {
			return convert2JenaNode((BNode)nonLiteral);
		}
	}

	public Node convert2JenaNode(Literal literal) {
		if (literal == null) {
			return null;
		}
		if (literal instanceof PlainLiteral) {
			return convert2JenaNode((PlainLiteral)literal);
		}
		return convert2JenaNode((TypedLiteral)literal);
	}
	public Node convert2JenaNode(PlainLiteral literal) {
		return com.hp.hpl.jena.graph.Node.createLiteral(
							literal.getLexicalForm(),
							literal.getLanguage() == null ? null : literal.getLanguage().
							toString(), false);
	}
	public Node convert2JenaNode(TypedLiteral literal) {
		return com.hp.hpl.jena.graph.Node.createLiteral(
							literal.getLexicalForm(), null, TypeMapper.getInstance().
							getSafeTypeByName(
							literal.getDataType().getUnicodeString()));
	}

	public Node convert2JenaNode(Resource resource) {
		if (resource instanceof NonLiteral) {
			return convert2JenaNode((NonLiteral)resource);
		}
		return convert2JenaNode((Literal)resource);
	}

	public Node convert2JenaNode(UriRef uriRef) {
		if (uriRef == null) {
			return null;
		}
		return com.hp.hpl.jena.graph.Node.createURI(
						uriRef.getUnicodeString());
	}

	public Node convert2JenaNode(BNode bnode) {
		if (bnode == null) {
			return null;
		}
		Node result = tria2JenaBNodes.get(bnode);
		if (result == null) {
			result = com.hp.hpl.jena.graph.Node.createAnon();
			tria2JenaBNodes.put(bnode, result);
		}
		return result;
	}

	public com.hp.hpl.jena.graph.Triple convertTriple(Triple triple) {
		Node subject = convert2JenaNode(triple.getSubject());
		Node predicate = convert2JenaNode(triple.getPredicate());
		Node object = convert2JenaNode(triple.getObject());
		return new com.hp.hpl.jena.graph.Triple(subject, predicate, object);
	}
}
