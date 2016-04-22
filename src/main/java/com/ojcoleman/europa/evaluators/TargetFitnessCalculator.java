package com.ojcoleman.europa.evaluators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ojcoleman.europa.algos.vector.VectorFunctionEvaluator;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.evaluators.TargetFitnessCalculator.ErrorType;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.util.IntervalDouble;
import com.ojcoleman.europa.util.NiceWriter;
import com.ojcoleman.europa.util.Stringer;

/**
 * <p>
 * Utility class to perform target fitness function calculations.
 * </p>
 * 
 * @author O. J. Coleman
 */
public abstract class TargetFitnessCalculator extends VectorFunctionEvaluator {
	private static Logger logger = LoggerFactory.getLogger(TargetFitnessCalculator.class);
	
	@Parameter(description="The type of error calculation to perform over the errors (as calculated by errorTypeOutput) for all examples. Valid values are "
			+ "'SAE' (Sum of Absolute Errors), 'SSAE' (Squared Sum of Absolute Errors), 'SSE' (Sum of Squared Errors), 'RSSE' (Root of Sum of Squared Errors), "
			+ "'MAE' (Mean of Absolute Errors), 'SMAE' (Squared Mean of Absolute Errors), 'MSE' (Mean of Squared Errors), 'RMSE' (Root of Mean of Squared Errors). The default is RMSE.",
			defaultValue="RMSE")
	protected ErrorType errorTypeExample;
	
	@Parameter(description="The type of error calculation to perform over the error of each output within a single example. Any type listed for errorTypeExample may be used. Default is SAE.", defaultValue="SAE")
	protected ErrorType errorTypeOutput;
	
	@Parameter(description="The method for calculating a fitness value from the error value. Valid types are "
			+ "'proportional': the fitness is calculated as a proportion of the maximum possible error value; "
			+ "'inverse': the fitness is calculated as the inverse of the error. The default is proportional.",
			defaultValue="proportional")
	protected String fitnessConversionType;
	
	@Parameter(description="The method used to calculate the performance. Valid types are: "
			+ "'proportional': the performance is calculated as a proportion of the maximum possible error; "
			+ "'percent-correct': the performance is calculated as the percentage of correct examples. Default is proportional.",
			defaultValue="proportional")
	protected String performanceMetric;
	
	@Parameter(description="The absolute value that an output can differ from the target output and still be considered correct (i.e. the acceptable error margin). "
			+ "Used for calculating the percentage of examples for which the correct output was given. Default is 0.1.", defaultValue="0.1", minimumValue="0")
	protected double acceptableError;

	@Parameter(description="Terminate when the performance reaches this value. 0 to disable.", defaultValue="0", minimumValue="0", maximumValue="1")
	protected double terminateOnPerformance;
	
	
	private static boolean outputRangeChecked = false;
	
	private EvaluationDescription fitnessEvalDesc;
	private EvaluationDescription performanceEvalDesc;
	
	private boolean terminate = false;
	
	public TargetFitnessCalculator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		if (errorTypeOutput.rootTotalError() && errorTypeExample.squareErrors()) {
			logger.warn("It doesn't make sense to take the square root of the total error over all outputs for a single example (errorTypeOutput=" + errorTypeOutput + ") and then square the error for each example in calculating the error over all examples (errorTypeExample=" + errorTypeExample + ").");
		}
		
		if (!fitnessConversionType.equals("proportional") && !fitnessConversionType.equals("inverse")) {
			throw new IllegalArgumentException("fitnessConversionType must be one of \"proportional\" or \"inverse\".");
		}
		
		if (!performanceMetric.equals("proportional") && !performanceMetric.equals("percent-correct")) {
			throw new IllegalArgumentException("performanceMetric property must be one of \"proportional\" or \"percent-correct\".");
		}
		
		fitnessEvalDesc = new EvaluationDescription(getName()+" fitness", this, IntervalDouble.UNIT, 1, false);
		performanceEvalDesc = new EvaluationDescription(getName()+" performance", this, IntervalDouble.UNIT, 1, true);
		
		this.getParentComponent(Run.class).monitor(this);
	}
	
	
	@Override
	public Set<EvaluationDescription> getEvaluationDescriptions() {
		Set<EvaluationDescription> evalDescs = new HashSet<>();
		evalDescs.add(fitnessEvalDesc);
		evalDescs.add(performanceEvalDesc);
		return evalDescs;
	}
	
	
	/**
	 * Evaluate the given function on the given input and target output pairs. Subclasses should call this from {@link Evaluator#evaluate(Individual)}.
	 * 
	 * @param Individual The individual to evaluate.
	 * @param input Array containing input examples, in the form [example][input].
	 * @param targetOutput Array containing output examples, in the form [example][output].
	 * @param minTargetOutputValue The smallest value that occurs in the target outputs.
	 * @param maxTargetOutputValue The largest value that occurs in the target outputs.
	 * @param logOutput If not null then for each pattern the input, target and output will be written to this.
	 */
	public void evaluate(Individual<?, VectorFunction> individual, double[][] input, double[][] targetOutput, double minTargetOutputValue, double maxTargetOutputValue, Log log) {
		VectorFunction function = (VectorFunction) individual.getFunction();
		
		Random random = this.getParentComponent(Run.class).random;
		
		double[][] responses = function.apply(input);
		
		double maxError = 0;
		int exampleCount = input.length;
		int outputCount = targetOutput[0].length;
		
		double maxResponse = function.getMaximumOutputValue();
		double minResponse = function.getMinimumOutputValue();
		
		if (minResponse > minTargetOutputValue || maxResponse < maxTargetOutputValue) {
			throw new IllegalStateException("The response range of the function does not encompass the target output range.");
		}
		
		if (!outputRangeChecked && maxResponse - minResponse > 100) {
			logger.warn("The substrate output range seems quite large (" + (maxResponse - minResponse) + "), you might want to consider using a more tightly bounded activation function for the output neuron(s) to facilitate calculating the target error.");
			outputRangeChecked = true;
		}
		
		double maxErrorPerOutput = Math.max(maxResponse - minTargetOutputValue, maxTargetOutputValue - minResponse);
		
		if (errorTypeOutput.squareErrors())
			maxErrorPerOutput = maxErrorPerOutput * maxErrorPerOutput;
		if (errorTypeOutput.sumErrors())
			maxErrorPerOutput = outputCount * maxErrorPerOutput;
		if (errorTypeOutput.rootTotalError())
			maxErrorPerOutput = Math.sqrt(maxErrorPerOutput);
		else if (errorTypeOutput.squareTotalError())
			maxErrorPerOutput = maxErrorPerOutput * maxErrorPerOutput;
		
		maxError = errorTypeExample.squareErrors() ? maxErrorPerOutput * maxErrorPerOutput : maxErrorPerOutput;
		if (errorTypeExample.sumErrors())
			maxError = exampleCount * maxError;
		if (errorTypeExample.rootTotalError())
			maxError = Math.sqrt(maxError);
		else if (errorTypeExample.squareTotalError())
			maxError = maxError * maxError;
		
		TargetFitnessCalculatorLog logOutput = null;
		if (log.specifiesItem("string")) {
			logOutput = new TargetFitnessCalculatorLog(input, targetOutput, responses, new double[exampleCount], new boolean[exampleCount], maxError, errorTypeOutput, errorTypeExample);
			log.setLog("string", logOutput);
		}
		
		List<Integer> exampleIndexes = new ArrayList<Integer>(exampleCount);
		for (int i = 0; i < exampleCount; i++)
			exampleIndexes.add(i);
		if (logOutput == null) // Keep examples in order when logging.
			Collections.shuffle(exampleIndexes, random);
		
		double totalError = 0;
		double percentCorrect = 0;
		for (int i = 0; i < exampleCount; i++) {
			int example = exampleIndexes.get(i);
			double exampleError = 0;
			boolean correct = true;
			for (int x = 0; x < targetOutput[example].length; x++) {
				double diff = Math.abs(responses[example][x] - targetOutput[example][x]);
				exampleError += errorTypeOutput.squareErrors() ? diff * diff : diff;
				if (diff > acceptableError)
					correct = false;
			}
			
			if (errorTypeOutput.avgErrors())
				exampleError /= outputCount;
			if (errorTypeOutput.rootTotalError())
				exampleError = Math.sqrt(exampleError);
			else if (errorTypeOutput.squareTotalError())
				exampleError = exampleError * exampleError;

			if (logOutput != null) {
				logOutput.error[i] = exampleError;
			}
			
			totalError += errorTypeExample.squareErrors() ? exampleError * exampleError : exampleError;

			if (correct) {
				percentCorrect++;
			}
			if (logOutput != null) {
				logOutput.correct[i] = correct;
			}
		}

		if (errorTypeExample.avgErrors())
			totalError /= exampleCount;
		if (errorTypeExample.rootTotalError())
			totalError = Math.sqrt(totalError);
		else if (errorTypeExample.squareTotalError())
			totalError = totalError * totalError;

		double proportionalPerformance = 1 - (totalError / maxError);
		double proportionalFitness = proportionalPerformance;
		double inverseFitness = 1.0 / (1 + totalError);
		percentCorrect /= exampleCount;
		
		//System.out.println(proportionalFitness);
		
		double fitness = fitnessConversionType.equals("proportional") ? proportionalFitness : inverseFitness;
		double performance = performanceMetric.equals("proportional") ? proportionalPerformance : percentCorrect;
				
		individual.evaluationData.setResult(fitnessEvalDesc, fitness);
		individual.evaluationData.setResult(performanceEvalDesc, performance);
		
		if (terminateOnPerformance != 0 && performance >= terminateOnPerformance) {
			terminate = true;
		}
	}
	
	
	public String getFitnessConversionType() {
		return fitnessConversionType;
	}
	
	
	@Override
	public boolean shouldTerminate() {
		return terminate;
	}
	
	
	/**
	 * The type of error calculation to use.
	 * 
	 * @see TargetFitnessCalculator#ERROR_TYPE_TRIAL_KEY
	 * @see TargetFitnessCalculator#ERROR_TYPE_OUTPUT_KEY
	 */
	public enum ErrorType {
		/**
		 * Sum of Absolute Errors, the sum of the absolute of the errors.
		 */
		SAE,
		/**
		 * Squared Sum of Absolute Errors, the squared sum of the absolute of the errors.
		 */
		SSAE,
		/**
		 * Sum of Squared Errors, the sum of the squared errors.
		 */
		SSE,
		/**
		 * Root of Sum of Squared Errors, the square root of the sum of the squared errors.
		 */
		RSSE,
		/**
		 * Mean of Absolute Errors, the average of the absolute errors.
		 */
		MAE,
		/**
		 * Squared Mean of Absolute Errors, the squared average of the absolute errors.
		 */
		SMAE,
		/**
		 * MSE: Mean of Squared Errors, the average of the squared errors.
		 */
		MSE,
		/**
		 * Root of Mean of Squared Errors, the square root of the average of the squared errors.
		 */
		RMSE;

		/**
		 * @return True iff the individual errors are to be squared.
		 */
		public boolean squareErrors() {
			return this == SSE || this == RSSE || this == MSE || this == RMSE;
		}

		/**
		 * @return True iff the individual errors are to be summed (possibly after being squared).
		 */
		public boolean sumErrors() {
			return this == SAE || this == SSAE || this == SSE || this == RSSE;
		}

		/**
		 * @return True iff the individual errors are to be averaged (possibly after being squared). This will always
		 *         return the opposite of {@link #sumErrors()}.
		 */
		public boolean avgErrors() {
			return !sumErrors();
		}

		/**
		 * @return True iff the total error is to be square rooted.
		 */
		public boolean rootTotalError() {
			return this == RSSE || this == RMSE;
		}

		/**
		 * @return True iff the total error is to be squared.
		 */
		public boolean squareTotalError() {
			return this == SSAE || this == SMAE;
		}
	}
}
