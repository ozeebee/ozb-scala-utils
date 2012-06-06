package org.ozb.utils.xml

import scala.xml.Node
import scala.xml.Elem
import scala.xml.UnprefixedAttribute
import scala.xml.Null

object XmlUtils {

	/**
	 * @return true if the given node has an attribute with given name and value
	 * Usage : val targetExists = xml \ "target" exists XmlUtils.attributeEquals("name", targetName)
	 * 	 val node = xml \ "target" find attributeEquals("name", "some name")
	 */
	def attributeEquals(name: String, value: String)(node: Node): Boolean = {  
		node.attribute(name).filter(_.text==value).isDefined 
	}

	/**
	 * Add (or replace) the given attribute with the given value and return
	 * the modified Elem
	 */
	def addAttribute(elm: Elem, name: String, value: String): Elem = {
		elm % new UnprefixedAttribute(name, value, Null)
	}
	
	/**
	 * Add the given newChild node to the n node and returns the result
	 * courtesy of http://stackoverflow.com/questions/2199040/scala-xml-building-adding-children-to-existing-nodes 
	 */
	def addChild(n: Node, newChild: Node): Elem = n match {
		case Elem(prefix, label, attribs, scope, child @ _*) =>
			Elem(prefix, label, attribs, scope, child ++ newChild: _*)
		case _ => sys.error("Can only add children to elements!")
	}

//	def addChild(e: Elem, newChild: Node): Elem = {
//		e.copy(child = e.child ++ newChild)
//	}
	
	/**
	 * Add the given newChildren nodes to the n node and returns the result
	 * courtesy of http://stackoverflow.com/questions/2199040/scala-xml-building-adding-children-to-existing-nodes 
	 */
	def addChildren(n: Node, newChildren: Seq[Node]): Elem = n match {
		case Elem(prefix, label, attribs, scope, child @ _*) =>
			Elem(prefix, label, attribs, scope, child ++ newChildren: _*)
		case _ => sys.error("Can only add children to elements!")
	}
	
//	def addChild(e: Elem, newChildren: Seq[Node]): Elem = {
//		e.copy(child = e.child ++ newChildren)
//	}


}