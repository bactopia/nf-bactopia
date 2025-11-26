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
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.DataflowQueue

/**
 * Utility class for sample data manipulation operations in Bactopia pipelines.
 * Provides functions for formatting and transforming sample tuples.
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 */
class SampleUtils {

    /**
     * Adapt 4-element sample tuples to appropriate size based on data availability.
     * Follows nf-core pattern: detects if input is a channel or list, applies built-in operators.
     * If dataTypes is a DataflowVariable (value channel), extracts the value first.
     *
     * @param samples Channel or List of tuples containing [meta, inputs, extra, extra2]
     * @param dataTypes Number of data types (1, 2, or 3) to include in output, or a DataflowVariable containing the number
     * @return Channel or List with tuples of appropriate size based on dataTypes
     * @throws IllegalArgumentException if samples is null or dataTypes is invalid
     */
    static Object formatSamples(Object samples, Object dataTypes) {
        // Input validation
        if (samples == null) {
            throw new IllegalArgumentException("samples cannot be null")
        }
        if (dataTypes == null) {
            throw new IllegalArgumentException("dataTypes cannot be null")
        }
        
        // Extract value from DataflowVariable if needed
        def actualDataTypes = dataTypes
        if (dataTypes instanceof DataflowVariable) {
            actualDataTypes = dataTypes.val
        }
        
        if (actualDataTypes < 1 || actualDataTypes > 3) {
            throw new IllegalArgumentException("dataTypes must be 1, 2, or 3 (received: ${actualDataTypes})")
        }

        // Check if we're dealing with a channel or list
        def isChannel = samples instanceof DataflowReadChannel || samples instanceof DataflowWriteChannel || samples instanceof DataflowQueue

        if (isChannel) {
            if (actualDataTypes == 1) {
                return samples.map { meta, inputs, _extra, _extra2 -> 
                    [meta, inputs]
                }
            } else if (actualDataTypes == 2) {
                return samples.map { meta, inputs, extra, _extra2 -> 
                    [meta, inputs, extra]
                }
            } else {
                // actualDataTypes == 3, return as-is
                return samples
            }
        } else {
            // Process list directly
            if (actualDataTypes == 1) {
                return samples.collect { tuple -> 
                    [tuple[0], tuple[1]]
                }
            } else if (actualDataTypes == 2) {
                return samples.collect { tuple -> 
                    [tuple[0], tuple[1], tuple[2]]
                }
            } else {
                // actualDataTypes == 3, return as-is
                return samples
            }
        }
    }
}
