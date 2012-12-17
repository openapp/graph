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
package org.openapplication.graph.turtle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openapplication.binder.Bindable;
import org.openapplication.binder.Binder;
import org.openapplication.graph.streaming.GraphWriter;
import org.openapplication.graph.streaming.GraphWriterFactory;

public final class TurtleStreamer implements Bindable, GraphWriterFactory {

	private final Map<String, String> namespaces = new LinkedHashMap<String, String>();

	@Override
	public void bind(Binder binder) {
		for (Object instance : binder.getInstances(binder.getApplication(),
				"http://purl.org/openapp/server/namespace"))
			for (String value : binder.getValues(instance,
					"http://purl.org/openapp/server/namespaceValue"))
				for (String prefix : binder.getValues(instance,
						"http://purl.org/openapp/server/namespacePrefix"))
					if (!prefix.isEmpty() && !namespaces.containsKey(prefix))
						namespaces.put(prefix, value);
	}

	@Override
	public GraphWriter createWriter(OutputStream out, String mediaType)
			throws IOException {
		return new Turtle(out, namespaces);
	}

	@Override
	public Collection<String> getWriteMediaTypes() {
		return Collections.singleton("text/turtle");
	}

}
