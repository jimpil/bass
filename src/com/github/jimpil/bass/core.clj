(ns com.github.jimpil.bass.core
  (:require [com.github.jimpil.bass.impl.finder :as impl]))

(defmacro with-base*
  [k [encoder decoder] & body]
  (case k
    :encode `(~encoder (do ~@body))   ;; body must return byte-array or ByteBuffer
    :decode `(~decoder (do ~@body)))) ;; body must return String

(defmacro with-base
  [b k & body]
  (case b
    (2,   "2", :base2) `(with-base* ~k ~(impl/encoder-decoder :base2) ~@body)
    (8,   "8", :base8) `(with-base* ~k ~(impl/encoder-decoder :base8) ~@body)
    (32, "32", :base32) `(with-base* ~k ~(impl/encoder-decoder :base32) ~@body)
    (58, "58", :base58) `(with-base* ~k ~(impl/encoder-decoder :base58) ~@body)
    (16, "16", :base16 :base16/lc)  `(with-base* ~k ~(impl/encoder-decoder :base16) ~@body)
    (64, "64", :base64 :base64/std) `(with-base* ~k ~(impl/encoder-decoder :base64) ~@body)

    :base16/uc   `(with-base* ~k ~(impl/encoder-decoder :base16/uc)   ~@body)
    :base64/url  `(with-base* ~k ~(impl/encoder-decoder :base64/url)  ~@body)
    :base64/mime `(with-base* ~k ~(impl/encoder-decoder :base64/mime) ~@body)))

(comment
  (require '[criterium.core :refer [bench quick-bench]]
           '[com.github.jimpil.bass.util :refer [random-bytes]])

  (let [bs (random-bytes 1024)]
    (quick-bench
      (with-base 2 :encode bs)))

  (let [bs (with-base 8 :encode (random-bytes 1024))]
    (quick-bench
      (with-base 8 :decode bs)))


  ;; full round-trip in 0.66 ms
  (let [s1 "xpub67uA5wAUuv1ypp7rEY7jUZBZmwFSULFUArLBJrHr3amnymkUEYWzQJz13zLacZv33sSuxKVmerpZeFExapBNt8HpAqtTtWqDQRAgyqSKUHu"]
    (->> s1
         (with-base 58 :decode)
         (with-base :base16/uc :encode)
         (with-base :base16/uc :decode)
         (with-base 2 :encode)
         (with-base 2 :decode)
         (with-base 64 :encode)
         (with-base 64 :decode)
         (with-base 8 :encode)
         (with-base 8 :decode)
         (with-base 58 :encode)
         time
         ;println
         (= s1)))

  )

