(ns com.github.jimpil.bass.impl.base2
  (:require
    [com.github.jimpil.bass.util :as util])
  (:import
    [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(let [^String/1 lut (->> util/unsigned-byte-range
                         (map (fn [^long i]
                                (-> (bit-or 256 i)
                                    (Integer/toBinaryString) ;; will give 9 bits
                                    (subs 1))))
                         (into-array String))]
  (defn byte->octet
    "Convert a byte value to a binary octet."
    ^String [^Byte b]
    (->> (Byte/toUnsignedInt b)
          (aget lut))))

(defn encode
  "Encodes the provided byte-array <bs> in Base2 (i.e. binary).
   Returns a String whole length is a multiple of 8."
  ^String [bs]
  (if (bytes? bs)
    (let [^bytes bs bs
          bs-length (alength bs)]
      (->> (aget bs idx)
           (byte->octet)
           (.append ret)
           (areduce bs idx ret (StringBuilder. (unchecked-multiply-int bs-length 8)))
           (str)))
    ;; assume ByteBuffer
    (let [^ByteBuffer buf bs
          buf-length (.remaining buf)]
      (->> (.get buf)
           (byte->octet)
           (.append ret)
           (util/breduce buf ret (StringBuilder. (unchecked-multiply-int buf-length 8)))
           (str)))))

(defn decode
  "Decodes a binary string back into a byte array,
   preserving exact leading zeroes (8 bits per byte)."
  ^bytes [^String s]
  (let [len (count s)]
    (if (zero? len)
      util/no-bytes
      (if (pos? (rem len 8))
        (throw
          (IllegalArgumentException.
            "Binary String size must be a multiple of 8!"))
        (let [num-bytes (int (quot len 8))
              ret (byte-array num-bytes)]
          (loop [i (int 0)]
            (when (< i num-bytes)
              (let [start (unchecked-multiply-int i 8)
                    end   (unchecked-add-int start 8)
                    b     (Integer/parseInt s start end 2)]
                (aset-byte ret i (unchecked-byte b))
                (recur (unchecked-inc-int i)))))
          ret)))))
