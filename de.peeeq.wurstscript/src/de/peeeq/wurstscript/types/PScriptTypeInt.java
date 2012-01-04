package de.peeeq.wurstscript.types;


public class PScriptTypeInt extends PscriptType {

	private static final PScriptTypeInt instance = new PScriptTypeInt();

	// make constructor private as we only need one instance
	protected PScriptTypeInt() {}
	
	@Override
	public boolean isSubtypeOf(PscriptType other) {
		return other instanceof PScriptTypeInt || other instanceof PscriptTypeTypeParam;
	}


	@Override
	public String getName() {
		return "integer";
	}

	@Override
	public String getFullName() {
		return "integer";
	}

	public static PScriptTypeInt instance() {
		return instance;
	}
	
	@Override
	public String printJass() {
		return getName();
	}

}
