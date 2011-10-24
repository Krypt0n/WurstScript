//generated by parseq
package de.peeeq.wurstscript.ast;

class NativeTypeImpl implements NativeType, SortPosIntern {
	NativeTypeImpl(WPos source, VisibilityModifier visibility, String name, OptTypeExpr typ) {
		if (source == null) throw new IllegalArgumentException();
		((SortPosIntern)source).setParent(this);
		this.source = source;
		if (visibility == null) throw new IllegalArgumentException();
		((SortPosIntern)visibility).setParent(this);
		this.visibility = visibility;
		if (name == null) throw new IllegalArgumentException();
		this.name = name;
		if (typ == null) throw new IllegalArgumentException();
		((SortPosIntern)typ).setParent(this);
		this.typ = typ;
	}

	private SortPos parent;
	public SortPos getParent() { return parent; }
	public void setParent(SortPos parent) {
		if (parent != null && this.parent != null) { 			throw new Error("Parent of " + this + " already set: " + this.parent + "\ntried to change to " + parent); 		}
		this.parent = parent;
	}

	private WPos source;
	public void setSource(WPos source) {
		if (source == null) throw new IllegalArgumentException();
		((SortPosIntern)this.source).setParent(null);
		((SortPosIntern)source).setParent(this);
		this.source = source;
	} 
	public WPos getSource() { return source; }

	private VisibilityModifier visibility;
	public void setVisibility(VisibilityModifier visibility) {
		if (visibility == null) throw new IllegalArgumentException();
		((SortPosIntern)this.visibility).setParent(null);
		((SortPosIntern)visibility).setParent(this);
		this.visibility = visibility;
	} 
	public VisibilityModifier getVisibility() { return visibility; }

	private String name;
	public void setName(String name) {
		if (name == null) throw new IllegalArgumentException();
		this.name = name;
	} 
	public String getName() { return name; }

	private OptTypeExpr typ;
	public void setTyp(OptTypeExpr typ) {
		if (typ == null) throw new IllegalArgumentException();
		((SortPosIntern)this.typ).setParent(null);
		((SortPosIntern)typ).setParent(this);
		this.typ = typ;
	} 
	public OptTypeExpr getTyp() { return typ; }

	public SortPos get(int i) {
		switch (i) {
			case 0: return source;
			case 1: return visibility;
			case 2: return typ;
			default: throw new IllegalArgumentException("Index out of range: " + i);
		}
	}
	public int size() {
		return 3;
	}
	public NativeType copy() {
		return new NativeTypeImpl(source.copy(), visibility.copy(), name, typ.copy());
	}
	@Override public void accept(WEntities.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(WPackage.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(NativeType.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(CompilationUnit.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(WScope.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(TopLevelDeclaration.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(WEntity.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(JassToplevelDeclaration.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(TypeDef.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public void accept(PackageOrGlobal.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		typ.accept(v);
		v.visit(this);
	}
	@Override public <T> T match(TopLevelDeclaration.Matcher<T> matcher) {
		return matcher.case_NativeType(this);
	}
	@Override public void match(TopLevelDeclaration.MatcherVoid matcher) {
		matcher.case_NativeType(this);
	}

	@Override public <T> T match(WEntity.Matcher<T> matcher) {
		return matcher.case_NativeType(this);
	}
	@Override public void match(WEntity.MatcherVoid matcher) {
		matcher.case_NativeType(this);
	}

	@Override public <T> T match(TypeDef.Matcher<T> matcher) {
		return matcher.case_NativeType(this);
	}
	@Override public void match(TypeDef.MatcherVoid matcher) {
		matcher.case_NativeType(this);
	}

	@Override public <T> T match(JassToplevelDeclaration.Matcher<T> matcher) {
		return matcher.case_NativeType(this);
	}
	@Override public void match(JassToplevelDeclaration.MatcherVoid matcher) {
		matcher.case_NativeType(this);
	}

}
