//
// This file holds several functions used within the nf-core pipeline template.
//
// This is based on the original NF-Core template (the OG 'libs' folder) and the nf-validation
// plugin (which you should probably be using instead of this file).
package nextflow.bactopia

import groovy.util.logging.Slf4j

@Slf4j
class BactopiaTemplate {

    //
    // Check params.hostnames
    //
    public static void hostName(workflow, params, log) {
        Map colors = logColours(params.monochrome_logs)
        if (params.hostnames) {
            try {
                def hostname = "hostname".execute().text.trim()
                params.hostnames.each { prof, hnames ->
                    hnames.each { hname ->
                        if (hostname.contains(hname) && !workflow.profile.contains(prof)) {
                            log.info "=${colors.yellow}====================================================${colors.reset}=\n" +
                                "${colors.yellow}WARN: You are running with `-profile $workflow.profile`\n" +
                                "      but your machine hostname is ${colors.white}'$hostname'${colors.reset}.\n" +
                                "      ${colors.yellow_bold}Please use `-profile $prof${colors.reset}`\n" +
                                "=${colors.yellow}====================================================${colors.reset}="
                        }
                    }
                }
            } catch (Exception e) {
                log.warn "[$workflow.manifest.name] Could not determine 'hostname' - skipping check. Reason: ${e.message}."
            }
        }
    }

    //
    // Construct and send completion email
    //
    public static void email(workflow, params, summary_params, projectDir, log, multiqc_report=[]) {

        // Set up the e-mail variables
        def subject = "[$workflow.manifest.name] Successful: $workflow.runName"
        if (!workflow.success) {
            subject = "[$workflow.manifest.name] FAILED: $workflow.runName"
        }

        def summary = [:]
        for (group in summary_params.keySet()) {
            summary << summary_params[group]
        }

        def misc_fields = [:]
        misc_fields['Date Started']              = workflow.start
        misc_fields['Date Completed']            = workflow.complete
        misc_fields['Pipeline script file path'] = workflow.scriptFile
        misc_fields['Pipeline script hash ID']   = workflow.scriptId
        if (workflow.repository) misc_fields['Pipeline repository Git URL']    = workflow.repository
        if (workflow.commitId)   misc_fields['Pipeline repository Git Commit'] = workflow.commitId
        if (workflow.revision)   misc_fields['Pipeline Git branch/tag']        = workflow.revision
        misc_fields['Nextflow Version']           = workflow.nextflow.version
        misc_fields['Nextflow Build']             = workflow.nextflow.build
        misc_fields['Nextflow Compile Timestamp'] = workflow.nextflow.timestamp

        def email_fields = [:]
        email_fields['version']      = workflow.manifest.version
        email_fields['runName']      = workflow.runName
        email_fields['success']      = workflow.success
        email_fields['dateComplete'] = workflow.complete
        email_fields['duration']     = workflow.duration
        email_fields['exitStatus']   = workflow.exitStatus
        email_fields['errorMessage'] = (workflow.errorMessage ?: 'None')
        email_fields['errorReport']  = (workflow.errorReport ?: 'None')
        email_fields['commandLine']  = workflow.commandLine
        email_fields['projectDir']   = workflow.projectDir
        email_fields['summary']      = summary << misc_fields

        // On success try attach the multiqc report
        def mqc_report = null
        try {
            if (workflow.success) {
                mqc_report = multiqc_report.getVal()
                if (mqc_report.getClass() == ArrayList && mqc_report.size() >= 1) {
                    if (mqc_report.size() > 1) {
                        log.warn "[$workflow.manifest.name] Found multiple reports from process 'MULTIQC', will use only one"
                    }
                    mqc_report = mqc_report[0]
                }
            }
        } catch (all) {
            if (multiqc_report) {
                log.warn "[$workflow.manifest.name] Could not attach MultiQC report to summary email"
            }
        }

        // Check if we are only sending emails on failure
        def email_address = params.email
        if (!params.email && params.email_on_fail && !workflow.success) {
            email_address = params.email_on_fail
        }

        // Render the TXT template
        def engine       = new groovy.text.GStringTemplateEngine()
        def tf           = new File("$projectDir/assets/email_template.txt")
        def txt_template = engine.createTemplate(tf).make(email_fields)
        def email_txt    = txt_template.toString()

        // Render the HTML template
        def hf            = new File("$projectDir/assets/email_template.html")
        def html_template = engine.createTemplate(hf).make(email_fields)
        def email_html    = html_template.toString()

        // Render the sendmail template
        def max_multiqc_email_size = params.max_multiqc_email_size as nextflow.util.MemoryUnit
        def smail_fields           = [ email: email_address, subject: subject, email_txt: email_txt, email_html: email_html, projectDir: "$projectDir", mqcFile: mqc_report, mqcMaxSize: max_multiqc_email_size.toBytes() ]
        def sf                     = new File("$projectDir/assets/sendmail_template.txt")
        def sendmail_template      = engine.createTemplate(sf).make(smail_fields)
        def sendmail_html          = sendmail_template.toString()

        // Send the HTML e-mail
        Map colors = logColours(params.monochrome_logs)
        if (email_address) {
            try {
                if (params.plaintext_email) { throw GroovyException('Send plaintext e-mail, not HTML') }
                // Try to send HTML e-mail using sendmail
                [ 'sendmail', '-t' ].execute() << sendmail_html
                log.info "-${colors.purple}[$workflow.manifest.name]${colors.green} Sent summary e-mail to $email_address (sendmail)-"
            } catch (all) {
                // Catch failures and try with plaintext
                def mail_cmd = [ 'mail', '-s', subject, '--content-type=text/html', email_address ]
                if ( mqc_report.size() <= max_multiqc_email_size.toBytes() ) {
                    mail_cmd += [ '-A', mqc_report ]
                }
                mail_cmd.execute() << email_html
                log.info "-${colors.purple}[$workflow.manifest.name]${colors.green} Sent summary e-mail to $email_address (mail)-"
            }
        }

        // Write summary e-mail HTML to a file
        def output_d = new File("${params.outdir}/pipeline_info/")
        if (!output_d.exists()) {
            output_d.mkdirs()
        }
        def output_hf = new File(output_d, "pipeline_report.html")
        output_hf.withWriter { w -> w << email_html }
        def output_tf = new File(output_d, "pipeline_report.txt")
        output_tf.withWriter { w -> w << email_txt }
    }

    //
    // Print pipeline summary on completion
    //
    public static void summary(workflow, params, log) {
        Map colors = logColours(params.monochrome_logs)
        if (workflow.success) {
            if (workflow.stats.ignoredCount == 0) {
                log.info "-${colors.purple}[$workflow.manifest.name]${colors.green} Pipeline completed successfully${colors.reset}-"
            } else {
                log.info "-${colors.purple}[$workflow.manifest.name]${colors.red} Pipeline completed successfully, but with errored process(es) ${colors.reset}-"
            }
        } else {
            hostName(workflow, params, log)
            log.info "-${colors.purple}[$workflow.manifest.name]${colors.red} Pipeline completed with errors${colors.reset}-"
        }
    }

    //
    // ANSII Colors used for terminal logging
    //
    public static Map getLogColors(Boolean monochrome_logs) {
        Map colorcodes = [:]

        // Reset / Meta
        colorcodes['reset']      = monochrome_logs ? '' : "\033[0m"
        colorcodes['bold']       = monochrome_logs ? '' : "\033[1m"
        colorcodes['dim']        = monochrome_logs ? '' : "\033[2m"
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

    //
    // Prints a dashed line, some might even call it 'fancy'
    //
    public static String dashedLine(monochrome_logs) {
        Map colors = this.getLogColors(monochrome_logs)
        return "-${colors.dim}------------------------------------------------------------------${colors.reset}-"
    }

    //
    // Bactopia Logos
    //
    public static String getLogo(workflow, monochrome_logs, worflow_name, worflow_description) {
        Map colors = this.getLogColors(monochrome_logs)
        if (worflow_name == "bactopia") {
            String.format(
                """\n
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
                """\n
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
                """\n
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
                """\n
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
                """\n
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
            String.format(
                """\n
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
}
