CC = gcc
CFLAGS = -fPIC -g

SOURCES = src
RAPL_SOURCES = $(SOURCES)/arch_spec.o $(SOURCES)/msr.o $(SOURCES)/rapl.o
TARGET = bin

JAVA_HOME = $(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")
JAVA_INCLUDE = $(JAVA_HOME)include
JAVA_LINUX_INCLUDE = $(JAVA_INCLUDE)/linux
JNI_INCLUDE = -I$(JAVA_INCLUDE) -I$(JAVA_LINUX_INCLUDE)

JAVAC=javac
JAVA_CLASSPATH = lib/dacapo.jar:lib/renaissance-gpl-0.11.0.jar

JAVA_SOURCES = edu/binghamton/vpc
JAR = vpc.jar

get_java_deps:
	mkdir -p lib
	wget https://sourceforge.net/projects/dacapobench/files/evaluation/dacapo-evaluation-git%2B309e1fa.jar/download
	mv download lib/dacapo.jar
	wget https://github.com/renaissance-benchmarks/renaissance/releases/download/v0.11.0/renaissance-gpl-0.11.0.jar
	mv renaissance-gpl-0.11.0.jar lib/renaissance-gpl-0.11.0.jar

.PHONY: %.o %.class
%.o: %.c
	$(CC) -c -o $@ $^ $(CFLAGS) $(JNI_INCLUDE)

%.class: %.java
	$(JAVAC) -cp $(JAVA_CLASSPATH) $^ -d .

jar: $(JAVA_SOURCES)/*.class
	jar -cf $(JAR) $^
	rm -rf $^

libMonotonic.so: $(SOURCES)/monotonic_timestamp.o
	mkdir -p $(TARGET)
	$(CC) -shared -Wl,-soname,$@ -o $(TARGET)/$@ $^ $(JNI_INCLUDE) -lc
	rm -f $^

libRapl.so: $(RAPL_SOURCES)
	mkdir -p $(TARGET)
	$(CC) -shared -Wl,-soname,$@ -o $(TARGET)/$@ $^ $(JNI_INCLUDE) -lc
	rm -f $^

smoke_test: jar libRapl.so libMonotonic.so
	java -cp $(JAR) edu.binghamton.vpc.MonotonicTimestamp $(TARGET)/libMonotonic.so
	java -cp $(JAR) edu.binghamton.vpc.Rapl $(TARGET)/libRapl.so
	java -Dvpc.library.path=$(PWD)/lib -cp $(JAR):$(JAVA_CLASSPATH) Harness sunflow -c edu.binghamton.vpc.VpcDacapoCallback
	@echo 'all targets successfully built!'

clean:
	rm -r $(SOURCES)/*.o $(TARGET) $(JAVA_SOURCES)/*.class $(JAR)
