package bactopia.plugin

import groovy.util.logging.Slf4j

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.config.spec.ScopeName
import nextflow.script.dsl.Description

/**
 * This class allows model an specific configuration, extracting values from a map and converting
 *
 * @author : Robert Petit <robbie.petit@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
@ScopeName('bactopia')
@Description('''
    The `bactopia` scope allows you to configure the `nf-bactopia` plugin.
''')
class BactopiaConfig implements ConfigScope  {

    @ConfigOption
    @Description('Enable monochrome logs.')
    final public Boolean monochromeLogs = false

    @ConfigOption
    @Description('Path to the parameters schema file.')
    final public CharSequence parametersSchema = "nextflow_schema.json"

    @ConfigOption
    @Description('Ignore specific parameters.')
    final public Set<CharSequence> ignoreParams = [] // Defaults will be set on the Bactopia side

    // Keep the no-arg constructor in order to be able to use the `@ConfigOption` annotation
    BactopiaConfig(){}

    BactopiaConfig(Map map, Map params){
        def config = map ?: Collections.emptyMap()

        // monochromeLogs
        if(config.containsKey("monochromeLogs")) {
            if(config.monochromeLogs instanceof Boolean) {
                monochromeLogs = config.monochromeLogs
                log.debug("Set `bactopia.monochromeLogs` to ${monochromeLogs}")
            } else {
                logWarn("Incorrect value detected for `bactopia.monochromeLogs`, a boolean is expected. Defaulting to `${monochromeLogs}`")
            }
        }

        // parametersSchema
        if(config.containsKey("parametersSchema")) {
            if(config.parametersSchema instanceof CharSequence) {
                parametersSchema = config.parametersSchema
                log.debug("Set `bactopia.parametersSchema` to ${parametersSchema}")
            } else {
                logWarn("Incorrect value detected for `bactopia.parametersSchema`, a string is expected. Defaulting to `${parametersSchema}`")
            }
        }

        // ignoreParams
        if(config.containsKey("ignoreParams")) {
            if(config.ignoreParams instanceof List<CharSequence>) {
                ignoreParams += config.ignoreParams
                log.debug("Added the following parameters to the ignored parameters: ${config.ignoreParams}")
            } else {
                logWarn("Incorrect value detected for `bactopia.ignoreParams`, a list with string values is expected. Defaulting to `${ignoreParams}`")
            }
        }
    }
}
