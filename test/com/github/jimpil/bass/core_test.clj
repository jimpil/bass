(ns com.github.jimpil.bass.core-test
  (:require [clojure.test :refer :all]
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

(defspec buffer-roundtrip-is-lossless 2000
  (prop/for-all [^ByteBuffer buf (gen/fmap
                                   (fn [^bytes bs]
                                      (doto (ByteBuffer/wrap bs)
                                      .mark)) ;; we will consume this twice!
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

