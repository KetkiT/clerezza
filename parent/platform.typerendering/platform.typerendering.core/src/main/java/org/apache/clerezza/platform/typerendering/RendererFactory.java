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
package org.apache.clerezza.platform.typerendering;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.apache.clerezza.platform.typepriority.TypePrioritizer;
import org.apache.clerezza.platform.typerendering.utils.MediaTypeMap;
import org.apache.clerezza.platform.typerendering.utils.RegexMap;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a <code>Renderer</code> which can used to render a <code>GraphNode</code>.
 *
 * @author mir, reto
 */
@Component
@Services({
	@Service(RendererFactory.class)
})
@Reference(name = "typeRenderlet",
cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
policy = ReferencePolicy.DYNAMIC,
referenceInterface = TypeRenderlet.class)
public class RendererFactory {

	private Logger logger = LoggerFactory.getLogger(RendererFactory.class);
	
	@Reference
	private TypePrioritizer typePrioritizer;

	private Map<UriRef, RegexMap<MediaTypeMap<TypeRenderlet>>> typeRenderletMap =
			new HashMap<UriRef, RegexMap<MediaTypeMap<TypeRenderlet>>>();

	private BundleContext bundleContext;

	protected void activate(ComponentContext componentContext) {
		bundleContext = componentContext.getBundleContext();
	}

	protected void deactivate(ComponentContext componentContext) {
		bundleContext = null;
	}

	/**
	 * Creates a <code>Renderer</code> for the specified mode, acceptable 
	 * media-types as well as the types of <code>GraphNode</code>.
	 * The <code>acceptableMediaTypes</code> list represent the media
	 * types that are acceptable for the rendered output. The list has a
	 * order where the most desirable media type is a the beginning of the list.
	 * The media type of the rendered output will be compatible to at least one
	 * media type in the list.
	 *
	 * @param resource The <code>GraphNode</code> to be rendered
	 * @param mode mode
	 * @param acceptableMediaTypes acceptable media types for the rendered output
	 * @return the Renderer or null if no renderer could be created for the specified parameters
	 */
	public Renderer createRenderer(GraphNode resource, String mode,
			List<MediaType> acceptableMediaTypes) {
		Set<UriRef> types = new HashSet<UriRef>();
		if (resource.getNode() instanceof TypedLiteral) {
			types.add(((TypedLiteral) resource.getNode()).getDataType());
		} else {
			// extract rdf types
			Iterator<UriRef> it = resource.getUriRefObjects(RDF.type);
			while (it.hasNext()) {
				final UriRef rdfType = it.next();
				types.add(rdfType);
			}
			types.add(RDFS.Resource);
		}
		return getRenderer(types, mode, acceptableMediaTypes);
	}

	private Renderer getRenderer(Set<UriRef> types, String mode,
			List<MediaType> acceptableMediaTypes) {
		Iterator<UriRef> sortedTypes = typePrioritizer.iterate(types);
		while (sortedTypes.hasNext()) {
			final UriRef currentType = sortedTypes.next();
			final RegexMap<MediaTypeMap<TypeRenderlet>> regexMap = typeRenderletMap.get(currentType);
			if (regexMap != null) {
				Iterator<MediaTypeMap<TypeRenderlet>> mediaTypeMapIter = regexMap.getMatching(mode);
				while (mediaTypeMapIter.hasNext()) {
					MediaTypeMap<TypeRenderlet> mediaTypeMap = mediaTypeMapIter.next();
					for (MediaType acceptableType : acceptableMediaTypes) {
						Iterator<TypeRenderlet> renderlets = mediaTypeMap.getMatching(acceptableType);
						if (renderlets.hasNext()) {
							TypeRenderlet typeRenderlet = renderlets.next();
							//TODO is this the most convrete type
							return new TypeRenderletRendererImpl(
								typeRenderlet,
								acceptableType,
								this,
								bundleContext);
						}
					}
				}
			}
		}
		return null;
	}

	protected void bindTypeRenderlet(TypeRenderlet typeRenderlet) {
		final UriRef rdfType = typeRenderlet.getRdfType();
		RegexMap<MediaTypeMap<TypeRenderlet>> regexMap = typeRenderletMap.get(rdfType);
		if (regexMap == null) {
			regexMap = new RegexMap<MediaTypeMap<TypeRenderlet>>();
			typeRenderletMap.put(rdfType, regexMap);
		}
		final String mode = typeRenderlet.getModePattern();
		MediaTypeMap<TypeRenderlet> mediaTypeMap = regexMap.getFirstExactMatch(mode);
		if (mediaTypeMap == null) {
			mediaTypeMap = new MediaTypeMap<TypeRenderlet>();
			regexMap.addEntry(mode, mediaTypeMap);
		}
		final MediaType mediaType = typeRenderlet.getMediaType();
		mediaTypeMap.addEntry(mediaType, typeRenderlet);
	}

	protected void unbindTypeRenderlet(TypeRenderlet typeRenderlet) {

	}


}
