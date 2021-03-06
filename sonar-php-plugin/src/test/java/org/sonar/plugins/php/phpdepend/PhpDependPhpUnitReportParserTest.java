/*
 * Sonar PHP Plugin
 * Copyright (C) 2010 Codehaus Sonar Plugins
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.php.phpdepend;

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Builder;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.resources.File;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.php.HasComplexityDistribution;
import org.sonar.plugins.php.api.PhpConstants;
import org.sonar.test.TestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Class PhpDependPhpUnitReportParserTest.
 */
public class PhpDependPhpUnitReportParserTest {

  /** The context. */
  private SensorContext context;

  /** The metric. */
  private Metric metric;

  private Project project;

  private static final String PDEPEND_RESULT = "/org/sonar/plugins/php/phpdepend/sensor/parser/pdepend.xml";
  private static final String PDEPEND_RESULT_SAMEFILE_DIFFERBYFSUFFIX = "/org/sonar/plugins/php/phpdepend/sensor/parser/pdepend.samefiledifferbysuffix.xml";

  /**
   * Inits the result parser.
   */
  private void init(String pdependResultFile) {
    try {
      java.io.File xmlReport = new java.io.File(getClass().getResource(pdependResultFile).toURI());
      context = mock(SensorContext.class);
      project = mock(Project.class);

      ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
      when(project.getFileSystem()).thenReturn(fileSystem);
      when(fileSystem.getSourceDirs()).thenReturn(Arrays.asList(new java.io.File("C:/projets/PHP/Money/Sources/main")));
      when(fileSystem.getTestDirs()).thenReturn(Arrays.asList(new java.io.File("C:/projets/PHP/Money/Sources/test")));

      java.io.File f1 = new java.io.File("C:/projets/PHP/Money/Sources/test/MoneyTest.php");
      java.io.File f2 = new java.io.File("C:/projets/PHP/Money/Sources/main/Money.php");
      java.io.File f3 = new java.io.File("C:/projets/PHP/Money/Sources/main/MoneyBag.php");
      java.io.File f4 = new java.io.File("C:/projets/PHP/Money/Sources/main/Common/IMoney.php");
      java.io.File f5 = new java.io.File("C:/projets/PHP/Money/Sources/main/Money.inc");

      when(fileSystem.mainFiles(PhpConstants.LANGUAGE_KEY)).thenReturn(
          InputFileUtils.create(new java.io.File("C:/projets/PHP/Money/Sources/main"), Arrays.asList(f2, f3, f4, f5)));
      when(fileSystem.testFiles(PhpConstants.LANGUAGE_KEY)).thenReturn(
          InputFileUtils.create(new java.io.File("C:/projets/PHP/Money/Sources/test"), Arrays.asList(f1)));

      Set<Metric> metrics = new HashSet<Metric>();
      metrics.add(metric);
      PhpDependPhpUnitReportParser parser = new PhpDependPhpUnitReportParser(project, context);

      parser.parse(xmlReport);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Should generate functions count measure.
   */
  @Test
  public void shouldNotGenerateMeasureForFileNotInSourceDirectory() {
    metric = CoreMetrics.LINES;
    init(PDEPEND_RESULT);
    File file = new File("d:/projets/PHP/Money/Sources/main/Common/IMoney.php");
    verify(context, never()).saveMeasure(eq(file), eq(metric), anyDouble());
  }

  @Test(expected = SonarException.class)
  public void shouldThrowAnExceptionWhenReportNotFound() {
    project = mock(Project.class);
    PhpDependPhpUnitReportParser parser = new PhpDependPhpUnitReportParser(project, null);
    parser.parse(new java.io.File("path/to/nowhere"));
  }

  @Test(expected = SonarException.class)
  public void shouldFailIfReportInvalid() {
    project = mock(Project.class);
    PhpDependPhpUnitReportParser parser = new PhpDependPhpUnitReportParser(project, null);
    parser.parse(TestUtils.getResource("/org/sonar/plugins/php/phpdepend/sensor/parser/pdepend-invalid.xml"));
  }

  /**
   * Should not stop if a file name is empty
   */
  @Test
  public void shouldNotStopIfFilenameEmpty() {
    project = mock(Project.class);
    PhpDependPhpUnitReportParser parser = new PhpDependPhpUnitReportParser(project, null);
    parser.parse(TestUtils.getResource("/org/sonar/plugins/php/phpdepend/sensor/parser/pdepend-with-empty-filename.xml"));
  }

  /**
   * Should not throw an exception if metric not found.
   */
  @Test
  public void shouldNotThrowAnExceptionIfMetricNotFound() {
    metric = new Builder("fake", "Fake", ValueType.BOOL).create();
    init(PDEPEND_RESULT);
    verify(context, never()).saveMeasure(eq(metric), anyDouble());
  }

  /**
   * Should generate loc measures.
   */
  @Test
  public void shouldGenerateLocMeasures() {
    metric = CoreMetrics.LINES;
    init(PDEPEND_RESULT);
    verify(context).saveMeasure(new File("Money.php"), metric, 188.0);
    verify(context).saveMeasure(new File("MoneyBag.php"), metric, 251.0);
    verify(context).saveMeasure(new File("Common/IMoney.php"), metric, 74.0);
  }

  /**
   * Should generate ncloc measures.
   */
  @Test
  public void shouldGenerateNclocMeasures() {
    metric = CoreMetrics.NCLOC;
    init(PDEPEND_RESULT);
    verify(context).saveMeasure(new File("Money.php"), metric, 94.0);
    verify(context).saveMeasure(new File("MoneyBag.php"), metric, 150.0);
    verify(context).saveMeasure(new File("Common/IMoney.php"), metric, 15.0);
  }

  /**
   * Should generate functions count measure.
   */
  @Test
  public void shouldGenerateFunctionsCountMeasure() {
    metric = CoreMetrics.FUNCTIONS;
    init(PDEPEND_RESULT);
    verify(context).saveMeasure(new File("Money.php"), metric, 17.0);
    verify(context).saveMeasure(new File("MoneyBag.php"), metric, 18.0);
    verify(context).saveMeasure(new File("Common/IMoney.php"), metric, 8.0);
  }

  /**
   * Should generate classes count measure.
   */
  @Test
  public void shouldGenerateClassesCountMeasure() {
    metric = CoreMetrics.CLASSES;
    init(PDEPEND_RESULT);
    verify(context).saveMeasure(new File("Money.php"), metric, 2.0);
    verify(context).saveMeasure(new File("MoneyBag.php"), metric, 1.0);
    verify(context).saveMeasure(new File("Common/IMoney.php"), metric, 1.0);
  }

  /**
   * Should generate complexity measure.
   */
  @Test
  public void shouldGenerateComplexityMeasure() {
    metric = CoreMetrics.COMPLEXITY;
    init(PDEPEND_RESULT);
    verify(context).saveMeasure(new File("Money.php"), metric, 22.0);
    verify(context).saveMeasure(new File("MoneyBag.php"), metric, 39.0);
    verify(context).saveMeasure(new File("Common/IMoney.php"), metric, 0.0);
  }

  /**
   * Should not generate dir or project measures.
   */
  @Test
  public void shouldNotGenerateDirOrProjectMeasures() {
    metric = CoreMetrics.LINES;
    init(PDEPEND_RESULT);
    verify(context, never()).saveMeasure(eq(metric), anyDouble());
    verify(context, never()).saveMeasure(eq(new org.sonar.api.resources.Directory("Sources/main")), eq(metric), anyDouble());
    verify(context, never()).saveMeasure(eq(new org.sonar.api.resources.Directory("Sources/main/Common")), eq(metric), anyDouble());
  }

  @Test
  public void shouldGenerateValidMeasuresOnSameFileWithDifferentSuffix() {
    metric = CoreMetrics.COMPLEXITY;
    init(PDEPEND_RESULT_SAMEFILE_DIFFERBYFSUFFIX);
    verify(context).saveMeasure(new File("Money.php"), metric, 21.0);
    verify(context).saveMeasure(new File("Money.inc"), metric, 39.0);
  }

  @Test
  /**
   * It was hard to have a seperate test case for class and method complexity.
   * Only assume because saveMeasure() method call arguments types are the same
   */
  public void testShouldGenerateComplexityDistribution() {
    init(PDEPEND_RESULT);

    verify(context).saveMeasure(
        eq(new File("Money.php")),
        (Measure) argThat(new HasComplexityDistribution(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, "0=1;5=0;10=1;20=0;30=0;60=0;90=0"))
        );
    verify(context).saveMeasure(
        eq(new File("Money.php")),
        (Measure) argThat(new HasComplexityDistribution(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=15;2=1;4=1;6=0;8=0;10=0;12=0"))
        );

    verify(context).saveMeasure(
        eq(new File("MoneyBag.php")),
        (Measure) argThat(new HasComplexityDistribution(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, "0=0;5=0;10=0;20=0;30=1;60=0;90=0"))
        );
    verify(context).saveMeasure(
        eq(new File("MoneyBag.php")),
        (Measure) argThat(new HasComplexityDistribution(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=8;2=8;4=0;6=2;8=0;10=0;12=0"))
        );
  }
}
