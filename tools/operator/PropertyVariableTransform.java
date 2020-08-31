/*
* Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package grakn.verification.tools.operator;

import com.google.common.collect.ImmutableMap;
import grakn.common.collection.Either;
import graql.lang.Graql;
import graql.lang.common.GraqlToken;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.property.TypeProperty;
import graql.lang.pattern.property.ValueOperation;
import graql.lang.pattern.variable.ThingVariable;
import graql.lang.pattern.variable.TypeVariable;
import graql.lang.pattern.variable.UnboundVariable;
import graql.lang.pattern.variable.Variable;
/*
import graql.lang.property.HasAttributeProperty;
import graql.lang.property.IsaProperty;
import graql.lang.property.NeqProperty;
import graql.lang.property.RelationProperty;
import graql.lang.property.ValueProperty;
import graql.lang.property.VarProperty;
import graql.lang.statement.Statement;
import graql.lang.statement.Variable;

 */

import java.beans.Statement;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Introduces a variable transform for different types of VarProperty. The variables are transformed according to provided mappings.
 */
public class PropertyVariableTransform {

    private final static Map<Class, BiFunction<ThingProperty, Map<Variable, Variable>, ThingProperty>> transformMap = ImmutableMap.of(
            ThingProperty.Relation.class, PropertyVariableTransform::transformRelation,
            ThingProperty.Has.class, PropertyVariableTransform::transformAttribute,
            ThingProperty.Isa.class, PropertyVariableTransform::transformIsa,
            ThingProperty.Value.class, PropertyVariableTransform::transformValue,
            ThingProperty.NEQ.class, PropertyVariableTransform::transformNeq
    );

    static ThingProperty transform(ThingProperty prop, Map<Variable, Variable> vars){
        BiFunction<ThingProperty, Map<Variable, Variable>, ThingProperty> func = transformMap.get(prop.getClass());
        if (func == null) return defaultTransform(prop, vars);
        return func.apply(prop, vars);
    }

    static private ThingProperty defaultTransform(ThingProperty prop, Map<Variable, Variable> vars){
        return prop;
    }

    static private ThingProperty transformRelation(ThingProperty prop, Map<Variable, Variable> vars){
        ThingProperty.Relation relProp = (ThingProperty.Relation) prop;
        ThingProperty.Relation relation = relProp.asRelation();
        Set<ThingProperty.Relation.RolePlayer> transformedRPs = relation.players().stream().map(rp -> {
            ThingVariable playerVar = rp.player();
            TypeVariable role = rp.roleType().orElse(null);
            TypeProperty.Label typeLabel = null;
            TypeVariable transformedRole = role;
            if (role != null) {
                typeLabel = role.label().orElse(null);
                TypeVariable newRoleVar = vars.getOrDefault(role, role).asType();
                if (newRoleVar != null) {
                    transformedRole = newRoleVar;
                    if (typeLabel != null) transformedRole = transformedRole.type(typeLabel.label());
                }
            }

            UnboundVariable transformedPlayer = Graql.var(vars.getOrDefault(playerVar, playerVar).name());
            return typeLabel != null?
                    new ThingProperty.Relation.RolePlayer(typeLabel.label(), transformedPlayer) :
                    new ThingProperty.Relation.RolePlayer(transformedRole.asUnbound(), transformedPlayer);
        }).collect(Collectors.toSet());
        return Utils.relationProperty(transformedRPs);
    }

    static private ThingProperty transformAttribute(ThingProperty prop, Map<Variable, Variable> vars){
        ThingProperty.Has attrProp = (ThingProperty.Has) prop;
        ThingVariable<?> attrVar = attrProp.attribute();
        if (!attrVar.isNamed()) return prop;

        TypeProperty.Label typeLabel = attrProp.type().label().orElse(null);
        UnboundVariable newAttrVar = vars.getOrDefault(attrVar, attrVar).asThing().asUnbound();

        return new ThingProperty.Has(typeLabel.label(), newAttrVar);
    }

    static private ThingProperty transformIsa(ThingProperty prop, Map<Variable, Variable> vars){
        ThingProperty.Isa isaProp = (ThingProperty.Isa) prop;
        TypeVariable typeVar = isaProp.type();
        if (!typeVar.isNamed()) return prop;

        UnboundVariable newStatement = vars.getOrDefault(typeVar, typeVar).asType().asUnbound();
        //TODO forcing it to not being explicit
        return new ThingProperty.Isa(newStatement, false);
    }

    static private ThingProperty transformValue(ThingProperty prop, Map<Variable, Variable> vars){
        ThingProperty.Value<?> valProp = (ThingProperty.Value<?>) prop;
        ThingVariable<?> opVar = valProp.operation().variable();
        if(!valProp.operation().hasVariable()) return prop;

        UnboundVariable varStatement = vars.getOrDefault(opVar, opVar).asThing().asUnbound();
        ValueOperation.Comparison.Variable operation = new ValueOperation.Comparison.Variable(GraqlToken.Comparator.NEQV, varStatement);
        return new ThingProperty.Value<>(operation);
    }

    static private ThingProperty transformNeq(ThingProperty prop, Map<Variable, Variable> vars){
        ThingProperty.NEQ neqProp = (ThingProperty.NEQ) prop;
        Variable var = neqProp.variable();
        return new ThingProperty.NEQ(Graql.var(vars.getOrDefault(var, var).name()));
    }

}
