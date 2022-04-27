package(default_visibility = ["//visibility:public"])

cc_library(
    name = "perf-utils-lib",
    srcs = glob(["*.c"]),
    hdrs = glob(["*.h"]),
    deps = ["//tools:copy_jni_hdr_lib"],
    alwayslink = True,
)

cc_binary(
    name = "libPerfUtils.so",
    linkshared = True,
    data = glob(["*.h"]),
    deps = [":perf-utils-lib"],
)
