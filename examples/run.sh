if test -n "$(find ../target/JavaEWAH-*.jar -maxdepth 1 -name 'files*' -print -quit)"
then
    cd .. && mvn -Dmaven.test.skip=true package && cd examples 
fi
echo "Running MemoryMappingExample"
javac -cp "../target/*" MemoryMappingExample.java && java -cp ../target/*:. MemoryMappingExample

rm *.class
