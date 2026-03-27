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

/**
 * Utility class for channel manipulation operations in Bactopia pipelines.
 * Provides functions for gathering, mixing, and transforming channels.
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 */
@Slf4j
class ChannelUtils {

    /**
     * Gather results from a channel of records by extracting a named field,
     * collecting all values into a set, and wrapping with a meta map.
     * Follows nf-core pattern: detects if input is a channel or list, applies built-in operators.
     *
     * @param chResults Channel or List of records
     * @param field     The record field name to extract (e.g., 'tsv', 'report')
     * @param meta      Output meta map (required). Must contain 'name'. All keys pass through
     *                  as-is to output. Downstream modules null-guard their own keys.
     * @return Channel or List containing a single tuple [meta, outputSet], or empty [] if no outputs
     * @throws IllegalArgumentException if chResults is null, field is null/empty, meta is null, or meta.name is missing
     */
    static Object gather(Object chResults, String field, Map meta) {
        // Input validation
        if (chResults == null) {
            throw new IllegalArgumentException("chResults cannot be null")
        }
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field cannot be null or empty")
        }
        if (meta == null) {
            throw new IllegalArgumentException("meta cannot be null")
        }
        if (!meta.name || meta.name.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("meta.name is required")
        }

        // Detect if input is a channel
        if (chResults instanceof DataflowReadChannel || chResults instanceof DataflowWriteChannel) {
            return chResults
                .collect { r -> r[field] }
                .map { output -> [meta, output.findAll { it != null }.toSet()] }
                .filter { _meta, outputs -> !outputs.isEmpty() }
        } else {
            def outputs = chResults.collect { r -> r[field] }.findAll { it != null }.toSet()
            return outputs.isEmpty() ? [] : [meta, outputs]
        }
    }

    /**
     * Mix multiple channels and flatten file sets.
     * Transforms Tuple<Map, Set<Path>> to Tuple<Map, Path>.
     * Follows nf-core pattern: detects if inputs are channels or lists, applies built-in operators.
     *
     * @param channels List of channels or lists, each containing tuples of [meta, files]
     * @return Channel or List with flattened tuples [meta, file] for each file
     * @throws IllegalArgumentException if channels is null or contains null elements
     */
    static Object flattenPaths(List channels) {
        // Input validation
        if (channels == null) {
            throw new IllegalArgumentException("channels cannot be null")
        }
        if (channels.isEmpty()) {
            return []
        }
        if (channels.any { it == null }) {
            throw new IllegalArgumentException("channels list cannot contain null elements")
        }

        // Check if we're dealing with channels or lists
        def firstItem = channels[0]
        def isChannel = firstItem instanceof DataflowReadChannel || firstItem instanceof DataflowWriteChannel

        if (isChannel) {
            // Handle single channel case
            if (channels.size() == 1) {
                return channels[0].flatMap { row ->
                    def meta, files
                    if (row instanceof List && row.size() >= 2) {
                        meta = row[0]
                        files = row[1]
                    } else {
                        return [row]  // Return as-is if structure is unexpected
                    }

                    // Check if files is a collection or single path
                    if (files instanceof Collection) {
                        // Collection of files (Set, List, etc.)
                        return files.collect { file -> [meta, file] }
                    } else {
                        // Single file path (String, Path, etc.)
                        return [[meta, files]]
                    }
                }
            }

            // Mix multiple channels and flatten
            def mixed = channels[0]
            channels[1..-1].each { ch -> mixed = mixed.mix(ch) }
            return mixed.flatMap { row ->
                def meta, files
                if (row.size() == 1 && row[0] instanceof List && row[0].size() == 2) {
                    // Handle mixed channels where row is wrapped: [[meta, files]]
                    meta = row[0][0]
                    files = row[0][1]
                } else if (row.size() == 2) {
                    // Handle normal case: [meta, files]
                    meta = row[0]
                    files = row[1]
                } else {
                    return [row]  // Return as-is if structure is unexpected
                }

                // Check if files is a collection or single path
                if (files instanceof Collection) {
                    // Collection of files (Set, List, etc.)
                    return files.collect { file -> [meta, file] }
                } else {
                    // Single file path (String, Path, etc.)
                    return [[meta, files]]
                }
            }
        } else {
            // Process lists directly
            def result = []
            channels.each { list ->
                list.each { row ->
                    def meta = row[0]
                    def files = row[1]

                    // Check if files is a collection or single path
                    if (files instanceof Collection) {
                        // Collection of files (Set, List, etc.)
                        files.each { file ->
                            result.add([meta, file])
                        }
                    } else {
                        // Single file path (String, Path, etc.)
                        result.add([meta, files])
                    }
                }
            }
            return result
        }
    }

    /**
     * Filter records where at least one of the specified fields is non-null.
     * Filters records where at least one of the specified fields is non-null, then projects
     * the record down to only {@code _meta} (mapped from {@code meta}) plus the requested fields.
     * This prevents downstream processes from receiving extra record fields that cause type errors.
     *
     * @param input  Channel or List of records
     * @param fields List of field names to check for non-null values and include in output
     * @return Channel or List of projected records containing only _meta + requested fields
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
            if (!fields.any { f -> r[f] != null }) return null
            def projected = new LinkedHashMap()
            projected['_meta'] = r['meta']
            for (f in fields) {
                projected[f] = r[f]
            }
            return projected
        }

        if (input instanceof DataflowReadChannel || input instanceof DataflowWriteChannel) {
            return input.map(filterAndProject).filter { it != null }
        } else {
            return input.collect(filterAndProject).findAll { it != null }
        }
    }
}
