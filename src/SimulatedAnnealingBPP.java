import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulatedAnnealingBPP {
    private Problem problem;
    private double initialTemperature;
    private double coolingRate;
    private Random random = new Random();

    public SimulatedAnnealingBPP(Problem problem, double initialTemperature, double coolingRate) {
        this.problem = problem;
        this.initialTemperature = initialTemperature;
        this.coolingRate = coolingRate;
    }

    public List<List<Integer>> run() {
        List<List<Integer>> currentSolution = generateInitialSolution();
        List<List<Integer>> bestSolution = new ArrayList<>(currentSolution);

        double temperature = initialTemperature;

        while (temperature > 1) {
            List<List<Integer>> neighborSolution = generateNeighborSolution(currentSolution);
            double currentCost = calculateCost(currentSolution);
            double neighborCost = calculateCost(neighborSolution);

            if (acceptanceProbability(currentCost, neighborCost, temperature) > random.nextDouble()) {
                currentSolution = new ArrayList<>(neighborSolution);
            }

            if (neighborCost < calculateCost(bestSolution)) {
                bestSolution = new ArrayList<>(neighborSolution);
            }

            temperature *= coolingRate;
        }

        return bestSolution;
    }

    private List<List<Integer>> generateInitialSolution() {
        List<List<Integer>> bins = new ArrayList<>();
        for (Integer item : problem.items) {
            boolean placed = false;
            for (List<Integer> bin : bins) {
                int binSum = bin.stream().mapToInt(Integer::intValue).sum();
                if (binSum + item <= problem.binCapacity) {
                    bin.add(item);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Integer> newBin = new ArrayList<>();
                newBin.add(item);
                bins.add(newBin);
            }
        }
        return bins;
    }

    private List<List<Integer>> generateNeighborSolution(List<List<Integer>> solution) {
        List<List<Integer>> neighborSolution = new ArrayList<>(solution);
        int randomItemIndex = random.nextInt(problem.items.size());
        Integer item = problem.items.get(randomItemIndex);
        List<Integer> bin = neighborSolution.get(random.nextInt(neighborSolution.size()));
        bin.remove(item);

        // Move the item to another bin or create a new bin
        if (random.nextDouble() > 0.5 || bin.isEmpty()) {
            List<Integer> newBin = new ArrayList<>();
            newBin.add(item);
            neighborSolution.add(newBin);
        } else {
            neighborSolution.get(random.nextInt(neighborSolution.size())).add(item);
        }

        return neighborSolution;
    }

    private double acceptanceProbability(double currentCost, double neighborCost, double temperature) {
        if (neighborCost < currentCost) {
            return 1.0;
        }
        return Math.exp((currentCost - neighborCost) / temperature);
    }

    private double calculateCost(List<List<Integer>> solution) {
        return solution.size();
    }
}
