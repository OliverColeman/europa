package com.ojcoleman.europa.evaluators.dpb;

import java.util.HashSet;
import java.util.Set;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.evaluators.VectorFunctionEvaluator;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.util.IntervalDouble;

/**
 * Implements the Markovian double pole balancing task (with velocities included in inputs) as described in:
 * <blockquote> Gruau, F., Whitley, D., and Pyeatt, L. (1996). A comparison between cellular encoding and direct encoding for genetic neural networks. 
 * In Genetic Programming 1996: Proceedings of the First Annual Conference, pages 81–89, MIT Press, Cambridge, Massachusetts </blockquote>
 * 
 * This code was adapted from SharpNEAT by Colin Green (http://sharpneat.sourceforge.net/).
 */
public class DoublePoleBalancing extends VectorFunctionEvaluator  {
	// Some physical model constants.
	protected static final double Gravity = -9.8;
	protected static final double MassCart = 1.0;
	// Actually half the pole's length.
	protected static final double Length1 = 0.5;
	protected static final double MassPole1 = 0.1;
	protected static final double Length2 = 0.05;
	protected static final double MassPole2 = 0.01;
	protected static final double ForceMag = 10.0;
	/**
	 * Time increment interval in seconds.
	 */
	public static final double TimeDelta = 0.01;
	protected static final double FourThirds = 4.0 / 3.0;
	/**
	 * Uplifting moment?
	 */
	protected static final double MUP = 0.000002;
	
	
	@Parameter (description="The track length in metres.", defaultValue="4.8")
	protected double trackLength;
	
	@Parameter (description="The number of time steps for an evaluation.", defaultValue="100000")
	protected int maxTimesteps;
	
	@Parameter (description="The maximum allowable angle from vertical, in degrees.", defaultValue="36")
	protected double poleAngleThreshold;
	
	protected double trackLengthHalf;
	
	protected EvaluationDescription fitnessEvalDesc;
	protected double poleAngleThresholdRad;
	protected boolean shouldTerminate = false;
	
	public DoublePoleBalancing(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		trackLengthHalf = trackLength / 2.0;
		
		poleAngleThresholdRad = Math.toRadians(poleAngleThreshold);
		
		fitnessEvalDesc = new EvaluationDescription(getName() + " fitness", this, IntervalDouble.UNIT, 1, false);
	}

	
	public void evaluate(Individual individual, Log log) {	
		VectorFunction substrate = (VectorFunction) individual.getFunction();
		
		// [0] - Cart Position (meters).
		// [1] - Cart velocity (m/s).
		// [2] - Pole 1 angle (radians)
		// [3] - Pole 1 angular velocity (radians/sec).
		// [4] - Pole 2 angle (radians)
		// [5] - Pole 2 angular velocity (radians/sec).
		double[] state = new double[6];
		state[2] = Math.toRadians(4); // 4 degrees
		// Run the pole-balancing simulation.
		int timestep = 0;
		double[] input = new double[7];
		for (; timestep < maxTimesteps; timestep++) {
			// Provide state info to the network (normalised to +-1.0). Markovian (With velocity info)
			// Cart Position is +-trackLengthHalfed
			input[0] = state[0] / trackLengthHalf;
			// Cart velocity is typically +-0.75
			input[1] = state[1] / 0.75;
			// Pole Angle is +-poleAngleThresholdRad. Values outside of this range stop the simulation.
			input[2] = state[2] / poleAngleThresholdRad;
			// Pole angular velocity is typically +-1.0 radians. No scaling required.
			input[3] = state[3];
			// Pole Angle is +-poleAngleThresholdRad. Values outside of this range stop the simulation.
			input[4] = state[4] / poleAngleThresholdRad;
			// Pole angular velocity is typically +-1.0 radians. No scaling required.
			input[5] = state[5];
			// Bias.
			input[6] = 1;
			
			
			// Activate the network.
			double[] output = substrate.apply(input);
			
			// Scale output to range [0, 1].
			output[0] = (output[0] - substrate.getMinimumOutputValue()) / (substrate.getMaximumOutputValue() - substrate.getMinimumOutputValue());

			// Get network response and calc next timestep state.
			performAction(state, output[0]);
			
			// Check for failure state. Has the cart run off the ends of the track or has the pole
			// angle gone beyond the threshold.
			if ((state[0] < -trackLengthHalf) || (state[0] > trackLengthHalf) || (state[2] > poleAngleThresholdRad) || (state[2] < -poleAngleThresholdRad) || (state[4] > poleAngleThresholdRad) || (state[4] < -poleAngleThresholdRad)) {
				break;
			}
		}
		
		shouldTerminate |= timestep == maxTimesteps;
		
		individual.evaluationData.setResult(fitnessEvalDesc, (double) timestep / maxTimesteps);
	}

	/**
	 * Calculates a state update for the next timestep using current model state and a single action from the
	 * controller. The action is a continuous variable with range [0:1]. 0 -> push left, 1 -> push right.
	 * 
	 * @param state Model state.
	 * @param output Push force.
	 */
	protected void performAction(double[] state, double output) {
		int i;
		double[] dydx = new double[6];
		for (i = 0; i < 2; ++i) {
			// Apply action to the simulated cart-pole
			// Runge-Kutta 4th order integration method
			dydx[0] = state[1];
			dydx[2] = state[3];
			dydx[4] = state[5];
			step(output, state, dydx);
			rk4(output, state, dydx);
		}
	}

	private void step(double action, double[] st, double[] derivs) {
		double force, costheta_1, costheta_2, sintheta_1, sintheta_2, gsintheta_1, gsintheta_2, temp_1, temp_2, ml_1, ml_2, fi_1, fi_2, mi_1, mi_2;
		force = (action - 0.5) * ForceMag * 2;
		costheta_1 = Math.cos(st[2]);
		sintheta_1 = Math.sin(st[2]);
		gsintheta_1 = Gravity * sintheta_1;
		costheta_2 = Math.cos(st[4]);
		sintheta_2 = Math.sin(st[4]);
		gsintheta_2 = Gravity * sintheta_2;
		ml_1 = Length1 * MassPole1;
		ml_2 = Length2 * MassPole2;
		temp_1 = MUP * st[3] / ml_1;
		temp_2 = MUP * st[5] / ml_2;
		fi_1 = (ml_1 * st[3] * st[3] * sintheta_1) + (0.75 * MassPole1 * costheta_1 * (temp_1 + gsintheta_1));
		fi_2 = (ml_2 * st[5] * st[5] * sintheta_2) + (0.75 * MassPole2 * costheta_2 * (temp_2 + gsintheta_2));
		mi_1 = MassPole1 * (1 - (0.75 * costheta_1 * costheta_1));
		mi_2 = MassPole2 * (1 - (0.75 * costheta_2 * costheta_2));
		derivs[1] = (force + fi_1 + fi_2) / (mi_1 + mi_2 + MassCart);
		derivs[3] = -0.75 * (derivs[1] * costheta_1 + gsintheta_1 + temp_1) / Length1;
		derivs[5] = -0.75 * (derivs[1] * costheta_2 + gsintheta_2 + temp_2) / Length2;
	}

	private void rk4(double f, double[] y, double[] dydx) {
		int i;
		double hh, h6;
		double[] dym = new double[6];
		double[] dyt = new double[6];
		double[] yt = new double[6];
		hh = TimeDelta * 0.5;
		h6 = TimeDelta / 6.0;
		for (i = 0; i <= 5; i++) {
			yt[i] = y[i] + (hh * dydx[i]);
		}
		step(f, yt, dyt);
		dyt[0] = yt[1];
		dyt[2] = yt[3];
		dyt[4] = yt[5];
		for (i = 0; i <= 5; i++) {
			yt[i] = y[i] + (hh * dyt[i]);
		}
		step(f, yt, dym);
		dym[0] = yt[1];
		dym[2] = yt[3];
		dym[4] = yt[5];
		for (i = 0; i <= 5; i++) {
			yt[i] = y[i] + (TimeDelta * dym[i]);
			dym[i] = dym[i] + dyt[i];
		}
		step(f, yt, dyt);
		dyt[0] = yt[1];
		dyt[2] = yt[3];
		dyt[4] = yt[5];

		for (i = 0; i <= 5; i++) {
			y[i] = y[i] + h6 * (dydx[i] + dyt[i] + 2.0 * dym[i]);
		}
	}


	@Override
	public int getVectorFunctionInputSize() {
		return 7;
	}


	@Override
	public int getVectorFunctionOutputSize() {
		return 1;
	}


	@Override
	public Set<EvaluationDescription> getEvaluationDescriptions() {
		Set<EvaluationDescription> evalDescs = new HashSet<>();
		evalDescs.add(fitnessEvalDesc);
		return evalDescs;
	}
	
	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
}
