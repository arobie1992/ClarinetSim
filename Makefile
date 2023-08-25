VER=1.0.4
RUN_BASE=peersim-$(VER)

.PHONY: all clean doc release

all:
	javac -Xlint:unchecked -classpath src:jep-2.3.0.jar:djep-1.0.0.jar `find src -name "*.java"`
clean:
	rm -f `find . -name "*.class"`
	rm -rf peersim-$(VER)

release: clean all
	rm -fr peersim-$(VER)
	mkdir peersim-$(VER)
	cp Makefile overview.html README CHANGELOG RELEASE-NOTES build.xml peersim-doclet.jar peersim-$(VER)
	mkdir peersim-$(VER)/example
	cp example/*.txt peersim-$(VER)/example
	mkdir peersim-$(VER)/src
	if [ "$(shell uname)" = "Darwin" ]; then\
		rsync -R `find src/peersim src/example src/clarinetsim -name "*.java"` peersim-$(VER);\
	else\
		cp --parents `find src/peersim src/example -name "*.java"` peersim-$(VER);\
	fi
	cd src ; jar cf ../peersim-$(VER).jar `find peersim example clarinetsim -name "*.class"`
	mv peersim-$(VER).jar peersim-$(VER)
	cp jep-2.3.0.jar peersim-$(VER)
	cp djep-1.0.0.jar peersim-$(VER)

run:
	java -cp "$(RUN_BASE)/peersim-$(VER).jar:$(RUN_BASE)/jep-2.3.0.jar:$(RUN_BASE)/djep-1.0.0.jar" peersim.Simulator $(filter-out $@,$(MAKECMDGOALS))
