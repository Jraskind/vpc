#include <jni.h>

#ifndef _Included_edu_binghamton_vpc_PerfUtils
#define _Included_edu_binghamton_vpc_PerfUtils
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_edu_binghamton_vpc_MonotonicTimestamp_getMonotonicTimestamp
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
