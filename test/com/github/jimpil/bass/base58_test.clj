(ns com.github.jimpil.bass.base58-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [com.github.jimpil.bass.impl.base16 :as base16]
            [com.github.jimpil.bass.impl.base58 :as base58]
            [com.github.jimpil.bass.util :as util])
  (:import
    [org.apache.commons.codec.binary Base58]))

(deftest by-example
  (testing "main-net bitcoin address"
    (let [b58-str "xpub67uA5wAUuv1ypp7rEY7jUZBZmwFSULFUArLBJrHr3amnymkUEYWzQJz13zLacZv33sSuxKVmerpZeFExapBNt8HpAqtTtWqDQRAgyqSKUHu"
          b58-bytes (base58/decode b58-str)]
      (is ;; https://en.bitcoin.it/wiki/List_of_address_prefixes
        (-> (base16/encode :upper b58-bytes)
            (str/starts-with? "0488B21E")))
      (is
        (= b58-str (base58/encode b58-bytes))))))

(defspec base58-encoding-matches-commons-codec 5000
  (let [B58 (Base58.)]
    (prop/for-all [bs gen/bytes]
      (is (= (.encodeToString B58 bs)
             (base58/encode bs))))))

(defspec base58-decoding-matches-commons-codec 5000
  (let [B58 (Base58.)]
    (prop/for-all [s (gen/fmap
                       (partial apply str)
                       (gen/vector
                         (gen/elements (vec base58/alphabet))))]
      (is (= (seq (.decode B58 s))
             (seq (util/buffer->bytes (base58/decode s))))))))
