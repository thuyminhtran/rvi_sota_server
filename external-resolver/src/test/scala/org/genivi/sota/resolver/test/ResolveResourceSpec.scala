package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling._
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.CirceSupport._
import org.genivi.sota.resolver.types.{Vehicle, Package, PackageFilter}
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}


class ResolveResourceWordSpec extends ResourceWordSpec {

  "Resolve resource" should {

    "give back a list of vehicles onto which the supplied package should be installed" in {

      // Add some vehicles.
      val vins = List(
        "00RESOLVEVIN12345",
        "01RESOLVEVIN12345",
        "10RESOLVEVIN12345",
        "11RESOLVEVIN12345")

      vins map addVehicleOK

      // Add a package.
      addPackageOK("resolve pkg", "0.0.1", None, None)

      // Add a trival filter that lets all vins through.
      addFilterOK("truefilter", "TRUE")
      addPackageFilterOK("resolve pkg", "0.0.1", "truefilter")

      resolveOK("resolve pkg", "0.0.1", vins)

      // Add another filter.
      addFilterOK("0xfilter", s"""vin_matches "^00.*" OR vin_matches "^01.*"""")
      addPackageFilterOK("resolve pkg", "0.0.1", "0xfilter")

      resolveOK("resolve pkg", "0.0.1",
        List("00RESOLVEVIN12345", "01RESOLVEVIN12345"))

      // Add trivially false filter.
      addFilterOK("falsefilter", "FALSE")
      addPackageFilterOK("resolve pkg", "0.0.1", "falsefilter")

      resolveOK("resolve pkg", "0.0.1", List())

    }
  }

  "fail if a non-existing package name is given" in {

    resolve("resolve pkg2", "0.0.1") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingPackage
    }
  }

  "fail if a non-existing package version is given" in {

    resolve("resolve pkg", "0.0.2") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingPackage
    }
  }

  "return a string that the core server can parse" in {

    deletePackageFilter("resolve pkg", "0.0.1", "falseFilter") ~> route ~> check {
      status shouldBe StatusCodes.OK
      resolve("resolve pkg", "0.0.1") ~> route ~> check {
        status shouldBe StatusCodes.OK

        responseAs[io.circe.Json].noSpaces shouldBe
          s"""[["00RESOLVEVIN12345",[{"version":"0.0.1","name":"resolve pkg"}]],["01RESOLVEVIN12345",[{"version":"0.0.1","name":"resolve pkg"}]]]"""

        responseAs[Map[Vehicle.Vin, Set[Package.Id]]] shouldBe
          Map(Refined("00RESOLVEVIN12345") -> Set(Package.Id(Refined("resolve pkg"), Refined("0.0.1"))),
              Refined("01RESOLVEVIN12345") -> Set(Package.Id(Refined("resolve pkg"), Refined("0.0.1"))))

      }
    }
  }

}
/*

class ResolveResourcePropSpec extends ResourcePropSpec {

  import ArbitraryFilter.arbFilter
  import ArbitraryPackage.arbPackage
  import ArbitraryVehicle.arbVehicle
  import akka.http.scaladsl.model.StatusCodes
  import org.genivi.sota.resolver.db.Resolve.makeFakeDependencyMap
  import org.genivi.sota.resolver.types.FilterParser.parseValidFilter
  import org.genivi.sota.resolver.types._
  import org.scalacheck.Prop.{True => _, _}
  import org.genivi.sota.CirceSupport._
  import io.circe.generic.auto._
  import akka.http.scaladsl.unmarshalling._


  property("Resolve should give back the same thing as if we filtered with the filters") {

    forAll() { (
      vs: Seq[Vehicle],   // The available vehicles.
      p : Package,        // The package we want to install.
      ps: Seq[Package],   // Other packages in the system.
      fs: Seq[Filter])    // Filters to be associated to the package we
                          // want to install.
        => {

          // Add some new vehicles.
          vs map (v => addVehicleOK(v.vin.get))

          // Add some new packages.
          (p +: ps) map (q => addPackageOK(q.id.name.get, q.id.version.get, q.description, q.vendor))

          // Add some new filters.
          fs map (f => addFilterOK(f.name.get, f.expression.get))

          // Associate the filters to the package we want to install.
          fs map (f => addPackageFilterOK(p.id.name.get, p.id.version.get, f.name.get))

          // The resolver should give back the same VINs as if...
          listVehicles ~> route ~> check {
            status === StatusCodes.OK
            val allVehicles = responseAs[Seq[Vehicle]]

            resolve(p.id.name.get, p.id.version.get) ~> route ~> check {
              status === StatusCodes.OK
              val result = responseAs[Map[Vehicle.Vin, Seq[Package.Id]]]
              classify(result.toList.length > 0, "more than zero", "zero") {
                result === makeFakeDependencyMap(p.id.name, p.id.version,

                    // ... we filtered the list of all VINs by the boolean
                    // predicate that arises from the combined filter
                    // queries.
                    allVehicles.filter(FilterQuery.query
                      (fs.map(_.expression).map(parseValidFilter)
                         .foldLeft[FilterAST](True)(And))))
              }
            }
          }

        }
    }

  }

}

 */