//generated by parseq
package de.peeeq.wurstscript.ast;

public abstract class WImports extends ParseqList<WImport> implements SortPos{
	public WImports copy() {
		WImports result = new WImportsImpl();
		result.addAll(this);
		return result;
	}
	public abstract void accept(WPackage.Visitor v);
	public abstract void accept(CompilationUnit.Visitor v);
	public abstract void accept(WScope.Visitor v);
	public abstract void accept(TopLevelDeclaration.Visitor v);
	public abstract void accept(WImports.Visitor v);
	public abstract void accept(PackageOrGlobal.Visitor v);
	public interface Visitor {
		void visit(WImport wImport);
		void visit(WPos wPos);
		void visit(WImports wImports);
	}
	public static abstract class DefaultVisitor implements Visitor {
		@Override public void visit(WImport wImport) {}
		@Override public void visit(WPos wPos) {}
		@Override public void visit(WImports wImports) {}
	}
}
