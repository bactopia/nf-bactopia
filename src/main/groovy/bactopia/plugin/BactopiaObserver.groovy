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
import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.Paths

import nextflow.Session
import nextflow.script.WorkflowMetadata
import nextflow.trace.TraceObserverV2

import bactopia.plugin.BactopiaConfig
import static bactopia.plugin.BactopiaMotD.getMotD
import static bactopia.plugin.BactopiaTemplate.getWorkflowSummary
import static bactopia.plugin.BactopiaUtils.paramsHelp
import static bactopia.plugin.BactopiaUtils.paramsSummaryLog
import static bactopia.plugin.BactopiaUtils.validateParameters

/**
 * Bactopia workflow observer
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
class BactopiaObserver implements TraceObserverV2 {

    private Session session
    BactopiaConfig config

    BactopiaObserver(
        Session session,
        BactopiaConfig config
    ) {
        this.session = session
        this.config = config
    }

    @Override
    void onFlowCreate(Session session) {
        def Map params = this.session.params
        def WorkflowMetadata metadata = this.session.getWorkflowMetadata()
        if (params["help"] || params["help_all"]) {
            println paramsHelp(session, config)
            System.exit(0)
        } else {
            // print params summary
            println paramsSummaryLog(
                metadata,
                this.session,
                this.config,
                null
            )
        }
    }

    @Override
    void onFlowComplete() {
        def Map params = this.session.params
        def WorkflowMetadata metadata = this.session.getWorkflowMetadata()
        println getWorkflowSummary( 
            metadata,
            params,
            this.session.config.manifest.version,
            this.config.monochromeLogs,
        )

    }

    void onWorkflowPublish(String name, Object value) {}
    void onFilePublish(Path destination, Path source, Map annotations) {}
}
