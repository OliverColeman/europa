package com.ojcoleman.europa.evaluators;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import com.ojcoleman.europa.evaluators.TargetFitnessCalculator.ErrorType;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.util.NiceWriter;

/**
 * Stores log data for {@link TargetFitnessCalculator}.
 * 
 * @author O. J. Coleman
 */
public class TargetFitnessCalculatorLog {
	/**
	 * Array containing input examples, in the form [example][input].
	 */
	public final double[][] input;
	
	/**
	 * Array containing output examples, in the form [example][output].
	 */
	public final double[][] targetOutput;
	
	/**
	 * Array containing the actual output of the function being evaluated, in the form [example][output].
	 */
	public final double[][] actualOutput;
	
	/**
	 * Array containing the computed error for each example.
	 */
	public final double[] error;
	
	/**
	 * Array indicating whether for a given example the error for each output was acceptably low.
	 */
	public final boolean[] correct;

	/**
	 * The maximum possible error for a example.
	 */
	public final double maximumError;
	
	/**
	 * The type of error calculation performed for each each output.
	 */
	public final ErrorType errorTypeOutput;
	
	/**
	 * The type of error calculation performed for each example.
	 */
	public final ErrorType errorTypeExample;
	
	
	public TargetFitnessCalculatorLog(double[][] input, double[][] targetOutput, double[][] actualOutput, double[] exampleError, boolean[] correct, double maximumError, ErrorType errorTypeOutput, ErrorType errorTypeExample) {
		this.input = ArrayUtil.copy(input);
		this.targetOutput = ArrayUtil.copy(targetOutput);
		this.actualOutput = ArrayUtil.copy(actualOutput);
		this.error = Arrays.copyOf(exampleError, exampleError.length);
		this.correct = Arrays.copyOf(correct, correct.length);
		this.maximumError = maximumError;
		this.errorTypeOutput = errorTypeOutput;
		this.errorTypeExample = errorTypeExample;
	}
	
	
	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		NiceWriter logOutput = new NiceWriter(writer);
		
		int exampleCount = error.length;
		double totalError = 0;
		double totalCorrect = 0;
		
		try {
			for (int example = 0; example < exampleCount; example++) {
				logOutput.put(example).put("\tInput:  ").put(input[example]);
				logOutput.put("\n\tTarget: ").put(targetOutput[example]);
				logOutput.put("\n\tOutput: ").put(actualOutput[example]);
				logOutput.put("\n\tError: ").put(error[example]);
				logOutput.put((errorTypeOutput.squareErrors() ? " (sum of squared)" : ""));
				logOutput.put("  (" + (correct[example] ? "" : "in") + "correct)\n\n");
				
				totalError += errorTypeExample.squareErrors() ? error[example] * error[example] : error[example];

				totalCorrect += correct[example] ? 1 : 0;
			}
			
			if (errorTypeExample.avgErrors())
				totalError /= exampleCount;
			if (errorTypeExample.rootTotalError())
				totalError = Math.sqrt(totalError);
			else if (errorTypeExample.squareTotalError())
				totalError = totalError * totalError;
			
			logOutput.put("\nOverall results:\n");
			logOutput.put("\n  Total error: ").put(totalError);
			logOutput.put("\n  Maximum possible error: ").put(maximumError);
			logOutput.put("\n  Proportional error: ").put(totalError / maximumError);
			logOutput.put("\n  Percent correct: ").put(totalCorrect / exampleCount);
			
			logOutput.close();
		} 
		catch (IOException e) {
			// Since we're using a StringWriter we shouldn't ever get here.
		}
		
		return writer.toString();
	}
}
