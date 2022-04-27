load("@rules_java//java:defs.bzl", "java_import", "java_library")

java_library(
  name = "vpc",
  srcs = glob(["*.java"]),
)

java_import(
  name = "jars",
  visibility = ["//visibility:public"],
  jars = [
    "dacapo-evaluation-git.jar",
    "javassist.jar",
    "renaissance-mit-0.14.0.jar",
  ],
)

java_library(
  name = "profiling",
  srcs = glob([
    "research/utils/**/*.java",
    "research/utils/**/**/*.java",
  ]),
  deps = [":jars"],
)

java_binary(
  name = "perf_utils",
  main_class = "research.utils.perf.PerfUtils",
  runtime_deps = [":profiling"],
)

java_binary(
  name = "dacapo",
  main_class = "Harness",
  runtime_deps = [":profiling"],
  args = [
    "--callback research.utils.dacapo.IterationCallBack",
  ]
)
