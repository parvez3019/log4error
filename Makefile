.PHONY: help setup clean test test-it verify package install benchmark

MVN ?= mvn
MVN_FLAGS := -B -Dgpg.skip=true

help:
	@echo "log4error local targets:"
	@echo "  make setup      - Check JDK 17+ and resolve Maven dependencies"
	@echo "  make clean      - mvn clean"
	@echo "  make test       - Unit tests (Surefire)"
	@echo "  make test-it    - Integration tests (Failsafe)"
	@echo "  make verify     - clean + unit + integration tests"
	@echo "  make package    - Package JAR (skip tests)"
	@echo "  make install    - Install to local Maven repo"
	@echo "  make benchmark  - Run JMH benchmarks"

setup:
	@java -version 2>&1 | head -n 1
	@JAVA_VER=$$(java -version 2>&1 | awk -F[\".] '/version/ {print $$2}'); \
	if [ "$$JAVA_VER" -lt 17 ]; then echo "JDK 17+ required"; exit 1; fi
	$(MVN) $(MVN_FLAGS) dependency:resolve
	@echo "Setup complete."

clean:
	$(MVN) $(MVN_FLAGS) clean

test:
	$(MVN) $(MVN_FLAGS) test

test-it:
	$(MVN) $(MVN_FLAGS) failsafe:integration-test failsafe:verify

verify:
	$(MVN) $(MVN_FLAGS) clean verify

package:
	$(MVN) $(MVN_FLAGS) package -DskipTests

install:
	$(MVN) $(MVN_FLAGS) install

benchmark:
	$(MVN) $(MVN_FLAGS) clean test-compile exec:java \
		-DskipTests -DskipITs \
		-Dexec.classpathScope=test \
		-Dexec.mainClass=io.github.parvez3019.benchmarking.Main
