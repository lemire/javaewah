if test -n "$(find ../target/JavaEWAH-*.jar -maxdepth 1 -name 'files*' -print -quit)"
then
    cd .. && mvn -Dmaven.test.skip=true package && cd examples 
fi
echo "Running MemoryMappingExample"
javac -cp "../target/*" MemoryMappingExample.java && java -cp ../target/*:. MemoryMappingExample
echo

echo "Running BitSetMemoryMappingExample"
javac -cp "../target/*" BitSetMemoryMappingExample.java && java -cp ../target/*:. BitSetMemoryMappingExample
echo


echo "Running BitSetSimpleExample"
javac -cp "../target/*" BitSetSimpleExample.java && java -cp ../target/*:. BitSetSimpleExample
echo

rm *.class
