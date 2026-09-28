(ns com.github.jimpil.bass.base2-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [com.github.jimpil.bass.impl.base2 :as base2]
            [com.github.jimpil.bass.impl.base8 :as base8]))

(deftest by-example
  (testing "BASE 2"
    (testing "max byte (exactly 8 bits)"
      (let [binary-str (str/join (repeat 8 \1)) ;; byte -1
            expected-octal "377"
            expected-decimal 255
            bs (base2/decode binary-str)]
        (is
          (= 1 (alength bs)))

        (is
          (= expected-decimal (Byte/toUnsignedInt (first bs))))

        (is
          (= expected-octal (base8/encode bs)))

        (is
          (= binary-str (base2/encode bs)))))

    (testing "12 bits"
      (let [binary-str (str/join    ;; "101010101010"
                         (concat
                           (repeat 4 \0)  ;; 12 bits => 2 bytes (pad left)
                           (interleave
                             (repeat 6 \1)
                             (repeat 6 \0))))
            expected-octal "012252"
            expected-decimal 2730
            bs (base2/decode binary-str)]
        (is
          (= 2 (alength bs)))

        (is
          (= expected-decimal (BigInteger. 1 bs)))

        (is
          (= expected-octal (base8/encode bs)))

        (is
          (= binary-str (base2/encode bs)))))))
