package de.peeeq.wurstscript.attributes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableList;
import de.peeeq.wurstscript.ast.ClassDef;
import de.peeeq.wurstscript.ast.InterfaceDef;
import de.peeeq.wurstscript.ast.TypeExpr;
import de.peeeq.wurstscript.ast.TypeParamDef;
import de.peeeq.wurstscript.types.WurstTypeBoundTypeParam;
import de.peeeq.wurstscript.types.WurstTypeClass;
import de.peeeq.wurstscript.types.WurstTypeInterface;
import de.peeeq.wurstscript.utils.Utils;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Map;

public class InterfaceInstances {


    public static ImmutableCollection<WurstTypeInterface> getImplementedInterfaces(ClassDef c) {
        ImmutableCollection.Builder<WurstTypeInterface> result = ImmutableList.builder();
        for (TypeExpr t : c.getImplementsList()) {
            addInterface(result, t, null);
        }

        if (c.getExtendedClass() instanceof TypeExpr) {
            addInterfacesFromExtends(result, ((TypeExpr) c.getExtendedClass()));
        }

        return result.build();
    }

    public static ImmutableCollection<WurstTypeInterface> getExtendedInterfaces(InterfaceDef in) {
        ImmutableCollection.Builder<WurstTypeInterface> result = ImmutableList.builder();
        for (TypeExpr t : in.getExtendsList()) {
            addInterface(result, t, in);
        }
        return result.build();
    }

    private static void addInterface(Builder<WurstTypeInterface> result, TypeExpr t, @Nullable InterfaceDef in) {
        if (t.attrTyp() instanceof WurstTypeInterface) {
            WurstTypeInterface i = (WurstTypeInterface) t.attrTyp();
            if (i.getDef() == in) {
                t.addError("Interfaces must not extend themselves.");
                return;
            }
            result.add(i);
            Map<TypeParamDef, WurstTypeBoundTypeParam> typeParamBounds = i.getTypeArgBinding();
            for (WurstTypeInterface i2 : i.getInterfaceDef().attrExtendedInterfaces()) {
                result.add((WurstTypeInterface) i2.setTypeArgs(typeParamBounds));
            }

        } else {
            t.addError(Utils.printElement(t) + " is not an interface.");
        }
    }

    private static void addInterfacesFromExtends(Builder<WurstTypeInterface> result, TypeExpr t) {
        if (t.attrTyp() instanceof WurstTypeClass) {
            WurstTypeClass wtc = (WurstTypeClass) t.attrTyp();
            ClassDef c = wtc.getClassDef();
            Map<TypeParamDef, WurstTypeBoundTypeParam> typeParamBounds = wtc.getTypeArgBinding();
            for (WurstTypeInterface i2 : c.attrImplementedInterfaces()) {
                result.add((WurstTypeInterface) i2.setTypeArgs(typeParamBounds));
            }
        }
    }



}
