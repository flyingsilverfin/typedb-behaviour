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
import graql.lang.pattern.Conjunction;
import graql.lang.pattern.property.Property;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.variable.BoundVariable;
import graql.lang.pattern.variable.ThingVariable;
import graql.lang.pattern.variable.UnboundVariable;
import graql.lang.pattern.variable.Variable;
/*
import graql.lang.property.VarProperty;
import graql.lang.statement.Statement;
import graql.lang.statement.Variable;

 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Introduces the variable fuzzying operator - it fuzzes each variable in the input pattern such that
 * the input and output patterns are alpha-equivalent (we preserve variable bindings).
 *
 * For an input pattern the application of the pattern returns a a stream of patterns each with a single variable randomised.
 *
 */
public class VariableFuzzyingOperator implements Operator{

    @Override
    public Stream<Conjunction<?>> apply(Conjunction<?> src, TypeContext ctx) {
        //generate new variables and how they map to existing variables
        Map<Variable, Variable> varTransforms = new HashMap<>();
        src.variables()
                .forEach(v -> {
                    String newVarName = UUID.randomUUID().toString();
                    Variable newVar = Graql.var(newVarName);
                    varTransforms.put(v, newVar);
                });

        return varTransforms.entrySet().stream()
                .map(e -> src.variables()
                        .map(v -> transformStatement(v, ImmutableMap.of(e.getKey(), e.getValue())))
                        .collect(Collectors.toList()))
                .map(Graql::and);
    }

    private BoundVariable transformStatement(Variable<?> src, Map<Variable, Variable> vars){
        if (src.isType()) return src.asType();
        List<ThingProperty> transformedProperties = src.asThing().properties().stream()
                .map(p -> PropertyVariableTransform.transform(p, vars))
                .distinct()
                .collect(Collectors.toList());
        ThingVariable statementVar = vars.getOrDefault(src, src).asThing();
        //make sure to create a fresh variable instead of mutating the old one
        return statementVar.asUnbound().asThing().asSameThingWith(transformedProperties);
    }
}
