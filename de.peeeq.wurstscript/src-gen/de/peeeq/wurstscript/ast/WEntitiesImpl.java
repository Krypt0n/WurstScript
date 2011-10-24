//generated by parseq
package de.peeeq.wurstscript.ast;

class WEntitiesImpl extends WEntities implements SortPosIntern {
 	private SortPos parent;
	public SortPos getParent() { return parent; }
	public void setParent(SortPos parent) {
		if (parent != null && this.parent != null) { 			throw new Error("Parent of " + this + " already set: " + this.parent + "\ntried to change to " + parent); 		}
		this.parent = parent;
	}

	protected void other_setParentToThis(WEntity t) {
		((SortPosIntern) t).setParent(this);
	}
	protected void other_clearParent(WEntity t) {
		((SortPosIntern) t).setParent(null);
	}
	@Override public void accept(WEntities.Visitor v) {
		for (WEntity i : this ) {
			i.accept(v);
		}
		v.visit(this);
	}
	@Override public void accept(WPackage.Visitor v) {
		for (WEntity i : this ) {
			i.accept(v);
		}
		v.visit(this);
	}
	@Override public void accept(CompilationUnit.Visitor v) {
		for (WEntity i : this ) {
			i.accept(v);
		}
		v.visit(this);
	}
	@Override public void accept(WScope.Visitor v) {
		for (WEntity i : this ) {
			i.accept(v);
		}
		v.visit(this);
	}
	@Override public void accept(TopLevelDeclaration.Visitor v) {
		for (WEntity i : this ) {
			i.accept(v);
		}
		v.visit(this);
	}
	@Override public void accept(PackageOrGlobal.Visitor v) {
		for (WEntity i : this ) {
			i.accept(v);
		}
		v.visit(this);
	}
}
