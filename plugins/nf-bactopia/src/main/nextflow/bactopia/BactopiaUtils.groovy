//
// This file holds several Groovy functions that could be useful for any Nextflow pipeline
//
// Modified from NF-Core's template: https://github.com/nf-core/tools
package nextflow.bactopia

import groovy.util.logging.Slf4j
import java.io.RandomAccessFile
import java.util.stream.IntStream
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml

import static nextflow.bactopia.BactopiaTemplate.logError

@Slf4j
class BactopiaUtils {


    //
    // When running with -profile conda, warn if channels have not been set-up appropriately
    //
    public static void checkCondaChannels() {
        Yaml parser = new Yaml()
        def channels = []
        try {
            def config = parser.load("conda config --show channels".execute().text)
            channels = config.channels
        } catch(NullPointerException | IOException e) {
            log.warn "Could not verify conda channel configuration."
            return
        }

        // Check that all channels are present
        def required_channels = ['conda-forge', 'bioconda', 'defaults']
        def conda_check_failed = !required_channels.every { ch -> ch in channels }

        // Check that they are in the right order
        conda_check_failed |= !(channels.indexOf('conda-forge') < channels.indexOf('bioconda'))
        conda_check_failed |= !(channels.indexOf('bioconda') < channels.indexOf('defaults'))

        if (conda_check_failed) {
            log.warn "=============================================================================\n" +
                "  There is a problem with your Conda configuration!\n\n" +
                "  You will need to set-up the conda-forge and bioconda channels correctly.\n" +
                "  Please refer to https://bioconda.github.io/user/install.html#set-up-channels\n" +
                "  NB: The order of the channels matters!\n" +
                "==================================================================================="
        }
    }


    //
    // Join module args with appropriate spacing
    //
    public static String joinModuleArgs(args_list) {
        return ' ' + args_list.join(' ')
    }


    //
    //  Verify input is a positive integer
    //
    public static Integer isPositiveInteger(value, name) {
        def error = 0
        if (value.getClass() == Integer) {
            if (value < 0) {
                log.error '* --'+ name +': "' + value + '" is not a positive integer.'
                error = 1
            }
        } else {
            if (!value.isInteger()) {
                log.error '* --'+ name +': "' + value + '" is not numeric.'
                error = 1
            } else if (value as Integer < 0) {
                log.error '* --'+ name +': "' + value + '" is not a positive integer.'
                error = 1
            }
        }
        return error
    }


    //
    //  Verify input file exists
    //
    public static Boolean fileExists(filename) {
        if (isLocal(filename)) {
            return new File(filename).exists()
        }
        // For remote files, we assume they exist
        return true
    }


    //
    // Check if file is remote (e.g. AWS, Azure, GCP)
    //
    public static Boolean isLocal(filename) {
        if (filename.startsWith('gs://') || filename.startsWith('s3://') || filename.startsWith('az://') || filename.startsWith('https://')) {
            return false
        }
        return true
    }


    //
    //  Check is a file is not found
    //
    public static Integer fileNotFound(filename, parameter) {
        if (!fileExists(filename)) {
            logError('* --'+ parameter +': Unable to find "' + filename + '", please verify it exists.'.trim())
            return 1
        }
        return 0
    }


    //
    //  Verify input file is GZipped
    //
    public static Integer fileNotGzipped(filename, parameter) {
        // https://github.com/ConnectedPlacesCatapult/TomboloDigitalConnector/blob/master/src/main/java/uk/org/tombolo/importer/ZipUtils.java

        if (fileNotFound(filename, parameter)) {
            return 1
        } else {
            int magic = 0
            try {
                RandomAccessFile raf = new RandomAccessFile(new File(filename), "r");
                magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
                raf.close();
            } catch (Throwable e) {
                log.error('* --'+ parameter +': Please verify "' + filename + '" is compressed using GZIP')
                return 1
            }

            if (magic == GZIPInputStream.GZIP_MAGIC) {
                return 0
            } else {
                log.error('* --'+ parameter +': Please verify "' + filename + '" is compressed using GZIP')
                return 1
            }
        }
    }
}
