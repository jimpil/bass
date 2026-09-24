(ns com.github.jimpil.bass.impl.base32
  (:require
    [com.github.jimpil.bass.impl.alphabet :as ab])
  (:import
    [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defonce ^:const ^String alphabet
  "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567")

(defonce index-lookup
  (ab/index-lookup alphabet))

(defn encode
  "Encodes a byte-array, or ByteBuffer
   into a standard Base32 string."
  ^String [bs]
  (let [[^int len get-byte] (if (bytes? bs)
                              [(alength ^bytes bs)
                               (fn ^Byte [^long i] (aget ^bytes bs i))]
                              [(.remaining ^ByteBuffer bs)
                               (fn ^Byte [_] (.get ^ByteBuffer bs))])
        capacity (int (* (quot (unchecked-add-int len 4) 5) 8))
        sb (StringBuilder. capacity)]
    (loop [idx       (int 0)
           buffer    (int 0)
           bits-left (int 0)]
      (cond
        ;; Case 1: We have enough bits to emit a Base32 character
        (>= bits-left 5)
        (let [next-bits (unchecked-subtract-int bits-left 5)
              alpha-idx (-> buffer
                            (bit-shift-right next-bits)
                            (bit-and 0x1F))]
          (.append sb (.charAt alphabet alpha-idx))
          (recur idx buffer next-bits))

        ;; Case 2: Buffer low, but we still have input bytes to consume
        (< idx len)
        (let [b (bit-and ^byte (get-byte idx) 0xFF)
              next-buffer (bit-or (bit-shift-left buffer 8) b)
              next-bits (unchecked-add-int bits-left 8)]
          (recur (unchecked-inc-int idx) next-buffer next-bits))

        :else
        (let [c= (char \=)]
          (when (pos? bits-left)
            ;; No more bytes, handle partial trailing bits (less than 5)
            (let [shift (unchecked-subtract-int 5 bits-left)
                  alpha-idx (-> (bit-shift-left buffer shift)
                                (bit-and 0x1F))]
              (.append sb (.charAt alphabet alpha-idx))))
          ;; Pad out to multiple of 8
          (while (pos? (rem (.length sb) 8))
            (.append sb c=))
          (str sb))))))

(defn decode
  "Decodes a Base32 string into a byte-array."
  ^bytes [^String s]
  (let [pad-start (.indexOf s "=")
        sanitized-len (if (pos? pad-start)
                        pad-start
                        (count s))
        ;; Calculate exact expected output length from sanitized data bits
        output-len (-> sanitized-len
                       (unchecked-multiply 5)
                       (quot 8))
        ret (byte-array output-len)]
    (loop [char-idx  (int 0)
           byte-idx  (int 0)
           buffer    (int 0)
           bits-left (int 0)]
      (if (< char-idx sanitized-len)
        (let [c (.charAt s char-idx)
              ^long v (index-lookup c)]
          (if (neg? v)
            (throw (IllegalArgumentException.
                     (str "Invalid character in Base32 string: " c)))
            (let [next-buffer (-> buffer
                                  (bit-shift-left 5)
                                  (bit-or v))
                  next-bits (unchecked-add-int bits-left 5)]
              (if (>= next-bits 8)
                (let [shift (unchecked-subtract-int next-bits 8)
                      b (-> next-buffer
                            (bit-shift-right shift)
                            (bit-and 0xFF))
                      ;; Mask out the bits that were just converted to a byte
                      ;; to keep the buffer clean and prevent potential long overflow
                      clean-buffer (->> (bit-shift-left 1 shift)
                                        unchecked-dec-int
                                        (bit-and next-buffer))]
                  (aset-byte ret byte-idx (unchecked-byte b))
                  (recur (unchecked-inc-int char-idx)
                         (unchecked-inc-int byte-idx)
                         clean-buffer
                         shift))
                (recur (unchecked-inc-int char-idx)
                       byte-idx
                       next-buffer
                       next-bits)))))
        ret))))
