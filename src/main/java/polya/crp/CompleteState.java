package polya.crp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.analysis.MultivariateFunction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import polya.crp.CRPSamplers.PYPrior;
import polya.mcmc.ExponentialPrior;
import polya.mcmc.Factor;
import polya.mcmc.MHMixture;
import polya.mcmc.RealVariable;
import polya.mcmc.UniformPrior;
import polya.parametric.Parametrics;
import polya.parametric.SufficientStatistic;
import polya.parametric.normal.CollapsedNIWModel;
import polya.parametric.normal.NIWHyperParameter;
import polya.parametric.normal.NIWs;



public class CompleteState
{
  public final CRPState clustering;
  public final PYPrior clusteringParams;
  public final NIWHyperParameter hp;
  public final CollapsedNIWModel model;
  public final MHMixture mhMoves;
  public final List<Integer> allCustomers;
  
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
    this.mhMoves = initMHMoves();
  }
  
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
  
  public static class LogAverageFunction implements MultivariateFunction
  {
    private final List<Double> logWeights = Lists.newArrayList();
    private final List<MultivariateFunction> functions = Lists.newArrayList();
    
    public void addFunction(double logWeight, MultivariateFunction function)
    {
      logWeights.add(logWeight);
      functions.add(function);
    }

    @Override
    public double value(double[] point)
    {
      double sum = Double.NEGATIVE_INFINITY;
      for (int i =0; i < logWeights.size(); i++)
      {
        double lw = logWeights.get(i);
        double pred = functions.get(i).value(point);
        sum = logAdd(sum, lw + pred);
      }
      return sum;
    }
    
  }
  
  public static double logAdd(double logX, double logY) {
    // make a the max
    if (logY > logX) {
      double temp = logX;
      logX = logY;
      logY = temp;
    }
    // now a is bigger
    if (logX == Double.NEGATIVE_INFINITY) {
      return logX;
    }
    double negDiff = logY - logX;
    if (negDiff < -20) {
      return logX;
    }
    return logX + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff));
  }

  private MHMixture initMHMoves()
  {
    MHMixture result = new MHMixture();
    
    // resample clustering parameters
    result.addRealNodeToResampleWithPrior(
        clusteringParams.alpha0VariableView(), 
        ExponentialPrior.withRate(1e-100).truncateAt(-1), 
        clusteringFactor);
    result.addRealNodeToResampleWithPrior(
        clusteringParams.discountVariableView(),
        UniformPrior.onUnitInteval(),
        clusteringFactor);
    
    // resample likelihood hyper-params
    result.addRealNodeToResampleWithPrior(
        hp.kappaVariableView(),
        ExponentialPrior.withRate(1e-100),
        collapsedLikelihoodFactor);
    result.addRealNodeToResampleWithPrior(
        hp.nuVariableView(),
        ExponentialPrior.withRate(1e-100).truncateAt(hp.dim() - 1),
        collapsedLikelihoodFactor);
    
    return result;
  }

  public void doOneSamplingRound(Random rand)
  {
    Collections.shuffle(allCustomers, rand);
    for (Integer customer : allCustomers)
      CRPSamplers.gibbs(rand, customer, clustering, hp, model, clusteringParams);
    mhMoves.sampleOneRound(rand);
  }
  
  private final Factor collapsedLikelihoodFactor = new Factor() 
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
  
  private final Factor clusteringFactor = new Factor()
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
//    result.append("kappa=" + hp.kappa() + "\n");
//    result.append("nu=" + hp.nu() + "\n");
//    result.append("alpha0=" + clusteringParams.alpha0() + "\n");
//    result.append("discount=" + clusteringParams.discount() + "\n");
//    result.append("nTables=" + clustering.nTables() + "\n");
    return result.toString();
  }

  public int nVariables()
  {
    return clustering.nCustomers() + 4;
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
