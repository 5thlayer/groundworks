// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The shortest path of joined members from a start to an end, both included, start first, for a
 * {@link DismantleFamily} whose blocks are joined to their neighbours (ADR 0002). Two equally short
 * paths refuse the end rather than pick one.
 */
public final class ShortestPath {

    /** The members a family's span may cross, which of them touch, and which touching two are joined. */
    public interface Graph<N> {

        boolean member(N node);

        /** The nodes touching {@code node}, members or not. */
        Iterable<N> neighbours(N node);

        /** Whether two touching members are joined; asked only of members. By default they are. */
        default boolean joined(N a, N b) {
            return true;
        }
    }

    /** Why there is no path. The family tells the player, as with any of its own refusals. */
    public enum Refused implements Refusal {
        OUTSIDE_FAMILY,
        NOT_JOINED,
        TIED
    }

    public record Result<N>(List<N> path, @Nullable Refused refusal) {

        public Result {
            path = List.copyOf(path);
        }

        static <N> Result<N> refused(Refused refusal) {
            return new Result<>(List.of(), refusal);
        }
    }

    private ShortestPath() {
    }

    public static <N> Result<N> between(N start, N end, Graph<N> graph) {
        if (!graph.member(start) || !graph.member(end)) {
            return Result.refused(Refused.OUTSIDE_FAMILY);
        }
        Map<N, Integer> distance = new HashMap<>();
        // How many shortest paths reach a node, counted no higher than two.
        Map<N, Integer> routes = new HashMap<>();
        Map<N, N> previous = new HashMap<>();
        Deque<N> queue = new ArrayDeque<>();
        distance.put(start, 0);
        routes.put(start, 1);
        queue.add(start);
        while (!queue.isEmpty()) {
            N at = queue.poll();
            int here = distance.get(at);
            Integer reached = distance.get(end);
            if (reached != null && here >= reached) {
                break;
            }
            for (N next : graph.neighbours(at)) {
                if (!graph.member(next) || !graph.joined(at, next)) {
                    continue;
                }
                Integer there = distance.get(next);
                if (there == null) {
                    distance.put(next, here + 1);
                    routes.put(next, routes.get(at));
                    previous.put(next, at);
                    queue.add(next);
                } else if (there == here + 1) {
                    routes.put(next, Math.min(2, routes.get(next) + routes.get(at)));
                }
            }
        }
        if (!distance.containsKey(end)) {
            return Result.refused(Refused.NOT_JOINED);
        }
        if (routes.get(end) > 1) {
            return Result.refused(Refused.TIED);
        }
        List<N> path = new ArrayList<>();
        for (N at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return new Result<>(path, null);
    }
}
