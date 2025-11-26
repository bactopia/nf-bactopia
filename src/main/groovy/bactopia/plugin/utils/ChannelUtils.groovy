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

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel

/**
 * Utility class for channel manipulation operations in Bactopia pipelines.
 * Provides functions for gathering, mixing, and transforming channels.
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 */
class ChannelUtils {

    /**
     * Gather results from a channel by collecting outputs and mapping to a single tuple.
     * Follows nf-core pattern: detects if input is a channel or list, applies built-in operators.
     *
     * @param chResults Channel or List of tuples containing [meta, output]
     * @param toolName The tool name to use as the id in the output meta map
     * @return Channel or List containing a single tuple [meta, outputSet] where meta = [id: toolName]
     * @throws IllegalArgumentException if chResults is null or toolName is null/empty
     */
    static Object gather(Object chResults, String toolName) {
        // Input validation
        if (chResults == null) {
            throw new IllegalArgumentException("chResults cannot be null")
        }
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("toolName cannot be null or empty")
        }

        // Detect if input is a channel
        if (chResults instanceof DataflowReadChannel || chResults instanceof DataflowWriteChannel) {
            // Apply built-in operators to the channel
            return chResults
                .collect { _meta, output -> output }
                .map { output -> [[id: toolName], output.toSet()] }
        } else {
            // Input is a list - process directly
            def outputs = chResults.collect { tuple -> tuple[1] }
            return [[id: toolName], outputs.toSet()]
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
                return channels[0].flatMap { meta, files -> 
                    files.collect { file -> [meta, file] }
                }
            }
            
            // Mix multiple channels and flatten
            def mixed = channels[0]
            channels[1..-1].each { ch -> mixed = mixed.mix(ch) }
            return mixed.flatMap { meta, files -> 
                files.collect { file -> [meta, file] }
            }
        } else {
            // Process lists directly
            def result = []
            channels.each { list ->
                list.each { tuple ->
                    def meta = tuple[0]
                    def files = tuple[1]
                    files.each { file ->
                        result.add([meta, file])
                    }
                }
            }
            return result
        }
    }
}
