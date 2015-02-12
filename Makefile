all: compile

compile: 
	@javac *.java

clean:
	@echo "Cleaning Up"
	@rm -rf *.class

