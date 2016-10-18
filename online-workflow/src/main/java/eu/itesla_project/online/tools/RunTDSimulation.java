/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.csvreader.CsvWriter;
import com.google.common.collect.Sets;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.computation.local.LocalComputationManager;
import eu.itesla_project.iidm.datasource.GenericReadOnlyDataSource;
import eu.itesla_project.iidm.import_.Importer;
import eu.itesla_project.iidm.import_.Importers;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.Security;
import eu.itesla_project.simulation.securityindexes.SecurityIndex;
import eu.itesla_project.simulation.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
//@AutoService(Tool.class)
public class RunTDSimulation implements Tool {

	private static final String EMPTY_CONTINGENCY_ID = "Empty-Contingency";

	private static Command COMMAND = new Command() {
		
		@Override
		public String getName() {
			return "run-td-simulation";
		}

		@Override
		public String getTheme() {
			return Themes.ONLINE_WORKFLOW;
		}

		@Override
		public String getDescription() {
			return "Run time-domain simulation";
		}

		@Override
		public Options getOptions() {
			Options options = new Options();
	        options.addOption(Option.builder().longOpt("case-dir")
	                                .desc("the directory where the case is")
	                                .hasArg()
	                                .argName("DIR")
	                                .required()
	                                .build());
	        options.addOption(Option.builder().longOpt("case-basename")
	                                .desc("the case base name (all cases of the directory if not set)")
	                                .hasArg()
	                                .argName("NAME")
	                                .build());
	        options.addOption(Option.builder().longOpt("contingencies")
	                                .desc("contingencies to test separated by , (all the db in not set)")
	                                .hasArg()
	                                .argName("LIST")
	                                .build());
	        options.addOption(Option.builder().longOpt("empty-contingency")
					                .desc("include the empty contingency among the contingencies")
					                .build());
	        options.addOption(Option.builder().longOpt("output-folder")
	                                .desc("the folder where to store the data")
	                                .hasArg()
	                                .argName("FOLDER")
	                                .build());
	        return options;
		}

		@Override
		public String getUsageFooter() {
			return null;
		}
		
	};
	
	@Override
	public Command getCommand() {
		return COMMAND;
	}
	
	private Map<String, Boolean> runTDSimulation(Network network, Set<String> contingencyIds, boolean emptyContingency,
												 ComputationManager computationManager, SimulatorFactory simulatorFactory,
												 ContingenciesAndActionsDatabaseClient contingencyDb,
												 Writer metricsContent) throws Exception {
		Map<String, Boolean> tdSimulationResults = new HashMap<String, Boolean>();
		Map<String, Object> initContext = new HashMap<>();
		SimulationParameters simulationParameters = SimulationParameters.load();
		// run stabilization 
		Stabilization stabilization = simulatorFactory.createStabilization(network, computationManager, 0);
		stabilization.init(simulationParameters, initContext);
		ImpactAnalysis impactAnalysis = null;
		System.out.println("running stabilization on network " + network.getId());
		StabilizationResult stabilizationResults = stabilization.run();
		metricsContent.write("****** BASECASE " + network.getId()+"\n");
		metricsContent.write("*** Stabilization Metrics ***\n");
		Map<String, String> stabilizationMetrics = stabilizationResults.getMetrics();
		if ( stabilizationMetrics!=null && !stabilizationMetrics.isEmpty()) {
			for(String parameter : stabilizationMetrics.keySet())
				metricsContent.write(parameter + " = " + stabilizationMetrics.get(parameter)+"\n");
		}
		metricsContent.flush();
		if (stabilizationResults.getStatus() == StabilizationStatus.COMPLETED) {
			if ( emptyContingency ) // store data for t-d simulation on empty contingency, i.e. stabilization
				tdSimulationResults.put(EMPTY_CONTINGENCY_ID, true);
			// check if there are contingencies to run impact analysis
			if ( contingencyIds==null && contingencyDb.getContingencies(network).size()==0 )
        		contingencyIds = new HashSet<String>();
			if ( contingencyIds==null || !contingencyIds.isEmpty() ) {
				// run impact analysis
				impactAnalysis = simulatorFactory.createImpactAnalysis(network, computationManager, 0, contingencyDb);
				impactAnalysis.init(simulationParameters, initContext);
				System.out.println("running impact analysis on network " + network.getId());
				ImpactAnalysisResult impactAnalisResults = impactAnalysis.run(stabilizationResults.getState(), contingencyIds);
				for(SecurityIndex index : impactAnalisResults.getSecurityIndexes() ) {
					tdSimulationResults.put(index.getId().toString(), index.isOk());
				}
				metricsContent.write("*** Impact Analysis Metrics ***\n");
				Map<String, String> impactAnalysisMetrics = impactAnalisResults.getMetrics();
				if ( impactAnalysisMetrics!=null && !impactAnalysisMetrics.isEmpty()) {
					for(String parameter : impactAnalysisMetrics.keySet())
						metricsContent.write(parameter + " = " + impactAnalysisMetrics.get(parameter)+"\n");
				}
				metricsContent.flush();
			}
		} else {
			if ( emptyContingency ) // store data for t-d simulation on empty contingency, i.e. stabilization
				tdSimulationResults.put(EMPTY_CONTINGENCY_ID, false);
		}
		return tdSimulationResults;
	}
	
	private Path getFile(Path folder, String filename) {
		if ( folder != null )
			return Paths.get(folder.toString(), filename);
		return Paths.get(filename);
	}
	
	private void writeCsvViolations(Path folder, Map<String, List<LimitViolation>> networksViolations) throws IOException {
		Path csvFile = getFile(folder, "networks-violations.csv");
		System.out.println("writing pre-contingency network violations to file " + csvFile.toString());
		try (FileWriter content = new FileWriter(csvFile.toFile())) {
			CsvWriter cvsWriter = null;
			try {
				cvsWriter = new CsvWriter(content, ',');
				String[] headers = new String[]{"Basecase", "Equipment", "Type", "Value", "Limit"};
				cvsWriter.writeRecord(headers);
				for(String caseBasename : networksViolations.keySet()) {
					for(LimitViolation violation : networksViolations.get(caseBasename)) {
						String[] values = new String[]{caseBasename, 
													   violation.getSubject().getId(), 
													   violation.getLimitType().name(), 
													   Float.toString(violation.getValue()), 
													   Float.toString(violation.getLimit())};
						cvsWriter.writeRecord(values);
					}
				}
				cvsWriter.flush();
			} catch (IOException e) {
				throw e;
			} finally {
				if ( cvsWriter!=null )
					cvsWriter.close();
			}
		} catch (IOException e1) {
			throw e1;
		}
	}
	
	private void writeCsvTDResults(Path folder, Map<String, Map<String, Boolean>> tdSimulationsResults) throws IOException {
		Path csvFile = getFile(folder, "simulation-results.csv");
		System.out.println("writing simulation results to file " + csvFile.toString());
		Set<String> securityIndexIds = new LinkedHashSet<>();
        for (Map<String, Boolean> securityIndexesPerBasecase : tdSimulationsResults.values()) {
            if (securityIndexesPerBasecase != null) {
                securityIndexIds.addAll(securityIndexesPerBasecase.keySet());
            }
        }
        String[] indexIds = securityIndexIds.toArray(new String[securityIndexIds.size()]);
        Arrays.sort(indexIds);
		try (FileWriter content = new FileWriter(csvFile.toFile())) {
			CsvWriter cvsWriter = null;
			try {
				cvsWriter = new CsvWriter(content, ',');
				String[] headers = new String[indexIds.length+1];
				headers[0] = "Basecase";
				int i = 1;
				for(String securityIndexId : indexIds)
					headers[i++] = securityIndexId;
				cvsWriter.writeRecord(headers);
				for(String caseBasename : tdSimulationsResults.keySet()) {
					String[] values = new String[indexIds.length+1];
					values[0] = caseBasename;
					i = 1;
					for(String securityIndexId : indexIds) {
						String result = "NA";
						if ( tdSimulationsResults.get(caseBasename).containsKey(securityIndexId) )
							result = tdSimulationsResults.get(caseBasename).get(securityIndexId) ? "OK" : "KO";
						values[i++] = result;
					}
					cvsWriter.writeRecord(values);
				}
				cvsWriter.flush();
			} catch (IOException e) {
				throw e;
			} finally {
				if ( cvsWriter!=null )
					cvsWriter.close();
			}
		} catch (IOException e1) {
			throw e1;
		}
	}

    @Override
    public void run(CommandLine line) throws Exception {
        Path caseDir = Paths.get(line.getOptionValue("case-dir"));
        String caseBaseName = line.hasOption("case-basename") ? line.getOptionValue("case-basename") : null;
        Set<String> contingencyIds = line.hasOption("contingencies") ?  Sets.newHashSet(line.getOptionValue("contingencies").split(",")) : null;
        boolean emptyContingency = line.hasOption("empty-contingency") ? true : false;
        Path outputFolder = line.hasOption("output-folder") ? Paths.get(line.getOptionValue("output-folder")) : null;
        
        Map<String, List<LimitViolation>> networksViolations = new HashMap<String, List<LimitViolation>>();
        Map<String, Map<String, Boolean>> tdSimulationsResults = new HashMap<String, Map<String,Boolean>>();
        Path metricsFile = getFile(outputFolder, "metrics.log");
        try (ComputationManager computationManager = new LocalComputationManager();
             FileWriter metricsContent = new FileWriter(metricsFile.toFile())) {

        	OnlineConfig config = OnlineConfig.load();
            ContingenciesAndActionsDatabaseClient contingencyDb = config.getContingencyDbClientFactoryClass().newInstance().create();
            SimulatorFactory simulatorFactory = config.getSimulatorFactoryClass().newInstance();

            Importer importer = Importers.getImporter("CIM1", computationManager);

            if (caseBaseName != null) {
            	Network network = importer.import_(new GenericReadOnlyDataSource(caseDir, caseBaseName), new Properties());
            	List<LimitViolation> networkViolations = Security.checkLimits(network);
            	networksViolations.put(network.getId(), networkViolations);
            	Map<String, Boolean> tdSimulationResults = runTDSimulation(network, 
            															   contingencyIds, 
            															   emptyContingency, 
            															   computationManager, 
            															   simulatorFactory,
            															   contingencyDb,
            															   metricsContent);
            	tdSimulationsResults.put(network.getId(), tdSimulationResults);
            } else {
                Importers.importAll(caseDir, importer, false, network -> {
                    try {
                    	List<LimitViolation> networkViolations = Security.checkLimits(network);
                    	networksViolations.put(network.getId(), networkViolations);
                    	Map<String, Boolean> tdSimulationResults = runTDSimulation(network, 
                    															   contingencyIds, 
                    															   emptyContingency, 
                    															   computationManager, 
                    															   simulatorFactory,
                    															   contingencyDb,
                    															   metricsContent);
                    	tdSimulationsResults.put(network.getId(), tdSimulationResults);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, dataSource -> System.out.println("loading case " + dataSource.getBaseName()));
            }
        }
        try {
        	writeCsvViolations(outputFolder, networksViolations);
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
        	writeCsvTDResults(outputFolder, tdSimulationsResults);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

}
