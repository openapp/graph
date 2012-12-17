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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openapplication.binder.Bindable;
import org.openapplication.binder.Binder;

public final class GraphStreamerImpl implements Bindable, GraphStreamer {

	private final Map<String, GraphReader> readers = new LinkedHashMap<String, GraphReader>();

	private final Map<String, GraphWriterFactory> writers = new LinkedHashMap<String, GraphWriterFactory>();

	private final Collection<String> readMediaTypes = Collections
			.unmodifiableSet(readers.keySet());

	private final Collection<String> writeMediaTypes = Collections
			.unmodifiableSet(writers.keySet());

	@Override
	public void bind(Binder binder) {
		for (Object instance : binder.getInstances(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
				"http://purl.org/openapp/server/GraphReader"))
			for (String mediaType : ((GraphReader) instance)
					.getReadMediaTypes())
				readers.put(mediaType, (GraphReader) instance);

		for (Object instance : binder.getInstances(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
				"http://purl.org/openapp/server/GraphWriter"))
			for (String mediaType : ((GraphWriterFactory) instance)
					.getWriteMediaTypes())
				writers.put(mediaType, (GraphWriterFactory) instance);
	}

	@Override
	public void read(InputStream in, String mediaType,
			GraphReaderCallback callback) throws IOException {
		GraphReader reader = readers.get(mediaType);
		if (reader == null)
			throw new IllegalArgumentException(
					"There is no graph reader for the specified media type: "
							+ mediaType);
		reader.read(in, mediaType, callback);
	}

	@Override
	public GraphWriter createWriter(OutputStream out, String mediaType)
			throws IOException {
		GraphWriterFactory writerFactory = writers.get(mediaType);
		if (writerFactory == null)
			throw new IllegalArgumentException(
					"There is no graph writer for the specified media type: "
							+ mediaType);
		return writerFactory.createWriter(out, mediaType);
	}

	@Override
	public Collection<String> getReadMediaTypes() {
		return readMediaTypes;
	}

	@Override
	public Collection<String> getWriteMediaTypes() {
		return writeMediaTypes;
	}

}
