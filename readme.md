#	Europa

Europa is a new general purpose evolutionary algorithm framework designed with the following features and goals in mind:
* Very flexible, for example: 
  * able to support any population-based algorithm;
  * genotype-agnostic - genotypes may be programs (for example Genetic Programming), arbitrary sequences of vectors, bit-strings, etc.; 
  * algorithm-agnostic - can implement GP, neuro-evolution algorithms, swarm-based methods, etc.
  This is possible because Europa is built on a "configurable component" architecture. See more below.  
* Easily configurable: configuration files are in JSON format. Java's reflection mechanisms are used to configure a hierarchy of 
  parametrised and pluggable components and prototype objects (see Configurable Component Architecture below).
* Intuitive: the hierarchical organisation of parametrised components allows for an intuitive understanding of how the software is organised. 
  Sample configuration files with parameter and sub-component descriptions and other meta-data can be automatically generated from the Class field annotations.
* Provide built-in support for multi-objective evaluation.
* Provide built-in support for multi-threaded evaluation of the members of a population.
* Provide built-in support for cluster-computing evaluation of the members of a population (TODO).
* Support generating snap-shots of a run in order to be able to resume from them later.
* Provide implementations of many standard or typical evolutionary algorithms (TODO).

If you'd like to contribute please contact me at oliver.coleman@gmail.com

## Links

Manual: http://olivercoleman.github.com/europa/manual.html
API documentation: http://olivercoleman.github.com/europa/doc/index.html
GitHub repository: https://github.com/OliverColeman/europa

## Building and Running


Europa requires Java 7 or greater. Europa is built with 
[gradle](http://gradle.org). To build a runnable JAR file, run the following 
command from the project directory (it is not necessary to install gradle):
```
./gradlew withDeps
```
on *nix systems, or
```
gradlew withDeps
```
on Windows systems. This will create a runnable jar file in build/libs. If you
want a jar file that does not include the dependencies replace "withDeps" with
"assemble".


## Configurable Component Architecture

Every significant piece of functionality is a "component" (extending com.ojcoleman.europa.configurable.ComponentBase). 
A component class can define parameters, general purpose configurable objects, prototype objects and sub-components. 
These are simply fields in the class with a custom annotation, for example:

```java
	class MyComponent extends ComponentBase {
		@Parameter(description="A simple parameter.", optional=true)
		protected long randomSeed;
		
		@Parameter (description = "The class to use to generate random numbers. It must extend Java.util.Random.", defaultValue = "java.util.Random")
		private Class<? extends Random> randomClass;
		
		@Configurable (description = "Configuration for utility class to perform operations in parallel.", defaultClass = Parallel.class)
		protected Parallel parallel;
	
		@Prototype (description = "The configuration for the prototype Individual.", defaultClass = Individual.class)
		protected Individual<G, F> individualPrototype;
		
		@Component (description = "Sub-component for the population of individuals.", defaultImplementation = SimplePopulation.class)
		protected Population population;
		
		...
	}
```

When Europa is launched it uses the given configuration file or files to build the hierarchy of specified 
components with the specified parameter values, prototypes and other configurable objects. As well as specifying
the parameter values and sub-configurations for configurable objects, prototypes and components, the configuration
file may also optionally specify custom classes to use for each of these (the only constraint being that the specified
classes must be a sub-class of the default class. Thus alternate implementations of just about any part of the software 
can easily be deployed.

All this is achieved via Java's reflection API. However extreme care is taken to make this very efficient and have negligible
impact on performance once the system is initialised.

## Configuration Files

Configuration files are in JSON format. The structure of these files reflects the organisation of the components that make up the evolutionary algorithm. 
The descriptions and other meta-data provided in the annotations described above may be used to produce template configuration files in a self-documenting manner.
To print out a template or documentation configuration file, specify the --printConfig option on the command line. An abridged example is below. 
Note that it includes comments, which aren't allowed in JSON but makes the meta-data associated with parameters a lot more readable 
(Europa can read JSON with comments, and configuration files can be printed with out meta-data comments). 

```
{
  // description: The name of the run. Default is the name of the configuration file appended with
  //            : the current date and time.
  // type: java.lang.String
  // optional: true
  "name": "neat",

  // description: The maximum number of iterations/generations to perform. Values <= 0 indicate no
  //            : limit.
  // type: int
  // optional: false
  // defaultValue: 0
  "maximumIterations": 101,

  // description: Component for transcribing a genotype to a 'phenotype' function to be evaluated
  //            : (these may be one and the same).
  // optional: false
  // multiple: false
  // defaultImplementation: com.ojcoleman.europa.core.DummyTranscriber
  // available implementations: 
  // :  com.ojcoleman.europa.core.DummyTranscriber
  // :  com.ojcoleman.europa.algos.neat.NEATTranscriber
  "transcriber": {
    // class: The class to use for 'transcriber', if defaultImplementation is not defined or a custom class is required.
    "class": "com.ojcoleman.europa.algos.neat.NEATTranscriber",

    // description: The prototype configuration for the genotype.
    // base class: com.ojcoleman.europa.algos.neat.NEATGenotype
    "genotype": {
      // class: The class to use for 'genotype', if a class other than the base class is required.
      "class": "com.ojcoleman.europa.algos.neat.NEATGenotype",

      // description: The prototype configuration for the NEATGene (used only for the "type" system,
      //            : see paramsType in the neuron and synapse configs). This is only useful if a
      //            : custom sub-class of NEATGene is to be used.
      // base class: com.ojcoleman.europa.algos.neat.NEATGene
      // available implementations: 
      // :  com.ojcoleman.europa.algos.neat.NEATNeuronGene
      // :  com.ojcoleman.europa.algos.neat.NEATSynapseGene
      "genePrototype": {
        // class: The class to use for 'genePrototype', if a class other than the base class is required.
        "class": "com.ojcoleman.europa.algos.neat.NEATGene",

      },

      // description: The prototype configuration for the NEATAllele (used only for the "type" system,
      //            : see paramsType in the neuron and synapse configs). This is only useful if a
      //            : custom sub-class of NEATAllele is to be used.
      // base class: com.ojcoleman.europa.algos.neat.NEATAllele
      // available implementations: 
      // :  com.ojcoleman.europa.algos.neat.NEATNeuronAllele
      // :  com.ojcoleman.europa.algos.neat.NEATSynapseAllele
      "allelePrototype": {
        // class: The class to use for 'allelePrototype', if a class other than the base class is required.
        "class": "com.ojcoleman.europa.algos.neat.NEATAllele",

      },

      ...
    },

    // description: Component for the population of individuals.
    // optional: false
    // multiple: false
    // defaultImplementation: com.ojcoleman.europa.populations.SimplePopulation
    // available implementations: 
    // :  com.ojcoleman.europa.populations.SimplePopulation
    "population": {
      // class: The class to use for 'population', if defaultImplementation is not defined or a custom class is required.
      "class": "com.ojcoleman.europa.populations.SimplePopulation",

      // description: The desired population size.
      // type: int
      // optional: false
      // defaultValue: 100
      "desiredSize": 150,

      // description: The configuration for the prototype Individual.
      // base class: com.ojcoleman.europa.core.Individual
      "individualPrototype": {
        // class: The class to use for 'individualPrototype', if a class other than the base class is required.
        "class": "com.ojcoleman.europa.core.Individual",

      },

      // description: Component for creating the initial population and new individuals from existing
      //            : individuals via genetic operators.
      // optional: false
      // multiple: false
      // defaultImplementation: com.ojcoleman.europa.core.DefaultEvolver
      // available implementations: 
      // :  com.ojcoleman.europa.core.DefaultEvolver
      // :  com.ojcoleman.europa.algos.neat.NEATEvolver
      "evolver": {
        // class: The class to use for 'evolver', if defaultImplementation is not defined or a custom class is required.
        "class": "com.ojcoleman.europa.algos.neat.NEATEvolver",

        // description: The relative proportion of children to produce by cloning (relative to the
        //            : proportions set for recombiners, if any).
        // type: double
        // optional: false
        // defaultValue: 1
        "relativeCloneProportion": 0.25,

        ...

        // description: The configuration for the prototype Genotype.
        // base class: com.ojcoleman.europa.algos.neat.NEATGenotype
        "genotypePrototype": {
          // class: The class to use for 'genotypePrototype', if a class other than the base class is required.
          "class": "com.ojcoleman.europa.algos.neat.NEATGenotype",

          // description: The prototype configuration for the NEATGene (used only for the "type" system,
          //            : see paramsType in the neuron and synapse configs). This is only useful if a
          //            : custom sub-class of NEATGene is to be used.
          // base class: com.ojcoleman.europa.algos.neat.NEATGene
          // available implementations: 
          // :  com.ojcoleman.europa.algos.neat.NEATNeuronGene
          // :  com.ojcoleman.europa.algos.neat.NEATSynapseGene
          "genePrototype": {
            // class: The class to use for 'genePrototype', if a class other than the base class is required.
            "class": "com.ojcoleman.europa.algos.neat.NEATGene",

          },

          // description: The prototype configuration for the NEATAllele (used only for the "type" system,
          //            : see paramsType in the neuron and synapse configs). This is only useful if a
          //            : custom sub-class of NEATAllele is to be used.
          // base class: com.ojcoleman.europa.algos.neat.NEATAllele
          // available implementations: 
          // :  com.ojcoleman.europa.algos.neat.NEATNeuronAllele
          // :  com.ojcoleman.europa.algos.neat.NEATSynapseAllele
          "allelePrototype": {
            // class: The class to use for 'allelePrototype', if a class other than the base class is required.
            "class": "com.ojcoleman.europa.algos.neat.NEATAllele",

          },

          ...
        },

        // description: Component(s) used to mutate genotypes.
        // optional: false
        // multiple: true
        // defaultImplementation: com.ojcoleman.europa.core.DummyMutator
        // available implementations: 
        // :  com.ojcoleman.europa.algos.neat.NEATNeuronAddMutator
        // :  com.ojcoleman.europa.algos.vector.VectorMutator
        // :  com.ojcoleman.europa.core.DummyMutator
        // :  com.ojcoleman.europa.algos.neat.NEATSynapseAddMutator
        "mutators": [
          {
            // class: The class to use for 'mutators', if defaultImplementation is not defined or a custom class is required.
            "class": "com.ojcoleman.europa.algos.vector.VectorMutator",

            // description: The percentage of clones to apply this mutator to.
            // type: double
            // optional: false
            // defaultValue: 1
            "applyPercentageClones": 1.0,
            ...

          },
          
          ...
        ],

        // description: Component(s) used to recombine genotypes to produce child genotypes.
        // optional: true
        // multiple: true
        // available implementations: 
        // :  com.ojcoleman.europa.algos.neat.NEATRecombiner
        "recombiners": [
          {
            // class: The class to use for 'recombiners', if defaultImplementation is not defined or a custom class is required.
            "class": "com.ojcoleman.europa.algos.neat.NEATRecombiner",

            // description: If set to true then the children produced by this recombiner will not be
            //            : mutated.
            // type: boolean
            // optional: false
            // defaultValue: false
            "vetoMutation": false,

            // description: The relative proportion of children to produce with this recombiner.
            // type: double
            // optional: false
            // defaultValue: 1
            "relativeProportion": 0.75,

          }
        ],

      },

      ...
    },

  },
  ...
}
```

## License
  
Europa is licensed under the GNU General Public License v3. A copy of the license
is included in the distribution. Please note that Europa is distributed WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. Please refer to the license for details.
