#	Europa

Europa is a new general purpose evolutionary algorithm framework designed with the following features and goals in mind:
* Very flexible, for example: 
  * able to support any population-based algorithm;
  * genotype-agnostic - genotypes may be programs (for example Genetic Programming), arbitrary sequences of vectors, bit-strings, etc.; 
  * algorithm-agnostic - can implement GP, neuro-evolution algorithms, swarm-based methods, etc.
  This is possible because Europa is built on a "configurable component" architecture. See more below.  
* Easily configurable: configuration files are in JSON format. Java's reflection mechanisms are used to configure a hierarchy of parametrised and pluggable components (see  Configurable Component Architecture below).
* Intuitive: the hierarchical organisation of parametrised components allows for an intuitive understanding of how the software is organised. Sample configuration files with parameter and sub-component descriptions and other meta-data can be automatically generated from the Class field annotations.
* Provide built-in support for multi-threaded evaluation of the members of a population.
* Provide built-in support for cluster-computing evaluation of the members of a population.
* Support generating snap-shots of a run in order to be able to resume from them later.
* Provide implementations of many standard or typical evolutionary algorithms.

Europa is currently a work in progress. If you'd like to contribute please contact me at oliver.coleman@gmail.com

## Building and Running

Europa requires Java 7 or greater. Europa is built with [gradle](http://gradle.org). A runnable JAR file can be built from the source files with:

```sh
gradle build
```

This will create a runnable jar file in a directory called europa-build in the same directory that the europa project 
directory resides in. The jar file will contain all the required dependencies.

To see usage instruction run:

```sh
java -jar europa-[version].jar
```

## Configurable Component Architecture

Every significant piece of functionality is a "component" (extending com.ojcoleman.europa.configurable.ConfigurableComponent). Components can have various kinds of parameters and sub-components. A parameter or sub-component is just a field in a Class with a custom annotation, for example:

```java
	@Parameter(description="The random seed. Default value is the system time.", optional=true)
	protected long randomSeed;
	
	@Parameter(description="The class to use to generate random numbers. It must extend Java.util.Random.", defaultValue="java.util.Random")
	protected Class<? extends Random> randomClass;
	
	
	@SubComponent (description="Sub-component for transcribing a genotype to a 'phenotype' function to be evaluated (these may be one and the same).", defaultImplementation=DummyTranscriber.class)
	protected Transcriber<Function<? ,?>> transcriber;
	
	@SubComponent (description="Sub-component for the population of individuals.", defaultImplementation=SimplePopulation.class)
	protected Population population;
```

When Europa is launched it takes the values and sub-component classes specified in the configuration file and builds - using Java's reflection mechanisms - the hierarchy of specified components with the specified parameter values.

## Configuration Files

Configuration files are in JSON format. The structure of these files reflects the organisation of the components that make up the evolutionary algorithm. Europa can print out a configuration file containing information about all the available parameters of a component and, recursively, its sub-components by supplying the --printConfig option. An example is below. Note that it includes comments, which aren't allowed in JSON but makes the meta-data associated with parameters a lot more readable (Europa can read JSON with comments, and configuration files can be printed with out meta-data comments if required). 

```
{
  // description: "The number of runs to perform."
  // type: "int"
  // defaultValue: 1
  "numRuns": 1,

  // description: "How many iterations/generations to perform before saving the Run state to a file (for later resume or examination). A value <= 0 indicates no saving."
  // type: "int"
  // defaultValue: 1000
  "saveFrequency": 1000,

  "components": {
    "run": {
      // description: "Main component for the evolutionary run."
      // optional: false
      // multiple: false
      // defaultImplementation: "com.ojcoleman.europa.core.Run"
      "componentClass": "com.ojcoleman.europa.core.Run",

      // description: "The maximum number of iterations/generations to perform. Values <= 0 indicate no limit."
      // type: "int"
      // defaultValue: 0
      "maximumIterations": 0,

      // description: "The class to use to generate random numbers. It must extend Java.util.Random."
      // type: "java.lang.Class"
      // defaultValue: "java.util.Random"
      "randomClass": "java.util.Random",

      // description: "How many threads to use to transcribe and evaluate individuals simultaneously. If <= 0 given then this defaults to the number of CPU cores."
      // type: "int"
      // defaultValue: 0
      "parallelThreads": 8,

      "components": {
        "transcriber": {
          // description: "Sub-component for transcribing a genotype to a 'phenotype' function to be evaluated (these may be one and the same)."
          // optional: false
          // multiple: false
          // defaultImplementation: "com.ojcoleman.europa.transcribers.DummyTranscriber"
          "componentClass": "com.ojcoleman.europa.transcribers.DummyTranscriber",

        },
        "evolver": {
          // description: "Sub-component for creating the initial population and new individuals from existing individuals via genetic operators."
          // optional: false
          // multiple: false
          // defaultImplementation: "com.ojcoleman.europa.evolvers.DefaultEvolver"
          "componentClass": "com.ojcoleman.europa.evolvers.DefaultEvolver",

          // description: "The proportion of parents to select from the population or each species, used to generate new genotypes."
          // type: "double"
          // defaultValue: 0.2
          "parentsProportion": 0.2,

          // description: "The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if any)."
          // type: "double"
          // defaultValue: 1
          "relativeCloneProportion": 1.0,

          // description: "The proportion of elites to select from the population or each species, elites coninue to next generation unchanged."
          // type: "double"
          // defaultValue: 0.05
          "elitismProportion": 0.05,

          // description: "The class to use for the Genotype."
          // type: "java.lang.Class"
          // defaultValue: "com.ojcoleman.europa.genotypes.DummyGenotype"
          "genotypeClass": "com.ojcoleman.europa.genotypes.DummyGenotype",

          // description: "If speciation is used, whether to use fitness sharing for a species when determining the relative number of children to produce from a species."
          // type: "boolean"
          // defaultValue: true
          "speciesFitnessSharing": true,

          "components": {
            "mutators": [
              {
                // description: "Sub-component(s) used to mutate genotypes."
                // optional: false
                // multiple: true
                // defaultImplementation: "com.ojcoleman.europa.mutators.DummyMutator"
                "componentClass": "com.ojcoleman.europa.mutators.DummyMutator",

                // description: "The percentage of children (produced by a recombiner) to apply this mutator to."
                // type: "double"
                // defaultValue: 0.25
                "applyPercentageChildren": 0.25,

                // description: "The percentage of clones to apply this mutator to."
                // type: "double"
                // defaultValue: 1
                "applyPercentageClones": 1.0,

              }
            ],
            "recombiners": [
              
            ]
          }
        },
        "speciator": {
          // description: "Sub-component for dividing the population into species."
          // optional: false
          // multiple: false
          // defaultImplementation: "com.ojcoleman.europa.speciators.NoSpeciation"
          "componentClass": "com.ojcoleman.europa.speciators.NoSpeciation",

        },
        "evaluators": [
          {
            // description: "Sub-component(s) for the fitness evaluator(s). The first evaluator is considered the Primary evaluator, which may be used by the Transcriber to obtain information about how the genotype should be constructed."
            // optional: false
            // multiple: true
            // defaultImplementation: "com.ojcoleman.europa.evaluators.DummyEvaluator"
            "componentClass": "com.ojcoleman.europa.evaluators.DummyEvaluator",

          }
        ],
        "ranker": {
          // description: "Sub-component for determining the overall relative fitness of individuals in the population."
          // optional: false
          // multiple: false
          // defaultImplementation: "com.ojcoleman.europa.rankers.DefaultRanker"
          "componentClass": "com.ojcoleman.europa.rankers.DefaultRanker",

        },
        "history": {
          // description: "Sub-component for maintaining pertinent bits of evolution history."
          // optional: false
          // multiple: false
          // defaultImplementation: "com.ojcoleman.europa.core.History"
          "componentClass": "com.ojcoleman.europa.core.History",

        },
        "population": {
          // description: "Sub-component for the population of individuals."
          // optional: false
          // multiple: false
          // defaultImplementation: "com.ojcoleman.europa.populations.SimplePopulation"
          "componentClass": "com.ojcoleman.europa.populations.SimplePopulation",

          // description: "The class to use for Individuals. Must extend com.ojcoleman.europa.core.Individual."
          // type: "java.lang.Class"
          // defaultValue: "com.ojcoleman.europa.core.Individual"
          "individualClass": "com.ojcoleman.europa.core.Individual",

          // description: "The desired population size."
          // type: "int"
          // defaultValue: 100
          "desiredSize": 100,

        }
      }
    }
  }
}
```

## License
  
Europa is licensed under the Apache 2 license, please refer to LICENSE.txt, which should accompany this file.