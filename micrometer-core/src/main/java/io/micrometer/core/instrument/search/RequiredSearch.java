/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.search;

import io.micrometer.core.instrument.*;
import io.micrometer.core.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Search that requires the search terms are satisfiable, or an {@link MeterNotFoundException} is thrown.
 */
public final class RequiredSearch {
    private final MeterRegistry registry;
    private final List<Tag> tags = new ArrayList<>();
    private Predicate<String> nameMatches = n -> true;

    @Nullable
    private String exactNameMatch;

    private RequiredSearch(MeterRegistry registry) {
        this.registry = registry;
    }

    public RequiredSearch name(String exactName) {
        this.nameMatches = n -> n.equals(exactName);
        this.exactNameMatch = exactName;
        return this;
    }

    public RequiredSearch name(Predicate<String> nameMatches) {
        this.nameMatches = nameMatches;
        return this;
    }

    public RequiredSearch tags(Iterable<Tag> tags) {
        tags.forEach(this.tags::add);
        return this;
    }

    /**
     * @param tags Must be an even number of arguments representing key/value pairs of tags.
     */
    public RequiredSearch tags(String... tags) {
        return tags(Tags.of(tags));
    }

    public RequiredSearch tag(String tagKey, String tagValue) {
        return tags(Tags.of(tagKey, tagValue));
    }

    public Timer timer() {
        return findOne(Timer.class);
    }

    public Counter counter() {
        return findOne(Counter.class);
    }

    public Gauge gauge() {
        return findOne(Gauge.class);
    }

    public FunctionCounter functionCounter() {
        return findOne(FunctionCounter.class);
    }

    public TimeGauge timeGauge() {
        return findOne(TimeGauge.class);
    }

    public FunctionTimer functionTimer() {
        return findOne(FunctionTimer.class);
    }

    public DistributionSummary summary() {
        return findOne(DistributionSummary.class);
    }

    public LongTaskTimer longTaskTimer() {
        return findOne(LongTaskTimer.class);
    }

    public Meter meter() {
        return findOne(Meter.class);
    }

    private <M extends Meter> M findOne(Class<M> clazz) {
        Optional<M> meter = meters()
            .stream()
            .filter(clazz::isInstance)
            .findAny()
            .map(clazz::cast);

        if (meter.isPresent()) {
            return meter.get();
        }

        throw new MeterNotFoundException(exactNameMatch, tags, clazz);
    }

    public Collection<Meter> meters() {
        Stream<Meter> meterStream = registry.getMeters().stream().filter(m -> nameMatches.test(m.getId().getName()));

        if (!tags.isEmpty()) {
            meterStream = meterStream.filter(m -> m.getId().getTags().containsAll(tags));
        }

        return meterStream.collect(Collectors.toList());
    }

    public static RequiredSearch search(MeterRegistry registry) {
        return new RequiredSearch(registry);
    }
}
