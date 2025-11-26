//
// This file holds several functions used within the nf-core pipeline template.
//
// This is based on the original NF-Core template (the OG 'libs' folder) and the nf-validation
// plugin (which you should probably be using instead of this file).
package bactopia.plugin

import groovy.util.logging.Slf4j

import static bactopia.plugin.BactopiaMotD.getMotD

@Slf4j
class BactopiaTemplate {

    /**
     * ANSII Colors used for terminal logging.
     *
     * @param monochrome_logs Whether to use monochrome logs
     * @return Map containing color codes
     */
    public static Map getLogColors(Boolean monochrome_logs) {
        Map colorcodes = [:]

        // Reset / Meta
        colorcodes['reset']      = monochrome_logs ? '' : "\033[0m"
        colorcodes['bold']       = monochrome_logs ? '' : "\033[1m"
        colorcodes['dim']        = monochrome_logs ? '' : "\033[2m"
        colorcodes['italic']     = monochrome_logs ? '' : "\033[3m"
        colorcodes['underlined'] = monochrome_logs ? '' : "\033[4m"
        colorcodes['blink']      = monochrome_logs ? '' : "\033[5m"
        colorcodes['reverse']    = monochrome_logs ? '' : "\033[7m"
        colorcodes['hidden']     = monochrome_logs ? '' : "\033[8m"

        // Regular Colors
        colorcodes['black']      = monochrome_logs ? '' : "\033[0;30m"
        colorcodes['red']        = monochrome_logs ? '' : "\033[0;31m"
        colorcodes['green']      = monochrome_logs ? '' : "\033[0;32m"
        colorcodes['yellow']     = monochrome_logs ? '' : "\033[0;33m"
        colorcodes['blue']       = monochrome_logs ? '' : "\033[0;34m"
        colorcodes['purple']     = monochrome_logs ? '' : "\033[0;35m"
        colorcodes['cyan']       = monochrome_logs ? '' : "\033[0;36m"
        colorcodes['white']      = monochrome_logs ? '' : "\033[0;37m"

        // Bold
        colorcodes['bblack']     = monochrome_logs ? '' : "\033[1;30m"
        colorcodes['bred']       = monochrome_logs ? '' : "\033[1;31m"
        colorcodes['bgreen']     = monochrome_logs ? '' : "\033[1;32m"
        colorcodes['byellow']    = monochrome_logs ? '' : "\033[1;33m"
        colorcodes['bblue']      = monochrome_logs ? '' : "\033[1;34m"
        colorcodes['bpurple']    = monochrome_logs ? '' : "\033[1;35m"
        colorcodes['bcyan']      = monochrome_logs ? '' : "\033[1;36m"
        colorcodes['bwhite']     = monochrome_logs ? '' : "\033[1;37m"

        // Underline
        colorcodes['ublack']     = monochrome_logs ? '' : "\033[4;30m"
        colorcodes['ured']       = monochrome_logs ? '' : "\033[4;31m"
        colorcodes['ugreen']     = monochrome_logs ? '' : "\033[4;32m"
        colorcodes['uyellow']    = monochrome_logs ? '' : "\033[4;33m"
        colorcodes['ublue']      = monochrome_logs ? '' : "\033[4;34m"
        colorcodes['upurple']    = monochrome_logs ? '' : "\033[4;35m"
        colorcodes['ucyan']      = monochrome_logs ? '' : "\033[4;36m"
        colorcodes['uwhite']     = monochrome_logs ? '' : "\033[4;37m"

        // High Intensity
        colorcodes['iblack']     = monochrome_logs ? '' : "\033[0;90m"
        colorcodes['ired']       = monochrome_logs ? '' : "\033[0;91m"
        colorcodes['igreen']     = monochrome_logs ? '' : "\033[0;92m"
        colorcodes['iyellow']    = monochrome_logs ? '' : "\033[0;93m"
        colorcodes['iblue']      = monochrome_logs ? '' : "\033[0;94m"
        colorcodes['ipurple']    = monochrome_logs ? '' : "\033[0;95m"
        colorcodes['icyan']      = monochrome_logs ? '' : "\033[0;96m"
        colorcodes['iwhite']     = monochrome_logs ? '' : "\033[0;97m"

        // Bold High Intensity
        colorcodes['biblack']    = monochrome_logs ? '' : "\033[1;90m"
        colorcodes['bired']      = monochrome_logs ? '' : "\033[1;91m"
        colorcodes['bigreen']    = monochrome_logs ? '' : "\033[1;92m"
        colorcodes['biyellow']   = monochrome_logs ? '' : "\033[1;93m"
        colorcodes['biblue']     = monochrome_logs ? '' : "\033[1;94m"
        colorcodes['bipurple']   = monochrome_logs ? '' : "\033[1;95m"
        colorcodes['bicyan']     = monochrome_logs ? '' : "\033[1;96m"
        colorcodes['biwhite']    = monochrome_logs ? '' : "\033[1;97m"

        return colorcodes
    }

    /**
     * Prints a dashed line, some might even call it 'fancy'.
     *
     * @param monochrome_logs Whether to use monochrome logs
     * @return String containing the dashed line
     */
    public static String dashedLine(monochrome_logs = false) {
        Map colors = this.getLogColors(monochrome_logs)
        return "-${colors.dim}------------------------------------------------------------------${colors.reset}-"
    }

    /**
     * Get the Bactopia logo for the specified workflow.
     *
     * @param workflow The workflow metadata
     * @param monochrome_logs Whether to use monochrome logs
     * @param worflow_name The workflow name
     * @param worflow_description The workflow description
     * @return String containing the formatted logo
     */
    public static String getLogo(workflow, monochrome_logs, worflow_name, worflow_description) {
        Map colors = this.getLogColors(monochrome_logs)
        if (worflow_name == "bactopia") {
            String.format(
                """
                -${colors.dim}-------------------------------------------${colors.reset}-
                ${colors.blue}   _                _              _             ${colors.reset}
                ${colors.blue}  | |__   __ _  ___| |_ ___  _ __ (_) __ _       ${colors.reset}
                ${colors.blue}  | '_ \\ / _` |/ __| __/ _ \\| '_ \\| |/ _` |   ${colors.reset}
                ${colors.blue}  | |_) | (_| | (__| || (_) | |_) | | (_| |      ${colors.reset}
                ${colors.blue}  |_.__/ \\__,_|\\___|\\__\\___/| .__/|_|\\__,_| ${colors.reset}
                ${colors.blue}                            |_|                  ${colors.reset}
                ${colors.cyan}  ${workflow.manifest.name} v${workflow.manifest.version}${colors.reset}
                ${colors.cyan}  ${worflow_description} ${colors.reset}
                -${colors.dim}-------------------------------------------${colors.reset}-
                """.stripIndent()
            )
        } else if (worflow_name == "staphopia") {
            String.format(
                """
                -${colors.dim}------------------------------------------------${colors.reset}-
                ${colors.blue}       _              _                 _            ${colors.reset}
                ${colors.blue}   ___| |_ __ _ _ __ | |__   ___  _ __ (_) __ _      ${colors.reset}
                ${colors.blue}  / __| __/ _` | '_ \\| '_ \\ / _ \\| '_ \\| |/ _` | ${colors.reset}
                ${colors.blue}  \\__ \\ || (_| | |_) | | | | (_) | |_) | | (_| |   ${colors.reset}
                ${colors.blue}  |___/\\__\\__,_| .__/|_| |_|\\___/| .__/|_|\\__,_| ${colors.reset}
                ${colors.blue}               |_|               |_|                 ${colors.reset}
                ${colors.cyan}  staphopia v${workflow.manifest.version}${colors.reset}
                ${colors.cyan}  ${worflow_description} ${colors.reset}
                -${colors.dim}------------------------------------------------${colors.reset}-
                """.stripIndent()
            )
        } else if (worflow_name == "enteropia") {
            String.format(
                """
                -${colors.dim}---------------------------------------------------------------------------------------${colors.reset}-
                ${colors.blue}              _                       _             ${colors.reset}
                ${colors.blue}    ___ _ __ | |_ ___ _ __ ___  _ __ (_) __ _       ${colors.reset}
                ${colors.blue}   / _ \\ '_ \\| __/ _ \\ '__/ _ \\| '_ \\| |/ _` | ${colors.reset}
                ${colors.blue}  |  __/ | | | ||  __/ | | (_) | |_) | | (_| |      ${colors.reset}
                ${colors.blue}   \\___|_| |_|\\__\\___|_|  \\___/| .__/|_|\\__,_| ${colors.reset}
                ${colors.blue}                               |_|                  ${colors.reset}
                ${colors.cyan}  enteropia v${workflow.manifest.version}${colors.reset}
                ${colors.cyan}  ${worflow_description} ${colors.reset}
                -${colors.dim}---------------------------------------------------------------------------------------${colors.reset}-
                """.stripIndent()
            )
        } else if (worflow_name == "cleanyerreads") {
            String.format(
                """
                -${colors.dim}------------------------------------------------------------------------${colors.reset}-
                ${colors.blue}    ____ _                   __   __          ____                _              ${colors.reset}
                ${colors.blue}   / ___| | ___  __ _ _ __   \\ \\ / /__ _ __  |  _ \\ ___  __ _  __| |___       ${colors.reset}
                ${colors.blue}  | |   | |/ _ \\/ _` | '_ \\   \\ V / _ \\ '__| | |_) / _ \\/ _` |/ _` / __|    ${colors.reset}
                ${colors.blue}  | |___| |  __/ (_| | | | |   | |  __/ |    |  _ <  __/ (_| | (_| \\__ \\       ${colors.reset}
                ${colors.blue}   \\____|_|\\___|\\__,_|_| |_|   |_|\\___|_|    |_| \\_\\___|\\__,_|\\__,_|___/ ${colors.reset}
                ${colors.blue} ${colors.reset}
                ${colors.blue}                      ⠀⠀⡶⠛⠲⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡶⠚⢲⡀⠀           ${colors.reset}
                ${colors.blue}                      ⣰⠛⠃⠀⢠⣏⠀⠀⠀⠀⣀⣠⣤⣤⣤⣤⣤⣤⣤⣀⡀⠀⠀⠀⣸⡇⠀⠈⠙⣧         ${colors.reset}
                ${colors.blue}                      ⠸⣦⣤⣄⠀⠙⢷⣤⣶⠟⠛⢉⣁⣠⣤⣤⣤⣀⣉⠙⠻⢷⣤⡾⠋⢀⣠⣤⣴⠟        ${colors.reset}
                ${colors.blue}                      ⠀⠀⠀⠈⠳⣤⡾⠋⣀⣴⣿⣿⠿⠿⠟⠛⠿⠿⣿⣿⣶⣄⠙⢿⣦⠟⠁⠀⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⠀⢀⣾⠟⢀⣼⣿⠟⠋⠀⠀⠀⠀⠀⠀⠀⠀⠉⠻⣿⣷⡄⠹⣷⡀⠀⠀⠀          ${colors.reset}
                ${colors.blue}                      ⠀⠀⠀⣾⡏⢠⣿⣿⡯⠤⠤⠤⠒⠒⠒⠒⠒⠒⠒⠤⠤⠽⣿⣿⡆⠹⣷⡀⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⢸⣟⣠⡿⠿⠟⠒⣒⣒⣈⣉⣉⣉⣉⣉⣉⣉⣁⣒⣒⡛⠻⠿⢤⣹⣇⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⣾⡭⢤⣤⣠⡞⠉⠉⢀⣀⣀⠀⠀⠀⠀⢀⣀⣀⠀⠈⢹⣦⣤⡤⠴⣿⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⣿⡇⢸⣿⣿⣇⠀⣼⣿⣿⣿⣷⠀⠀⣼⣿⣿⣿⣷⠀⢸⣿⣿⡇⠀⣿⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⢻⡇⠸⣿⣿⣿⡄⢿⣿⣿⣿⡿⠀⠀⢿⣿⣿⣿⡿⢀⣿⣿⣿⡇⢸⣿⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⠸⣿⡀⢿⣿⣿⣿⣆⠉⠛⠋⠁⢴⣶⠀⠉⠛⠉⣠⣿⣿⣿⡿⠀⣾⠇⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⠀⢻⣷⡈⢻⣿⣿⣿⣿⣶⣤⣀⣈⣁⣀⡤⣴⣿⣿⣿⣿⡿⠁⣼⠟⠀⠀⠀         ${colors.reset}
                ${colors.blue}                      ⠀⠀⠀⢀⣽⣷⣄⠙⢿⣿⣿⡟⢲⠧⡦⠼⠤⢷⢺⣿⣿⡿⠋⣠⣾⢿⣄⠀⠀⠀         ${colors.reset}
                ${colors.blue}                      ⢰⠟⠛⠟⠁⣨⡿⢷⣤⣈⠙⢿⡙⠒⠓⠒⠓⠚⣹⠛⢉⣠⣾⠿⣧⡀⠙⠋⠙⣆         ${colors.reset}
                ${colors.blue}                      ⠹⣄⡀⠀⠐⡏⠀⠀⠉⠛⠿⣶⣿⣦⣤⣤⣤⣶⣷⡾⠟⠋⠀⠀⢸⡇⠀⢠⣤⠟         ${colors.reset}
                ${colors.blue}                      ⠀⠀⠳⢤⠼⠃⠀⠀⠀⠀⠀⠀⠈⠉⠉⠉⠉⠁⠀⠀⠀⠀⠀⠀⠘⠷⢤⠾⠁⠀         ${colors.reset}
                ${colors.blue} ${colors.reset}
                ${colors.cyan} clean-yer-reads v${workflow.manifest.version}${colors.reset}
                ${colors.cyan} ${worflow_description} ${colors.reset}
                -${colors.dim}------------------------------------------------------------------------${colors.reset}-
                """.stripIndent()
            )
        } else if (worflow_name == "teton") {
            String.format(
                """
                -${colors.dim}------------------------------------------------------------------${colors.reset}-
                ${colors.blue}   _       _                          _     *                     ${colors.reset}
                ${colors.blue}  | |_ ___| |_ ___  _ __       *     / \\_       *   /\\'__       ${colors.reset}
                ${colors.blue}  | __/ _ \\ __/ _ \\| '_ \\       /\\ _/    \\        _/  /  \\  * ${colors.reset}
                ${colors.blue}  | ||  __/ || (_) | | | |     /\\/\\  /\\/  \\_   _^/  ^/    `--.  ${colors.reset}
                ${colors.blue}   \\__\\___|\\__\\___/|_| |_|    /    \\/  \\    \\ /.' ^_   \\_   .'\\ ${colors.reset}
                ${colors.blue}                                      Art by Joan Stark         ${colors.reset}
                ${colors.blue}  ${colors.reset}
                ${colors.cyan}  teton v${workflow.manifest.version}${colors.reset}
                ${colors.cyan}  ${worflow_description}${colors.reset}
                -${colors.dim}------------------------------------------------------------------${colors.reset}-
                """.stripIndent()
            )
        } else {
            String.format("""
                -${colors.dim}------------------------------------------------------------------${colors.reset}-
                ${colors.blue}   _                _              _         _              _              ${colors.reset}
                ${colors.blue}  | |__   __ _  ___| |_ ___  _ __ (_) __ _  | |_ ___   ___ | |___          ${colors.reset}
                ${colors.blue}  | '_ \\ / _` |/ __| __/ _ \\| '_ \\| |/ _` | | __/ _ \\ / _ \\| / __|    ${colors.reset}
                ${colors.blue}  | |_) | (_| | (__| || (_) | |_) | | (_| | | || (_) | (_) | \\__ \\        ${colors.reset}
                ${colors.blue}  |_.__/ \\__,_|\\___|\\__\\___/| .__/|_|\\__,_|  \\__\\___/ \\___/|_|___/ ${colors.reset}
                ${colors.blue}                            |_|                                            ${colors.reset}
                ${colors.cyan}  bactopia tools ${worflow_name} v${workflow.manifest.version}${colors.reset}
                ${colors.cyan}  ${worflow_description} ${colors.reset}
                -${colors.dim}------------------------------------------------------------------${colors.reset}-
                """.stripIndent()
            )
        }
    }

    /**
     * Get the workflow execution summary.
     *
     * @param workflow The workflow metadata
     * @param params The workflow parameters
     * @param manifest_version The manifest version
     * @param monochrome_logs Whether to use monochrome logs
     * @return String containing the formatted workflow summary
     */
    public static String getWorkflowSummary(workflow, params, manifest_version, monochrome_logs) {
        Map colors = getLogColors(monochrome_logs)
        def output = "\n" + dashedLine(monochrome_logs) + "\n\n"
        if (params.workflow.name == "bactopia") {
            output += "${colors.bold}Bactopia Execution Summary${colors.reset}"
        } else if (params.workflow.name == "staphopia") {
            output += "${colors.bold}Staphopia Execution Summary${colors.reset}"
        } else if (params.workflow.name == "enteropia") {
            output += "${colors.bold}Enteropia Execution Summary${colors.reset}"
        } else if (params.workflow.name == "cleanyerreads") {
            output += "${colors.bold}Clean-yer-reads Execution Summary${colors.reset}"
        } else if (params.workflow.name == "teton") {
            output += "${colors.bold}Teton Execution Summary${colors.reset}"
        } else {
            output += "${colors.bold}Bactopia Tool Execution Summary${colors.reset}"
        }
        output += """
            -${colors.dim}-----------------------------${colors.reset}-
            Workflow         : ${params.workflow.name}
            Bactopia Version : ${manifest_version}
            Nextflow Version : ${workflow.nextflow.version}
            Command Line     : ${workflow.commandLine}
            Launch Dir       : ${workflow.launchDir}
            Profile          : ${workflow.profile}
            Completed At     : ${workflow.complete}
            Duration         : ${workflow.duration}
            Resumed          : ${workflow.resume}
            """.stripIndent()

        if (workflow.success) {
            output += "Success          : ${workflow.success}\n"
            output += "${colors.bgreen}Merged Results${colors.reset}   : ${colors.green}${params.outdir}/${params.rundir}${colors.reset}\n\n"

            if (params.workflow.name == "bactopia" || params.workflow.name == "staphopia") {
                output += """
                    ${colors.bold}Further analyze your samples using Bactopia Tools, with the following command:${colors.reset}
                    -${colors.dim}------------------------------------------------------------------------------${colors.reset}-
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf <REPLACE_WITH_BACTOPIA_TOOL_NAME>${colors.reset}

                    Examples:
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf pangenome${colors.reset}
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf merlin${colors.reset}
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf sccmec${colors.reset}

                    See the full list of available Bactopia Tools: ${colors.cyan}bactopia --list_wfs${colors.reset}
                """.stripIndent()
            } else if (params.workflow.name == "teton") {
                output += """
                    ${colors.bold}Further analyze bacterial samples using Bactopia, with the following command:${colors.reset}
                    -${colors.dim}------------------------------------------------------------------------------${colors.reset}-
                    ${colors.cyan}bactopia -profile ${workflow.profile} --samples ${params.outdir}/bactopia-runs/${params.rundir}/merged-results/teton-prepare.tsv${colors.reset}
                """.stripIndent()
            } else if (params.workflow.name == "cleanyerreads") {
                output += """
                    ${colors.bold}Further analyze your samples using Bactopia Tools, with the following command:${colors.reset}
                    -${colors.dim}------------------------------------------------------------------------------${colors.reset}-
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf <REPLACE_WITH_BACTOPIA_TOOL_NAME>${colors.reset}

                    Examples:
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf bracken${colors.reset}
                    ${colors.cyan}bactopia -profile ${workflow.profile} --bactopia ${params.outdir} --wf kraken2${colors.reset}

                    See the full list of available Bactopia Tools: ${colors.cyan}bactopia --list_wfs${colors.reset}
                """.stripIndent()
            }

            output += """
                ${colors.bold}Message of the Day${colors.reset}
                -${colors.dim}-----------------------------${colors.reset}-
                ${getMotD(monochrome_logs)}
            """.stripIndent()
        } else {
            output += "Success          : ${colors.red}${workflow.success}${colors.reset}"
        }

        return output
    }
}
