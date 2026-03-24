# Build the plugin
assemble:
	./gradlew assemble

clean:
	rm -rf .nextflow*
	rm -rf work
	rm -rf build
	./gradlew clean

# Run plugin unit tests
test:
	./gradlew test

# Install the plugin into local nextflow plugins dir
install:
	rm -rf ~/.nextflow/plugins/nf-bactopia-$(shell grep "^version" build.gradle | sed "s/version = '//;s/'//")
	./gradlew install

# Publish the plugin
release:
	./gradlew releasePlugin
