(ns com.github.jimpil.bass.base8-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [com.github.jimpil.bass.impl.base2 :as base2]
            [com.github.jimpil.bass.impl.base8 :as base8]
            [com.github.jimpil.bass.util :as util]))

(deftest by-example
  (testing "BASE 8"
    (testing "some random byte"
      (let [octal-str "247" ;; 2*8^2 + 4*8 + 7 = 167
            expected-decimal 167
            expected-binary "10100111" ;; byte -89
            bs (util/buffer->bytes
                 (base8/decode octal-str))]
        (is
          (= expected-decimal (Byte/toUnsignedInt (first bs))))

        (is
          (= -89 (first bs)))

        (is
          (= expected-binary (base2/encode bs)))

        (is
          (= octal-str (base8/encode bs)))))

    (testing "max unsigned byte"
      (let [octal-str        "377" ;; 3*8^2 + 7*8 + 7 = 255
            expected-decimal 255
            expected-binary  (str/join (repeat 8 \1)) ;; byte -1
            bs (util/buffer->bytes
                 (base8/decode octal-str))]
        (is
          (= expected-decimal (Byte/toUnsignedInt (first bs))))

        (is
          (= -1 (first bs)))

        (is
          (= expected-binary (base2/encode bs)))

        (is
          (= octal-str (base8/encode bs)))))

    (testing "more than 1 bytes"
      (let [octal-str "001377"
            expected-decimal 511
            expected-binary (str/join
                              (concat
                                (repeat 7 \0)  ;; bytes [1, -1] need 9 bits
                                (repeat 9 \1)))
            ^bytes bs (base8/decode octal-str)]
        (is
          (= expected-decimal (BigInteger. 1 bs)))

        (is
          (= [1 -1] (seq bs)))

        (is
          (= expected-binary (base2/encode bs)))

        (is
          (= octal-str (base8/encode bs)))))))
