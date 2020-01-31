package scala.xml

import scala.xml.NodeSeq.seqToNodeSeq

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.fail

class NodeSeqTest {

  @Test
  def testAppend: Unit = { // Bug #392.
    val a: NodeSeq = <a>Hello</a>
    val b = <b>Hi</b>
    a ++ <b>Hi</b> match {
      case res: NodeSeq => assertEquals(2, res.size)
      case res: Seq[Node] => fail("Should be NodeSeq") // Unreachable code?
    }
    val res: NodeSeq = a ++ b
    val exp = NodeSeq.fromSeq(Seq(<a>Hello</a>, <b>Hi</b>))
    assertEquals(exp, res)
  }

  @Test
  def testAppendedAll: Unit = { // Bug #392.
    val a: NodeSeq = <a>Hello</a>
    val b = <b>Hi</b>
    a :+ <b>Hi</b> match {
      case res: Seq[Node] => assertEquals(2, res.size)
      case res: NodeSeq => fail("Should be Seq[Node]") // Unreachable code?
    }
    val res: NodeSeq = a :+ b
    val exp = NodeSeq.fromSeq(Seq(<a>Hello</a>, <b>Hi</b>))
    assertEquals(exp, res)
  }

  @Test
  def testPrepended: Unit = {
    val a: NodeSeq = <a>Hello</a>
    val b = <b>Hi</b>
    a +: <b>Hi</b> match {
      case res: Seq[NodeSeq] => assertEquals(2, res.size)
      case res: NodeSeq => fail("Should be NodeSeq was Seq[Node]") // Unreachable code?
    }
    val res: Seq[NodeSeq] = a +: b
    val exp = <a>Hello</a><b>Hi</b>
    assertEquals(exp, res)
  }

  @Test
  def testPrependedAll: Unit = {
    val a: NodeSeq = <a>Hello</a>
    val b = <b>Hi</b>
    val c = <c>Hey</c>
    a ++: <b>Hi</b> ++: <c>Hey</c> match {
      case res: Seq[Node] => assertEquals(3, res.size)
      case res: NodeSeq => fail("Should be Seq[Node]") // Unreachable code?
    }
    val res: NodeSeq = a ++: b ++: c
    val exp = NodeSeq.fromSeq(Seq(<a>Hello</a>, <b>Hi</b>, <c>Hey</c>))
    assertEquals(exp, res)
  }

  @Test
  def testMap: Unit = {
    val a: NodeSeq = <a>Hello</a>
    val exp: NodeSeq = Seq(<b>Hi</b>)
    assertEquals(exp, a.map(_ => <b>Hi</b>))
    assertEquals(exp, for { _ <- a } yield { <b>Hi</b> })
  }

  @Test
  def testFlatMap: Unit = {
    val a: NodeSeq = <a>Hello</a>
    val exp: NodeSeq = Seq(<b>Hi</b>)
    assertEquals(exp, a.flatMap(_ => Seq(<b>Hi</b>)))
    assertEquals(exp, for { b <- a; _ <- b } yield { <b>Hi</b> })
    assertEquals(exp, for { b <- a; c <- b; _ <- c } yield { <b>Hi</b> })
  }

  @Test
  def testStringProjection: Unit = {
    val a =
      <a>
        <b>b</b>
        <b>
          <c d="d">
            <e>e</e>
            <e>e</e>
          </c>
          <c>c</c>
        </b>
      </a>
    val res = for {
      b <- a \ "b"
      c <- b.child
      e <- (c \ "e").headOption
    } yield {
      e.text.trim
    }
    assertEquals(Seq("e"), res)
  }
}
