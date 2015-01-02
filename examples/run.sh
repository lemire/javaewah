cd .. && mvn -Dmaven.test.skip=true package && cd examples 
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
