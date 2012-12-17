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

public final class Values implements ValueFactory {

	public static final Values INSTANCE = new Values();

	private static class AbstractValue implements Value {

		@Override
		public String toIri() {
			return null;
		}

		@Override
		public String getId() {
			return null;
		}

		@Override
		public String getType() {
			return null;
		}

		@Override
		public String getLanguage() {
			return null;
		}

		@Override
		public String getString() {
			return null;
		}

		@Override
		public boolean isIri() {
			return false;
		}

		@Override
		public List<? extends Value> getList() {
			return null;
		}

		@Override
		public Set<? extends Value> getSet() {
			return null;
		}

		@Override
		public Set<? extends Triple> getDefinition() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((getDefinition() == null) ? 0 : getDefinition()
							.hashCode());
			result = prime * result
					+ ((getId() == null) ? 0 : getId().hashCode());
			result = prime * result + (isIri() ? 1231 : 1237);
			result = prime * result
					+ ((getLanguage() == null) ? 0 : getLanguage().hashCode());
			result = prime * result
					+ ((getList() == null) ? 0 : getList().hashCode());
			result = prime * result
					+ ((getSet() == null) ? 0 : getSet().hashCode());
			result = prime * result
					+ ((getString() == null) ? 0 : getString().hashCode());
			result = prime * result
					+ ((getType() == null) ? 0 : getType().hashCode());
			result = prime * result
					+ ((toIri() == null) ? 0 : toIri().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AbstractValue))
				return false;
			AbstractValue other = (AbstractValue) obj;
			if (getDefinition() == null) {
				if (other.getDefinition() != null)
					return false;
			} else if (!getDefinition().equals(other.getDefinition()))
				return false;
			if (getId() == null) {
				if (other.getId() != null)
					return false;
			} else if (!getId().equals(other.getId()))
				return false;
			if (isIri() != other.isIri())
				return false;
			if (getLanguage() == null) {
				if (other.getLanguage() != null)
					return false;
			} else if (!getLanguage().equals(other.getLanguage()))
				return false;
			if (getList() == null) {
				if (other.getList() != null)
					return false;
			} else if (!getList().equals(other.getList()))
				return false;
			if (getSet() == null) {
				if (other.getSet() != null)
					return false;
			} else if (!getSet().equals(other.getSet()))
				return false;
			if (getString() == null) {
				if (other.getString() != null)
					return false;
			} else if (!getString().equals(other.getString()))
				return false;
			if (getType() == null) {
				if (other.getType() != null)
					return false;
			} else if (!getType().equals(other.getType()))
				return false;
			if (toIri() == null) {
				if (other.toIri() != null)
					return false;
			} else if (!toIri().equals(other.toIri()))
				return false;
			return true;
		}

	}

	public static Value iri(final String iri) {
		return new AbstractValue() {
			@Override
			public String toIri() {
				return iri;
			}

			@Override
			public boolean isIri() {
				return true;
			}
		};
	}

	public static Value blank(final String id) {
		return new AbstractValue() {
			@Override
			public String getId() {
				return id;
			}
		};
	}

	public static Value literal(final String string, final String dataType) {
		return new AbstractValue() {
			@Override
			public String getString() {
				return string;
			}

			@Override
			public String getType() {
				return dataType;
			}
		};
	}

	public static Value string(final String string, final String language) {
		return new AbstractValue() {
			@Override
			public String getString() {
				return string;
			}

			@Override
			public String getLanguage() {
				return language;
			}
		};
	}

	public static Value string(final String string) {
		return new AbstractValue() {
			@Override
			public String getString() {
				return string;
			}
		};
	}

	public static Value list(final List<? extends Value> list) {
		return new AbstractValue() {
			@Override
			public List<? extends Value> getList() {
				return list;
			}
		};
	}

	public static Value set(final Set<? extends Value> set) {
		return new AbstractValue() {
			@Override
			public Set<? extends Value> getSet() {
				return set;
			}
		};
	}

	public static Value definition(final Set<? extends Triple> definition) {
		return new AbstractValue() {
			@Override
			public Set<? extends Triple> getDefinition() {
				return definition;
			}
		};
	}

	public static Triple triple(final Value subject, final Value predicate,
			final Value object) {
		return new Triple() {
			@Override
			public Value getSubject() {
				return subject;
			}

			@Override
			public Value getPredicate() {
				return predicate;
			}

			@Override
			public Value getObject() {
				return object;
			}
		};
	}

	private Values() {
	}

	@Override
	public Value createIRI(String iri) {
		return iri(iri);
	}

	@Override
	public Value createBlank(String id) {
		return blank(id);
	}

	@Override
	public Value createLiteral(String string, String dataType) {
		return literal(string, dataType);
	}

	@Override
	public Value createString(String string, String language) {
		return string(string, language);
	}

	@Override
	public Value createString(String string) {
		return string(string);
	}

	@Override
	public Value createList(List<? extends Value> list) {
		return list(list);
	}

	@Override
	public Value createSet(Set<? extends Value> set) {
		return set(set);
	}

	@Override
	public Value createDefinition(Set<? extends Triple> definition) {
		return definition(definition);
	}

	@Override
	public Triple createTriple(Value subject, Value predicate, Value object) {
		return triple(subject, predicate, object);
	}

}
