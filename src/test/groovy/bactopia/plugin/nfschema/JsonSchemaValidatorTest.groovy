package bactopia.plugin.nfschema

import org.json.JSONObject
import spock.lang.Specification
import bactopia.plugin.BactopiaConfig

class JsonSchemaValidatorTest extends Specification {

    JsonSchemaValidator validator

    def setup() {
        def config = new BactopiaConfig()
        validator = new JsonSchemaValidator(config)
    }

    private static String schemaWithType(String paramName, String type) {
        return """{
            "\$schema": "https://json-schema.org/draft/2020-12/schema",
            "type": "object",
            "properties": {
                "${paramName}": {
                    "type": "${type}"
                }
            }
        }"""
    }

    // -- Number/float coercion tests --

    def "float string against number schema produces no error"() {
        given: "a string value parseable as a float"
        def input = new JSONObject()
        input.put("my_float", "3.14")
        def schema = schemaWithType("my_float", "number")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because the string is a valid BigDecimal"
        result[0].isEmpty()
    }

    def "invalid string against number schema produces error"() {
        given: "a string value that is not a number"
        def input = new JSONObject()
        input.put("my_float", "not_a_number")
        def schema = schemaWithType("my_float", "number")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "an error is reported"
        result[0].size() == 1
        result[0][0].contains("Value is [string] but should be [number]")
    }

    def "negative float string against number schema produces no error"() {
        given: "a negative float string"
        def input = new JSONObject()
        input.put("my_float", "-2.5")
        def schema = schemaWithType("my_float", "number")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because the string is a valid BigDecimal"
        result[0].isEmpty()
    }

    def "integer string against number schema produces no error"() {
        given: "an integer string validated against a number schema"
        def input = new JSONObject()
        input.put("my_float", "42")
        def schema = schemaWithType("my_float", "number")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because integers are valid BigDecimals"
        result[0].isEmpty()
    }

    // -- Integer coercion tests --

    def "integer string against integer schema produces no error"() {
        given: "a string value parseable as an integer"
        def input = new JSONObject()
        input.put("my_int", "42")
        def schema = schemaWithType("my_int", "integer")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because the string is a valid BigInteger"
        result[0].isEmpty()
    }

    def "non-integer string against integer schema produces error"() {
        given: "a string value that is not an integer"
        def input = new JSONObject()
        input.put("my_int", "not_an_int")
        def schema = schemaWithType("my_int", "integer")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "an error is reported"
        result[0].size() == 1
        result[0][0].contains("Value is [string] but should be [integer]")
    }

    def "float string against integer schema produces error"() {
        given: "a float string validated against an integer schema"
        def input = new JSONObject()
        input.put("my_int", "3.14")
        def schema = schemaWithType("my_int", "integer")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "an error is reported because floats are not valid BigIntegers"
        result[0].size() == 1
        result[0][0].contains("Value is [string] but should be [integer]")
    }

    // -- Boolean coercion tests --

    def "true string against boolean schema produces no error"() {
        given: "the string 'true'"
        def input = new JSONObject()
        input.put("my_bool", "true")
        def schema = schemaWithType("my_bool", "boolean")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because 'true' is a valid boolean string"
        result[0].isEmpty()
    }

    def "mixed case false string against boolean schema produces no error"() {
        given: "the string 'False' with mixed case"
        def input = new JSONObject()
        input.put("my_bool", "False")
        def schema = schemaWithType("my_bool", "boolean")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because toLowerCase converts it to 'false'"
        result[0].isEmpty()
    }

    def "non-boolean string against boolean schema produces error"() {
        given: "a string that is not a boolean"
        def input = new JSONObject()
        input.put("my_bool", "yes")
        def schema = schemaWithType("my_bool", "boolean")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "an error is reported"
        result[0].size() == 1
        result[0][0].contains("Value is [string] but should be [boolean]")
    }

    // -- Sanity checks --

    def "string value against string schema produces no error"() {
        given: "a string value matching a string schema"
        def input = new JSONObject()
        input.put("my_string", "hello")
        def schema = schemaWithType("my_string", "string")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no type mismatch errors"
        result[0].isEmpty()
    }

    def "native number value against number schema produces no error"() {
        given: "an actual numeric value (not a string)"
        def input = new JSONObject()
        input.put("my_num", 3.14)
        def schema = schemaWithType("my_num", "number")

        when: "validating parameters"
        def result = validator.validate(input, schema)

        then: "no errors because the type matches natively"
        result[0].isEmpty()
    }
}
