#include <jni.h>

#ifndef _Included_edu_binghamton_vpc_MonotonicTimestamp
#define _Included_edu_binghamton_vpc_MonotonicTimestamp
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_edu_binghamton_vpc_MonotonicTimestamp_getMonotonicTimestamp
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
