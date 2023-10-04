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

package com.google.android.fhir.workflow

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.UriParam
import ca.uhn.fhir.util.BundleBuilder
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.getResourceType
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Search
import org.cqframework.fhir.api.FhirDal
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import timber.log.Timber

internal class FhirEngineDal(
  private val fhirContext: FhirContext,
  private val fhirEngine: FhirEngine,
  private val knowledgeManager: KnowledgeManager,
) : FhirDal {

  override fun read(id: IIdType): IBaseResource = runBlockingOrThrowMainThreadException {
    val clazz = id.getResourceClass()
    if (id.isAbsolute) {
      knowledgeManager
        .loadResources(
          resourceType = id.resourceType,
          url = "${id.baseUrl}/${id.resourceType}/${id.idPart}",
        )
        .single()
    } else {
      try {
        fhirEngine.get(getResourceType(clazz), id.idPart)
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        // Searching by resourceType and Id to workaround
        // https://github.com/google/android-fhir/issues/1920
        // remove when the issue is resolved.
        val searchByNameWorkaround =
          knowledgeManager.loadResources(resourceType = id.resourceType, id = id.toString())
        if (searchByNameWorkaround.count() > 1) {
          Timber.w("Found more than one value in the IgManager for the id $id")
        }
        searchByNameWorkaround.firstOrNull() ?: throw resourceNotFoundException
      }
    }
  }

  override fun create(resource: IBaseResource): Unit = runBlockingOrThrowMainThreadException {
    fhirEngine.create(resource as Resource)
  }

  override fun update(resource: IBaseResource) = runBlockingOrThrowMainThreadException {
    fhirEngine.update(resource as Resource)
  }

  override fun delete(id: IIdType) = runBlockingOrThrowMainThreadException {
    val clazz = id.getResourceClass()
    fhirEngine.delete(getResourceType(clazz), id.idPart)
  }

  override fun search(
    resourceType: String?,
    searchParameters: MutableMap<String, MutableList<MutableList<IQueryParameterType>>>?,
  ): IBaseBundle {
    return runBlockingOrThrowMainThreadException {
      val builder = BundleBuilder(fhirContext)
      builder.setType("searchset")

      if (resourceType != null) {
        if (searchParameters == null) {
          search(resourceType).forEach(builder::addCollectionEntry)
        } else if (searchParameters.size == 1 && searchParameters.containsKey("url")) {
          // first AND then OR
          val search = Search(type = ResourceType.fromCode(resourceType))

          searchParameters.forEach { param ->
            if (param.key.equals("url", true)) {
              // special case
              param.value.forEach {
                it.forEach {
                  ((it as? UriParam)?.value ?: (it as? StringParam)?.value)?.let { url ->
                    knowledgeManager
                      .loadResources(resourceType = resourceType, url = url)
                      .forEach(builder::addCollectionEntry)
                  }
                }
              }
            } else {
              param.value.forEach {
                it.forEach { search.applyFilterParam(param.key, it, Operation.OR) }
              }
            }
          }

          fhirEngine.search<Resource>(search).forEach(builder::addCollectionEntry)
        }
      }

      builder.bundle
    }
  }

  fun search(resourceType: String): Iterable<IBaseResource> =
    runBlockingOrThrowMainThreadException {
      val search = Search(type = ResourceType.fromCode(resourceType))
      knowledgeManager.loadResources(resourceType = resourceType) + fhirEngine.search(search)
    }

  @Suppress("UNCHECKED_CAST")
  private fun IIdType.getResourceClass(): Class<Resource> {
    try {
      return Class.forName("org.hl7.fhir.r4.model.$resourceType") as Class<Resource>
    } catch (exception: ClassNotFoundException) {
      throw IllegalArgumentException("invalid resource type : $resourceType", exception)
    } catch (exception: ClassCastException) {
      throw IllegalArgumentException("invalid resource type : $resourceType", exception)
    }
  }
}
