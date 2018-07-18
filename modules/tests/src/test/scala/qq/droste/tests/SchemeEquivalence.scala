package qq.droste
package test

import cats.Functor

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Properties
import org.scalacheck.Prop._

import examples.histo.MakeChange

final class SchemeEquivalence extends Properties("SchemeEquivalence") {

  // TODO: see about generalizing this for testing more scheme equivalences
  trait AlgebraFamily {
    type F[_]
    type R
    type B

    final object implicits {
      implicit def implicitFunctorF: Functor[F] = functorF
      implicit def implicitProjectFR: Project[F, R] = projectFR
      implicit def implicitArbitraryR: Arbitrary[R] = arbitraryR
    }

    def functorF: Functor[F]
    def projectFR: Project[F, R]
    def arbitraryR: Arbitrary[R]

    def cvalgebra: CVAlgebra[F, B]
  }

  object AlgebraFamily {
    case class Default[FF[_], RR, BB](
      genR: Gen[RR],
      cvalgebra: CVAlgebra[FF, BB]
    )(
      implicit
        val functorF: Functor[FF],
        val projectFR: Project[FF, RR]
    ) extends AlgebraFamily {
      type F[A] = FF[A]
      type R = RR
      type B = BB

      val arbitraryR = Arbitrary(genR)
    }
  }

  implicit val arbitraryAlgebraFamily: Arbitrary[AlgebraFamily] =
    Arbitrary(Gen.const(AlgebraFamily.Default(
      Gen.choose(0, 50).map(MakeChange.toNat),
      MakeChange.makeChangeAlgebra
    )))


  property("histo ≡ gcata(dist.cofree, ...)") = {

    forAll { (z: AlgebraFamily) =>
      import z.implicits._

      val f = scheme.histo(z.cvalgebra)
      val g = scheme.gcata(gather.histo[z.F, z.B], z.cvalgebra)

      forAll((r: z.R) => f(r) ?= g(r))
    }
  }

}
