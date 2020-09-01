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
import graql.lang.pattern.Pattern;
import graql.lang.pattern.property.Property;
import graql.lang.pattern.property.ThingProperty;
import graql.lang.pattern.variable.ThingVariable;
import graql.lang.pattern.variable.TypeVariable;
import graql.lang.pattern.variable.UnboundVariable;
import graql.lang.pattern.variable.Variable;
/*
import graql.lang.property.RelationProperty;
import graql.lang.statement.Statement;

import graql.lang.statement.Variable;

 */
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {

    /**
     * Sanitise input pattern:
     * - remove statements without properties
     * - remove statements that are disconnected from the original pattern
     * @param p transformed pattern
     * @param src original Pattern
     * @return
     */
    /*
    static Conjunction<?> sanitise(Conjunction<?> p, Conjunction<?> src){
        Set<ThingVariable<?>> toRemove = Sets.difference(rolePlayerVariables(src), rolePlayerVariables(p));
        return Graql.and(
                p.variables()
                        .filter(s -> !toRemove.contains(s))
                        .collect(Collectors.toList())
        );
    }

    static Set<Variable> rolePlayerVariables(Pattern p){
        return p.statements().stream()
                .flatMap(s -> s.properties().stream())
                .filter(RelationProperty.class::isInstance)
                .map(RelationProperty.class::cast)
                .flatMap(rp -> rp.relationPlayers().stream())
                .map(rp -> rp.getPlayer().var())
                .collect(Collectors.toSet());
    }
*/
    static ThingProperty.Relation relationProperty(Collection<ThingProperty.Relation.RolePlayer> relationPlayers) {
        if (relationPlayers.isEmpty()) return null;
        ThingVariable.Relation var = null;
        List<ThingProperty.Relation.RolePlayer> sortedRPs = relationPlayers.stream()
                .sorted(Comparator.comparing(rp -> rp.player().name()))
                .collect(Collectors.toList());
        for (ThingProperty.Relation.RolePlayer rp : sortedRPs) {
            TypeVariable rolePattern = rp.roleType().orElse(null);
            if (var == null) {
                var = rolePattern != null ? Graql.var().rel(rolePattern.asUnbound(), rp.player().asUnbound()) : Graql.var().rel(rp.player().asUnbound());
            } else {
                var = rolePattern != null ? var.rel(rolePattern.asUnbound(), rp.player().asUnbound()) :  var.rel(rp.player().asUnbound());
            }
        }
        return var.relation().orElse(null);
    }

    static <T extends Property> T getProperty(Variable<?> src, Class<T> type){
        return src.properties().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst().orElse(null);
    }
}
