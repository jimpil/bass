(ns com.github.jimpil.bass.impl.base58
  (:require
    [com.github.jimpil.bass.impl.alphabet :as ab]
    [com.github.jimpil.bass.util :as util])
  (:import [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def ^:const ^String alphabet
  "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")

(def index-lookup
  (ab/index-lookup alphabet))

(try
  (Class/forName "org.apache.commons.codec.binary.Base58")
  (println "Detected `commons-codec` on the classpath - delegating to its optimised Base58 impl...")

  (eval
    `(def ~(with-meta 'BASE58 {:tag 'org.apache.commons.codec.binary.Base58})
       (~'org.apache.commons.codec.binary.Base58.)))

  (eval
    `(defn ~(with-meta 'encode {:tag 'java.lang.String})
       [~'bs]
       (.encodeToString ~'BASE58 (util/buffer->bytes ~'bs))))

  (eval
    `(defn ~(with-meta 'decode {:tag 'byte/1})
       [~(with-meta 's {:tag 'java.lang.String})]
       (.decode ~'BASE58 ~'s)))

  (catch ClassNotFoundException _

    (defn encode
      ^String [bs]
      (let [^bytes bs (util/buffer->bytes bs)
            zeroes (count (take-while zero? bs))
            [characters ^long n] (if (< zeroes (alength bs))
                                   (ab/divide alphabet bs)
                                   [[] 0])
            sb (StringBuilder. (unchecked-add n zeroes))
            c1 (char \1)]
        (dotimes [_ zeroes] (.append sb c1))
        (run! (fn [^Character c] (.append sb c)) characters)
        (str sb)))

    (defn decode
      [s-or-chars]
      (let [nzeroes (count (take-while #{\1} s-or-chars))]
        (cond
          (= nzeroes (count s-or-chars))
          (byte-array nzeroes)

          (zero? nzeroes) ;; fast path
          (let [byte-arr (ab/multiply alphabet index-lookup s-or-chars)
                data-length (alength byte-arr)
                signed? (and (> data-length 1)
                             (zero? (aget byte-arr 0)))]
            (if signed?
              (ByteBuffer/wrap byte-arr 1 (unchecked-dec-int data-length))
              byte-arr))

          :else ;; slow path
          (let [byte-arr (ab/multiply alphabet index-lookup s-or-chars)
                data-length (alength byte-arr)
                signed? (and (> data-length 1)
                             (zero? (aget byte-arr 0)))
                length (cond-> (unchecked-add nzeroes data-length)
                               signed? unchecked-dec)
                ret (byte-array length)]
            (System/arraycopy
              byte-arr (if signed? 1 0)
              ret nzeroes
              (unchecked-subtract length nzeroes))
            ret))))
    )
  )


