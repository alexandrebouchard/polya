package polya.mcmc;

import java.util.Collection;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;




public class RealVariableMHMove
  {
    final SummaryStatistics acceptanceProbabilities = new SummaryStatistics();
    private final RealVariable variable;
    private final Collection<? extends Factor> connectedFactors;
    
    public RealVariableMHMove(RealVariable variable,
        Collection<? extends Factor> connectedFactors)
    {
      this.variable = variable;
      this.connectedFactors = connectedFactors;
    }

    public void sample(Random rand)
    {
      final double logDensityBefore = computeLogUnnormalizedPotentials();
      final double initialValue = variable.getValue();
      final double proposalDelta = rand.nextGaussian();
      final double newValue = initialValue + proposalDelta;
      variable.setValue(newValue);
      final double logDensityAfter = computeLogUnnormalizedPotentials();
      final double ratio = Math.exp(logDensityAfter - logDensityBefore);
      final boolean accept = rand.nextDouble() < ratio;
      acceptanceProbabilities.addValue(accept ? 1.0 : 0.0);
      if (!accept)
        variable.setValue(initialValue); // reject
    }
    
    private double computeLogUnnormalizedPotentials()
    {
      double result = 0.0;
      for (Factor f : connectedFactors)
        result += f.logUnnormalizedPotential();
//      if (Double.isNaN(result))
//        throw new RuntimeException();
      return result;
    }
  }