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
import grakn.verification.tools.operator.range.Range;
import grakn.verification.tools.operator.range.Ranges;
import graql.lang.Graql;
import graql.lang.pattern.Conjunction;
import graql.lang.pattern.Pattern;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.variable.BoundVariable;
import graql.lang.pattern.variable.ThingVariable;
import graql.lang.pattern.variable.UnboundVariable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: this assumes there is no stray value properties (not attached to HasAttributeProperty)
//TODO: we currently only convert Number attributes


// TODO we may need to look at using `asGraph()` to solve these
// TODO asGraph will combine multiple mentions of one variable into a single Variable
public class GeneraliseAttributeOperator implements Operator{

    @Override
    public Stream<? extends Conjunction<? extends Pattern>> apply(Conjunction<?> src, TypeContext ctx) {
        List<Set<ThingVariable<?>>> transformedStatements = src.variables()
                .filter(var -> var.isThing())
                .map(var -> var.asThing())
                .map(var -> transformStatement(var))
                .collect(Collectors.toList());
        return Sets.cartesianProduct(transformedStatements).stream()
                .map(stmtList -> Graql.and(stmtList))
                .filter(p -> !p.equals(src));
    }

    private Set<ThingVariable<?>> transformStatement(ThingVariable<?> src){
        List<ThingProperty.Has> attributes = src.has();
        if (attributes.isEmpty()) return Sets.newHashSet(src);

        // we may have create a separate Variable per constraint, along with attribtue ownerships
        // eg. $x has age 10;
        // or. $x has age $y; $y < 10; $y > 5;
        Set<ThingVariable.Attribute> transformedProps = null;
                /*
                attributes.stream()
                .flatMap(this::transformAttributeProperty)
                .collect(Collectors.toSet());

                 */

        Set<ThingVariable<?>> transformedStatements = Sets.newHashSet(src);
        transformedProps.stream()
                .map(o -> {
                    List<ThingProperty> properties = new ArrayList<>(src.asThing().properties());
                    properties.removeAll(attributes);
                    //properties.addAll(transformedProps);
                    // TODO: which is preferred style
                    src.asUnbound().asThing().asSameThingWith(properties);
                    return UnboundVariable.of(src.reference()).asThing().asSameThingWith(properties);
                })
                .forEach(transformedStatements::add);

        return transformedStatements;
    }

    private ThingProperty.Has transformAttributeProperty(ThingProperty.Has src){
        LinkedHashSet<ThingProperty> properties = src.attribute().properties().stream()
                .filter(p -> !(p instanceof ThingProperty.Value))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Optional<Range> range = src.attribute().value().map(vp -> Ranges.create(vp));
        if (!range.isPresent()) return src;

        //TODO
        // ThingProperty.Value newValue = range.get().generalise().toProperties().iterator().next();

        assert properties.size() == 1;

        String type = src.type().label().get().toString();
        //return new ThingProperty.Has(type, properties);
        return null;
    }

}
