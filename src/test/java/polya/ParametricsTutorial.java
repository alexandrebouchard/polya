package polya;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import polya.parametric.HyperParameter;
import polya.parametric.Parameter;
import polya.parametric.Parametrics;
import polya.parametric.SufficientStatistic;
import polya.parametric.TestedModel;
import polya.parametric.normal.CollapsedNIWModel;
import polya.parametric.normal.NIWHyperParameter;
import polya.parametric.normal.NIWParameter;
import polya.parametric.normal.NIWs;
import bayonet.rplot.PlotContour;

import tutorialj.Tutorial;


/**
 * Tests and tutorials for the Bayesian parametric part of the code base.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 */
public class ParametricsTutorial
{
  /**
   * For the next test case to run, you will need to implement 
   * the NIW conjugacy machinery. The main things to do will
   * be in ``Parametrics``, which contains some behaviors that 
   * applies to all conjugate models, and in ``CollapsedNIWModel``,
   * which contains behaviors specific to NIW.
   */
  @Tutorial(showSource = false, nextStep = Parametrics.class)
  @Test
  public void runTests()
  {
    // Accuracy of reconstructions on simulated data
    Random rand = new Random(1);
    TestedModel model = CollapsedNIWModel.instance;
    NIWHyperParameter hp = NIWHyperParameter.withDimensionality(2);
    testParametricModel(rand, model, hp);
    System.out.println();
    
    // Visualization of the predictive
    visualize(rand, hp);
  }
  
  /**
   * ### Expected results of test cases.
   * 
   * After you implement the above mentioned functions, in the first test you
   * should see the average distance between the inferred (MAP) parameters and
   * the generated true ones decrease as the size of the generated dataset increases.
   * It should get down to a distance of  about 3. Note that these distances are
   * fairly large because they are max norms, and the hyperparameters are picked
   * such that the distribution on parameters is vague (more specifically, by picking 
   * 
   */
  @Tutorial(showSource = false, showLink = true)
  public static void testParametricModel(Random rand, TestedModel model, HyperParameter initialHP)
  {
    for (int datasetSize = 10; datasetSize < 1000000; datasetSize *= 10)
    {
      SummaryStatistics distanceStatistics = new SummaryStatistics();
      for (int nReplicates = 0; nReplicates < 100; nReplicates++)
      {
        Pair<Parameter, SufficientStatistic> generatedData = model.generateData(rand, initialHP, datasetSize);
        Parameter trueParam = generatedData.getLeft();
        SufficientStatistic data = generatedData.getRight();
        HyperParameter updatedHP = model.update(initialHP, data);
        Parameter map = model.maximumAPosteriori(updatedHP);
        distanceStatistics.addValue(model.distance(trueParam, map));
      }
      System.out.println(
          "dataSize=" + datasetSize + ", " +
          "distanceMean=" + distanceStatistics.getMean() + ", " +
          "distanceSD=" + distanceStatistics.getStandardDeviation());
    }
  }
  

  
  public static void visualize(Random rand, NIWHyperParameter initialHP)
  {
    TestedModel model = CollapsedNIWModel.instance;
    for (int datasetSize = 10; datasetSize < 1000000; datasetSize *= 10)
    {
      SummaryStatistics distanceStatistics = new SummaryStatistics();
      for (int nReplicates = 0; nReplicates < 3; nReplicates++)
      {
        Pair<Parameter, SufficientStatistic> generatedData = 
          model.generateData(rand, initialHP, datasetSize);
        NIWParameter trueParam = (NIWParameter) generatedData.getLeft();
        SufficientStatistic data = generatedData.getRight();
        HyperParameter updatedHP = model.update(initialHP, data);
        Parameter map = model.maximumAPosteriori(updatedHP);
        distanceStatistics.addValue(model.distance(trueParam, map));
        NIWHyperParameter nhp = (NIWHyperParameter) updatedHP;
        System.out.println("size=" + datasetSize + ",rep=" + nReplicates + ",trueMean=" + Arrays.toString(trueParam.getMeanParameter().getMatrix().getData()));
        PlotContour pc = PlotContour.fromFunction(NIWs.logMarginalAsFunctionOfData(nhp));
        pc.min_x = -100;
        pc.max_x = 100;
        pc.min_y = -100;
        pc.max_y = 100;
        pc.toPDF(new File("/Users/bouchard/temp/contour,size=" + datasetSize + ",rep=" + nReplicates + ".pdf"));
      }
    }
  }

}
