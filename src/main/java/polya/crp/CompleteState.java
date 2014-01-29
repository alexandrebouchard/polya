package polya.crp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.analysis.MultivariateFunction;

import polya.crp.utils.ClusterId;
import polya.crp.utils.LogAverageFunction;
import polya.mcmc.Factor;
import polya.mcmc.MHAlternation;
import polya.mcmc.RealVariable;
import polya.parametric.Parametrics;
import polya.parametric.SufficientStatistic;
import polya.parametric.normal.CollapsedNIWModel;
import polya.parametric.normal.NIWHyperParameter;
import polya.parametric.normal.NIWs;

import com.google.common.collect.Maps;


/**
 * Contains the information needed for running an MCMC
 * chain over a CRP.
 * 
 * TODO: Currently specialized to NIW observations, generalize this.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class CompleteState
{
  /**
   * The seating arrangement.
   */
  public final CRPState clustering;
  
  /**
   * The alpha0 and discount parameters of a PY process
   */
  public final PYPrior clusteringParams;
  
  /**
   * The hyper parameters of the NIW
   */
  public final NIWHyperParameter hp;
  
  /**
   * 
   */
  public final CollapsedNIWModel model;
  
  /**
   * The MCMC kernels used to resample this state.
   */
  public final MHAlternation mhMoves;
  
  /**
   * An ordering of the customer, reshuffled at
   * each sampling round.
   */
  public final List<Integer> allCustomers;
  
  /**
   * Initialize the arrangement by putting each customer alone at their table.
   * 
   * Initialize the likelihood using the provided csv,
   * each line is a customer (no header), each column is a dimension.
   * 
   * TODO: currently assumes 2 dimensions
   * 
   * @param csvFile
   * @return
   */
  public static CompleteState standardInit(File csvFile)
  {
    CRPState state = CRPState.fullyDisconnectedClustering(NIWs.loadFromCSVFile(csvFile));
    NIWHyperParameter hp = NIWHyperParameter.withDimensionality(2);
    CollapsedNIWModel model = CollapsedNIWModel.instance;
    PYPrior prior = new PYPrior(1, 0);
    return new CompleteState(state, prior, hp, model);
  }
  
  private CompleteState(
      CRPState clustering, 
      PYPrior clusteringParams,
      NIWHyperParameter hp, 
      CollapsedNIWModel model)
  {
    this.clustering = clustering;
    this.clusteringParams = clusteringParams;
    this.hp = hp;
    this.model = model;
    allCustomers = new ArrayList<Integer>(clustering.getAllCustomers());
    Collections.sort(allCustomers);
    this.mhMoves = new MHAlternation();
  }
  
  /**
   * Return the logpredictive distribution corresponding to the current configuration.
   * 
   * @return
   */
  public MultivariateFunction logPredictive()
  {
    LogAverageFunction result = new LogAverageFunction();
    
    List<ClusterId> existingTables = clustering.getAllClusterIds();
    
    for (int i = 0; i < clustering.nTables(); i++)
    {
      ClusterId current = existingTables.get(i);
      SufficientStatistic customerAlreadyAtTable = clustering.getClusterStatistics(current);
      NIWHyperParameter updated = (NIWHyperParameter) model.update(hp, customerAlreadyAtTable);
      MultivariateFunction currentFct = NIWs.logMarginalAsFunctionOfData(updated);
      double logW = clusteringParams.logUnnormalizedPredictive(clustering.getTable(current).size(), clustering.nTables());
      result.addFunction(logW, currentFct);
    }
    NIWHyperParameter copy = NIWHyperParameter.copyOf(hp);
    MultivariateFunction currentFct = NIWs.logMarginalAsFunctionOfData(copy);
    double logW = clusteringParams.logUnnormalizedPredictive(0, clustering.nTables());
    result.addFunction(logW, currentFct);
    
    return result;
  }
  
  /**
   * Sample the customer seatings and the parameters.
   * @param rand
   */
  public void doOneSamplingRound(Random rand)
  {
    Collections.shuffle(allCustomers, rand);
    for (Integer customer : allCustomers)
      CRPSamplers.gibbs(rand, customer, clustering, hp, model, clusteringParams);
    mhMoves.sampleOneRound(rand);
  }
  
  /**
   * The factor corresponding to the collapsed likelihood.
   * Connected to:
   * - hp
   * - clustering
   */
  public final Factor collapsedLikelihoodFactor = new Factor() 
  {
    @Override
    public double logUnnormalizedPotential()
    {
      double result = 0.0;
      for (ClusterId id : clustering.getAllClusterIds())
        result += Parametrics.logMarginal(model, hp, clustering.getClusterStatistics(id));
      return result;
    }
  };
  
  /**
   * The factor corresponding to the PY/CRP clustering prior.
   * Connected to:
   * - clusteringParams
   * - clustering
   */
  public final Factor clusteringFactor = new Factor()
  {
    @Override
    public double logUnnormalizedPotential()
    {
      return CRPs.crpAssignmentLogProbabilitiy(clusteringParams, clustering);
    }
  };
  
  @Override
  public String toString()
  {
    StringBuilder result = new StringBuilder();
    for (String key : realValuedStatistics().keySet())
      result.append(key + "=" + realValuedStatistics().get(key).getValue() + "\n");
    return result.toString();
  }

  /**
   * The 
   * @return
   */
  public int nVariables()
  {
    return clustering.nCustomers() + mhMoves.nVariables();
  }
  
  private Map<String, RealVariable> _realValuedStatistics = null;
  public Map<String, RealVariable> realValuedStatistics()
  {
    if (_realValuedStatistics == null)
    {
      _realValuedStatistics = Maps.newTreeMap();
      _realValuedStatistics.put("nu", hp.nuVariableView());
      _realValuedStatistics.put("kappa", hp.kappaVariableView());
      _realValuedStatistics.put("alpha0", clusteringParams.alpha0VariableView());
      _realValuedStatistics.put("discount", clusteringParams.discountVariableView());
      _realValuedStatistics.put("nClusters", new RealVariable() {
        @Override public void setValue(double newValue)  { throw new RuntimeException(); }
        @Override public double getValue() { return clustering.nTables(); }
      });
    }
    return _realValuedStatistics;
  }
}
