/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class EnergyCheckUtils */

#ifndef _Included_edu_binghamton_vpc_Rapl
#define _Included_edu_binghamton_vpc_Rapl
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     EnergyCheckUtils
 * Method:    ProfileInit
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_binghamton_vpc_Rapl_ProfileInit
  (JNIEnv *, jclass);

/*
 * Class:     EnergyCheckUtils
 * Method:    GetSocketNum
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_binghamton_vpc_Rapl_GetSocketNum
  (JNIEnv *, jclass);

/*
 * Class:     EnergyCheckUtils
 * Method:    EnergyStatCheck
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_edu_binghamton_vpc_Rapl_EnergyStatCheck
  (JNIEnv *, jclass);

/*
 * Class:     EnergyCheckUtils
 * Method:    ProfileDealloc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_binghamton_vpc_Rapl_ProfileDealloc
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
