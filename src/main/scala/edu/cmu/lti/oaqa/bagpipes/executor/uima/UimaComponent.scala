package edu.cmu.lti.oaqa.bagpipes.executor.uima

import org.apache.uima.jcas.JCas
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory
import edu.cmu.lti.oaqa.bagpipes.configuration.Parameters.Parameter
import edu.cmu.lti.oaqa.bagpipes.configuration.Descriptors._
import UimaComponent._
import edu.cmu.lti.oaqa.bagpipes.executor.ExecutableComponent
import org.apache.uima.fit.factory.JCasFactory
import org.apache.uima.util.CasCopier
import java.util.Arrays
import java.util.HashMap
import edu.cmu.lti.oaqa.bagpipes.configuration.Parameters.ListParameter
import edu.cmu.lti.oaqa.bagpipes.configuration.Parameters.MapParameter
abstract class UimaComponent(execDesc: ExecutableConf) extends ExecutableComponent[JCas] {
  protected final val typeSysDesc = getTypeSystem()
  protected final val className = execDesc.getClassName
  protected final val params: List[Object] = params2ObjectList(execDesc.getParams)

  final override def execute(input: JCas) = {
    var dupJCas = JCasFactory.createJCas(typeSysDesc)
    CasCopier.copyCas(input.getCas(), dupJCas.getCas(), true)
    executeComponent(dupJCas)
    dupJCas
  }

  def executeComponent(input: JCas): Unit
}

object UimaComponent {
  // Create a type system description singleton
  /**
   * Get the type system description using uimaFIT.
   *
   * Type system descriptors (*.xml) must be in "types/" to be
   * included.
   *
   * @return TypeSystemDescription for the pipeline
   */
  def getTypeSystem() = {
    System.setProperty("org.uimafit.type.import_pattern", "classpath*:types/*.xml")
    TypeSystemDescriptionFactory.createTypeSystemDescription()
  }

  /**
   * Convert a list of parameter tuples into a flat list of Object's.
   * Be sure to unpack the list in the function call with ":_*"!
   *
   * @param params
   * 	The map of parameters, i.e. key-value pairs
   * @return
   * 	List of Objects that can be used in a uimaFIT analysis engine description factory method
   */
  private def params2ObjectList(params: Map[String, Parameter]): List[Object] = params.toList.flatMap { case (k, v) => k :: scala2JavaType(v) :: Nil }

  implicit def scala2JavaType(value: Parameter): Object = value match {
    case ListParameter(pList) => Arrays.asList(pList.map(_.getElem): _*)
    case MapParameter(pMap) =>
      new HashMap[String, Object]() {
        pMap.foreach { case (k: String, v) => put(k, v.getElem.asInstanceOf[Object]) }
      }
    case o => o.getElem.asInstanceOf[Object]
  }
}