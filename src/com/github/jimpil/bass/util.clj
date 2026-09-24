(ns com.github.jimpil.bass.util
  (:import [clojure.lang IReduceInit]
           [java.nio ByteBuffer]))

(defonce ^byte/1 no-bytes (byte-array 0))
(defonce unsigned-byte-range (range 256))

(defn buffer->bytes
  "Bulk get on this ByteBuffer."
  ^bytes [buf]
  (if (bytes? buf)
    buf
    (let [^ByteBuffer buf buf
          ret (byte-array (.remaining buf))]
      (.get buf ret)
      ret)))

(defmacro breduce
  "Similar to `clojure.core/areduce` but for ByteBuffer first arg.
   Does NOT expose an index symbol, because <expr> is expected to
   get the next byte via `ByteBuffer.get()`. The position of the
   buffer is marked at the start, so you do have options when it
   comes to consuming it more than once, or <expr> throwing.
   Avoids boxing."
  [b ret init expr]
  `(let [b# (.mark ~b)]
     (loop  [~ret ~init]
       (if (.hasRemaining b#)
         (recur ~expr)
         ~ret))))

(defn buffer-reducible
  "Turns the provided ByteBuffer into something reducible
   (i.e. compatible with reduce/transduce). The position of the
   buffer is marked at the start, so you do have options when it
   comes to consuming it more than once, partial consumption (per `reduced?`),
   or <expr> throwing. Incurs boxing (i.e. the reducing-fn will receive
   Byte as the second arg). See `breduce` above, if that is a concern."
  [^ByteBuffer buf]
  (reify IReduceInit
    (reduce [_ f init]
      (let [buf (.mark buf)]
        (loop [state init]
          (if (reduced? state)
            @state
            (if (.hasRemaining buf)
              (recur (f state (.get buf)))
              state)))))))

(defn random-bytes
  ^bytes [n]
  (->> (range Byte/MIN_VALUE (inc Byte/MAX_VALUE))
       (partial rand-nth)
       (repeatedly n)
       byte-array))
