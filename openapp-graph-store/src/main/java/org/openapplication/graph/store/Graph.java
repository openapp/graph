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
package org.openapplication.graph.store;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.openapplication.encoding.Id;
import org.openapplication.graph.Triple;
import org.openapplication.graph.Value;
import org.openapplication.graph.ValueFactory;
import org.openapplication.graph.Values;
import org.openapplication.graph.streaming.GraphReaderCallback;
import org.openapplication.store.Definition;
import org.openapplication.store.Entry;
import org.openapplication.store.EntryConverter;
import org.openapplication.store.EntryFilter;
import org.openapplication.store.Field;
import org.openapplication.store.FieldRange;
import org.openapplication.store.FieldValue;
import org.openapplication.store.Store;
import org.openapplication.store.StoreClient;
import org.openapplication.store.StoreImpl;
import org.openapplication.store.EntryFilter.Evaluation;

public final class Graph {

	private enum LongField implements Field<Long> {

		SUBJECT("http://purl.org/openapp/fields/subject.long"),

		PREDICATE("http://purl.org/openapp/fields/predicate.long"),

		OBJECT("http://purl.org/openapp/fields/object.long");

		private final UUID uuid;

		private final String uri;

		private LongField(String uri) {
			this.uuid = Id.asUuid(URI.create(uri));
			this.uri = uri;
		}

		@Override
		public Field<Long> toField() {
			return this;
		}

		@Override
		public UUID toUuid() {
			return uuid;
		}

		@Override
		public String toUri() {
			return uri;
		}

		@Override
		public FieldValue<Long> value(Long value) {
			return new FieldValue<Long>(this, value);
		}

		@Override
		public FieldRange<Long> range(Long min, Long max) {
			return new FieldRange<Long>(this, min, max);
		}

		@Override
		public int size() {
			return 8;
		}

		@Override
		public ByteBuffer toBytes(Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void put(ByteBuffer buffer, Object value) {
			buffer.putLong((Long) value);
		}

		@Override
		public Long get(ByteBuffer buffer) {
			return buffer.getLong();
		}

		@Override
		public String toString(Object value) {
			return Long.toHexString(((Long) value));
		}

	}

	private static final Definition S_P_O = new Definition(new Field<?>[] {
			LongField.SUBJECT, LongField.PREDICATE, LongField.OBJECT },
			new Field<?>[] {});

	private static final Definition P_O_S = new Definition(new Field<?>[] {
			LongField.PREDICATE, LongField.OBJECT, LongField.SUBJECT },
			new Field<?>[] {});

	private final EntryConverter.Convert<Triple> ENTRY_TO_STATEMENT = //
	new EntryConverter.Convert<Triple>() {
		@Override
		public Triple convert(Entry entry) {
			return valueFactory.createTriple(
					toEntity(entry.get(LongField.SUBJECT)),
					toEntity(entry.get(LongField.PREDICATE)),
					toEntity(entry.get(LongField.OBJECT)));
		}
	};

	private final EntryConverter.Convert<Value> ENTRY_TO_OBJECT = //
	new EntryConverter.Convert<Value>() {
		@Override
		public Value convert(Entry entry) {
			return toEntity(entry.get(LongField.OBJECT));
		}
	};

	private final EntryConverter.Convert<Value> ENTRY_TO_SUBJECT = //
	new EntryConverter.Convert<Value>() {
		@Override
		public Value convert(Entry entry) {
			return toEntity(entry.get(LongField.SUBJECT));
		}
	};

	private final EntryConverter.Convert<Value> ENTRY_TO_PREDICATE = //
	new EntryConverter.Convert<Value>() {
		@Override
		public Value convert(Entry entry) {
			return toEntity(entry.get(LongField.PREDICATE));
		}
	};

	private final Map<Value, Long> entityToId = new ConcurrentHashMap<Value, Long>();

	private final Map<Long, Value> idToEntity = new ConcurrentHashMap<Long, Value>();

	private final Store store = new StoreClient(new StoreImpl(new Definition[] {
			S_P_O, P_O_S }));

	private long index = 1;

	private int nextBlankNode = 0;

	private final Graph context;

	private final ValueFactory valueFactory;

	public Graph() {
		context = this;
		valueFactory = Values.INSTANCE;
	}

	public Graph(ValueFactory valueFactory) {
		context = this;
		this.valueFactory = valueFactory;
	}

	private Graph(Graph parent) {
		context = parent;
		valueFactory = context.valueFactory;
	}

	private Long toId(Value entity) {
		if (entity == null)
			return 0L;
		Long id = context.entityToId.get(entity);
		if (id != null)
			return id;
		context.entityToId.put(entity, id = context.index++);
		context.idToEntity.put(id, entity);
		return id;
	}

	private Value toEntity(Long id) {
		if (id == 0L)
			return null;
		return context.idToEntity.get(id);
	}

	public void rename(Value oldName, Value newName) {
		if (newName == null) // if (oldName == null || newName == null)
			throw new NullPointerException();
		Long id = toId(oldName);
		context.idToEntity.put(id, newName);
		context.entityToId.put(newName, id);
		Long newId = context.entityToId.get(newName);
		if (newId != null)
			for (Entry entry : store.iterate(LongField.SUBJECT,
					LongField.PREDICATE, LongField.OBJECT)) {
				Long newSubject = null, newPredicate = null, newObject = null;
				Long subject = entry.get(LongField.SUBJECT), predicate = entry
						.get(LongField.PREDICATE), object = entry
						.get(LongField.OBJECT);
				if (newId.equals(subject))
					newSubject = id;
				if (newId.equals(predicate))
					newPredicate = id;
				if (newId.equals(object))
					newObject = id;
				if (newSubject != null || newPredicate != null
						|| newObject != null) {
					store.remove(LongField.SUBJECT.value(subject),
							LongField.PREDICATE.value(predicate),
							LongField.OBJECT.value(object));
					store.remove(LongField.PREDICATE.value(predicate),
							LongField.OBJECT.value(object),
							LongField.SUBJECT.value(subject));
					store.put(LongField.SUBJECT
							.value(newSubject == null ? subject : newSubject),
							LongField.PREDICATE
									.value(newPredicate == null ? predicate
											: newPredicate), LongField.OBJECT
									.value(newObject == null ? object
											: newObject));
				}
			}
	}

	public void add(Iterable<Triple> statements) {
		add(statements.iterator());
	}

	public void add(Iterator<Triple> statements) {
		while (statements.hasNext()) {
			Triple statement = statements.next();
			store.put(LongField.SUBJECT.value(toId(statement.getSubject())),
					LongField.PREDICATE.value(toId(statement.getPredicate())),
					LongField.OBJECT.value(toId(statement.getObject())));
		}
	}

	public void add(Triple statement) {
		store.put(LongField.SUBJECT.value(toId(statement.getSubject())),
				LongField.PREDICATE.value(toId(statement.getPredicate())),
				LongField.OBJECT.value(toId(statement.getObject())));
	}

	public Iterable<Triple> describe(Value subject) {
		return new EntryConverter<Triple>(store.iterate(
				LongField.SUBJECT.value(toId(subject)), LongField.PREDICATE,
				LongField.OBJECT), ENTRY_TO_STATEMENT);
	}

	public Iterable<Value> entities() {
		return entityToId.keySet();
	}

	public Iterable<Value> properties(Value subject) {
		return new EntryConverter<Value>(new EntryFilter(
				Collections.singleton(store.iterate(
						LongField.SUBJECT.value(toId(subject)),
						LongField.PREDICATE, LongField.OBJECT)),
				new EntryFilter.Evaluate() {
					Value lastSubject;

					@Override
					public Evaluation evaluate(Entry entry) {
						return entry.get(LongField.SUBJECT).equals(lastSubject) ? Evaluation.REJECT
								: Evaluation.ACCEPT;
					}
				}), ENTRY_TO_PREDICATE);
	}

	public Iterable<Value> values(Value subject, Value predicate) {
		return new EntryConverter<Value>(store.iterate(
				LongField.SUBJECT.value(toId(subject)),
				LongField.PREDICATE.value(toId(predicate)), LongField.OBJECT),
				ENTRY_TO_OBJECT);
	}

	public Iterable<Triple> find(Value subject, Value predicate, Value object) {
		final Long subjectId = subject == null ? null : entityToId.get(subject);
		if (subject != null && subjectId == null)
			return Collections.emptyList();
		final Long predicateId = predicate == null ? null : entityToId
				.get(predicate);
		if (predicate != null && predicateId == null)
			return Collections.emptyList();
		final Long objectId = object == null ? null : entityToId.get(object);
		if (object != null && objectId == null)
			return Collections.emptyList();
		return new EntryConverter<Triple>(new EntryFilter(
				Collections.singleton(store.iterate(LongField.SUBJECT,
						LongField.PREDICATE, LongField.OBJECT)),
				new EntryFilter.Evaluate() {
					@Override
					public Evaluation evaluate(Entry entry) {
						if (subjectId != null
								&& subjectId != entry.get(LongField.SUBJECT))
							return Evaluation.REJECT;
						if (predicateId != null
								&& predicateId != entry
										.get(LongField.PREDICATE))
							return Evaluation.REJECT;
						if (objectId != null
								&& objectId != entry.get(LongField.OBJECT))
							return Evaluation.REJECT;
						return Evaluation.ACCEPT;
					}
				}), ENTRY_TO_STATEMENT);
	}

	public Value first(Value subject, Value predicate) {
		EntryConverter<Value> list = new EntryConverter<Value>(store.iterate(
				LongField.SUBJECT.value(toId(subject)),
				LongField.PREDICATE.value(toId(predicate)), LongField.OBJECT),
				ENTRY_TO_OBJECT);
		Iterator<Value> iter = list.iterator();
		Value entity = iter.hasNext() ? iter.next() : null;
		return entity;
	}

	public Iterable<Value> project(Value predicate) {
		final Long predicateId = entityToId.get(predicate);
		if (predicateId == null)
			return Collections.emptyList();
		return new EntryConverter<Value>(new EntryFilter(
				Collections.singleton(store.iterate(LongField.PREDICATE,
						LongField.OBJECT, LongField.SUBJECT)),
				new EntryFilter.Evaluate() {
					@Override
					public Evaluation evaluate(Entry entry) {
						if (predicateId != entry.get(LongField.PREDICATE))
							return Evaluation.REJECT;
						return Evaluation.ACCEPT;
					}
				}), ENTRY_TO_SUBJECT);
	}

	public Iterable<Value> project(Value predicate, Value object) {
		final Long predicateId = entityToId.get(predicate);
		if (predicateId == null)
			return Collections.emptyList();
		final Long objectId = entityToId.get(object);
		if (objectId == null)
			return Collections.emptyList();
		return new EntryConverter<Value>(new EntryFilter(
				Collections.singleton(store.iterate(LongField.PREDICATE,
						LongField.OBJECT, LongField.SUBJECT)),
				new EntryFilter.Evaluate() {
					@Override
					public Evaluation evaluate(Entry entry) {
						if (predicateId != entry.get(LongField.PREDICATE))
							return Evaluation.REJECT;
						if (objectId != entry.get(LongField.OBJECT))
							return Evaluation.REJECT;
						return Evaluation.ACCEPT;
					}
				}), ENTRY_TO_SUBJECT);
	}

	public Iterable<Value> project(final Triple... properties) {
		if (properties.length == 0)
			throw new IllegalArgumentException();
		return new Iterable<Value>() {
			@Override
			public Iterator<Value> iterator() {
				return new Iterator<Value>() {

					@SuppressWarnings("unchecked")
					private Iterator<Entry>[] iterators = //
					(Iterator<Entry>[]) new Iterator<?>[properties.length];
					{
						for (int i = 0; i < properties.length; i++) {
							Triple property = properties[i];
							iterators[i] = store.iterate(
									LongField.PREDICATE.value(toId(property
											.getPredicate())),
									LongField.OBJECT.value(toId(property
											.getObject())), LongField.SUBJECT)
									.iterator();
						}
					}

					private boolean endOfIteration = false;

					private long current[] = new long[iterators.length];
					{
						for (int i = 0; i < iterators.length; i++) {
							if (!iterators[i].hasNext()) {
								endOfIteration = true;
								break;
							}
							current[i] = iterators[i].next().get(
									LongField.SUBJECT);
						}
					}

					private long candidate = Long.MIN_VALUE;

					private boolean hasNext = false;

					private boolean check() {
						if (endOfIteration)
							return false;
						int score = 0;
						while (score < iterators.length) {
							score = 0;
							for (int i = 0; i < iterators.length; i++) {
								while (current[i] < candidate) {
									if (!iterators[i].hasNext()) {
										endOfIteration = true;
										return false;
									}
									current[i] = iterators[i].next().get(
											LongField.SUBJECT);
								}
								if (current[i] == candidate)
									score++;
								else if (current[i] > candidate)
									candidate = current[i];
							}
						}
						return true;
					}

					@Override
					public boolean hasNext() {
						return hasNext ? true : check();
					}

					@Override
					public Value next() {
						Value result = toEntity(candidate);
						candidate++;
						return result;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

				};
			}
		};
	}

	private GraphReaderCallback graphReaderCallback;

	public GraphReaderCallback toReader() {
		if (graphReaderCallback != null)
			return graphReaderCallback;
		return graphReaderCallback = toReader(new GraphCallback() {
			@Override
			public void triple(Value subject, Value predicate, Value object) {
			}

			@Override
			public void node(Value node) {
			}

			@Override
			public void graph(Value name, Graph graph) {
			}
		});
	}

	public GraphReaderCallback toReader(final GraphCallback callback) {
		return new GraphReaderCallback() {
			Deque<Graph> graphs = new ArrayDeque<Graph>();

			{
				graphs.push(Graph.this);
			}

			@Override
			public String generateBlankNode(String node) {
				return "_:t" + nextBlankNode++;
			}

			private Value toEntity(String uri) {
				return uri.startsWith("_:") ? valueFactory.createBlank(uri
						.substring(2)) : valueFactory.createIRI(uri);
			}

			@Override
			public void triple(String subject, String predicate,
					String literal, String language, String dataType) {
				Value s = toEntity(subject);
				Value p = toEntity(predicate);
				Value o;
				if (language != null)
					o = valueFactory.createString(literal, language);
				else if (dataType != null)
					o = valueFactory.createLiteral(literal, dataType);
				else
					o = valueFactory.createString(literal);
				graphs.peek().add(valueFactory.createTriple(s, p, o));
				callback.triple(s, p, o);
			}

			@Override
			public void triple(String subject, String predicate, String object) {
				Value s = toEntity(subject);
				Value p = toEntity(predicate);
				Value o = toEntity(object);
				graphs.peek().add(valueFactory.createTriple(s, p, o));
				callback.triple(s, p, o);
			}

			@Override
			public void startGraph(String graph) {
				graphs.push(new Graph(context));
			}

			@Override
			public void endGraph(String graph) {
				callback.graph(toEntity(graph), graphs.pop());
			}

			@Override
			public void startDefinition(String node) {
			}

			@Override
			public void endDefinition(String node) {
				callback.node(toEntity(node));
			}
		};
	}

}
