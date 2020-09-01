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

import graql.lang.Graql;
import graql.lang.pattern.Conjunction;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.property.TypeProperty;
import graql.lang.pattern.variable.BoundVariable;
import graql.lang.pattern.variable.ThingVariable;
import graql.lang.pattern.variable.TypeVariable;
import graql.lang.pattern.variable.UnboundVariable;
import graql.lang.pattern.variable.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines a type generalisation operator. Type generalisation is performed by generalising types contained
 * in IsaProperty properties. As each statement contains at most one IsaProperty, we do not need to
 * compute a Cartesian product in order to obtain the transformed patterns.
 */
public class TypeGeneraliseOperator implements Operator {

    private static String TYPE_POSTFIX = "type";

    @Override
    public Stream<Conjunction<?>> apply(Conjunction<?> src, TypeContext ctx) {
        Set<BoundVariable<?>> originalStatements = src.variables()
                .filter(Variable::isNamed)
                .collect(Collectors.toSet());
        Set<Conjunction<?>> transformedPatterns = new HashSet<>();

        originalStatements.forEach(s -> {
            BoundVariable<?> transformed = transformStatement(s, ctx);
            Set<BoundVariable<?>> statements = new HashSet<>(originalStatements);
            statements.remove(s);
            if (transformed != null) statements.add(transformed);
            if (!statements.isEmpty()) transformedPatterns.add(Graql.and(new ArrayList<>(statements)));
        });
        return transformedPatterns.stream()
                .filter(p -> !p.equals(src));
    }

    private BoundVariable<?> transformStatement(Variable<?> src, TypeContext ctx){
        if (src.isThing()) return transformThingStatement(src.asThing(), ctx);
        else return transformTypeStatement(src.asType(), ctx);
    }

    //TODO this shouldn't be needed
    private BoundVariable<?> transformTypeStatement(TypeVariable src, TypeContext ctx){
        TypeProperty.Label type = src.label().orElse(null);
        if (type == null) return src;
        String typeLabel = type.label();
        UnboundVariable srcVar = src.asUnbound();

        if(ctx.isMetaType(typeLabel)){
            return src;
        } else {
            String superType = ctx.sup(typeLabel);
            return srcVar.type(superType);
        }
    }

    private BoundVariable<?> transformThingStatement(ThingVariable<?> src, TypeContext ctx){
        UnboundVariable var = src.asUnbound();
        ThingProperty.Isa isaProperty = Utils.getProperty(src, ThingProperty.Isa.class);
        if (isaProperty == null) return src;

        TypeProperty.Label type = isaProperty.type().label().orElse(null);
        //we only remove the statement if it's a bare ISA (not relation)
        if (type == null){
            return src.properties().size() == 1? null : src;
        }
        String typeLabel = type.label();

        List<ThingProperty> properties = new ArrayList<>(src.properties());
        properties.remove(isaProperty);

        ThingVariable<?> newStatement = var.asThing().asSameThingWith(properties);

        if(ctx.isMetaType(typeLabel)){
            ThingProperty.Isa newIsa = new ThingProperty.Isa(Graql.var(var.name() + TYPE_POSTFIX), false);
            newStatement = newStatement.asSameThingWith(newIsa);
        } else {
            String superType = ctx.sup(typeLabel);
            ThingProperty.Isa newIsa = new ThingProperty.Isa(superType, false);
            newStatement = newStatement.asSameThingWith(newIsa);;
        }
        return newStatement;
    }

}
