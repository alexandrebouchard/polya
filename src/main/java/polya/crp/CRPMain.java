package polya.crp;

import java.io.File;
import java.util.Random;

import polya.crp.utils.LogAverageFunction;
import polya.mcmc.ExponentialPrior;
import polya.mcmc.RealVariableMHMove;
import polya.mcmc.UniformPrior;

import tutorialj.Tutorial;
import bayonet.coda.CodaParser;
import bayonet.coda.SimpleCodaPlots;
import bayonet.rplot.PlotContour;
import briefj.OutputManager;

import static briefj.Results.*;


/**
 * A simple main for CRP sampling
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class CRPMain
{
  private static int paramThinPeriod = 10, burnIn = 1000, predThinPeriod = 1000;

  /**
   * ### Running the sampler
   * 
   * #### How to do it from eclipse
   * 
   * 1. right click on CRPMain in the left panel, 
   * 2. choose ``Run as..``, then ``Run configuration..``
   * 3. click on ``java application on the left panel, 
   * 4. click on the icon with a ``+`` sign (new) (new launch configuration)
   * 5. go in the ``arguments`` tab, where you can make control useful settings:
   *  - provide command line arguments as if the program is called from terminal (not needed here)
   *  - provide arguments to the java virtual machine; the most useful is to increase the 
   *  memory given to your program. For example write ``-Xmx2g`` to give it 2 GB here.
   *  - the working directory (not needed here).
   * 6. Click on ``Run``
   * 
   * Note that you only need to do this once, you run configuration is saved afterwards
   * and is available via the small arrow by the green play icon on the top of your editor.
   * 
   * #### Expected result
   * 
   * Running the code will create a new unique directory, ``experiments/all/[name of main]-[unique id].exec``,
   * symlinked in ``experiments/latest`` containing:
   * 
   * - Coda files for various variables (number of tables, more hyper-parameters later)
   * - Generated traceplots for the above ``codaPlots.pdf``
   * - The average of the predictive distributions, ``predictive.pdf`` (see CompleteState.logPredictive()
   * and LogAverageFunction if you are curious about how this plot is created).
   * 
   * The predictive should be a fairly faithful reconstruction of the data if your code is 
   * correct.
   */
  @Tutorial(showSource = false, showLink = true)
  public static void main(String [] args)
  {
    // initialize with each customer alone at their table
    CompleteState completeState = CompleteState.standardInit(new File("data/circle.csv"));
    initMHMoves(completeState);
    
    Random rand = new Random(1);
    
    // utility to output the sampled variables (alpha0, disount, hyper-params, etc) into a csv
    OutputManager output = new OutputManager();
    File csvSamples = new File(getResultFolder(), "samples-csv");
    output.setOutputFolder(csvSamples);
    
    // average (over MCMC samplers) of the predictive distribution
    LogAverageFunction averagedPredictive = new LogAverageFunction();
    for (int mcmcSweep = 0; mcmcSweep < 10000; mcmcSweep++)
    {
      // sample all the variables once
      completeState.doOneSamplingRound(rand);
      
      // accumulate statistics on the samples
      if (mcmcSweep % paramThinPeriod  == 0 && mcmcSweep > burnIn)
      {
        for (String key : completeState.realValuedStatistics().keySet())
          output.printWrite(key, "mcmcIter", mcmcSweep, key, completeState.realValuedStatistics().get(key).getValue());
        System.out.println("Number of individual sampling steps so far: " + (mcmcSweep+1)* completeState.nVariables());
        System.out.println();
      }
      if (mcmcSweep % predThinPeriod  == 0 && mcmcSweep > burnIn)
      {
        averagedPredictive.addFunction(0.0, completeState.logPredictive());
      }
    }
    
    output.close();
    
    // use R's coda to create trace plots and histograms for real-valued parameters
    File 
      indexFile = new File(getResultFolder(), "CODAindex.txt"),
      chainFile = new File(getResultFolder(), "CODAchain1.txt");
    CodaParser.CSVToCoda(indexFile, chainFile, csvSamples);
    SimpleCodaPlots codaPlots = new SimpleCodaPlots(chainFile, indexFile);
    codaPlots.toPDF(new File(getResultFolder(), "codaPlots.pdf"));
    
    // plot the predictive distribution
    PlotContour pc = PlotContour.fromFunction(averagedPredictive);
    pc.centerToZero(15);
    pc.toPDF(new File(getResultFolder(), "predictive.pdf"));
  }
  
  /**
   * ### Resampling hyper parameters (Optional)
   * 
   * Next, you will add some Metropolis-Hastings steps to resample the following variables:
   * 
   * - the concentration or strength parameter alpha0 of the PY
   * - the discount parameter theta
   * - the hyper-parameter kappa of the NIW model
   * - the hyper-parameter nu of the NIW model
   * 
   * For example, once you have completed the next step, un-commenting the line below will 
   * enable the resampling of alpha0. Similar lines can be used for the other quantities.
   * Just be careful of picking a reasonable prior (see ExponentialPrior and
   * UniformPrior).
   * 
   * **Warning:** Make sure you provide all the factors connected to the variable in state.mhMoves.addRealNodeToResampleWithPrior()
   * (see state.clusteringFactor and state.collapsedLikelihoodFactor).
   */
  @Tutorial(showLink = true, nextStep = RealVariableMHMove.class)
  private static void initMHMoves(CompleteState state)
  {
    /* startRem // state.mhMoves.addRealNodeToResampleWithPrior(state.clusteringParams.alpha0VariableView(), ExponentialPrior.withRate(1e-100).truncateAt(-1), state.clusteringFactor); */
    // resample clustering parameters
    state.mhMoves.addRealNodeToResampleWithPrior(
        state.clusteringParams.alpha0VariableView(), 
        ExponentialPrior.withRate(1e-100).truncateAt(-1), 
        state.clusteringFactor);
    state.mhMoves.addRealNodeToResampleWithPrior(
        state.clusteringParams.discountVariableView(),
        UniformPrior.onUnitInteval(),
        state.clusteringFactor);
    
    // resample likelihood hyper-params
    state.mhMoves.addRealNodeToResampleWithPrior(
        state.hp.kappaVariableView(),
        ExponentialPrior.withRate(1e-100),
        state.collapsedLikelihoodFactor);
    state.mhMoves.addRealNodeToResampleWithPrior(
        state.hp.nuVariableView(),
        ExponentialPrior.withRate(1e-100).truncateAt(state.hp.dim() - 1),
        state.collapsedLikelihoodFactor);
    /* endRem */
  }
}
