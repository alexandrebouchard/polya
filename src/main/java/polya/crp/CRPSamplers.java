package polya.crp;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.special.Gamma;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Optional;
import com.sun.media.jai.mlib.MlibHistogramRIF;

import polya.mcmc.Factor;
import polya.mcmc.RealVariable;
import polya.parametric.HyperParameter;
import polya.parametric.Parametrics;
import polya.parametric.SufficientStatistic;
import polya.parametric.normal.CollapsedNIWModel;



public class CRPSamplers
{
  public static void gibbs(
      Random rand, 
      Integer customer,
      CRPState state, 
      HyperParameter hp, 
      CollapsedNIWModel collapsedModel,
      PYPrior prior)
  {
    state.removeCustomer(customer);
    // consider all the way to re-insert them
    int nOutcomes = state.nTables() + 1;
    List<ClusterId> existingTables = state.getAllClusterIds();
    double [] logUnnormalizedPrs = new double[nOutcomes];
    SufficientStatistic currentCustomer = state.getCustomerStatistic(customer);
    for (int i = 0; i < state.nTables(); i++)
    {
      ClusterId current = existingTables.get(i);
      SufficientStatistic customerAlreadyAtTable = state.getClusterStatistics(current);
      logUnnormalizedPrs[i] = 
        Parametrics.logPredictive(collapsedModel, hp, currentCustomer, customerAlreadyAtTable) // G_0
        + prior.logUnnormalizedPredictive(state.getTable(current).size(), state.nTables()); // table prior probabilities
    }
    int createTableIndex = state.nTables();
    logUnnormalizedPrs[createTableIndex] = 
      Parametrics.logMarginal(collapsedModel, hp, currentCustomer) // G_0
      + prior.logUnnormalizedPredictive(0, state.nTables());
    
    expNormalize(logUnnormalizedPrs);
    
    // sample
    int sampledIndex = sampleMultinomial(rand , logUnnormalizedPrs);
    
    // do the assignment
    if (sampledIndex == createTableIndex)
      state.addCustomerToNewTable(customer);
    else
      state.addCustomerToExistingTable(customer, existingTables.get(sampledIndex));
  }
  
  public static int sampleMultinomial(Random random, double[] probs) 
  {
    double v = random.nextDouble();
    double sum = 0;
    for(int i = 0; i < probs.length; i++) 
    {
      sum += probs[i]; 
      if(v < sum) return i;
    }
    throw new RuntimeException(sum + " < " + v);
  }
  
  /**
   * Destructively normalize and returns the normalization
   * @param data
   * @return The normalization
   */
  public static double normalize(double[] data) 
  {
    double sum = 0;
    for(double x : data) 
      sum += x;
    if (sum != 1.0)
      for(int i = 0; i < data.length; i++) 
        data[i] /= sum;
    return sum;
  }
  
  /**
   * Input: log probabilities (unnormalized too), to be destructively exponentiated and normalized
   * Output: normalized probabilities
   * probs actually contains log probabilities; so we can add an arbitrary constant to make
   * the largest log prob 0 to prevent overflow problems
   * @param logProbs
   * @return The normalization in log space
   */
  public static double expNormalize(double[] logProbs) 
  {
    double max = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < logProbs.length; i++)
      max = Math.max(max, logProbs[i]);
    for(int i = 0; i < logProbs.length; i++)
      logProbs[i] = Math.exp(logProbs[i]-max);
    return max + Math.log(normalize(logProbs));
  }
  

  
  public static class PYPrior
  {
    private double alpha0, discount;
    
    public PYPrior(double alpha0, double discount)
    {
      this.alpha0 = alpha0;
      this.discount = discount;
      checkBounds();
    }

    public void checkBounds()
    {
      if (!inBounds())
        throw new RuntimeException();
    }
    
    public boolean inBounds()
    {
      return discount >= 0 && discount < 1 && alpha0 > -discount;
    }

    public double logUnnormalizedPredictive(int nCustomersAtTable, int nTables)
    {
      if (nCustomersAtTable == 0)
        return Math.log(alpha0 + nTables * discount);
      else
        return Math.log(nCustomersAtTable - discount);
    }
    
    public RealVariable alpha0VariableView()
    {
      return new RealVariable() {
        @Override public void setValue(double newValue) { alpha0 = newValue; }
        @Override  public double getValue()             { return alpha0; }
      };
    }

    public RealVariable discountVariableView()
    {
      return new RealVariable() {
        @Override public void setValue(double newValue) { discount = newValue; }
        @Override  public double getValue()             { return discount; }
      };
    }

    public double discount()
    {
      return discount;
    }

    public double alpha0()
    {
      return alpha0;
    }
  }
}
