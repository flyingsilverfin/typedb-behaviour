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
import graql.lang.Graql;
import graql.lang.common.GraqlToken;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.property.TypeProperty;
import graql.lang.pattern.property.ValueOperation;
import graql.lang.pattern.variable.TypeVariable;
import graql.lang.pattern.variable.UnboundVariable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Introduces a variable transform for different types of VarProperty. The variables are transformed according to provided mappings.
 */
public class PropertyVariableTransform {

    private final static Map<Class, BiFunction<ThingProperty, Map<UnboundVariable, UnboundVariable>, ThingProperty>> transformMap = ImmutableMap.of(
            ThingProperty.Relation.class, PropertyVariableTransform::transformRelation,
            ThingProperty.Has.class, PropertyVariableTransform::transformAttribute,
            ThingProperty.Isa.class, PropertyVariableTransform::transformIsa,
            ThingProperty.Value.class, PropertyVariableTransform::transformValue,
            ThingProperty.NEQ.class, PropertyVariableTransform::transformNeq
    );

    static ThingProperty transform(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        BiFunction<ThingProperty, Map<UnboundVariable, UnboundVariable>, ThingProperty> func = transformMap.get(prop.getClass());
        if (func == null) return defaultTransform(prop, vars);
        return func.apply(prop, vars);
    }

    static private ThingProperty defaultTransform(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        return prop;
    }

    static private ThingProperty transformRelation(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        ThingProperty.Relation relProp = (ThingProperty.Relation) prop;
        ThingProperty.Relation relation = relProp.asRelation();
        Set<ThingProperty.Relation.RolePlayer> transformedRPs = relation.players().stream().map(rp -> {
            UnboundVariable playerVar = rp.player().asUnbound();
            TypeVariable role = rp.roleType().orElse(null);

            TypeProperty.Label typeLabel = null;
            UnboundVariable transformedRole = null;
            if (role != null) {
                UnboundVariable roleVar = role.asUnbound();
                typeLabel = role.label().orElse(null);
                transformedRole = vars.getOrDefault(roleVar, roleVar);
            }

            UnboundVariable transformedPlayer = Graql.var(vars.getOrDefault(playerVar, playerVar).name());
            return typeLabel != null?
                    new ThingProperty.Relation.RolePlayer(typeLabel.label(), transformedPlayer) :
                    new ThingProperty.Relation.RolePlayer(transformedRole, transformedPlayer);
        }).collect(Collectors.toSet());
        return Utils.relationProperty(transformedRPs);
    }

    static private ThingProperty transformAttribute(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        ThingProperty.Has attrProp = (ThingProperty.Has) prop;
        UnboundVariable attrVar = attrProp.attribute().asUnbound();
        if (!attrVar.isNamed()) return prop;

        TypeProperty.Label typeLabel = attrProp.type().label().orElse(null);
        UnboundVariable newAttrVar = vars.getOrDefault(attrVar, attrVar).asThing().asUnbound();

        return new ThingProperty.Has(typeLabel.label(), newAttrVar);
    }

    static private ThingProperty transformIsa(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        ThingProperty.Isa isaProp = (ThingProperty.Isa) prop;
        UnboundVariable typeVar = isaProp.type().asUnbound();
        if (!typeVar.isNamed()) return prop;

        UnboundVariable newStatement = vars.getOrDefault(typeVar, typeVar).asType().asUnbound();
        //TODO forcing it to not being explicit
        return new ThingProperty.Isa(newStatement, false);
    }

    static private ThingProperty transformValue(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        ThingProperty.Value<?> valProp = (ThingProperty.Value<?>) prop;
        UnboundVariable opVar = valProp.operation().variable().asUnbound();
        if(!valProp.operation().hasVariable()) return prop;

        UnboundVariable varStatement = vars.getOrDefault(opVar, opVar).asThing().asUnbound();
        ValueOperation.Comparison.Variable operation = new ValueOperation.Comparison.Variable(GraqlToken.Comparator.NEQV, varStatement);
        return new ThingProperty.Value<>(operation);
    }

    static private ThingProperty transformNeq(ThingProperty prop, Map<UnboundVariable, UnboundVariable> vars){
        ThingProperty.NEQ neqProp = (ThingProperty.NEQ) prop;
        UnboundVariable var = neqProp.variable().asUnbound();
        return new ThingProperty.NEQ(Graql.var(vars.getOrDefault(var, var).name()));
    }

}
