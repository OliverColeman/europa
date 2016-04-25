package com.ojcoleman.europa.evaluators;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.util.ArrayUtil;

/**
 * @author O. J. Coleman
 *
 */
public class CSVTargetFunctionEvaluator extends TargetFitnessCalculator {
	@Parameter(description = "The path to the CSV file containing the input and target target vectors. Input and target pairs must be in the same row and be separated by a blank column. The first line must be the input and target labels.")
	String pathToCSVFile;

	double[][] inputRaw;
	double[][] targetRaw;
	protected Vector[] input;
	protected Vector[] target;
	protected double minTargetOutputValue;
	protected double maxTargetOutputValue;

	public CSVTargetFunctionEvaluator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		if (componentConfig.isDummy) {
			return;
		}

		CSVParser parser = CSVParser.parse(Paths.get(pathToCSVFile).toFile(), StandardCharsets.UTF_8, CSVFormat.DEFAULT.withHeader());
		List<CSVRecord> lines = parser.getRecords();
		List<String> headers = new ArrayList<String>(parser.getHeaderMap().keySet());

		int inputSize = 0, outputSize;
		for (String head : headers) {
			if (head == null || head.equals("")) {
				break;
			}
			inputSize++;
		}

		if (inputSize == headers.size()) {
			throw new IllegalArgumentException("The input and target values in " + pathToCSVFile + " must be separated by an empty column");
		}

		outputSize = headers.size() - inputSize - 1;

		inputRaw = new double[lines.size()][inputSize];
		targetRaw = new double[lines.size()][outputSize];

		double[] minValuesInput = new double[inputSize];
		double[] minValuesOutput = new double[outputSize];
		double[] maxValuesInput = new double[inputSize];
		double[] maxValuesOutput = new double[outputSize];
		Arrays.fill(minValuesInput, Double.MAX_VALUE);
		Arrays.fill(minValuesOutput, Double.MAX_VALUE);
		Arrays.fill(maxValuesInput, -Double.MAX_VALUE);
		Arrays.fill(maxValuesOutput, -Double.MAX_VALUE);

		for (int line = 0; line < lines.size(); line++) {
			for (int i = 0; i < inputSize; i++) {
				inputRaw[line][i] = Double.parseDouble(lines.get(line).get(i));
				minValuesInput[i] = Math.min(minValuesInput[i], inputRaw[line][i]);
				maxValuesInput[i] = Math.max(maxValuesInput[i], inputRaw[line][i]);
			}
			for (int r = inputSize + 1, t = 0; t < outputSize; r++, t++) {
				targetRaw[line][t] = Double.parseDouble(lines.get(line).get(r));
				minValuesOutput[t] = Math.min(minValuesOutput[t], inputRaw[line][t]);
				maxValuesOutput[t] = Math.max(maxValuesOutput[t], inputRaw[line][t]);
			}
		}

		VectorMetadata inputMD = new VectorMetadata(headers.subList(0, inputSize), minValuesInput, maxValuesInput, new boolean[inputSize]);
		VectorMetadata outputMD = new VectorMetadata(headers.subList(inputSize + 1, headers.size()), minValuesOutput, maxValuesOutput, new boolean[outputSize]);
		input = new Vector[lines.size()];
		target = new Vector[lines.size()];

		for (int line = 0; line < lines.size(); line++) {
			input[line] = new Vector(inputMD, inputRaw[line], false);
			// Because Vector constructor above copies the values, and we don't won't duplicate array instances.
			inputRaw[line] = input[line].getValuesReference();

			target[line] = new Vector(outputMD, targetRaw[line], false);
			targetRaw[line] = target[line].getValuesReference();
		}

		minTargetOutputValue = ArrayUtil.getMinValue(minValuesOutput);
		maxTargetOutputValue = ArrayUtil.getMaxValue(maxValuesOutput);
	}

	@Override
	public void evaluate(Individual individual, Log log) {
		super.evaluate((Individual<?, VectorFunction>) individual, inputRaw, targetRaw, minTargetOutputValue, maxTargetOutputValue, log);
	}

	@Override
	public int getVectorFunctionInputSize() {
		return input != null && input.length > 0 ? input[0].size() : 0;
	}

	@Override
	public int getVectorFunctionOutputSize() {
		return target != null && target.length > 0 ? target[0].size() : 0;
	}
}
