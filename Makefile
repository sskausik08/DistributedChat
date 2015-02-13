all: compile

compile: 
	@javac *.java 
	@rmic Chat

clean:
	@echo "Cleaning Up"
	@rm -rf *.class

