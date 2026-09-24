(ns com.github.jimpil.bass.impl.base8
  (:require
    [com.github.jimpil.bass.util :as util])
  (:import
    [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(let [^String/1 lut (->> util/unsigned-byte-range
                         (map (fn [^long i]
                                (let [s (Integer/toOctalString i)]
                                  (case (count s)
                                    1 (str "00" s)
                                    2 (str "0" s)
                                    3 s))))
                         (into-array String))]
  (defn encode
    "Encodes the provided byte-array <bs> in Base8 (i.e. octal).
     Returns a String whole length is a multiple of 3."
    ^String [bs]
    (if (bytes? bs)
      (let [^bytes bs bs
            len (alength bs)]
        (str
          (areduce bs idx ret
                   (StringBuilder. (unchecked-multiply-int len 3))
                   (let [^String s (->> (aget bs idx)
                                        (Byte/toUnsignedInt)
                                        (aget lut))]
                     (.append ret s)))))
      (let [^ByteBuffer buf bs
            buf-length (.remaining buf)]
        (str
          (util/breduce buf ret
                        (StringBuilder. (unchecked-multiply-int buf-length 3))
                        (let [^String s (->> (.get buf)
                                             (Byte/toUnsignedInt)
                                             (aget lut))]
                          (.append ret s)))))

      #_(let [^ByteBuffer buf bs
            len (.remaining buf)]
        (let [sb (StringBuilder. (unchecked-multiply-int len 3))]
          (while (.hasRemaining buf)
            (let [^String s (->> (.get buf)
                                 (Byte/toUnsignedInt)
                                 (aget lut))]
              (.append sb s)))
          (str sb))))))

(defn decode
  "Decodes an octal string back into a byte array,
   preserving exact leading zeroes (3 octal chars per byte)."
  ^bytes [^String s]
  (let [len (count s)]
    (if (zero? len)
      util/no-bytes
      (if (pos? (rem len 3))
        (throw
          (IllegalArgumentException.
            "Octal String size must be a multiple of 3!"))
        (let [num-bytes (int (quot len 3))
              ret (byte-array num-bytes)]
          (loop [i (int 0)]
            (when (< i num-bytes)
              (let [start (unchecked-multiply-int i 3)
                    end   (unchecked-add-int start 3)
                    b     (Integer/parseInt s start end 8)]
                (aset-byte ret i (unchecked-byte b))
                (recur (unchecked-inc-int i)))))
          ret)))))
