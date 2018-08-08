package de.peeeq.wurstscript.intermediatelang.optimizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.peeeq.wurstscript.jassIm.*;
import de.peeeq.wurstscript.translation.imoptimizer.OptimizerPass;
import de.peeeq.wurstscript.translation.imtranslation.AssertProperty;
import de.peeeq.wurstscript.translation.imtranslation.ImTranslator;
import de.peeeq.wurstscript.utils.Utils;
import org.eclipse.jdt.annotation.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class TempMerger implements OptimizerPass {
    private int totalMerged = 0;


    @Override
    public String getName() {
        return "Temp variables merged";
    }

    /**
     * @return The amount of merged temp variables
     */
    @Override
    public int optimize(ImTranslator trans) {
        containsFuncCall.clear();
        readsVar.clear();
        readsGlobal.clear();
        ImProg prog = trans.getImProg();
        totalMerged = 0;
        trans.assertProperties(AssertProperty.FLAT, AssertProperty.NOTUPLES);
        prog.clearAttributes();
        for (ImFunction f : prog.getFunctions()) {
            optimizeFunc(f);
        }
        // flatten the program because we introduced null-statements
        prog.flatten(trans);
        return totalMerged;
    }

    private void optimizeFunc(ImFunction f) {
        optimizeStatements(f.getBody());
    }

    private void optimizeStatements(ImStmts stmts) {
        Knowledge kn = new Knowledge();

        while (true) { // repeat while there are changes found
            kn.clear();
            // this terminates, because each replacement eliminates one set-statement
            // FIXME this is no longer true, because assignments which are used more than once are not removed
            List<Replacement> replacements = new ArrayList<>();
            for (ImStmt s : stmts) {
                if (s instanceof ImSet) {
                    ImSet imSet = (ImSet) s;
                    if (imSet.getRight() instanceof ImVarAccess) {
                        ImVarAccess right = (ImVarAccess) imSet.getRight();
                        if (imSet.getLeft() == right.getVar()) {
                            // statement has the form 'x = x' so remove it
                            totalMerged++;
                            imSet.replaceBy(JassIm.ImNull());
                            continue;
                        }
                    }
                }


                processStatement(s, kn, replacements);
            }
            if (replacements.isEmpty()) {
                break;
            }
            for (Replacement replacement : replacements) {
                // do the replacement
                totalMerged++;
                replacement.apply();
            }
        }

        // process nested statements:
        for (ImStmt s : stmts) {
            if (s instanceof ImIf) {
                ImIf imIf = (ImIf) s;
                optimizeStatements(imIf.getThenBlock());
                optimizeStatements(imIf.getElseBlock());
            } else if (s instanceof ImLoop) {
                ImLoop imLoop = (ImLoop) s;
                optimizeStatements(imLoop.getBody());
            }
        }
    }

    private void processStatement(ImStmt s, Knowledge kn, List<Replacement> replacements) {
        Replacement rep = getPossibleReplacement(s, kn);
        if (rep != null) {
            replacements.add(rep);
        }
        if (containsFuncCall(s)) {
            kn.invalidateGlobals();
        }
        if (readsGlobal(s)) {
            kn.invalidateMutatingExpressions();
        }
        if (s instanceof ImSet) {
            ImSet imSet = (ImSet) s;
            // update the knowledge with the new set statement
            kn.update(imSet.getLeft(), imSet);
        } else if (s instanceof ImSetArray) {
            kn.invalidateVar(((ImSetArray) s).getLeft());
        } else if (s instanceof ImExitwhen || s instanceof ImIf || s instanceof ImLoop) {
            kn.clear();
            // TODO this could be more precise for local variables,
            // but for now we just forget everything if we see a loop or if statement
        }
    }

    private @Nullable Replacement getPossibleReplacement(Element elem, Knowledge kn) {
        if (kn.isEmpty()) {
            return null;
        }
        if (elem instanceof ImVarAccess) {
            ImVarAccess va = (ImVarAccess) elem;
            return kn.getReplacementIfPossible(va);
        } else if (elem instanceof ImLoop) {
            return null;
        } else if (elem instanceof ImIf) {
            ImIf imIf = (ImIf) elem;
            return getPossibleReplacement(imIf.getCondition(), kn);
        } else if (elem instanceof ImOperatorCall) {
            ImOperatorCall opCall = (ImOperatorCall) elem;
            if (opCall.getOp().isLazy()) {
                // for lazy operators (and, or) we only search the left expression for possible replacements
                return getPossibleReplacement(opCall.getArguments().get(0), kn);
            }
        }
        // process children
        for (int i = 0; i < elem.size(); i++) {
            Replacement r = getPossibleReplacement(elem.get(i), kn);
            if (r != null) {
                return r;
            }
        }
        if (elem instanceof ImFunctionCall) {
            // function call invalidates globals
            kn.invalidateGlobals();
        } else if (elem instanceof ImVarRead) {
            ImVarRead va = (ImVarRead) elem;
            if (va.getVar().isGlobal()) {
                // in case we read a global variable
                kn.invalidateMutatingExpressions();
            }
        }
        return null;
    }

    private HashMap<Element, Boolean> containsFuncCall = new HashMap<>();

    private boolean containsFuncCall(Element elem) {
        if (!containsFuncCall.containsKey(elem)) {
            if (elem instanceof ImFunctionCall) {
                containsFuncCall.put(elem, true);
                return true;
            }
            // process children
            boolean r = false;
            for (int i = 0; i < elem.size(); i++) {
                r = containsFuncCall(elem.get(i));
                if (r) break;
            }
            containsFuncCall.put(elem, r);

        }
        return containsFuncCall.get(elem);
    }

    private HashMap<Element, Boolean> readsVar = new HashMap<>();

    private boolean readsVar(Element elem, ImVar left) {
        if (!readsVar.containsKey(elem)) {
            if (elem instanceof ImVarRead) {
                ImVarRead va = (ImVarRead) elem;
                if (va.getVar() == left) {
                    readsVar.put(elem, true);
                    return true;
                }
            }
            boolean r = false;
            // process children
            for (int i = 0; i < elem.size(); i++) {
                r = readsVar(elem.get(i), left);
                if (r) break;
            }
            readsVar.put(elem, r);
        }
        return readsVar.get(elem);
    }

    private HashMap<Element, Boolean> readsGlobal = new HashMap<>();

    private boolean readsGlobal(Element elem) {
        if (!readsGlobal.containsKey(elem)) {
            if (elem instanceof ImVarRead) {
                ImVarRead va = (ImVarRead) elem;
                if (va.getVar().isGlobal()) {
                    readsGlobal.put(elem, true);
                    return true;
                }
            }
            boolean r = false;
            // process children
            for (int i = 0; i < elem.size(); i++) {
                r = readsGlobal(elem.get(i));
                if (r) break;
            }
            readsGlobal.put(elem, r);
        }

        return readsGlobal.get(elem);
    }

    class Replacement {
        public final ImSet set;
        public final ImVarAccess read;

        public Replacement(ImSet set, ImVarAccess read) {
            this.set = set;
            this.read = read;
        }

        @Override
        public String toString() {
            return "replace " + read + ", using " + set;
        }

        public void apply() {
            ImExpr e = set.getRight();
            if (set.getLeft().attrReads().size() <= 1) {
                // make sure that an impure expression is only evaluated once
                // by removing the assignment
                set.replaceBy(JassIm.ImNull());

                // remove variables which are no longer read
                for (ImVarRead r : readVariables(set)) {
                    r.getVar().attrReads().remove(r);
                }
            }

            ImExpr newE = (ImExpr) e.copy();
            read.replaceBy(newE);
            // update attrReads:
            set.getLeft().attrReads().remove(read);

            // for all the variables in e: add to read
            for (ImVarRead r : readVariables(newE)) {
                r.getVar().attrReads().add(r);
            }

        }

    }

    private Collection<ImVarRead> readVariables(Element e) {
        Collection<ImVarRead> result = Lists.newArrayList();
        collectReadVariables(result, e);
        return result;
    }


    private void collectReadVariables(Collection<ImVarRead> result, Element e) {
        if (e instanceof ImVarRead) {
            result.add((ImVarRead) e);
        }
        for (int i = 0; i < e.size(); i++) {
            collectReadVariables(result, e.get(i));
        }
    }

    class Knowledge {

        private HashMap<ImVar, ImSet> currentValues = Maps.newLinkedHashMap();

        List<ImVar> invalid = Lists.newArrayList();

        public void invalidateGlobals() {
            // invalidate all knowledge which might be based on global state
            // i.e. using a global var or calling a function
            currentValues.entrySet().removeIf(e -> readsGlobal(e.getValue().getRight()) || containsFuncCall(e.getValue()));
        }

        public void invalidateMutatingExpressions() {
            // invalidate all knowledge which can change global state
            // i.e. calling a function
            currentValues.entrySet().removeIf(e -> containsFuncCall(e.getValue()));
        }

        public void clear() {
            currentValues.clear();
        }

        public @Nullable Replacement getReplacementIfPossible(ImVarAccess va) {
            for (Entry<ImVar, ImSet> e : currentValues.entrySet()) {
                if (e.getKey() == va.getVar()) {
                    return new Replacement(e.getValue(), va);
                }
            }
            return null;
        }

        public boolean isEmpty() {
            return currentValues.isEmpty();
        }

        public void update(ImVar left, ImSet set) {
            invalidateVar(left);

            if (isMergable(left, set.getRight())) {
                // only store local vars which are read exactly once
                currentValues.put(left, set);
            }
        }

        private boolean isMergable(ImVar left, ImExpr e) {
            if (left.isGlobal()) {
                // never merge globals
                return false;
            }
            if (e instanceof ImVarAccess) {
                ImVarAccess va = (ImVarAccess) e;
                if (va.getVar() == left) {
                    // this is a stupid assignment, ignore it
                    return false;
                }
            }
            if (left.attrReads().size() == 1) {
                // variable read exactly once can be replaced
                return true;
            }
            if (isSimplePureExpr(e)) {
                // simple and pure expressions can always be merged
                return true;
            }
            return false;
        }

        /**
         * invalidates all expression depending on 'left'
         */
        private void invalidateVar(ImVar left) {
            currentValues.remove(left);
            if (left.isGlobal()) {
                invalidateGlobals();
            } else {
                currentValues.entrySet().removeIf(e -> readsVar(e.getValue().getRight(), left));
            }
        }

        @Override
        public String toString() {
            ArrayList<ImVar> keys = Lists.newArrayList(currentValues.keySet());
            keys.sort(Utils.<ImVar>compareByNameIm());
            StringBuilder sb = new StringBuilder();
            for (ImVar v : keys) {
                ImSet s = currentValues.get(v);
                sb.append(v.getName()).append(" -> ").append(s).append(", ");
            }
            return sb.toString();
        }

    }

    private boolean isSimplePureExpr(ImExpr e) {
        if (e instanceof ImConst) {
            // constants are ok
            return true;
        } else if (e instanceof ImVarAccess) {
            // local variables are ok
            ImVarAccess va = (ImVarAccess) e;
            return !va.getVar().isGlobal();
        }
        return false;
    }

}
