package com.sksamuel.kotest.engine.interceptors

import io.kotest.common.KotestInternal
import io.kotest.core.ProjectContext
import io.kotest.core.TagExpression
import io.kotest.core.extensions.ProjectExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.EngineResult
import io.kotest.engine.config.MutableConfiguration
import io.kotest.engine.config.toConfiguration
import io.kotest.engine.interceptors.EngineContext
import io.kotest.engine.interceptors.ProjectExtensionEngineInterceptor
import io.kotest.matchers.shouldBe

@KotestInternal
class ProjectExtensionEngineInterceptorTest : FunSpec({

   test("should invoke all extensions") {

      var fired1 = false
      var fired2 = false

      val ext1 = object : ProjectExtension {
         override suspend fun interceptProject(context: ProjectContext, callback: suspend (ProjectContext) -> Unit) {
            fired1 = true
            callback(context)
         }
      }

      val ext2 = object : ProjectExtension {
         override suspend fun interceptProject(context: ProjectContext, callback: suspend (ProjectContext) -> Unit) {
            fired2 = true
            callback(context)
         }
      }

      val c = MutableConfiguration()
      c.registry().add(ext1)
      c.registry().add(ext2)
      ProjectExtensionEngineInterceptor.intercept(
         EngineContext.Companion.empty.withProjectConfiguration(c.toConfiguration())
      ) {
         EngineResult.empty
      }

      fired1 shouldBe true
      fired2 shouldBe true
   }

   test("should invoke downstream after extensions") {

      var fired = false

      val ext1 = object : ProjectExtension {
         override suspend fun interceptProject(context: ProjectContext, callback: suspend (ProjectContext) -> Unit) {
            callback(context)
         }
      }

      val ext2 = object : ProjectExtension {
         override suspend fun interceptProject(context: ProjectContext, callback: suspend (ProjectContext) -> Unit) {
            callback(context)
         }
      }

      val c = MutableConfiguration()
      c.registry().add(ext1)
      c.registry().add(ext2)
      ProjectExtensionEngineInterceptor.intercept(
         EngineContext.Companion.empty.withProjectConfiguration(c.toConfiguration())
      ) {
         fired = true
         EngineResult.empty
      }

      fired shouldBe true
   }

   test("should invoke downstream without extensions") {

      var fired = false

      ProjectExtensionEngineInterceptor.intercept(
         EngineContext.Companion.empty
      ) {
         fired = true
         EngineResult.empty
      }

      fired shouldBe true
   }

   test("should propagate tag changes") {
      var tags = TagExpression("none")

      val ext = object : ProjectExtension {
         override suspend fun interceptProject(context: ProjectContext, callback: suspend (ProjectContext) -> Unit) {
            callback(context.copy(tags = context.tags.include("bar")))
         }
      }

      val c = MutableConfiguration()
      c.registry().add(ext)
      ProjectExtensionEngineInterceptor.intercept(
         EngineContext.empty.withTags(TagExpression("foo")).withProjectConfiguration(c.toConfiguration())
      ) {
         tags = it.tags
         EngineResult.empty
      }

      tags.expression shouldBe "foo & bar"
   }
})
