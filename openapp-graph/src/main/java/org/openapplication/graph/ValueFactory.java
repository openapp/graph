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
package org.openapplication.graph;

import java.util.List;
import java.util.Set;

public interface ValueFactory {

	Value createIRI(String iri);

	Value createBlank(String id);

	Value createLiteral(String string, String dataType);

	Value createString(String string, String language);

	Value createString(String string);

	Value createList(List<? extends Value> list);

	Value createSet(Set<? extends Value> set);

	Value createDefinition(Set<? extends Triple> definition);

	Triple createTriple(Value subject, Value predicate, Value object);

}
