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
import java.util.Collection;
import java.util.Collections;

import org.openapplication.graph.streaming.GraphReaderCallback;
import org.openapplication.graph.streaming.GraphStreamer;
import org.openapplication.graph.streaming.GraphWriter;
import org.openapplication.json.JSONException;

public final class JSONLDStreamer implements GraphStreamer {

	@Override
	public void read(InputStream in, String mediaType,
			GraphReaderCallback callback) throws IOException {
		try {
			JSONLD.parse(in, callback);
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	@Override
	public GraphWriter createWriter(OutputStream out, String mediaType)
			throws IOException {
		return new JSONLD(out);
	}

	@Override
	public Collection<String> getReadMediaTypes() {
		return Collections.singleton("application/ld+json");
	}

	@Override
	public Collection<String> getWriteMediaTypes() {
		return Collections.singleton("application/ld+json");
	}

}
