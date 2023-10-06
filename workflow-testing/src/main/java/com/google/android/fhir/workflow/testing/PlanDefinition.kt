/*
 * Copyright 2022-2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.workflow.testing

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Parameters
import org.json.JSONException
import org.junit.Assert.fail
import org.opencds.cqf.fhir.api.Repository
import org.opencds.cqf.fhir.cql.EvaluationSettings
import org.opencds.cqf.fhir.cr.plandefinition.r4.PlanDefinitionProcessor
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository
import org.skyscreamer.jsonassert.JSONAssert

object PlanDefinition : Loadable() {
  private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  private val jsonParser = fhirContext.newJsonParser()

  fun parse(assetName: String): IBaseResource {
    return jsonParser.parseResource(open(assetName))
  }

  fun buildProcessor(repository: Repository): PlanDefinitionProcessor {
    val evaluationSettings: EvaluationSettings = EvaluationSettings.getDefault()

    return PlanDefinitionProcessor(repository, evaluationSettings)
  }

  object Assert {
    fun that(planDefinitionID: String, patientID: String, encounterID: String?) =
      Apply(planDefinitionID, patientID, encounterID)

    fun that(planDefinitionID: String, patientID: String) = Apply(planDefinitionID, patientID, null)
  }

  class Apply(
    private val planDefinitionID: String,
    private val patientID: String?,
    private val encounterID: String?,
  ) {
    private val fhirDal = InMemoryFhirRepository(fhirContext)
    private lateinit var dataEndpoint: Endpoint
    private lateinit var libraryEndpoint: Endpoint
    private lateinit var baseResource: IBaseResource

    fun addAll(resource: IBaseResource) {
      when (resource) {
        is Bundle -> resource.entry.forEach { addAll(it.resource) }
        else -> fhirDal.create(resource)
      }
    }

    fun withData(dataAssetName: String): Apply {
      addAll(parse(dataAssetName))
      return this
    }

    fun withLibrary(libraryAssetName: String): Apply {
      addAll(parse(libraryAssetName))
      return this
    }

    fun apply(): GeneratedCarePlan {
      return GeneratedCarePlan(
        buildProcessor(fhirDal)
          .apply(
            /* id = */ IdType("PlanDefinition", planDefinitionID),
            /* canonical = */ null,
            /* planDefinition = */ null,
            /* subject = */ patientID,
            /* encounterId = */ encounterID,
            /* practitionerId = */ null,
            /* organizationId = */ null,
            /* userType = */ null,
            /* userLanguage = */ null,
            /* userTaskContext = */ null,
            /* setting = */ null,
            /* settingContext = */ null,
            /* parameters = */ Parameters(),
            /* useServerData = */ false,
            /* bundle = */ baseResource as Bundle,
            /* prefetchData = */ null,
            /* libraryEngine = */ null,
          ),
      )
    }
  }

  class GeneratedCarePlan(val carePlan: IBaseResource) {
    fun isEqualsTo(expectedCarePlanAssetName: String) {
      try {
        JSONAssert.assertEquals(
          load(expectedCarePlanAssetName),
          jsonParser.encodeResourceToString(carePlan),
          true,
        )
      } catch (e: JSONException) {
        e.printStackTrace()
        fail("Unable to compare Jsons: " + e.message)
      } catch (e: AssertionError) {
        println("Actual: " + jsonParser.encodeResourceToString(carePlan))
        throw e
      }
    }
  }
}
