(ns com.github.jimpil.bass.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.github.jimpil.bass.core :refer [with-base]]
            [com.github.jimpil.bass.util :as util])
  (:import [java.nio ByteBuffer]
           [java.util Arrays]))

(set! *warn-on-reflection* true)

(defmacro let-timed
  "Just like 'let' but each binding expression will be timed."
  [bindings & body]
  (let [parts   (partition 2 bindings)
        names   (map first parts)
        results (map #(list 'clojure.core/time (second %)) parts)]
    `(let ~(vec (interleave names results))
       ~@body)))

(deftest correctness-examples
  (testing "BASE 2"
    (testing "max byte (exactly 8 bits)"
      (let [binary-str (str/join (repeat 8 \1)) ;; byte -1
            expected-octal "377"
            expected-decimal 255
            bs (with-base 2 :decode binary-str)]
        (is
          (= 1 (alength bs)))

        (is
          (= expected-decimal (Byte/toUnsignedInt (first bs))))

        (is
          (= expected-octal (with-base 8 :encode bs)))

        (is
          (= binary-str (with-base 2 :encode bs)))))

    (testing "12 bits"
      (let [binary-str (str/join    ;; "101010101010"
                         (concat
                           (repeat 4 \0)  ;; 12 bits => 2 bytes (pad left)
                           (interleave
                             (repeat 6 \1)
                             (repeat 6 \0))))
            expected-octal "012252"
            expected-decimal 2730
            bs (with-base 2 :decode binary-str)]
        (is
          (= 2 (alength bs)))

        (is
          (= expected-decimal (BigInteger. 1 bs)))

        (is
          (= expected-octal (with-base 8 :encode bs)))

        (is
          (= binary-str (with-base 2 :encode bs))))))

  (testing "BASE 8"
    (testing "some random byte"
      (let [octal-str "247" ;; 2*8^2 + 4*8 + 7 = 167
            expected-decimal 167
            expected-binary "10100111" ;; byte -89
            bs (util/buffer->bytes
                 (with-base 8 :decode octal-str))]
        (is
          (= expected-decimal (Byte/toUnsignedInt (first bs))))

        (is
          (= -89 (first bs)))

        (is
          (= expected-binary (with-base 2 :encode bs)))

        (is
          (= octal-str (with-base 8 :encode bs)))))

    (testing "max unsigned byte"
      (let [octal-str        "377" ;; 3*8^2 + 7*8 + 7 = 255
            expected-decimal 255
            expected-binary  (str/join (repeat 8 \1)) ;; byte -1
            bs (util/buffer->bytes
                 (with-base 8 :decode octal-str))]
        (is
          (= expected-decimal (Byte/toUnsignedInt (first bs))))

        (is
          (= -1 (first bs)))

        (is
          (= expected-binary (with-base 2 :encode bs)))

        (is
          (= octal-str (with-base 8 :encode bs)))))

    (testing "more than 1 bytes"
      (let [octal-str "001377"
            expected-decimal 511
            expected-binary (str/join
                              (concat
                                (repeat 7 \0)  ;; bytes [1, -1] need 9 bits
                                (repeat 9 \1)))
            ^bytes bs (with-base 8 :decode octal-str)]
        (is
          (= expected-decimal (BigInteger. 1 bs)))

        (is
          (= [1 -1] (seq bs)))

        (is
          (= expected-binary (with-base 2 :encode bs)))

        (is
          (= octal-str (with-base 8 :encode bs))))))

  (testing "BASE 32"
    (are ;; simple-cases
      [expected produced]
      (= expected produced)
      ""         (with-base 32 :encode (.getBytes  "" "UTF-8"))
      "MY======" (with-base 32 :encode (.getBytes  "f" "UTF-8"))
      "MZXQ====" (with-base 32 :encode (.getBytes  "fo" "UTF-8"))
      "MZXW6===" (with-base 32 :encode (.getBytes  "foo" "UTF-8"))
      "MZXW6YQ=" (with-base 32 :encode (.getBytes  "foob" "UTF-8"))
      "MZXW6YTB" (with-base 32 :encode (.getBytes  "fooba" "UTF-8"))
      "MZXW6YTBOI======" (with-base 32 :encode (.getBytes  "foobar" "UTF-8"))
      "JBSWY3DPEBLW64TMMQ======" (with-base 32 :encode (.getBytes  "Hello World" "UTF-8")))

    (are ;; edge-cases
      [expected produced]
      (= expected produced)
      ;; 1. All Zeros (Tests if zero-bit shifts handle indices correctly)
      "AAAAAAAA" (with-base 32 :encode (byte-array (repeat 5 0)))
      ;; 2. Maximum Byte Values (Tests if bit-masking `0xFF` handles signed vs unsigned bytes correctly)
      "77777777" (with-base 32 :encode (byte-array (repeat 5 -1)))
      ;; 3. Sequential Bytes
      "AAAQEAYE" (with-base 32 :encode (byte-array [0 1 2 3 4]))))

  (testing "BASE 58"
    (let [b58-str "xpub67uA5wAUuv1ypp7rEY7jUZBZmwFSULFUArLBJrHr3amnymkUEYWzQJz13zLacZv33sSuxKVmerpZeFExapBNt8HpAqtTtWqDQRAgyqSKUHu"
          b58-bytes (with-base 58 :decode b58-str)]
      (is ;; https://en.bitcoin.it/wiki/List_of_address_prefixes
        (-> (with-base :base16/uc :encode b58-bytes)
            (str/starts-with? "0488B21E")))

      (is
        (= b58-str (with-base 58 :encode b58-bytes)))))
  )


(defspec array-roundtrip-is-lossless 2000
  (prop/for-all [bs gen/bytes]
    (let [roundtripped (->> bs
                            (with-base 2 :encode)
                            (with-base 2 :decode)
                            (with-base 8 :encode)
                            (with-base 8 :decode)
                            (with-base 32 :encode)
                            (with-base 32 :decode)
                            (with-base 58 :encode)
                            (with-base 58 :decode))]
      (is
        (= (seq bs)
           (seq (util/buffer->bytes roundtripped)))))))

(defspec buffer-roundtrip-is-lossless 1000
  (prop/for-all [^ByteBuffer buf (gen/fmap
                                   #(doto (ByteBuffer/wrap %)
                                      .mark) ;; we will consume this twice!
                                   gen/bytes)]
    (let [roundtripped (->> buf
                            (with-base 2 :encode)
                            (with-base 2 :decode)
                            (with-base 8 :encode)
                            (with-base 8 :decode)
                            (with-base 32 :encode)
                            (with-base 32 :decode)
                            (with-base 58 :encode)
                            (with-base 58 :decode))]
      (is
        (= (seq (util/buffer->bytes (doto buf (.reset)))) ;; reset position to the previous mark
           (seq (util/buffer->bytes roundtripped)))))))

(deftest full-roundtrip
  (testing "byte-array terminals"
    (let [bs (util/random-bytes 1024)]  ;; 1KB worth
      (let-timed [b58-encoded (with-base 58 :encode bs)
                  b58-decoded (with-base 58 :decode b58-encoded)
                  b2-encoded  (with-base 2  :encode b58-decoded)
                  b2-decoded  (with-base 2  :decode b2-encoded)
                  b8-encoded  (with-base 8 :encode  b2-decoded)
                  b8-decoded  (with-base 8 :decode  b8-encoded)
                  b16-encoded (with-base 16 :encode b8-decoded)
                  b16-decoded (with-base 16 :decode b16-encoded)
                  b32-encoded (with-base 32 :encode b16-decoded)
                  b32-decoded (with-base 32 :decode b32-encoded)]
        (is (Arrays/equals bs (util/buffer->bytes b32-decoded)))))
    )

  (testing "String terminals"
    (let [s "xpub67uA5wAUuv1ypp7rEY7jUZBZmwFSULFUArLBJrHr3amnymkUEYWzQJz13zLacZv33sSuxKVmerpZeFExapBNt8HpAqtTtWqDQRAgyqSKUHu"]
      (let-timed [b58-decoded (with-base 58 :decode s)
                  b2-encoded  (with-base 2  :encode b58-decoded)
                  b2-decoded  (with-base 2  :decode b2-encoded)
                  b8-encoded  (with-base 8 :encode  b2-decoded)
                  b8-decoded  (with-base 8 :decode  b8-encoded)
                  b16-encoded (with-base 16 :encode b8-decoded)
                  b16-decoded (with-base 16 :decode b16-encoded)
                  b58-encoded (with-base 58 :encode b16-decoded)]
        (is (= s b58-encoded))))))

