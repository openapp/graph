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
package org.openapplication.graph.jsonld.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openapplication.graph.jsonld.JSONLD;
import org.openapplication.graph.streaming.GraphReaderCallback;
import org.openapplication.json.JSONException;

public class JSONLDTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testParse() throws JSONException {
		String string = "[{ \"@context\": { \"ex\": \"http://example.com/\" }, \"@id\": \"ex:test\", \"ex:predicate\": { \"@id\": \"ex:object\" } }]";
		JSONLD.parse(string, new GraphReaderCallback() {
			@Override
			public void triple(String subject, String predicate,
					String literal, String language, String dataType) {
				System.out.println(subject + " " + predicate + " " + literal
						+ " " + language + " " + dataType);
			}

			@Override
			public void triple(String subject, String predicate, String object) {
				System.out.println(subject + " " + predicate + " " + object);
			}

			@Override
			public void startGraph(String graph) {
				// TODO Auto-generated method stub

			}

			@Override
			public void endGraph(String graph) {
				// TODO Auto-generated method stub

			}

			@Override
			public void startDefinition(String node) {
				// TODO Auto-generated method stub

			}

			@Override
			public void endDefinition(String node) {
				// TODO Auto-generated method stub

			}

			@Override
			public String generateBlankNode(String node) {
				// TODO Auto-generated method stub
				return null;
			}
		});
	}

}
