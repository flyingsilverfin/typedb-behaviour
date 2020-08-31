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

import com.google.common.collect.Sets;
import graql.lang.Graql;
import graql.lang.pattern.Conjunction;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.variable.ThingVariable;
import graql.lang.pattern.variable.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Introduces the id fuzzying operator - it fuzzes each id in the input pattern such that
 * the input and output patterns are structurally-equivalent (equivalent up to the choice of ids).
 *
 * For an input pattern the application of the operator returns an exhaustive stream of patterns each with at least a single id randomised.
 *
 */
public class IdFuzzyingOperator implements Operator{

    @Override
    public Stream<? extends Conjunction<?>> apply(Conjunction<?> src, TypeContext ctx) {
        if (!src.variables().map(s -> s.asThing().iid()).findFirst().isPresent()){
            return Stream.of(src);
        }

        List<Set<ThingVariable<?>>> transformedStatements = src.variables()
                .filter(Variable::isThing)
                .map(Variable::asThing)
                .map(p -> transformStatement(p, ctx))
                .collect(Collectors.toList());
        return Sets.cartesianProduct(transformedStatements).stream()
                .map(Graql::and)
                .filter(p -> !p.equals(src));
    }

    private Set<ThingVariable<?>> transformStatement(ThingVariable<?> src, TypeContext ctx){
        Optional<ThingProperty.IID> iid = src.iid();
        Set<ThingVariable<?>> transformedStatements = Sets.newHashSet(src);
        iid.map(idProp -> {
            List<ThingProperty> properties = new ArrayList<>(src.properties());
            properties.remove(idProp);
            properties.add(new ThingProperty.IID(ctx.instanceId()));
            return src.asUnbound().asThing().asSameThingWith(properties);
        }).ifPresent(transformedStatements::add);

        return transformedStatements;
    }
}
