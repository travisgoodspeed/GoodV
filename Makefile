
help:
	@echo "You probably want to run 'make clean assemble install'."
	@echo "Install Android Studio and Gradle first."

.PHONY: clean build assemble install

clean:
	./gradlew clean
build:
	./gradlew build
assemble:
	./gradlew assemble
install:
	./gradlew installDebug


