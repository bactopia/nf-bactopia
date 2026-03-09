package bactopia.plugin.utils

import java.nio.file.Path

/**
 * Utility class for managing empty placeholder file paths.
 *
 * Empty files are used as placeholders in Bactopia input tuples when
 * certain input types are not available (e.g., no ONT reads for an
 * Illumina-only sample).
 */
class EmptyFiles {
    // File name constants
    static final String META = "EMPTY_META"
    static final String R1 = "EMPTY_R1"
    static final String R2 = "EMPTY_R2"
    static final String SE = "EMPTY_SE"
    static final String ONT = "EMPTY_ONT"
    static final String ASSEMBLY = "EMPTY_ASSEMBLY"
    static final String EXTRA = "EMPTY_EXTRA"
    static final String PROTEINS = "EMPTY_PROTEINS"
    static final String GBK = "EMPTY_GBK"
    static final String GFF = "EMPTY_GFF"
    static final String BLASTDB = "EMPTY_BLASTDB"
    static final String TF = "EMPTY_TF"
    static final String ADAPTERS = "EMPTY_ADAPTERS"
    static final String PHIX = "EMPTY_PHIX"

    /**
     * Returns all empty file paths as a map.
     *
     * @param baseDir The base directory containing the empty files
     * @return Map of empty file identifiers to their full paths
     */
    static Map<String, Path> getEmptyPaths(String baseDir) {
        return [
            empty_meta: Path.of("${baseDir}/${META}"),
            empty_r1: Path.of("${baseDir}/${R1}"),
            empty_r2: Path.of("${baseDir}/${R2}"),
            empty_se: Path.of("${baseDir}/${SE}"),
            empty_ont: Path.of("${baseDir}/${ONT}"),
            empty_assembly: Path.of("${baseDir}/${ASSEMBLY}"),
            empty_extra: Path.of("${baseDir}/${EXTRA}"),

            // QC
            empty_adapters: Path.of("${baseDir}/${ADAPTERS}"),
            empty_phix: Path.of("${baseDir}/${PHIX}"),

            // Annotations
            empty_proteins: Path.of("${baseDir}/${PROTEINS}"),
            empty_gbk: Path.of("${baseDir}/${GBK}"),
            empty_gff: Path.of("${baseDir}/${GFF}"),
            empty_blastdb: Path.of("${baseDir}/${BLASTDB}"),
            empty_tf: Path.of("${baseDir}/${TF}")
        ]
    }

    /**
     * Checks if the given input path is an empty placeholder file.
     *
     * @param input The input path string to check
     * @return true if the input contains the empty files path pattern, false otherwise
     */
    static Boolean isEmptyFile(input) {
        if (input == null) {
            return false
        }
        def String s = input.toString()
        if (s.isEmpty()) {
            return false
        }
        return s.contains("data/empty/EMPTY")
    }
}
