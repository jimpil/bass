(ns com.github.jimpil.bass.impl.base16
  (:require
    [com.github.jimpil.bass.util :as util])
  (:import
    [java.util HexFormat]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn encode
  "Encodes the provided byte-array <bs> in Base16 (i.e. hex).
   Returns String."
  (^String [bs]
   (encode :lower bs))
  (^String [char-case bs]
   (let [^bytes bs (util/buffer->bytes bs)
         ^HexFormat hex-format
                   (case char-case
                     :lower (HexFormat/of)
                     :upper (-> (HexFormat/of)
                                (.withUpperCase)))]
     (.formatHex hex-format bs))))

(defn decode
  "Decodes the provided String <s> from Base16 (i.e. hex).
   Returns byte-array."
  (^bytes [s]
   (decode :lower s))
  (^bytes [char-case ^String s]
   (let [^HexFormat hex-format
         (case char-case
           :lower (HexFormat/of)
           :upper (-> (HexFormat/of)
                      (.withUpperCase)))]
     (.parseHex hex-format s))))
