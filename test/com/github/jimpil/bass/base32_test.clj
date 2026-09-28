(ns com.github.jimpil.bass.base32-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [com.github.jimpil.bass.impl.base32 :as base32])
  (:import
    [org.apache.commons.codec.binary Base32]))

(deftest by-example
  (testing "encoding examples"
    (are ;; simple-cases
      [expected produced]
      (= expected produced)
      ""                         (base32/encode (byte-array 0))
      "MY======"                 (base32/encode (.getBytes  "f" "UTF-8"))
      "MZXQ===="                 (base32/encode (.getBytes  "fo" "UTF-8"))
      "MZXW6==="                 (base32/encode (.getBytes  "foo" "UTF-8"))
      "MZXW6YQ="                 (base32/encode (.getBytes  "foob" "UTF-8"))
      "MZXW6YTB"                 (base32/encode (.getBytes  "fooba" "UTF-8"))
      "MZXW6YTBOI======"         (base32/encode (.getBytes  "foobar" "UTF-8"))
      "JBSWY3DPEBLW64TMMQ======" (base32/encode (.getBytes  "Hello World" "UTF-8")))

    (are ;; edge-cases
      [expected produced]
      (= expected produced)
      ;; 1. All Zeros (Tests if zero-bit shifts handle indices correctly)
      "AAAAAAAA" (base32/encode (byte-array (repeat 5 0)))
      ;; 2. Maximum Byte Values (Tests if bit-masking `0xFF` handles signed vs unsigned bytes correctly)
      "77777777" (base32/encode (byte-array (repeat 5 -1)))
      ;; 3. Sequential Bytes
      "AAAQEAYE" (base32/encode (byte-array [0 1 2 3 4])))))

(defspec commons-codec-parity 10000
  (let [B32 (Base32.)]
    (prop/for-all [bs gen/bytes]
      (let [encoded (base32/encode bs)
            decoded (base32/decode encoded)]
        (is (= (.encodeToString B32 bs)
               encoded))
        (is (= (seq (.decode B32 encoded))
               (seq decoded)))))))
