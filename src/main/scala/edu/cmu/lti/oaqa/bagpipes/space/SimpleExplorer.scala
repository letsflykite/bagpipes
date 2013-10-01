package edu.cmu.lti.oaqa.bagpipes.space
import SimpleExplorer._
import edu.cmu.lti.oaqa.bagpipes.space.ConfigurationSpace._
import edu.cmu.lti.oaqa.bagpipes.configuration.Descriptors.ExecutableConf

/**
 * SimpleExplorer provides a depth/breadth-first (given by the specified
 * `Ordering`) traversal of all the nodes in a configuration space.
 *
 * @author Avner Maiberg (amaiberg@cs.cmu.edu)
 */

protected sealed class SimpleExplorer(order: Ordering) extends Explorer[ExecutableConf] {
  /**
   * Returns either a depth-first or breadth-first ordering of the configuration
   * space as specified by `Ordering`.
   *
   * @oaram initial the initial position, or root in the configuration space
   */
  def from(initial: Tree[ExecutableConf]) = {
    val initialStream = Stream[TreeWithHistory]((initial, Stream()))
    order match {
      case Depth => fromDepth(initialStream) //depth-first
      case Breadth => fromBreadth(initialStream) //breadth-first
    }
  }
  /**
   * Returns a stream of [[edu.cmu.lti.oaqa.bagpipes.space.TreeWithHistory]] in
   * using depth-first search traversal.
   *
   * @oaram initial the initial position (or root) in the configuration space tree.
   */
  private def fromDepth(initial: Stream[TreeWithHistory]): Stream[LeafWithHistory] = initial match {
    case Stream() => Stream() // no more nodes, terminate
    case current @ (Leaf(element), hist) #:: siblings => current #::: fromDepth(siblings) // leaf encountered, visit current, and go to next sibling  
    case current @ (Node(element, children), hist) #:: siblings => { // node encountered, 
      val childrenWithHistory = for (c <- children) yield { (c, hist #::: Stream(Leaf(element))) } // append current to its history and initialize as history of all of the children
      current #::: fromDepth(childrenWithHistory) #::: fromDepth(siblings) //visit current, then visit its children, and then visit its siblings
    }
  }
  /**
   * Returns a stream of [[edu.cmu.lti.oaqa.bagpipes.space.TreeWithHistory]] in
   * using breadth-first search traversal.
   *
   * @oaram initial the initial position (or root) in the configuration space tree.
   */
  private def fromBreadth(initial: Stream[TreeWithHistory]): Stream[LeafWithHistory] = {
    def fromBreadth(initial: Stream[TreeWithHistory], childrenAcc: Stream[TreeWithHistory]): Stream[LeafWithHistory] =
      (initial, childrenAcc) match {
        case (Stream(), Stream()) => Stream() // no more children for breadth-first search, terminate
        case (Stream(), childrenAcc) => fromBreadth(childrenAcc, Stream()) // finished level of breadth-first, move on to next
        case (current @ (Leaf(element), hist) #:: siblings, _) => current #::: fromBreadth(siblings, Stream()) // leaf encountered, visit leaf and move on to next sibling
        case (current @ (Node(element, children), hist) #:: siblings, _) => { // node encountered, visit node 
          val childrenWithHistory = for (c <- children) yield { (c, hist #::: Stream(Leaf(element))) } // append current to its history and initialize as history of all of the children
          current #::: fromBreadth(siblings, childrenAcc #::: childrenWithHistory) //visit node, go to next sibling, and remember children for next breadth-first level
        }
      }
    fromBreadth(initial, Stream())
  }
}

/*Explorer singletons*/
object BreadthExplorer extends SimpleExplorer(Breadth)
object DepthExplorer extends SimpleExplorer(Depth)

/*Enums for selecting either depth-first or breadth-first search*/
object SimpleExplorer {
  sealed trait Ordering
  object Depth extends Ordering
  object Breadth extends Ordering
}