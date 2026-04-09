/*
 * Copyright 2025, Robert A. Petit III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bactopia.plugin.utils

import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import nextflow.util.RecordMap

/**
 * Utility class for channel manipulation operations in Bactopia pipelines.
 * Provides functions for gathering, filtering, and transforming channels.
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 */
@Slf4j
class ChannelUtils {

    /**
     * Core gather implementation. Extracts fields from a channel of records,
     * collects all values into sets, and wraps with a meta map into a record-like map.
     *
     * The fieldMapping maps input field names to output field names:
     *   [inputKey: outputKey] -- extract inputKey from records, emit as outputKey
     *
     * @param chResults    Channel or List of records
     * @param fieldMapping Map of input field name to output field name
     * @param meta         Output meta map (required). Must contain 'name'.
     * @return Channel or List containing a single record-like map with meta + collected fields
     * @throws IllegalArgumentException if inputs are invalid
     */
    private static Object _gather(Object chResults, Map<String, String> fieldMapping, Map meta) {
        if (chResults == null) {
            throw new IllegalArgumentException("chResults cannot be null")
        }
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            throw new IllegalArgumentException("fieldMapping cannot be null or empty")
        }
        if (meta == null) {
            throw new IllegalArgumentException("meta cannot be null")
        }
        if (!meta.name || meta.name.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("meta.name is required")
        }

        if (chResults instanceof DataflowReadChannel || chResults instanceof DataflowWriteChannel) {
            // Channel-based: collect all records, then build output
            return chResults
                .collect { r ->
                    def Map extracted = [:]
                    fieldMapping.each { String inputKey, String outputKey ->
                        extracted[outputKey] = r[inputKey]
                    }
                    extracted
                }
                .flatMap { List<Map> collected ->
                    def Map result = [meta: meta]
                    fieldMapping.values().each { String outputKey ->
                        def Set values = collected.collect { v -> v[outputKey] }.findAll { v -> v != null }.toSet()
                        result[outputKey] = values
                    }
                    def boolean hasData = fieldMapping.values().any { String key -> !result[key].isEmpty() }
                    hasData ? [new RecordMap(result)] : []
                }
        } else {
            // List-based
            def Map result = [meta: meta]
            fieldMapping.each { String inputKey, String outputKey ->
                def Set values = chResults.collect { r -> r[inputKey] }.findAll { v -> v != null }.toSet()
                result[outputKey] = values
            }
            def boolean hasData = fieldMapping.values().any { String key -> !result[key].isEmpty() }
            return hasData ? new RecordMap(result) : []
        }
    }

    /**
     * Gather a single field from records, keeping the original field name.
     * Returns a record-like map with meta and the collected field as a Set.
     *
     * @param chResults Channel or List of records
     * @param field     The record field name to extract (e.g., 'gff', 'tsv')
     * @param meta      Output meta map (required). Must contain 'name'.
     * @return Channel or List containing a single record-like map
     */
    static Object gather(Object chResults, String field, Map meta) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field cannot be null or empty")
        }
        return _gather(chResults, [(field): field], meta)
    }

    /**
     * Gather a single field from records, renaming it to 'csv' for CSVTK_CONCAT input.
     * Returns a record-like map with meta and the collected values under the 'csv' key.
     *
     * @param chResults Channel or List of records
     * @param field     The record field name to extract (e.g., 'tsv', 'report')
     * @param meta      Output meta map (required). Must contain 'name'.
     * @return Channel or List containing a single record-like map with csv field
     */
    static Object gatherCsvtk(Object chResults, String field, Map meta) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field cannot be null or empty")
        }
        return _gather(chResults, [(field): 'csv'], meta)
    }

    /**
     * Gather multiple fields from records with explicit rename mapping.
     * Returns a record-like map with meta and each collected field as a Set.
     *
     * @param chResults    Channel or List of records
     * @param fieldMapping Map of input field names to output field names
     * @param meta         Output meta map (required). Must contain 'name'.
     * @return Channel or List containing a single record-like map
     */
    static Object gatherFields(Object chResults, Map<String, String> fieldMapping, Map meta) {
        return _gather(chResults, fieldMapping, meta)
    }

    /**
     * Filter records where at least one of the specified fields is non-null.
     * Filters records where at least one of the specified fields is non-null, then projects
     * the record down to only {@code meta} plus the requested fields.
     * This prevents downstream processes from receiving extra record fields that cause type errors.
     *
     * @param input  Channel or List of records
     * @param fields List of field names to check for non-null values and include in output
     * @return Channel or List of projected records containing only meta + requested fields
     * @throws IllegalArgumentException if input is null or fields is null/empty
     */
    static Object filterWithData(Object input, List<String> fields) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null")
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields cannot be null or empty")
        }

        def filterAndProject = { r ->
            if (!fields.any { f ->
                def val = r[f]
                if (val == null) return false
                if (val instanceof Collection && val.isEmpty()) return false
                return true
            }) return null
            def projected = new LinkedHashMap()
            projected['meta'] = r['meta']
            for (f in fields) {
                projected[f] = r[f]
            }
            return new RecordMap(projected)
        }

        if (input instanceof DataflowReadChannel || input instanceof DataflowWriteChannel) {
            return input.map(filterAndProject).filter { v -> v != null }
        } else {
            return input.collect(filterAndProject).findAll { v -> v != null }
        }
    }

    /**
     * Collect Nextflow log files from a channel of records into [meta, file] tuples.
     * Expands each record's nf_logs field into individual tuples suitable for publishing.
     *
     * @param chResults Channel or List of records containing meta and nf_logs fields
     * @return Channel or List of [meta, file] tuples
     * @throws IllegalArgumentException if chResults is null
     */
    static Object collectNextflowLogs(Object chResults) {
        if (chResults == null) {
            throw new IllegalArgumentException("chResults cannot be null")
        }

        if (chResults instanceof DataflowReadChannel || chResults instanceof DataflowWriteChannel) {
            return chResults.flatMap { r ->
                r.nf_logs.collect { f -> [r.meta, f] }
            }
        } else {
            def result = []
            chResults.each { r ->
                r.nf_logs.each { f ->
                    result << [r.meta, f]
                }
            }
            return result
        }
    }

    /**
     * Create a cartesian product by combining a gathered channel with a multi-item
     * channel, merging each item into the gathered map under the specified field name.
     * Replaces the deprecated Nextflow {@code each} input qualifier.
     *
     * <p>Example: if gathered emits {@code [meta:[name:fastani], query:[a.fna, b.fna]]}
     * and items emits {@code ref1.fna}, {@code ref2.fna}, the result emits:
     * <ul>
     *   <li>{@code [meta:[name:fastani], query:[a.fna, b.fna], reference:ref1.fna]}</li>
     *   <li>{@code [meta:[name:fastani], query:[a.fna, b.fna], reference:ref2.fna]}</li>
     * </ul>
     *
     * @param gathered Channel or List (single-item, e.g. from gatherFields)
     * @param items    Channel or List (multi-item, e.g. individual references)
     * @param field    Field name to assign each item in the output map
     * @return Channel or List of maps with the item merged in under field name
     * @throws IllegalArgumentException if any argument is null or field is empty
     */
    static Object combineWith(Object gathered, Object items, String field) {
        if (gathered == null) {
            throw new IllegalArgumentException("gathered cannot be null")
        }
        if (items == null) {
            throw new IllegalArgumentException("items cannot be null")
        }
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field cannot be null or empty")
        }

        if (gathered instanceof DataflowReadChannel || gathered instanceof DataflowWriteChannel) {
            return gathered.combine(items).map { List tuple ->
                def Map result = new LinkedHashMap(tuple[0] as Map)
                result[field] = tuple[1]
                return new RecordMap(result)
            }
        } else {
            // List-based: standard cartesian product with merge
            def List gatheredList = gathered instanceof List ? gathered : [gathered]
            def List itemsList = items instanceof List ? items : [items]
            def List results = []
            gatheredList.each { Map g ->
                itemsList.each { item ->
                    def Map merged = new LinkedHashMap(g)
                    merged[field] = item
                    results << new RecordMap(merged)
                }
            }
            return results
        }
    }
}
