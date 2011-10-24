//generated by parseq
package de.peeeq.wurstscript.ast;

class FuncDefImpl implements FuncDef, SortPosIntern {
	FuncDefImpl(WPos source, VisibilityModifier visibility, FuncSignature signature, WStatements body) {
		if (source == null) throw new IllegalArgumentException();
		((SortPosIntern)source).setParent(this);
		this.source = source;
		if (visibility == null) throw new IllegalArgumentException();
		((SortPosIntern)visibility).setParent(this);
		this.visibility = visibility;
		if (signature == null) throw new IllegalArgumentException();
		((SortPosIntern)signature).setParent(this);
		this.signature = signature;
		if (body == null) throw new IllegalArgumentException();
		((SortPosIntern)body).setParent(this);
		this.body = body;
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

	private FuncSignature signature;
	public void setSignature(FuncSignature signature) {
		if (signature == null) throw new IllegalArgumentException();
		((SortPosIntern)this.signature).setParent(null);
		((SortPosIntern)signature).setParent(this);
		this.signature = signature;
	} 
	public FuncSignature getSignature() { return signature; }

	private WStatements body;
	public void setBody(WStatements body) {
		if (body == null) throw new IllegalArgumentException();
		((SortPosIntern)this.body).setParent(null);
		((SortPosIntern)body).setParent(this);
		this.body = body;
	} 
	public WStatements getBody() { return body; }

	public SortPos get(int i) {
		switch (i) {
			case 0: return source;
			case 1: return visibility;
			case 2: return signature;
			case 3: return body;
			default: throw new IllegalArgumentException("Index out of range: " + i);
		}
	}
	public int size() {
		return 4;
	}
	public FuncDef copy() {
		return new FuncDefImpl(source.copy(), visibility.copy(), signature.copy(), body.copy());
	}
	@Override public void accept(WEntities.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(WPackage.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(ClassMember.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(CompilationUnit.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(ClassDef.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(WScope.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(FunctionDefinition.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(FuncDef.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(TopLevelDeclaration.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(WEntity.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(JassToplevelDeclaration.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(TypeDef.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(ClassSlots.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(ClassSlot.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public void accept(PackageOrGlobal.Visitor v) {
		source.accept(v);
		visibility.accept(v);
		signature.accept(v);
		body.accept(v);
		v.visit(this);
	}
	@Override public <T> T match(TopLevelDeclaration.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(TopLevelDeclaration.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

	@Override public <T> T match(WScope.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(WScope.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

	@Override public <T> T match(FunctionDefinition.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(FunctionDefinition.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

	@Override public <T> T match(WEntity.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(WEntity.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

	@Override public <T> T match(ClassMember.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(ClassMember.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

	@Override public <T> T match(ClassSlot.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(ClassSlot.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

	@Override public <T> T match(JassToplevelDeclaration.Matcher<T> matcher) {
		return matcher.case_FuncDef(this);
	}
	@Override public void match(JassToplevelDeclaration.MatcherVoid matcher) {
		matcher.case_FuncDef(this);
	}

}
