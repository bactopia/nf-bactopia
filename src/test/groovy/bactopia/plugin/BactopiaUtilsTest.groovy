package bactopia.plugin

import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tests for BactopiaUtils class covering internal utility functions.
 */
class BactopiaUtilsTest extends Specification {

    @TempDir
    Path tempDir

    // ========================================================================
    // Tests for isPositiveInteger()
    // ========================================================================

    def "test_isPositiveInteger_withValidIntegerObject"() {
        when:
        def result = BactopiaUtils.isPositiveInteger(5, "test_param")

        then:
        result == 0
    }

    def "test_isPositiveInteger_withZero"() {
        when:
        def result = BactopiaUtils.isPositiveInteger(0, "test_param")

        then:
        result == 0
    }

    def "test_isPositiveInteger_withNegativeInteger"() {
        when:
        def result = BactopiaUtils.isPositiveInteger(-5, "test_param")

        then:
        result == 1
    }

    def "test_isPositiveInteger_withValidIntegerString"() {
        when:
        def result = BactopiaUtils.isPositiveInteger("42", "test_param")

        then:
        result == 0
    }

    def "test_isPositiveInteger_withNegativeIntegerString"() {
        when:
        def result = BactopiaUtils.isPositiveInteger("-10", "test_param")

        then:
        result == 1
    }

    def "test_isPositiveInteger_withNonNumericString"() {
        when:
        def result = BactopiaUtils.isPositiveInteger("not_a_number", "test_param")

        then:
        result == 1
    }

    def "test_isPositiveInteger_withFloatString"() {
        when:
        def result = BactopiaUtils.isPositiveInteger("3.14", "test_param")

        then:
        result == 1
    }

    // ========================================================================
    // Tests for fileExists()
    // ========================================================================

    def "test_fileExists_withExistingFile"() {
        given:
        def testFile = tempDir.resolve("test.txt").toFile()
        testFile.text = "test content"

        when:
        def result = BactopiaUtils.fileExists(testFile.absolutePath)

        then:
        result == true
    }

    def "test_fileExists_withNonExistentFile"() {
        given:
        def nonExistentPath = tempDir.resolve("does_not_exist.txt").toString()

        when:
        def result = BactopiaUtils.fileExists(nonExistentPath)

        then:
        result == false
    }

    def "test_fileExists_withRemoteS3File"() {
        when:
        def result = BactopiaUtils.fileExists("s3://bucket/file.txt")

        then:
        result == true  // Remote files are assumed to exist
    }

    def "test_fileExists_withRemoteGSFile"() {
        when:
        def result = BactopiaUtils.fileExists("gs://bucket/file.txt")

        then:
        result == true  // Remote files are assumed to exist
    }

    def "test_fileExists_withRemoteAzureFile"() {
        when:
        def result = BactopiaUtils.fileExists("az://container/file.txt")

        then:
        result == true  // Remote files are assumed to exist
    }

    def "test_fileExists_withRemoteHttpsFile"() {
        when:
        def result = BactopiaUtils.fileExists("https://example.com/file.txt")

        then:
        result == true  // Remote files are assumed to exist
    }

    // ========================================================================
    // Tests for isLocal()
    // ========================================================================

    def "test_isLocal_withLocalPath"() {
        when:
        def result = BactopiaUtils.isLocal("/path/to/local/file.txt")

        then:
        result == true
    }

    def "test_isLocal_withRelativePath"() {
        when:
        def result = BactopiaUtils.isLocal("relative/path/file.txt")

        then:
        result == true
    }

    def "test_isLocal_withS3Path"() {
        when:
        def result = BactopiaUtils.isLocal("s3://bucket/file.txt")

        then:
        result == false
    }

    def "test_isLocal_withGoogleStoragePath"() {
        when:
        def result = BactopiaUtils.isLocal("gs://bucket/file.txt")

        then:
        result == false
    }

    def "test_isLocal_withAzurePath"() {
        when:
        def result = BactopiaUtils.isLocal("az://container/file.txt")

        then:
        result == false
    }

    def "test_isLocal_withHttpsPath"() {
        when:
        def result = BactopiaUtils.isLocal("https://example.com/file.txt")

        then:
        result == false
    }

    def "test_isLocal_withHttpPath"() {
        when:
        def result = BactopiaUtils.isLocal("http://example.com/file.txt")

        then:
        result == true  // http (without 's') is considered local per current implementation
    }

    // ========================================================================
    // Tests for fileNotFound()
    // ========================================================================

    def "test_fileNotFound_withExistingFile"() {
        given:
        def testFile = tempDir.resolve("exists.txt").toFile()
        testFile.text = "content"

        when:
        def result = BactopiaUtils.fileNotFound(testFile.absolutePath, "test_param")

        then:
        result == 0
    }

    def "test_fileNotFound_withNonExistentFile"() {
        given:
        def nonExistentPath = tempDir.resolve("missing.txt").toString()

        when:
        def result = BactopiaUtils.fileNotFound(nonExistentPath, "test_param")

        then:
        result == 1
    }

    def "test_fileNotFound_withRemoteFile"() {
        when:
        def result = BactopiaUtils.fileNotFound("s3://bucket/file.txt", "test_param")

        then:
        result == 0  // Remote files are assumed to exist
    }

    // ========================================================================
    // Tests for fileNotGzipped()
    // ========================================================================

    def "test_fileNotGzipped_withGzippedFile"() {
        given:
        def gzFile = tempDir.resolve("test.txt.gz").toFile()
        // Create a proper GZIP file
        def output = new java.util.zip.GZIPOutputStream(new FileOutputStream(gzFile))
        output.write("test content".bytes)
        output.close()

        when:
        def result = BactopiaUtils.fileNotGzipped(gzFile.absolutePath, "test_param")

        then:
        result == 0
    }

    def "test_fileNotGzipped_withNonGzippedFile"() {
        given:
        def textFile = tempDir.resolve("test.txt").toFile()
        textFile.text = "plain text content"

        when:
        def result = BactopiaUtils.fileNotGzipped(textFile.absolutePath, "test_param")

        then:
        result == 1
    }

    def "test_fileNotGzipped_withNonExistentFile"() {
        given:
        def nonExistentPath = tempDir.resolve("missing.txt.gz").toString()

        when:
        def result = BactopiaUtils.fileNotGzipped(nonExistentPath, "test_param")

        then:
        result == 1
    }

    def "test_fileNotGzipped_withEmptyFile"() {
        given:
        def emptyFile = tempDir.resolve("empty.txt").toFile()
        emptyFile.text = ""

        when:
        def result = BactopiaUtils.fileNotGzipped(emptyFile.absolutePath, "test_param")

        then:
        result == 1
    }

    // ========================================================================
    // Edge cases and integration scenarios
    // ========================================================================

    def "test_isPositiveInteger_withLargeInteger"() {
        when:
        def result = BactopiaUtils.isPositiveInteger(1000000, "test_param")

        then:
        result == 0
    }

    def "test_isPositiveInteger_withLargeIntegerString"() {
        when:
        def result = BactopiaUtils.isPositiveInteger("999999999", "test_param")

        then:
        result == 0
    }

    def "test_fileExists_withDirectory"() {
        given:
        def testDir = tempDir.resolve("testdir")
        Files.createDirectory(testDir)

        when:
        def result = BactopiaUtils.fileExists(testDir.toString())

        then:
        result == true  // Directory exists, so should return true
    }

    def "test_fileNotFound_withDirectory"() {
        given:
        def testDir = tempDir.resolve("testdir2")
        Files.createDirectory(testDir)

        when:
        def result = BactopiaUtils.fileNotFound(testDir.toString(), "test_param")

        then:
        result == 0  // Directory exists
    }

    def "test_isLocal_withWindowsStylePath"() {
        when:
        def result = BactopiaUtils.isLocal("C:\\Users\\test\\file.txt")

        then:
        result == true
    }

    def "test_fileExists_withSymlink"() {
        given:
        def targetFile = tempDir.resolve("target.txt").toFile()
        targetFile.text = "target content"
        def linkPath = tempDir.resolve("link.txt")
        
        try {
            Files.createSymbolicLink(linkPath, targetFile.toPath())
            
            when:
            def result = BactopiaUtils.fileExists(linkPath.toString())

            then:
            result == true
        } catch (UnsupportedOperationException e) {
            // Skip test if symlinks not supported on this system
            expect:
            true
        }
    }
}
