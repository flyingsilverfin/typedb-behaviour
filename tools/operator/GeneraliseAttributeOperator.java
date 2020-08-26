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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: this assumes there is no stray value properties (not attached to HasAttributeProperty)
//TODO: we currently only convert Number attributes
public class GeneraliseAttributeOperator implements Operator{

    @Override
    public Stream<? extends Conjunction<? extends Pattern>> apply(Conjunction<?> src, TypeContext ctx) {
        List<Set<? extends BoundVariable<?>>> transformedStatements = src.variables()
                .map(var -> transformStatement(var))
                .collect(Collectors.toList());
        return Sets.cartesianProduct(transformedStatements).stream()
                .map(stmtList -> Graql.and(stmtList))
                .filter(p -> !p.equals(src));
    }

    private <T extends BoundVariable<T>> Set<? extends BoundVariable<T>> transformStatement(BoundVariable<T> src){
        if (!src.isThing()) {
            return Sets.newHashSet(src);
        }

        ThingVariable<? extends ThingVariable<?>> thingVariable = src.asThing();

        List<ThingProperty.Has> attributes = thingVariable.has();
        if (attributes.isEmpty()) return thingVariable;

        Set<ThingProperty.Has> transformedProps = attributes.stream()
                .map(this::transformAttributeProperty)
                .collect(Collectors.toSet());

        Set<BoundVariable<T>> transformedStatements = Sets.newHashSet(src);
        transformedProps.stream()
                .map(o -> {
                    ArrayList<ThingProperty> properties = new ArrayList<>(src.asThing().properties());
                    properties.removeAll(attributes);
                    properties.addAll(transformedProps);
                    T t = src.withoutProperties();
                    return t.asThing().asSameThingWith(properties);
                })
                .forEach(transformedStatements::add);

        return transformedStatements;
    }

    private ThingProperty.Has transformAttributeProperty(ThingProperty.Has src){
        LinkedHashSet<ThingProperty> properties = src.variable().properties().stream()
                .filter(p -> !(p instanceof ThingProperty.Value))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Optional<Range> range = src.variable().valueProperty().map(vp -> Ranges.create(vp));
        if (!range.isPresent()) return src;

        properties.addAll(range.get().generalise().toProperties());

        String type = src.type();
        return new ThingProperty.Has(type, Statement.create(attribute.var(), properties));
    }

}
