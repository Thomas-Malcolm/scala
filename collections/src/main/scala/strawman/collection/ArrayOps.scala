package strawman
package collection

import scala.{AnyVal, Array, Char, Int, throws, Boolean, Serializable, Unit, `inline`, Option, Some, None, PartialFunction}
import java.lang.String
import mutable.ArrayBuilder
import immutable.ImmutableArray
import scala.reflect.ClassTag
import scala.math.{max, min}

object ArrayOps {
  /** A lazy filtered array. No filtering is applied until one of `foreach`, `map` or `flatMap` is called. */
  class WithFilter[A](p: A => Boolean, xs: Array[A]) {

    private[this] implicit def elemTag: ClassTag[A] = ClassTag(xs.getClass.getComponentType)

    /** Apply `f` to each element for its side effects.
      * Note: [U] parameter needed to help scalac's type inference.
      */
    def foreach[U](f: A => U): Unit = {
      val len = xs.length
      var i = 0
      while(i < len) {
        val x = xs(i)
        if(p(x)) f(xs(i))
        i += 1
      }
    }

    /** Builds a new array by applying a function to all elements of this array.
      *
      *  @param f      the function to apply to each element.
      *  @tparam B     the element type of the returned array.
      *  @return       a new aray resulting from applying the given function
      *                `f` to each element of this array and collecting the results.
      */
    def map[B: ClassTag](f: A => B): Array[B] = {
      val b = ArrayBuilder.make[B]()
      var i = 0
      while (i < xs.length) {
        val x = xs(i)
        if(p(x)) b += f(x)
        i = i + 1
      }
      b.result()
    }

    /** Builds a new array by applying a function to all elements of this array
      * and using the elements of the resulting collections.
      *
      *  @param f      the function to apply to each element.
      *  @tparam B     the element type of the returned array.
      *  @return       a new array resulting from applying the given collection-valued function
      *                `f` to each element of this array and concatenating the results.
      */
    def flatMap[B: ClassTag](f: A => IterableOnce[B]): Array[B] = {
      val b = ArrayBuilder.make[B]()
      var i = 0
      while(i < xs.length) {
        val x = xs(i)
        if(p(x)) b ++= f(xs(i))
        i += 1
      }
      b.result()
    }

    def flatMap[BS, B](f: A => BS)(implicit asIterable: BS => Iterable[B], m: ClassTag[B]): Array[B] =
      flatMap[B](x => asIterable(f(x)))

    /** Creates a new non-strict filter which combines this filter with the given predicate. */
    def withFilter(q: A => Boolean): WithFilter[A] = new WithFilter[A](a => p(a) && q(a), xs)
  }
}

/** This class serves as a wrapper for `Array`s with many of the operations found in
  *  indexed sequences. Where needed, instances of arrays are implicitly converted
  *  into this class. There is generally no reason to create an instance explicitly or use
  *  an `ArrayOps` type. It is better to work with plain `Array` types instead and rely on
  *  the implicit conversion to `ArrayOps` when calling a method (which does not actually
  *  allocate an instance of `ArrayOps` because it is a value class).
  *
  *  Neither Array` nor `ArrayOps` are proper collection types
  *  (i.e. they do not extend `Iterable` or even `IterableOnce`). `WrappedArray` and
  *  `ImmutableArray` serve this purpose.
  *
  *  The difference between this class and `WrappedArray` and `ImmutableArray` is that calling
  *  transformer methods such as `filter` and `map` will yield an array, whereas a `WrappedArray`
  *  will remain a `WrappedArray`.
  *
  *  @since 2.8
  *
  *  @tparam A   type of the elements contained in this array.
  */
final class ArrayOps[A](val xs: Array[A]) extends AnyVal {

  private[this] implicit def elemTag: ClassTag[A] = ClassTag(xs.getClass.getComponentType)

  /** The size of this array.
    *
    *  @return    the number of elements in this array.
    */
  @`inline` def size: Int = xs.length

  /** Tests whether the array is empty.
    *
    *  @return    `true` if the array contains no elements, `false` otherwise.
    */
  def isEmpty: Boolean = xs.length == 0

  /** Tests whether the array is not empty.
    *
    *  @return    `true` if the array contains at least one element, `false` otherwise.
    */
  def nonEmpty: Boolean = xs.length != 0

  /** Selects the first element of this array.
    *
    *  @return  the first element of this array.
    *  @throws NoSuchElementException if the array is empty.
    */
  def head: A = xs.apply(0)

  /** Selects the last element.
    *
    * @return The last element of this array.
    * @throws NoSuchElementException If the array is empty.
    */
  def last: A = xs.apply(xs.length-1)

  /** Optionally selects the first element.
    *
    *  @return  the first element of this array if it is nonempty,
    *           `None` if it is empty.
    */
  def headOption: Option[A] = if(isEmpty) None else Some(head)

  /** Optionally selects the last element.
    *
    *  @return  the last element of this array$ if it is nonempty,
    *           `None` if it is empty.
    */
  def lastOption: Option[A] = if(isEmpty) None else Some(last)

  /** Selects an interval of elements. The returned array is made up
    * of all elements `x` which satisfy the invariant:
    * {{{
    *   from <= indexOf(x) < until
    * }}}
    *
    *  @param from   the lowest index to include from this array.
    *  @param until  the lowest index to EXCLUDE from this array.
    *  @return  an array containing the elements greater than or equal to
    *           index `from` extending up to (but not including) index `until`
    *           of this array.
    */
  def slice(from: Int, until: Int): Array[A] = {
    val lo = max(from, 0)
    val hi = min(until, xs.length)
    val len = max(hi - lo, 0)
    val res = new Array[A](len)
    if(len > 0) Array.copy(xs, lo, res, 0, len)
    res
  }

  /** The rest of the array without its first element. */
  def tail: Array[A] = slice(1, xs.length)

  /** The initial part of the array without its last element. */
  def init: Array[A] = slice(0, xs.length-1)

  /** An array containing the first `n` elements of this array. */
  def take(n: Int): Array[A] = slice(0, min(n, xs.length))

  /** The rest of the array without its `n` first elements. */
  def drop(n: Int): Array[A] = slice(n, xs.length - max(n, 0))

  /** An array containing the last `n` elements of this array. */
  def takeRight(n: Int): Array[A] = drop(xs.length - max(n, 0))

  /** The rest of the array without its `n` last elements. */
  def dropRight(n: Int): Array[A] = take(xs.length - max(n, 0))

  /** Splits this array into two at a given position.
    * Note: `c splitAt n` is equivalent to `(c take n, c drop n)`.
    *
    *  @param n the position at which to split.
    *  @return  a pair of arrayss consisting of the first `n`
    *           elements of this array, and the other elements.
    */
  def splitAt(n: Int): (Array[A], Array[A]) = (take(n), drop(n))

  /** A pair of, first, all elements that satisfy prediacte `p` and, second, all elements that do not. */
  def partition(p: A => Boolean): (Array[A], Array[A]) = {
    var res1, res2 = ArrayBuilder.make[A]()
    var i = 0
    while(i < xs.length) {
      val x = xs(i)
      (if(p(x)) res1 else res2) += x
      i += 1
    }
    (res1.result(), res2.result())
  }

  /** Returns a new array with the elements in reversed order. */
  def reverse: Array[A] = {
    var len = xs.length
    var res = new Array[A](len)
    var i = 0
    while(i < len) {
      res(len-i-1) = xs(i)
      i += 1
    }
    res
  }

  /** Selects all elements of this array which satisfy a predicate.
    *
    *  @param p  the predicate used to test elements.
    *  @return   a new array consisting of all elements of this array that satisfy the given predicate `p`.
    */
  def filter(p: A => Boolean): Array[A] = {
    var res = ArrayBuilder.make[A]()
    var i = 0
    while(i < xs.length) {
      val x = xs(i)
      if(p(x)) res += x
      i += 1
    }
    res.result()
  }

  /** Creates a non-strict filter of this array.
    *
    *  Note: the difference between `c filter p` and `c withFilter p` is that
    *        the former creates a new array, whereas the latter only
    *        restricts the domain of subsequent `map`, `flatMap`, `foreach`,
    *        and `withFilter` operations.
    *
    *  @param p   the predicate used to test elements.
    *  @return    an object of class `ArrayOps.WithFilter`, which supports
    *             `map`, `flatMap`, `foreach`, and `withFilter` operations.
    *             All these operations apply to those elements of this array
    *             which satisfy the predicate `p`.
    */
  def withFilter(p: A => Boolean): ArrayOps.WithFilter[A] = new ArrayOps.WithFilter[A](p, xs)

  /** Finds index of the first element satisfying some predicate after or at some start index.
    *
    *  @param   p     the predicate used to test elements.
    *  @param   from  the start index
    *  @return  the index `>= from` of the first element of this array that satisfies the predicate `p`,
    *           or `-1`, if none exists.
    */
  def indexWhere(f: A => Boolean, from: Int = 0): Int = {
    var i = from
    while(i < xs.length) {
      if(f(xs(i))) return i
      i += 1
    }
    -1
  }

  /** Finds the first element of the array satisfying a predicate, if any.
    *
    *  @param p       the predicate used to test elements.
    *  @return        an option value containing the first element in the array
    *                 that satisfies `p`, or `None` if none exists.
    */
  def find(f: A => Boolean): Option[A] = {
    var idx = indexWhere(f)
    if(idx == -1) None else Some(xs(idx))
  }

  /** Tests whether a predicate holds for at least one element of this array.
    *
    *  @param   p     the predicate used to test elements.
    *  @return        `true` if the given predicate `p` is satisfied by at least one element of this array, otherwise `false`
    */
  def exists(f: A => Boolean): Boolean = indexWhere(f) >= 0

  /** Tests whether a predicate holds for all elements of this array.
    *
    *  @param   p     the predicate used to test elements.
    *  @return        `true` if this array is empty or the given predicate `p`
    *                 holds for all elements of this array, otherwise `false`.
    */
  def forall(f: A => Boolean): Boolean = {
    var i = 0
    while(i < xs.length) {
      if(!f(xs(i))) return false
      i += 1
    }
    true
  }

  /** Applies a binary operator to a start value and all elements of this array,
    * going left to right.
    *
    *  @param   z    the start value.
    *  @param   op   the binary operator.
    *  @tparam  B    the result type of the binary operator.
    *  @return  the result of inserting `op` between consecutive elements of this array,
    *           going left to right with the start value `z` on the left:
    *           {{{
    *             op(...op(z, x_1), x_2, ..., x_n)
    *           }}}
    *           where `x,,1,,, ..., x,,n,,` are the elements of this array.
    *           Returns `z` if this array is empty.
    */
  def foldLeft[B](z: B)(op: (B, A) => B): B = {
    var v = z
    var i = 0
    while(i < xs.length) {
      v = op(v, xs(i))
      i += 1
    }
    v
  }

  /** Applies a binary operator to all elements of this array and a start value,
    * going right to left.
    *
    *  @param   z    the start value.
    *  @param   op   the binary operator.
    *  @tparam  B    the result type of the binary operator.
    *  @return  the result of inserting `op` between consecutive elements of this array,
    *           going right to left with the start value `z` on the right:
    *           {{{
    *             op(x_1, op(x_2, ... op(x_n, z)...))
    *           }}}
    *           where `x,,1,,, ..., x,,n,,` are the elements of this array.
    *           Returns `z` if this array is empty.
    */
  def foldRight[B](z: B)(op: (A, B) => B): B = {
    var v = z
    var i = xs.length - 1
    while(i >= 0) {
      v = op(xs(i), v)
      i -= 1
    }
    v
  }

  /** Builds a new array by applying a function to all elements of this array.
    *
    *  @param f      the function to apply to each element.
    *  @tparam B     the element type of the returned array.
    *  @return       a new aray resulting from applying the given function
    *                `f` to each element of this array and collecting the results.
    */
  def map[B : ClassTag](f: A => B): Array[B] = {
    var res = new Array[B](xs.length)
    var i = 0
    while (i < xs.length) {
      res(i) = f(xs(i))
      i = i + 1
    }
    res
  }

  def mapInPlace(f: A => A): Array[A] = {
    var i = 0
    while (i < xs.length) {
      xs.update(i, f(xs(i)))
      i = i + 1
    }
    xs
  }

  /** Builds a new array by applying a function to all elements of this array
    * and using the elements of the resulting collections.
    *
    *  @param f      the function to apply to each element.
    *  @tparam B     the element type of the returned array.
    *  @return       a new array resulting from applying the given collection-valued function
    *                `f` to each element of this array and concatenating the results.
    */
  def flatMap[B: ClassTag](f: A => IterableOnce[B]): Array[B] = {
    val b = ArrayBuilder.make[B]()
    var i = 0
    while(i < xs.length) {
      b ++= f(xs(i))
      i += 1
    }
    b.result()
  }

  def flatMap[BS, B](f: A => BS)(implicit asIterable: BS => Iterable[B], m: ClassTag[B]): Array[B] =
    flatMap[B](x => asIterable(f(x)))

  /** Flattens a two-dimensional array by concatenating all its rows
    *  into a single array.
    *
    *  @tparam B         Type of row elements.
    *  @param asIterable A function that converts elements of this array to rows - Iterables of type `B`.
    *  @return           An array obtained by concatenating rows of this array.
    */
  def flatten[B](implicit asIterable: A => strawman.collection.Iterable[B], m: ClassTag[B]): Array[B] = {
    val b = ArrayBuilder.make[B]()
    val sizes = map {
      case is: IndexedSeq[_] => is.size
      case _ => 0
    }
    b.sizeHint(sizes.sum)
    for (xs <- this)
      b ++= asIterable(xs)
    b.result()
  }

  /** Builds a new array by applying a partial function to all elements of this array
    * on which the function is defined.
    *
    *  @param pf     the partial function which filters and maps the array.
    *  @tparam B     the element type of the returned array.
    *  @return       a new array resulting from applying the given partial function
    *                `pf` to each element on which it is defined and collecting the results.
    *                The order of the elements is preserved.
    */
  def collect[B : ClassTag](f: PartialFunction[A, B]): Array[B] = {
    var i = 0
    var matched = true
    def d(x: A): B = {
      matched = false
      null.asInstanceOf[B]
    }
    val b = ArrayBuilder.make[B]()
    while(i < xs.length) {
      matched = true
      val v = f.applyOrElse(xs(i), d)
      if(matched) b += v
      i += 1
    }
    b.result()
  }

  @`inline` final def ++[B >: A : ClassTag](xs: Iterable[B]): Array[B] = appendedAll(xs)

  /** Returns an array formed from this array and another iterable collection
    * by combining corresponding elements in pairs.
    * If one of the two collections is longer than the other, its remaining elements are ignored.
    *
    *  @param   that  The iterable providing the second half of each result pair
    *  @tparam  B     the type of the second half of the returned pairs
    *  @return        a new array containing pairs consisting of corresponding elements of this array and `that`.
    *                 The length of the returned array is the minimum of the lengths of this array and `that`.
    */
  def zip[B: ClassTag](that: Iterable[B]): Array[(A, B)] = {
    val b = new ArrayBuilder.ofRef[(A, B)]
    val k = that.knownSize
    b.sizeHint(if(k >= 0) min(k, xs.length) else xs.length)
    var i = 0
    val it = that.iterator()
    while(i < xs.length && it.hasNext) {
      b += ((xs(i), it.next()))
      i += 1
    }
    b.result()
  }

  /** Zips this array with its indices.
    *
    *  @return   A new array containing pairs consisting of all elements of this array paired with their index.
    *            Indices start at `0`.
    */
  def zipWithIndex: Array[(A, Int)] = {
    val b = new ArrayBuilder.ofRef[(A, Int)]
    b.sizeHint(xs.length)
    var i = 0
    while(i < xs.length) {
      b += ((xs(i), i))
      i += 1
    }
    b.result()
  }

  /** A copy of this array with an element appended. */
  def appended[B >: A : ClassTag](x: B): Array[B] = {
    val dest = new Array[B](xs.length + 1)
    Array.copy(xs, 0, dest, 0, xs.length)
    dest(xs.length) = x
    dest
  }

  @`inline` final def :+ [B >: A : ClassTag](x: B): Array[B] = appended(x)

  /** A copy of this array with an element prepended. */
  def prepended[B >: A : ClassTag](x: B): Array[B] = {
    val dest = new Array[B](xs.length + 1)
    dest(0) = x
    Array.copy(xs, 0, dest, 1, xs.length)
    dest
  }

  @`inline` final def +: [B >: A : ClassTag](x: B): Array[B] = prepended(x)

  /** A copy of this array with all elements of a collection prepended. */
  def prependedAll[B >: A : ClassTag](prefix: Iterable[B]): Array[B] = prefix match {
    case bs: ImmutableArray[B] =>
      val dest = Array.ofDim[B](xs.length + bs.length)
      Array.copy(bs.unsafeArray, 0, dest, xs.length, bs.length)
      Array.copy(xs, 0, dest, 0, xs.length)
      dest
    case _ =>
      val b = ArrayBuilder.make[B]()
      val k = prefix.knownSize
      if(k >= 0) b.sizeHint(k + xs.length)
      b.addAll(prefix)
      if(k < 0) b.sizeHint(b.length + xs.length)
      b.addAll(xs)
      b.result()
  }

  @`inline` final def ++: [B >: A : ClassTag](prefix: Iterable[B]): Array[B] = prependedAll(prefix)

  /** A copy of this array with all elements of a collection appended. */
  def appendedAll[B >: A : ClassTag](suffix: Iterable[B]): Array[B] = suffix match {
    case bs: ImmutableArray[B] =>
      val dest = Array.ofDim[B](xs.length + bs.length)
      Array.copy(xs, 0, dest, 0, xs.length)
      Array.copy(bs.unsafeArray, 0, dest, xs.length, bs.length)
      dest
    case _ =>
      val b = ArrayBuilder.make[B]()
      val k = suffix.knownSize
      if(k >= 0) b.sizeHint(k + xs.length)
      b.addAll(xs)
      b.addAll(suffix)
      b.result()
  }

  @`inline` final def :++ [B >: A : ClassTag](suffix: Iterable[B]): Array[B] = appendedAll(suffix)

  @`inline` final def concat[B >: A : ClassTag](suffix: Iterable[B]): Array[B] = appendedAll(suffix)

  /** Returns a copy of this array with patched values.
    * Patching at negative indices is the same as patching starting at 0.
    * Patching at indices at or larger than the length of the original array appends the patch to the end.
    * If more values are replaced than actually exist, the excess is ignored.
    *
    *  @param from       The start index from which to patch
    *  @param other      The patch values
    *  @param replaced   The number of values in the original array that are replaced by the patch.
    */
  def patch[B >: A : ClassTag](from: Int, other: IterableOnce[B], replaced: Int): Array[B] = {
    val b = ArrayBuilder.make[B]()
    val k = other.knownSize
    if(k >= 0) b.sizeHint(xs.length + k - replaced)
    val chunk1 = if(from > 0) min(from, xs.length) else 0
    if(chunk1 > 0) b.addAll(xs, 0, chunk1)
    b ++= other
    val remaining = xs.length - chunk1 - replaced
    if(remaining > 0) b.addAll(xs, xs.length - remaining, remaining)
    b.result()
  }

  /** Converts an array of pairs into an array of first elements and an array of second elements.
    *
    *  @tparam A1    the type of the first half of the element pairs
    *  @tparam A2    the type of the second half of the element pairs
    *  @param asPair an implicit conversion which asserts that the element type
    *                of this Array is a pair.
    *  @param ct1    a class tag for `A1` type parameter that is required to create an instance
    *                of `Array[A1]`
    *  @param ct2    a class tag for `A2` type parameter that is required to create an instance
    *                of `Array[A2]`
    *  @return       a pair of Arrays, containing, respectively, the first and second half
    *                of each element pair of this Array.
    */
  def unzip[A1, A2](implicit asPair: A => (A1, A2), ct1: ClassTag[A1], ct2: ClassTag[A2]): (Array[A1], Array[A2]) = {
    val a1 = new Array[A1](xs.length)
    val a2 = new Array[A2](xs.length)
    var i = 0
    while (i < xs.length) {
      val e = asPair(xs(i))
      a1(i) = e._1
      a2(i) = e._2
      i += 1
    }
    (a1, a2)
  }

  /** Converts an array of triples into three arrays, one containing the elements from each position of the triple.
    *
    *  @tparam A1      the type of the first of three elements in the triple
    *  @tparam A2      the type of the second of three elements in the triple
    *  @tparam A3      the type of the third of three elements in the triple
    *  @param asTriple an implicit conversion which asserts that the element type
    *                  of this Array is a triple.
    *  @param ct1      a class tag for T1 type parameter that is required to create an instance
    *                  of Array[T1]
    *  @param ct2      a class tag for T2 type parameter that is required to create an instance
    *                  of Array[T2]
    *  @param ct3      a class tag for T3 type parameter that is required to create an instance
    *                  of Array[T3]
    *  @return         a triple of Arrays, containing, respectively, the first, second, and third
    *                  elements from each element triple of this Array.
    */
  def unzip3[A1, A2, A3](implicit asTriple: A => (A1, A2, A3), ct1: ClassTag[A1], ct2: ClassTag[A2],
                         ct3: ClassTag[A3]): (Array[A1], Array[A2], Array[A3]) = {
    val a1 = new Array[A1](xs.length)
    val a2 = new Array[A2](xs.length)
    val a3 = new Array[A3](xs.length)
    var i = 0
    while (i < xs.length) {
      val e = xs(i)
      a1(i) = e._1
      a2(i) = e._2
      a3(i) = e._3
      i += 1
    }
    (a1, a2, a3)
  }

  /** Transposes a two dimensional array.
    *
    *  @tparam B       Type of row elements.
    *  @param asArray  A function that converts elements of this array to rows - arrays of type `B`.
    *  @return         An array obtained by replacing elements of this arrays with rows the represent.
    */
  def transpose[B](implicit asArray: A => Array[B]): Array[Array[B]] = {
    val aClass = xs.getClass.getComponentType
    val bb = Array.newBuilder[Array[B]](ClassTag[Array[B]](aClass))
    if (xs.length == 0) bb.result()
    else {
      def mkRowBuilder() = Array.newBuilder[B](ClassTag[B](aClass.getComponentType))
      val bs = asArray(xs(0)) map ((x: B) => mkRowBuilder())
      var j = 0
      for (xs <- this) {
        var i = 0
        for (x <- asArray(xs)) {
          bs(i) += x
          i += 1
        }
      }
      for (b <- bs) bb += b.result()
      bb.result()
    }
  }

  /** Apply `f` to each element for its side effects.
    * Note: [U] parameter needed to help scalac's type inference.
    */
  def foreach[U](f: A => U): Unit = {
    val len = xs.length
    var i = 0
    while(i < len) {
      f(xs(i))
      i += 1
    }
  }
}
