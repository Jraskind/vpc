load("@rules_java//java:defs.bzl", "java_import", "java_library")

java_library(
  name = "vpc",
  srcs = glob(["*.java"]),
)

java_binary(
  name = "smoke_test",
  main_class = "edu.binghamton.vpc.PerfUtils",
  runtime_deps = [":vpc"],
  data = [":lib-perf-utils"],
  # jvm_flags = [
  #   "-Djava.library.path=java/edu/binghamton/vpc"
  # ]
)

genrule(
  name = "lib-perf-utils",
  visibility = ["//visibility:private"],
  srcs = ["//src:libPerfUtils.so"],
  outs = ["libPerfUtils.so"],
  cmd = "cp -f $< $@",
)

# /usr/lib/x86_64-linux-gnu/libpapi.so
