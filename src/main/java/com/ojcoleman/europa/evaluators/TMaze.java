package com.ojcoleman.europa.evaluators;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Observer;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.util.IntervalDouble;
import com.ojcoleman.europa.util.NiceWriter;
import com.ojcoleman.europa.util.Stringer;

/**
 * <p>
 * Implements the T-maze navigation task, for example see: Sebastian Risi and Kenneth O. Stanley (2010) Indirectly
 * Encoding Neural Plasticity as a Pattern of Local Rules. In: Proceedings of the 11th International Conference on
 * Simulation of Adaptive Behavior (SAB 2010).
 * </p>
 * <p>
 * The input and output formats can be set to match the various schemes used by previous authors, and either a single or
 * double T-maze can be specified.
 * </p>
 */
public class TMaze extends VectorFunctionEvaluator implements Observer {
	private static final long serialVersionUID = 1L;

	@Parameter (description="The total number of trials to evaluate an agent over.", defaultValue="100")
	protected int trialCount;
	
	@Parameter (description="The total number of times to move the high value reward to a different maze end location during an evaluation.", defaultValue="3")
	protected int rewardSwitchCount;
	
	@Parameter (description="The variation in switch times (fraction of number of trials between switching).", defaultValue="0.2")
	protected double rewardSwitchVariation;
	
	@Parameter (description="Reward value of low reward (used in fitness calculation).", defaultValue="0.1")
	protected double rewardLow;
	
	@Parameter (description="Reward value of high reward (used in fitness calculation).", defaultValue="1")
	protected double rewardHigh;
	
	@Parameter (description="\"Colour\" of low reward (reward signal input to agent).", defaultValue="0.2")
	protected double rewardLowColour;
	
	@Parameter (description="\"Colour\" of high reward (reward signal input to agent).", defaultValue="1")
	protected double rewardHighColour;
	
	@Parameter (description="Reward value given upon crashing into a wall (used in fitness calculation).", defaultValue="-0.4")
	protected double rewardCrash;
	
	@Parameter (description="Reward value given upon failing to return home (used in fitness calculation). If this is set to 0 then the agent is not required to return home.", defaultValue="-0.3")
	protected double rewardFailReturnHome;
	
	@Parameter (description="Length of passages of maze.", defaultValue="3")
	protected int passageLength;
	
	@Parameter (description="Set to \"true\" to specify a double T-maze.", defaultValue="false")
	protected boolean isDouble;
	
	@Parameter (description="Set to \"RANGE\" to use range-finder type inputs indicating if walls are present to the left, right and forward, and a reward input. Set to \"FEATURES\" to use inputs that indicate the following conditions: turn required; maze end reached; home position reached; reward.", defaultValue="FEATURES")
	protected InputType inputType;
	
	@Parameter (description="Set to \"SINGLE\" to use a single output to indicate the action to take next. Set to \"MULTIPLE\" to use three outputs to indicate which action to take next.", defaultValue="SINGLE")
	protected OutputType outputType;
	
	
	private enum InputType {
		RANGE, FEATURES
	};
	
	
	private enum OutputType {
		SINGLE, MULTIPLE
	};
	
	
	private int[] rewardSwitchTrials, rewardIndexForSwitch;
	private boolean[][] map; // The map of the maze, true indicates passage, false indicates walls.
	private int startX, startY; // Initial location of agent in map.
	private int[] rewardLocationsX, rewardLocationsY;
	private double minPossibleReward;
	private double possibleRewardRange;
	private boolean terminate = false;
	
	private Run run;
	private DecimalFormat logNumberFormat;
	
	private EvaluationDescription fitnessEvalDesc;
	private EvaluationDescription rewardEvalDesc;
	private EvaluationDescription performanceEvalDesc;
	
	
	public TMaze(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		run = this.getParentComponent(Run.class);
		logNumberFormat = this.getParentComponent(Run.class).getDefaultNumberFormat();

		float switchTrials = (float) trialCount / (rewardSwitchCount + 1);
		int randomRange = (int) Math.round(switchTrials * rewardSwitchVariation);

		// Set-up the map.
		if (!isDouble) {
			map = new boolean[7 + passageLength * 2][6 + passageLength];
			// Create passage starting from bottom of T.
			int x = 3 + passageLength;
			int y;
			for (y = 2; y <= 3 + passageLength; y++) {
				map[x][y] = true;
			}
			// Create passage starting from left arm to right arm.
			y = 3 + passageLength;
			for (x = 2; x <= 4 + passageLength * 2; x++) {
				map[x][y] = true;
			}

			rewardLocationsX = new int[] { 2, 4 + passageLength * 2 };
			rewardLocationsY = new int[] { y, y };
		} else {
			int extent = 7 + passageLength * 2;
			map = new boolean[extent][extent];
			// Create passage starting from bottom of T.
			int x = 3 + passageLength;
			int y;
			for (y = 2; y <= 3 + passageLength; y++) {
				map[x][y] = true;
			}
			// Create passage starting from left arm to right arm.
			y = 3 + passageLength;
			for (x = 2; x <= 4 + passageLength * 2; x++) {
				map[x][y] = true;
			}
			// Create final passages at ends of first T.
			for (y = 2; y <= 4 + passageLength * 2; y++) {
				map[2][y] = true;
				map[4 + passageLength * 2][y] = true;
			}

			rewardLocationsX = new int[] { 2, 2, extent - 3, extent - 3 };
			rewardLocationsY = new int[] { 2, extent - 3, 2, extent - 3 };
		}

		/*
		 * for (int y = 0; y < map[0].length; y++) { for (int x = 0; x < map.length; x++) { System.out.print(map[x][y] +
		 * " "); } System.out.println(); }
		 */

		// Agent starting location.
		startX = 3 + passageLength;
		startY = 2;
		
		minPossibleReward = Math.min(Math.min(rewardCrash, rewardFailReturnHome), 0);
		possibleRewardRange = rewardHigh - minPossibleReward;
		
		fitnessEvalDesc = new EvaluationDescription(getName() + " fitness", this, IntervalDouble.UNIT, 1, false);
		rewardEvalDesc = new EvaluationDescription(getName() + " reward average ", this, new IntervalDouble(minPossibleReward, rewardHigh), rewardHigh, true);
		performanceEvalDesc = new EvaluationDescription(getName() + " performance", this, IntervalDouble.UNIT, 1, true);
		
		run.addEventListener(this);
	}
	
	
	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (event == Run.Event.IterationBegin) {
			// Set-up when reward switches should occur for this set of trials.
			rewardSwitchTrials = new int[rewardSwitchCount];
			List<Integer> rewardIndexForSwitchList = new ArrayList<Integer>(rewardSwitchCount + 1);
			float switchTrials = (float) trialCount / (rewardSwitchCount + 1);
			int randomRange = (int) Math.round(switchTrials * rewardSwitchVariation);
			rewardIndexForSwitchList.add(0);
			for (int i = 0; i < rewardSwitchCount; i++) {
				rewardSwitchTrials[i] = Math.round(switchTrials * (i + 1));
				if (randomRange > 0) {
					rewardSwitchTrials[i] += run.random.nextInt(randomRange + 1) * 2 - randomRange;
				}
				rewardIndexForSwitchList.add((i + 1) % rewardLocationsX.length);
			}
			if (rewardSwitchVariation > 0 && isDouble) {
				Collections.shuffle(rewardIndexForSwitchList, run.random);
			}
			rewardIndexForSwitch = new int[rewardSwitchCount + 1];
			for (int i = 0; i < rewardSwitchCount + 1; i++) {
				rewardIndexForSwitch[i] = rewardIndexForSwitchList.get(i);
			}
		}
	}
	
	
	@Override
	public Set<EvaluationDescription> getEvaluationDescriptions() {
		Set<EvaluationDescription> evalDescs = new HashSet<>();
		evalDescs.add(fitnessEvalDesc);
		evalDescs.add(rewardEvalDesc);
		evalDescs.add(performanceEvalDesc);
		return evalDescs;
	}
	
	
	@Override
	public void evaluate(Individual individual, Log log) {
		try {
			NiceWriter logOutput = null;
			if (log.specifiesItem("string")) {
				StringWriter sw = new StringWriter();
				logOutput = new NiceWriter(sw, logNumberFormat);
				log.setLog("string", sw);
			}
			StringBuilder logSummary = logOutput == null ? null : new StringBuilder();
			
			if (logOutput != null) {
				logOutput.put("Map:\n");
				logOutput.put(map).put("\n");
			}
			
			VectorFunction substrate = (VectorFunction) individual.getFunction();
			
			int rewardSwitchTrialsIndex = 0;
			int rewardHighIndex = rewardIndexForSwitch[0];
			double[] input = new double[4];
			int[] walls = new int[4];
			double reward = 0;
			int correctTrialCount = 0, highRewardCount = 0, lowRewardCount = 0, crashCount = 0, failReturnHomeCount = 0;
			int maxSteps = isDouble ? passageLength * 6 + 12 : passageLength * 4 + 8;
			if (rewardFailReturnHome == 0) { // If returning home is not required.
				maxSteps = isDouble ? passageLength * 3 + 5 : passageLength * 2 + 3;
			}
			maxSteps += 2;
			
			for (int trial = 0; trial < trialCount; trial++) {
				if (logOutput != null) {
					logOutput.put("\n=== BEGIN TRIAL " + trial + "===\n");
					logSummary.append("Trial " + trial + "\n");
				}
	
				// If we should switch reward locations now.
				if (rewardSwitchTrialsIndex < rewardSwitchTrials.length && trial == rewardSwitchTrials[rewardSwitchTrialsIndex]) {
					rewardSwitchTrialsIndex++;
					rewardHighIndex = rewardIndexForSwitch[rewardSwitchTrialsIndex];
				}
				if (logOutput != null)
					logOutput.put("Reward is at " + rewardLocationsX[rewardHighIndex] + ", " + rewardLocationsY[rewardHighIndex] + "\n");
	
				int agentX = startX;
				int agentY = startY;
				int direction = 0; // 0 = up, 1 = right, 2 = down, 3 = left.
				boolean collectedReward = false, collectedHighReward = false;
				boolean finished = false;
				int step = 0;
				double trialReward = 0;
				int action = 0;
				
				int previousAction = -1;
				int samePositionCount = 0;
	
				while (!finished) {
					Arrays.fill(input, 0);
					if (!collectedReward && atReward(agentX, agentY)) {
						if (agentX == rewardLocationsX[rewardHighIndex] && agentY == rewardLocationsY[rewardHighIndex]) {
							trialReward += rewardHigh;
							input[3] = rewardHighColour;
							collectedHighReward = true;
							highRewardCount++;
							if (logSummary != null) logSummary.append("\tHigh reward collected.\n");
						} else {
							trialReward += rewardLow;
							input[3] = rewardLowColour;
							lowRewardCount++;
							if (logSummary != null) logSummary.append("\tLow reward collected.\n");
						}
						collectedReward = true;
						if (rewardFailReturnHome == 0) { // If returning home is not required.
							finished = true;
							if (rewardLowColour == rewardHighColour || collectedHighReward)
								correctTrialCount++;
						}
					} else if (collectedReward && agentX == startX && agentY == startY) {
						// If collected reward and returned home.
						finished = true;
						if (rewardLowColour == rewardHighColour || collectedHighReward)
							correctTrialCount++;
						if (logSummary != null) logSummary.append("\tReturned home.\n");
					} else if (!map[agentX][agentY] || samePositionCount == 4) { // If it's hit a wall or is spinning on the spot.
						trialReward += rewardCrash;
						finished = true;
						crashCount++;
						if (logSummary != null) logSummary.append("\tCrashed.\n");
					} else if (step >= maxSteps) { // If it's taken too long then it's taken a wrong turn.
						trialReward += rewardFailReturnHome;
						finished = true;
						failReturnHomeCount++;
						if (logSummary != null) logSummary.append("\tFailed to return home, or took too long.\n");
					}
	
					// Detect walls in each direction. 1 indicates a wall, 0 passage.
					walls[0] = map[agentX - 1][agentY] ? 0 : 1; // left
					walls[1] = map[agentX][agentY + 1] ? 0 : 1; // up
					walls[2] = map[agentX + 1][agentY] ? 0 : 1; // right
					walls[3] = map[agentX][agentY - 1] ? 0 : 1; // down
	
					// If input type is range sensor.
					if (inputType == InputType.RANGE) {
						for (int i = 0; i < 3; i++)
							input[i] = walls[(direction + i) % 4];
					} 
					else { // input type is maze features.
						// Turn point (only one wall in any direction).
						if (ArrayUtil.sum(walls) == 1)
							input[0] = 1;
						// Maze end.
						else if (atReward(agentX, agentY))
							input[1] = 1;
						// Home (check collectedReward instead of step > 0?).
						else if (step > 0 && agentX == startX && agentY == startY)
							input[2] = 1;
					}
	
					// Ask the network what it wants to do next (and/or allow the agent to update itself given the reward
					// received).
					double[] output = substrate.apply(input);
	
					// Determine action to perform.
					if (outputType == OutputType.MULTIPLE) {
						// The action to perform is the one corresponding to the output with the highest output value.
						action = ArrayUtil.getMaxIndex(output);
					} else {
						// The action to perform depends on the value of the single output.
						double o = output[0];
						if (substrate.getMinimumOutputValue() >= 0) { // if output function doesn't do negative values.
							o = (o * 2) - 1; // scale to [-1, 1]
						}
						action = (o < -0.3) ? 0 : ((o > 0.3) ? 2 : 1);
					}
	
					if (logOutput != null) {
						logOutput.put("Agent is at " + agentX + ", " + agentY + "\n");
						logOutput.put("\tInput: " + ArrayUtil.toString(input, ", ", logNumberFormat) + "\n");
						logOutput.put("\tOutput: " + ArrayUtil.toString(output, ", ", logNumberFormat) + "\n");
						logOutput.put("\tAction: " + action + "\n");
						logOutput.put("\tCurrent reward: " + trialReward + "  " + (finished ? "finished" : "") + "\n");
					}
	
					if (action == 1) { // Forward movement.
						if (direction == 0) { // Up
							agentY += 1;
						} else if (direction == 1) { // Right
							agentX += 1;
						} else if (direction == 2) { // Down
							agentY -= 1;
						} else if (direction == 3) { // Left
							agentX -= 1;
						}

						// Reset the number of times it's stayed in the same position.
						samePositionCount = 0;
					} else { // Turning.
						if (action == 0) { // Left turn.
							direction = direction == 0 ? 3 : direction - 1;
						} else { // Action == 2, right turn.
							direction = direction == 3 ? 0 : direction + 1;
						}
						
						// Record the number of times it's stayed in the same position.
						samePositionCount++;
					}
					
					previousAction = action;
					
					step++;
				}
				
				// If the agent crashed, but still managed to move, then reward it for moving.
				if (trialReward == rewardCrash && (agentX != startX || agentY != startY)) {
					trialReward += Math.abs(rewardCrash) / 2;
				}
				
				reward += trialReward;
			}
			
			// Get average reward received.
			reward /= trialCount;
			
			double fitness = (reward - minPossibleReward) / possibleRewardRange;
			// Allow for numerical imprecision.
			if (fitness < 0 && fitness > -0.000001) {
				fitness = 0;
			}
			// Highest performance is reached when all trials are correct except those where the reward was moved and the agent had to explore all possible options.
			double performance = (double) correctTrialCount / (trialCount - ((rewardSwitchCount+1) * (isDouble ? 3 : 1)));
			if (performance > 1) performance = 1; // Sometimes there's a lucky one.
			
			if (logOutput != null) {
				logOutput.put("\n=== Summary ===\n" + logSummary);
				logOutput.put("\n=== Stats ===\nfitness: " + fitness + "\nperformance: " + performance + "\nhigh reward count: " + highRewardCount + "\nlow reward count: " + lowRewardCount + "\ncrash count: " + crashCount + "\nfail return home (or took too long) count: " + failReturnHomeCount + "\n");
				logOutput.close();
			}

			individual.evaluationData.setResult(rewardEvalDesc, reward);
			individual.evaluationData.setResult(fitnessEvalDesc, fitness);
			individual.evaluationData.setResult(performanceEvalDesc, performance);

			if (performance >= 1) {
				terminate = true;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private boolean atReward(int agentX, int agentY) {
		for (int i = 0; i < rewardLocationsX.length; i++) {
			if (agentX == rewardLocationsX[i] && agentY == rewardLocationsY[i])
				return true;
		}
		return false;
	}
	
	
	@Override
	public int getVectorFunctionInputSize() {
		return 4;
	}
	
	
	@Override
	public int getVectorFunctionOutputSize() {
		if (outputType == OutputType.SINGLE)
			return 1;
		return 3; // Action to perform next (left, forward, right).
	}
	
	
	@Override
	public boolean shouldTerminate() {
		return terminate;
	}
}
