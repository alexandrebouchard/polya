package polya.mcmc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


import com.beust.jcommander.internal.Lists;


/**
 * An alternation of MH kernels.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class MHAlternation
{
  private List<RealVariableMHMove> moves = Lists.newArrayList();
  
  /**
   * Add a variable connected to the provided factors to resample.
   * 
   * WARNING: make sure you provide all the connected factors.
   * 
   * @param variable
   * @param connectedFactors
   */
  public void addRealNodeToResample(RealVariable variable, Factor ... connectedFactors)
  {
    RealVariableMHMove move = new RealVariableMHMove(variable, Arrays.asList(connectedFactors));
    moves.add(move);
  }
  
  /**
   * Add a variable connected to a prior factor, plus some other factors.
   * 
   * WARNING: make sure you provide all the connected factors.
   * 
   * @param variable
   * @param prior
   * @param otherFactors
   */
  @SuppressWarnings("unchecked")
  public void addRealNodeToResampleWithPrior(RealVariable variable, RealNodePrior prior, Factor ... otherFactors)
  {
    prior.setVariable(variable);
    @SuppressWarnings("rawtypes")
    List allFactors = Lists.newArrayList();
    allFactors.addAll(Arrays.asList(otherFactors));
    allFactors.add(prior);
    RealVariableMHMove move = new RealVariableMHMove(variable, allFactors);
    moves.add(move);
  }
  
  /**
   * Reshuffle the order of the variables and do one round of sampling.
   * 
   * @param rand
   */
  public void sampleOneRound(Random rand)
  {
    Collections.shuffle(moves, rand);
    for (RealVariableMHMove move : moves)
      move.sample(rand);
  }

  public int nVariables()
  {
    return moves.size();
  }
}
