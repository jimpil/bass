(ns com.github.jimpil.bass.impl.alphabet)

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn index-lookup
  ([ab]
   (index-lookup ab (count ab)))
  ([ab ab-length]
   (zipmap ab (range ab-length))))

(defn multiply
  (^bytes [alphabet characters]
   (multiply alphabet (index-lookup alphabet (count alphabet)) characters))
  (^bytes [alphabet index-of characters]
   (let [base-len (count alphabet)
         len (count characters)
         bi-base  (BigInteger/valueOf base-len)
         ;; 1. Safely calculate exactly how many base-N digits fit into a signed long
         ;; Max long is ~9.22e18. For Base58, log2(58) ≈ 5.85. 62 / 5.85 = 10 digits safe.
         log2-base (/ (Math/log (double base-len))
                      (Math/log 2.0))
         safe-chunk-size (int (quot 62 log2-base))
         ;; 2. Pre-calculate BigInteger powers for the chunks: [base^0, base^1, ... base^safe-chunk-size]
         base-powers  (mapv #(.pow bi-base %) (range (unchecked-inc-int safe-chunk-size)))
         ;; 3. Pre-parse all characters into a flat primitive array of long indices
         indices  (mapv
                    (fn [token]
                      (or (index-of token)
                          (throw
                            (ex-info "Invalid token!" {:token token :alphabet alphabet}))))
                    characters)]
     ;; 4. Tight loop processing data in fast primitive chunks
     (let [^BigInteger ret
           (loop [i   (int 0)
                  acc BigInteger/ZERO]
             (if (< i len)
               (let [remainder (unchecked-subtract-int len i)
                     chunk-sz (if (< remainder safe-chunk-size)
                                remainder
                                safe-chunk-size)
                     end  (unchecked-add-int i chunk-sz)
                     ;; Inner loop: accumulates up to 10+ digits
                     chunk-val (loop [idx i
                                      local-acc 0]
                                 (if (< idx end)
                                   (recur (unchecked-inc-int idx)
                                          (unchecked-add
                                            ^long (nth indices idx)
                                            (unchecked-multiply local-acc base-len)))
                                   local-acc))]
                 (recur end
                        (-> acc
                            (.multiply ^BigInteger (nth base-powers chunk-sz))
                            (.add (BigInteger/valueOf chunk-val)))))
               acc))]
       (.toByteArray ret)))))

(defn divide
  "Calculates a sequence of tokens (from alphabet) for a byte array."
  [^String alphabet ^bytes bs]
  (let [base  (count alphabet)
        limit (quot Long/MAX_VALUE base)
        ;; Dynamically compute the maximum safe power and chunk size for ANY base
        [^int max-power ^long chunk-base]
        (loop [p (int 1)
               val base]
          (if (< val limit) ;; Keep it safely under Long/MAX_VALUE / base
            (recur (unchecked-inc-int p)
                   (unchecked-multiply val base))
            [p val]))
        chunk-base-big (BigInteger/valueOf chunk-base)]
    (loop [n (BigInteger. 1 bs)
           tokens (transient [])]
      (if (neg? (.compareTo n chunk-base-big))
        ;; Final small remainder path (use standard primitive math)
        (loop [rem-long (.longValue n)
               t-acc tokens]
          (if (zero? rem-long)
            (let [ret (persistent! t-acc)]
              [(rseq ret) ;; constant-time op
               (count ret)])
            (recur
              (quot rem-long base)
              (conj! t-acc (.charAt alphabet (rem rem-long base))))))

        ;; Outer Loop: Divide the massive integer by a huge chunk
        (let [^BigInteger/1 drs (.divideAndRemainder n chunk-base-big)
              ^BigInteger quotient      (aget drs 0)
              ^BigInteger remainder-big (aget drs 1)
              ;; Inner Loop: Dissect the remainder chunk using blistering-fast native long arithmetic
              next-tokens (loop [rem-long (.longValue remainder-big)
                                 i (int 0)
                                 t-acc tokens]
                            (if (< i max-power)
                              (recur (quot rem-long base)
                                     (unchecked-inc-int i)
                                     (conj! t-acc (.charAt alphabet (rem rem-long base))))
                              t-acc))]
          (recur quotient next-tokens))))))
