/**
 * Copyright 2012 Erik Isaksson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openapplication.graph.jsonld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openapplication.graph.streaming.GraphReaderCallback;
import org.openapplication.graph.streaming.GraphWriter;
import org.openapplication.json.JSON;
import org.openapplication.json.JSONCallback;
import org.openapplication.json.JSONException;
import org.openapplication.json.JSONToken;
import org.openapplication.json.PrettyJSON;

public class JSONLD implements GraphWriter {

	public static void parse(InputStream in, GraphReaderCallback callback)
			throws JSONException, IOException {
		JSON.parse(in, new JSONProcessor(new JSONLDProcessor(callback)));
	}

	public static void parse(String in, GraphReaderCallback callback)
			throws JSONException {
		JSON.parse(in, new JSONProcessor(new JSONLDProcessor(callback)));
	}

	public static void parse(Object element, GraphReaderCallback callback) {
		new JSONLDProcessor(callback).process(element);
	}

	private static class JSONLDProcessor {

		final GraphReaderCallback callback;

		final Map<String, String> identifierMap = new HashMap<String, String>();

		JSONLDProcessor(GraphReaderCallback callback) {
			this.callback = callback;
		}

		public void process(Object definition) {
			HashMap<String, Object> initialContext = new HashMap<String, Object>();
			HashMap<String, Object> rdfTypeCoercion = new HashMap<String, Object>();
			rdfTypeCoercion.put("@type", "@id");
			initialContext.put(
					"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
					rdfTypeCoercion);

			triples(definition, null, null, null, null, initialContext);
		}

		@SuppressWarnings("unchecked")
		private void triples(Object element, String activeSubject,
				String activeProperty, String activeObject, String activeGraph,
				Map<String, Object> activeContext) {
			activeObject = null;
			if (element instanceof Map<?, ?>) {
				Map<String, Object> map = (Map<String, Object>) element;
				Object value;
				String dataType, language, id = null;
				if (map.containsKey("@context"))
					activeContext = processContext(map.get("@context"),
							activeContext);
				if ((value = map.get("@value")) != null) {
					if (value instanceof Number) {
						dataType = (String) map.get("@type");
						if (dataType == null)
							dataType = value instanceof BigInteger ? "xsd:integer"
									: "xsd:double"; // TODO
						if (activeSubject != null && activeProperty != null)
							callback.triple(activeSubject, activeProperty,
									value.toString(), null, dataType);
					} else if (value instanceof Boolean) {
						dataType = (String) map.get("@type");
						if (dataType == null)
							dataType = "xsd:boolean"; // TODO
						if (activeSubject != null && activeProperty != null)
							callback.triple(activeSubject, activeProperty,
									value.toString(), null, dataType);
					} else if ((dataType = (String) map.get("@type")) != null) {
						if (activeSubject != null && activeProperty != null)
							callback.triple(activeSubject, activeProperty,
									value.toString(), null, dataType);
					} else if ((language = (String) map.get("@language")) != null) {
						if (activeSubject != null && activeProperty != null)
							callback.triple(activeSubject, activeProperty,
									value.toString(), language, null);
					} else {
						if (activeSubject != null && activeProperty != null)
							callback.triple(activeSubject, activeProperty,
									value.toString(), null, null);
					}
				} else if ((value = map.get("@list")) != null) {
					// TODO
					System.out.println("Ignoring list!");
					return;
				} else if ((value = map.get("@set")) != null) {
					// TODO
					System.out.println("Ignoring set!");
					return;
				} else if ((id = (String) map.get("@id")) != null) {
					if (id.startsWith("_:")) {
						String mappedId = identifierMap.get(id);
						if (mappedId != null)
							id = mappedId;
						else {
							mappedId = callback.generateBlankNode(id);
							identifierMap.put(id, mappedId);
							id = mappedId;
						}
					} else
						id = expand(id, activeContext);
					if (activeSubject != null && activeProperty != null)
						callback.triple(activeSubject, activeProperty, id);
					else if (activeGraph == null)
						callback.startDefinition(id);
					// activeSubject = id;
				} else {
					id = callback.generateBlankNode(null);
					if (activeSubject != null && activeProperty != null)
						callback.triple(activeSubject, activeProperty, id);
					else if (activeGraph == null)
						callback.startDefinition(id);
					// activeSubject = id;
				}
				if (id != null)
					for (Map.Entry<String, Object> entry : new TreeMap<String, Object>(
							map).entrySet()) {
						String property = entry.getKey();
						value = entry.getValue();
						if (property.equals("@type")) {
							activeProperty = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
						} else if (property.equals("@graph")) {
							callback.startGraph(id);
							triples(value, null, null, null, id, activeContext);
							callback.endGraph(id);
							continue;
						} else if (property.startsWith("@")) { // TODO
							// Ignore
							continue;
						} else {
							activeProperty = expand(property, activeContext);
						}
						triples(value, id, activeProperty, activeObject,
								activeGraph, activeContext);
					}
				if (id != null && activeSubject == null && activeGraph == null)
					callback.endDefinition(id);
				activeSubject = id;
				activeObject = activeSubject;
			} else if (element instanceof List<?>) {
				// process element recursively using this algorithm, using
				// copies of active subject, active property, and graph
				// name.
				for (Object e : ((List<Object>) element))
					triples(e, activeSubject, activeProperty, null,
							activeGraph, activeContext);
			} else if (element instanceof String) {
				// TODO
				// the active property must be rdf:type so set the active
				// object to an IRI
				// if ("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
				// .equals(activeProperty))
				// callback.triple(activeSubject, activeProperty,
				// expand((String) element, activeContext));
				// else
				if (activeContext.get(activeProperty) instanceof Map<?, ?>) {
					String type = (String) ((Map<String, Object>) activeContext
							.get(activeProperty)).get("@type");
					if (type == null)
						callback.triple(activeSubject, activeProperty,
								(String) element, null, null);
					else if ("@id".equals(type))
						callback.triple(activeSubject, activeProperty,
								expand((String) element, activeContext));
					else
						callback.triple(activeSubject, activeProperty,
								(String) element, null, type);
				} else
					callback.triple(activeSubject, activeProperty,
							(String) element, null, null);
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, Object> processContext(Object context,
				Map<String, Object> activeContext) {
			activeContext = new HashMap<String, Object>(activeContext);
			if (context == null) {
				activeContext = null;
			} else if (context instanceof List<?>) {
				for (Object element : ((List<Object>) context))
					activeContext = processContext(element, activeContext);
			} else if (context instanceof String) {
				// TODO
			} else if (context instanceof Map<?, ?>) {
				Map<String, Object> map = (Map<String, Object>) context;
				Map<String, Object> localContext = new HashMap<String, Object>();
				Map<String, Object> newContext;
				Object value;
				int equalLoopCount = 0;
				for (;;) {
					newContext = new HashMap<String, Object>();
					for (Map.Entry<String, Object> entry : map.entrySet()) {
						String property = entry.getKey();
						value = entry.getValue();
						if (property.equals("@vocab")
								|| property.equals("@language")) {
							newContext.put(property, value);
						} else if (value instanceof String) {
							// TODO
							// determine the IRI mapping value by performing IRI
							// Expansion on the associated value. If the result
							// of the IRI mapping is an absolute IRI, merge the
							// property into the local context term mapping,
							// unless the property is a JSON-LD keyword, in
							// which case return an error.
							newContext.put(property,
									expand((String) value, localContext));
						} else if (value == null) {
							newContext.put(property, value);
						} else if (value instanceof Map<?, ?>) {
							Map<String, Object> valueMap = (Map<String, Object>) value;
							if (valueMap.get("@type") instanceof String) {
								valueMap.put(
										"@type",
										expand((String) valueMap.get("@type"),
												localContext));
							}
							newContext.put(expand(property, localContext),
									valueMap);
						}
					}
					localContext = newContext;
					if (map.equals(localContext))
						if (equalLoopCount++ > 1)
							break;
					map = localContext;
				}
				for (Map.Entry<String, Object> entry : localContext.entrySet())
					if ((value = entry.getValue()) != null)
						activeContext.put(entry.getKey(), value);
					else
						activeContext.remove(entry.getKey());
			}
			return activeContext;
		}

		private String expand(String value, Map<String, Object> activeContext) {
			if (activeContext.get(value) instanceof String) {
				return (String) activeContext.get(value);
			}
			int colon = value.indexOf(':');
			if (colon != -1) {
				String prefix = value.substring(0, colon);
				String suffix = value.substring(colon + 1);
				if (prefix.equals("_")) {
					return value;
				} else if (activeContext.containsKey(prefix)) {
					return ((String) activeContext.get(prefix)) + suffix;
				}
			} else {
				// TODO: If is a property (i.e., key in JSON object) and
				// there is a @vocab

				// TODO: If is not a property
				// TODO: Resolve against base URI
			}
			return value;
		}

	}

	private static class JSONProcessor implements JSONCallback {

		final JSONLDProcessor jsonldProcessor;

		final Deque<Object> stack = new ArrayDeque<Object>();

		String fieldName;

		JSONProcessor(JSONLDProcessor jsonldProcessor) {
			this.jsonldProcessor = jsonldProcessor;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void token(JSONToken token, String value) throws JSONException {
			Object container = stack.peek();
			Object element = null;
			switch (token) {
			case START_OBJECT:
				stack.push(element = new HashMap<String, Object>());
				break;
			case END_OBJECT:
				element = stack.pop();
				// If element is a top-level object or an object within a
				// top-level array, then processed it as JSON-LD now
				if (stack.isEmpty())
					jsonldProcessor.process((Map<String, Object>) element);
				return;
			case START_ARRAY:
				if (!stack.isEmpty()) // Don't push top-level array
					stack.push(element = new ArrayList<Object>());
				break;
			case END_ARRAY:
				element = stack.poll(); // null for top-level array
				return;
			case FIELD_NAME:
				fieldName = value;
				return;
			case VALUE_STRING:
				element = value;
				break;
			case VALUE_NUMBER:
				element = new BigDecimal(value);
				break;
			case VALUE_TRUE:
				element = Boolean.TRUE;
				break;
			case VALUE_FALSE:
				element = Boolean.FALSE;
				break;
			case VALUE_NULL:
				element = null;
				break;
			}
			if (container instanceof Map<?, ?>)
				((Map<String, Object>) container).put(fieldName, element);
			else if (container instanceof List<?>)
				((List<Object>) container).add(element);
		}

	}

	private final JSON json;

	private Deque<String> stack = new ArrayDeque<String>();

	private Deque<String> defaultStack;

	private String currentGraph;

	private String currentSubject;

	private String currentPredicate;

	private int maxStripes = 100;

	public JSONLD(OutputStream out) throws IOException {
		// json = new JSON(out);
		json = new PrettyJSON(out) {
			@Override
			public void writeStartArray() throws IOException {
				super.writeStartArray();
				level--;
				noIndent = true;
			}

			@Override
			public void writeEndArray() throws IOException {
				level++;
				noIndent = true;
				super.writeEndArray();
			}

			@Override
			public void writeStartObject() throws IOException {
				noIndent = true;
				super.writeStartObject();
			}

			@Override
			public void writeFieldName(CharSequence name) throws IOException {
				// noIndent = false;
				super.writeFieldName(name);
			}
		};
		json.writeStartArray();
	}

	@Override
	public int getMaxStripes() {
		return maxStripes / 2;
	}

	@Override
	public void setMaxStripes(int count) {
		count *= 2;
		if (count < 0)
			throw new IllegalArgumentException();
		maxStripes = count;
	}

	@Override
	public void trim(int maxStripes) throws IOException {
		while (currentSubject != null && (stack.size() - 1) > maxStripes) {
			if (currentPredicate != null) {
				currentPredicate = stack.pop();
				currentSubject = stack.peek();
				json.writeEndArray();
				currentPredicate = null;
			}
			if ((stack.size() - 1) > maxStripes) {
				json.writeEndObject();
				currentSubject = stack.pop();
				if (stack.isEmpty()) {
					currentSubject = null;
				} else {
					currentPredicate = stack.pop();
					currentSubject = stack.peek();
					stack.push(currentPredicate);
				}
			}
		}
	}

	@Override
	public void writeStatement(String graph, String subject, String predicate,
			String object, String literal, String language, String dataType)
			throws IOException {
		if (currentGraph != graph) {
			if ((currentGraph == null || !currentGraph.equals(graph))
					&& graph != null) {
				if (currentGraph != null) {
					collapse(false);
					json.writeEndArray();

					stack = defaultStack;
					currentSubject = stack.peek();
					currentGraph = null;
				}

				if (currentPredicate != null) {
					if (currentSubject != null
							&& ((currentSubject != subject && !currentSubject
									.equals(subject)) || (currentPredicate != null
									&& currentPredicate != predicate && !currentPredicate
										.equals(predicate)))) {
						currentPredicate = stack.pop();
						currentSubject = stack.peek();
						json.writeEndArray();
						currentPredicate = null;
					}
				}
				while (currentSubject != null
						&& ((currentSubject != graph && !currentSubject
								.equals(graph)) || (currentPredicate != null && currentPredicate != null))) {
					if (currentPredicate != null) {
						currentPredicate = stack.pop();
						currentSubject = stack.peek();
						json.writeEndArray();
						currentPredicate = null;
					}
					if (currentSubject != graph
							&& !currentSubject.equals(graph)) {
						json.writeEndObject();
						currentSubject = stack.pop();
						if (stack.isEmpty()) {
							currentSubject = null;
						} else {
							currentPredicate = stack.pop();
							currentSubject = stack.peek();
							stack.push(currentPredicate);
						}
					}
				}
				if (currentSubject == null || !currentSubject.equals(graph)) {
					json.writeStartObject();
					json.writeFieldName("@id");
					json.writeString(graph);
					stack.push(graph);
				}
				currentSubject = null;
				currentPredicate = null;

				defaultStack = stack;
				stack = new ArrayDeque<String>();

				// writeDefinition();
				// json.writeStartObject();
				// json.writeFieldName("@id");
				// json.writeString(graph);
				json.writeFieldName("@graph");
				json.writeStartArray();
				currentGraph = graph;
			} else if (graph == null) {
				collapse(false);
				json.writeEndArray();

				stack = defaultStack;
				currentSubject = stack.peek();
				currentGraph = null;
				// } else if (!currentGraph.equals(graph)) {
				// writeDefinition();
				// json.writeEndArray();
				// json.writeEndObject();
				//
				// stack = defaultStack;
				// currentSubject = stack.peek();
				// currentGraph = graph;
				//
				// json.writeStartObject();
				// json.writeFieldName("@id");
				// json.writeString(graph);
				// json.writeFieldName("@graph");
				// json.writeStartArray();
				// currentGraph = graph;
			} else
				currentGraph = graph;
		}
		if (currentPredicate != null) {
			if (currentSubject != null
					&& ((currentSubject != subject && !currentSubject
							.equals(subject)) || (currentPredicate != null
							&& currentPredicate != predicate && !currentPredicate
								.equals(predicate)))) {
				currentPredicate = stack.pop();
				currentSubject = stack.peek();
				json.writeEndArray();
				currentPredicate = null;
			}
		}
		while (currentSubject != null
				&& (stack.size() > maxStripes
						|| (currentSubject != subject && !currentSubject
								.equals(subject)) || (currentPredicate != null
						&& currentPredicate != predicate && !currentPredicate
							.equals(predicate)))) {
			if (currentPredicate != null) {
				currentPredicate = stack.pop();
				currentSubject = stack.peek();
				json.writeEndArray();
				currentPredicate = null;
			}
			if (stack.size() > maxStripes || currentSubject != subject
					&& !currentSubject.equals(subject)) {
				json.writeEndObject();
				currentSubject = stack.pop();
				if (stack.isEmpty()) {
					currentSubject = null;
				} else {
					currentPredicate = stack.pop();
					currentSubject = stack.peek();
					stack.push(currentPredicate);
				}
			}
		}
		if (currentSubject == null) {
			json.writeStartObject();
			json.writeFieldName("@id");
			json.writeString(subject);
			stack.push(subject);
			currentSubject = subject;
		}
		if (currentPredicate == null) {
			json.writeFieldName(predicate);
			json.writeStartArray();
			stack.push(predicate);
			currentPredicate = predicate;
		}
		if (object != null) {
			json.writeStartObject();
			json.writeFieldName("@id");
			json.writeString(object);
			stack.push(object);
			currentSubject = object;
			currentPredicate = null;
		} else if (literal != null) {
			json.writeStartObject();
			json.writeFieldName("@value");
			json.writeString(literal);
			if (language != null) {
				json.writeFieldName("@language");
				json.writeString(language);
			} else if (dataType != null) {
				json.writeFieldName("@type");
				json.writeString(dataType);
			}
			json.writeEndObject();
		}
	}

	@Override
	public void collapse() throws IOException {
		collapse(true);
	}

	@Override
	public void collapse(boolean closeGraph) throws IOException {
		if (closeGraph && currentGraph != null) {
			collapse(false);
			json.writeEndArray();

			stack = defaultStack;
			currentSubject = stack.peek();
			currentGraph = null;
		}
		if (currentPredicate != null) {
			json.writeEndArray();
			stack.pop();
		}
		while (!stack.isEmpty()) {
			json.writeEndObject();
			stack.pop();
			if (stack.isEmpty())
				break;
			json.writeEndArray();
			stack.pop();
		}
		currentSubject = null;
		currentPredicate = null;
	}

	@Override
	public void flush() throws IOException {
		json.flush();
	}

	@Override
	public void close() throws IOException {
		collapse(true);
		json.writeEndArray();
		json.close();
	}

	@Override
	public void writeStartDefinition(String subject) throws IOException {
		json.writeStartObject();
	}

	@Override
	public void writeEndDefinition() throws IOException {
		json.writeEndObject();
	}

	@Override
	public void writeStartPredicate(String predicate) throws IOException {
		json.writeFieldName(predicate);
		json.writeStartArray();
	}

	@Override
	public void writeEndPredicate() throws IOException {
		json.writeEndArray();
	}

	@Override
	public void writeLiteral(String literal, String language, String dataType)
			throws IOException {
		json.writeStartObject();
		json.writeFieldName("@value");
		json.writeString(literal);
		if (language != null) {
			json.writeFieldName("@language");
			json.writeString(language);
		} else if (dataType != null) {
			json.writeFieldName("@type");
			json.writeString(dataType);
		}
		json.writeEndObject();
	}

	@Override
	public void writeIRI(String object) throws IOException {
		json.writeStartObject();
		json.writeFieldName("@id");
		json.writeString(object);
		json.writeEndObject();
	}

	@Override
	public void writeStartStatement(String subject, String predicate)
			throws IOException {
		writeStatement(null, subject, predicate, null, null, null, null);
	}

	@Override
	public void writeEndStatement() throws IOException {
	}

}
