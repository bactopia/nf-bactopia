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
package nextflow.bactopia

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceObserver

import nextflow.bactopia.BactopiaConfig
import nextflow.bactopia.nfschema.HelpMessageCreator

/**
 * Bactopia workflow observer
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class BactopiaObserver implements TraceObserver {

    @Override
    void onFlowCreate(Session session) {
        // Help message logic
        def Map params = (Map)session.params ?: [:]
        def BactopiaConfig config = new BactopiaConfig(session?.config?.navigate('bactopia') as Map, params)
        if (params["help"] || params["help_all"]) {
            def String help = ""
            def HelpMessageCreator helpCreator = new HelpMessageCreator(config, session, params["help_all"])
            help += helpCreator.getBeforeText(session, (String) params["workflow"]["name"], (String) params["workflow"]["description"])
            if (params["help_all"]) {
                log.debug("Printing out the full help message")
                help += helpCreator.getFullMessage()
            } else if (params["help"]) {
                log.debug("Printing out the short help message")
                def paramValue = null
                help += helpCreator.getShortMessage(paramValue instanceof String ? paramValue : "")
            }
            help += helpCreator.getAfterText()
            log.info(help)
            System.exit(0)
        }
    }

    @Override
    void onFlowComplete() {
        log.info "Pipeline complete! 👋"
    }
}
