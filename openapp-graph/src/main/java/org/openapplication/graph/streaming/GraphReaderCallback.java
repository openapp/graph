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
package org.openapplication.graph.streaming;

public interface GraphReaderCallback {

	void triple(String subject, String predicate, String object);

	void triple(String subject, String predicate, String literal,
			String language, String dataType);

	String generateBlankNode(String node);

	void startGraph(String graph);

	void endGraph(String graph);

	void startDefinition(String node);

	void endDefinition(String node);

}
