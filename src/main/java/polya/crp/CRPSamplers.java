package polya.crp;

import java.util.List;
import java.util.Random;

import bayonet.distributions.Multinomial;

import polya.crp.utils.ClusterId;
import polya.parametric.CollapsedConjugateModel;
import polya.parametric.HyperParameter;
import polya.parametric.Parametrics;
import polya.parametric.SufficientStatistic;
import tutorialj.Tutorial;


/**
 * Samplers for CRPs.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class CRPSamplers
{
  /**
   * ### Function to implement in this part
   * 
   * The main function to implement, CRPSamplers.gibbs(),
   * should perform a single Gibbs step for the provided customer.
   * 
   * The probability of insertion at each table should combine 
   * the prior (via the provided PYPrior) and the likelihood (via 
   * the provided 
   * CollapsedConjugateModel and HyperParameter).
   * 
   * To make sure you are avoiding underflows, have a look 
   * at the utilities in bayonet.distributions.Multinomial
   */
  @Tutorial(showSource = false, showLink = true, nextStep = CRPMain.class)
  public static void gibbs(
      Random rand, 
      Integer customer,
      CRPState state, 
      HyperParameter hp, 
      CollapsedConjugateModel collapsedModel,
      PYPrior prior)
  {
    /* startRem throw new RuntimeException(); */ 
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
    
    Multinomial.expNormalize(logUnnormalizedPrs);
    
    // sample
    int sampledIndex = Multinomial.sampleMultinomial(rand , logUnnormalizedPrs);
    
    // do the assignment
    if (sampledIndex == createTableIndex)
      state.addCustomerToNewTable(customer);
    else
      state.addCustomerToExistingTable(customer, existingTables.get(sampledIndex));
    /* endRem */
  }
}
