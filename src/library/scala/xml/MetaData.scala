/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$

package scala.xml

import Utility.sbToString
import annotation.tailrec


/**
 * Copyright 2008 Google Inc. All Rights Reserved.
 * @author Burak Emir <bqe@google.com>
 */
object MetaData {

  /**
   * appends all attributes from new_tail to attribs, without attempting to detect
   * or remove duplicates. The method guarantees that all attributes from attribs come before
   * the attributes in new_tail, but does not guarantee to preserve the relative order of attribs.
   * Duplicates can be removed with normalize.
   */
  @tailrec
  def concatenate(attribs: MetaData, new_tail: MetaData): MetaData =
    if (attribs eq Null) new_tail
    else concatenate(attribs.next, attribs copy new_tail)

  /**
   * returns normalized MetaData, with all duplicates removed and namespace prefixes resolved to
   *  namespace URIs via the given scope.
   */
  def normalize(attribs: MetaData, scope: NamespaceBinding): MetaData = {
    def iterate(md: MetaData, normalized_attribs: MetaData, set: Set[String]): MetaData = {
      lazy val key = getUniversalKey(md, scope)
      if (md eq Null) normalized_attribs
      else if (set(key)) iterate(md.next, normalized_attribs, set)
      else iterate(md.next, md copy normalized_attribs, set + key)
    }
    iterate(attribs, Null, Set())
  }

  /**
   * returns key if md is unprefixed, pre+key is md is prefixed
   */
  def getUniversalKey(attrib: MetaData, scope: NamespaceBinding) = attrib match {
    case prefixed: PrefixedAttribute     => scope.getURI(prefixed.pre) + prefixed.key
    case unprefixed: UnprefixedAttribute => unprefixed.key
  }

  /**
   *  returns MetaData with attributes updated from given MetaData
   */
  def update(attribs: MetaData, scope: NamespaceBinding, updates: MetaData): MetaData =
    normalize(concatenate(updates, attribs), scope)

}

/** <p>
 *    This class represents an attribute and at the same time a linked list of attributes.
 *    Every instance of this class is either an instance of UnprefixedAttribute <code>key,value</code>
 *    or an instance of PrefixedAttribute <code>namespace_prefix,key,value</code> or Null, the empty
 *    attribute list. Namespace URIs are obtained by using the namespace scope of the element owning
 *    this attribute (see <code>getNamespace</code>)
 * </p>
 *
 * Copyright 2008 Google Inc. All Rights Reserved.
 * @author Burak Emir <bqe@google.com>
 */
@serializable
abstract class MetaData extends Iterable[MetaData] with Equality
{
  /** Updates this MetaData with the MetaData given as argument. All attributes that occur in updates
   *  are part of the resulting MetaData. If an attribute occurs in both this instance and
   *  updates, only the one in updates is part of the result (avoiding duplicates). For prefixed
   *  attributes, namespaces are resolved using the given scope, which defaults to TopScope.
   *
   *  @param updates MetaData with new and updated attributes
   *  @return a new MetaData instance that contains old, new and updated attributes
   */
  def append(updates: MetaData, scope: NamespaceBinding = TopScope): MetaData =
    MetaData.update(this, scope, updates)

  /**
   * Gets value of unqualified (unprefixed) attribute with given key, null if not found
   *
   * @param  key
   * @return value as Seq[Node] if key is found, null otherwise
   */
  def apply(key: String): Seq[Node]

  /** convenience method, same as <code>apply(namespace, owner.scope, key)</code>.
   *
   *  @param namespace_uri namespace uri of key
   *  @param owner the element owning this attribute list
   *  @param key   the attribute key
   *  @return          ...
   */
  final def apply(namespace_uri: String, owner: Node, key: String): Seq[Node] =
    apply(namespace_uri, owner.scope, key)

  /**
   * Gets value of prefixed attribute with given key and namespace, null if not found
   *
   * @param  namespace_uri namespace uri of key
   * @param  scp a namespace scp (usually of the element owning this attribute list)
   * @param  key to be looked fore
   * @return value as Seq[Node] if key is found, null otherwise
   */
  def apply(namespace_uri:String, scp:NamespaceBinding, k:String): Seq[Node]

  /** returns a copy of this MetaData item with next field set to argument.
   *
   *  @param next ...
   *  @return     ...
   */
  def copy(next: MetaData): MetaData

  /** if owner is the element of this metadata item, returns namespace */
  def getNamespace(owner: Node): String

  def hasNext = (Null != next)

  def length: Int = length(0)

  def length(i: Int): Int = next.length(i + 1)

  def isPrefixed: Boolean

  override def canEqual(other: Any) = other match {
    case _: MetaData  => true
    case _            => false
  }
  override def strict_==(other: Equality) = other match {
    case m: MetaData  => this.toSet == m.toSet
    case _            => false
  }
  def basisForHashCode: Seq[Any] = List(this.toSet)

  /** Returns an iterator on attributes */
  def iterator: Iterator[MetaData] = Iterator.single(this) ++ next.iterator
  override def size: Int = 1 + iterator.length

  /** filters this sequence of meta data */
  override def filter(f: MetaData => Boolean): MetaData =
    if (f(this)) copy(next filter f)
    else next filter f

  /** returns key of this MetaData item */
  def key: String

  /** returns value of this MetaData item */
  def value: Seq[Node]

  /** Returns a String containing "prefix:key" if the first key is
   *  prefixed, and "key" otherwise.
   */
  def prefixedKey = this match {
    case x: Attribute if x.isPrefixed => x.pre + ":" + key
    case _                            => key
  }

  /** Returns a Map containing the attributes stored as key/value pairs.
   */
  def asAttrMap: Map[String, String] =
    iterator map (x => (x.prefixedKey, x.value.text)) toMap

  /** returns Null or the next MetaData item */
  def next: MetaData

  /**
   * Gets value of unqualified (unprefixed) attribute with given key, None if not found
   *
   * @param  key
   * @return value in Some(Seq[Node]) if key is found, None otherwise
   */
  final def get(key: String): Option[Seq[Node]] = Option(apply(key))

  /** same as get(uri, owner.scope, key) */
  final def get(uri: String, owner: Node, key: String): Option[Seq[Node]] =
    get(uri, owner.scope, key)

  /** gets value of qualified (prefixed) attribute with given key.
   *
   * @param  uri namespace of key
   * @param  scope a namespace scp (usually of the element owning this attribute list)
   * @param  key to be looked fore
   * @return value as Some[Seq[Node]] if key is found, None otherwise
   */
  final def get(uri: String, scope: NamespaceBinding, key: String): Option[Seq[Node]] =
    Option(apply(uri, scope, key))

  def toString1(): String = sbToString(toString1)

  // appends string representations of single attribute to StringBuilder
  def toString1(sb: StringBuilder): Unit

  override def toString(): String = sbToString(buildString)

  def buildString(sb: StringBuilder): StringBuilder = {
    sb.append(' ')
    toString1(sb)
    next.buildString(sb)
  }

  /**
   *  @param scope ...
   *  @return      <code>true</code> iff ...
   */
  def wellformed(scope: NamespaceBinding): Boolean

  /**
   *  @param key ...
   *  @return    ...
   */
  def remove(key: String): MetaData

  /**
   *  @param namespace ...
   *  @param scope     ...
   *  @param key       ...
   *  @return          ...
   */
  def remove(namespace: String, scope: NamespaceBinding, key: String): MetaData

  /**
   *  @param namespace ...
   *  @param owner     ...
   *  @param key       ...
   *  @return          ...
   */
  final def remove(namespace: String, owner: Node, key: String): MetaData =
    remove(namespace, owner.scope, key)
}
