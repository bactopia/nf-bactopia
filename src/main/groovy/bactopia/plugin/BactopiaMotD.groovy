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
            // Bactopia Related
            "Learn more about Bactopia at https://bactopia.github.io/",
            "Feeling stuck? Check out the docs at https://bactopia.github.io/",
            "Bactopia is a pipeline for the analysis of bacterial genomes.",
            "Bactopia was first released in February 2019.",
            "Bactopia is based on Staphopia which was specific to Staphylococcus aureus.",
            "Found a bug? Report it at https://github.com/bactopia/bactopia/issues",
            "Have a feature request? Open an issue at https://github.com/bactopia/bactopia/issues",

            // Random ATL and Wyoming facts
            "Atlanta, Georgia has the highest percentage of tree canopy of any city in the United States.",
            "There are 71 variations of 'Peachtree' streets in Atlanta, Georgia.",
            "Wyoming is the least populous state in the United States. (~600k in 2025)",
            "Wyoming has approximately 2.15 to 2.4 cows per person, the highest ratio of any U.S. state.",
            "Wyoming is home to two escalators.",

            // Jokes
            "How does a penguin build its house? ${colors.cyan}${colors.italic}Igloos it together!${colors.reset}",
            "How does a train eat? ${colors.cyan}${colors.italic}It goes chew chew!${colors.reset}",
            "Why did the scarecrow win an award? ${colors.cyan}${colors.italic}Because he was outstanding in his field!${colors.reset}",
            "Why did the bicycle fall over? ${colors.cyan}${colors.italic}Because it was two-tired!${colors.reset}",
            "Why don't scientists trust atoms? ${colors.cyan}${colors.italic}Because they make up everything!${colors.reset}",
            "Want to hear a joke about a piece of paper? ${colors.cyan}${colors.italic}Never mind, it's tearable!${colors.reset}",
            "What do you call a factory that sells passable products? ${colors.cyan}${colors.italic}A satisfactory!${colors.reset}",
            "What do you call sad coffee? ${colors.cyan}${colors.italic}Depresso!${colors.reset}",
            "Did you hear the story about the cheese that saved the world? ${colors.cyan}${colors.italic}It was legend dairy!${colors.reset}",
            "Can February march? ${colors.cyan}${colors.italic}No, but April may!${colors.reset}",
            "How do hens stay fit? ${colors.cyan}${colors.italic}They always egg-cercise!${colors.reset}",
            "How do the trees get on the internet? ${colors.cyan}${colors.italic}They log in!${colors.reset}",
            "How good are you at Power Point? ${colors.cyan}${colors.italic}I Excel at it!${colors.reset}",
            "How does a French skeleton say hello? ${colors.cyan}${colors.italic}Bone jour!${colors.reset}",
            "How does a scientist freshen their breath? ${colors.cyan}${colors.italic}With experi-mints!${colors.reset}",
            "How many bones are in the human hand? ${colors.cyan}${colors.italic}A handful of them!${colors.reset}",
            "How do you organize a space party? ${colors.cyan}${colors.italic}You planet!${colors.reset}",
            "What cheese can never be yours? ${colors.cyan}${colors.italic}Nacho cheese!${colors.reset}",
            "What creature is smarter than a talking parrot? ${colors.cyan}${colors.italic}A spelling bee!${colors.reset}",
            "What did the 0 say to the 8? ${colors.cyan}${colors.italic}Nice belt!${colors.reset}",
            "What did the Buffalo say to his little boy when he dropped him off at school? ${colors.cyan}${colors.italic}Bison!${colors.reset}",
            "What did the digital clock say to the grandfather clock? ${colors.cyan}${colors.italic}Look grandpa, no hands!${colors.reset}",
            "What did the ocean say to the shore? ${colors.cyan}${colors.italic}Nothing, it just waved!${colors.reset}",
            "What do vegetarian zombies eat? ${colors.cyan}${colors.italic}Graaaaaains!${colors.reset}",
            "What do you call a dad that has fallen through the ice? ${colors.cyan}${colors.italic}A popsicle!${colors.reset}",
            "What do you call a duck that gets all A's? ${colors.cyan}${colors.italic}A wise quacker!${colors.reset}",
            "What do you call a fake noodle? ${colors.cyan}${colors.italic}An impasta!${colors.reset}",
            "What do you call a fish with no eyes? ${colors.cyan}${colors.italic}Fsh!${colors.reset}",
            "What do you call a fly without wings? ${colors.cyan}${colors.italic}A walk!${colors.reset}",
            "What do you call a group of killer whales playing instruments? ${colors.cyan}${colors.italic}An orca-stra!${colors.reset}",
            "What do you call a nervous javelin thrower? ${colors.cyan}${colors.italic}Shakespeare!${colors.reset}",
            "What do you call a pig with three eyes? ${colors.cyan}${colors.italic}Piiig!${colors.reset}",
            "What do you call a pile of cats? ${colors.cyan}${colors.italic}A meow-tain!${colors.reset}",
            "What do you call a snowman with a six-pack? ${colors.cyan}${colors.italic}An abdominal snowman!${colors.reset}",
            "What do you call an elephant that doesn't matter? ${colors.cyan}${colors.italic}An irrelephant!${colors.reset}",
            "What do you call an old snowman? ${colors.cyan}${colors.italic}Water!${colors.reset}",
            "What does an angry pepper do? ${colors.cyan}${colors.italic}It gets jalapeño business!${colors.reset}",
            "What does a cloud wear under his raincoat? ${colors.cyan}${colors.italic}Thunderwear!${colors.reset}",
            "What is a vampire's favorite fruit? ${colors.cyan}${colors.italic}A blood orange!!${colors.reset}",
            "What is a witch's favorite subject in school? ${colors.cyan}${colors.italic}Spelling!${colors.reset}",
            "What kind of dinosaur loves to sleep? ${colors.cyan}${colors.italic}A stega-snore-us!${colors.reset}",
            "What kind of music do planets listen to? ${colors.cyan}${colors.italic}Neptunes!${colors.reset}",
            "What kind of tree fits in your hand? ${colors.cyan}${colors.italic}A palm tree!${colors.reset}",
            "What's brown and sticky? ${colors.cyan}${colors.italic}A stick!${colors.reset}",
            "What's the worst thing about ancient history class? ${colors.cyan}${colors.italic}The teachers tend to Babylon!${colors.reset}",
            "When does a joke become a 'dad' joke? ${colors.cyan}${colors.italic}When it becomes apparent!${colors.reset}",
            "Why did the computer go to the doctor? ${colors.cyan}${colors.italic}Because it had a virus!${colors.reset}",
            "Why did the cookie go to the doctor? ${colors.cyan}${colors.italic}Because it felt crumby!${colors.reset}",
            "When is a door not a door? ${colors.cyan}${colors.italic}When it's ajar!${colors.reset}",
            "Why are fish so smart? ${colors.cyan}${colors.italic}Because they live in schools!${colors.reset}",
            "Why are ghosts such bad liars? ${colors.cyan}${colors.italic}Because you can see right through them!${colors.reset}",
            "Why are elevator jokes so good? ${colors.cyan}${colors.italic}Because they work on so many levels!${colors.reset}",
            "Why are frogs so happy? ${colors.cyan}${colors.italic}Because they eat whatever bugs them!${colors.reset}",
            "Why are skeletons so calm? ${colors.cyan}${colors.italic}Because nothing gets under their skin!${colors.reset}",
            "Why did the tree go to the dentist? ${colors.cyan}${colors.italic}Because it needed a root canal!${colors.reset}",
            "Why didn't the orange win the race? ${colors.cyan}${colors.italic}Because it ran out of juice!${colors.reset}",
            "What do you call a bee that can't make up its mind? ${colors.cyan}${colors.italic}A maybee!${colors.reset}",
            "Why is seven bigger than nine? ${colors.cyan}${colors.italic}Because seven eight nine!${colors.reset}",
            "Why don't eggs tell jokes? ${colors.cyan}${colors.italic}Because they might crack up!${colors.reset}",
            "Why don't skeletons fight each other? ${colors.cyan}${colors.italic}They don't have the guts!${colors.reset}",
            "Did you hear about the Viking who was reincarnated? ${colors.cyan}${colors.italic}He was Bjorn again!${colors.reset}",
            "I was going to tell a sodium joke, ${colors.cyan}${colors.italic}then I thought, 'Na.'${colors.reset}",
            "What did the triangle say to the circle? ${colors.cyan}${colors.italic}You're so pointless!${colors.reset}",
            "What happens when a snowman throws a tantrum? ${colors.cyan}${colors.italic}He has a meltdown!${colors.reset}",
            "What do you call a bear with no teeth? ${colors.cyan}${colors.italic}A gummy bear!${colors.reset}",
            "Humpty Dumpty had a great fall. ${colors.cyan}${colors.italic}Summer wasn't too bad either!${colors.reset}",
            "There are two reasons not to drink toilet water. ${colors.cyan}${colors.italic}Number one and number two!${colors.reset}",
            "Why couldn't Pseudomonas get backstage at the concert? ${colors.cyan}${colors.italic}Because it was Staph only!${colors.reset}",
            "No matter how popular antibiotics get, ${colors.cyan}${colors.italic}they will never go viral!${colors.reset}",
            "What do call a bacterium that can swim fast? ${colors.cyan}${colors.italic}micro-Phelps!${colors.reset}",
            "How do you turn soup into gold? ${colors.cyan}${colors.italic}Add 24 carrots!${colors.reset}",
        ]
        def random = new Random()
        def index = random.nextInt(messages.size())
        return messages[index]
    }
}
