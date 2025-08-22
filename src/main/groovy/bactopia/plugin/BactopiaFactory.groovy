/*
 * Copyright 2021, Seqera Labs
 * Modifications Copyright 2025, Robert A. Petit III
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
package bactopia.plugin

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceObserverFactoryV2

/**
 * Implements the observer factory
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class BactopiaFactory implements TraceObserverFactoryV2 {

    @Override
    Collection<TraceObserverV2> create(Session session) {
        final result = new ArrayList()
        result.add( new BactopiaObserver() )
        return result
    }
}
