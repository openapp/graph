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

import java.io.Closeable;
import java.io.IOException;

public interface GraphWriter extends Closeable {

	void writeStatement(String graph, String subject, String predicate,
			String object, String literal, String language, String dataType)
			throws IOException;

	void collapse() throws IOException;

	void collapse(boolean closeGraph) throws IOException;

	void trim(int maxStripes) throws IOException;

	int getMaxStripes();

	void setMaxStripes(int count);

	void writeStartStatement(String subject, String predicate)
			throws IOException;

	void writeEndStatement() throws IOException;

	void writeStartDefinition(String subject) throws IOException;

	void writeEndDefinition() throws IOException;

	void writeStartPredicate(String predicate) throws IOException;

	void writeEndPredicate() throws IOException;

	void writeLiteral(String literal, String language, String dataType)
			throws IOException;

	void writeIRI(String object) throws IOException;

	void flush() throws IOException;

}
