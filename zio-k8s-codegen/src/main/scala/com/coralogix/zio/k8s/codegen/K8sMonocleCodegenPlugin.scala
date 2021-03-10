package com.coralogix.zio.k8s.codegen

import sbt.Keys._
import sbt._
import zio.nio.core.file.{ Path => ZPath }
import K8sSwaggerPlugin.autoImport._

object K8sMonocleCodegenPlugin extends AutoPlugin {
  object autoImport {
    lazy val generateSources =
      Def.task {
        val log = streams.value.log
        val runtime = zio.Runtime.default
        val codegen = new K8sResourceCodegen(log)

        val sourcesDir = (Compile / sourceManaged).value
        val ver = scalaVersion.value

        val cachedFun = FileFunction.cached(
          streams.value.cacheDirectory / s"k8s-monocle-src-${ver}",
          FileInfo.hash
        ) { input: Set[File] =>
          input.foldLeft(Set.empty[File]) { (result, k8sSwagger) =>
            val fs = runtime.unsafeRunTask(
              codegen.generateAllMonocle(
                ZPath.fromJava(k8sSwagger.toPath),
                ZPath.fromJava(sourcesDir.toPath)
              )
            )
            result union fs.toSet
          }
        }

        val k8sSwagger = getK8sSwagger.value

        cachedFun(Set(k8sSwagger)).toSeq
      }
  }

  import autoImport._

  override val requires = K8sSwaggerPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      Compile / sourceGenerators += generateSources.taskValue
    )
}