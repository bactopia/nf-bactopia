package bactopia.plugin

import groovy.util.logging.Slf4j

import static bactopia.plugin.BactopiaTemplate.getLogColors

@Slf4j
class BactopiaMotD {

    //
    // Print a Message of the Day (MotD) to the console
    //
    public static String getMotD(Boolean monochrome_logs) {
        Map colors = getLogColors(monochrome_logs)
        def messages = [
            "Learn more about Bactopia at https://bactopia.github.io/",
            "Feeling stuck? Check out the docs at https://bactopia.github.io/",
            "Bactopia is a pipeline for the analysis of bacterial genomes.",
            "Bactopia was first released in Feburary 2019.",
            "Bactopia is based on Staphopia which was specific to Staphylococcus aureus.",
            "Found a bug? Report it at https://github.com/bactopia/bactopia/issues",
            "Have a feature request? Open an issue at https://github.com/bactopia/bactopia/issues",
            // Jokes
            "How does a penguin build its house? ${colors.blue}Igloos it together!${colors.reset}",
        ]
        def random = new Random()
        def index = random.nextInt(messages.size())
        return messages[index]
    }
}
