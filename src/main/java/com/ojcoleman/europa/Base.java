package com.ojcoleman.europa;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.SubComponent;
import com.ojcoleman.europa.core.Run;

/**
 * Base ConfigurableComponent from which all other components are created.
 */
public class Base extends ConfigurableComponent {
	final Logger logger = LoggerFactory.getLogger(Base.class);
	
	@Parameter (description="The path to the original configuration file (.json extension). NOTE: this is informational only, setting it has no effect.", optional=true)
	protected String configFilePath;
	
	@Parameter (description="How many iterations/generations to perform before saving the Run state to a file (for later resume or examination). A value <= 0 indicates no saving.", defaultValue="1000")
	protected int saveFrequency;
	
	@Parameter (description="The path of the file to save the Run state to (for later resume or examination). DefaultEvolver is the name of the Run in the current working directory.", optional=true)
	protected String savePath;
	
	@Parameter (description="The number of runs to perform.", defaultValue="1")
	protected int numRuns;
	
	
	@SubComponent (description="Main component for the evolutionary run.", defaultImplementation=Run.class)
	Run run;
	
	
	public Base(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		if (savePath == null || savePath.equals("")) {
			savePath = run.getName();
		}
	}
	
	/**
	 * Returns the path to the original configuration file, or null if none specified.
	 */ 
	public Path getConfigFilePath() {
		if (configFilePath != null) {
			return Paths.get(configFilePath);
		}
		return null;
	}
}