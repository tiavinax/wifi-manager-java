#!/bin/bash
echo "=== LANCEMENT DU TEST ==="

# Compile TestRun
echo "🔨 Compilation de TestRun..."
javac -d target/test-classes \
      -cp "target/classes:lib/*" \
      src/test/java/com/wifimanager/detector/TestRun.java 2> test-compile-errors.txt

if [ -s test-compile-errors.txt ]; then
    echo "❌ Erreurs de compilation:"
    cat test-compile-errors.txt
    exit 1
fi

echo "✅ TestRun compilé avec succès"

# Exécute
echo -e "\n🚀 Exécution du test..."
echo "========================================"
java -cp "target/classes:target/test-classes:lib/*" \
     com.wifimanager.detector.TestRun

echo -e "\n========================================"
echo "✅ Test terminé !"