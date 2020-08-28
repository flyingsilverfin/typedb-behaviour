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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static grakn.verification.tools.operator.Utils.sanitise;
import static java.util.stream.Collectors.toSet;

/**
 * Generates a set of generalised patterns by removing existing roleplayers.
 * The set is computed from a Cartesian product of sets of statements each containing a single roleplayer removal
 * - the set is computed in analogous fashion to RemoveSubstitutionOperator for substitution removal.
 */
public class RemoveRoleplayerOperator implements Operator {

    @Override
    public Stream<Conjunction<?>> apply(Conjunction<?> src, TypeContext ctx) {
        // if there are no players present, return original
        if (!src.variables()
                .filter(Variable::isThing)
                .map(s -> s.asThing().relation().map(rel -> rel.players()).isPresent())
                .findAny().isPresent()) {
            return Stream.of(src);
        }

        List<Set<ThingVariable<?>>> transformedStatements = src.variables()
                .filter(var -> var.isThing())
                .filter(var -> var.asThing().relation().isPresent())
                .map(var -> transformStatement(var.asThing()))
                .collect(Collectors.toList());
        return Sets.cartesianProduct(transformedStatements).stream()
                .map(Graql::and)
                .filter(p -> !p.equals(src))
                .map(p -> sanitise(p, src));
    }

    private Set<ThingVariable<?>> transformStatement(ThingVariable<?> src) {
        ThingProperty.Relation relProperty = src.relation().get();
        Set<Optional<ThingProperty.Relation>> transformedProps = transformRelationProperty(relProperty);

        Set<ThingVariable<?>> transformedStatements = Sets.newHashSet(src);
        transformedProps.stream()
                .map(o -> {
                    List<ThingProperty> properties = new ArrayList<>(src.properties());
                    properties.remove(relProperty);
                    o.ifPresent(properties::add);
                    return src.asUnbound().asThing().asSameThingWith(properties);
                })
                .forEach(transformedStatements::add);

        return transformedStatements;
    }

    private Set<Optional<ThingProperty.Relation>> transformRelationProperty(ThingProperty.Relation prop) {
        List<Set<Optional<ThingProperty.Relation.RolePlayer>>> rPconfigurations = new ArrayList<>();

        prop.players().forEach(rp -> {
            Set<Optional<ThingProperty.Relation.RolePlayer>> rps = Sets.newHashSet(Optional.of(rp));
            rps.add(Optional.empty());
            rPconfigurations.add(rps);
        });

        return Sets.cartesianProduct(rPconfigurations).stream()
                .map(rpSet -> rpSet.stream().map(o -> o.orElse(null)).filter(Objects::nonNull).collect(toSet()))
                .map(rpSet -> Optional.ofNullable(Utils.relationProperty(rpSet)))
                .collect(toSet());
    }
}
