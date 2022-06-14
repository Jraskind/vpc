CC = gcc
CFLAGS = -fPIC -g

SOURCE_DIR = src
OBJ = $(SOURCE_DIR)/monotonic_timestamp.o
TARGET = *.so

JAVA_HOME = $(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")
JAVA_INCLUDE = $(JAVA_HOME)include
JAVA_LINUX_INCLUDE = $(JAVA_INCLUDE)/linux
JNI_INCLUDE = -I$(JAVA_INCLUDE) -I$(JAVA_LINUX_INCLUDE)

JAVAC=javac

JAVA_SOURCES = java/edu/binghamton/vpc/MonotonicTimestamp.java
JAVA_OBJ = edu/binghamton/vpc
JAR = monotonic.jar

%.o: %.c
	$(CC) -c -o $@ $< $(CFLAGS) $(JNI_INCLUDE)

libMonotonic.so: $(OBJ)
	$(CC) -shared -Wl,-soname,$@ -o $@ $^ $(JNI_INCLUDE) -lc
	rm -f $(OBJ)

jar: libMonotonic.so build
	javac $(JAVA_SOURCES) -d .
	jar --create --file $(JAR) $(JAVA_OBJ)/MonotonicTimestamp.class
	rm -rf edu

clean:
	rm -rf $(TARGET) $(JAR) $(JAVA_OBJ)

# dacapo:
# 	ifneq ("$(wildcard $(PATH_TO_FILE))","")
# 	FILE_EXISTS = 1
# 	else
# 	wget https://sourceforge.net/projects/dacapobench/files/evaluation/dacapo-evaluation-git%2B309e1fa.jar/download -o dacapo.jar
# 	endif
# 	javac -cp dacapo.jar:third_party/jrapl/jrapl.jar java/edu/binghamton/vpc/VpcCallback.java
#
# run:
# 	javac -cp dacapo.jar:third_party/jrapl/jrapl.jar java/edu/binghamton/vpc/VpcCallback.java
# 	java -cp dacapo.jar:third_party/jrapl/jrapl.jar:java/edu/binghamton/vpc/VpcCallback.class Harness sunflow -c edu.binghamton.vpc.VpcCallback
#
# smoke_test:
# 	java -cp dacapo.jar:third_party/jrapl/jrapl.jar:java/edu/binghamton/vpc/VpcCallback.class Harness sunflow -c edu.binghamton.vpc.VpcCallback
