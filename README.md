# One-Dimensional Bin Packing Algorithms

This repository contains the implementation of various algorithms designed to tackle the One-Dimensional Bin Packing Problem (1DBPP). The primary goal is to fit items of varying sizes into the minimal number of bins. The algorithms included are:

1. Genetic Algorithm (GA)
2. Ant Colony Optimization (ACO)
3. Cuckoo Search Genetic Algorithm (CSGA)
4. Hybrid Firefly Algorithm (HFA)

## Overview

### One-Dimensional Bin Packing Problem

The Bin Packing Problem (BPP) is a classic combinatorial optimization problem categorized under the NP-hard complexity class. It involves packing a set of items of different sizes into a finite number of bins of fixed capacity, aiming to minimize the number of bins used.

### Algorithms

#### Genetic Algorithm (GA)
A population-based metaheuristic that simulates the process of natural evolution. It uses selection, crossover, and mutation to generate solutions that converge towards an optimal or near-optimal solution over successive generations.

#### Ant Colony Optimization (ACO)
Inspired by the foraging behavior of real ants, ACO uses a metaheuristic approach to solve complex optimization problems by mimicking how ants find the shortest path between food sources and their colony.

#### Cuckoo Search Genetic Algorithm (CSGA)
Combines elements of cuckoo search and genetic algorithms to optimize solutions. This hybrid approach leverages the strengths of both metaheuristics to enhance efficiency and convergence.

#### Hybrid Firefly Algorithm (HFA)
Incorporates mutation mechanisms to enhance population diversity and prevent stagnation. Fireflies move towards brighter (better) solutions, guided by an attractiveness factor.

## Files

- `HybridFireflyAlgorithm.java`: Implementation of the Hybrid Firefly Algorithm.
- `GeneticAlgorithm.java`: Implementation of the Genetic Algorithm.
- `AntColonyOptimization.java`: Implementation of the Ant Colony Optimization.
- `CuckooSearchGeneticAlgorithm.java`: Implementation of the Cuckoo Search Genetic Algorithm.
- `Bin.java`: Represents the bin structure used in the algorithms.
- `Item.java`: Represents the item structure used in the algorithms.

## Installation

To use these algorithms, clone the repository and compile the Java files using your preferred Java IDE or command line.

```bash
git clone https://github.com/yourusername/OneDimensionalBinPacking.git
cd OneDimensionalBinPacking 
```

## Results

The algorithms are evaluated based on their solution optimality, computational efficiency, and convergence traits. The repository includes test cases and benchmarks to demonstrate the performance of each algorithm.

## Contributing

Contributions are welcome! If you have improvements or additional algorithms to add, please fork the repository and submit a pull request.

For a detailed review and analysis, refer to the included [Research Paper](Research%20Paper.pdf).

