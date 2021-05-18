package com.googlecode.javaewah32;

import java.util.Random;
import org.junit.Test;
// credit @svanmald
@SuppressWarnings("javadoc")
public class FuzzEWAH32Test {
    public static boolean areAssertsEnabled() {
        boolean assertsEnabled = false;
        assert assertsEnabled = true; // Intentional side effect!!!
        return assertsEnabled;
    }

    @Test
    public void testEwah() {
      if(!areAssertsEnabled()) { throw new RuntimeException("asserts need to be enabled."); }
      // ENABLE ASSERTS BEFORE EXECUTING TO ENABLE VALIDATION.
      // if print = false and seed -1, the code will execute 10 random mutation to 2 bitmaps until infinity and each time validate the result 
      // Each time a set of 10 random mutations starts, a seed is printed (even if print = false).
      // if one of the sets of 10 mutations fails validation, the printed seed can be used here together with print = true to reproduce the issue
      System.out.println(" == Launching  @svanmald's fuzzer! ");
      testEwah(false, -1);
    }

    private void testEwah(boolean print, int seed) {
        Random seedGenerator = new Random();
        Mutation[] mutations = Mutation.values();
        int times = 1000000;

        while (times > 0) {
            times --;
            if((times % 10000) == 0) { System.out.print("."); System.out.flush(); }
            int currentSeed = seed;
            if (currentSeed == -1) {
                currentSeed = seedGenerator.nextInt();
            }
            if (print) {
                System.out.println("Seed " + currentSeed);
            }
            Random seededRandom = new Random(currentSeed);
            EWAH32BitSetPair main = new EWAH32BitSetPair();
            if (print) {
                System.out.println("EWAHCompressedBitmap main = new EWAHCompressedBitmap();");
            }
            EWAH32BitSetPair other = new EWAH32BitSetPair();
            if (print) {
                System.out.println("EWAHCompressedBitmap other = new EWAHCompressedBitmap();");
            }
            for (int i = 0; i < 10; i++) {
                Mutation mutation = mutations[seededRandom.nextInt(mutations.length)];
                mutation.apply(print, seededRandom, main, other);
                main.validate();
                other.validate();
            }
        }
        System.out.println();
    }

    public enum Mutation {
        OR {
            @Override
            void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other) {
                if (random.nextDouble() < 0.5) {
                    if (print) {
                        System.out.println("main = main.or(other);");
                    }
                    main.or(other);
                } else {
                    if (print) {
                        System.out.println("other = other.or(main);");
                    }
                    other.or(main);
                }
            }
        },
        AND {
            @Override
            void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other) {
                if (random.nextDouble() < 0.5) {
                    if (print) {
                        System.out.println("main = main.and(other);");
                    }
                    main.and(other);
                } else {
                    if (print) {
                        System.out.println("other = other.and(main);");
                    }
                    other.and(main);
                }
            }
        },
        AND_NOT {
            @Override
            void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other) {
                if (random.nextDouble() < 0.5) {
                    if (print) {
                        System.out.println("main = main.andNot(other);");
                    }
                    main.andNot(other);
                } else {
                    if (print) {
                        System.out.println("other = other.andNot(main);");
                    }
                    other.andNot(main);
                }
            }
        },
        XOR {
            @Override
            void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other) {
                if (random.nextDouble() < 0.5) {
                    if (print) {
                        System.out.println("main = main.xor(other);");
                    }
                    main.xor(other);
                } else {
                    if (print) {
                        System.out.println("other = other.xor(main);");
                    }
                    other.xor(main);
                }
            }
        },
        SET {
            @Override
            void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other) {
                int value = random.nextInt(100_000);
                if (random.nextDouble() < 0.5) {
                    if (print) {
                        System.out.println("main.set(" + value + ");");
                    }
                    main.set(value);
                } else {
                    if (print) {
                        System.out.println("other.set(" + value + ");");
                    }
                    other.set(value);
                }
            }
        },
        CLEAR_RANDOM {
            @Override
            void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other) {
                int value = random.nextInt(100_000);
                if (random.nextDouble() < 0.5) {
                    if (print) {
                        System.out.println("main.clear(" + value + ");");
                    }
                    main.clear(value);
                } else {
                    if (print) {
                        System.out.println("other.clear(" + value + ");");
                    }
                    other.clear(value);
                }
            }
        };

        abstract void apply(boolean print, Random random, EWAH32BitSetPair main, EWAH32BitSetPair other);
    }
}