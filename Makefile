CC = gcc
CFLAGS = -fPIC -g

SOURCES = src

JAVA_HOME = $(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")
JAVA_INCLUDE = $(JAVA_HOME)include
JAVA_LINUX_INCLUDE = $(JAVA_INCLUDE)/linux
JNI_INCLUDE = -I$(JAVA_INCLUDE) -I$(JAVA_LINUX_INCLUDE)

JAVAC=javac

JAVA_SOURCES = edu/binghamton/vpc
JAVA_CLASSPATH = lib/dacapo.jar:lib/renaissance-gpl-0.11.0.jar
JAR = vpc.jar

get_java_deps:
	mkdir -p lib
	wget https://sourceforge.net/projects/dacapobench/files/evaluation/dacapo-evaluation-git%2B309e1fa.jar/download
	mv download lib/dacapo.jar
	wget https://github.com/renaissance-benchmarks/renaissance/releases/download/v0.11.0/renaissance-gpl-0.11.0.jar
	mv renaissance-gpl-0.11.0.jar lib/renaissance-gpl-0.11.0.jar

.PHONY: %.o
%.o: %.c
	$(CC) -c -o $@ $< $(CFLAGS) $(JNI_INCLUDE)

libMonotonic.so: $(SOURCES)/monotonic_timestamp.o
	mkdir -p bin
	$(CC) -shared -Wl,-soname,bin/$@ -o $@ $^ $(JNI_INCLUDE) -lc
	rm -f $^

libRapl.so: $(SOURCES)/jrapl.o
	mkdir -p lib
	$(CC) -shared -Wl,-soname,bin/$@ -o $@ $^ $(JNI_INCLUDE) -lc
	rm -f $^

.PHONY: %.class
%.class: java/%.java
	$(JAVAC) -cp $(JAVA_CLASSPATH) $^ -d .

jar: $(JAVA_SOURCES)/*.class
	jar -cf $(JAR) $^
	rm -rf $^

smoke_test: jar libRapl.so libMonotonic.so
	java -cp $(JAR) MonotonicTimestamp bin/libMonotonic.so
	java -cp $(JAR) JRapl bin/libRapl.so
	java -Dvpc.library.path=lib -cp $(JAR):$(JAVA_CLASSPATH) Harness sunflow -c edu.binghamton.vpc.VpcDacapoCallback
	@echo 'all targets successfully built!'

clean:
	rm -r $(JAR) $(JAVA_SOURCES) bin
