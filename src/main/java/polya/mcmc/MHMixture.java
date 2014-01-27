package polya.mcmc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


import com.beust.jcommander.internal.Lists;



public class MHMixture
{
  private List<RealVariableMHMove> moves = Lists.newArrayList();
  
  
  public void addRealNodeToResample(RealVariable variable, Factor ... connectedFactors)
  {
    RealVariableMHMove move = new RealVariableMHMove(variable, Arrays.asList(connectedFactors));
    moves.add(move);
  }
  
  public void addRealNodeToResampleWithPrior(RealVariable variable, RealNodePrior prior, Factor ... otherFactors)
  {
    prior.setVariable(variable);
    List allFactors = Lists.newArrayList();
    allFactors.addAll(Arrays.asList(otherFactors));
    allFactors.add(prior);
    RealVariableMHMove move = new RealVariableMHMove(variable, allFactors);
    moves.add(move);
  }
  
  public void sampleOneRound(Random rand)
  {
    Collections.shuffle(moves, rand);
    for (RealVariableMHMove move : moves)
      move.sample(rand);
  }
}
