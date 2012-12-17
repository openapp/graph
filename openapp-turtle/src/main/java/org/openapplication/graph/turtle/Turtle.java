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
import java.nio.charset.Charset;
import java.util.Map;

import org.openapplication.graph.streaming.GraphWriter;

public class Turtle implements GraphWriter {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	private static final byte[] PREFIX = "@prefix".getBytes(CHARSET);

	private static final byte[] SENTENCE_END = " .\n".getBytes(CHARSET);

	private static final byte[] ESCAPE_SEQ_NUMERIC_FOUR = " \\u"
			.getBytes(CHARSET);

	// private static final byte[] ESCAPE_SEQ_NUMERIC_EIGHT = " \\U00"
	// .getBytes(CHARSET);

	private static final int SPACE = ' ';

	private static final int COLON = ':';

	private static final int SEMI_COLON = ';';

	private static final int COMMA = ',';

	private static final int LESS_THAN = '<';

	private static final int GREATER_THAN = '>';

	private static final int LEFT_BRACKET = '[';

	private static final int RIGHT_BRACKET = ']';

	private static final int QUOT = '"';

	private static final int NEW_LINE = '\n';

	private final OutputStream out;

	private final Map<String, String> namespaces;

	public Turtle(OutputStream out, Map<String, String> namespaces)
			throws IOException {
		this.out = out;
		this.namespaces = namespaces;
		for (Map.Entry<String, String> namespace : namespaces.entrySet()) {
			out.write(PREFIX);
			out.write(SPACE);
			escapeIRI(namespace.getKey());
			out.write(COLON);
			out.write(SPACE);
			escapeIRI(namespace.getValue());
			out.write(SENTENCE_END);
		}
		out.write(NEW_LINE);
	}

	@Override
	public void writeStatement(String graph, String subject, String predicate,
			String object, String literal, String language, String dataType)
			throws IOException {
		if (graph != null)
			return;
		escapeIRI(subject);
		out.write(SPACE);
		escapeIRI(predicate);
		out.write(SPACE);
		if (object != null) {
			escapeIRI(object);
			out.write(SENTENCE_END);
		} else if (literal != null) {
			out.write(QUOT);
			out.write(literal.getBytes(CHARSET)); // TODO
			out.write(QUOT);
			out.write(SENTENCE_END);
			if (language != null) {
			} else if (dataType != null) {
			} else {
			}
		}
	}

	private void writeRaw(CharSequence value) throws IOException {
		charSequence = value;
		charSequenceLength = charSequence.length();
		for (charSequenceIndex = 0; charSequenceIndex < charSequenceLength; charSequenceIndex++) {
			// Get the next character, which for surrogate pairs is two chars
			int cc = nextCodePoint();

			// UTF-8 encode
			writeUTF8Seq(cc);
		}
	}

	private CharSequence charSequence;

	private int charSequenceIndex, charSequenceLength;

	private void escapeIRI(CharSequence value) throws IOException {
		out.write(LESS_THAN);
		charSequence = value;
		charSequenceLength = charSequence.length();
		for (charSequenceIndex = 0; charSequenceIndex < charSequenceLength; charSequenceIndex++) {
			// Get the next character, which for surrogate pairs is two chars
			int cc = nextCodePoint();

			// UTF-8 encode
			if (cc <= 0x7F)
				// IRI escape
				switch (cc) {
				case '<':
				case '>':
				case '"':
				case '{':
				case '}':
				case '|':
				case '^':
				case '`':
				case '\\':
					escapeSeqNumeric((char) cc);
					break;
				default:
					if (cc <= 0x20)
						escapeSeqNumeric((char) cc);
					else
						// IRI escaping is not required
						out.write(cc);
				}
			else
				// IRI escaping is not required
				writeUTF8Seq(cc);
		}
		out.write(GREATER_THAN);
	}

	private void escapeLocal(CharSequence value) throws IOException {

	}

	private void escapeString(CharSequence value) throws IOException {

	}

	private void escapeSeqNumeric(char c) throws IOException {
		out.write(ESCAPE_SEQ_NUMERIC_FOUR);
		out.write(toHexChar((c & 0xF000) >>> 12));
		out.write(toHexChar((c & 0xF00) >>> 8));
		out.write(toHexChar((c & 0xF0) >>> 4));
		out.write(toHexChar(c & 0xF));
	}

	// private void writeEscapeSeqNumeric(int cc) throws IOException {
	// out.write(ESCAPE_SEQ_NUMERIC_EIGHT);
	// // First two hex digits are always zero and have already been written
	// out.write(toHexChar((cc & 0xF00000) >>> 20));
	// out.write(toHexChar((cc & 0xF0000) >>> 16));
	// out.write(toHexChar((cc & 0xF000) >>> 12));
	// out.write(toHexChar((cc & 0xF00) >>> 8));
	// out.write(toHexChar((cc & 0xF0) >>> 4));
	// out.write(toHexChar(cc & 0xF));
	// }

	private int nextCodePoint() {
		char c = charSequence.charAt(charSequenceIndex);
		int cc = c;
		if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
			// Not a surrogate
			cc = c;
		} else { // Surrogate
			if (c > Character.MAX_HIGH_SURROGATE) // If not lead surrogate
				throw new IllegalArgumentException(
						"Tail surrogate without lead surrogate");
			if (++charSequenceIndex == charSequenceLength)
				throw new IllegalArgumentException(
						"Lead surrogate without tail surrogate");
			// Fetch tail surrogate
			char c2 = charSequence.charAt(charSequenceIndex);
			if (c2 < Character.MIN_LOW_SURROGATE
					|| c2 > Character.MAX_SURROGATE)
				throw new IllegalArgumentException("Invalid tail surrogate");
			cc = Character.MIN_SUPPLEMENTARY_CODE_POINT
					| ((c - Character.MIN_SURROGATE) << 10)
					| (c2 - Character.MIN_LOW_SURROGATE);
		}
		return cc;
	}

	private void writeUTF8Seq(int cc) throws IOException {
		if (cc <= 0x7F) {
			out.write(cc);
		} else if (cc <= 0x7FF) {
			out.write(0xC0 | (cc >>> 6));
			out.write(0x80 | (cc & 0x3F));
		} else if (cc <= 0xFFFF) {
			out.write(0xE0 | (cc >>> 12));
			out.write(0x80 | ((cc >>> 6) & 0x3F));
			out.write(0x80 | (cc & 0x3F));
		} else { // if (cc <= 0x1FFFFF) {
			out.write(0xF0 | (cc >>> 18));
			out.write(0x80 | ((cc >>> 12) & 0x3F));
			out.write(0x80 | ((cc >>> 6) & 0x3F));
			out.write(0x80 | (cc & 0x3F));
		}
	}

	private static char toHexChar(int digit) {
		return (char) (digit < 0xA ? (digit + '0') : (digit + 'A' - 0xA));
	}

	@Override
	public void collapse() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void collapse(boolean closeGraph) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void trim(int maxStripes) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxStripes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxStripes(int count) {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public void writeStartDefinition(String subject) throws IOException {
		out.write(LEFT_BRACKET);
		out.write(SPACE);
	}

	@Override
	public void writeEndDefinition() throws IOException {
		out.write(RIGHT_BRACKET);
		out.write(SEMI_COLON);
		out.write(SPACE);
	}

	@Override
	public void writeStartPredicate(String predicate) throws IOException {
		escapeIRI(predicate);
		out.write(SPACE);
	}

	@Override
	public void writeEndPredicate() throws IOException {
		out.write(SEMI_COLON);
		out.write(SPACE);
	}

	@Override
	public void writeLiteral(String literal, String language, String dataType)
			throws IOException {
		out.write(QUOT);
		out.write(literal.getBytes(CHARSET)); // TODO
		out.write(QUOT);
		out.write(COMMA);
		out.write(SPACE);
	}

	@Override
	public void writeIRI(String object) throws IOException {
		escapeIRI(object);
		out.write(COMMA);
		out.write(SPACE);
	}

	@Override
	public void writeStartStatement(String subject, String predicate)
			throws IOException {
		writeStatement(null, subject, predicate, null, null, null, null);
	}

	@Override
	public void writeEndStatement() throws IOException {
		out.write(SENTENCE_END);
	}

}
