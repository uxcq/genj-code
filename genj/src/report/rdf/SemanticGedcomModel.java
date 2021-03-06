package rdf;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class SemanticGedcomModel {

	private static final String PREDICATE = "http://genj.sourceforge.net/rdf/GedcomTags/predicate#";
	public final static Map<String, String> PREFIXES = new HashMap<String, String>();
	static {
		PREFIXES.put("p", PREDICATE);
		PREFIXES.put("t", "http://genj.sourceforge.net/rdf/GedcomTags/type#");
		PREFIXES.put("r", "http://genj.sourceforge.net/rdf/GedcomTags/rule#");
		PREFIXES.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		PREFIXES.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");

	}
	
	private final Model model = ModelFactory.createDefaultModel();
	final Property valueProperty = model.createProperty(PREFIXES.get("rdfs")+"label");
	private final Property idProperty = model.createProperty(PREDICATE + "id");
	
	private final Map<String, Property> properties = new HashMap<String, Property>();
	private final Map<String, Resource> types = new HashMap<String, Resource>();
	private final Map<String, String> uriFormats;

	/**
	 * @param uriFormats
	 *            pairs of entity tags and URIs (preferably URLs)
	 */
	public SemanticGedcomModel(final Map<String, String> uriFormats) {
		this.uriFormats = uriFormats;

		getModel().setNsPrefixes(PREFIXES);
	}

	private Resource toType(final String tag) {
		if (!types.containsKey(tag))
			types.put(tag, model.createResource(PREFIXES.get("t") + tag));
		return types.get(tag);
	}

	private Property toProperty(final String tag) {
		if (!properties.containsKey(tag))
			properties.put(tag, model.createProperty(PREFIXES.get("p") + tag));
		return properties.get(tag);
	}

	public Resource addEntity(final String id, final String tag) {
		final Resource resource = model.createResource(toUri(id, tag), toType(tag));
		resource.addLiteral(idProperty, id);
		return resource;
	}

	private String toUri(final String id, final String tag) {
		return MessageFormat.format(uriFormats.get(tag), id);
	}

	public Resource addProperty(final Resource resource, final String tag, String value) {
		final Resource property = model.createResource(toType(tag));
		resource.addProperty(toProperty(tag), property);
		if (value != null && value.trim().length() > 0) {
			property.addProperty(valueProperty, value);
		}
		return property;
	}

	public void addLiteral(final Resource resource, final String value) {
		resource.addProperty(valueProperty, value);
	}

	public void addLiteral(final Resource resource, final Object value) {
		resource.addLiteral(valueProperty, value);
	}

	public Model getModel() {
		return model;
	}

	public void addConnection(final Resource referrer, final String id, final String referrerTag, final String referredTag) {
		Resource referred = model.getResource(toUri(id, referredTag));
		referrer.addProperty(toProperty(referrerTag),referred);
	}
}
