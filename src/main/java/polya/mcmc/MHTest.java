package polya.mcmc;

import java.util.Collections;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import tutorialj.Tutorial;


/**
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class MHTest
{

  
  /**
   * #### Simple test case for the MH sampling code
   * 
   * Here is a simple test case if you want to test the MH code 
   * without running the whole pipeline (this should output approximately
   * 1/lambda):
   */
  @Tutorial(showLink = true)
  public static void main(String [] args)
  {
    final RealVariable variable = new RealVariableImpl(1.0);
    final double lambda = 5.0;
    final Factor exponentialDist = new Factor() {
      @Override
      public double logUnnormalizedPotential()
      {
        if (variable.getValue() < 0.0)
          return Double.NEGATIVE_INFINITY;
        return -lambda * variable.getValue();
      }
    };
    
    RealVariableMHMove move = new RealVariableMHMove(variable, Collections.singleton(exponentialDist));
    
    SummaryStatistics stat = new SummaryStatistics();
    Random rand = new Random(1);
    for (int i = 0; i < 100000; i++)
    {
      move.sample(rand);
      stat.addValue(variable.getValue());
    }
    System.out.println("EX=" + stat.getMean());
    System.out.println("acceptRate=" + move.acceptanceProbabilities.getMean());
  }
  
  /**
   * A default implementation of RealVariable for simple test cases.
   * 
   * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
   *
   */
  public static class RealVariableImpl implements RealVariable
  {
    private double value;

    /**
     * 
     * @param value
     */
    public RealVariableImpl(double value)
    {
      this.value = value;
    }
    
    /**
     * 
     */
    public double getValue()
    {
      return value;
    }
    
    /**
     * 
     */
    public void setValue(double value)
    {
      this.value = value;
    }
  }
}
